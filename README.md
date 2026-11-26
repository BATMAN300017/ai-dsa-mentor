# AI DSA Mentor

> **"Think with me. Don't solve it for me."**

**AI DSA Mentor** is a local-first AI-powered Data Structures and Algorithms (DSA) tutoring application built around Java 21, Spring Boot, JavaFX, React + TypeScript, and a Chrome Manifest V3 extension. It is designed to guide students through progressive Socratic hints, mistake detection, and complexity intuition without immediately revealing full solutions.

---

## Task 1 Scope & Current Status

Task 1 delivers the **reproducible project foundation**:

- Multi-module Java 21 Maven monorepo (`modules/*`, `apps/backend`, `apps/desktop`) with Maven Wrapper (`mvnw` / `mvnw.cmd`).
- Spring Boot 3.4 backend bound strictly to `127.0.0.1` (`GET /api/v1/health`, `GET /api/v1/system/status`), structured exception handling, and localhost origin/token security foundations.
- Integrated React + TypeScript + Tailwind CSS frontend faithfully converted from the supplied Stitch design (`apps/frontend`), connected to the backend health/status API.
- JavaFX 21 desktop shell (`apps/desktop`) with single-instance `BackendLauncher` (no duplicate backend processes) and `WebViewManager` with automatic default-browser fallback.
- Chrome Manifest V3 browser extension scaffold (`apps/browser-extension`) with background service worker, popup, and localhost messaging bridge.
- Target model descriptor at [`resources/models/qwen2.5-coder-3b/model-info.json`](resources/models/qwen2.5-coder-3b/model-info.json) for **Qwen2.5-Coder-3B-Instruct (GGUF Q4_K_M)** (not downloaded in Task 1).

Not yet implemented (scheduled for subsequent tasks): `llama.cpp` inference, GGUF model downloading, Socratic LLM hint generation, Tree-sitter parsing, complexity analysis, sandboxed code execution, LeetCode DOM observation, voice interaction, learning analytics, and `jpackage` installers.

---

## Technology Stack & Prerequisites

| Tool | Required Version |
| :--- | :--- |
| **JDK** | Java 21 LTS or newer (includes `jpackage`) |
| **Maven** | Apache Maven 3.9+ (or included `./mvnw` / `mvnw.cmd`) |
| **Node.js** | Node.js 20+ (with npm 10+) |

Planned local AI model:
- **Model**: `Qwen2.5-Coder-3B-Instruct` (`GGUF`, `Q4_K_M`)
- **Repository**: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- **Planned Runtime**: `llama.cpp` (100% local-first; no paid cloud LLM API)

---

## Project Structure

```text
ai-dsa-mentor/
├── apps/
│   ├── backend/             # Spring Boot REST API (127.0.0.1:8787)
│   ├── desktop/             # JavaFX 21 desktop shell & single-instance launcher
│   ├── frontend/            # React + TypeScript + Vite + Tailwind CSS UI
│   └── browser-extension/   # Chrome Manifest V3 companion scaffold
├── modules/
│   ├── ai-engine/           # Local inference contracts (llama.cpp boundary)
│   ├── code-analyzer/       # AST analysis contracts (Tree-sitter boundary)
│   ├── model-manager/       # Model metadata reader & OS AppData path resolver
│   ├── session-manager/     # Tutoring session store & lifecycle contracts
│   ├── test-engine/         # Out-of-process sandbox execution policy & contracts
│   └── tutoring-engine/     # Progressive Socratic guidance policy contracts
├── resources/
│   ├── models/qwen2.5-coder-3b/model-info.json
│   ├── runtimes/llama.cpp/
│   ├── prompts/
│   ├── dsa-knowledge/
│   └── test-data/
├── scripts/                 # Dev setup & placeholder model/release scripts
├── installer/               # Future jpackage OS packaging targets
├── docs/                    # Architecture, API, security, setup guides
└── .github/workflows/       # CI workflow
```

---

## Quick Setup & Run Commands

### 1. Automated Dev Setup

**Windows (PowerShell):**
```powershell
.\scripts\setup-dev.ps1
```

**macOS / Linux:**
```bash
chmod +x scripts/*.sh
./scripts/setup-dev.sh
```

### 2. Start the Backend (`http://127.0.0.1:8787`)

```bash
# Linux / macOS
./mvnw -pl apps/backend spring-boot:run

# Windows PowerShell
.\mvnw.cmd -pl apps/backend spring-boot:run
```

Verify health check:
```bash
curl http://127.0.0.1:8787/api/v1/health
```
Expected response:
```json
{
  "status": "UP",
  "application": "AI DSA Mentor",
  "version": "0.1.0",
  "modelLoaded": false
}
```

### 3. Start the Frontend (`http://127.0.0.1:5173`)

```bash
cd apps/frontend
npm install
npm run dev
```

Build production frontend bundle (`apps/frontend/dist`):
```bash
cd apps/frontend
npm run build
```

### 4. Build & Run the Desktop Application

```bash
# Build all modules including desktop & backend JARs
./mvnw clean verify

# Launch JavaFX desktop shell
./mvnw -pl apps/desktop -am javafx:run
```
(On Windows, use `.\mvnw.cmd`.)

### 5. Build & Validate the Browser Extension

```bash
cd apps/browser-extension
npm install
npm run build
npm run validate
```

### 6. Run All Java Tests

```bash
# Linux / macOS
./mvnw clean test

# Windows PowerShell
.\mvnw.cmd clean test
```

---

## Documentation

- [Architecture Overview](docs/architecture.md)
- [Installation Guide](docs/installation.md)
- [API Reference](docs/api-reference.md)
- [Development Guide](docs/development-guide.md)
- [Model Setup Guide](docs/model-setup.md)
- [Security Design](docs/security.md)

---

## License

[MIT License](LICENSE)
