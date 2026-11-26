# Local Inference Runtime Directory (llama.cpp)

AI DSA Mentor uses `llama-server` from [`ggml-org/llama.cpp`](https://github.com/ggml-org/llama.cpp) as a loopback-only (`127.0.0.1`) local inference subprocess managed via Java `ProcessBuilder`.

## Installation Options

Runtime binaries are **never** committed to Git. You can provision `llama-server` using either method:

1. **Automated SHA-256-Verified Setup (Windows 11 CPU-only)**:
   ```powershell
   .\scripts\setup-llama-cpp.ps1
   ```
   This downloads pinned release `b12760` (`llama-b12760-bin-win-cpu-x64.zip`), verifies its SHA-256 digest (`c82a83190be4f0506778d458db685b6ba5d1b06372b7f7083e6e76605c84cb97`), and extracts into `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp`.

2. **Manual Installation**:
   - Download an official CPU release archive from https://github.com/ggml-org/llama.cpp/releases (`llama-b12760-bin-win-cpu-x64.zip` on Windows 11).
   - Verify the archive SHA-256 against the GitHub release digest.
   - Extract all `.exe` and `.dll` files into `%LOCALAPPDATA%\AI DSA Mentor\runtimes\llama.cpp` (or set `AIDSA_LLAMA_SERVER_PATH` to the full path of `llama-server.exe`).
   - Do **not** separate `llama-server.exe` from its accompanying `.dll` files (`llama-server-impl.dll`, `llama.dll`, `ggml*.dll`, `libomp.dll`).
