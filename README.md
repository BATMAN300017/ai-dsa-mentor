# AI DSA Mentor

> **"Think with me. Don't solve it for me."**

**AI DSA Mentor** is a local-first AI-powered Data Structures and Algorithms (DSA) tutoring application built around Java 21, Spring Boot, JavaFX, React + TypeScript, and a Chrome Manifest V3 extension.

---

## Current Scope & Status (Task 2: Local AI Model Integration)

Implemented so far:

- Multi-module Java 21 Maven monorepo (`modules/*`, `apps/backend`, `apps/desktop`) with Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Model Manager (`modules/model-manager`)**: Pinned descriptor for **Qwen2.5-Coder-3B-Instruct (GGUF Q4_K_M)** (`resources/models/qwen2.5-coder-3b/model-info.json`), OS AppData storage (`%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b`), `.part` resumable downloads with HTTP `Range`, cancellation, and SHA-256 verification against the trusted Hugging Face LFS digest.
- **Local Inference Engine (`modules/ai-engine`)**: Loopback-only (`127.0.0.1`) `llama.cpp` `llama-server` subprocess discovery, lifecycle management (start, `/health` readiness polling, unexpected exit handling, unload, shutdown), and CPU-only inference (`-ngl 0`).
- **Spring Boot Backend (`apps/backend`)**: Bound strictly to `127.0.0.1:8787`, exposing `/api/v1/health`, `/api/v1/system/status`, `/api/v1/security/handshake`, `/api/v1/model/status`, `/api/v1/model/download`, `/api/v1/model/download/cancel`, `/api/v1/model/load`, `/api/v1/model/unload`, and `/api/v1/ai/infer`.
- **Frontend UI (`apps/frontend`)**: Stitch-themed React + TypeScript landing page with the integrated **Local Model & Runtime Console** under `#installation` for downloading, verifying, loading/unloading, and running raw local inference tests.
- **Desktop Shell & Extension Scaffold**: JavaFX 21 single-instance `BackendLauncher` (`apps/desktop`) and Chrome MV3 extension scaffold (`apps/browser-extension`).

Not yet implemented (reserved for Tasks 3–8): Socratic DSA tutoring policy (`modules/tutoring-engine`), Tree-sitter AST parsing (`modules/code-analyzer`), isolated test sandbox (`modules/test-engine`), LeetCode adapters (`apps/browser-extension`), voice mentoring, and `jpackage` installers (`installer/`).

---

## Technology Stack & Prerequisites

| Tool | Required Version |
| :--- | :--- |
| **JDK** | Java 21 LTS or newer |
| **Maven** | Apache Maven 3.9+ (or included `./mvnw` / `mvnw.cmd`) |
| **Node.js** | Node.js 20+ (with npm 10+) |
| **Local Inference** | `llama.cpp` (`llama-server`, official `b12760` CPU release supported) |

Local AI Model:
- **Model**: `Qwen2.5-Coder-3B-Instruct` (`GGUF`, `Q4_K_M`, `2,104,932,800` bytes)
- **Repository**: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- **Pinned Revision**: `f74adce6aa16316c625447af059dbebe4983757c`

---

## Quick Setup & Run Commands

### 1. Provision Dependencies & `llama.cpp` Runtime

**Windows 11 (PowerShell, CPU-only):**
```powershell
.\scripts\setup-dev.ps1
.\scripts\setup-llama-cpp.ps1
```

**macOS / Linux:**
```bash
chmod +x scripts/*.sh
./scripts/setup-dev.sh
./scripts/setup-llama-cpp.sh
```

### 2. Build Frontend & Start Backend (`http://127.0.0.1:8787`)

```powershell
# Build production frontend bundle served by Spring Boot
Push-Location apps\frontend; npm install; npm run build; Pop-Location

# Start Spring Boot backend (127.0.0.1:8787)
.\mvnw.cmd -pl apps/backend -am spring-boot:run
```
Open `http://127.0.0.1:8787` and use the **Local Model & Runtime Console** in `#installation` to download, verify, load, and test the model locally (or run `.\scripts\download-model.ps1`).

### 3. Run All Automated Tests

```powershell
# Full Java Maven build & JUnit 5 verification (uses mocks; does not download 2 GB)
.\mvnw.cmd clean verify

# Frontend tests, typecheck & build
Push-Location apps\frontend; npm test; npm run typecheck; npm run build; Pop-Location

# Browser extension manifest validation & build
Push-Location apps\browser-extension; npm run validate; npm run build; Pop-Location
```

---

## Documentation

- [Model Setup & Windows 11 CPU Guide](docs/model-setup.md)
- [API Reference](docs/api-reference.md)
- [Architecture Overview](docs/architecture.md)
- [Security Design](docs/security.md)
- [Development Guide](docs/development-guide.md)
- [Installation Guide](docs/installation.md)
- [Development Progress Log](docs/DEVELOPMENT_PROGRESS.md)

---

## License

[MIT License](LICENSE)
