package com.aidsa.controller;

import com.aidsa.dto.SystemStatusResponse;
import com.aidsa.service.SystemStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes read-only Task 1 system & model readiness telemetry.
 */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final SystemStatusService systemStatusService;

    public SystemController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> status() {
        return ResponseEntity.ok(systemStatusService.getSystemStatus());
    }
}
