#!/usr/bin/env bash
# Stop trade-lab Java processes in WSL (run before mvn package on /mnt/d)
pkill -f 'trade-lab-1.0.0-SNAPSHOT.jar' 2>/dev/null || true
pkill -f 'trade-lab' 2>/dev/null || true
sleep 1
if pgrep -f 'trade-lab' >/dev/null 2>&1; then
  echo "Force killing..."
  pkill -9 -f 'trade-lab' 2>/dev/null || true
fi
echo "WSL trade-lab processes stopped."
echo "If mvn package still fails, also run in PowerShell:"
echo "  Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force"
