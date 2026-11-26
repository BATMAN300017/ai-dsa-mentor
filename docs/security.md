# Security Design

## Threat Model & Local-First Principles

AI DSA Mentor runs entirely on the developer's local machine. Because local HTTP servers on developer workstations can be targeted by malicious web pages (DNS rebinding, cross-site requests from arbitrary tabs, or unauthorized browser extensions), Task 1 establishes strict security defaults from day one.

## Implemented Safeguards (Task 1)

1. **Loopback-Only Binding**: Spring Boot configures `server.address: 127.0.0.1`, preventing exposure on LAN/Wi-Fi interfaces (`0.0.0.0` is never used).
2. **Strict CORS Allowlist**: Wildcard origins (`*`) are prohibited. Only explicit local UI origins (`http://127.0.0.1:5173`, `http://localhost:5173`, `http://127.0.0.1:8787`, `http://localhost:8787`) and configured extension origin patterns are permitted.
3. **Host Header / Local Origin Validation Filter**: `LocalOriginFilter` validates `Host` and `Origin` headers on API requests to block DNS-rebinding attempts and unauthorized remote origins.
4. **Per-Launch Ephemeral Token Architecture**: `LocalTokenService` generates a high-entropy per-session token at startup (`SecureRandom`, 256-bit) stored in-memory (and intended for authenticated desktop/extension handshakes on protected endpoints in Task 2+).
5. **Safe Error Handling**: `GlobalExceptionHandler` strips internal stack traces from HTTP error payloads.
6. **No Code Execution in Main Backend**: Student code execution is isolated to a dedicated out-of-process sandbox contract (`modules/test-engine`), never inside the Spring Boot JVM.

## Planned Extension Handshake (Task 2+)

When the browser extension pairs with the local desktop daemon:
1. Only explicit extension IDs (`chrome-extension://<id>`) configured by the user/build are accepted.
2. Mutating/tutoring endpoints will require a verified local handshake token (`X-Mentor-Local-Token`) paired via the desktop app or popup pairing flow.
3. No model files or filesystem listings are exposed over HTTP endpoints.
