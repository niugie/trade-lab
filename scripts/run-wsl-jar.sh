#!/usr/bin/env bash
set -euo pipefail

if command -v conda &>/dev/null; then
  conda deactivate 2>/dev/null || true
fi

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$PATH"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

wait_for_port() {
  local host="$1" port="$2" label="$3" max="${4:-60}"
  local i=0
  while [ "$i" -lt "$max" ]; do
    if (echo >/dev/tcp/"$host"/"$port") 2>/dev/null; then
      echo "  OK: $label ($host:$port)"
      return 0
    fi
    i=$((i + 1))
    sleep 1
  done
  echo "ERROR: $label not reachable at $host:$port after ${max}s"
  return 1
}

echo "JAVA_HOME=$JAVA_HOME"
java -version

JAR="$ROOT/target/trade-lab-1.0.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
  echo ""
  echo "JAR not found. Build first:"
  echo "  ./scripts/build-wsl.sh"
  exit 1
fi

echo ""
echo "==> Starting Docker services (MySQL 3307, Redis 6379, RabbitMQ 5672)..."
if ! command -v docker &>/dev/null; then
  echo "ERROR: docker not found. Install Docker in WSL or use Windows:"
  echo "  .\\scripts\\run.ps1 -Local"
  exit 1
fi

docker compose up -d

echo "==> Waiting for services..."
wait_for_port 127.0.0.1 3307 "MySQL" 90 || {
  echo ""
  echo "MySQL failed to start. Check: docker compose ps && docker compose logs mysql"
  exit 1
}
wait_for_port 127.0.0.1 6379 "Redis" 30 || true
wait_for_port 127.0.0.1 5672 "RabbitMQ" 60 || true

echo ""
echo "Starting trade-lab..."
exec java -jar "$JAR"
