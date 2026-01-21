@echo off
setlocal enabledelayedexpansion

:: Path to shared config
set "CONFIG=%~dp0config.bat"

:: Ensure config exists
if not exist "%CONFIG%" (
    echo ERROR: Config file "%CONFIG%" not found.
    pause
    exit /b
)

:: Load config
call "%CONFIG%"

:: Ensure rclone exists
set "RCLONE=%~dp0rclone.exe"
if not exist "%RCLONE%" (
    echo ERROR: rclone.exe not found at "%RCLONE%".
    echo Download from https://rclone.org/downloads/ and place it in the tools folder.
    pause
    exit /b
)

cd /d "%~dp0"

:: Require at least one .apk file in LOCAL_DIR_DEBUG
if not exist "%LOCAL_DIR_DEBUG%\*.apk" (
    echo(
    echo No .apk file found in "%LOCAL_DIR_DEBUG%"
    echo(
    pause
    exit /b
)

:: Use the first .apk file found
for /f "delims=" %%i in ('dir /b "%LOCAL_DIR_DEBUG%\*.apk"') do (
    set "APK_FILENAME=%%i"
    goto found_apk
)
:found_apk

:: Setup Rclone FTP remote
"%RCLONE%" config create ftp-remote ftp host "%FTP_HOST%" user "%FTP_USER%" pass "%FTP_PASS%" >nul 2>&1

:: Upload debug APK with -debug suffix
set "TARGET_DEBUG_NAME=%TARGET_APK_NAME:.apk=-debug.apk%"
"%RCLONE%" copyto "%LOCAL_DIR_DEBUG%\%APK_FILENAME%" "ftp-remote:%REMOTE_DIR%%TARGET_DEBUG_NAME%" --progress

:: Get last modified date of the apk file
for %%i in ("%LOCAL_DIR_DEBUG%\%APK_FILENAME%") do set "APK_DATE=%%~ti"

:: Get version from libs.versions.toml
set "VERSIONS_FILE=%~dp0..\gradle\libs.versions.toml"
if exist "%VERSIONS_FILE%" (
    for /f "tokens=2 delims== " %%a in ('findstr /r "^versionName" "%VERSIONS_FILE%"') do (
        set "VERSION=%%~a"
    )
) else (
    set "VERSION=(libs.versions.toml not found)"
)

echo(
echo Version: %VERSION% (debug)
echo Last modified: %APK_DATE%
echo Uploaded to: %APK_LINK_DIR%/%TARGET_DEBUG_NAME%
echo(

endlocal
