package com.aidsa.session;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence abstraction for local mentoring sessions.
 */
public interface SessionStorePort {

    TutoringSession save(TutoringSession session);

    Optional<TutoringSession> findById(UUID sessionId);

    int count();
}
