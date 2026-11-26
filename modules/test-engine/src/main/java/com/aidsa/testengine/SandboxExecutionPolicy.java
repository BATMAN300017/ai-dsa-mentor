package com.aidsa.testengine;

/**
 * Isolation policy enforcing that student code is never executed inside the main backend JVM process.
 */
public record SandboxExecutionPolicy(
        boolean allowInProcessExecution,
        long timeoutMillis,
        int maxMemoryMb
) {
    public SandboxExecutionPolicy {
        if (allowInProcessExecution) {
            throw new IllegalArgumentException("Student code must never run inside the main backend process.");
        }
    }

    public static SandboxExecutionPolicy defaultIsolatedPolicy() {
        return new SandboxExecutionPolicy(false, 3000L, 256);
    }
}
