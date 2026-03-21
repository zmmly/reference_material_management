@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-17.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

echo ====================================
echo Backend Startup Script (Background Mode)
echo ====================================
echo.

echo Java Home: %JAVA_HOME%
echo Java Version:
java -version
echo.

echo [0/3] Checking for existing backend services...
cd /d "D:\AI\reference_material_management\backend"

REM Check and kill any existing Java process on port 8080
set "OLD_PID="
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo Found existing backend process on port 8080, PID: %%a
    set "OLD_PID=%%a"
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo [Killed] Stopped existing backend service (PID: %%a)
    ) else (
        echo [Warning] Could not kill process %%a, trying alternative method...
        wmic process where "ProcessId=%%a" call terminate >nul 2>&1
    )
)

if not "%OLD_PID%"=="" timeout /t 2 >nul

echo.
echo [1/3] Cleaning and compiling...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo [Error] Compilation failed
    pause
    exit /b 1
)

echo.
echo [2/3] Compilation successful
echo.

echo [3/3] Starting application in background...
echo.
start cmd /c "cd /d D:\AI\reference_material_management\backend && set JAVA_HOME=%JAVA_HOME% && set PATH=%JAVA_HOME%\bin;%%PATH%% && mvn spring-boot:run"

echo.
echo ====================================
echo Backend started in background
echo Backend URL: http://localhost:8080
echo API Docs: http://localhost:8080/doc.html
echo ====================================
echo.
echo Press Ctrl+C to stop monitoring or close this window
echo.
echo To stop the backend, use: taskkill /F /IM java.exe
echo.

echo Monitoring backend startup...
timeout /t 30 >nul
netstat -ano | findstr ":8080" >nul
if %errorlevel% equ 0 (
    echo [SUCCESS] Backend is running on port 8080
    echo.
    echo Backend service details:
    netstat -ano | findstr ":8080"
) else (
    echo [ERROR] Backend failed to start within 30 seconds
    echo.
    echo Check backend logs for details
)

echo.
echo Script completed. Backend should be running in background.
