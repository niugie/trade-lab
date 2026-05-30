#!/usr/bin/env bash
set -euo pipefail

# Exit conda - it often breaks Maven dependency resolution in WSL
if command -v conda &>/dev/null; then
  conda deactivate 2>/dev/null || true
fi

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "JAVA_HOME=$JAVA_HOME"
java -version

SETTINGS="$ROOT/.mvn/settings.xml"
echo "Downloading dependencies..."
mvn -s "$SETTINGS" -U dependency:resolve

echo "Starting application..."
mvn -s "$SETTINGS" spring-boot:run
