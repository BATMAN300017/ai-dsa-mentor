package com.aidsa.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemorySessionStoreTest {

    @Test
    void storesAndRetrievesSession() {
        InMemorySessionStore store = new InMemorySessionStore();
        UUID id = UUID.randomUUID();
        TutoringSession session = new TutoringSession(id, "lc-11-container-with-most-water", SessionState.CREATED, Instant.now());

        store.save(session);

        assertEquals(1, store.count());
        assertTrue(store.findById(id).isPresent());
        assertEquals("lc-11-container-with-most-water", store.findById(id).get().problemKey());
    }
}
