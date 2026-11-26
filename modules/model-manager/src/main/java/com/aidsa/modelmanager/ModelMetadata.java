package com.aidsa.modelmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Metadata descriptor loaded from {@code model-info.json} for a local model package.
 * Distinguishes a trusted upstream checksum ({@code upstreamChecksum}) from local installation state;
 * having a nonempty checksum field in metadata never implies that a local binary on disk is verified.
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
        long expectedSizeBytes,
        String downloadStatus,
        String checksumAlgorithm,
        String upstreamChecksum,
        String upstreamChecksumSource,
        String checksum,
        String revision,
        String downloadUrl,
        String runtimeTarget
) {
    private static final Pattern SHA256_HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    /**
     * Checks only whether metadata carries a syntactically valid trusted upstream SHA-256 digest.
     * This does NOT mean a file on disk has been downloaded or verified against it.
     */
    public boolean hasTrustedUpstreamChecksum() {
        return isValidSha256Hex(upstreamChecksum);
    }

    public static boolean isValidSha256Hex(String value) {
        return value != null && SHA256_HEX_PATTERN.matcher(value.trim()).matches();
    }

    public boolean isSafeFilename() {
        if (expectedFilename == null || expectedFilename.isBlank()) {
            return false;
        }
        String trimmed = expectedFilename.trim();
        return !trimmed.contains("/")
                && !trimmed.contains("\\")
                && !trimmed.contains("..")
                && !trimmed.equals(".");
    }

    public Optional<String> resolvedDownloadUrl() {
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            return Optional.of(downloadUrl.trim());
        }
        if (huggingFaceRepo != null && !huggingFaceRepo.isBlank()
                && revision != null && !revision.isBlank()
                && isSafeFilename()) {
            return Optional.of(String.format(
                    "https://huggingface.co/%s/resolve/%s/%s",
                    huggingFaceRepo.trim(),
                    revision.trim(),
                    expectedFilename.trim()
            ));
        }
        return Optional.empty();
    }
}
