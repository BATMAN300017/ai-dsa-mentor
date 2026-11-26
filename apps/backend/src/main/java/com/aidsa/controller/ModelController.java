package com.aidsa.controller;

import com.aidsa.dto.ModelDownloadRequest;
import com.aidsa.dto.ModelLoadRequest;
import com.aidsa.dto.ModelStatusResponse;
import com.aidsa.modelmanager.ModelDownloadStatus;
import com.aidsa.service.ModelManagementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes endpoints for inspecting, downloading, cancelling, loading, and unloading the local GGUF model.
 */
@RestController
@RequestMapping("/api/v1/model")
public class ModelController {

    private final ModelManagementService modelManagementService;

    public ModelController(ModelManagementService modelManagementService) {
        this.modelManagementService = modelManagementService;
    }

    @GetMapping("/status")
    public ResponseEntity<ModelStatusResponse> status() {
        return ResponseEntity.ok(modelManagementService.getStatus());
    }

    @PostMapping("/download")
    public ResponseEntity<ModelStatusResponse> download(
            @Valid @RequestBody(required = false) ModelDownloadRequest request
    ) {
        boolean force = request != null && request.forceRedownload();
        ModelStatusResponse response = modelManagementService.startDownload(force);
        if (response.downloadStatus() == ModelDownloadStatus.VERIFIED && !force) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/download/cancel")
    public ResponseEntity<ModelStatusResponse> cancelDownload() {
        return ResponseEntity.ok(modelManagementService.cancelDownload());
    }

    @PostMapping("/load")
    public ResponseEntity<ModelStatusResponse> load(
            @Valid @RequestBody(required = false) ModelLoadRequest request
    ) {
        ModelStatusResponse response = modelManagementService.loadModel(request);
        if (response.modelLoaded() && !response.modelLoading()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/unload")
    public ResponseEntity<ModelStatusResponse> unload() {
        return ResponseEntity.ok(modelManagementService.unloadModel());
    }
}
