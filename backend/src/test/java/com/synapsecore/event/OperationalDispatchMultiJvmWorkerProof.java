package com.synapsecore.event;

import com.synapsecore.SynapseCoreApplication;
import com.synapsecore.audit.RequestTraceContext;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import com.synapsecore.domain.service.DashboardService;
import com.synapsecore.observability.OperationalMetricsService;
import com.synapsecore.observability.ScheduledTaskExecutionDiagnostics;
import com.synapsecore.realtime.RealtimeService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class OperationalDispatchMultiJvmWorkerProof {

    private OperationalDispatchMultiJvmWorkerProof() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4
            || !List.of("seed", "drain", "verify").contains(args[0])
            || !args[1].matches("[A-Za-z0-9-]{1,32}")
            || ("drain".equals(args[0]) && (args.length != 4 || !List.of("one", "two").contains(args[3])))
            || (!"drain".equals(args[0]) && args.length != 3)) {
            throw new IllegalArgumentException("Expected seed|drain|verify, proof key, directory, and worker for drain.");
        }
        Path directory = Path.of(args[2]);
        String key = args[1];
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SynapseCoreApplication.class)
            .profiles("test")
            .web(WebApplicationType.NONE)
            .run()) {
            OperationalDispatchWorkItemRepository repository = context.getBean(OperationalDispatchWorkItemRepository.class);
            switch (args[0]) {
                case "seed" -> seed(repository, key, directory);
                case "drain" -> drain(context, repository, key, directory, args[3]);
                case "verify" -> verify(repository, key, directory);
                default -> throw new IllegalStateException("Invalid proof mode");
            }
        }
    }

    private static void seed(OperationalDispatchWorkItemRepository repository, String key, Path directory)
        throws Exception {
        OperationalDispatchWorkItem item = repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode("DISPATCH-WORKER-" + key)
            .updateType(OperationalUpdateType.INTEGRATION_STATE)
            .source("postgres-two-jvm-worker-proof")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(Instant.now())
            .build());
        Files.writeString(file(directory, key, "id"), item.getId().toString(), StandardOpenOption.CREATE_NEW);
    }

    private static void drain(ConfigurableApplicationContext context, OperationalDispatchWorkItemRepository repository,
                              String key, Path directory, String worker) throws Exception {
        DefaultListableBeanFactory realtimeBeans = new DefaultListableBeanFactory();
        realtimeBeans.registerSingleton("realtime", new RecordingRealtimeService(
            "DISPATCH-WORKER-" + key, file(directory, key, worker + ".broadcast")));
        OperationalDispatchQueueService service = new OperationalDispatchQueueService(
            repository,
            new DefaultListableBeanFactory().getBeanProvider(DashboardService.class),
            realtimeBeans.getBeanProvider(RealtimeService.class),
            new RequestTraceContext(),
            context.getBean(OperationalMetricsService.class),
            null,
            new ScheduledTaskExecutionDiagnostics(null)
        );
        Files.writeString(file(directory, key, worker + ".ready"), Long.toString(ProcessHandle.current().pid()),
            StandardOpenOption.CREATE_NEW);
        Path release = file(directory, key, "release");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (!Files.exists(release) && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        if (!Files.exists(release)) {
            throw new IllegalStateException("Two-JVM worker release was not signalled");
        }
        int processed = service.processPendingWork();
        Files.writeString(file(directory, key, worker + ".result"), Integer.toString(processed),
            StandardOpenOption.CREATE_NEW);
    }

    private static void verify(OperationalDispatchWorkItemRepository repository, String key, Path directory)
        throws Exception {
        long firstPid = Long.parseLong(Files.readString(file(directory, key, "one.ready")).trim());
        long secondPid = Long.parseLong(Files.readString(file(directory, key, "two.ready")).trim());
        if (firstPid == secondPid) {
            throw new IllegalStateException("Dispatch workers must be distinct JVM processes");
        }
        int first = Integer.parseInt(Files.readString(file(directory, key, "one.result")).trim());
        int second = Integer.parseInt(Files.readString(file(directory, key, "two.result")).trim());
        int firstBroadcast = Files.exists(file(directory, key, "one.broadcast")) ? 1 : 0;
        int secondBroadcast = Files.exists(file(directory, key, "two.broadcast")) ? 1 : 0;
        Long id = Long.valueOf(Files.readString(file(directory, key, "id")).trim());
        OperationalDispatchWorkItem item = repository.findById(id).orElseThrow();
        if (first + second != 1 || Math.min(first, second) != 0 || Math.max(first, second) != 1
            || firstBroadcast != first || secondBroadcast != second) {
            throw new IllegalStateException("Expected one worker completion and one test-recorded publication; "
                + "results=" + first + "," + second + " markers=" + firstBroadcast + "," + secondBroadcast
                + " durableStatus=" + item.getStatus() + " durableAttempt=" + item.getAttemptCount());
        }
        if (item.getStatus() != OperationalDispatchStatus.COMPLETED || item.getAttemptCount() != 1
            || item.getProcessedAt() == null) {
            throw new IllegalStateException("Shared dispatch item did not complete exactly one attempt");
        }
        repository.deleteById(id);
        System.out.println("Two independent queue workers produced one PostgreSQL completion and one "
            + "test-recorded publication; results=" + first + "," + second
            + " markers=" + firstBroadcast + "," + secondBroadcast + ".");
    }

    private static Path file(Path directory, String key, String suffix) {
        return directory.resolve("dispatch-two-worker-" + key + "." + suffix);
    }

    private static final class RecordingRealtimeService extends RealtimeService {
        private final String expectedTenant;
        private final Path broadcastMarker;

        private RecordingRealtimeService(String expectedTenant, Path broadcastMarker) {
            super(null, null, null, null, null);
            this.expectedTenant = expectedTenant;
            this.broadcastMarker = broadcastMarker;
        }

        @Override
        public void broadcastIntegrationUpdates(String tenantCode) {
            if (!expectedTenant.equals(tenantCode)) {
                throw new IllegalStateException("Dispatch publication crossed the test tenant boundary");
            }
            try {
                Files.writeString(broadcastMarker, tenantCode, StandardOpenOption.CREATE_NEW);
            } catch (java.io.IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        }
    }
}
