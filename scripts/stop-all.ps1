# Stop trade-lab Java processes on Windows (run before mvn package)
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
Write-Host "Windows Java processes stopped." -ForegroundColor Green
Write-Host "Also run in WSL if needed: ./scripts/stop-all.sh" -ForegroundColor Yellow
