package com.aidsa.service;

import com.aidsa.ai.InferenceDetailedResponse;
import com.aidsa.ai.InferenceParameters;
import com.aidsa.ai.InferenceRequest;
import com.aidsa.ai.LlamaCppInferenceProvider;
import com.aidsa.dto.AiInferRequest;
import com.aidsa.dto.AiInferResponse;
import com.aidsa.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Executes direct local model inference via {@link LlamaCppInferenceProvider} without
 * injecting Task 3 DSA tutoring policies.
 */
@Service
public class AiInferenceService {

    private final LlamaCppInferenceProvider inferenceProvider;

    public AiInferenceService(LlamaCppInferenceProvider inferenceProvider) {
        this.inferenceProvider = inferenceProvider;
    }

    public AiInferResponse infer(AiInferRequest request) {
        int maxTokens = request.maxTokens() != null ? request.maxTokens() : 256;
        double temperature = request.temperature() != null ? request.temperature() : 0.2;
        double topP = request.topP() != null ? request.topP() : 0.95;

        InferenceRequest engineRequest = new InferenceRequest(
                request.systemPrompt(),
                request.userPrompt().trim(),
                maxTokens,
                temperature
        );
        InferenceParameters params = new InferenceParameters(topP, 1.05, List.of(), null);

        try {
            InferenceDetailedResponse result = inferenceProvider.generateDetailed(engineRequest, params);
            return new AiInferResponse(
                    result.text(),
                    result.modelName(),
                    result.latencyMillis(),
                    result.promptTokens(),
                    result.completionTokens(),
                    result.finishReason()
            );
        } catch (IllegalStateException ex) {
            if (ex instanceof com.aidsa.ai.ModelRuntimeNotConfiguredException) {
                throw ex;
            }
            String lower = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
            if (lower.contains("timed out")) {
                throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }
    }
}
