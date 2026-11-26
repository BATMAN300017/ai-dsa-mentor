package com.aidsa.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Generates and verifies high-entropy local session tokens used to authenticate
 * mutating desktop, web UI, and paired browser-extension requests.
 */
@Service
public class LocalTokenService {

    public static final String TOKEN_HEADER = "X-Mentor-Local-Token";
    public static final String CLIENT_HEADER = "X-Mentor-Client";
    public static final String EXPECTED_WEB_CLIENT = "ai-dsa-mentor-ui";
    private static final int MAX_ISSUED_UI_TOKENS = 64;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String ephemeralToken;
    private final Queue<String> uiHandshakeTokens = new ConcurrentLinkedQueue<>();

    public LocalTokenService() {
        this.ephemeralToken = generateToken();
    }

    public boolean isValid(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.trim();
        if (constantTimeEquals(ephemeralToken, trimmed)) {
            return true;
        }
        for (String issued : uiHandshakeTokens) {
            if (constantTimeEquals(issued, trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Issues a scoped UI handshake token for local allowlisted browser origins after
     * CORS/Origin/Fetch-Metadata verification. Never returns {@link #currentToken()}.
     */
    public String issueUiHandshakeToken() {
        String token = generateToken();
        uiHandshakeTokens.add(token);
        while (uiHandshakeTokens.size() > MAX_ISSUED_UI_TOKENS) {
            uiHandshakeTokens.poll();
        }
        return token;
    }

    /**
     * Exposed strictly for local desktop/process coordination, never logged or returned on public HTTP routes.
     */
    public String currentToken() {
        return ephemeralToken;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
