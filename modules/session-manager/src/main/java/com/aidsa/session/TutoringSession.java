package com.aidsa.session;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a local tutoring session tracking problem context without leaking code remotely.
 */
public record TutoringSession(
        UUID id,
        String problemKey,
        SessionState state,
        Instant createdAt
) {
}
