package com.aidsa.model;

/**
 * Describes a planned or foundational subsystem and its current status.
 */
public record CapabilityDescriptor(
        String id,
        String name,
        CapabilityState state,
        String notes
) {
}
