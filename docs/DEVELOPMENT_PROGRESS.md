# Development Progress

## Current Task: Task 2 — Local AI Model Integration

**Status:** Implemented & verified with automated unit/integration tests (manual ~2.1 GB real-model integration run ready on Windows 11).

### Recovered Pre-Existing State (Phase 0)

- Inspected `C:\Users\BATMA\.gemini\antigravity\scratch\ai-dsa-mentor` on branch `main` at commit `d444608` (`feat: initialize AI DSA Mentor Task 1 foundation`).
- Confirmed via `1d948b68-63b7-48a4-ba42-f246949bbdcf` conversation logs that the previous Task 2 session had only read Task 1 files and checked upstream Hugging Face / GitHub release metadata before stopping; zero Task 2 files had been modified locally.
- Reused and extended Task 1's existing `AppDataPathResolver`, `ModelMetadataReader`, `InferenceProvider` contracts, `LocalOriginFilter`, `LocalTokenService`, `HealthController`, `SystemController`, and Stitch-themed React frontend.

### Completed Components (Task 2)

1. **Model Selection & Pinned Metadata (`resources/models/qwen2.5-coder-3b/model-info.json`)**:
   - Verified upstream repository `Qwen/Qwen2.5-Coder-3B-Instruct-GGUF` at pinned revision `f74adce6aa16316c625447af059dbebe4983757c`.
   - Verified exact filename `qwen2.5-coder-3b-instruct-q4_k_m.gguf`, byte size `2104932800`, HTTP `Accept-Ranges: bytes`, and trusted Git LFS SHA-256 `724fb256bec1ff062b2f65e4569e871ad2e95ab2a3989723d1769c54294730b7`.
2. **Model Manager (`modules/model-manager`)**:
   - Separated trusted `upstreamChecksum` from `localCalculatedChecksum` using `ChecksumVerificationStatus` (`NOT_CHECKED`, `VERIFIED_AGAINST_UPSTREAM`, `UNVERIFIED_NO_UPSTREAM_CHECKSUM`, `MISMATCH`). Removed Task 1's placeholder `hasVerifiedChecksum()` check.
   - Added `Sha256ChecksumCalculator` (size check, `GGUF` magic header check, streaming SHA-256).
   - Added `ModelManagerService` with async downloads, `.part` staging, HTTP `Range` (`206`) resume, live progress snapshots, cancellation, external file deletion/corruption detection, and persistent OS AppData `install-state.json`.
   - Extended `AppDataPathResolver` with `resolveRuntimesDirectory()` and `resolveRuntimeDirectory(...)` with single-segment path-traversal validation.
3. **llama.cpp Runtime & AI Engine (`modules/ai-engine`)**:
   - Added `LlamaRuntimeDiscovery` (`AIDSA_LLAMA_SERVER_PATH`, AppData `runtimes/llama.cpp`, workspace `resources/runtimes/llama.cpp`, `PATH`, rejecting non-`llama-server` binaries).
   - Added `LlamaRuntimeConfig` (CPU-only `-ngl 0`, configurable context size, CPU threads, timeouts).
   - Implemented `LlamaCppInferenceProvider` implementing `InferenceProvider` and `AutoCloseable`, launching `llama-server` strictly on `127.0.0.1` via `ProcessBuilder` (`ProcessStarter`), polling `/health`, monitoring unexpected process exits, cleanly unloading/shutting down, and executing `/v1/chat/completions` (plus SSE streaming support).
4. **Backend API & Local Security (`apps/backend`)**:
   - Wired `ModelManagerService` and `LlamaCppInferenceProvider` in `CoreModulesConfig` with `destroyMethod = "close"`.
   - Updated `HealthService` and `SystemStatusService` to report real live model/runtime state.
   - Added `ModelController` (`GET /api/v1/model/status`, `POST /api/v1/model/download`, `POST /api/v1/model/download/cancel`, `POST /api/v1/model/load`, `POST /api/v1/model/unload`) and `AiController` (`POST /api/v1/ai/infer`).
   - Added `SecurityController` (`GET /api/v1/security/handshake`) and Fetch-Metadata checks in `LocalOriginFilter` so the local React UI obtains an in-memory `X-Mentor-Local-Token` securely without weakening CSRF/origin protections.
5. **Frontend Minimal Model Console (`apps/frontend`)**:
   - Extended `types/api.ts`, `apiClient.ts`, `modelService.ts`, and `useBackendStatus.ts` (adaptive 1s polling during active download/loading).
   - Added `ModelSetupPanel.tsx` inside `#installation` preserving the existing Stitch dark theme, displaying installation status, download progress/errors, separate upstream vs. local SHA-256 status, runtime availability, load/unload controls, and a raw local inference test console.
6. **Scripts, Automated Tests, & Documentation**:
   - Upgraded `scripts/download-model.ps1` and `scripts/download-model.sh`, and added SHA-256-verified `scripts/setup-llama-cpp.ps1` and `scripts/setup-llama-cpp.sh` pinned to `ggml-org/llama.cpp` `b12760`.
   - Added JUnit 5 tests in `model-manager`, `ai-engine`, and `backend` using loopback `HttpServer` mocks and fake `Process` instances, plus Vitest integration tests in `apps/frontend/tests/modelService.test.ts`.
   - Updated `docs/model-setup.md`, `docs/api-reference.md`, `docs/architecture.md`, `docs/security.md`, `resources/runtimes/llama.cpp/README.md`, and `README.md`.

### Remaining Components (Future Tasks — Intentionally Not Started)

- **Task 3**: DSA tutoring engine & Socratic hint policies (`modules/tutoring-engine`).
- **Task 4**: Tree-sitter AST code intelligence (`modules/code-analyzer`).
- **Task 5**: Isolated student code execution sandbox (`modules/test-engine`).
- **Task 6**: LeetCode / Chrome extension problem adapters (`apps/browser-extension`).
- **Task 7**: Voice mentoring, progress analytics, personalization.
- **Task 8**: Native `jpackage` packaging and installers (`installer/`).

### Important Architectural Decisions

- **Separate `install-state.json` in OS AppData**: Keeps Git-tracked `resources/models/qwen2.5-coder-3b/model-info.json` read-only and immutable, avoiding dirty worktrees and supporting read-only packaged installs.
- **`llama-server` Loopback HTTP + `/v1/chat/completions`**: Verified against `ggml-org/llama.cpp` `b12760` (`--host 127.0.0.1`, `--no-webui`, `-ngl 0`, `/health` 503->200 readiness, embedded GGUF ChatML templating).
- **In-Memory UI Token Handshake**: Web UI requests `GET /api/v1/security/handshake` with `X-Mentor-Client: ai-dsa-mentor-ui`; `LocalOriginFilter` blocks remote `Origin` and `Sec-Fetch-Site: cross-site`, keeping mutating endpoints protected against arbitrary websites while allowing seamless local UI workflows.

### Commands Used to Verify

- `mvn clean verify` (all 9 Maven modules, JUnit 5 suites using mock HTTP servers and fake processes)
- `cd apps/frontend && npm test && npm run typecheck && npm run build`
- `cd apps/browser-extension && npm run validate && npm run build`
- Live browser check of `http://127.0.0.1:8787/` against running Spring Boot backend

### Known Issues & Manual Steps Required from User

- Automated CI/tests intentionally do **not** download the ~2.10 GB GGUF model or execute the real `llama-server.exe` binary.
- On Windows 11, run `.\scripts\setup-llama-cpp.ps1` (to install verified `llama.cpp` `b12760` CPU binaries into `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp`) and download `qwen2.5-coder-3b-instruct-q4_k_m.gguf` via the UI or `.\scripts\download-model.ps1` following [docs/model-setup.md](model-setup.md).

### Next Recommended Action

- Complete manual Windows 11 real-model verification per `docs/model-setup.md`, then begin **Task 3 (DSA Tutoring Engine)** inside `modules/tutoring-engine`.
