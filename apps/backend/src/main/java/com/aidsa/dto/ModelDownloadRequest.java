package com.aidsa.dto;

/**
 * Optional request body for {@code POST /api/v1/model/download}.
 */
public record ModelDownloadRequest(
        boolean forceRedownload
) {
}
