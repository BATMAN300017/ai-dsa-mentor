package com.aidsa.modelmanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sha256ChecksumCalculatorTest {

    private final Sha256ChecksumCalculator calculator = new Sha256ChecksumCalculator();

    @Test
    void verifiesMatchingGgufFileAgainstTrustedUpstreamChecksum(@TempDir Path tempDir) throws IOException {
        byte[] content = "GGUF-test-payload-0123456789".getBytes(StandardCharsets.UTF_8);
        Path model = tempDir.resolve("model.gguf");
        Files.write(model, content);

        String expectedSha = calculator.computeSha256(model, () -> false);
        Sha256ChecksumCalculator.VerificationResult result =
                calculator.verifyFile(model, expectedSha, content.length, true, () -> false);

        assertTrue(result.verifiedAgainstUpstream());
        assertEquals(ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM, result.status());
        assertEquals(expectedSha, result.localCalculatedSha256());
        assertEquals(content.length, result.actualSizeBytes());
    }

    @Test
    void neverClaimsVerifiedWhenUpstreamChecksumIsMissing(@TempDir Path tempDir) throws IOException {
        byte[] content = "GGUF-unverified-payload".getBytes(StandardCharsets.UTF_8);
        Path model = tempDir.resolve("model.gguf");
        Files.write(model, content);

        Sha256ChecksumCalculator.VerificationResult result =
                calculator.verifyFile(model, "", content.length, true, () -> false);

        assertFalse(result.verifiedAgainstUpstream());
        assertEquals(ChecksumVerificationStatus.UNVERIFIED_NO_UPSTREAM_CHECKSUM, result.status());
        assertNotNull(result.localCalculatedSha256());
    }

    @Test
    void detectsChecksumMismatchSizeMismatchAndInvalidGgufHeader(@TempDir Path tempDir) throws IOException {
        byte[] content = "GGUF-valid-header-payload".getBytes(StandardCharsets.UTF_8);
        Path model = tempDir.resolve("model.gguf");
        Files.write(model, content);

        String wrongSha = "0".repeat(64);
        Sha256ChecksumCalculator.VerificationResult mismatch =
                calculator.verifyFile(model, wrongSha, content.length, true, () -> false);
        assertFalse(mismatch.verifiedAgainstUpstream());
        assertEquals(ChecksumVerificationStatus.MISMATCH, mismatch.status());

        Sha256ChecksumCalculator.VerificationResult wrongSize =
                calculator.verifyFile(model, wrongSha, content.length + 10L, true, () -> false);
        assertFalse(wrongSize.verifiedAgainstUpstream());
        assertEquals(ChecksumVerificationStatus.MISMATCH, wrongSize.status());

        Path notGguf = tempDir.resolve("html-error.gguf");
        Files.writeString(notGguf, "<html>404 Not Found</html>");
        Sha256ChecksumCalculator.VerificationResult badHeader =
                calculator.verifyFile(notGguf, wrongSha, 0L, true, () -> false);
        assertFalse(badHeader.verifiedAgainstUpstream());
        assertEquals(ChecksumVerificationStatus.MISMATCH, badHeader.status());
    }
}
