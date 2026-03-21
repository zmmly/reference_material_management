@echo off
setlocal

echo ====================================
echo Frontend Startup Script (Background Mode)
echo ====================================
echo.

echo [0/3] Checking for existing frontend services...
cd /d "D:\AI\reference_material_management\frontend"

REM Check and kill any existing Node/Vite process on port 3002
set "OLD_PID="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3002"') do (
    echo Found existing frontend process on port 3002, PID: %%a
    set "OLD_PID=%%a"
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo [Killed] Stopped existing frontend service (PID: %%a)
    ) else (
        echo [Warning] Could not kill process %%a, trying alternative method...
        for /f "tokens=2" %%b in ('tasklist ^| findstr "node.exe"') do (
            wmic process where "ProcessId=%%b" get commandline 2>nul | findstr /i "vite\|node" >nul && taskkill /F /PID %%b >nul 2>&1
        )
    )
    timeout /t 2 >nul
)

if not "%OLD_PID%"=="" timeout /t 2 >nul

echo.
echo [1/3] Installing dependencies...
if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo [Error] npm install failed
        pause
        exit /b 1
    )
) else (
    echo Dependencies already installed
)

echo.
echo [2/3] Starting frontend development server in background...
echo.
start cmd /c "cd /d D:\AI\reference_material_management\frontend && npm run dev -- --port 3002"

echo.
echo ====================================
echo Frontend started in background
echo Frontend URL: http://localhost:3002
echo ====================================
echo.
echo Press Ctrl+C to stop monitoring or close this window
echo.
echo To stop frontend, use: taskkill /F /IM node.exe
echo.

echo Monitoring frontend startup...
timeout /t 30 >nul
netstat -ano | findstr ":3002" >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] Frontend is running on port 3002
    echo.
    echo Frontend service details:
    netstat -ano | findstr ":3002"
) else (
    echo [ERROR] Frontend failed to start within 30 seconds
    echo.
    echo Check frontend console for details
)

echo.
echo Script completed. Frontend should be running in background.
