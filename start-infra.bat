@echo off
cd /d "%~dp0"

echo === Checking Docker Desktop ===
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo Docker Desktop not running, starting it...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    echo Waiting for Docker to initialize...
    :wait_docker
    timeout /t 3 /nobreak >nul
    docker info >nul 2>&1
    if %errorlevel% neq 0 goto wait_docker
    echo Docker Desktop is ready.
)

echo.
echo === Starting Redis + RabbitMQ (MySQL uses local) ===
docker compose up -d redis rabbitmq

echo.
echo === All infrastructure services ready ===
echo MySQL (local): localhost:3306
echo Redis (Docker): localhost:6379
echo RabbitMQ (Docker): localhost:5572 ^(management: http://localhost:15672^)
