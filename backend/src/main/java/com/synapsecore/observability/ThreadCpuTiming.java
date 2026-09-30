package com.synapsecore.observability;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public final class ThreadCpuTiming {

    private static final ThreadMXBean THREADS = ManagementFactory.getThreadMXBean();

    private ThreadCpuTiming() {
    }

    public static long currentNanos() {
        return THREADS.isCurrentThreadCpuTimeSupported() && THREADS.isThreadCpuTimeEnabled()
            ? THREADS.getCurrentThreadCpuTime()
            : -1;
    }

    public static long elapsedMillis(long startedAtNanos) {
        long endedAtNanos = currentNanos();
        return startedAtNanos < 0 || endedAtNanos < startedAtNanos
            ? -1
            : (endedAtNanos - startedAtNanos) / 1_000_000;
    }
}
