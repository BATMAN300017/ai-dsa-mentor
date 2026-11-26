package com.aidsa.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlamaCppInferenceProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reportsMissingRuntimeAndRejectsInferenceBeforeLoad(@TempDir Path tempDir) {
        LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                "", tempDir.resolve("empty-rt"), tempDir.resolve("empty-ws"), tempDir.resolve("logs"),
                4096, 4, 0, 0, Duration.ofSeconds(5), Duration.ofSeconds(5)
        );
        try (LlamaCppInferenceProvider provider = new LlamaCppInferenceProvider(config, objectMapper)) {
            assertEquals(InferenceRuntimeStatus.RUNTIME_MISSING, provider.status());
            assertFalse(provider.isModelLoaded());
            assertThrows(ModelRuntimeNotConfiguredException.class,
                    () -> provider.generate(new InferenceRequest("sys", "hi", 64, 0.2)));
        }
    }

    @Test
    void managesFullLifecycleLoadInferStreamAndUnload(@TempDir Path tempDir) throws Exception {
        Path serverExe = createStubServerExe(tempDir);
        Path modelFile = createDummyModel(tempDir);
        AtomicInteger healthChecks = new AtomicInteger(0);
        AtomicReference<String> lastChatRequestBody = new AtomicReference<>();

        HttpServer mockLlama = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = mockLlama.getAddress().getPort();
        mockLlama.createContext("/health", exchange -> {
            int count = healthChecks.incrementAndGet();
            if (count == 1) {
                byte[] loading = "{\"error\":{\"code\":503,\"message\":\"Loading model\"}}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, loading.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(loading);
                }
            } else {
                byte[] ok = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, ok.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(ok);
                }
            }
        });
        mockLlama.createContext("/v1/chat/completions", exchange -> {
            String reqBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            lastChatRequestBody.set(reqBody);
            if (reqBody.contains("\"stream\":true")) {
                byte[] sse = (
                        "data: {\"model\":\"Qwen2.5-Coder-3B-Instruct\",\"choices\":[{\"delta\":{\"content\":\"Hello \"}}]}\n\n" +
                        "data: {\"model\":\"Qwen2.5-Coder-3B-Instruct\",\"choices\":[{\"delta\":{\"content\":\"World\"},\"finish_reason\":\"stop\"}]}\n\n" +
                        "data: [DONE]\n\n"
                ).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, sse.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(sse);
                }
            } else {
                byte[] json = """
                        {
                          "model": "Qwen2.5-Coder-3B-Instruct",
                          "choices": [
                            {
                              "message": { "role": "assistant", "content": "Binary search runs in O(log n) time." },
                              "finish_reason": "stop"
                            }
                          ],
                          "usage": { "prompt_tokens": 18, "completion_tokens": 9 }
                        }
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json);
                }
            }
        });
        mockLlama.start();

        List<String> capturedCommand = new ArrayList<>();
        FakeProcess fakeProcess = new FakeProcess(0);
        ProcessStarter starter = (cmd, workDir, logFile) -> {
            capturedCommand.addAll(cmd);
            return fakeProcess;
        };

        LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                serverExe.toString(),
                tempDir.resolve("rt"), tempDir.resolve("ws"), tempDir.resolve("logs"),
                2048, 6, 0, port, Duration.ofSeconds(10), Duration.ofSeconds(5)
        );

        try (LlamaCppInferenceProvider provider = new LlamaCppInferenceProvider(
                config, starter, HttpClient.newHttpClient(), objectMapper)) {

            assertEquals(InferenceRuntimeStatus.MODEL_NOT_LOADED, provider.status());

            LlamaRuntimeSnapshot loaded = provider.loadModelBlocking(
                    modelFile, "Qwen2.5-Coder-3B-Instruct", 2048, 6, 0);
            assertEquals(InferenceRuntimeStatus.READY, loaded.status());
            assertTrue(provider.isModelLoaded());
            assertTrue(capturedCommand.contains("--host"));
            assertEquals("127.0.0.1", capturedCommand.get(capturedCommand.indexOf("--host") + 1));
            assertEquals("0", capturedCommand.get(capturedCommand.indexOf("--gpu-layers") + 1));
            assertTrue(capturedCommand.contains("--no-webui"));

            InferenceDetailedResponse resp = provider.generateDetailed(
                    new InferenceRequest("Be concise.", "Explain binary search.", 128, 0.2),
                    InferenceParameters.defaults()
            );
            assertEquals("Binary search runs in O(log n) time.", resp.text());
            assertEquals(18, resp.promptTokens());
            assertEquals(9, resp.completionTokens());
            assertTrue(lastChatRequestBody.get().contains("Explain binary search."));

            List<String> streamedChunks = new ArrayList<>();
            InferenceDetailedResponse streamResp = provider.generateStream(
                    new InferenceRequest(null, "Say hello", 32, 0.1),
                    InferenceParameters.defaults(),
                    streamedChunks::add
            );
            assertEquals("Hello World", streamResp.text());
            assertEquals(List.of("Hello ", "World"), streamedChunks);

            LlamaRuntimeSnapshot unloaded = provider.unloadModel();
            assertEquals(InferenceRuntimeStatus.MODEL_NOT_LOADED, unloaded.status());
            assertFalse(provider.isModelLoaded());
            assertFalse(fakeProcess.isAlive());
        } finally {
            mockLlama.stop(0);
        }
    }

    @Test
    void handlesUnexpectedProcessExitAndStartupFailure(@TempDir Path tempDir) throws Exception {
        Path serverExe = createStubServerExe(tempDir);
        Path modelFile = createDummyModel(tempDir);

        // 1. Premature exit during startup
        FakeProcess crashingProcess = new FakeProcess(1);
        crashingProcess.crashImmediately(1);
        LlamaRuntimeConfig crashConfig = new LlamaRuntimeConfig(
                serverExe.toString(),
                tempDir.resolve("rt"), tempDir.resolve("ws"), tempDir.resolve("logs"),
                2048, 4, 0, 19999, Duration.ofSeconds(5), Duration.ofSeconds(2)
        );
        try (LlamaCppInferenceProvider provider = new LlamaCppInferenceProvider(
                crashConfig, (cmd, wd, log) -> crashingProcess, HttpClient.newHttpClient(), objectMapper)) {
            LlamaRuntimeSnapshot failedSnap = provider.loadModelBlocking(modelFile, "test", null, null, null);
            assertEquals(InferenceRuntimeStatus.ERROR, failedSnap.status());
            assertFalse(provider.isModelLoaded());
            assertTrue(failedSnap.lastError().contains("exit code 1"));
        }

        // 2. Unexpected crash after becoming READY
        HttpServer mockLlama = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = mockLlama.getAddress().getPort();
        mockLlama.createContext("/health", exchange -> {
            byte[] ok = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });
        mockLlama.start();

        FakeProcess liveThenCrash = new FakeProcess(137);
        LlamaRuntimeConfig liveConfig = new LlamaRuntimeConfig(
                serverExe.toString(),
                tempDir.resolve("rt"), tempDir.resolve("ws"), tempDir.resolve("logs"),
                2048, 4, 0, port, Duration.ofSeconds(5), Duration.ofSeconds(2)
        );
        try (LlamaCppInferenceProvider provider = new LlamaCppInferenceProvider(
                liveConfig, (cmd, wd, log) -> liveThenCrash, HttpClient.newHttpClient(), objectMapper)) {
            LlamaRuntimeSnapshot readySnap = provider.loadModelBlocking(modelFile, "test", null, null, null);
            assertEquals(InferenceRuntimeStatus.READY, readySnap.status());

            liveThenCrash.crashImmediately(137);
            LlamaRuntimeSnapshot afterCrash = provider.snapshot();
            assertEquals(InferenceRuntimeStatus.ERROR, afterCrash.status());
            assertFalse(afterCrash.modelLoaded());
            assertTrue(afterCrash.lastError().contains("137"));
        } finally {
            mockLlama.stop(0);
        }
    }

    @Test
    void surfacesRequestTimeoutCleanly(@TempDir Path tempDir) throws Exception {
        Path serverExe = createStubServerExe(tempDir);
        Path modelFile = createDummyModel(tempDir);

        HttpServer slowLlama = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = slowLlama.getAddress().getPort();
        slowLlama.createContext("/health", exchange -> {
            byte[] ok = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });
        slowLlama.createContext("/v1/chat/completions", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
        });
        slowLlama.start();

        FakeProcess fakeProcess = new FakeProcess(0);
        LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                serverExe.toString(),
                tempDir.resolve("rt"), tempDir.resolve("ws"), tempDir.resolve("logs"),
                2048, 4, 0, port, Duration.ofSeconds(5), Duration.ofMillis(250)
        );
        try (LlamaCppInferenceProvider provider = new LlamaCppInferenceProvider(
                config, (cmd, wd, log) -> fakeProcess, HttpClient.newHttpClient(), objectMapper)) {
            provider.loadModelBlocking(modelFile, "test", null, null, null);

            InferenceParameters shortTimeout = new InferenceParameters(
                    0.95, 1.05, List.of(), Duration.ofMillis(200)
            );
            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    provider.generateDetailed(new InferenceRequest(null, "test", 32, 0.1), shortTimeout));
            assertTrue(ex.getMessage().toLowerCase(Locale.ROOT).contains("timed out"));
        } finally {
            slowLlama.stop(0);
        }
    }

    private static Path createStubServerExe(Path dir) throws IOException {
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path exe = dir.resolve(win ? "llama-server.exe" : "llama-server");
        Files.writeString(exe, "stub");
        exe.toFile().setExecutable(true);
        return exe;
    }

    private static Path createDummyModel(Path dir) throws IOException {
        Path model = dir.resolve("dummy.gguf");
        Files.writeString(model, "GGUF-dummy");
        return model;
    }

    private static final class FakeProcess extends Process {
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final CompletableFuture<Process> exitFuture = new CompletableFuture<>();
        private volatile int exitCode;

        FakeProcess(int defaultExitCode) {
            this.exitCode = defaultExitCode;
        }

        void crashImmediately(int code) {
            this.exitCode = code;
            if (alive.compareAndSet(true, false)) {
                exitFuture.complete(this);
            }
        }

        @Override
        public OutputStream getOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            exitFuture.join();
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) {
            return !alive.get();
        }

        @Override
        public int exitValue() {
            if (alive.get()) {
                throw new IllegalThreadStateException("Process still alive");
            }
            return exitCode;
        }

        @Override
        public void destroy() {
            crashImmediately(0);
        }

        @Override
        public Process destroyForcibly() {
            crashImmediately(1);
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }

        @Override
        public long pid() {
            return 424242L;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            return exitFuture;
        }
    }
}
