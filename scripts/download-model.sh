#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL_INFO="$REPO_ROOT/resources/models/qwen2.5-coder-3b/model-info.json"

case "$(uname -s)" in
  Darwin*) TARGET_DIR="$HOME/Library/Application Support/AI DSA Mentor/models/qwen2.5-coder-3b" ;;
  *)       TARGET_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/ai-dsa-mentor/models/qwen2.5-coder-3b" ;;
esac

if [ ! -f "$MODEL_INFO" ]; then
  echo "Model metadata descriptor not found: $MODEL_INFO" >&2
  exit 1
fi

FILENAME="qwen2.5-coder-3b-instruct-q4_k_m.gguf"
REVISION="f74adce6aa16316c625447af059dbebe4983757c"
EXPECTED_SIZE=2104932800
UPSTREAM_SHA256="724fb256bec1ff062b2f65e4569e871ad2e95ab2a3989723d1769c54294730b7"
DOWNLOAD_URL="https://huggingface.co/Qwen/Qwen2.5-Coder-3B-Instruct-GGUF/resolve/${REVISION}/${FILENAME}"

mkdir -p "$TARGET_DIR"
TARGET_FILE="$TARGET_DIR/$FILENAME"
PART_FILE="$TARGET_FILE.part"

compute_sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print tolower($1)}'
  else
    shasum -a 256 "$1" | awk '{print tolower($1)}'
  fi
}

echo "=== AI DSA Mentor Model Downloader ==="
echo "Model:            Qwen2.5-Coder-3B-Instruct (Q4_K_M GGUF)"
echo "Pinned Revision:  $REVISION"
echo "Upstream SHA-256: $UPSTREAM_SHA256"
echo "Destination:      $TARGET_FILE"

if [ -f "$TARGET_FILE" ]; then
  ACTUAL_SHA="$(compute_sha256 "$TARGET_FILE")"
  if [ "$ACTUAL_SHA" = "$UPSTREAM_SHA256" ]; then
    echo "Model already installed and verified against upstream SHA-256."
    exit 0
  fi
  echo "Existing model checksum mismatch ($ACTUAL_SHA). Re-downloading..."
fi

curl -fL --retry 3 -C - -o "$PART_FILE" "$DOWNLOAD_URL"

ACTUAL_SIZE="$(wc -c < "$PART_FILE" | tr -d ' ')"
if [ "$ACTUAL_SIZE" -ne "$EXPECTED_SIZE" ]; then
  echo "Size mismatch: expected $EXPECTED_SIZE bytes, got $ACTUAL_SIZE bytes" >&2
  exit 1
fi

ACTUAL_SHA="$(compute_sha256 "$PART_FILE")"
if [ "$ACTUAL_SHA" != "$UPSTREAM_SHA256" ]; then
  rm -f "$PART_FILE"
  echo "SHA-256 mismatch: expected $UPSTREAM_SHA256, got $ACTUAL_SHA" >&2
  exit 1
fi

mv -f "$PART_FILE" "$TARGET_FILE"
echo "Verified and installed model at: $TARGET_FILE"
