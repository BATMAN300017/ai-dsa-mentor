package com.aidsa.modelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelManagerServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void downloadsVerifiesAndDetectsExternalDeletion(@TempDir Path tempDir) throws Exception {
        byte[] payload = createMockGgufPayload(64 * 1024);
        String sha256 = sha256Hex(payload);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model.gguf", exchange -> {
            exchange.getResponseHeaders().add("Accept-Ranges", "bytes");
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });
        server.start();

        Path appDataModelDir = tempDir.resolve("appdata");
        try (ModelManagerService manager = newManager(server.getAddress().getPort(), payload.length, sha256, tempDir, appDataModelDir)) {
            assertEquals(ModelDownloadStatus.NOT_DOWNLOADED, manager.inspectStatus().status());

            ModelInstallState result = manager.startDownloadAsync(false).get(15, TimeUnit.SECONDS);
            assertEquals(ModelDownloadStatus.VERIFIED, result.downloadStatus());
            assertEquals(ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM, result.verificationStatus());
            assertEquals(sha256, result.localCalculatedChecksum());

            Path target = appDataModelDir.resolve("test-model.gguf");
            assertTrue(Files.isRegularFile(target));
            assertFalse(Files.exists(appDataModelDir.resolve("test-model.gguf.part")));
            assertEquals(ModelDownloadStatus.VERIFIED, manager.inspectStatus().status());

            // Deleting the file externally must be detected immediately
            Files.delete(target);
            ModelDownloadProgress afterDelete = manager.inspectStatus();
            assertEquals(ModelDownloadStatus.MISSING_OR_CORRUPT, afterDelete.status());
            assertFalse(afterDelete.binaryPresentOnDisk());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void resumesPartialDownloadUsingHttpRange206(@TempDir Path tempDir) throws Exception {
        byte[] payload = createMockGgufPayload(40_000);
        String sha256 = sha256Hex(payload);
        int splitPoint = 15_000;
        AtomicReference<String> seenRangeHeader = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model.gguf", exchange -> {
            String range = exchange.getRequestHeaders().getFirst("Range");
            seenRangeHeader.set(range);
            if (("bytes=" + splitPoint + "-").equals(range)) {
                byte[] tail = Arrays.copyOfRange(payload, splitPoint, payload.length);
                exchange.getResponseHeaders().add("Content-Range",
                        "bytes " + splitPoint + "-" + (payload.length - 1) + "/" + payload.length);
                exchange.sendResponseHeaders(206, tail.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(tail);
                }
            } else {
                exchange.sendResponseHeaders(200, payload.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(payload);
                }
            }
        });
        server.start();

        Path appDataModelDir = tempDir.resolve("appdata");
        Files.createDirectories(appDataModelDir);
        Files.write(appDataModelDir.resolve("test-model.gguf.part"), Arrays.copyOfRange(payload, 0, splitPoint));

        try (ModelManagerService manager = newManager(server.getAddress().getPort(), payload.length, sha256, tempDir, appDataModelDir)) {
            ModelInstallState state = manager.startDownloadAsync(false).get(15, TimeUnit.SECONDS);

            assertEquals("bytes=" + splitPoint + "-", seenRangeHeader.get());
            assertEquals(ModelDownloadStatus.VERIFIED, state.downloadStatus());
            assertArrayEquals(payload, Files.readAllBytes(appDataModelDir.resolve("test-model.gguf")));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void supportsDownloadCancellationAndFailureHandling(@TempDir Path tempDir) throws Exception {
        byte[] payload = createMockGgufPayload(32_768);
        String sha256 = sha256Hex(payload);
        CountDownLatch firstChunkSent = new CountDownLatch(1);
        CountDownLatch serverRelease = new CountDownLatch(1);

        HttpServer slowServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        slowServer.createContext("/model.gguf", exchange -> {
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload, 0, 4096);
                os.flush();
                firstChunkSent.countDown();
                serverRelease.await(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Client cancelled stream as expected
            }
        });
        slowServer.start();

        Path appDataModelDir = tempDir.resolve("appdata-cancel");
        try (ModelManagerService manager = newManager(slowServer.getAddress().getPort(), payload.length, sha256, tempDir, appDataModelDir)) {
            var future = manager.startDownloadAsync(false);
            assertTrue(firstChunkSent.await(5, TimeUnit.SECONDS));

            long deadline = System.currentTimeMillis() + 5000;
            while (manager.inspectStatus().bytesDownloaded() < 4096 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertTrue(manager.inspectStatus().bytesDownloaded() >= 4096);

            ModelDownloadProgress cancelled = manager.cancelDownload();
            assertEquals(ModelDownloadStatus.CANCELLED, cancelled.status());
            assertTrue(cancelled.resumablePartPresent());
            assertTrue(cancelled.partialBytesOnDisk() >= 4096);
            assertFalse(Files.exists(appDataModelDir.resolve("test-model.gguf")));
            future.get(5, TimeUnit.SECONDS);
        } finally {
            serverRelease.countDown();
            slowServer.stop(0);
        }

        // HTTP 404 failure test
        HttpServer errorServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        errorServer.createContext("/model.gguf", exchange -> exchange.sendResponseHeaders(404, -1));
        errorServer.start();

        Path errorDir = tempDir.resolve("appdata-404");
        try (ModelManagerService manager = newManager(errorServer.getAddress().getPort(), payload.length, sha256, tempDir, errorDir)) {
            ModelInstallState failed = manager.startDownloadAsync(false).get(10, TimeUnit.SECONDS);
            assertEquals(ModelDownloadStatus.FAILED, failed.downloadStatus());
            assertNotNull(failed.lastError());
            assertTrue(failed.lastError().contains("404"));
        } finally {
            errorServer.stop(0);
        }
    }

    @Test
    void rejectsChecksumMismatchAndMissingUpstreamChecksum(@TempDir Path tempDir) throws Exception {
        byte[] payload = createMockGgufPayload(8192);
        String wrongSha = "f".repeat(64);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/model.gguf", exchange -> {
            exchange.sendResponseHeaders(200, payload.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });
        server.start();

        Path mismatchDir = tempDir.resolve("appdata-mismatch");
        try (ModelManagerService manager = newManager(server.getAddress().getPort(), payload.length, wrongSha, tempDir, mismatchDir)) {
            ModelInstallState state = manager.startDownloadAsync(false).get(10, TimeUnit.SECONDS);
            assertEquals(ModelDownloadStatus.MISSING_OR_CORRUPT, state.downloadStatus());
            assertEquals(ChecksumVerificationStatus.MISMATCH, state.verificationStatus());
            assertFalse(Files.exists(mismatchDir.resolve("test-model.gguf")));
            assertFalse(Files.exists(mismatchDir.resolve("test-model.gguf.part")));
        }

        Path noUpstreamDir = tempDir.resolve("appdata-no-upstream");
        try (ModelManagerService manager = newManager(server.getAddress().getPort(), payload.length, "", tempDir, noUpstreamDir)) {
            ModelInstallState state = manager.startDownloadAsync(false).get(10, TimeUnit.SECONDS);
            assertEquals(ModelDownloadStatus.FAILED, state.downloadStatus());
            assertEquals(ChecksumVerificationStatus.UNVERIFIED_NO_UPSTREAM_CHECKSUM, state.verificationStatus());
            assertNotNull(state.localCalculatedChecksum());
            assertFalse(Files.exists(noUpstreamDir.resolve("test-model.gguf")));
        } finally {
            server.stop(0);
        }
    }

    private ModelManagerService newManager(
            int port,
            long expectedSize,
            String upstreamSha,
            Path workspaceDir,
            Path appDataDir
    ) {
        ModelMetadata metadata = new ModelMetadata(
                "Test-Model",
                "Test",
                "org/repo",
                "https://huggingface.co/org/repo",
                "Q4_K_M",
                "GGUF",
                "test-model.gguf",
                expectedSize,
                "NOT_DOWNLOADED",
                "SHA-256",
                upstreamSha,
                "Unit test mock upstream",
                "",
                "rev123",
                "http://127.0.0.1:" + port + "/model.gguf",
                "llama.cpp"
        );
        return new ModelManagerService(
                metadata,
                workspaceDir,
                appDataDir,
                objectMapper,
                java.net.http.HttpClient.newHttpClient(),
                true
        );
    }

    private static byte[] createMockGgufPayload(int size) {
        byte[] bytes = new byte[size];
        byte[] header = "GGUF".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, bytes, 0, header.length);
        for (int i = header.length; i < size; i++) {
            bytes[i] = (byte) (i * 31 + 7);
        }
        return bytes;
    }

    private static String sha256Hex(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }
}
