@echo off
:: Shared build script for Android APKs
:: Usage: call build_android.bat [debug|release]
:: Returns APK_PATH variable with the built APK location

setlocal enabledelayedexpansion

:: Change to tools directory (where this script lives)
cd /d "%~dp0"

:: Check for debug parameter
set "BUILD_MODE=release"
if /i "%~1"=="debug" set "BUILD_MODE=debug"

:: Path to shared config
set "CONFIG=%~dp0config.bat"

:: Ensure config exists
if not exist "%CONFIG%" (
    echo ERROR: Config file "%CONFIG%" not found.
    echo Please create config.bat based on config_example.bat
    exit /b 1
)

:: Load config to get LOCAL_DIR
call "%CONFIG%"

:: Set paths based on build mode
if "%BUILD_MODE%"=="debug" (
    set "TARGET_DIR=%LOCAL_DIR_DEBUG%"
    set "APK_PATH=%LOCAL_DIR_DEBUG%\%FILENAME_DEBUG%"
    set "GRADLE_TASK=assembleDebug"
) else (
    set "TARGET_DIR=%LOCAL_DIR%"
    set "APK_PATH=%LOCAL_DIR%\%FILENAME%"
    set "GRADLE_TASK=assembleRelease"
)

:: Step 1: Clean existing APKs
echo [Build Step 1/3] Cleaning existing APKs from !TARGET_DIR!
if exist "!TARGET_DIR!" (
    del /q "!TARGET_DIR!\*.apk" 2>nul
)
echo.

:: Step 2: Build Android APK using Gradle
echo [Build Step 2/3] Building Android %BUILD_MODE% APK...
cd ..
call gradlew.bat %GRADLE_TASK%
set "BUILD_RESULT=%ERRORLEVEL%"
cd tools

if %BUILD_RESULT% neq 0 (
    echo.
    echo ERROR: Build failed!
    exit /b 1
)
echo.

:: Step 3: Verify build success
echo [Build Step 3/3] Verifying build...
if not exist "!APK_PATH!" (
    echo.
    echo ERROR: APK not found at "!APK_PATH!"
    echo Build may have failed.
    exit /b 1
)

:: Get APK file size and print info
for %%A in ("!APK_PATH!") do (
    set "APK_SIZE=%%~zA"
    set /a "APK_SIZE_MB=!APK_SIZE! / 1048576"
    echo    - APK found: %%~fA (!APK_SIZE_MB! MB^)
    echo.
)

:: Export APK_PATH to caller
endlocal & set "APK_PATH=%APK_PATH%"
exit /b 0
