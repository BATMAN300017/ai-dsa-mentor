package com.aidsa.dto;

import com.aidsa.ai.InferenceRuntimeStatus;
import com.aidsa.modelmanager.ChecksumVerificationStatus;
import com.aidsa.modelmanager.ModelDownloadStatus;

/**
 * Complete local model installation, checksum verification, download progress,
 * and llama.cpp runtime readiness payload exposed by {@code /api/v1/model/*}.
 */
public record ModelStatusResponse(
        String modelName,
        String modelProvider,
        String huggingFaceRepo,
        String huggingFaceUrl,
        String expectedQuantization,
        String expectedFileFormat,
        String expectedFilename,
        long expectedSizeBytes,
        String revision,
        String runtimeTarget,
        String storageLocationHint,
        ModelDownloadStatus downloadStatus,
        ChecksumVerificationStatus checksumVerificationStatus,
        String upstreamChecksum,
        String upstreamChecksumSource,
        String localCalculatedChecksum,
        boolean binaryPresentOnDisk,
        boolean resumablePartPresent,
        long bytesDownloaded,
        long totalBytes,
        double progressPercent,
        long bytesPerSecond,
        long actualSizeBytes,
        String downloadError,
        boolean runtimeAvailable,
        String runtimeSource,
        InferenceRuntimeStatus runtimeStatus,
        boolean modelLoaded,
        boolean modelLoading,
        int runtimePort,
        int contextSize,
        int threads,
        int gpuLayers,
        String runtimeError
) {
}
