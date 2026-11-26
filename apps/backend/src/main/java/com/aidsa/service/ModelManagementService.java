package com.aidsa.service;

import com.aidsa.ai.LlamaCppInferenceProvider;
import com.aidsa.ai.LlamaRuntimeSnapshot;
import com.aidsa.dto.ModelLoadRequest;
import com.aidsa.dto.ModelStatusResponse;
import com.aidsa.exception.ApiException;
import com.aidsa.modelmanager.ModelDownloadProgress;
import com.aidsa.modelmanager.ModelDownloadStatus;
import com.aidsa.modelmanager.ModelManagerService;
import com.aidsa.modelmanager.ModelMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Coordinates local model downloading, integrity verification, and llama.cpp runtime lifecycle.
 */
@Service
public class ModelManagementService {

    private final ModelManagerService modelManagerService;
    private final LlamaCppInferenceProvider inferenceProvider;

    public ModelManagementService(
            ModelManagerService modelManagerService,
            LlamaCppInferenceProvider inferenceProvider
    ) {
        this.modelManagerService = modelManagerService;
        this.inferenceProvider = inferenceProvider;
    }

    public ModelStatusResponse getStatus() {
        ModelMetadata meta = modelManagerService.getMetadata();
        ModelDownloadProgress progress = modelManagerService.inspectStatus();
        LlamaRuntimeSnapshot runtime = inferenceProvider.snapshot();
        return toResponse(meta, progress, runtime);
    }

    public ModelStatusResponse startDownload(boolean forceRedownload) {
        LlamaRuntimeSnapshot runtime = inferenceProvider.snapshot();
        if (forceRedownload && (runtime.modelLoaded() || runtime.modelLoading())) {
            inferenceProvider.unloadModel();
        }
        modelManagerService.startDownloadAsync(forceRedownload);
        return getStatus();
    }

    public ModelStatusResponse cancelDownload() {
        modelManagerService.cancelDownload();
        return getStatus();
    }

    public ModelStatusResponse loadModel(ModelLoadRequest request) {
        ModelDownloadProgress progress = modelManagerService.inspectStatus();
        if (progress.status() == ModelDownloadStatus.DOWNLOADING || progress.status() == ModelDownloadStatus.VERIFYING) {
            throw new ApiException(HttpStatus.CONFLICT, "Model download is currently in progress. Wait for completion before loading.");
        }

        Path verifiedModel = modelManagerService.resolveVerifiedModelFile()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "Verified GGUF model binary is not installed yet (current status: " + progress.status() + "). Download the model first."
                ));

        Integer ctxSize = request != null ? request.contextSize() : null;
        Integer threads = request != null ? request.threads() : null;
        Integer gpuLayers = request != null ? request.gpuLayers() : null;

        inferenceProvider.loadModelAsync(
                verifiedModel,
                modelManagerService.getMetadata().modelName(),
                ctxSize,
                threads,
                gpuLayers
        );
        return getStatus();
    }

    public ModelStatusResponse unloadModel() {
        inferenceProvider.unloadModel();
        return getStatus();
    }

    private ModelStatusResponse toResponse(
            ModelMetadata meta,
            ModelDownloadProgress progress,
            LlamaRuntimeSnapshot runtime
    ) {
        return new ModelStatusResponse(
                meta.modelName(),
                meta.modelProvider(),
                meta.huggingFaceRepo(),
                meta.huggingFaceUrl(),
                meta.expectedQuantization(),
                meta.expectedFileFormat(),
                meta.expectedFilename(),
                meta.expectedSizeBytes(),
                meta.revision(),
                meta.runtimeTarget(),
                modelManagerService.getAppDataModelDir().toString(),
                progress.status(),
                progress.verificationStatus(),
                progress.upstreamChecksum(),
                progress.upstreamChecksumSource(),
                progress.localCalculatedChecksum(),
                progress.binaryPresentOnDisk(),
                progress.resumablePartPresent(),
                progress.bytesDownloaded(),
                progress.totalBytes(),
                progress.progressPercent(),
                progress.bytesPerSecond(),
                progress.actualSizeBytes(),
                progress.errorMessage(),
                runtime.runtimeAvailable(),
                runtime.runtimeSource(),
                runtime.status(),
                runtime.modelLoaded(),
                runtime.modelLoading(),
                runtime.port(),
                runtime.contextSize(),
                runtime.threads(),
                runtime.gpuLayers(),
                runtime.lastError()
        );
    }
}
