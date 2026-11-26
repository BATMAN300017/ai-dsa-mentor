package com.aidsa.modelmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelMetadataReaderTest {

    private final ModelMetadataReader reader = new ModelMetadataReader(new ObjectMapper());

    @Test
    void readsConfiguredModelInfoJsonAndRejectsUnverifiedLegacyChecksum(@TempDir Path tempDir) throws IOException {
        Path descriptor = tempDir.resolve("model-info.json");
        Files.writeString(descriptor, """
                {
                  "modelName": "Qwen2.5-Coder-3B-Instruct",
                  "modelProvider": "Qwen (Alibaba Cloud)",
                  "huggingFaceRepo": "Qwen/Qwen2.5-Coder-3B-Instruct-GGUF",
                  "expectedQuantization": "Q4_K_M",
                  "expectedFileFormat": "GGUF",
                  "expectedFilename": "qwen2.5-coder-3b-instruct-q4_k_m.gguf",
                  "downloadStatus": "NOT_DOWNLOADED",
                  "checksum": "nonempty-but-not-trusted-upstream"
                }
                """);

        Optional<ModelMetadata> loaded = reader.readFromFile(descriptor);
        assertTrue(loaded.isPresent());
        assertEquals("Qwen2.5-Coder-3B-Instruct", loaded.get().modelName());
        assertEquals("Q4_K_M", loaded.get().expectedQuantization());
        assertEquals("NOT_DOWNLOADED", loaded.get().downloadStatus());
        assertFalse(loaded.get().hasTrustedUpstreamChecksum());
        assertFalse(reader.isModelBinaryPresent(loaded.get(), tempDir, tempDir.resolve("appdata")));
    }

    @Test
    void readsRepositoryModelDescriptorWithPinnedRevisionAndUpstreamChecksum() throws IOException {
        Path repoDescriptor = Path.of("..", "..", "resources", "models", "qwen2.5-coder-3b", "model-info.json")
                .toAbsolutePath().normalize();
        if (!Files.isRegularFile(repoDescriptor)) {
            repoDescriptor = Path.of("resources", "models", "qwen2.5-coder-3b", "model-info.json")
                    .toAbsolutePath().normalize();
        }
        Optional<ModelMetadata> loaded = reader.readFromFile(repoDescriptor);
        assertTrue(loaded.isPresent());
        ModelMetadata meta = loaded.get();
        assertEquals("qwen2.5-coder-3b-instruct-q4_k_m.gguf", meta.expectedFilename());
        assertEquals("f74adce6aa16316c625447af059dbebe4983757c", meta.revision());
        assertEquals(2104932800L, meta.expectedSizeBytes());
        assertTrue(meta.hasTrustedUpstreamChecksum());
        assertTrue(meta.isSafeFilename());
        assertTrue(meta.resolvedDownloadUrl().isPresent());
    }

    @Test
    void resolvesCrossPlatformAppDataDirectoriesAndRejectsPathTraversal() {
        AppDataPathResolver resolver = new AppDataPathResolver();
        assertEquals(
                Path.of("C:\\Users\\Demo\\AppData\\Local", "AI DSA Mentor"),
                resolver.resolveAppDataRoot("Windows 11", "C:\\Users\\Demo", "C:\\Users\\Demo\\AppData\\Local", null)
        );
        assertEquals(
                Path.of("/Users/demo/Library/Application Support", "AI DSA Mentor"),
                resolver.resolveAppDataRoot("Mac OS X", "/Users/demo", null, null)
        );
        assertEquals(
                Path.of("/home/demo/.local/share", "ai-dsa-mentor"),
                resolver.resolveAppDataRoot("Linux", "/home/demo", null, "")
        );
        assertTrue(resolver.resolveRuntimeDirectory("llama.cpp").endsWith(Path.of("runtimes", "llama.cpp")));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveModelDirectory("../escape"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolveRuntimeDirectory("foo/bar"));
    }
}
