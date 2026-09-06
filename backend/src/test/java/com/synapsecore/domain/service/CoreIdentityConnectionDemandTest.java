package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

class CoreIdentityConnectionDemandTest {

    @Test
    void repairUsesOneConnectionWithTheRealTransactionProxy() {
        try (ProofContext proof = new ProofContext(2, null)) {
            HikariDataSource pool = proof.pool;
            JdbcTemplate jdbc = proof.jdbc;
            RecordingTransactionManager transactions = proof.transactions;
            CoreIdentityWriteIsolationService service = proof.service;
            AtomicInteger attempts = new AtomicInteger();

            service.persistWithSequenceRepair("identity demand proof", () -> {
                if (attempts.getAndIncrement() == 0) {
                    jdbc.update("insert into proof_witness (note) values ('must-rollback')");
                    jdbc.update("insert into products (id, note) values (1, 'duplicate')");
                } else {
                    jdbc.update("insert into proof_witness (note) values ('retry-committed')");
                    jdbc.update("insert into products (note) values ('retry')");
                }
            });

            assertThat(attempts).hasValue(2);
            assertThat(jdbc.queryForObject("select count(*) from products", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForList("select note from proof_witness", String.class)).containsExactly("retry-committed");
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(transactions.peakActive.get())
                .as("one independent repair must not suspend an otherwise empty wrapper connection")
                .isEqualTo(1);
        }
    }

    @Test
    void secondConflictPropagatesWithoutAnotherRetryAndReleasesConnections() {
        try (ProofContext proof = new ProofContext(2, null)) {
            AtomicInteger attempts = new AtomicInteger();
            assertThatThrownBy(() -> proof.service.persistWithSequenceRepair("permanent conflict", () -> {
                attempts.incrementAndGet();
                proof.jdbc.update("insert into proof_witness (note) values ('must-rollback')");
                proof.jdbc.update("insert into products (id, note) values (1, 'duplicate')");
            })).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(attempts).hasValue(2);
            assertThat(proof.jdbc.queryForObject("select count(*) from products", Integer.class)).isEqualTo(1);
            assertThat(proof.jdbc.queryForObject("select count(*) from proof_witness", Integer.class)).isZero();
            assertThat(proof.pool.getHikariPoolMXBean().getActiveConnections()).isZero();
        }
    }

    @Test
    void existingTransactionStillRollsBackItsWriteAndAuxiliaryEvidenceTogether() {
        try (ProofContext proof = new ProofContext(2, null)) {
            TransactionTemplate transaction = new TransactionTemplate(proof.transactions);
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                proof.jdbc.update("insert into products (note) values ('outer-write')");
                proof.service.persistWithSequenceRepair("atomic auxiliary evidence", () ->
                    proof.jdbc.update("insert into proof_witness (note) values ('same-transaction')"));
                throw new IllegalStateException("force outer rollback");
            })).isInstanceOf(IllegalStateException.class).hasMessage("force outer rollback");

            assertThat(proof.jdbc.queryForObject("select count(*) from products", Integer.class)).isEqualTo(1);
            assertThat(proof.jdbc.queryForObject("select count(*) from proof_witness", Integer.class)).isZero();
            assertThat(proof.transactions.peakActive.get()).isEqualTo(1);
            assertThat(proof.pool.getHikariPoolMXBean().getActiveConnections()).isZero();
        }
    }

    @Test
    @Timeout(30)
    void tenConcurrentRepairsCompleteWithTenConnections() throws Exception {
        int workers = 10;
        // Align entry after the failed write rolls back, before sequence repair executes.
        // This controls overlap without delaying SQL or injecting database locks.
        try (ProofContext proof = new ProofContext(workers, new CyclicBarrier(workers))) {
            HikariDataSource pool = proof.pool;
            JdbcTemplate jdbc = proof.jdbc;
            RecordingTransactionManager transactions = proof.transactions;
            CoreIdentityWriteIsolationService service = proof.service;
            AtomicInteger attempts = new AtomicInteger();
            AtomicInteger peakWaiters = new AtomicInteger();
            var executor = Executors.newFixedThreadPool(workers);
            List<Future<Throwable>> tasks = new ArrayList<>();
            try {
                for (int i = 0; i < workers; i++) {
                    int worker = i;
                    tasks.add(executor.submit(() -> {
                        AtomicInteger ownAttempts = new AtomicInteger();
                        try {
                            service.persistWithSequenceRepair("concurrent repair " + worker, () -> {
                                attempts.incrementAndGet();
                                if (ownAttempts.getAndIncrement() == 0) {
                                    jdbc.update("insert into products (id, note) values (1, ?)", "duplicate-" + worker);
                                } else {
                                    jdbc.update("insert into products (note) values (?)", "retry-" + worker);
                                }
                            });
                            return null;
                        } catch (RuntimeException exception) {
                            return exception;
                        }
                    }));
                }
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                while (tasks.stream().anyMatch(task -> !task.isDone()) && System.nanoTime() < deadline) {
                    peakWaiters.accumulateAndGet(pool.getHikariPoolMXBean().getThreadsAwaitingConnection(), Math::max);
                    Thread.sleep(10);
                }
                List<Throwable> failures = new ArrayList<>();
                for (Future<Throwable> task : tasks) {
                    Throwable failure = task.get(2, TimeUnit.SECONDS);
                    if (failure != null) {
                        failures.add(failure);
                    }
                }
                assertThat(failures)
                    .as("ten repairs; peak active=%s; peak waiters=%s", transactions.peakActive.get(), peakWaiters.get())
                    .isEmpty();
                assertThat(attempts).hasValue(workers * 2);
                assertThat(jdbc.queryForObject("select count(*) from products", Integer.class)).isEqualTo(workers + 1);
                assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
                assertThat(pool.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    private HikariDataSource pool(int size) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(size);
        config.setMinimumIdle(size);
        config.setConnectionTimeout(1500);
        config.setPoolName("IdentityDemandProof");
        return new HikariDataSource(config);
    }

    private JdbcTemplate prepareSchema(HikariDataSource pool) {
        JdbcTemplate jdbc = new JdbcTemplate(pool);
        jdbc.execute("create table products (id bigint generated by default as identity primary key, note varchar(64))");
        jdbc.execute("create table proof_witness (note varchar(64))");
        jdbc.update("insert into products (id, note) values (1, 'existing')");
        jdbc.execute("alter table products alter column id restart with 2");
        return jdbc;
    }

    private CoreIdentityWriteIsolationService service(JdbcTemplate jdbc, JpaTransactionManager transactions) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(new IdentitySequenceMigrationService(jdbc));
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(interceptor);
        return new CoreIdentityWriteIsolationService(
            transactions, (IdentitySequenceMigrationService) proxy.getProxy(),
            SharedEntityManagerCreator.createSharedEntityManager(transactions.getEntityManagerFactory()));
    }

    private final class ProofContext implements AutoCloseable {
        private final HikariDataSource pool;
        private final JdbcTemplate jdbc;
        private final LocalContainerEntityManagerFactoryBean factory;
        private final RecordingTransactionManager transactions;
        private final CoreIdentityWriteIsolationService service;

        private ProofContext(int size, CyclicBarrier barrier) {
            pool = pool(size);
            jdbc = prepareSchema(pool);
            factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(pool);
            factory.setPackagesToScan("com.synapsecore.domain.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.afterPropertiesSet();
            transactions = new RecordingTransactionManager(pool, factory.getObject(), barrier);
            service = service(jdbc, transactions);
        }

        @Override
        public void close() {
            factory.destroy();
            pool.close();
        }
    }

    private static final class RecordingTransactionManager extends JpaTransactionManager {
        private final HikariDataSource pool;
        private final CyclicBarrier repairBarrier;
        private final ThreadLocal<Integer> begins = ThreadLocal.withInitial(() -> 0);
        private final AtomicInteger peakActive = new AtomicInteger();

        private RecordingTransactionManager(HikariDataSource pool, EntityManagerFactory factory, CyclicBarrier repairBarrier) {
            super(factory);
            setDataSource(pool);
            this.pool = pool;
            this.repairBarrier = repairBarrier;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            super.doBegin(transaction, definition);
            peakActive.accumulateAndGet(pool.getHikariPoolMXBean().getActiveConnections(), Math::max);
            int begin = begins.get() + 1;
            begins.set(begin);
            if (repairBarrier != null && begin == 2) {
                try {
                    repairBarrier.await(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Repair overlap could not be established", exception);
                }
            }
        }
    }
}
