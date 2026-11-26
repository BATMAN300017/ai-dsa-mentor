# API Reference

Base URL: `http://127.0.0.1:8787` (bound exclusively to IPv4 loopback `127.0.0.1`).

Mutating endpoints (`POST`) require:
1. Loopback `Host` header (`127.0.0.1`, `localhost`, or `[::1]`).
2. Allowlisted `Origin` (no wildcard CORS) and non-`cross-site` `Sec-Fetch-Site`.
3. Valid `X-Mentor-Local-Token` header obtained via `GET /api/v1/security/handshake` (for the local web UI) or process coordination.

---

## Endpoints

### `GET /api/v1/health`

Returns live backend health and whether a local GGUF model is loaded and ready.

```json
{
  "status": "UP",
  "application": "AI DSA Mentor",
  "version": "0.1.0",
  "modelLoaded": false
}
```

### `GET /api/v1/system/status`

Returns overall subsystem capability states, philosophy, and active model summary.

### `GET /api/v1/security/handshake`

Issues an ephemeral `X-Mentor-Local-Token` for allowlisted localhost UI origins.

- Required header: `X-Mentor-Client: ai-dsa-mentor-ui`
- Rejects `Sec-Fetch-Site: cross-site`, `Sec-Fetch-Mode: navigate`, and non-empty `Sec-Fetch-Dest` (such as `<script>`).

### `GET /api/v1/model/status`

Returns detailed model metadata, installation state, download progress, upstream vs. locally calculated SHA-256 checksums, `llama.cpp` runtime availability, loaded state, active loopback port, and errors.

### `POST /api/v1/model/download`

Starts asynchronous GGUF download into `<AppDataModelDir>/<expectedFilename>.part` with HTTP `Range` resume support and SHA-256 verification.

Optional body:
```json
{
  "forceRedownload": false
}
```
Returns `202 Accepted` (or `200 OK` if already verified and `forceRedownload` is `false`).

### `POST /api/v1/model/download/cancel`

Cancels any active download, preserving the `.part` file for future resume. Returns `200 OK` with updated `ModelStatusResponse`.

### `POST /api/v1/model/load`

Starts `llama-server` asynchronously on `127.0.0.1` using the verified local GGUF file. Returns `409 Conflict` if the model is not yet downloaded/verified, or `503 Service Unavailable` if `llama-server` is missing.

Optional validated body:
```json
{
  "contextSize": 4096,
  "threads": 6,
  "gpuLayers": 0
}
```
Validation bounds: `contextSize` `[512, 32768]`, `threads` `[1, 64]`, `gpuLayers` `[0, 128]`.

### `POST /api/v1/model/unload`

Terminates the running `llama-server` subprocess cleanly (`destroy()` with forcible fallback) and resets `modelLoaded` to `false`.

### `POST /api/v1/ai/infer`

Runs raw local inference against the loaded `llama-server` (`POST /v1/chat/completions` on `127.0.0.1`).

Request body (`@Valid`):
```json
{
  "systemPrompt": "You are a helpful local assistant.",
  "userPrompt": "Explain binary search time complexity.",
  "maxTokens": 256,
  "temperature": 0.2,
  "topP": 0.95
}
```
Validation: `userPrompt` non-blank (`<= 32000` chars), `systemPrompt` (`<= 16000` chars), `maxTokens` `[1, 4096]`, `temperature` `[0.0, 2.0]`, `topP` `[0.05, 1.0]`.

Response `200 OK`:
```json
{
  "text": "...",
  "modelName": "Qwen2.5-Coder-3B-Instruct",
  "latencyMillis": 1450,
  "promptTokens": 24,
  "completionTokens": 48,
  "finishReason": "stop"
}
```

## Error Format

All errors are handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`) without exposing stack traces:

```json
{
  "timestamp": "2026-11-26T09:30:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "No local GGUF model is loaded. Download and load Qwen2.5-Coder-3B-Instruct first.",
  "path": "/api/v1/ai/infer"
}
```
