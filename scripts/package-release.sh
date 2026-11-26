#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "=== AI DSA Mentor Release Packaging (Task 1 Foundation) ==="
echo "Full jpackage OS installers belong to a later task."
echo "Building frontend production bundle and verifying Maven package..."

(cd "$REPO_ROOT/apps/frontend" && npm ci && npm run build)
cd "$REPO_ROOT"
if [ -x "./mvnw" ]; then
  ./mvnw -B clean verify
else
  mvn -B clean verify
fi

echo "Verification artifacts ready. jpackage installer scripts will assemble from apps/desktop/target in a future release task."
