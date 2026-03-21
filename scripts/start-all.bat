@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-17.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

echo ====================================
echo Starting All Services (Backend + Frontend)
echo ====================================
echo.

echo [Step 1] Stopping any existing services...
echo.

REM Stop backend services on port 8080
cd /d "D:\AI\reference_material_management\backend"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo   Found backend process on port 8080, stopping... (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] Backend service stopped (PID: %%a)
    ) else (
        echo   [Warning] Could not kill process %%a, trying alternative...
        wmic process where "ProcessId=%%a" call terminate >nul 2>&1
        )
    timeout /t 1 >nul
)

REM Stop frontend services on port 3002
cd /d "D:\AI\reference_material_management\frontend"
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":3002"') do (
    echo   Found frontend process on port 3002, stopping... (PID: %%a)
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo   [OK] Frontend service stopped (PID: %%a)
    ) else (
        echo   [Warning] Could not kill process %%a, trying alternative...
        for /f "tokens=2" %%b in ('tasklist ^| findstr "node.exe"') do (
            wmic process where "ProcessId=%%b" get commandline 2>nul | findstr /i "vite\|node" >nul && taskkill /F /PID %%b >nul 2>&1
        )
    )
    timeout /t 1 >nul
)

timeout /t 2 >nul
echo.

echo [Step 2] Starting backend service...
echo.

start cmd /c "D:\AI\reference_material_management\scripts\run-backend-bg.bat"

echo.
echo [Step 3] Starting frontend service...
echo.

start cmd /c "D:\AI\reference_material_management\scripts\run-frontend-bg.bat"

echo.
echo ====================================
echo All services starting...
echo.
echo Services will start in background
echo.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:3002
echo API Docs: http://localhost:8080/doc.html
echo.
echo ====================================
echo.
echo Services started! Check the separate console windows for logs.
echo.
echo Press Ctrl+C here to close this monitoring script.
echo.

echo Waiting for services to initialize...
timeout /t 45 >nul

echo.
echo ====================================
echo Service Status Check
echo ====================================
echo.

echo Backend status:
netstat -ano | findstr ":8080"
echo.
echo Frontend status:
netstat -ano | findstr ":3002"
echo.

echo.
echo ====================================
echo If both services are running, you can access the application:
echo   Frontend: http://localhost:3002
echo   Backend:  http://localhost:8080
echo   API Docs: http://localhost:8080/doc.html
echo.

pause
