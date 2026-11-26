# AI DSA Mentor Architecture

> Philosophy: **"Think with me. Don't solve it for me."**

## 1. Monorepo Overview

AI DSA Mentor is built as a local-first Java-centric monorepo designed to evolve into a standalone desktop application paired with an optional browser extension and local LLM runtime.

| Layer | Technology | Responsibility | Task 1 Status |
| :--- | :--- | :--- | :--- |
| Desktop Shell | Java 21 + JavaFX 21 | Single-instance backend lifecycle, UI presentation (WebView with browser fallback), clean shutdown | Foundation implemented |
| Backend API | Spring Boot 3.4 (Java 21) | Localhost-bound REST API (`127.0.0.1`), health/system telemetry, CORS/security foundation, structured error handling | Foundation implemented |
| Frontend UI | React + TypeScript + Vite + Tailwind CSS | Local product landing, runtime setup & health connectivity view (derived faithfully from supplied Stitch design) | Integrated & connected |
| Browser Extension | Chrome Manifest V3 + TypeScript | Minimal background worker, popup UI, and local messaging scaffold (no LeetCode scraping yet) | Scaffolded |
| Domain Modules | Java 21 Maven modules (`modules/*`) | Shared contracts and domain services (`model-manager`, `ai-engine`, `tutoring-engine`, `code-analyzer`, `test-engine`, `session-manager`) | Contracts & `model-manager` foundation |
| Local Model | `Qwen2.5-Coder-3B-Instruct` (`GGUF Q4_K_M`) via `llama.cpp` | Offline local Socratic inference | Metadata prepared; binary not downloaded |

## 2. Module Boundaries (No Duplication)

- `apps/backend` exposes HTTP endpoints, security filters, DTOs, and orchestration services. It delegates model metadata & OS application-data resolution directly to `modules/model-manager` instead of duplicating domain code.
- `apps/desktop` manages launching the Spring Boot backend (checking whether an instance is already listening on the configured local port to prevent duplicate backend processes), validating WebView compatibility, and falling back to the system default browser if WebView cannot render reliably.
- `modules/*` hold framework-light Java domain contracts so future tasks can implement `llama.cpp` JNI/process bindings (`ai-engine`), Tree-sitter bindings (`code-analyzer`), isolated sandbox test execution (`test-engine`), and Socratic hint policies (`tutoring-engine`) cleanly.

## 3. Code Execution Isolation Guarantee

Student code must **never** be executed inside the main Spring Boot backend JVM process. `modules/test-engine` defines the sandbox contract for isolated out-of-process execution in a future task.
