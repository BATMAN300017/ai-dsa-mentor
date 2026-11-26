# Model Setup & Windows 11 CPU-Only Inference Guide

## 1. Selected Model Architecture & Pinned Source

- **Model**: `Qwen2.5-Coder-3B-Instruct` (decoder-only transformer, 3B parameters, ChatML instruction-tuned)
- **Quantization**: `Q4_K_M` (4-bit K-quant medium, GGUF format)
- **Hugging Face Repository**: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- **Pinned Revision**: `f74adce6aa16316c625447af059dbebe4983757c`
- **Exact Filename**: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`
- **Exact File Size**: `2,104,932,800` bytes (~1.96 GiB / 2.10 GB)
- **Trusted Upstream SHA-256**: `724fb256bec1ff062b2f65e4569e871ad2e95ab2a3989723d1769c54294730b7` (from Hugging Face Git LFS pointer `oid` at revision `f74adce6aa16316c625447af059dbebe4983757c`)
- **Descriptor File**: `resources/models/qwen2.5-coder-3b/model-info.json`
- **Inference Runtime**: `llama.cpp` (`llama-server` loopback HTTP interface, CPU-only `-ngl 0` by default)

## 2. Online vs. Offline Requirements

| Step | Internet Required? | Notes |
| :--- | :--- | :--- |
| Initial `llama.cpp` setup | Yes (one-time) | Downloads official release `b12760` from GitHub and verifies SHA-256. |
| Initial GGUF model download | Yes (one-time) | Downloads ~2.10 GB GGUF from Hugging Face into OS AppData using `.part` staging and HTTP Range resume. |
| Model integrity verification | **No (100% offline)** | Computes local SHA-256 and compares against `upstreamChecksum` in `model-info.json`. |
| Loading / unloading model | **No (100% offline)** | Launches local `llama-server` on `127.0.0.1` only. |
| Local AI inference & UI | **No (100% offline)** | All requests stay on `127.0.0.1` loopback. Zero cloud LLM APIs. |

## 3. Model & Runtime Storage Locations

Model weights and runtime binaries are **never** stored in the application installation directory or committed to Git:

- **Windows 11**:
  - Models: `%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b\`
  - Runtime: `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp\`
  - Logs: `%LOCALAPPDATA%\AI DSA Mentor\logs\llama-server.log`
- **macOS**:
  - Models: `~/Library/Application Support/AI DSA Mentor/models/qwen2.5-coder-3b/`
  - Runtime: `~/Library/Application Support/AI DSA Mentor/runtimes/llama.cpp/`
- **Linux**:
  - Models: `${XDG_DATA_HOME:-~/.local/share}/ai-dsa-mentor/models/qwen2.5-coder-3b/`
  - Runtime: `${XDG_DATA_HOME:-~/.local/share}/ai-dsa-mentor/runtimes/llama.cpp/`

Local installation state (local calculated SHA-256, verified timestamp, file size, errors) is persisted per user inside `<AppDataModelDir>\install-state.json`, leaving `resources/models/qwen2.5-coder-3b/model-info.json` unmodified.

## 4. Windows 11 CPU-Only Setup Procedure

### Step A: Install `llama.cpp` (`llama-server.exe`)

**Option 1 — Verified Setup Script (Recommended):**
```powershell
.\scripts\setup-llama-cpp.ps1
```
This downloads official `ggml-org/llama.cpp` release `b12760` (`llama-b12760-bin-win-cpu-x64.zip`), verifies its SHA-256 (`c82a83190be4f0506778d458db685b6ba5d1b06372b7f7083e6e76605c84cb97`), and extracts it into `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp\`.

**Option 2 — Manual Installation:**
1. Download `llama-b12760-bin-win-cpu-x64.zip` from https://github.com/ggml-org/llama.cpp/releases/tag/b12760.
2. Verify the archive checksum in PowerShell:
   ```powershell
   Get-FileHash "$env:USERPROFILE\Downloads\llama-b12760-bin-win-cpu-x64.zip" -Algorithm SHA256
   ```
3. Extract the entire archive into `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp\`. Keep all `.dll` files (`llama-server-impl.dll`, `llama.dll`, `ggml*.dll`, `libomp.dll`) next to `llama-server.exe`.
4. Alternatively, place `llama-server.exe` anywhere and set `AIDSA_LLAMA_SERVER_PATH=C:\path\to\llama-server.exe` before starting the backend.

### Step B: Start the Backend & Frontend

```powershell
# Build frontend production bundle (if not built yet)
Push-Location apps\frontend
npm install
npm run build
Pop-Location

# Start Spring Boot backend on 127.0.0.1:8787
.\mvnw.cmd -pl apps/backend -am spring-boot:run
```
Open `http://127.0.0.1:8787` in your browser and scroll to **Quickstart & Local Setup** (`#installation`).

### Step C: First-Time Model Download

**Option 1 — Via Web UI:**
Click **Download Model** in the **Local Model & Runtime Console**. Progress updates live from `qwen2.5-coder-3b-instruct-q4_k_m.gguf.part`, supports **Cancel Download** and **Resume Download** (HTTP `Range`), and automatically verifies size, GGUF magic header, and SHA-256.

**Option 2 — Via PowerShell Script:**
```powershell
.\scripts\download-model.ps1
```

**Option 3 — Manual Browser Download:**
Download `qwen2.5-coder-3b-instruct-q4_k_m.gguf` from:
`https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/f74adce6aa16316c625447af059dbebe4983757c/qwen2.5-coder-3b-instruct-q4_k_m.gguf`
Save it to `%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b\qwen2.5-coder-3b-instruct-q4_k_m.gguf`. On first status check, the backend calculates the local SHA-256 and verifies it against `upstreamChecksum`.

### Step D: Load, Test Inference, and Unload

1. Set CPU parameters in the UI (defaults: `Context = 4096`, `CPU Threads = 6`, `GPU Layers = 0`).
2. Click **Load Model**. Wait while `llama-server.exe` loads the GGUF into RAM (`STARTING` -> `READY`).
3. Verify `GET http://127.0.0.1:8787/api/v1/health` reports `"modelLoaded": true`.
4. Enter a prompt in **Local Inference Test** and click **Run Local Inference** to receive a real local response with latency and token counts.
5. Click **Unload Model** to terminate the `llama-server` subprocess and free RAM immediately.

## 5. Manual Integration Test Procedure (Real ~2.1 GB Model on Windows 11)

Automated unit/CI tests use mock HTTP servers and fake processes so CI never downloads 2 GB. To verify the real model end-to-end on Windows 11:

1. Run `.\scripts\setup-llama-cpp.ps1` and verify `llama-server.exe` is extracted in `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp`.
2. Start backend: `.\mvnw.cmd -pl apps/backend -am spring-boot:run`.
3. Open `http://127.0.0.1:8787/#installation`:
   - Confirm `downloadStatus = NOT_DOWNLOADED`, `checksumVerificationStatus = NOT_CHECKED`, `runtimeAvailable = true`, `modelLoaded = false`.
4. Click **Download Model**:
   - Confirm `.part` file appears in `%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b\qwen2.5-coder-3b-instruct-q4_k_m.gguf.part`.
   - Optionally click **Cancel Download**, confirm status becomes `CANCELLED` and `.part` remains, then click **Resume Download** and verify it resumes via HTTP `Range` (`206`).
   - Confirm completion sets `downloadStatus = VERIFIED` and `checksumVerificationStatus = VERIFIED_AGAINST_UPSTREAM`.
5. Click **Load Model** (`Context = 4096`, `Threads = 6`, `GPU Layers = 0`):
   - Confirm `modelLoaded = true`, `runtimeStatus = READY`, and `GET /api/v1/health` shows `"modelLoaded": true`.
6. Submit prompt `"Write a two-sentence summary of merge sort complexity."` in **Local Inference Test**:
   - Confirm a coherent non-simulated response returns with measured latency and token counts.
7. Disconnect Wi-Fi/internet and repeat inference to verify **100% offline operation**.
8. Click **Unload Model** and verify `llama-server.exe` exits cleanly in Task Manager and `modelLoaded` returns to `false`.

## 6. Hardware, Memory & Performance Expectations (CPU-Only)

- **Minimum RAM**: 8 GB (with `-c 2048`).
- **Recommended RAM**: 16–32 GB (your 32 GB Windows 11 system has ample headroom).
- **Memory Footprint (`Qwen2.5-Coder-3B-Instruct Q4_K_M`)**:
  - Model weights: ~2.10 GB
  - KV cache & runtime overhead (`-c 4096`): ~0.6–1.2 GB
  - Total `llama-server` RSS: typically **~2.7–3.4 GB**
- **CPU Performance**:
  - Without a dedicated GPU (`-ngl 0`), generation runs purely on CPU threads (`-t` defaults to `min(8, logicalProcessors / 2)`, i.e., `6` threads on a 12-thread i5-13420H).
  - Prompt processing is typically fast (tens of tokens/sec); autoregressive token generation on laptop CPUs is typically **8–20 tokens/sec**, meaning 150–250 tokens take roughly **8–25 seconds**. Keep `maxTokens` reasonable (128–512) and `requestTimeout` >= 120s.

## 7. Integrity Verification Semantics

AI DSA Mentor strictly separates:
1. **`upstreamChecksum` (`upstreamChecksumSource`)**: SHA-256 published by a trusted upstream source (`Hugging Face Git LFS pointer oid` for pinned revision `f74adce6aa16316c625447af059dbebe4983757c`).
2. **`localCalculatedChecksum`**: SHA-256 computed locally over the downloaded file bytes.

Status guarantees:
- Having a non-empty string in metadata **never** marks a model verified.
- Only when `localCalculatedChecksum` matches `upstreamChecksum` (and file size + `GGUF` magic header pass) does `checksumVerificationStatus` become `VERIFIED_AGAINST_UPSTREAM`.
- If `upstreamChecksum` is missing, `checksumVerificationStatus` is `UNVERIFIED_NO_UPSTREAM_CHECKSUM` and the model is not marked verified.

Verify manually anytime on Windows PowerShell:
```powershell
Get-FileHash "$env:LOCALAPPDATA\AI DSA Mentor\models\qwen2.5-coder-3b\qwen2.5-coder-3b-instruct-q4_k_m.gguf" -Algorithm SHA256
```

## 8. Replacing the Model in the Future (e.g., Fine-Tuned GGUF)

Because `modules/ai-engine` depends only on `InferenceProvider` and `ModelManagerService` reads `model-info.json`:
1. Export or quantize your fine-tuned Qwen model to GGUF (e.g., `Q4_K_M`).
2. Compute its SHA-256 (`Get-FileHash` / `sha256sum`) and exact byte size.
3. Update `resources/models/qwen2.5-coder-3b/model-info.json` (or add a new slug directory and set `aidsa.model.slug`):
   - `expectedFilename`, `expectedSizeBytes`, `revision`, `downloadUrl`, `upstreamChecksum`, `upstreamChecksumSource`.
4. Place the new GGUF in the AppData model directory (or download from your pinned URL). No Java code or frontend redesign is required.

## 9. Troubleshooting

- **`Runtime Missing`**: Run `.\scripts\setup-llama-cpp.ps1` or set `AIDSA_LLAMA_SERVER_PATH` to `llama-server.exe`. Ensure all `.dll` files remain in the same folder.
- **Checksum Mismatch / Corrupt `.part`**: Delete any leftover `.part` file in `%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b\` and click **Download Model** again.
- **Startup Timeout (`STARTING` -> `ERROR`)**: Inspect `%LOCALAPPDATA%\AI DSA Mentor\logs\llama-server.log`. Try lowering context size (`-c 2048`) or increasing `AIDSA_LLAMA_STARTUP_TIMEOUT`.
- **Inference Timeout**: Increase `maxTokens` only as needed, or raise `AIDSA_LLAMA_REQUEST_TIMEOUT` (default `120` seconds).
