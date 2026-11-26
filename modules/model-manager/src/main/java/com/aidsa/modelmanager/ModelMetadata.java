package com.aidsa.modelmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Metadata descriptor loaded from {@code model-info.json} for a local model package.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelMetadata(
        String modelName,
        String modelProvider,
        String huggingFaceRepo,
        String huggingFaceUrl,
        String expectedQuantization,
        String expectedFileFormat,
        String expectedFilename,
        String downloadStatus,
        String checksumAlgorithm,
        String checksum,
        String revision,
        String runtimeTarget
) {
    public boolean hasVerifiedChecksum() {
        return checksum != null && !checksum.isBlank();
    }
}
