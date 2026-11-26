package com.aidsa.controller;

import com.aidsa.ai.InferenceParameters;
import com.aidsa.ai.LlamaCppInferenceProvider;
import com.aidsa.ai.LlamaRuntimeConfig;
import com.aidsa.ai.ProcessStarter;
import com.aidsa.modelmanager.ModelManagerService;
import com.aidsa.modelmanager.ModelMetadata;
import com.aidsa.security.LocalTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
class ModelAndAiControllerTest {

    @TempDir
    static Path tempDir;

    private static HttpServer mockUpstreamServer;
    private static byte[] mockGgufBytes;
    private static String mockGgufSha256;
    private static Path mockServerExe;

    @BeforeAll
    static void startMockServers() throws Exception {
        mockGgufBytes = "GGUF-spring-integration-test-model-bytes".getBytes(StandardCharsets.UTF_8);
        mockGgufSha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(mockGgufBytes));

        boolean isWin = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Files.createDirectories(tempDir.resolve("runtime"));
        mockServerExe = tempDir.resolve("runtime").resolve(isWin ? "llama-server.exe" : "llama-server");
        Files.writeString(mockServerExe, "stub");
        mockServerExe.toFile().setExecutable(true);

        mockUpstreamServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockUpstreamServer.createContext("/model.gguf", exchange -> {
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
            exchange.sendResponseHeaders(200, mockGgufBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(mockGgufBytes);
            }
        });
        mockUpstreamServer.createContext("/health", exchange -> {
            byte[] ok = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, ok.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(ok);
            }
        });
        mockUpstreamServer.createContext("/v1/chat/completions", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] resp = ("""
                    {
                      "model": "Qwen2.5-Coder-3B-Instruct",
                      "choices": [
                        {
                          "message": { "role": "assistant", "content": "Local mock response ready." },
                          "finish_reason": "stop"
                        }
                      ],
                      "usage": { "prompt_tokens": 12, "completion_tokens": 5 }
                    }
                    """).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });
        mockUpstreamServer.start();
    }

    @AfterAll
    static void stopMockServers() {
        if (mockUpstreamServer != null) {
            mockUpstreamServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void overridePaths(DynamicPropertyRegistry registry) {
        registry.add("aidsa.model.app-data-dir", () -> tempDir.resolve("models").toString());
        registry.add("aidsa.runtime.app-data-dir", () -> tempDir.resolve("runtime").toString());
        registry.add("aidsa.runtime.executable-path", () -> mockServerExe.toString());
    }

    @TestConfiguration
    static class MockIntegrationBeans {
        @Bean(destroyMethod = "close")
        @Primary
        public ModelManagerService testModelManagerService(ObjectMapper objectMapper) {
            int port = mockUpstreamServer.getAddress().getPort();
            ModelMetadata metadata = new ModelMetadata(
                    "Qwen2.5-Coder-3B-Instruct",
                    "Qwen (Alibaba Cloud)",
                    "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                    "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                    "Q4_K_M",
                    "GGUF",
                    "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
                    mockGgufBytes.length,
                    "NOT_DOWNLOADED",
                    "SHA-256",
                    mockGgufSha256,
                    "Mock test upstream SHA-256",
                    "",
                    "f74adce6aa16316c625447af059dbebe4983757c",
                    "http://127.0.0.1:" + port + "/model.gguf",
                    "llama.cpp"
            );
            return new ModelManagerService(
                    metadata,
                    tempDir.resolve("ws-models"),
                    tempDir.resolve("models"),
                    objectMapper,
                    HttpClient.newHttpClient(),
                    true
            );
        }

        @Bean(destroyMethod = "close")
        @Primary
        public LlamaCppInferenceProvider testInferenceProvider(ObjectMapper objectMapper) {
            int port = mockUpstreamServer.getAddress().getPort();
            LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                    mockServerExe.toString(),
                    tempDir.resolve("runtime"),
                    tempDir.resolve("ws-runtime"),
                    tempDir.resolve("logs"),
                    2048, 4, 0, port,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(5)
            );
            ProcessStarter fakeStarter = (cmd, wd, log) -> new StubProcess();
            return new LlamaCppInferenceProvider(config, fakeStarter, HttpClient.newHttpClient(), objectMapper);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void enforcesSecurityHandshakeAndRejectsUnauthorizedMutations() throws Exception {
        mockMvc.perform(get("/api/v1/security/handshake")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/security/handshake")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header(LocalTokenService.CLIENT_HEADER, LocalTokenService.EXPECTED_WEB_CLIENT)
                        .header("Sec-Fetch-Site", "cross-site"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/security/handshake")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header(LocalTokenService.CLIENT_HEADER, LocalTokenService.EXPECTED_WEB_CLIENT)
                        .header("Sec-Fetch-Mode", "navigate"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/model/download")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/ai/infer")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userPrompt\":\"hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void runsFullDownloadLoadInferAndUnloadWorkflow() throws Exception {
        String token = obtainUiToken();

        // 1. Initial status
        mockMvc.perform(get("/api/v1/model/status").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadStatus").value("NOT_DOWNLOADED"))
                .andExpect(jsonPath("$.modelLoaded").value(false))
                .andExpect(jsonPath("$.runtimeAvailable").value(true));

        // 2. Rejects load before download and infer before load
        mockMvc.perform(post("/api/v1/model/load")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/ai/infer")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userPrompt\":\"Test prompt\"}"))
                .andExpect(status().isServiceUnavailable());

        // 3. Rejects invalid request inputs
        mockMvc.perform(post("/api/v1/model/load")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextSize\":10}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/ai/infer")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userPrompt\":\"   \"}"))
                .andExpect(status().isBadRequest());

        // 4. Download model
        mockMvc.perform(post("/api/v1/model/download")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 200 || result.getResponse().getStatus() == 202));

        awaitStatusValue("$.downloadStatus", "VERIFIED");

        mockMvc.perform(get("/api/v1/model/status").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checksumVerificationStatus").value("VERIFIED_AGAINST_UPSTREAM"))
                .andExpect(jsonPath("$.localCalculatedChecksum").value(mockGgufSha256));

        // 5. Load model
        mockMvc.perform(post("/api/v1/model/load")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextSize\":2048,\"threads\":4,\"gpuLayers\":0}"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() == 200 || result.getResponse().getStatus() == 202));

        awaitStatusValue("$.modelLoaded", "true");

        mockMvc.perform(get("/api/v1/health").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelLoaded").value(true));

        // 6. Infer
        mockMvc.perform(post("/api/v1/ai/infer")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"systemPrompt\":\"You are concise.\",\"userPrompt\":\"Explain Two Sum\",\"maxTokens\":128,\"temperature\":0.2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value(containsString("Local mock response ready.")))
                .andExpect(jsonPath("$.promptTokens").value(12))
                .andExpect(jsonPath("$.completionTokens").value(5));

        // 7. Unload
        mockMvc.perform(post("/api/v1/model/unload")
                        .header("Host", "127.0.0.1:8787")
                        .header(LocalTokenService.TOKEN_HEADER, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelLoaded").value(false))
                .andExpect(jsonPath("$.runtimeStatus").value("MODEL_NOT_LOADED"));

        mockMvc.perform(get("/api/v1/health").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelLoaded").value(false));
    }

    private String obtainUiToken() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/security/handshake")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header(LocalTokenService.CLIENT_HEADER, LocalTokenService.EXPECTED_WEB_CLIENT))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("token").asText();
    }

    private void awaitStatusValue(String jsonPathExpr, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 10_000;
        String field = jsonPathExpr.replace("$.", "");
        while (System.currentTimeMillis() < deadline) {
            MvcResult res = mockMvc.perform(get("/api/v1/model/status").header("Host", "127.0.0.1:8787"))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
            if (expected.equals(node.path(field).asText())) {
                return;
            }
            Thread.sleep(50);
        }
        mockMvc.perform(get("/api/v1/model/status").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPathExpr).value(expected));
    }

    private static final class StubProcess extends Process {
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final CompletableFuture<Process> exitFuture = new CompletableFuture<>();

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
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, java.util.concurrent.TimeUnit unit) {
            return !alive.get();
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            if (alive.compareAndSet(true, false)) {
                exitFuture.complete(this);
            }
        }

        @Override
        public Process destroyForcibly() {
            destroy();
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }

        @Override
        public long pid() {
            return 99999L;
        }

        @Override
        public CompletableFuture<Process> onExit() {
            return exitFuture;
        }
    }
}
