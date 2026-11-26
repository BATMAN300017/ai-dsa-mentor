package com.aidsa.modelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages GGUF model downloading, HTTP Range resume via {@code .part} staging files,
 * progress tracking, cancellation, size/GGUF/SHA-256 verification against trusted upstream checksums,
 * and persistent installation state inside the OS user AppData directory.
 */
public class ModelManagerService implements AutoCloseable {

    private static final String INSTALL_STATE_FILENAME = "install-state.json";
    private static final int BUFFER_SIZE = 64 * 1024;

    private final ModelMetadata metadata;
    private final Path workspaceModelDir;
    private final Path appDataModelDir;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Sha256ChecksumCalculator checksumCalculator;
    private final boolean allowInsecureLoopbackHttpForTests;
    private final ExecutorService downloadExecutor;

    private final Object lifecycleLock = new Object();
    private final AtomicReference<ModelDownloadProgress> activeProgress = new AtomicReference<>();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private volatile CompletableFuture<ModelInstallState> activeDownloadFuture;
    private volatile CompletableFuture<HttpResponse<InputStream>> activeHttpFuture;
    private volatile InputStream activeStream;

    public ModelManagerService(
            ModelMetadata metadata,
            Path workspaceModelDir,
            Path appDataModelDir,
            ObjectMapper objectMapper
    ) {
        this(
                metadata,
                workspaceModelDir,
                appDataModelDir,
                objectMapper,
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(15))
                        .build(),
                false
        );
    }

    public ModelManagerService(
            ModelMetadata metadata,
            Path workspaceModelDir,
            Path appDataModelDir,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            boolean allowInsecureLoopbackHttpForTests
    ) {
        this.metadata = metadata;
        this.workspaceModelDir = workspaceModelDir;
        this.appDataModelDir = appDataModelDir;
        this.objectMapper = configureMapper(objectMapper);
        this.httpClient = httpClient;
        this.checksumCalculator = new Sha256ChecksumCalculator();
        this.allowInsecureLoopbackHttpForTests = allowInsecureLoopbackHttpForTests;
        this.downloadExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "aidsa-model-downloader");
            t.setDaemon(true);
            return t;
        });
    }

    private static ObjectMapper configureMapper(ObjectMapper base) {
        ObjectMapper copy = base == null ? new ObjectMapper() : base.copy();
        copy.registerModule(new JavaTimeModule());
        copy.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return copy;
    }

    public ModelMetadata getMetadata() {
        return metadata;
    }

    public Path getAppDataModelDir() {
        return appDataModelDir;
    }

    public Optional<Path> resolveVerifiedModelFile() {
        ModelDownloadProgress snapshot = inspectStatus();
        if (snapshot.status() != ModelDownloadStatus.VERIFIED) {
            return Optional.empty();
        }
        return findExistingTargetFile();
    }

    public Optional<Path> findExistingTargetFile() {
        if (!metadata.isSafeFilename()) {
            return Optional.empty();
        }
        Path appDataTarget = resolveAppDataTargetPath();
        if (Files.isRegularFile(appDataTarget)) {
            return Optional.of(appDataTarget);
        }
        if (workspaceModelDir != null) {
            Path workspaceTarget = workspaceModelDir.resolve(metadata.expectedFilename().trim()).normalize();
            if (Files.isRegularFile(workspaceTarget)) {
                return Optional.of(workspaceTarget);
            }
        }
        return Optional.empty();
    }

    public Path resolveAppDataTargetPath() {
        if (!metadata.isSafeFilename()) {
            throw new IllegalStateException("Invalid model expectedFilename: " + metadata.expectedFilename());
        }
        return appDataModelDir.resolve(metadata.expectedFilename().trim()).normalize();
    }

    public Path resolvePartPath() {
        if (!metadata.isSafeFilename()) {
            throw new IllegalStateException("Invalid model expectedFilename: " + metadata.expectedFilename());
        }
        return appDataModelDir.resolve(metadata.expectedFilename().trim() + ".part").normalize();
    }

    private Path resolveInstallStatePath() {
        return appDataModelDir.resolve(INSTALL_STATE_FILENAME).normalize();
    }

    /**
     * Inspects current installation, download, partial file, and integrity verification status.
     * Never trusts a cached state or non-empty checksum string blindly when the file on disk
     * has changed or is missing.
     */
    public ModelDownloadProgress inspectStatus() {
        ModelDownloadProgress current = activeProgress.get();
        if (current != null && (current.status() == ModelDownloadStatus.DOWNLOADING
                || current.status() == ModelDownloadStatus.VERIFYING)) {
            return current;
        }

        if (!metadata.isSafeFilename()) {
            return buildProgress(
                    ModelDownloadStatus.MISSING_OR_CORRUPT,
                    ChecksumVerificationStatus.NOT_CHECKED,
                    0L,
                    metadata.expectedSizeBytes(),
                    0.0,
                    0L,
                    false,
                    false,
                    0L,
                    0L,
                    null,
                    "Model descriptor has an invalid or unsafe expectedFilename.",
                    null
            );
        }

        Path partPath = resolvePartPath();
        boolean partExists = Files.isRegularFile(partPath);
        long partBytes = safeFileSize(partPath);

        Optional<Path> targetOpt = findExistingTargetFile();
        Optional<ModelInstallState> savedStateOpt = readInstallState();

        if (targetOpt.isEmpty()) {
            if (savedStateOpt.isPresent()) {
                ModelInstallState saved = savedStateOpt.get();
                if (saved.downloadStatus() == ModelDownloadStatus.CANCELLED) {
                    return buildProgress(
                            ModelDownloadStatus.CANCELLED,
                            ChecksumVerificationStatus.NOT_CHECKED,
                            partBytes,
                            metadata.expectedSizeBytes(),
                            computePercent(partBytes, metadata.expectedSizeBytes()),
                            0L,
                            false,
                            partExists,
                            partBytes,
                            0L,
                            saved.localCalculatedChecksum(),
                            saved.lastError(),
                            saved.lastVerifiedAt()
                    );
                }
                if (saved.downloadStatus() == ModelDownloadStatus.FAILED
                        || saved.downloadStatus() == ModelDownloadStatus.MISSING_OR_CORRUPT) {
                    return buildProgress(
                            saved.downloadStatus(),
                            saved.verificationStatus() != null ? saved.verificationStatus() : ChecksumVerificationStatus.NOT_CHECKED,
                            partBytes,
                            metadata.expectedSizeBytes(),
                            computePercent(partBytes, metadata.expectedSizeBytes()),
                            0L,
                            false,
                            partExists,
                            partBytes,
                            0L,
                            saved.localCalculatedChecksum(),
                            saved.lastError(),
                            saved.lastVerifiedAt()
                    );
                }
                if (saved.downloadStatus() == ModelDownloadStatus.VERIFIED) {
                    return buildProgress(
                            ModelDownloadStatus.MISSING_OR_CORRUPT,
                            ChecksumVerificationStatus.NOT_CHECKED,
                            partBytes,
                            metadata.expectedSizeBytes(),
                            0.0,
                            0L,
                            false,
                            partExists,
                            partBytes,
                            0L,
                            saved.localCalculatedChecksum(),
                            "Previously verified model file is missing from disk.",
                            saved.lastVerifiedAt()
                    );
                }
            }
            return buildProgress(
                    ModelDownloadStatus.NOT_DOWNLOADED,
                    ChecksumVerificationStatus.NOT_CHECKED,
                    partBytes,
                    metadata.expectedSizeBytes(),
                    computePercent(partBytes, metadata.expectedSizeBytes()),
                    0L,
                    false,
                    partExists,
                    partBytes,
                    0L,
                    null,
                    null,
                    null
            );
        }

        Path targetFile = targetOpt.get();
        long actualSize = safeFileSize(targetFile);
        long lastModified = safeLastModifiedMillis(targetFile);

        if (actualSize <= 0L) {
            return buildProgress(
                    ModelDownloadStatus.MISSING_OR_CORRUPT,
                    ChecksumVerificationStatus.MISMATCH,
                    actualSize,
                    metadata.expectedSizeBytes(),
                    0.0,
                    0L,
                    true,
                    partExists,
                    partBytes,
                    actualSize,
                    null,
                    "Model file exists on disk but is empty (0 bytes).",
                    null
            );
        }

        if (metadata.expectedSizeBytes() > 0L && actualSize != metadata.expectedSizeBytes()) {
            return buildProgress(
                    ModelDownloadStatus.MISSING_OR_CORRUPT,
                    ChecksumVerificationStatus.MISMATCH,
                    actualSize,
                    metadata.expectedSizeBytes(),
                    0.0,
                    0L,
                    true,
                    partExists,
                    partBytes,
                    actualSize,
                    null,
                    "Model file size mismatch: expected " + metadata.expectedSizeBytes()
                            + " bytes but found " + actualSize + " bytes.",
                    null
            );
        }

        if (savedStateOpt.isPresent()) {
            ModelInstallState saved = savedStateOpt.get();
            boolean matchesSavedFile = saved.actualSizeBytes() == actualSize
                    && saved.fileLastModifiedMillis() == lastModified
                    && safeEquals(saved.expectedFilename(), metadata.expectedFilename())
                    && safeEquals(saved.revision(), metadata.revision())
                    && safeEquals(saved.upstreamChecksum(), metadata.upstreamChecksum());
            if (matchesSavedFile) {
                ModelDownloadStatus resolvedStatus =
                        saved.verificationStatus() == ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM
                                ? ModelDownloadStatus.VERIFIED
                                : ModelDownloadStatus.MISSING_OR_CORRUPT;
                return buildProgress(
                        resolvedStatus,
                        saved.verificationStatus(),
                        actualSize,
                        metadata.expectedSizeBytes() > 0 ? metadata.expectedSizeBytes() : actualSize,
                        resolvedStatus == ModelDownloadStatus.VERIFIED ? 100.0 : 0.0,
                        0L,
                        true,
                        partExists,
                        partBytes,
                        actualSize,
                        saved.localCalculatedChecksum(),
                        saved.lastError(),
                        saved.lastVerifiedAt()
                );
            }
        }

        // File exists on disk without matching persisted install-state (e.g., manual download): verify now.
        ModelInstallState verified = verifyAndPersistTargetFile(targetFile);
        return progressFromInstallState(verified, true, partExists, partBytes);
    }

    /**
     * Verifies an existing file on disk and persists the result in {@code install-state.json}.
     */
    public ModelInstallState verifyAndPersistTargetFile(Path targetFile) {
        try {
            Sha256ChecksumCalculator.VerificationResult result = checksumCalculator.verifyFile(
                    targetFile,
                    metadata.upstreamChecksum(),
                    metadata.expectedSizeBytes(),
                    true,
                    () -> false
            );
            ModelDownloadStatus status = result.verifiedAgainstUpstream()
                    ? ModelDownloadStatus.VERIFIED
                    : ModelDownloadStatus.MISSING_OR_CORRUPT;

            ModelInstallState state = new ModelInstallState(
                    metadata.modelName(),
                    metadata.revision(),
                    metadata.expectedFilename(),
                    metadata.expectedSizeBytes(),
                    result.actualSizeBytes(),
                    safeLastModifiedMillis(targetFile),
                    metadata.checksumAlgorithm(),
                    metadata.upstreamChecksum(),
                    metadata.upstreamChecksumSource(),
                    result.localCalculatedSha256(),
                    result.status(),
                    status,
                    Instant.now(),
                    result.failureReason()
            );
            writeInstallState(state);
            return state;
        } catch (IOException ex) {
            ModelInstallState state = new ModelInstallState(
                    metadata.modelName(),
                    metadata.revision(),
                    metadata.expectedFilename(),
                    metadata.expectedSizeBytes(),
                    safeFileSize(targetFile),
                    safeLastModifiedMillis(targetFile),
                    metadata.checksumAlgorithm(),
                    metadata.upstreamChecksum(),
                    metadata.upstreamChecksumSource(),
                    null,
                    ChecksumVerificationStatus.NOT_CHECKED,
                    ModelDownloadStatus.MISSING_OR_CORRUPT,
                    Instant.now(),
                    "Verification failed: " + ex.getMessage()
            );
            safeWriteInstallState(state);
            return state;
        }
    }

    /**
     * Starts an asynchronous download into OS AppData {@code .part} file with Range resume support.
     */
    public CompletableFuture<ModelInstallState> startDownloadAsync(boolean forceRedownload) {
        synchronized (lifecycleLock) {
            if (activeDownloadFuture != null && !activeDownloadFuture.isDone()) {
                return activeDownloadFuture;
            }

            if (!metadata.isSafeFilename()) {
                throw new IllegalArgumentException("Model metadata has an invalid expectedFilename.");
            }
            String urlString = metadata.resolvedDownloadUrl()
                    .orElseThrow(() -> new IllegalStateException("No download URL configured for model."));
            validateDownloadUri(URI.create(urlString));

            if (!forceRedownload) {
                ModelDownloadProgress currentStatus = inspectStatus();
                if (currentStatus.status() == ModelDownloadStatus.VERIFIED) {
                    ModelInstallState alreadyVerified = readInstallState()
                            .orElseGet(() -> verifyAndPersistTargetFile(resolveAppDataTargetPath()));
                    activeProgress.set(currentStatus);
                    return CompletableFuture.completedFuture(alreadyVerified);
                }
            }

            cancelRequested.set(false);
            Instant startedAt = Instant.now();
            long initialPartSize = forceRedownload ? 0L : safeFileSize(resolvePartPath());
            long initialTotal = metadata.expectedSizeBytes() > 0 ? metadata.expectedSizeBytes() : -1L;

            activeProgress.set(buildProgress(
                    ModelDownloadStatus.DOWNLOADING,
                    ChecksumVerificationStatus.NOT_CHECKED,
                    initialPartSize,
                    initialTotal,
                    computePercent(initialPartSize, initialTotal),
                    0L,
                    Files.isRegularFile(resolveAppDataTargetPath()),
                    initialPartSize > 0,
                    initialPartSize,
                    0L,
                    null,
                    null,
                    startedAt
            ));

            activeDownloadFuture = CompletableFuture.supplyAsync(
                    () -> executeDownload(URI.create(urlString), forceRedownload, startedAt),
                    downloadExecutor
            );
            return activeDownloadFuture;
        }
    }

    /**
     * Requests cancellation of an in-flight download, closing any open HTTP response stream.
     */
    public ModelDownloadProgress cancelDownload() {
        cancelRequested.set(true);
        CompletableFuture<HttpResponse<InputStream>> httpFuture = activeHttpFuture;
        if (httpFuture != null) {
            httpFuture.cancel(true);
        }
        InputStream stream = activeStream;
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // Stream close unblocks pending read cleanly
            }
        }
        CompletableFuture<ModelInstallState> future = activeDownloadFuture;
        if (future != null && !future.isDone()) {
            try {
                future.get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Return current snapshot cleanly
            }
        }
        return inspectStatus();
    }

    public boolean isDownloadInProgress() {
        CompletableFuture<ModelInstallState> future = activeDownloadFuture;
        return future != null && !future.isDone();
    }

    private ModelInstallState executeDownload(URI uri, boolean forceRedownload, Instant startedAt) {
        Path partPath = resolvePartPath();
        Path targetPath = resolveAppDataTargetPath();

        try {
            Files.createDirectories(appDataModelDir);

            if (forceRedownload) {
                Files.deleteIfExists(partPath);
                Files.deleteIfExists(targetPath);
            }

            long existingPartBytes = Files.isRegularFile(partPath) ? Files.size(partPath) : 0L;
            if (metadata.expectedSizeBytes() > 0L && existingPartBytes > metadata.expectedSizeBytes()) {
                Files.deleteIfExists(partPath);
                existingPartBytes = 0L;
            }

            if (metadata.expectedSizeBytes() > 0L && existingPartBytes == metadata.expectedSizeBytes()) {
                return verifyAndFinalizePart(partPath, targetPath, startedAt);
            }

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofMinutes(30))
                    .header("User-Agent", "AI-DSA-Mentor/0.1.0");

            if (existingPartBytes > 0L) {
                reqBuilder.header("Range", "bytes=" + existingPartBytes + "-");
            }

            CompletableFuture<HttpResponse<InputStream>> responseFuture = httpClient.sendAsync(
                    reqBuilder.GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            this.activeHttpFuture = responseFuture;
            HttpResponse<InputStream> response = responseFuture.get();

            int statusCode = response.statusCode();
            try (InputStream body = response.body()) {
                this.activeStream = body;

                if (statusCode == 416 && existingPartBytes > 0L) {
                    // Range not satisfiable: reset corrupt .part and retry once from byte 0
                    Files.deleteIfExists(partPath);
                    return retryFromScratch(uri, partPath, targetPath, startedAt);
                }

                if (statusCode < 200 || statusCode >= 300) {
                    String err = "Download failed with HTTP status " + statusCode;
                    return recordTerminalFailure(ModelDownloadStatus.FAILED, ChecksumVerificationStatus.NOT_CHECKED,
                            existingPartBytes, metadata.expectedSizeBytes(), null, err, startedAt);
                }

                boolean appendMode = false;
                long startOffset = 0L;
                long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                long totalBytes = metadata.expectedSizeBytes() > 0L
                        ? metadata.expectedSizeBytes()
                        : (contentLength > 0 ? contentLength : -1L);

                if (statusCode == 206 && existingPartBytes > 0L) {
                    Optional<String> contentRange = response.headers().firstValue("Content-Range");
                    if (isMatchingContentRange(contentRange.orElse(null), existingPartBytes)) {
                        appendMode = true;
                        startOffset = existingPartBytes;
                        totalBytes = parseTotalFromContentRange(contentRange.orElse(null))
                                .orElse(contentLength > 0 ? existingPartBytes + contentLength : totalBytes);
                    } else {
                        Files.deleteIfExists(partPath);
                    }
                } else if (statusCode == 200 && existingPartBytes > 0L) {
                    // Server ignored Range and returned full file from byte 0
                    Files.deleteIfExists(partPath);
                }

                streamToPartFile(body, partPath, appendMode, startOffset, totalBytes, startedAt);
            } finally {
                this.activeStream = null;
                this.activeHttpFuture = null;
            }

            if (cancelRequested.get()) {
                long partial = safeFileSize(partPath);
                return recordCancelled(partial, metadata.expectedSizeBytes(), startedAt);
            }

            return verifyAndFinalizePart(partPath, targetPath, startedAt);
        } catch (IOException ex) {
            if (cancelRequested.get()) {
                long partial = safeFileSize(partPath);
                return recordCancelled(partial, metadata.expectedSizeBytes(), startedAt);
            }
            long partial = safeFileSize(partPath);
            return recordTerminalFailure(ModelDownloadStatus.FAILED, ChecksumVerificationStatus.NOT_CHECKED,
                    partial, metadata.expectedSizeBytes(), null,
                    "Download I/O error: " + ex.getMessage(), startedAt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            long partial = safeFileSize(partPath);
            if (cancelRequested.get()) {
                return recordCancelled(partial, metadata.expectedSizeBytes(), startedAt);
            }
            return recordTerminalFailure(ModelDownloadStatus.FAILED, ChecksumVerificationStatus.NOT_CHECKED,
                    partial, metadata.expectedSizeBytes(), null,
                    "Download interrupted: " + ex.getMessage(), startedAt);
        } catch (Exception ex) {
            long partial = safeFileSize(partPath);
            return recordTerminalFailure(ModelDownloadStatus.FAILED, ChecksumVerificationStatus.NOT_CHECKED,
                    partial, metadata.expectedSizeBytes(), null,
                    "Unexpected download failure: " + ex.getMessage(), startedAt);
        }
    }

    private ModelInstallState retryFromScratch(URI uri, Path partPath, Path targetPath, Instant startedAt)
            throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(30))
                .header("User-Agent", "AI-DSA-Mentor/0.1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            this.activeStream = body;
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return recordTerminalFailure(ModelDownloadStatus.FAILED, ChecksumVerificationStatus.NOT_CHECKED,
                        0L, metadata.expectedSizeBytes(), null,
                        "Download retry failed with HTTP status " + response.statusCode(), startedAt);
            }
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            long totalBytes = metadata.expectedSizeBytes() > 0L ? metadata.expectedSizeBytes() : contentLength;
            streamToPartFile(body, partPath, false, 0L, totalBytes, startedAt);
        } finally {
            this.activeStream = null;
        }
        if (cancelRequested.get()) {
            return recordCancelled(safeFileSize(partPath), metadata.expectedSizeBytes(), startedAt);
        }
        return verifyAndFinalizePart(partPath, targetPath, startedAt);
    }

    private void streamToPartFile(
            InputStream body,
            Path partPath,
            boolean append,
            long startOffset,
            long totalBytes,
            Instant startedAt
    ) throws IOException {
        StandardOpenOption[] options = append
                ? new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND}
                : new StandardOpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};

        long downloaded = startOffset;
        long sessionBytes = 0L;
        long startNanos = System.nanoTime();
        long lastReportNanos = startNanos;
        byte[] buffer = new byte[BUFFER_SIZE];

        try (OutputStream out = Files.newOutputStream(partPath, options)) {
            int read;
            boolean firstChunk = true;
            while (!cancelRequested.get() && (read = body.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloaded += read;
                sessionBytes += read;

                long nowNanos = System.nanoTime();
                if (firstChunk || nowNanos - lastReportNanos >= 150_000_000L) {
                    firstChunk = false;
                    long elapsedNanos = Math.max(1L, nowNanos - startNanos);
                    long bps = (sessionBytes * 1_000_000_000L) / elapsedNanos;
                    activeProgress.set(buildProgress(
                            ModelDownloadStatus.DOWNLOADING,
                            ChecksumVerificationStatus.NOT_CHECKED,
                            downloaded,
                            totalBytes,
                            computePercent(downloaded, totalBytes),
                            bps,
                            false,
                            true,
                            downloaded,
                            0L,
                            null,
                            null,
                            startedAt
                    ));
                    lastReportNanos = nowNanos;
                }
            }
            out.flush();
        }
    }

    private ModelInstallState verifyAndFinalizePart(Path partPath, Path targetPath, Instant startedAt)
            throws IOException {
        long partBytes = safeFileSize(partPath);
        long total = metadata.expectedSizeBytes() > 0L ? metadata.expectedSizeBytes() : partBytes;
        activeProgress.set(buildProgress(
                ModelDownloadStatus.VERIFYING,
                ChecksumVerificationStatus.NOT_CHECKED,
                partBytes,
                total,
                computePercent(partBytes, total),
                0L,
                false,
                true,
                partBytes,
                0L,
                null,
                null,
                startedAt
        ));

        Sha256ChecksumCalculator.VerificationResult result = checksumCalculator.verifyFile(
                partPath,
                metadata.upstreamChecksum(),
                metadata.expectedSizeBytes(),
                true,
                cancelRequested::get
        );

        if (cancelRequested.get()) {
            return recordCancelled(partBytes, total, startedAt);
        }

        if (result.status() == ChecksumVerificationStatus.UNVERIFIED_NO_UPSTREAM_CHECKSUM) {
            return recordTerminalFailure(
                    ModelDownloadStatus.FAILED,
                    ChecksumVerificationStatus.UNVERIFIED_NO_UPSTREAM_CHECKSUM,
                    partBytes,
                    total,
                    result.localCalculatedSha256(),
                    result.failureReason(),
                    startedAt
            );
        }

        if (!result.verifiedAgainstUpstream()) {
            // Remove corrupt staging file so the next download starts cleanly rather than appending to corrupt bytes
            Files.deleteIfExists(partPath);
            return recordTerminalFailure(
                    ModelDownloadStatus.MISSING_OR_CORRUPT,
                    ChecksumVerificationStatus.MISMATCH,
                    0L,
                    total,
                    result.localCalculatedSha256(),
                    result.failureReason(),
                    startedAt
            );
        }

        try {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(partPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        ModelInstallState state = new ModelInstallState(
                metadata.modelName(),
                metadata.revision(),
                metadata.expectedFilename(),
                metadata.expectedSizeBytes(),
                result.actualSizeBytes(),
                safeLastModifiedMillis(targetPath),
                metadata.checksumAlgorithm(),
                metadata.upstreamChecksum(),
                metadata.upstreamChecksumSource(),
                result.localCalculatedSha256(),
                ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM,
                ModelDownloadStatus.VERIFIED,
                Instant.now(),
                null
        );
        writeInstallState(state);

        activeProgress.set(buildProgress(
                ModelDownloadStatus.VERIFIED,
                ChecksumVerificationStatus.VERIFIED_AGAINST_UPSTREAM,
                result.actualSizeBytes(),
                total,
                100.0,
                0L,
                true,
                false,
                0L,
                result.actualSizeBytes(),
                result.localCalculatedSha256(),
                null,
                startedAt
        ));
        return state;
    }

    private ModelInstallState recordCancelled(long partialBytes, long totalBytes, Instant startedAt) {
        String message = partialBytes > 0
                ? "Download cancelled by user. Partial file preserved for resume (" + partialBytes + " bytes)."
                : "Download cancelled by user.";
        ModelInstallState state = new ModelInstallState(
                metadata.modelName(),
                metadata.revision(),
                metadata.expectedFilename(),
                metadata.expectedSizeBytes(),
                0L,
                0L,
                metadata.checksumAlgorithm(),
                metadata.upstreamChecksum(),
                metadata.upstreamChecksumSource(),
                null,
                ChecksumVerificationStatus.NOT_CHECKED,
                ModelDownloadStatus.CANCELLED,
                Instant.now(),
                message
        );
        safeWriteInstallState(state);
        activeProgress.set(buildProgress(
                ModelDownloadStatus.CANCELLED,
                ChecksumVerificationStatus.NOT_CHECKED,
                partialBytes,
                totalBytes,
                computePercent(partialBytes, totalBytes),
                0L,
                Files.isRegularFile(resolveAppDataTargetPath()),
                partialBytes > 0,
                partialBytes,
                0L,
                null,
                message,
                startedAt
        ));
        return state;
    }

    private ModelInstallState recordTerminalFailure(
            ModelDownloadStatus status,
            ChecksumVerificationStatus verificationStatus,
            long partialBytes,
            long totalBytes,
            String localSha256,
            String errorMessage,
            Instant startedAt
    ) {
        ModelInstallState state = new ModelInstallState(
                metadata.modelName(),
                metadata.revision(),
                metadata.expectedFilename(),
                metadata.expectedSizeBytes(),
                0L,
                0L,
                metadata.checksumAlgorithm(),
                metadata.upstreamChecksum(),
                metadata.upstreamChecksumSource(),
                localSha256,
                verificationStatus,
                status,
                Instant.now(),
                errorMessage
        );
        safeWriteInstallState(state);
        activeProgress.set(buildProgress(
                status,
                verificationStatus,
                partialBytes,
                totalBytes,
                computePercent(partialBytes, totalBytes),
                0L,
                Files.isRegularFile(resolveAppDataTargetPath()),
                partialBytes > 0,
                partialBytes,
                0L,
                localSha256,
                errorMessage,
                startedAt
        ));
        return state;
    }

    private ModelDownloadProgress progressFromInstallState(
            ModelInstallState state,
            boolean binaryPresent,
            boolean partExists,
            long partBytes
    ) {
        long total = metadata.expectedSizeBytes() > 0 ? metadata.expectedSizeBytes() : state.actualSizeBytes();
        long downloaded = state.downloadStatus() == ModelDownloadStatus.VERIFIED ? state.actualSizeBytes() : partBytes;
        return buildProgress(
                state.downloadStatus(),
                state.verificationStatus(),
                downloaded,
                total,
                state.downloadStatus() == ModelDownloadStatus.VERIFIED ? 100.0 : computePercent(partBytes, total),
                0L,
                binaryPresent,
                partExists,
                partBytes,
                state.actualSizeBytes(),
                state.localCalculatedChecksum(),
                state.lastError(),
                state.lastVerifiedAt()
        );
    }

    private ModelDownloadProgress buildProgress(
            ModelDownloadStatus status,
            ChecksumVerificationStatus verificationStatus,
            long bytesDownloaded,
            long totalBytes,
            double progressPercent,
            long bytesPerSecond,
            boolean binaryPresent,
            boolean partPresent,
            long partBytes,
            long actualSizeBytes,
            String localSha256,
            String errorMessage,
            Instant startedAt
    ) {
        return new ModelDownloadProgress(
                status,
                verificationStatus,
                bytesDownloaded,
                totalBytes,
                progressPercent,
                bytesPerSecond,
                binaryPresent,
                partPresent,
                partBytes,
                actualSizeBytes,
                localSha256,
                metadata.upstreamChecksum(),
                metadata.upstreamChecksumSource(),
                errorMessage,
                startedAt,
                Instant.now()
        );
    }

    private void validateDownloadUri(URI uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ("https".equals(scheme)) {
            return;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (allowInsecureLoopbackHttpForTests && "http".equals(scheme)
                && ("127.0.0.1".equals(host) || "localhost".equals(host) || "::1".equals(host))) {
            return;
        }
        throw new IllegalArgumentException("Model download URL must use HTTPS (rejected: " + uri + ")");
    }

    private static boolean isMatchingContentRange(String header, long expectedStart) {
        if (header == null || header.isBlank()) {
            return false;
        }
        String normalized = header.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("bytes " + expectedStart + "-");
    }

    private static Optional<Long> parseTotalFromContentRange(String header) {
        if (header == null) {
            return Optional.empty();
        }
        int slash = header.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= header.length()) {
            return Optional.empty();
        }
        String totalPart = header.substring(slash + 1).trim();
        if ("*".equals(totalPart)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(totalPart));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<ModelInstallState> readInstallState() {
        Path statePath = resolveInstallStatePath();
        if (!Files.isRegularFile(statePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(statePath.toFile(), ModelInstallState.class));
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private void writeInstallState(ModelInstallState state) throws IOException {
        Files.createDirectories(appDataModelDir);
        Path statePath = resolveInstallStatePath();
        Path tmp = appDataModelDir.resolve(INSTALL_STATE_FILENAME + ".tmp");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), state);
        try {
            Files.move(tmp, statePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tmp, statePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void safeWriteInstallState(ModelInstallState state) {
        try {
            writeInstallState(state);
        } catch (IOException ignored) {
            // Do not mask the primary failure
        }
    }

    private static long safeFileSize(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.size(path) : 0L;
        } catch (IOException ex) {
            return 0L;
        }
    }

    private static long safeLastModifiedMillis(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
        } catch (IOException ex) {
            return 0L;
        }
    }

    private static boolean safeEquals(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        return left.equalsIgnoreCase(right);
    }

    private static double computePercent(long downloaded, long total) {
        if (total <= 0L || downloaded <= 0L) {
            return 0.0;
        }
        return Math.min(100.0, Math.round((downloaded * 1000.0) / total) / 10.0);
    }

    @Override
    public void close() {
        cancelDownload();
        downloadExecutor.shutdownNow();
    }
}
