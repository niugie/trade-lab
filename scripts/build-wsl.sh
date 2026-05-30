#!/usr/bin/env bash
# Build inside WSL without /mnt/d file-lock or rename issues.
# Output: target/trade-lab-1.0.0-SNAPSHOT.jar (copied from native Linux build dir)
set -euo pipefail

if command -v conda &>/dev/null; then
  conda deactivate 2>/dev/null || true
fi

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

ARTIFACT="trade-lab-1.0.0-SNAPSHOT.jar"
NATIVE_TARGET="${HOME}/.cache/trade-lab-build/target"
PROJECT_TARGET="$ROOT/target"

echo "==> Stopping running trade-lab processes..."
pkill -f 'trade-lab-1.0.0-SNAPSHOT.jar' 2>/dev/null || true
sleep 1

echo "==> JAVA_HOME=$JAVA_HOME"
java -version

echo "==> Building on native Linux FS (~/.cache/trade-lab-build)..."
mkdir -p "$NATIVE_TARGET"
mvn -s "$ROOT/.mvn/settings.xml" -Pwsl package -DskipTests "$@"

mkdir -p "$PROJECT_TARGET"
cp -f "$NATIVE_TARGET/$ARTIFACT" "$PROJECT_TARGET/$ARTIFACT"
rm -f "$PROJECT_TARGET/${ARTIFACT}.original" 2>/dev/null || true

echo ""
echo "BUILD SUCCESS"
echo "  JAR: $PROJECT_TARGET/$ARTIFACT"
ls -lh "$PROJECT_TARGET/$ARTIFACT"
