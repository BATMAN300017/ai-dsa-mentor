package com.aidsa.modelmanager;

/**
 * Download and verification lifecycle states for local GGUF models.
 */
public enum ModelDownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFIED,
    MISSING_OR_CORRUPT
}
