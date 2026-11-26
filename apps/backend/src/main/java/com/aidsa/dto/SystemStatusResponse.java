package com.aidsa.dto;

import com.aidsa.model.CapabilityDescriptor;

import java.util.List;

/**
 * Read-only foundation status payload returned by {@code GET /api/v1/system/status}.
 */
public record SystemStatusResponse(
        String application,
        String version,
        String philosophy,
        String bindScope,
        boolean modelLoaded,
        ModelDescriptorDto model,
        List<CapabilityDescriptor> capabilities
) {
}
