package com.aidsa.testengine;

/**
 * Outcome status for isolated student test runs (planned for Task 2+).
 */
public enum ExecutionVerdict {
    NOT_EXECUTED,
    PASSED,
    FAILED,
    TIME_LIMIT_EXCEEDED,
    RUNTIME_ERROR
}
