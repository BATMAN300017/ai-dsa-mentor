#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL_INFO="$REPO_ROOT/resources/models/qwen2.5-coder-3b/model-info.json"

case "$(uname -s)" in
  Darwin*) TARGET_DIR="$HOME/Library/Application Support/AI DSA Mentor/models/qwen2.5-coder-3b" ;;
  *)       TARGET_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/ai-dsa-mentor/models/qwen2.5-coder-3b" ;;
esac

echo "=== AI DSA Mentor Model Downloader (Task 1 Placeholder) ==="
echo "Automated GGUF download and SHA-256 verification belong to a later task (Model Manager)."
echo "Metadata descriptor: $MODEL_INFO"
echo "Planned OS AppData destination: $TARGET_DIR"
if [ -f "$MODEL_INFO" ]; then
  cat "$MODEL_INFO"
fi
