package com.synapsecore.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.dto.ProductUpsertRequest;
import com.synapsecore.domain.entity.Tenant;
import com.synapsecore.tenant.TenantContextService;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class ProductPreflightConnectionDemandTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preflightAndCatalogWorkUseOneConnectionAtATime(boolean csv) {
        try (Proof proof = new Proof(2, null)) {
            assertThat(catchThrowable(() -> invoke(proof.product, csv)))
                .isInstanceOf(CatalogBoundaryReached.class);
            assertThat(proof.transactions.peakActive.get()).isEqualTo(1);
            assertThat(proof.transactions.peakDepth.get()).isEqualTo(1);
            proof.assertReleasedAndRolledBack();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @Timeout(30)
    void tenCatalogPreflightsMakeProgressWithTenConnections(boolean csv) throws Exception {
        int workers = 10;
        try (Proof proof = new Proof(workers, new CyclicBarrier(workers))) {
            var executor = Executors.newFixedThreadPool(workers);
            List<Future<Throwable>> tasks = new ArrayList<>();
            AtomicInteger peakWaiters = new AtomicInteger();
            try {
                for (int i = 0; i < workers; i++) {
                    tasks.add(executor.submit(() -> catchThrowable(() -> invoke(proof.product, csv))));
                }
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
                while (tasks.stream().anyMatch(task -> !task.isDone()) && System.nanoTime() < deadline) {
                    peakWaiters.accumulateAndGet(proof.pool.getHikariPoolMXBean().getThreadsAwaitingConnection(), Math::max);
                    Thread.sleep(10);
                }
                List<Throwable> outcomes = new ArrayList<>();
                for (Future<Throwable> task : tasks) {
                    outcomes.add(task.get(2, TimeUnit.SECONDS));
                }
                long reached = outcomes.stream().filter(CatalogBoundaryReached.class::isInstance).count();
                System.out.printf("Catalog preflight csv=%s workers=%d peakActive=%d peakWaiters=%d reached=%d%n",
                    csv, workers, proof.transactions.peakActive.get(), peakWaiters.get(), reached);
                assertThat(outcomes).allSatisfy(outcome -> assertThat(outcome)
                    .as("Every request must reach catalog work, not time out acquiring its nested connection")
                    .isInstanceOf(CatalogBoundaryReached.class));
                assertThat(proof.transactions.peakDepth.get()).isEqualTo(1);
                proof.assertReleasedAndRolledBack();
            } finally {
                executor.shutdownNow();
                assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    private void invoke(ProductService service, boolean csv) {
        if (csv) {
            service.importProducts(new MockMultipartFile("file", "proof.csv", "text/csv",
                "sku,name,category\nPROOF,Proof,Test\n".getBytes(java.nio.charset.StandardCharsets.UTF_8)), "proof");
        } else {
            service.createProduct(new ProductUpsertRequest("PROOF", "Proof", "Test"), "proof");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void explicitOuterCallerStillOwnsCatalogRollback(boolean csv) {
        try (Proof proof = new Proof(2, null)) {
            TransactionTemplate outer = new TransactionTemplate(proof.transactions);
            assertThat(catchThrowable(() -> outer.executeWithoutResult(status -> {
                proof.jdbc.update("insert into proof_witness (note) values ('outer-must-rollback')");
                invoke(proof.product, csv);
            }))).isInstanceOf(CatalogBoundaryReached.class);
            // These are not the HTTP entrypoints: a pre-existing caller still owns
            // its connection during independent preflight. Do not silently commit it.
            assertThat(proof.transactions.peakDepth.get()).isEqualTo(2);
            proof.assertReleasedAndRolledBack();
        }
    }

    private static final class CatalogBoundaryReached extends RuntimeException {}

    private static <T> T proxied(T target, Class<T> type, JpaTransactionManager transactions) {
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(interceptor);
        return type.cast(proxy.getProxy());
    }

    private static final class Proof implements AutoCloseable {
        private final HikariDataSource pool;
        private final JdbcTemplate jdbc;
        private final LocalContainerEntityManagerFactoryBean factory;
        private final RecordingTransactions transactions;
        private final ProductService product;

        private Proof(int size, CyclicBarrier barrier) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
            config.setUsername("sa");
            config.setMaximumPoolSize(size);
            config.setMinimumIdle(size);
            config.setConnectionTimeout(1500);
            config.setPoolName("CatalogPreflightProof");
            pool = new HikariDataSource(config);
            jdbc = new JdbcTemplate(pool);
            jdbc.execute("create table products (id bigint generated by default as identity primary key, note varchar(64))");
            jdbc.execute("create table proof_witness (note varchar(64))");
            factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(pool);
            factory.setPackagesToScan("com.synapsecore.domain.entity");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.afterPropertiesSet();
            transactions = new RecordingTransactions(pool, factory.getObject(), barrier);
            IdentitySequenceMigrationService sequences = proxied(new IdentitySequenceMigrationService(jdbc),
                IdentitySequenceMigrationService.class, transactions);
            // Stop immediately after real sequence preflight. The witness proves that
            // catalog work still has a write transaction and rolls back on failure.
            TenantContextService tenant = new TenantContextService(null, null, null, null, null) {
                @Override public Tenant getCurrentTenantOrDefault() {
                    assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                    assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
                    jdbc.update("insert into proof_witness (note) values ('must-rollback')");
                    throw new CatalogBoundaryReached();
                }
            };
            ProductService target = new ProductService(null, tenant, null, null, null, sequences,
                null, null, new RequestTraceContext(), null, new TransactionTemplate(transactions));
            product = proxied(target, ProductService.class, transactions);
        }

        private void assertReleasedAndRolledBack() {
            assertThat(jdbc.queryForObject("select count(*) from proof_witness", Integer.class)).isZero();
            assertThat(pool.getHikariPoolMXBean().getActiveConnections()).isZero();
            assertThat(pool.getHikariPoolMXBean().getThreadsAwaitingConnection()).isZero();
        }

        @Override public void close() {
            factory.destroy();
            pool.close();
        }
    }

    private static final class RecordingTransactions extends JpaTransactionManager {
        private final HikariDataSource pool;
        private final CyclicBarrier barrier;
        private final ThreadLocal<Integer> begins = ThreadLocal.withInitial(() -> 0);
        private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
        private final AtomicInteger peakActive = new AtomicInteger();
        private final AtomicInteger peakDepth = new AtomicInteger();

        private RecordingTransactions(HikariDataSource pool, EntityManagerFactory factory, CyclicBarrier barrier) {
            super(factory);
            setDataSource(pool);
            this.pool = pool;
            this.barrier = barrier;
        }

        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            super.doBegin(transaction, definition);
            depth.set(depth.get() + 1);
            peakDepth.accumulateAndGet(depth.get(), Math::max);
            peakActive.accumulateAndGet(pool.getHikariPoolMXBean().getActiveConnections(), Math::max);
            begins.set(begins.get() + 1);
            if (barrier != null && begins.get() == 1) {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not align first connection acquisitions", exception);
                }
            }
        }

        @Override protected void doCleanupAfterCompletion(Object transaction) {
            try {
                super.doCleanupAfterCompletion(transaction);
            } finally {
                depth.set(depth.get() - 1);
            }
        }
    }
}
