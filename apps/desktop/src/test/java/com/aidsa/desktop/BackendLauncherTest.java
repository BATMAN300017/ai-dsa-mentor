package com.aidsa.desktop;

import com.aidsa.modelmanager.AppDataPathResolver;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackendLauncherTest {

    @Test
    void reusesExistingHealthyBackendWithoutSpawningDuplicateProcess() throws Exception {
        HttpServer stubServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = stubServer.getAddress().getPort();
        stubServer.createContext("/api/v1/health", exchange -> {
            byte[] payload = "{\"status\":\"UP\",\"application\":\"AI DSA Mentor\",\"version\":\"0.1.0\",\"modelLoaded\":false}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });
        stubServer.start();

        try {
            BackendLauncher launcher = new BackendLauncher(port, new AppDataPathResolver());
            BackendLauncher.LaunchResult result = launcher.ensureRunning(Duration.ofSeconds(3));

            assertTrue(result.ready());
            assertFalse(result.startedNewProcess());
            assertFalse(launcher.isOwnedByThisLauncher());
            assertEquals("http://127.0.0.1:" + port, result.baseUrl());
        } finally {
            stubServer.stop(0);
        }
    }

    @Test
    void detectsOccupiedNonMentorPortWithoutThrowing() throws IOException, InterruptedException {
        HttpServer nonMentorServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = nonMentorServer.getAddress().getPort();
        nonMentorServer.createContext("/api/v1/health", exchange -> {
            byte[] payload = "{\"status\":\"UP\",\"application\":\"OtherApp\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });
        nonMentorServer.start();

        try {
            BackendLauncher launcher = new BackendLauncher(port, new AppDataPathResolver());
            BackendLauncher.LaunchResult result = launcher.ensureRunning(Duration.ofSeconds(2));

            assertFalse(result.ready());
            assertFalse(result.startedNewProcess());
            assertTrue(result.message().contains("occupied"));
        } finally {
            nonMentorServer.stop(0);
        }
    }
}
