package com.aidsa.testengine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SandboxExecutionPolicyTest {

    @Test
    void rejectsInProcessExecutionPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new SandboxExecutionPolicy(true, 1000L, 128));
        assertFalse(SandboxExecutionPolicy.defaultIsolatedPolicy().allowInProcessExecution());
    }
}
