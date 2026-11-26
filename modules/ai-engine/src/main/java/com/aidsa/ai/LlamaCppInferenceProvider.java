package com.aidsa.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Local {@link InferenceProvider} backed by a loopback-only {@code llama-server} subprocess.
 * Manages process lifecycle, readiness polling, unexpected exit handling, model loading/unloading,
 * and OpenAI-compatible {@code /v1/chat/completions} inference requests.
 */
public class LlamaCppInferenceProvider implements InferenceProvider, AutoCloseable {

    private static final String LOOPBACK_HOST = "127.0.0.1";
    private static final long HEALTH_POLL_INTERVAL_MS = 250L;

    private final LlamaRuntimeConfig config;
    private final ProcessStarter processStarter;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final Object lifecycleLock = new Object();

    private volatile Process managedProcess;
    private volatile InferenceRuntimeStatus currentStatus = InferenceRuntimeStatus.UNINITIALIZED;
    private volatile int activePort;
    private volatile int activeContextSize;
    private volatile int activeThreads;
    private volatile int activeGpuLayers;
    private volatile String loadedModelName;
    private volatile Path loadedModelPath;
    private volatile String lastError;
    private volatile boolean shuttingDownIntentionally;
    private volatile CompletableFuture<LlamaRuntimeSnapshot> activeLoadFuture;

    public LlamaCppInferenceProvider(LlamaRuntimeConfig config, ObjectMapper objectMapper) {
        this(
                config,
                ProcessStarter.systemDefault(),
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                objectMapper
        );
    }

    public LlamaCppInferenceProvider(
            LlamaRuntimeConfig config,
            ProcessStarter processStarter,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.config = config;
        this.processStarter = processStarter;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
        refreshBaselineStatus();
    }

    @Override
    public InferenceRuntimeStatus status() {
        return snapshot().status();
    }

    @Override
    public boolean isModelLoaded() {
        return status() == InferenceRuntimeStatus.READY;
    }

    public LlamaRuntimeSnapshot snapshot() {
        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config);
        Process proc = managedProcess;

        if (proc != null && !proc.isAlive() && !shuttingDownIntentionally
                && currentStatus == InferenceRuntimeStatus.READY) {
            recordUnexpectedExit(proc.exitValue());
        }

        InferenceRuntimeStatus effectiveStatus = currentStatus;
        if (proc == null && effectiveStatus != InferenceRuntimeStatus.STARTING
                && effectiveStatus != InferenceRuntimeStatus.ERROR) {
            effectiveStatus = resolution.available()
                    ? InferenceRuntimeStatus.MODEL_NOT_LOADED
                    : InferenceRuntimeStatus.RUNTIME_MISSING;
        }

        boolean loaded = effectiveStatus == InferenceRuntimeStatus.READY && proc != null && proc.isAlive();
        boolean loading = effectiveStatus == InferenceRuntimeStatus.STARTING;

        return new LlamaRuntimeSnapshot(
                effectiveStatus,
                resolution.available(),
                resolution.source(),
                resolution.executablePath() != null ? resolution.executablePath().getFileName().toString() : null,
                loaded,
                loading,
                activePort,
                activeContextSize > 0 ? activeContextSize : config.defaultContextSize(),
                activeThreads > 0 ? activeThreads : config.defaultThreads(),
                activeGpuLayers,
                loaded ? loadedModelName : null,
                lastError,
                proc != null && proc.isAlive() ? safePid(proc) : null
        );
    }

    public CompletableFuture<LlamaRuntimeSnapshot> loadModelAsync(
            Path modelFilePath,
            String modelName,
            Integer contextSizeOverride,
            Integer threadsOverride,
            Integer gpuLayersOverride
    ) {
        if (modelFilePath == null || !Files.isRegularFile(modelFilePath)) {
            throw new ModelRuntimeNotConfiguredException("GGUF model file is not installed on disk.");
        }

        int ctxSize = validateRange(
                contextSizeOverride != null ? contextSizeOverride : config.defaultContextSize(),
                512, 32768, "contextSize"
        );
        int threads = validateRange(
                threadsOverride != null ? threadsOverride : config.defaultThreads(),
                1, 64, "threads"
        );
        int gpuLayers = validateRange(
                gpuLayersOverride != null ? gpuLayersOverride : config.defaultGpuLayers(),
                0, 128, "gpuLayers"
        );

        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config);
        if (!resolution.available()) {
            this.currentStatus = InferenceRuntimeStatus.RUNTIME_MISSING;
            this.lastError = resolution.message();
            throw new ModelRuntimeNotConfiguredException(resolution.message());
        }

        synchronized (lifecycleLock) {
            if (activeLoadFuture != null && !activeLoadFuture.isDone()) {
                return activeLoadFuture;
            }
            if (isModelLoaded()
                    && modelFilePath.equals(loadedModelPath)
                    && ctxSize == activeContextSize
                    && threads == activeThreads
                    && gpuLayers == activeGpuLayers) {
                return CompletableFuture.completedFuture(snapshot());
            }

            if (managedProcess != null) {
                stopManagedProcessLocked();
            }

            this.lastError = null;
            this.shuttingDownIntentionally = false;
            this.currentStatus = InferenceRuntimeStatus.STARTING;
            this.activeContextSize = ctxSize;
            this.activeThreads = threads;
            this.activeGpuLayers = gpuLayers;
            this.loadedModelName = (modelName == null || modelName.isBlank())
                    ? modelFilePath.getFileName().toString()
                    : modelName.trim();
            this.loadedModelPath = modelFilePath;

            activeLoadFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return startAndWaitReady(resolution.executablePath(), modelFilePath, ctxSize, threads, gpuLayers);
                } catch (Exception ex) {
                    synchronized (lifecycleLock) {
                        stopManagedProcessLocked();
                        this.currentStatus = InferenceRuntimeStatus.ERROR;
                        this.lastError = ex.getMessage();
                    }
                    return snapshot();
                }
            });
            return activeLoadFuture;
        }
    }

    public LlamaRuntimeSnapshot loadModelBlocking(
            Path modelFilePath,
            String modelName,
            Integer contextSizeOverride,
            Integer threadsOverride,
            Integer gpuLayersOverride
    ) {
        return loadModelAsync(modelFilePath, modelName, contextSizeOverride, threadsOverride, gpuLayersOverride).join();
    }

    public LlamaRuntimeSnapshot unloadModel() {
        synchronized (lifecycleLock) {
            if (managedProcess == null && currentStatus != InferenceRuntimeStatus.STARTING) {
                refreshBaselineStatus();
                this.lastError = null;
                return snapshot();
            }
            this.currentStatus = InferenceRuntimeStatus.UNLOADING;
            stopManagedProcessLocked();
            this.lastError = null;
            refreshBaselineStatus();
            return snapshot();
        }
    }

    @Override
    public InferenceResponse generate(InferenceRequest request) {
        return generateDetailed(request, InferenceParameters.defaults()).toInferenceResponse();
    }

    public InferenceDetailedResponse generateDetailed(InferenceRequest request, InferenceParameters parameters) {
        if (request == null || request.userPrompt() == null || request.userPrompt().isBlank()) {
            throw new IllegalArgumentException("InferenceRequest userPrompt must not be blank.");
        }
        InferenceParameters params = parameters == null ? InferenceParameters.defaults() : parameters;
        LlamaRuntimeSnapshot snap = snapshot();
        if (!snap.modelLoaded() || activePort <= 0) {
            String reason = snap.lastError() != null && !snap.lastError().isBlank()
                    ? snap.lastError()
                    : switch (snap.status()) {
                case RUNTIME_MISSING -> "llama.cpp runtime (llama-server) is not installed or configured.";
                case STARTING -> "Model is still loading into local memory. Please wait until status is READY.";
                case ERROR -> "Local inference runtime encountered an error: " + snap.lastError();
                default -> "No local GGUF model is loaded. Download and load Qwen2.5-Coder-3B-Instruct first.";
            };
            throw new ModelRuntimeNotConfiguredException(reason);
        }

        int requestedMaxTokens = request.maxTokens() == 0 ? 256 : request.maxTokens();
        int maxTokens = validateRange(requestedMaxTokens, 1, 4096, "maxTokens");
        double temperature = validateRangeDouble(request.temperature(), 0.0, 2.0, "temperature");

        long startNanos = System.nanoTime();
        try {
            String requestBody = buildChatCompletionsPayload(request, maxTokens, temperature, params, false);
            Duration effectiveTimeout = params.requestTimeout() != null
                    ? params.requestTimeout()
                    : config.requestTimeout();

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activePort + "/v1/chat/completions"))
                    .timeout(effectiveTimeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "llama-server returned HTTP " + response.statusCode() + ": " + truncate(response.body(), 300));
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("llama-server response did not include any choices.");
            }
            JsonNode firstChoice = choices.get(0);
            String text = firstChoice.path("message").path("content").asText("");
            String finishReason = firstChoice.path("finish_reason").asText(null);
            String model = root.path("model").asText(loadedModelName);
            Integer promptTokens = root.path("usage").hasNonNull("prompt_tokens")
                    ? root.path("usage").get("prompt_tokens").asInt()
                    : null;
            Integer completionTokens = root.path("usage").hasNonNull("completion_tokens")
                    ? root.path("usage").get("completion_tokens").asInt()
                    : null;

            long latencyMs = Math.max(1L, (System.nanoTime() - startNanos) / 1_000_000L);
            return new InferenceDetailedResponse(text, model, latencyMs, promptTokens, completionTokens, finishReason);
        } catch (HttpTimeoutException ex) {
            throw new IllegalStateException("Local inference request timed out after " + params.requestTimeout().toSeconds() + "s.", ex);
        } catch (IOException ex) {
            if (managedProcess != null && !managedProcess.isAlive()) {
                recordUnexpectedExit(managedProcess.exitValue());
            }
            throw new IllegalStateException("Failed to communicate with local llama-server: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Local inference request was interrupted.", ex);
        }
    }

    /**
     * Streams generated text chunks from {@code llama-server} using SSE {@code /v1/chat/completions}.
     */
    public InferenceDetailedResponse generateStream(
            InferenceRequest request,
            InferenceParameters parameters,
            Consumer<String> tokenConsumer
    ) {
        if (request == null || request.userPrompt() == null || request.userPrompt().isBlank()) {
            throw new IllegalArgumentException("InferenceRequest userPrompt must not be blank.");
        }
        InferenceParameters params = parameters == null ? InferenceParameters.defaults() : parameters;
        LlamaRuntimeSnapshot snap = snapshot();
        if (!snap.modelLoaded() || activePort <= 0) {
            throw new ModelRuntimeNotConfiguredException("Local model is not loaded and ready.");
        }

        int requestedMaxTokens = request.maxTokens() == 0 ? 256 : request.maxTokens();
        int maxTokens = validateRange(requestedMaxTokens, 1, 4096, "maxTokens");
        double temperature = validateRangeDouble(request.temperature(), 0.0, 2.0, "temperature");

        long startNanos = System.nanoTime();
        StringBuilder fullText = new StringBuilder();
        String finishReason = null;
        String model = loadedModelName;

        try {
            String requestBody = buildChatCompletionsPayload(request, maxTokens, temperature, params, true);
            Duration effectiveTimeout = params.requestTimeout() != null
                    ? params.requestTimeout()
                    : config.requestTimeout();

            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + activePort + "/v1/chat/completions"))
                    .timeout(effectiveTimeout)
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(httpReq, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new IllegalStateException("llama-server streaming returned HTTP " + response.statusCode() + ": " + truncate(errBody, 300));
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if (data.isEmpty() || "[DONE]".equals(data)) {
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        continue;
                    }
                    JsonNode chunk = objectMapper.readTree(data);
                    if (chunk.hasNonNull("model")) {
                        model = chunk.get("model").asText(model);
                    }
                    JsonNode choices = chunk.path("choices");
                    if (choices.isArray() && !choices.isEmpty()) {
                        JsonNode choice = choices.get(0);
                        String delta = choice.path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            fullText.append(delta);
                            if (tokenConsumer != null) {
                                tokenConsumer.accept(delta);
                            }
                        }
                        if (choice.hasNonNull("finish_reason")) {
                            finishReason = choice.get("finish_reason").asText(finishReason);
                        }
                    }
                }
            }

            long latencyMs = Math.max(1L, (System.nanoTime() - startNanos) / 1_000_000L);
            return new InferenceDetailedResponse(fullText.toString(), model, latencyMs, null, null, finishReason);
        } catch (HttpTimeoutException ex) {
            throw new IllegalStateException("Local streaming inference timed out.", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Streaming inference failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Streaming inference interrupted.", ex);
        }
    }

    private LlamaRuntimeSnapshot startAndWaitReady(
            Path executablePath,
            Path modelFilePath,
            int ctxSize,
            int threads,
            int gpuLayers
    ) throws IOException, InterruptedException {
        int port = config.preferredPort() > 0 ? config.preferredPort() : allocateLoopbackPort();
        Path logFile = config.logsDir() != null ? config.logsDir().resolve("llama-server.log") : null;

        List<String> command = new ArrayList<>();
        command.add(executablePath.toAbsolutePath().toString());
        command.add("--model");
        command.add(modelFilePath.toAbsolutePath().toString());
        command.add("--alias");
        command.add(sanitizeAlias(loadedModelName));
        command.add("--host");
        command.add(LOOPBACK_HOST);
        command.add("--port");
        command.add(Integer.toString(port));
        command.add("--ctx-size");
        command.add(Integer.toString(ctxSize));
        command.add("--threads");
        command.add(Integer.toString(threads));
        command.add("--gpu-layers");
        command.add(Integer.toString(gpuLayers));
        command.add("--no-webui");

        Process proc = processStarter.start(command, executablePath.getParent(), logFile);
        synchronized (lifecycleLock) {
            this.managedProcess = proc;
            this.activePort = port;
        }

        proc.onExit().thenAccept(exited -> {
            if (!shuttingDownIntentionally && managedProcess == exited) {
                recordUnexpectedExit(exited.exitValue());
            }
        });

        long deadlineNanos = System.nanoTime() + config.startupTimeout().toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (shuttingDownIntentionally) {
                return snapshot();
            }
            if (!proc.isAlive()) {
                int code = proc.exitValue();
                String tail = readLogTail(logFile);
                throw new IllegalStateException("llama-server exited prematurely during startup (exit code " + code + ")"
                        + (tail.isBlank() ? "." : ": " + tail));
            }
            if (probeHealth(port)) {
                synchronized (lifecycleLock) {
                    if (managedProcess == proc && proc.isAlive() && !shuttingDownIntentionally) {
                        this.currentStatus = InferenceRuntimeStatus.READY;
                        this.lastError = null;
                    }
                }
                return snapshot();
            }
            Thread.sleep(HEALTH_POLL_INTERVAL_MS);
        }

        String tail = readLogTail(logFile);
        throw new IllegalStateException("Timed out waiting " + config.startupTimeout().toSeconds()
                + "s for llama-server readiness on 127.0.0.1:" + port
                + (tail.isBlank() ? "." : ". Log tail: " + tail));
    }

    private boolean probeHealth(int port) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + LOOPBACK_HOST + ":" + port + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return resp.statusCode() == 200;
        } catch (Exception ex) {
            return false;
        }
    }

    private String buildChatCompletionsPayload(
            InferenceRequest request,
            int maxTokens,
            double temperature,
            InferenceParameters params,
            boolean stream
    ) throws IOException {
        List<Map<String, String>> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.userPrompt()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", sanitizeAlias(loadedModelName));
        body.put("messages", messages);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("top_p", params.topP());
        body.put("repeat_penalty", params.repeatPenalty());
        body.put("stream", stream);
        if (!params.stopSequences().isEmpty()) {
            body.put("stop", params.stopSequences());
        }
        return objectMapper.writeValueAsString(body);
    }

    private void stopManagedProcessLocked() {
        Process proc = this.managedProcess;
        this.shuttingDownIntentionally = true;
        this.managedProcess = null;
        this.activePort = 0;
        this.loadedModelPath = null;
        this.loadedModelName = null;

        if (proc != null) {
            try {
                if (proc.isAlive()) {
                    proc.destroy();
                    boolean exited = proc.waitFor(5, TimeUnit.SECONDS);
                    if (!exited && proc.isAlive()) {
                        proc.destroyForcibly();
                        proc.waitFor(2, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                if (proc.isAlive()) {
                    proc.destroyForcibly();
                }
            }
        }
    }

    private void recordUnexpectedExit(int exitCode) {
        synchronized (lifecycleLock) {
            if (shuttingDownIntentionally) {
                return;
            }
            this.managedProcess = null;
            this.activePort = 0;
            this.loadedModelPath = null;
            this.currentStatus = InferenceRuntimeStatus.ERROR;
            Path logFile = config.logsDir() != null ? config.logsDir().resolve("llama-server.log") : null;
            String tail = readLogTail(logFile);
            this.lastError = "llama-server exited unexpectedly with code " + exitCode
                    + (tail.isBlank() ? "." : ": " + tail);
        }
    }

    private void refreshBaselineStatus() {
        LlamaRuntimeDiscovery.Resolution resolution = LlamaRuntimeDiscovery.locate(config);
        this.currentStatus = resolution.available()
                ? InferenceRuntimeStatus.MODEL_NOT_LOADED
                : InferenceRuntimeStatus.RUNTIME_MISSING;
    }

    private static int allocateLoopbackPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName(LOOPBACK_HOST))) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }

    private static String sanitizeAlias(String raw) {
        if (raw == null || raw.isBlank()) {
            return "qwen2.5-coder-3b-instruct";
        }
        return raw.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
    }

    private static String readLogTail(Path logFile) {
        if (logFile == null || !Files.isRegularFile(logFile)) {
            return "";
        }
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - 5);
            return truncate(String.join(" | ", lines.subList(start, lines.size())), 400);
        } catch (IOException ex) {
            return "";
        }
    }

    private static Long safePid(Process proc) {
        try {
            return proc.pid();
        } catch (UnsupportedOperationException ex) {
            return null;
        }
    }

    private static int validateRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max + " (got " + value + ")");
        }
        return value;
    }

    private static double validateRangeDouble(double value, double min, double max, String field) {
        if (Double.isNaN(value) || value < min || value > max) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max + " (got " + value + ")");
        }
        return value;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }

    @Override
    public void close() {
        unloadModel();
    }
}
