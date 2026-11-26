package com.aidsa.config;

import com.aidsa.ai.LlamaCppInferenceProvider;
import com.aidsa.ai.LlamaRuntimeConfig;
import com.aidsa.modelmanager.AppDataPathResolver;
import com.aidsa.modelmanager.ModelManagerService;
import com.aidsa.modelmanager.ModelMetadata;
import com.aidsa.modelmanager.ModelMetadataReader;
import com.aidsa.session.InMemorySessionStore;
import com.aidsa.session.SessionStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Wires domain services from {@code modules/*} into Spring without duplicating logic.
 */
@Configuration
public class CoreModulesConfig {

    private static final Logger log = LoggerFactory.getLogger(CoreModulesConfig.class);

    @Bean
    public AppDataPathResolver appDataPathResolver() {
        return new AppDataPathResolver();
    }

    @Bean
    public ModelMetadataReader modelMetadataReader(ObjectMapper objectMapper) {
        return new ModelMetadataReader(objectMapper);
    }

    @Bean
    public SessionStorePort sessionStorePort() {
        return new InMemorySessionStore();
    }

    @Bean(destroyMethod = "close")
    public ModelManagerService modelManagerService(
            AppProperties properties,
            ModelMetadataReader metadataReader,
            AppDataPathResolver pathResolver,
            ObjectMapper objectMapper
    ) {
        String slug = properties.model() != null && properties.model().slug() != null
                ? properties.model().slug()
                : "qwen2.5-coder-3b";

        Optional<Path> descriptorPath = resolveDescriptorPath(properties, slug);
        ModelMetadata metadata = loadDescriptor(descriptorPath, metadataReader);
        Path workspaceModelDir = descriptorPath.map(Path::getParent)
                .orElseGet(() -> Path.of("resources", "models", slug).toAbsolutePath().normalize());

        Path appDataModelDir = resolveModelAppDataDir(properties, pathResolver, slug);
        return new ModelManagerService(metadata, workspaceModelDir, appDataModelDir, objectMapper);
    }

    @Bean(destroyMethod = "close")
    public LlamaCppInferenceProvider llamaCppInferenceProvider(
            AppProperties properties,
            AppDataPathResolver pathResolver,
            ObjectMapper objectMapper
    ) {
        AppProperties.RuntimeConfig rt = properties.runtime();
        String executablePath = rt != null && rt.executablePath() != null ? rt.executablePath() : "";
        Path appDataRuntimeDir = (rt != null && rt.appDataDir() != null && !rt.appDataDir().isBlank())
                ? Path.of(rt.appDataDir()).toAbsolutePath().normalize()
                : pathResolver.resolveRuntimeDirectory("llama.cpp");
        Path workspaceRuntimeDir = resolveWorkspaceRuntimeDir();
        Path logsDir = pathResolver.resolveLogsDirectory();

        int ctxSize = rt != null && rt.contextSize() > 0 ? rt.contextSize() : 4096;
        int threads = rt != null && rt.threads() > 0 ? rt.threads() : LlamaRuntimeConfig.recommendedCpuThreads();
        int gpuLayers = rt != null && rt.gpuLayers() >= 0 ? rt.gpuLayers() : 0;
        int preferredPort = rt != null && rt.preferredPort() > 0 ? rt.preferredPort() : 0;
        Duration startupTimeout = Duration.ofSeconds(rt != null && rt.startupTimeoutSeconds() > 0 ? rt.startupTimeoutSeconds() : 90);
        Duration requestTimeout = Duration.ofSeconds(rt != null && rt.requestTimeoutSeconds() > 0 ? rt.requestTimeoutSeconds() : 120);

        LlamaRuntimeConfig config = new LlamaRuntimeConfig(
                executablePath,
                appDataRuntimeDir,
                workspaceRuntimeDir,
                logsDir,
                ctxSize,
                threads,
                gpuLayers,
                preferredPort,
                startupTimeout,
                requestTimeout
        );
        return new LlamaCppInferenceProvider(config, objectMapper);
    }

    private static Path resolveModelAppDataDir(
            AppProperties properties,
            AppDataPathResolver pathResolver,
            String slug
    ) {
        if (properties.model() != null
                && properties.model().appDataDir() != null
                && !properties.model().appDataDir().isBlank()) {
            return Path.of(properties.model().appDataDir()).toAbsolutePath().normalize();
        }
        return pathResolver.resolveModelDirectory(slug);
    }

    private static Optional<Path> resolveDescriptorPath(AppProperties properties, String slug) {
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

    private static Path resolveWorkspaceRuntimeDir() {
        List<Path> candidates = List.of(
                Path.of("resources", "runtimes", "llama.cpp"),
                Path.of("..", "..", "resources", "runtimes", "llama.cpp"),
                Path.of("..", "resources", "runtimes", "llama.cpp")
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                return normalized;
            }
        }
        return Path.of("resources", "runtimes", "llama.cpp").toAbsolutePath().normalize();
    }

    private static ModelMetadata loadDescriptor(Optional<Path> descriptorPath, ModelMetadataReader reader) {
        if (descriptorPath.isPresent()) {
            try {
                Optional<ModelMetadata> loaded = reader.readFromFile(descriptorPath.get());
                if (loaded.isPresent()) {
                    return loaded.get();
                }
            } catch (IOException ex) {
                log.warn("Could not parse model-info.json at {}: {}", descriptorPath.get(), ex.getMessage());
            }
        }
        return new ModelMetadata(
                "Qwen2.5-Coder-3B-Instruct",
                "Qwen (Alibaba Cloud)",
                "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                "Q4_K_M",
                "GGUF",
                "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
                2104932800L,
                "NOT_DOWNLOADED",
                "SHA-256",
                "724fb256bec1ff062b2f65e4569e871ad2e95ab2a3989723d1769c54294730b7",
                "Hugging Face Git LFS pointer oid at revision f74adce6aa16316c625447af059dbebe4983757c",
                "",
                "f74adce6aa16316c625447af059dbebe4983757c",
                "https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/f74adce6aa16316c625447af059dbebe4983757c/qwen2.5-coder-3b-instruct-q4_k_m.gguf",
                "llama.cpp"
        );
    }
}
