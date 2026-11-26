package com.aidsa.modelmanager;

import java.time.Instant;

/**
 * Thread-safe snapshot of model installation, download progress, and checksum verification state.
 */
public record ModelDownloadProgress(
        ModelDownloadStatus status,
        ChecksumVerificationStatus verificationStatus,
        long bytesDownloaded,
        long totalBytes,
        double progressPercent,
        long bytesPerSecond,
        boolean binaryPresentOnDisk,
        boolean resumablePartPresent,
        long partialBytesOnDisk,
        long actualSizeBytes,
        String localCalculatedChecksum,
        String upstreamChecksum,
        String upstreamChecksumSource,
        String errorMessage,
        Instant startedAt,
        Instant updatedAt
) {
}
