package com.aidsa.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Discovers the local {@code llama-server} binary safely across configured paths,
 * OS application-data runtime directories, workspace runtime directories, and {@code PATH}.
 */
public final class LlamaRuntimeDiscovery {

    public record Resolution(
            boolean available,
            Path executablePath,
            String source,
            String message
    ) {
    }

    private LlamaRuntimeDiscovery() {
    }

    public static Resolution locate(LlamaRuntimeConfig config) {
        return locate(config, System.getProperty("os.name", ""), System.getenv("PATH"));
    }

    public static Resolution locate(LlamaRuntimeConfig config, String osName, String pathEnv) {
        String binaryName = isWindows(osName) ? "llama-server.exe" : "llama-server";

        if (config != null && config.configuredExecutablePath() != null && !config.configuredExecutablePath().isBlank()) {
            Path configured = Path.of(config.configuredExecutablePath().trim()).toAbsolutePath().normalize();
            if (!isAllowedServerBinaryName(configured.getFileName().toString(), binaryName)) {
                return new Resolution(false, null, "configured",
                        "Configured runtime executable must be named '" + binaryName + "': " + configured.getFileName());
            }
            if (isUsableExecutable(configured, osName)) {
                return new Resolution(true, configured, "configured-path",
                        "Found configured llama-server binary.");
            }
            return new Resolution(false, null, "configured",
                    "Configured llama-server binary was not found or is not executable at: " + configured);
        }

        if (config != null && config.appDataRuntimeDir() != null) {
            Optional<Path> inAppData = findInRuntimeDir(config.appDataRuntimeDir(), binaryName, osName);
            if (inAppData.isPresent()) {
                return new Resolution(true, inAppData.get(), "app-data",
                        "Found llama-server inside OS AppData runtime directory.");
            }
        }

        if (config != null && config.workspaceRuntimeDir() != null) {
            Optional<Path> inWorkspace = findInRuntimeDir(config.workspaceRuntimeDir(), binaryName, osName);
            if (inWorkspace.isPresent()) {
                return new Resolution(true, inWorkspace.get(), "workspace",
                        "Found llama-server inside workspace runtime directory.");
            }
        }

        if (pathEnv != null && !pathEnv.isBlank()) {
            String separator = isWindows(osName) ? ";" : ":";
            for (String entry : pathEnv.split(java.util.regex.Pattern.quote(separator))) {
                if (entry == null || entry.isBlank()) {
                    continue;
                }
                Path candidate = Path.of(entry.trim()).resolve(binaryName).toAbsolutePath().normalize();
                if (isUsableExecutable(candidate, osName)) {
                    return new Resolution(true, candidate, "system-path",
                            "Found llama-server on system PATH.");
                }
            }
        }

        return new Resolution(
                false,
                null,
                "none",
                "llama-server binary (" + binaryName + ") was not found. Configure AIDSA_LLAMA_SERVER_PATH or extract an official llama.cpp release into the AppData runtimes/llama.cpp folder."
        );
    }

    private static Optional<Path> findInRuntimeDir(Path rootDir, String binaryName, String osName) {
        if (!Files.isDirectory(rootDir)) {
            return Optional.empty();
        }
        List<Path> directCandidates = new ArrayList<>();
        directCandidates.add(rootDir.resolve(binaryName));
        directCandidates.add(rootDir.resolve("bin").resolve(binaryName));
        directCandidates.add(rootDir.resolve("build").resolve("bin").resolve(binaryName));

        for (Path candidate : directCandidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (isUsableExecutable(normalized, osName)) {
                return Optional.of(normalized);
            }
        }

        try (Stream<Path> children = Files.list(rootDir)) {
            for (Path sub : children.filter(Files::isDirectory).toList()) {
                Path direct = sub.resolve(binaryName).toAbsolutePath().normalize();
                if (isUsableExecutable(direct, osName)) {
                    return Optional.of(direct);
                }
                Path nestedBin = sub.resolve("bin").resolve(binaryName).toAbsolutePath().normalize();
                if (isUsableExecutable(nestedBin, osName)) {
                    return Optional.of(nestedBin);
                }
            }
        } catch (Exception ignored) {
            // Directory unreadable
        }
        return Optional.empty();
    }

    private static boolean isAllowedServerBinaryName(String fileName, String expectedBinaryName) {
        return fileName.equalsIgnoreCase(expectedBinaryName);
    }

    private static boolean isUsableExecutable(Path path, String osName) {
        if (!Files.isRegularFile(path)) {
            return false;
        }
        if (isWindows(osName)) {
            return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe");
        }
        return Files.isExecutable(path);
    }

    private static boolean isWindows(String osName) {
        return osName != null && osName.toLowerCase(Locale.ROOT).contains("win");
    }
}
