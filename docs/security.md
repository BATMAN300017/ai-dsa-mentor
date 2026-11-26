# Security Design

## Threat Model & Local-First Principles

AI DSA Mentor runs local HTTP services on `127.0.0.1`. To protect developer workstations against DNS rebinding, cross-site request forgery (CSRF), arbitrary webpage probes, unverified downloads, and arbitrary command execution, strict controls are enforced across all layers.

## Implemented Safeguards

1. **Loopback-Only Binding**:
   - Spring Boot binds exclusively to `server.address: 127.0.0.1`.
   - `llama-server` is always launched with `--host 127.0.0.1` on an internal loopback port and `--no-webui`, never `0.0.0.0`.
2. **Strict CORS Allowlist**:
   - Wildcard origins (`*`) are never used. Only explicit localhost UI origins (`127.0.0.1:5173`, `localhost:5173`, `127.0.0.1:4173`, `localhost:4173`, `127.0.0.1:8787`, `localhost:8787`) and explicitly configured extension IDs are allowed.
3. **DNS Rebinding, Origin & Fetch-Metadata Filtering (`LocalOriginFilter`)**:
   - Rejects non-loopback `Host` headers (`403 Forbidden`).
   - Rejects unlisted `Origin` headers (`403 Forbidden`).
   - Rejects browser `Sec-Fetch-Site: cross-site` requests (`403 Forbidden`).
4. **Scoped Ephemeral Token Authentication (`LocalTokenService` + `GET /api/v1/security/handshake`)**:
   - All mutating endpoints (`POST /api/v1/model/*`, `POST /api/v1/ai/infer`) require a valid `X-Mentor-Local-Token` header (`401 Unauthorized` when absent or invalid).
   - The web UI retrieves a scoped ephemeral UI token in-memory via `GET /api/v1/security/handshake`, which requires `X-Mentor-Client: ai-dsa-mentor-ui` (triggering CORS preflight) and rejects browser top-level navigations (`Sec-Fetch-Mode: navigate`) and non-empty fetch destinations (`<script>`, `<img>`, `<iframe>`).
5. **No Arbitrary Process Execution**:
   - API payloads never accept executable paths or arbitrary CLI flags.
   - `LlamaRuntimeDiscovery` only resolves `llama-server` / `llama-server.exe` from server-side configuration or OS AppData/workspace/PATH and launches directly via `ProcessBuilder(List<String>)` without shell wrappers.
6. **Integrity Verification & Path Safety**:
   - Production model downloads require HTTPS (`huggingface.co` pinned revision URL), stage in `.part` files, reject path traversal in filenames, validate byte length + `GGUF` magic header + SHA-256 against trusted `upstreamChecksum`.
   - Official `llama.cpp` helper scripts validate SHA-256 digests before extracting binaries.
7. **Safe Error Handling**:
   - `GlobalExceptionHandler` strips internal stack traces from all HTTP error responses.
