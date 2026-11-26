package com.aidsa.session;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory session registry for local Task 1 foundation state.
 */
public class InMemorySessionStore implements SessionStorePort {

    private final Map<UUID, TutoringSession> sessions = new ConcurrentHashMap<>();

    @Override
    public TutoringSession save(TutoringSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<TutoringSession> findById(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public int count() {
        return sessions.size();
    }
}
