package com.synapsecore.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class AlertConditionLockServiceTest {

    private static final String CONDITION_KEY = "LOW_STOCK|PRODUCT:1|WAREHOUSE:1";

    @Test
    void transactionalAdvisoryFailureReleasesAfterRollbackCompletion() throws Exception {
        AlertConditionLockService service = new AlertConditionLockService(
            dataSource(true, true)
        );

        assertTransactionalFailureReleasesAfterCompletion(
            service,
            () -> service.withLock(CONDITION_KEY, () -> true),
            CannotAcquireLockException.class
        );
    }

    @Test
    void transactionalActionFailureReleasesAfterRollbackCompletion() throws Exception {
        AlertConditionLockService service = new AlertConditionLockService(
            dataSource(false, false)
        );

        assertTransactionalFailureReleasesAfterCompletion(
            service,
            () -> service.withLock(CONDITION_KEY, () -> {
                throw new IllegalStateException("simulated protected action failure");
            }),
            IllegalStateException.class
        );
    }

    @Test
    void nonTransactionalAdvisoryFailureReleasesBeforeWithLockExits() {
        AlertConditionLockService service = new AlertConditionLockService(
            dataSource(true, true)
        );

        assertThatThrownBy(() -> service.withLock(CONDITION_KEY, () -> true))
            .isInstanceOf(CannotAcquireLockException.class);
        assertThat(service.withLock(CONDITION_KEY, () -> "recovered"))
            .isEqualTo("recovered");
    }

    @Test
    void nonTransactionalActionFailureReleasesBeforeWithLockExits() {
        AlertConditionLockService service = new AlertConditionLockService(
            dataSource(false, false)
        );

        assertThatThrownBy(() -> service.withLock(CONDITION_KEY, () -> {
            throw new IllegalStateException("simulated protected action failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(service.withLock(CONDITION_KEY, () -> "recovered"))
            .isEqualTo("recovered");
    }

    @Test
    void nestedSameThreadEvaluationUsesOneTransactionCompletionRelease() throws Exception {
        AlertConditionLockService service = new AlertConditionLockService(
            dataSource(false, false)
        );

        TransactionSynchronizationManager.initSynchronization();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThat(service.withLock(CONDITION_KEY,
                () -> service.withLock(CONDITION_KEY, () -> "nested")))
                .isEqualTo("nested");

            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations.stream()
                .filter(this::isConditionLockSynchronization)
                .count()).isEqualTo(1);

            CountDownLatch entered = new CountDownLatch(1);
            Future<String> secondAttempt = executor.submit(() -> service.withLock(
                CONDITION_KEY,
                () -> {
                    entered.countDown();
                    return "second";
                }
            ));

            assertThat(entered.await(200, TimeUnit.MILLISECONDS)).isFalse();
            completeTransaction(synchronizations);
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAttempt.get(1, TimeUnit.SECONDS)).isEqualTo("second");
        } finally {
            clearSynchronization();
            executor.shutdownNow();
        }
    }

    private void assertTransactionalFailureReleasesAfterCompletion(AlertConditionLockService service,
                                                                    Runnable failingCall,
                                                                    Class<? extends Throwable> failureType)
        throws Exception {
        TransactionSynchronizationManager.initSynchronization();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(failingCall::run).isInstanceOf(failureType);

            CountDownLatch entered = new CountDownLatch(1);
            Future<Boolean> secondAttempt = executor.submit(() -> service.withLock(
                CONDITION_KEY,
                () -> {
                    entered.countDown();
                    return true;
                }
            ));

            assertThat(entered.await(200, TimeUnit.MILLISECONDS)).isFalse();
            List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations.stream().anyMatch(this::isConditionLockSynchronization))
                .as("transactional failure must register a completion release")
                .isTrue();

            completeTransaction(synchronizations);
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(secondAttempt.get(1, TimeUnit.SECONDS)).isTrue();
        } finally {
            clearSynchronization();
            executor.shutdownNow();
        }
    }

    private void completeTransaction(List<TransactionSynchronization> synchronizations) {
        synchronizations.forEach(synchronization ->
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
    }

    private boolean isConditionLockSynchronization(TransactionSynchronization synchronization) {
        return synchronization.getClass().getName().contains("AlertConditionLockService");
    }

    private void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private DataSource dataSource(boolean postgres, boolean failAdvisoryLock) {
        AtomicBoolean advisoryFailure = new AtomicBoolean(failAdvisoryLock);
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getDatabaseProductName")) {
                    return postgres ? "PostgreSQL" : "H2";
                }
                return defaultValue(method.getReturnType());
            }
        );
        Connection connection = (Connection) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getMetaData")) {
                    return metadata;
                }
                if (method.getName().equals("prepareStatement")) {
                    String sql = (String) args[0];
                    return preparedStatement(sql, advisoryFailure);
                }
                return defaultValue(method.getReturnType());
            }
        );
        return new DataSource() {
            @Override
            public Connection getConnection() {
                return connection;
            }

            @Override
            public Connection getConnection(String username, String password) {
                return connection;
            }

            @Override
            public <T> T unwrap(Class<T> iface) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) {
                return false;
            }

            @Override
            public java.io.PrintWriter getLogWriter() {
                return null;
            }

            @Override
            public void setLogWriter(java.io.PrintWriter out) {
            }

            @Override
            public void setLoginTimeout(int seconds) {
            }

            @Override
            public int getLoginTimeout() {
                return 0;
            }

            @Override
            public java.util.logging.Logger getParentLogger() {
                return java.util.logging.Logger.getGlobal();
            }
        };
    }

    private PreparedStatement preparedStatement(String sql, AtomicBoolean advisoryFailure) {
        return (PreparedStatement) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[] {PreparedStatement.class},
            (proxy, method, args) -> {
                if (method.getName().equals("executeQuery")
                    && advisoryFailure.compareAndSet(true, false)
                    && sql.contains("pg_advisory_xact_lock")) {
                    throw new SQLException("simulated advisory lock failure");
                }
                return defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
