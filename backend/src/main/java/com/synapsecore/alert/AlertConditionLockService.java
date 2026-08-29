package com.synapsecore.alert;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Serializes one alert condition in-process and, on PostgreSQL, across app nodes.
 */
@Service
@RequiredArgsConstructor
public class AlertConditionLockService {

    private final DataSource dataSource;
    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T withLock(String conditionKey, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(conditionKey, ignored -> new ReentrantLock());
        if (lock.isHeldByCurrentThread()) {
            // Nested evaluations share the outer transaction-scoped lock.
            return action.get();
        }
        lock.lock();

        boolean releaseAfterTransaction = TransactionSynchronizationManager.isSynchronizationActive();
        try {
            acquirePostgresLock(conditionKey);
            T result = action.get();
            if (releaseAfterTransaction) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        release(conditionKey, lock);
                    }
                });
            } else {
                release(conditionKey, lock);
            }
            return result;
        } catch (RuntimeException | Error exception) {
            if (!releaseAfterTransaction) {
                release(conditionKey, lock);
            }
            throw exception;
        }
    }

    private void acquirePostgresLock(String conditionKey) {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            if (!connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql")) {
                return;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                "select pg_advisory_xact_lock(hashtext(?))")) {
                statement.setString(1, conditionKey);
                statement.executeQuery();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not lock the Alert condition for safe synchronization.", exception);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void release(String conditionKey, ReentrantLock lock) {
        lock.unlock();
        if (!lock.isLocked() && !lock.hasQueuedThreads()) {
            locks.remove(conditionKey, lock);
        }
    }
}
