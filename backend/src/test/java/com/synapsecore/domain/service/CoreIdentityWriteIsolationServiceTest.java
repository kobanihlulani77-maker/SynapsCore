package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class CoreIdentityWriteIsolationServiceTest {

    @Test
    void acquiresCoreIdentityLockBeforeWriteInExistingTransaction() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:coreidentityactive;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table proof_rows (id bigint primary key, note varchar(64) not null)");

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TrackingIdentitySequenceMigrationService trackingSequenceService = new TrackingIdentitySequenceMigrationService(jdbcTemplate);
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
            EntityManager.class.getClassLoader(),
            new Class<?>[] { EntityManager.class },
            (proxy, method, args) -> null
        );
        CoreIdentityWriteIsolationService isolationService = new CoreIdentityWriteIsolationService(
            transactionManager,
            trackingSequenceService,
            entityManager
        );
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> isolationService.persistWithSequenceRepair(
            "Proof active-transaction write",
            () -> jdbcTemplate.update("insert into proof_rows (id, note) values (?, ?)", 1L, "active-transaction-row")
        ));

        assertThat(trackingSequenceService.lockAcquisitionCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from proof_rows", Long.class)).isEqualTo(1L);
    }

    @Test
    void retriesConflictedWriteInFreshTransactionWithoutPoisoningOuterTransaction() {
        DataSource dataSource = new DriverManagerDataSource(
            "jdbc:h2:mem:coreidentitywrite;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
            "sa",
            ""
        );
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table proof_rows (id bigint primary key, note varchar(64) not null)");

        DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
        TrackingIdentitySequenceMigrationService trackingSequenceService = new TrackingIdentitySequenceMigrationService(jdbcTemplate);
        CoreIdentityWriteIsolationService isolationService = new CoreIdentityWriteIsolationService(
            transactionManager,
            trackingSequenceService
        );
        TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);
        AtomicInteger attempts = new AtomicInteger();

        outerTransaction.executeWithoutResult(status -> {
            jdbcTemplate.update("insert into proof_rows (id, note) values (?, ?)", 1L, "outer-transaction-row");
            isolationService.persistWithSequenceRepair("Proof auxiliary write", () -> {
                if (attempts.getAndIncrement() == 0) {
                    throw new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"audit_logs_pkey\""
                    );
                }
                jdbcTemplate.update("insert into proof_rows (id, note) values (?, ?)", 2L, "retry-succeeded");
            });
            assertThat(status.isRollbackOnly()).isFalse();
        });

        assertThat(attempts.get()).isEqualTo(2);
        assertThat(trackingSequenceService.synchronizationCount()).isEqualTo(1);
        assertThat(trackingSequenceService.lockAcquisitionCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from proof_rows", Long.class)).isEqualTo(2L);
    }

    private static final class TrackingIdentitySequenceMigrationService extends IdentitySequenceMigrationService {

        private int synchronizationCount;
        private int lockAcquisitionCount;

        private TrackingIdentitySequenceMigrationService(JdbcTemplate jdbcTemplate) {
            super(jdbcTemplate);
        }

        @Override
        public synchronized void synchronizeCoreIdentitySequences() {
            synchronizationCount++;
        }

        @Override
        public void acquireCoreIdentityWriteLock() {
            lockAcquisitionCount++;
        }

        private int synchronizationCount() {
            return synchronizationCount;
        }

        private int lockAcquisitionCount() {
            return lockAcquisitionCount;
        }
    }
}
