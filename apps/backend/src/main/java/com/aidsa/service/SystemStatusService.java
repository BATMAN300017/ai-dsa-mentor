package com.aidsa.service;

import com.aidsa.config.AppProperties;
import com.aidsa.dto.ModelDescriptorDto;
import com.aidsa.dto.SystemStatusResponse;
import com.aidsa.model.CapabilityDescriptor;
import com.aidsa.model.CapabilityState;
import com.aidsa.modelmanager.AppDataPathResolver;
import com.aidsa.modelmanager.ModelMetadata;
import com.aidsa.modelmanager.ModelMetadataReader;
import com.aidsa.tutoring.SocraticPromptPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Aggregates read-only Task 1 foundation status and model descriptor metadata.
 */
@Service
public class SystemStatusService {

    private static final Logger log = LoggerFactory.getLogger(SystemStatusService.class);

    private final AppProperties properties;
    private final ModelMetadataReader metadataReader;
    private final AppDataPathResolver appDataPathResolver;

    public SystemStatusService(
            AppProperties properties,
            ModelMetadataReader metadataReader,
            AppDataPathResolver appDataPathResolver
    ) {
        this.properties = properties;
        this.metadataReader = metadataReader;
        this.appDataPathResolver = appDataPathResolver;
    }

    public SystemStatusResponse getSystemStatus() {
        ModelDescriptorDto modelDto = loadModelDescriptor();
        List<CapabilityDescriptor> capabilities = List.of(
                new CapabilityDescriptor("backend-api", "Spring Boot Local API", CapabilityState.FOUNDATION_READY, "Bound to 127.0.0.1 loopback"),
                new CapabilityDescriptor("model-manager", "Qwen2.5-Coder-3B Metadata", CapabilityState.NOT_CONFIGURED, "Descriptor ready; model binary not downloaded"),
                new CapabilityDescriptor("ai-engine", "llama.cpp Local Inference", CapabilityState.PLANNED, "Scheduled for Task 2+"),
                new CapabilityDescriptor("code-analyzer", "Tree-sitter AST Analysis", CapabilityState.PLANNED, "Scheduled for Task 2+"),
                new CapabilityDescriptor("tutoring-engine", "Socratic Guidance Engine", CapabilityState.PLANNED, "Policy guardrails defined"),
                new CapabilityDescriptor("test-engine", "Isolated Sandbox Runner", CapabilityState.PLANNED, "Out-of-process policy defined"),
                new CapabilityDescriptor("browser-extension", "Manifest V3 Extension", CapabilityState.IN_DEVELOPMENT, "Scaffold ready; site adapters planned")
        );

        return new SystemStatusResponse(
                properties.applicationName(),
                properties.version(),
                SocraticPromptPolicy.CORE_PHILOSOPHY,
                "127.0.0.1",
                false,
                modelDto,
                capabilities
        );
    }

    private ModelDescriptorDto loadModelDescriptor() {
        String slug = properties.model() != null && properties.model().slug() != null
                ? properties.model().slug()
                : "qwen2.5-coder-3b";

        Optional<Path> descriptorPath = resolveDescriptorPath(slug);
        if (descriptorPath.isPresent()) {
            try {
                Optional<ModelMetadata> loaded = metadataReader.readFromFile(descriptorPath.get());
                if (loaded.isPresent()) {
                    ModelMetadata meta = loaded.get();
                    boolean present = metadataReader.isModelBinaryPresent(
                            meta,
                            descriptorPath.get().getParent(),
                            appDataPathResolver.resolveModelDirectory(slug)
                    );
                    return new ModelDescriptorDto(
                            meta.modelName(),
                            meta.modelProvider(),
                            meta.huggingFaceRepo(),
                            meta.expectedQuantization(),
                            meta.expectedFileFormat(),
                            meta.expectedFilename(),
                            meta.downloadStatus(),
                            meta.runtimeTarget(),
                            present
                    );
                }
            } catch (IOException ex) {
                log.warn("Could not parse model-info.json at {}: {}", descriptorPath.get(), ex.getMessage());
            }
        }

        return new ModelDescriptorDto(
                "Qwen2.5-Coder-3B-Instruct",
                "Qwen (Alibaba Cloud)",
                "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                "Q4_K_M",
                "GGUF",
                "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
                "NOT_DOWNLOADED",
                "llama.cpp",
                false
        );
    }

    private Optional<Path> resolveDescriptorPath(String slug) {
        String configured = properties.model() != null ? properties.model().descriptorPath() : null;
        if (configured != null && !configured.isBlank()) {
            Path p = Path.of(configured).toAbsolutePath().normalize();
            if (Files.isRegularFile(p)) {
                return Optional.of(p);
            }
        }
        List<Path> candidates = List.of(
                Path.of("resources", "models", slug, "model-info.json"),
                Path.of("..", "..", "resources", "models", slug, "model-info.json"),
                Path.of("..", "resources", "models", slug, "model-info.json")
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }
}
