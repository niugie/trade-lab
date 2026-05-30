#!/usr/bin/env bash
set -euo pipefail

if command -v conda &>/dev/null; then
  conda deactivate 2>/dev/null || true
fi

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

exec mvn -s "$ROOT/.mvn/settings.xml" "$@"
