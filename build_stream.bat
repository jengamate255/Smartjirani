@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-21"
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
cd /d E:\Project\Smartjirani
call "E:\Project\Smartjirani\gradlew.bat" --no-daemon assembleDebug
echo EXIT_CODE=%ERRORLEVEL% >> E:\Project\Smartjirani\build_stream.log
