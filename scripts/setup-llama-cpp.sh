#!/usr/bin/env bash
set -euo pipefail

RELEASE_TAG="b12760"

case "$(uname -s)-$(uname -m)" in
  Darwin-arm64)
    ASSET="llama-${RELEASE_TAG}-bin-macos-arm64.tar.gz"
    EXPECTED_SHA="396fb1c5baef44c8b6b70d43c11df7cee4935010748df7803da5b924761c9c60"
    RUNTIME_DIR="$HOME/Library/Application Support/AI DSA Mentor/runtimes/llama.cpp"
    ;;
  Linux-x86_64)
    ASSET="llama-${RELEASE_TAG}-bin-ubuntu-x64.tar.gz"
    EXPECTED_SHA="5e4143974f92be87cbe534e10d270ceccc61352889e78b99a2a20c9837738513"
    RUNTIME_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/ai-dsa-mentor/runtimes/llama.cpp"
    ;;
  *)
    echo "Unsupported OS/arch for automated setup: $(uname -s)-$(uname -m). Please install llama-server manually." >&2
    exit 1
    ;;
esac

URL="https://github.com/ggml-org/llama.cpp/releases/download/${RELEASE_TAG}/${ASSET}"
TMP_TAR="$(mktemp)"
trap 'rm -f "$TMP_TAR"' EXIT

echo "Downloading official ggml-org/llama.cpp ${RELEASE_TAG} (${ASSET})..."
curl -fL -o "$TMP_TAR" "$URL"

if command -v sha256sum >/dev/null 2>&1; then
  ACTUAL_SHA="$(sha256sum "$TMP_TAR" | awk '{print tolower($1)}')"
else
  ACTUAL_SHA="$(shasum -a 256 "$TMP_TAR" | awk '{print tolower($1)}')"
fi

if [ "$ACTUAL_SHA" != "$EXPECTED_SHA" ]; then
  echo "SHA-256 verification failed: expected $EXPECTED_SHA, got $ACTUAL_SHA" >&2
  exit 1
fi

mkdir -p "$RUNTIME_DIR"
tar -xzf "$TMP_TAR" -C "$RUNTIME_DIR"
echo "Verified and extracted llama.cpp into: $RUNTIME_DIR"
