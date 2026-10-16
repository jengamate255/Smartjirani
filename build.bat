@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set GRADLE_USER_HOME=%USERPROFILE%\.gradle
cd /d E:\Project\Smartjirani
"%JAVA_HOME%\bin\java.exe" -cp "%TEMP%\gradle_bin\gradle-8.11.1\lib\gradle-launcher-8.11.1.jar" org.gradle.launcher.GradleMain --no-daemon assembleDebug > E:\Project\Smartjirani\build_full.log 2>&1
echo EXIT_CODE=%ERRORLEVEL% >> E:\Project\Smartjirani\build_full.log
