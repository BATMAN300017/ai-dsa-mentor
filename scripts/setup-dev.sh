#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
echo "=== AI DSA Mentor: Unix Dev Setup ==="

if ! command -v java >/dev/null 2>&1; then
  echo "Error: Java 21+ is required on PATH or via JAVA_HOME." >&2
  exit 1
fi
java -version

if ! command -v node >/dev/null 2>&1; then
  echo "Error: Node.js 20+ is required." >&2
  exit 1
fi
echo "Node: $(node -v), npm: $(npm -v)"

echo "Installing frontend dependencies..."
(cd "$REPO_ROOT/apps/frontend" && npm install)

echo "Installing browser-extension dependencies..."
(cd "$REPO_ROOT/apps/browser-extension" && npm install)

echo "Compiling Maven workspace..."
cd "$REPO_ROOT"
if [ -x "./mvnw" ]; then
  ./mvnw test-compile
elif command -v mvn >/dev/null 2>&1; then
  mvn test-compile
else
  echo "Error: Neither ./mvnw nor mvn found." >&2
  exit 1
fi

echo "Setup complete!"
echo "  1. Start backend:  ./mvnw -pl apps/backend -am spring-boot:run"
echo "  2. Start frontend: (cd apps/frontend && npm run dev)"
echo "  3. Start desktop:  ./mvnw -pl apps/desktop -am javafx:run"
