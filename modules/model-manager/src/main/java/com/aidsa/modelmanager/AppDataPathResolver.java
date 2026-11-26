package com.aidsa.modelmanager;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Resolves user-writable OS application-data directories for models, runtimes, and logs.
 * Packaged desktop applications should store downloaded GGUF weights under app-data rather
 * than inside read-only installation folders.
 */
public class AppDataPathResolver {

    private static final String APP_DISPLAY_DIR = "AI DSA Mentor";
    private static final String APP_LINUX_DIR = "ai-dsa-mentor";

    public Path resolveAppDataRoot() {
        return resolveAppDataRoot(
                System.getProperty("os.name", ""),
                System.getProperty("user.home", "."),
                System.getenv("LOCALAPPDATA"),
                System.getenv("XDG_DATA_HOME")
        );
    }

    public Path resolveAppDataRoot(String osName, String userHome, String localAppData, String xdgDataHome) {
        String normalizedOs = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalizedOs.contains("win")) {
            if (localAppData != null && !localAppData.isBlank()) {
                return Path.of(localAppData, APP_DISPLAY_DIR);
            }
            return Path.of(userHome, "AppData", "Local", APP_DISPLAY_DIR);
        }
        if (normalizedOs.contains("mac") || normalizedOs.contains("darwin")) {
            return Path.of(userHome, "Library", "Application Support", APP_DISPLAY_DIR);
        }
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Path.of(xdgDataHome, APP_LINUX_DIR);
        }
        return Path.of(userHome, ".local", "share", APP_LINUX_DIR);
    }

    public Path resolveModelsDirectory() {
        return resolveAppDataRoot().resolve("models");
    }

    public Path resolveModelDirectory(String modelSlug) {
        return resolveModelsDirectory().resolve(requireSafeSingleSegment(modelSlug, "modelSlug"));
    }

    public Path resolveRuntimesDirectory() {
        return resolveAppDataRoot().resolve("runtimes");
    }

    public Path resolveRuntimeDirectory(String runtimeName) {
        return resolveRuntimesDirectory().resolve(requireSafeSingleSegment(runtimeName, "runtimeName"));
    }

    public Path resolveLogsDirectory() {
        return resolveAppDataRoot().resolve("logs");
    }

    private static String requireSafeSingleSegment(String segment, String label) {
        if (segment == null || segment.isBlank()
                || segment.contains("/")
                || segment.contains("\\")
                || segment.contains("..")) {
            throw new IllegalArgumentException("Invalid " + label + ": must be a single path segment");
        }
        return segment.trim();
    }
}
