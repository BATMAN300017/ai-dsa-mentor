# AI DSA Mentor Architecture

> Philosophy: **"Think with me. Don't solve it for me."**

## 1. Monorepo Overview

AI DSA Mentor is a local-first Java-centric monorepo designed to run locally on developer workstations without cloud LLM dependencies.

| Layer | Technology | Responsibility | Current Status |
| :--- | :--- | :--- | :--- |
| Desktop Shell | Java 21 + JavaFX 21 | Single-instance backend lifecycle, UI presentation (WebView with browser fallback), clean shutdown | Foundation implemented |
| Backend API | Spring Boot 3.4 (Java 21) | Loopback REST API (`127.0.0.1`), model/runtime orchestration, inference endpoints, origin/token security | Task 2 APIs implemented |
| Frontend UI | React + TypeScript + Vite + Tailwind CSS | Landing page + Local Model & Runtime Setup + Local Inference Test console | Task 2 UI integrated |
| Browser Extension | Chrome Manifest V3 + TypeScript | Minimal background worker, popup UI, and local messaging scaffold | Scaffolded (Task 6) |
| `modules/model-manager` | Java 21 + Jackson + HttpClient | Pinned metadata, `.part` resumable downloads, cancellation, SHA-256 verification, AppData `install-state.json` | Implemented (Task 2) |
| `modules/ai-engine` | Java 21 + ProcessBuilder + HttpClient | `llama-server` discovery, loopback subprocess lifecycle, `/health` & `/v1/chat/completions` inference | Implemented (Task 2) |
| `modules/tutoring-engine` | Java 21 | Socratic guidance contracts and guardrails | Contracts only (Task 3) |
| `modules/code-analyzer` | Java 21 | Tree-sitter AST parsing contracts | Contracts only (Task 4) |
| `modules/test-engine` | Java 21 | Out-of-process sandbox execution policy | Contracts only (Task 5) |
| `modules/session-manager` | Java 21 | Tutoring session store contracts | Contracts only |

## 2. Module Boundaries (No Duplication)

- `modules/model-manager` owns model metadata (`ModelMetadata`, `ModelMetadataReader`), OS user AppData path resolution (`AppDataPathResolver`), integrity checks (`Sha256ChecksumCalculator`), and download lifecycle (`ModelManagerService`).
- `modules/ai-engine` owns `InferenceProvider` contracts and `LlamaCppInferenceProvider`, managing `llama-server` strictly on `127.0.0.1` via `ProcessBuilder` (`ProcessStarter`).
- `apps/backend` wires both domain modules via `CoreModulesConfig`, enforces security (`LocalOriginFilter`, `LocalTokenService`, `SecurityController`), and exposes `/api/v1/model/*` and `/api/v1/ai/infer`.
- `apps/desktop` manages launching the Spring Boot backend once per port and opening the local UI cleanly.

## 3. Code Execution Isolation Guarantee

Student code must **never** be executed inside the main Spring Boot backend JVM process. `modules/test-engine` reserves the isolated out-of-process sandbox contract for Task 5.
