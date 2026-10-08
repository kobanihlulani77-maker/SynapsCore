package com.synapsecore.observability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class SlowThreadWaitSampler implements AutoCloseable {

    private static final int MAX_SAMPLES = 60;
    private static final ScheduledThreadPoolExecutor SAMPLER = createSampler();

    private final Thread observedThread;
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private final Map<String, String> representativeFrames = new LinkedHashMap<>();
    private final ScheduledFuture<?> task;
    private int samples;
    private boolean closed;

    private SlowThreadWaitSampler(Thread observedThread) {
        this.observedThread = observedThread;
        this.task = SAMPLER.scheduleAtFixedRate(this::sample, 1, 1, TimeUnit.SECONDS);
    }

    public static SlowThreadWaitSampler start() {
        return new SlowThreadWaitSampler(Thread.currentThread());
    }

    public synchronized Summary summary() {
        return new Summary(samples,
            Collections.unmodifiableMap(new LinkedHashMap<>(counts)),
            Collections.unmodifiableMap(new LinkedHashMap<>(representativeFrames)));
    }

    @Override
    public synchronized void close() {
        closed = true;
        task.cancel(false);
    }

    private synchronized void sample() {
        if (closed || samples >= MAX_SAMPLES || !observedThread.isAlive()) {
            task.cancel(false);
            return;
        }
        try {
            Thread.State state = observedThread.getState();
            StackTraceElement[] frames = observedThread.getStackTrace();
            String category = classify(state, frames);
            samples++;
            counts.merge(category, 1, Integer::sum);
            representativeFrames.putIfAbsent(category, representativeFrame(category, frames));
        } catch (RuntimeException ignored) {
            // A diagnostic sample must not interfere with request processing.
        }
    }

    static String classify(Thread.State state, StackTraceElement[] frames) {
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if ((className.equals("com.zaxxer.hikari.HikariDataSource")
                    || className.equals("com.zaxxer.hikari.pool.HikariPool"))
                && frame.getMethodName().equals("getConnection")) {
                return "HIKARI_ACQUIRE";
            }
            if (className.equals("com.zaxxer.hikari.util.ConcurrentBag")
                && frame.getMethodName().equals("borrow")) {
                return "HIKARI_ACQUIRE";
            }
        }
        for (StackTraceElement frame : frames) {
            String className = frame.getClassName();
            if (className.startsWith("org.postgresql.")) {
                return "POSTGRES_JDBC";
            }
            if (className.startsWith("io.lettuce.")) {
                return "REDIS_CLIENT";
            }
        }
        if (state == Thread.State.BLOCKED) {
            return "JAVA_MONITOR";
        }
        if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
            return "JAVA_WAIT";
        }
        return "JAVA_ACTIVE_OR_OTHER";
    }

    static String representativeFrame(String category, StackTraceElement[] frames) {
        String prefix = switch (category) {
            case "HIKARI_ACQUIRE" -> "com.zaxxer.hikari.pool.HikariPool";
            case "POSTGRES_JDBC" -> "org.postgresql.";
            case "REDIS_CLIENT" -> "io.lettuce.";
            default -> "com.synapsecore.";
        };
        String libraryFrame = null;
        String applicationFrame = null;
        for (StackTraceElement frame : frames) {
            String name = frame.getClassName() + "#" + frame.getMethodName();
            if (libraryFrame == null && frame.getClassName().startsWith(prefix)) {
                libraryFrame = name;
            }
            if (applicationFrame == null && frame.getClassName().startsWith("com.synapsecore.")) {
                applicationFrame = name;
            }
        }
        if (libraryFrame != null && applicationFrame != null && !libraryFrame.equals(applicationFrame)) {
            return libraryFrame + " via " + applicationFrame;
        }
        if (libraryFrame != null) {
            return libraryFrame;
        }
        return frames.length == 0 ? "<no-stack>"
            : frames[0].getClassName() + "#" + frames[0].getMethodName();
    }

    private static ScheduledThreadPoolExecutor createSampler() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "SynapseSlowSnapshotSampler");
            thread.setDaemon(true);
            return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }

    public record Summary(int samples, Map<String, Integer> categories,
                          Map<String, String> representativeFrames) {
    }
}
