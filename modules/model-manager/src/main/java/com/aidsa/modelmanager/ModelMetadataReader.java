package com.aidsa.modelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads local {@code model-info.json} descriptors and checks whether an actual model file
 * is present on disk without downloading or fabricating model artifacts.
 */
public class ModelMetadataReader {

    private final ObjectMapper objectMapper;

    public ModelMetadataReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<ModelMetadata> readFromFile(Path metadataPath) throws IOException {
        if (metadataPath == null || !Files.isRegularFile(metadataPath)) {
            return Optional.empty();
        }
        return Optional.of(objectMapper.readValue(metadataPath.toFile(), ModelMetadata.class));
    }

    public Optional<ModelMetadata> readFromClasspath(String resourcePath) throws IOException {
        try (InputStream stream = ModelMetadataReader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(stream, ModelMetadata.class));
        }
    }

    /**
     * Checks whether the expected GGUF binary actually exists inside either the workspace
     * model directory or the OS user application-data directory.
     */
    public boolean isModelBinaryPresent(ModelMetadata metadata, Path workspaceModelDir, Path appDataModelDir) {
        if (metadata == null || metadata.expectedFilename() == null || metadata.expectedFilename().isBlank()) {
            return false;
        }
        return isRegularNonEmptyFile(workspaceModelDir, metadata.expectedFilename())
                || isRegularNonEmptyFile(appDataModelDir, metadata.expectedFilename());
    }

    private static boolean isRegularNonEmptyFile(Path dir, String filename) {
        if (dir == null) {
            return false;
        }
        Path candidate = dir.resolve(filename);
        try {
            return Files.isRegularFile(candidate) && Files.size(candidate) > 0L;
        } catch (IOException ex) {
            return false;
        }
    }
}
