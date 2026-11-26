package com.aidsa.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Abstraction for spawning {@code llama-server} OS processes via {@link ProcessBuilder}.
 * Allows deterministic unit testing with fake {@link Process} instances.
 */
@FunctionalInterface
public interface ProcessStarter {

    Process start(List<String> command, Path workingDirectory, Path logFile) throws IOException;

    static ProcessStarter systemDefault() {
        return (command, workingDirectory, logFile) -> {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (workingDirectory != null && Files.isDirectory(workingDirectory)) {
                pb.directory(workingDirectory.toFile());
            }
            pb.redirectErrorStream(true);
            if (logFile != null) {
                if (logFile.getParent() != null) {
                    Files.createDirectories(logFile.getParent());
                }
                pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
            } else {
                pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            }
            return pb.start();
        };
    }
}
