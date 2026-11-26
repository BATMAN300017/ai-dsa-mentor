# Model Setup Guide

## Selected Model

- **Model**: `Qwen2.5-Coder-3B-Instruct`
- **Quantization**: `Q4_K_M`
- **Format**: `GGUF`
- **Expected Filename**: `qwen2.5-coder-3b-instruct-q4_k_m.gguf`
- **Repository**: https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF
- **Planned Runtime**: `llama.cpp`

## Task 1 Status

- The descriptor lives at `resources/models/qwen2.5-coder-3b/model-info.json` with `downloadStatus: "NOT_DOWNLOADED"`.
- No ~2 GB binary is downloaded in Task 1.
- `/api/v1/health` accurately reports `"modelLoaded": false`.

## Future Storage Strategy (Packaged App)

Packaged installations cannot assume write access to the installation directory (`Program Files` on Windows, `/Applications` on macOS, `/opt` or `/usr` on Linux). Therefore, the `model-manager` module resolves user-writable application-data paths:

- **Windows**: `%LOCALAPPDATA%\AI DSA Mentor\models\qwen2.5-coder-3b`
- **macOS**: `~/Library/Application Support/AI DSA Mentor/models/qwen2.5-coder-3b`
- **Linux**: `${XDG_DATA_HOME:-~/.local/share}/ai-dsa-mentor/models/qwen2.5-coder-3b`
