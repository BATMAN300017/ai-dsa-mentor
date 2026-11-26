package com.aidsa.modelmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Persisted local installation state stored in the OS user AppData model directory.
 * Kept separate from repository-tracked {@code model-info.json} descriptors.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ModelInstallState(
        String modelName,
        String revision,
        String expectedFilename,
        long expectedSizeBytes,
        long actualSizeBytes,
        long fileLastModifiedMillis,
        String checksumAlgorithm,
        String upstreamChecksum,
        String upstreamChecksumSource,
        String localCalculatedChecksum,
        ChecksumVerificationStatus verificationStatus,
        ModelDownloadStatus downloadStatus,
        Instant lastVerifiedAt,
        String lastError
) {
}
