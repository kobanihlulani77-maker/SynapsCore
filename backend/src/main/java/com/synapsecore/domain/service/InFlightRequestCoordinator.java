package com.synapsecore.domain.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Coalesces identical concurrent reads without retaining completed results. */
final class InFlightRequestCoordinator<T> {

    private final ConcurrentHashMap<String, CompletableFuture<T>> inFlight = new ConcurrentHashMap<>();

    T execute(String key, Supplier<T> supplier) {
        CompletableFuture<T> created = new CompletableFuture<>();
        CompletableFuture<T> existing = inFlight.putIfAbsent(key, created);
        if (existing != null) {
            return await(existing);
        }

        try {
            T result = supplier.get();
            created.complete(result);
            return result;
        } catch (RuntimeException | Error exception) {
            created.completeExceptionally(exception);
            throw exception;
        } finally {
            inFlight.remove(key, created);
        }
    }

    private T await(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }
}
