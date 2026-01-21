:: config.bat - FastTrack build configuration
@echo off

:: Keystore configuration for release builds
set "KEYSTORE_PASSWORD=12345"
set "KEY_PASSWORD=12345"
set "KEY_ALIAS=test"

:: Public link base where APKs are served from
set "APK_LINK_DIR=https://yourdomain.com/apps"

:: Remote FTP server details
set "FTP_HOST=yourdomain.com"
set "FTP_USER=ftpuser"
set "FTP_PASS=ftppasss"

:: Local directory containing APKs (Gradle output)
set "LOCAL_DIR=%~dp0..\app\build\outputs\apk\release"
set "LOCAL_DIR_DEBUG=%~dp0..\app\build\outputs\apk\debug"

set "FILENAME=app-release.apk"
set "FILENAME_DEBUG=app-debug.apk"

:: Remote directory on FTP (root is /)
set "REMOTE_DIR=/yourfolder/"

:: Target filename on FTP
set "TARGET_APK_NAME=fasttrack.apk"
