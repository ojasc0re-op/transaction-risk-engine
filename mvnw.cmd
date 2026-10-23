@echo off
set "JAVA_HOME=C:\Users\ojasg\.gemini\antigravity\scratch\jdk-21.0.4+7"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if exist "C:\Users\ojasg\.gemini\antigravity\scratch\apache-maven-3.9.8\bin\mvn.cmd" (
    call "C:\Users\ojasg\.gemini\antigravity\scratch\apache-maven-3.9.8\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

mvn %*
exit /b %ERRORLEVEL%
