@echo off
setlocal

echo ====================================
echo Stopping All Services
echo ====================================
echo.

echo [1/2] Stopping backend service (port 8080)...
cd /d "D:\AI\reference_material_management\backend"
set "STOPPED=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo   Found backend process (PID: %%a), stopping...
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] Backend stopped (PID: %%a)
        set "STOPPED=1"
    ) else (
        echo   [Warning] Could not kill %%a, trying alternative...
        wmic process where "ProcessId=%%a" call terminate >nul 2>&1
    )
)

if "STOPPED"=="0" (
    echo   [INFO] No backend service found to stop
) else (
    echo   [INFO] Backend service stopped
)

echo.
echo [2/2] Stopping frontend service (port 3002)...
cd /d "D:\AI\reference_material_management\frontend"
set "STOPPED=0"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3002"') do (
    echo   Found frontend process (PID: %%a), stopping...
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] Frontend stopped (PID: %%a)
        set "STOPPED=1"
    ) else (
        echo   [Warning] Could not kill %%a, trying alternative...
        for /f "tokens=2" %%b in ('tasklist ^| findstr "node.exe"') do (
            wmic process where "ProcessId=%%b" get commandline 2>nul | findstr /i "vite\|node" >nul && taskkill /F /PID %%b >nul 2>&1
        )
    )
)

if "STOPPED"=="0" (
    echo   [INFO] No frontend service found to stop
) else (
    echo   [INFO] Frontend service stopped
)

echo.
echo ====================================
echo All services stopped
echo ====================================
echo.

echo Port status:
netstat -ano | findstr ":3002\|:8080"

echo.
echo Services stopped successfully.
echo.
pause
