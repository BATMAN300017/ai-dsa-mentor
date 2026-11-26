package com.aidsa.modelmanager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.function.BooleanSupplier;

/**
 * Calculates local SHA-256 digests and validates GGUF model files against expected size,
 * GGUF header signature, and trusted upstream checksums.
 */
public final class Sha256ChecksumCalculator {

    private static final int BUFFER_SIZE = 64 * 1024;
    private static final byte[] GGUF_MAGIC = new byte[]{'G', 'G', 'U', 'F'};

    public record VerificationResult(
            boolean verifiedAgainstUpstream,
            ChecksumVerificationStatus status,
            String localCalculatedSha256,
            long actualSizeBytes,
            String failureReason
    ) {
    }

    public String computeSha256(Path file, BooleanSupplier cancelled) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IOException("Cannot compute checksum: file does not exist: " + file);
        }
        MessageDigest digest = newSha256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = Files.newInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                if (cancelled != null && cancelled.getAsBoolean()) {
                    throw new IOException("Checksum calculation cancelled");
                }
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public VerificationResult verifyFile(
            Path file,
            String trustedUpstreamSha256,
            long expectedSizeBytes,
            boolean requireGgufHeader,
            BooleanSupplier cancelled
    ) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            return new VerificationResult(false, ChecksumVerificationStatus.NOT_CHECKED, null, 0L,
                    "Model file does not exist on disk.");
        }

        long actualSize = Files.size(file);
        if (actualSize <= 0L) {
            return new VerificationResult(false, ChecksumVerificationStatus.MISMATCH, null, actualSize,
                    "Model file is empty (0 bytes).");
        }

        if (expectedSizeBytes > 0L && actualSize != expectedSizeBytes) {
            return new VerificationResult(false, ChecksumVerificationStatus.MISMATCH, null, actualSize,
                    "Model file size mismatch: expected " + expectedSizeBytes + " bytes but found " + actualSize + " bytes.");
        }

        if (requireGgufHeader && !hasGgufMagicHeader(file)) {
            return new VerificationResult(false, ChecksumVerificationStatus.MISMATCH, null, actualSize,
                    "File does not begin with a valid GGUF magic header.");
        }

        String localSha = computeSha256(file, cancelled);
        if (!ModelMetadata.isValidSha256Hex(trustedUpstreamSha256)) {
            return new VerificationResult(
                    false,
                    ChecksumVerificationStatus.UNVERIFIED_NO_UPSTREAM_CHECKSUM,
                    localSha,
                    actualSize,
                    "Locally calculated SHA-256 (" + localSha + "), but no trusted upstream SHA-256 checksum is configured."
            );
        }

        String expectedNormalized = trustedUpstreamSha256.trim().toLowerCase();
        if (!localSha.equalsIgnoreCase(expectedNormalized)) {
            return new VerificationResult(
                    false,
                    ChecksumVerificationStatus.MISMATCH,
                    localSha,
                    actualSize,
                    "SHA-256 mismatch: expected upstream " + expectedNormalized + " but calculated locally " + localSha + "."
            );
        }

        return new VerificationResult(
                true,
                ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM,
                localSha,
                actualSize,
                null
        );
    }

    public static boolean hasGgufMagicHeader(Path file) throws IOException {
        byte[] header = new byte[4];
        try (InputStream in = Files.newInputStream(file)) {
            int n = in.readNBytes(header, 0, 4);
            return n == 4 && Arrays.equals(header, GGUF_MAGIC);
        }
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in JVM", ex);
        }
    }
}
