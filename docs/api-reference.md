# API Reference (Task 1 Foundation)

Base URL: `http://127.0.0.1:8787` (bound exclusively to IPv4 loopback `127.0.0.1`).

## Endpoints

### `GET /api/v1/health`

Returns the live backend health and model readiness state.

**Response `200 OK`:**

```json
{
  "status": "UP",
  "application": "AI DSA Mentor",
  "version": "0.1.0",
  "modelLoaded": false
}
```

| Field | Type | Description |
| :--- | :--- | :--- |
| `status` | `string` | Operational status (`"UP"`) |
| `application` | `string` | Application display name (`"AI DSA Mentor"`) |
| `version` | `string` | Current backend version (`"0.1.0"`) |
| `modelLoaded` | `boolean` | `false` in Task 1 until local GGUF model inference is wired in a later task |

### `GET /api/v1/system/status`

Returns non-sensitive local setup metadata read from `resources/models/qwen2.5-coder-3b/model-info.json` and OS user application-data path resolution (no arbitrary filesystem paths exposed over unauthenticated/public interfaces; only standardized app status metadata).

## Error Format

Unhandled or validation errors are handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`) and return JSON:

```json
{
  "timestamp": "2026-11-26T07:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found",
  "path": "/api/v1/unknown"
}
```
