package com.aidsa.desktop;

import com.aidsa.modelmanager.AppDataPathResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages single-instance startup and clean shutdown of the local Spring Boot backend.
 * If an AI DSA Mentor backend instance is already listening on the configured loopback port,
 * it is reused rather than spawning a duplicate JVM process.
 */
public class BackendLauncher {

    public static final int DEFAULT_PORT = 8787;
    private static final String LOOPBACK_HOST = "127.0.0.1";

    private final int port;
    private final HttpClient httpClient;
    private final AppDataPathResolver appDataPathResolver;

    private volatile Process managedProcess;
    private volatile boolean ownedByThisLauncher;

    public BackendLauncher() {
        this(resolveConfiguredPort(), new AppDataPathResolver());
    }

    public BackendLauncher(int port, AppDataPathResolver appDataPathResolver) {
        this.port = port;
        this.appDataPathResolver = appDataPathResolver;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public int getPort() {
        return port;
    }

    public String getBaseUrl() {
        return "http://" + LOOPBACK_HOST + ":" + port;
    }

    public boolean isOwnedByThisLauncher() {
        return ownedByThisLauncher;
    }

    /**
     * Ensures the local backend is running without starting duplicate processes.
     */
    public synchronized LaunchResult ensureRunning(Duration startupTimeout) throws IOException, InterruptedException {
        if (isBackendHealthy()) {
            this.ownedByThisLauncher = false;
            return new LaunchResult(true, false, getBaseUrl(), "Attached to existing local backend instance.");
        }

        if (isPortInUse()) {
            return new LaunchResult(
                    false,
                    false,
                    getBaseUrl(),
                    "Port " + port + " is already occupied by another process that is not AI DSA Mentor."
            );
        }

        ProcessBuilder builder = createBackendProcessBuilder()
                .orElseThrow(() -> new IOException(
                        "Unable to locate backend JAR. Build the project first with './mvnw -pl apps/backend -am package'."
                ));

        Path logsDir = appDataPathResolver.resolveLogsDirectory();
        Files.createDirectories(logsDir);
        Path backendLog = logsDir.resolve("backend-desktop.log");
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(backendLog.toFile()));

        this.managedProcess = builder.start();
        this.ownedByThisLauncher = true;

        long deadlineNanos = System.nanoTime() + startupTimeout.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            if (!managedProcess.isAlive()) {
                return new LaunchResult(
                        false,
                        true,
                        getBaseUrl(),
                        "Backend process exited early with code " + managedProcess.exitValue()
                                + ". Inspect log: " + backendLog
                );
            }
            if (isBackendHealthy()) {
                return new LaunchResult(true, true, getBaseUrl(), "Started local AI DSA Mentor backend.");
            }
            Thread.sleep(250L);
        }

        shutdownIfOwned();
        return new LaunchResult(false, false, getBaseUrl(), "Timed out waiting for backend health check.");
    }

    public boolean isBackendHealthy() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/v1/health"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200
                    && response.body() != null
                    && response.body().contains("\"AI DSA Mentor\"")
                    && response.body().contains("\"UP\"");
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isPortInUse() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(LOOPBACK_HOST, port), 500);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public synchronized void shutdownIfOwned() {
        if (!ownedByThisLauncher || managedProcess == null) {
            return;
        }
        try {
            if (managedProcess.isAlive()) {
                managedProcess.destroy();
                boolean exited = managedProcess.waitFor(5, TimeUnit.SECONDS);
                if (!exited && managedProcess.isAlive()) {
                    managedProcess.destroyForcibly();
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            if (managedProcess.isAlive()) {
                managedProcess.destroyForcibly();
            }
        } finally {
            managedProcess = null;
            ownedByThisLauncher = false;
        }
    }

    Optional<ProcessBuilder> createBackendProcessBuilder() {
        Optional<Path> jarPath = resolveBackendJar();
        if (jarPath.isEmpty()) {
            return Optional.empty();
        }
        String javaExec = ProcessHandle.current().info().command()
                .orElse(Path.of(System.getProperty("java.home"), "bin", "java").toString());

        ProcessBuilder pb = new ProcessBuilder(
                javaExec,
                "-jar",
                jarPath.get().toAbsolutePath().toString(),
                "--server.port=" + port
        );
        pb.directory(resolveWorkingDir(jarPath.get()).toFile());
        return Optional.of(pb);
    }

    private Optional<Path> resolveBackendJar() {
        String envJar = System.getenv("AIDSA_BACKEND_JAR");
        if (envJar != null && !envJar.isBlank()) {
            Path p = Path.of(envJar);
            if (Files.isRegularFile(p)) {
                return Optional.of(p);
            }
        }
        List<Path> candidates = List.of(
                Path.of("apps", "backend", "target", "backend-0.1.0-SNAPSHOT.jar"),
                Path.of("..", "backend", "target", "backend-0.1.0-SNAPSHOT.jar"),
                Path.of("..", "..", "apps", "backend", "target", "backend-0.1.0-SNAPSHOT.jar")
        );
        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isRegularFile(normalized)) {
                return Optional.of(normalized);
            }
        }
        return Optional.empty();
    }

    private Path resolveWorkingDir(Path jarPath) {
        Path targetDir = jarPath.getParent();
        if (targetDir != null && targetDir.getParent() != null
                && targetDir.getParent().getParent() != null
                && targetDir.getParent().getParent().getParent() != null) {
            Path potentialRoot = targetDir.getParent().getParent().getParent();
            if (Files.isDirectory(potentialRoot.resolve("resources"))) {
                return potentialRoot;
            }
        }
        return Path.of("").toAbsolutePath();
    }

    private static int resolveConfiguredPort() {
        String portProp = System.getProperty("aidsa.backend.port", System.getenv("BACKEND_PORT"));
        if (portProp != null && !portProp.isBlank()) {
            try {
                int parsed = Integer.parseInt(portProp.trim());
                if (parsed > 0 && parsed <= 65535) {
                    return parsed;
                }
            } catch (NumberFormatException ignored) {
                // Fall back to default port
            }
        }
        return DEFAULT_PORT;
    }

    public record LaunchResult(
            boolean ready,
            boolean startedNewProcess,
            String baseUrl,
            String message
    ) {
    }
}
