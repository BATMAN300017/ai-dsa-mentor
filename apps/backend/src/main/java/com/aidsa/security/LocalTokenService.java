package com.aidsa.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates and verifies a per-process ephemeral local token used to authenticate
 * future mutating desktop and browser-extension requests.
 */
@Service
public class LocalTokenService {

    public static final String TOKEN_HEADER = "X-Mentor-Local-Token";

    private final String ephemeralToken;

    public LocalTokenService() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        this.ephemeralToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public boolean isValid(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                ephemeralToken.getBytes(StandardCharsets.UTF_8),
                candidate.trim().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Exposed strictly for local desktop/process coordination, never logged or returned on public HTTP routes.
     */
    public String currentToken() {
        return ephemeralToken;
    }
}
