@echo off
setlocal

set JAVA_HOME=C:\Program Files\Java\jdk-17.0.2
set PATH=%JAVA_HOME%\bin;%PATH%

echo ====================================
echo Building and Running Backend
echo ====================================
echo.

echo Java Home: %JAVA_HOME%
echo Java Version:
java -version
echo.

echo [0/3] Checking for existing backend services...
cd /d "D:\AI\reference_material_management\backend"

REM Check and kill any existing Java process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080"') do (
    echo Found existing process on port 8080, PID: %%a
    taskkill /F /PID %%a >nul 2>&1
    if !errorlevel! equ 0 (
        echo [Killed] Stopped existing backend service (PID: %%a)
    ) else (
        echo [Warning] Could not kill process %%a, trying alternative method...
        wmic process where "ProcessId=%%a" call terminate >nul 2>&1
    )
)

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

echo [3/3] Starting application...
echo.
call mvn spring-boot:run

pause
