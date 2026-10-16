@echo off
cd /d E:\Project\Smartjirani
call gradlew.bat lintDebug --console=plain > lint_debug.log 2>&1
echo EXIT=%ERRORLEVEL% >> lint_debug.log
call gradlew.bat test --console=plain > test_run.log 2>&1
echo EXIT=%ERRORLEVEL% >> test_run.log
call gradlew.bat assembleDebug --console=plain > assemble_debug.log 2>&1
echo EXIT=%ERRORLEVEL% >> assemble_debug.log
