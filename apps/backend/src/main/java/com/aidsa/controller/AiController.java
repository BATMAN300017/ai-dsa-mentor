package com.aidsa.controller;

import com.aidsa.dto.AiInferRequest;
import com.aidsa.dto.AiInferResponse;
import com.aidsa.service.AiInferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes local AI inference over the loaded {@code llama.cpp} runtime.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiInferenceService aiInferenceService;

    public AiController(AiInferenceService aiInferenceService) {
        this.aiInferenceService = aiInferenceService;
    }

    @PostMapping("/infer")
    public ResponseEntity<AiInferResponse> infer(@Valid @RequestBody AiInferRequest request) {
        return ResponseEntity.ok(aiInferenceService.infer(request));
    }
}
