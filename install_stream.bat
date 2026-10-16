@echo off
if not defined ANDROID_HOME set "ANDROID_HOME=E:\AndroidSdk"
set "ADB=%ANDROID_HOME%\platform-tools\adb.exe"
set "SERIAL=%~1"
if "%SERIAL%"=="" set "SERIAL=192.168.1.8:37643"
set "APK=E:\Project\Smartjirani\app\build\outputs\apk\debug\app-debug.apk"

echo SERIAL=%SERIAL%
echo APK=%APK%
"%ADB%" -s "%SERIAL%" install -r "%APK%"
echo INSTALL_EXIT_CODE=%ERRORLEVEL%
