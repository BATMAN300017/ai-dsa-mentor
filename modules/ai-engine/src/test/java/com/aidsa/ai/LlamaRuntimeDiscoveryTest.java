package com.aidsa.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlamaRuntimeDiscoveryTest {

    @Test
    void locatesLlamaServerInAppDataNestedSubfolderOnWindows(@TempDir Path tempDir) throws IOException {
        Path appDataRuntime = tempDir.resolve("appdata-runtime");
        Path nested = appDataRuntime.resolve("llama-b12760-bin-win-cpu-x64");
        Files.createDirectories(nested);
        Path exe = nested.resolve("llama-server.exe");
        Files.writeString(exe, "stub");

        LlamaRuntimeConfig config = LlamaRuntimeConfig.defaults(appDataRuntime, tempDir.resolve("ws"), tempDir.resolve("logs"));
        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config, "Windows 11", "");

        assertTrue(resolution.available());
        assertEquals("app-data", resolution.source());
        assertEquals(exe.toAbsolutePath().normalize(), resolution.executablePath());
    }

    @Test
    void rejectsArbitraryNonLlamaServerExecutablePath(@TempDir Path tempDir) throws IOException {
        Path badExe = tempDir.resolve("cmd.exe");
        Files.writeString(badExe, "stub");

        LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                badExe.toString(),
                tempDir.resolve("appdata"),
                tempDir.resolve("ws"),
                tempDir.resolve("logs"),
                4096, 4, 0, 0,
                Duration.ofSeconds(30), Duration.ofSeconds(60)
        );
        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config, "Windows 11", "");

        assertFalse(resolution.available());
        assertTrue(resolution.message().contains("llama-server.exe"));
    }

    @Test
    void locatesExecutableOnPosixSystemPath(@TempDir Path tempDir) throws IOException {
        Path binDir = tempDir.resolve("usr-bin");
        Files.createDirectories(binDir);
        Path exe = binDir.resolve("llama-server");
        Files.writeString(exe, "#!/bin/sh\n");
        assertTrue(exe.toFile().setExecutable(true));

        LlamaRuntimeConfig config = LlamaRuntimeConfig.defaults(tempDir.resolve("appdata"), tempDir.resolve("ws"), tempDir.resolve("logs"));
        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config, "Linux", binDir.toString());

        assertTrue(resolution.available());
        assertEquals("system-path", resolution.source());
        assertEquals(exe.toAbsolutePath().normalize(), resolution.executablePath());
    }
}
