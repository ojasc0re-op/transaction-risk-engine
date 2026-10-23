@echo off
setlocal
title Transaction Risk and Fraud Detection Platform

echo ======================================================================
echo   Starting Transaction Risk & Fraud Detection Platform
echo ======================================================================

set "EMBEDDED_JAVA=C:\Users\ojasg\.gemini\antigravity\scratch\jdk-21.0.4+7\bin\java.exe"
set "JAR_PATH=target\fraud-detection-platform-1.0.0.jar"

if not exist "%JAR_PATH%" (
    set "JAR_PATH=fraud-detection-platform-1.0.0.jar"
)

if exist "%EMBEDDED_JAVA%" (
    set "JAVA_CMD=%EMBEDDED_JAVA%"
) else (
    set "JAVA_CMD=java"
)

echo Using Java: %JAVA_CMD%
echo Using JAR:  %JAR_PATH%
echo Profile:    dev (In-Memory H2 DB, zero external dependencies required)
echo.
echo Dashboard URL: http://localhost:8080/
echo Swagger URL:   http://localhost:8080/swagger-ui.html
echo.
echo Press Ctrl+C at any time to stop the server.
echo ======================================================================

"%JAVA_CMD%" -jar "%JAR_PATH%" --spring.profiles.active=dev
pause
