package com.aidsa.dto;

/**
 * Non-sensitive model metadata view derived from {@code model-info.json}.
 */
public record ModelDescriptorDto(
        String modelName,
        String modelProvider,
        String huggingFaceRepo,
        String expectedQuantization,
        String expectedFileFormat,
        String expectedFilename,
        String downloadStatus,
        String runtimeTarget,
        boolean binaryPresentOnDisk
) {
}
