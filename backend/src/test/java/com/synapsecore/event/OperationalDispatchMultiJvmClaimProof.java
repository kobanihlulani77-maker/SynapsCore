package com.synapsecore.event;

import com.synapsecore.SynapseCoreApplication;
import com.synapsecore.domain.entity.OperationalDispatchStatus;
import com.synapsecore.domain.entity.OperationalDispatchWorkItem;
import com.synapsecore.domain.repository.OperationalDispatchWorkItemRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class OperationalDispatchMultiJvmClaimProof {

    private OperationalDispatchMultiJvmClaimProof() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4
            || !List.of("seed", "claim", "verify").contains(args[0])
            || !args[1].matches("[A-Za-z0-9-]{1,32}")
            || ("claim".equals(args[0]) && (args.length != 4 || !List.of("one", "two").contains(args[3])))
            || (!"claim".equals(args[0]) && args.length != 3)) {
            throw new IllegalArgumentException("Expected seed|claim|verify, proof key, directory, and worker for claim.");
        }
        Path proofDirectory = Path.of(args[2]);
        String key = args[1];
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(SynapseCoreApplication.class)
            .profiles("test")
            .web(WebApplicationType.NONE)
            .run()) {
            OperationalDispatchWorkItemRepository repository = context.getBean(OperationalDispatchWorkItemRepository.class);
            switch (args[0]) {
                case "seed" -> seed(repository, key, proofDirectory);
                case "claim" -> claim(repository, key, proofDirectory, args[3]);
                case "verify" -> verify(repository, key, proofDirectory);
                default -> throw new IllegalStateException("Invalid proof mode");
            }
        }
    }

    private static void seed(OperationalDispatchWorkItemRepository repository, String key, Path directory)
        throws Exception {
        OperationalDispatchWorkItem item = repository.saveAndFlush(OperationalDispatchWorkItem.builder()
            .tenantCode("DISPATCH-TWO-JVM-" + key)
            .updateType(OperationalUpdateType.INTEGRATION_STATE)
            .source("postgres-two-jvm-claim-proof")
            .requestId(UUID.randomUUID().toString())
            .status(OperationalDispatchStatus.PENDING)
            .occurredAt(Instant.now())
            .build());
        Files.writeString(file(directory, key, "id"), item.getId().toString(), StandardOpenOption.CREATE_NEW);
    }

    private static void claim(OperationalDispatchWorkItemRepository repository, String key, Path directory,
                              String worker) throws Exception {
        Long id = Long.valueOf(Files.readString(file(directory, key, "id")).trim());
        Files.writeString(file(directory, key, worker + ".ready"), Long.toString(ProcessHandle.current().pid()),
            StandardOpenOption.CREATE_NEW);
        Path release = file(directory, key, "release");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (!Files.exists(release) && System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(50);
        }
        if (!Files.exists(release)) {
            throw new IllegalStateException("Two-JVM claim release was not signalled");
        }
        Instant now = Instant.now();
        int claimed = repository.claimForDispatch(id, 0, OperationalDispatchStatus.PENDING,
            OperationalDispatchStatus.PROCESSING, now.minusSeconds(300), now);
        if (claimed != 0 && claimed != 1) {
            throw new IllegalStateException("Unexpected claim count " + claimed);
        }
        Files.writeString(file(directory, key, worker + ".result"), Integer.toString(claimed),
            StandardOpenOption.CREATE_NEW);
    }

    private static void verify(OperationalDispatchWorkItemRepository repository, String key, Path directory)
        throws Exception {
        long firstPid = Long.parseLong(Files.readString(file(directory, key, "one.ready")).trim());
        long secondPid = Long.parseLong(Files.readString(file(directory, key, "two.ready")).trim());
        if (firstPid == secondPid || firstPid == ProcessHandle.current().pid()
            || secondPid == ProcessHandle.current().pid()) {
            throw new IllegalStateException("Claim workers must be distinct from each other and the verifier JVM");
        }
        int first = Integer.parseInt(Files.readString(file(directory, key, "one.result")).trim());
        int second = Integer.parseInt(Files.readString(file(directory, key, "two.result")).trim());
        if (first + second != 1 || Math.min(first, second) != 0 || Math.max(first, second) != 1) {
            throw new IllegalStateException("Exactly one independent JVM must win the shared claim");
        }
        Long id = Long.valueOf(Files.readString(file(directory, key, "id")).trim());
        OperationalDispatchWorkItem item = repository.findById(id).orElseThrow();
        if (item.getStatus() != OperationalDispatchStatus.PROCESSING || item.getAttemptCount() != 1
            || item.getProcessedAt() != null) {
            throw new IllegalStateException("Shared dispatch item has an unexpected state after the race");
        }
        if (repository.completeDispatch(id, 1, OperationalDispatchStatus.PROCESSING,
            OperationalDispatchStatus.COMPLETED, Instant.now()) != 1) {
            throw new IllegalStateException("Winning claim could not be completed");
        }
        repository.deleteById(id);
        System.out.println("Two independent JVMs produced one committed PostgreSQL claim and one completion.");
    }

    private static Path file(Path directory, String key, String suffix) {
        return directory.resolve("dispatch-two-jvm-" + key + "." + suffix);
    }
}
