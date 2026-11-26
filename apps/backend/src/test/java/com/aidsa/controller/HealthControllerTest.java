package com.aidsa.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void isolatedDirs(DynamicPropertyRegistry registry) {
        registry.add("aidsa.model.app-data-dir", () -> tempDir.resolve("models").toString());
        registry.add("aidsa.runtime.app-data-dir", () -> tempDir.resolve("runtimes").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsFoundationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.application").value("AI DSA Mentor"))
                .andExpect(jsonPath("$.version").value("0.1.0"))
                .andExpect(jsonPath("$.modelLoaded").value(false));
    }

    @Test
    void systemStatusEndpointReportsModelMetadataAndUnloadedState() throws Exception {
        mockMvc.perform(get("/api/v1/system/status").header("Host", "127.0.0.1:8787"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelLoaded").value(false))
                .andExpect(jsonPath("$.model.modelName").value("Qwen2.5-Coder-3B-Instruct"))
                .andExpect(jsonPath("$.model.expectedQuantization").value("Q4_K_M"))
                .andExpect(jsonPath("$.model.downloadStatus").value("NOT_DOWNLOADED"));
    }

    @Test
    void rejectsDisallowedRemoteOriginAndNonLoopbackHost() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/health")
                        .header("Host", "evil.example:8787"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsMutatingCallWithoutLocalToken() throws Exception {
        mockMvc.perform(post("/api/v1/health")
                        .header("Host", "127.0.0.1:8787")
                        .header("Origin", "http://127.0.0.1:5173"))
                .andExpect(status().isUnauthorized());
    }
}
