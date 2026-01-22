@echo off
REM kafka-lens - Single JAR Build Script (Windows)
REM Builds frontend (Next.js static export) and packages into backend JAR

setlocal enabledelayedexpansion

REM Project directories
set "PROJECT_ROOT=%~dp0"
set "FRONTEND_DIR=%PROJECT_ROOT%kafka-lens-frontend"
set "BACKEND_DIR=%PROJECT_ROOT%kafka-lens-backend"
set "STATIC_DIR=%BACKEND_DIR%\src\main\resources\static"

REM Check requirements
echo ==========================================
echo   kafka-lens - Single JAR Build
echo ==========================================
echo.

call :check_requirements
if errorlevel 1 goto :error

call :build_frontend
if errorlevel 1 goto :error

call :copy_static_files
if errorlevel 1 goto :error

call :build_backend
if errorlevel 1 goto :error

call :print_summary
goto :eof

:check_requirements
echo [INFO] Checking requirements...

where node >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Node.js is not installed. Please install Node.js 18+
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm is not installed.
    exit /b 1
)

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed. Please install Java 21+
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed. Please install Maven 3.9+
    exit /b 1
)

echo [INFO] All requirements satisfied.
exit /b 0

:build_frontend
echo [INFO] Building frontend (Next.js)...

cd /d "%FRONTEND_DIR%"

REM Install dependencies if node_modules doesn't exist
if not exist "node_modules" (
    echo [INFO] Installing npm dependencies...
    call npm install
    if errorlevel 1 exit /b 1
)

REM Build (static export)
call npm run build
if errorlevel 1 (
    echo [ERROR] Frontend build failed
    exit /b 1
)

REM Verify build output
if not exist "out" (
    echo [ERROR] Frontend build failed: 'out' directory not found
    exit /b 1
)

echo [INFO] Frontend build completed.
exit /b 0

:copy_static_files
echo [INFO] Copying frontend build to backend static resources...

REM Create static directory if not exists
if not exist "%STATIC_DIR%" mkdir "%STATIC_DIR%"

REM Clean existing static files
if exist "%STATIC_DIR%" (
    rd /s /q "%STATIC_DIR%"
    mkdir "%STATIC_DIR%"
)

REM Copy frontend build output
xcopy /s /e /y "%FRONTEND_DIR%\out\*" "%STATIC_DIR%\"

REM Verify copy
if not exist "%STATIC_DIR%\index.html" (
    echo [ERROR] Static file copy failed: index.html not found
    exit /b 1
)

echo [INFO] Static files copied successfully.
exit /b 0

:build_backend
echo [INFO] Building backend (Spring Boot)...

cd /d "%BACKEND_DIR%"

REM Maven build
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [ERROR] Backend build failed
    exit /b 1
)

REM Verify JAR exists
if not exist "target\kafka-lens-backend-0.0.1-SNAPSHOT.jar" (
    echo [ERROR] Backend build failed: JAR file not found
    exit /b 1
)

echo [INFO] Backend build completed.
exit /b 0

:print_summary
echo.
echo ==========================================
echo   Build completed successfully!
echo ==========================================
echo.
echo JAR file location:
echo   %BACKEND_DIR%\target\kafka-lens-backend-0.0.1-SNAPSHOT.jar
echo.
echo Run the application:
echo   java -jar %BACKEND_DIR%\target\kafka-lens-backend-0.0.1-SNAPSHOT.jar
echo.
echo Access the application:
echo   http://localhost:8080
echo.
exit /b 0

:error
echo.
echo [ERROR] Build failed. See error messages above.
exit /b 1
