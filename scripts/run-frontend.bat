@echo off
setlocal

echo ====================================
echo Starting Frontend
echo ====================================
echo.

echo [0/3] Checking for existing frontend services...
cd /d "D:\AI\reference_material_management\frontend"

REM Check and kill any existing Node/Vite process on port 3002
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3002"') do (
    echo Found existing process on port 3002, PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo [Killed] Stopped existing frontend service (PID: %%a)
    ) else (
        echo [Warning] Could not kill process %%a, trying alternative method...
        for /f "tokens=2" %%b in ('tasklist ^| findstr "node.exe"') do (
            wmic process where "ProcessId=%%b" get commandline 2>nul | findstr "vite" >nul && taskkill /F /PID %%b >nul 2>&1
        )
    )
    timeout /t 2 >nul
)

echo.
echo [1/3] Installing dependencies...
if not exist "node_modules" (
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
echo [2/3] Starting frontend development server...
echo.
call npm run dev -- --port 3002

pause
