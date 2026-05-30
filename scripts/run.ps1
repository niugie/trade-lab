# Run on Windows PowerShell
param(
    [switch]$Local
)

$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Filter "jdk-17*" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($jdk) {
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$env:JAVA_HOME\bin;" + $env:Path
}

Set-Location (Split-Path -Parent $PSScriptRoot)
Write-Host "JAVA_HOME=$env:JAVA_HOME"
java -version

if ($Local) {
    Write-Host "Starting with local profile (H2, no Docker required)..."
    mvn spring-boot:run "-Dspring-boot.run.profiles=local"
    exit $LASTEXITCODE
}

function Test-PortOpen([string]$HostName, [int]$Port) {
    return (Test-NetConnection -ComputerName $HostName -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded
}

$dockerHost = "localhost"
if (-not (Test-PortOpen $dockerHost 3307)) {
    Write-Host "localhost:3307 not reachable, trying WSL IP..." -ForegroundColor Yellow
    $wslIp = (wsl hostname -I 2>$null).Trim().Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)[0]
    if ($wslIp -and (Test-PortOpen $wslIp 3307)) {
        $dockerHost = $wslIp
        Write-Host "Using WSL Docker host: $dockerHost" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "ERROR: Windows cannot reach Docker ports in WSL." -ForegroundColor Red
        Write-Host ""
        Write-Host "Option 1 - No Docker (Windows, quickest):" -ForegroundColor Yellow
        Write-Host "  .\scripts\run.ps1 -Local" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Option 2 - Full stack (build on Windows, run in WSL):" -ForegroundColor Yellow
        Write-Host "  mvn package -DskipTests" -ForegroundColor Cyan
        Write-Host "  wsl -e bash -lc 'cd /mnt/d/30323/Desktop/silder-tool && ./scripts/run-wsl-jar.sh'" -ForegroundColor Cyan
        Write-Host ""
        exit 1
    }
}

$env:SPRING_DATASOURCE_URL = "jdbc:mysql://${dockerHost}:3307/trade_lab?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:SPRING_DATA_REDIS_HOST = $dockerHost
$env:SPRING_RABBITMQ_HOST = $dockerHost

Write-Host "MySQL/Redis/RabbitMQ -> ${dockerHost}" -ForegroundColor Cyan
mvn spring-boot:run
