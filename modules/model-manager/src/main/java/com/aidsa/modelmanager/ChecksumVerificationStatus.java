package com.aidsa.modelmanager;

/**
 * Integrity verification status distinguishing trusted upstream checksums from unverified local hashes.
 */
public enum ChecksumVerificationStatus {
    NOT_CHECKED,
    VERIFIED_AGAINST_UPSTREAM,
    UNVERIFIED_NO_UPSTREAM_CHECKSUM,
    MISMATCH
}
