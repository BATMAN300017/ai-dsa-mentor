package com.aidsa.service;

import com.aidsa.ai.InferenceRuntimeStatus;
import com.aidsa.ai.LlamaCppInferenceProvider;
import com.aidsa.ai.LlamaRuntimeSnapshot;
import com.aidsa.config.AppProperties;
import com.aidsa.dto.ModelDescriptorDto;
import com.aidsa.dto.SystemStatusResponse;
import com.aidsa.model.CapabilityDescriptor;
import com.aidsa.model.CapabilityState;
import com.aidsa.modelmanager.ModelDownloadProgress;
import com.aidsa.modelmanager.ModelDownloadStatus;
import com.aidsa.modelmanager.ModelManagerService;
import com.aidsa.modelmanager.ModelMetadata;
import com.aidsa.tutoring.SocraticPromptPolicy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Aggregates live system, model installation, and local inference runtime telemetry.
 */
@Service
public class SystemStatusService {

    private final AppProperties properties;
    private final ModelManagerService modelManagerService;
    private final LlamaCppInferenceProvider inferenceProvider;

    public SystemStatusService(
            AppProperties properties,
            ModelManagerService modelManagerService,
            LlamaCppInferenceProvider inferenceProvider
    ) {
        this.properties = properties;
        this.modelManagerService = modelManagerService;
        this.inferenceProvider = inferenceProvider;
    }

    public SystemStatusResponse getSystemStatus() {
        ModelMetadata meta = modelManagerService.getMetadata();
        ModelDownloadProgress progress = modelManagerService.inspectStatus();
        LlamaRuntimeSnapshot runtime = inferenceProvider.snapshot();

        ModelDescriptorDto modelDto = new ModelDescriptorDto(
                meta.modelName(),
                meta.modelProvider(),
                meta.huggingFaceRepo(),
                meta.expectedQuantization(),
                meta.expectedFileFormat(),
                meta.expectedFilename(),
                progress.status().name(),
                meta.runtimeTarget(),
                progress.binaryPresentOnDisk()
        );

        CapabilityState modelManagerState = progress.status() == ModelDownloadStatus.VERIFIED
                ? CapabilityState.FOUNDATION_READY
                : CapabilityState.NOT_CONFIGURED;

        CapabilityState aiEngineState = runtime.status() == InferenceRuntimeStatus.READY
                ? CapabilityState.FOUNDATION_READY
                : (runtime.runtimeAvailable() ? CapabilityState.IN_DEVELOPMENT : CapabilityState.NOT_CONFIGURED);

        String aiNotes = switch (runtime.status()) {
            case READY -> "llama-server ready on 127.0.0.1:" + runtime.port();
            case STARTING -> "Loading Qwen2.5-Coder-3B GGUF into local memory";
            case RUNTIME_MISSING -> "llama-server binary not yet installed";
            case ERROR -> runtime.lastError() != null ? runtime.lastError() : "Runtime error";
            default -> "llama-server discovered; model not loaded";
        };

        List<CapabilityDescriptor> capabilities = List.of(
                new CapabilityDescriptor("backend-api", "Spring Boot Local API", CapabilityState.FOUNDATION_READY, "Bound to 127.0.0.1 loopback"),
                new CapabilityDescriptor("model-manager", "Qwen2.5-Coder-3B Model Manager", modelManagerState,
                        progress.status() == ModelDownloadStatus.VERIFIED
                                ? "GGUF verified against upstream SHA-256"
                                : "Status: " + progress.status()),
                new CapabilityDescriptor("ai-engine", "llama.cpp Local Inference", aiEngineState, aiNotes),
                new CapabilityDescriptor("code-analyzer", "Tree-sitter AST Analysis", CapabilityState.PLANNED, "Scheduled for Task 4"),
                new CapabilityDescriptor("tutoring-engine", "Socratic Guidance Engine", CapabilityState.PLANNED, "Scheduled for Task 3"),
                new CapabilityDescriptor("test-engine", "Isolated Sandbox Runner", CapabilityState.PLANNED, "Scheduled for Task 5"),
                new CapabilityDescriptor("browser-extension", "Manifest V3 Extension", CapabilityState.IN_DEVELOPMENT, "Scaffold ready; site adapters planned")
        );

        return new SystemStatusResponse(
                properties.applicationName(),
                properties.version(),
                SocraticPromptPolicy.CORE_PHILOSOPHY,
                "127.0.0.1",
                runtime.modelLoaded(),
                modelDto,
                capabilities
        );
    }
}
