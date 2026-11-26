package com.aidsa.testengine;

/**
 * Port for future out-of-process sandboxed test execution.
 */
public interface SandboxRunnerPort {

    SandboxExecutionPolicy policy();
}
