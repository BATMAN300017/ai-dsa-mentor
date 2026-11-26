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
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelMetadataReaderTest {

    private final ModelMetadataReader reader = new ModelMetadataReader(new ObjectMapper());

    @Test
    void readsConfiguredModelInfoJson(@TempDir Path tempDir) throws IOException {
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
                  "checksum": ""
                }
                """);

        Optional<ModelMetadata> loaded = reader.readFromFile(descriptor);
        assertTrue(loaded.isPresent());
        assertEquals("Qwen2.5-Coder-3B-Instruct", loaded.get().modelName());
        assertEquals("Q4_K_M", loaded.get().expectedQuantization());
        assertEquals("NOT_DOWNLOADED", loaded.get().downloadStatus());
        assertFalse(loaded.get().hasVerifiedChecksum());
        assertFalse(reader.isModelBinaryPresent(loaded.get(), tempDir, tempDir.resolve("appdata")));
    }

    @Test
    void resolvesCrossPlatformAppDataDirectories() {
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
    }
}
