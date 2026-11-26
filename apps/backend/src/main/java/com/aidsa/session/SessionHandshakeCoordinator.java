package com.aidsa.session;

import org.springframework.stereotype.Component;

/**
 * Coordinates backend session operations with {@link SessionStorePort} from {@code modules/session-manager}.
 */
@Component
public class SessionHandshakeCoordinator {

    private final SessionStorePort sessionStorePort;

    public SessionHandshakeCoordinator(SessionStorePort sessionStorePort) {
        this.sessionStorePort = sessionStorePort;
    }

    public int activeSessionCount() {
        return sessionStorePort.count();
    }
}
