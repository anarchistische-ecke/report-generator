@echo off
setlocal

cd /d "%~dp0"
echo Report Generator
echo.
echo Enter arguments, for example:
echo   --from 2025-08-20 --to 2025-09-22 --db-user SA --db-pass "YourStrong!Passw0rd" --output C:\reports
echo.
set /p ARGS=Args: 

if "%ARGS%"=="" (
  echo No arguments provided. Exiting.
  pause
  exit /b 1
)

call "bin\\report-generator.bat" %ARGS%
echo.
pause
