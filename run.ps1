# Transaction Risk & Fraud Detection Platform PowerShell Launcher

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "  Starting Transaction Risk & Fraud Detection Platform" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan

$embeddedJava = "C:\Users\ojasg\.gemini\antigravity\scratch\jdk-21.0.4+7\bin\java.exe"
$jarPath = "target\fraud-detection-platform-1.0.0.jar"

if (-not (Test-Path $jarPath)) {
    $jarPath = "fraud-detection-platform-1.0.0.jar"
}

$javaCmd = if (Test-Path $embeddedJava) { $embeddedJava } else { "java" }

Write-Host "Using Java: $javaCmd" -ForegroundColor Gray
Write-Host "Using JAR:  $jarPath" -ForegroundColor Gray
Write-Host "Profile:    dev (In-Memory H2 DB, zero external dependencies required)" -ForegroundColor Gray
Write-Host ""
Write-Host "Dashboard:  http://localhost:8080/" -ForegroundColor Green
Write-Host "Swagger UI: http://localhost:8080/swagger-ui.html" -ForegroundColor Green
Write-Host ""
Write-Host "Press Ctrl+C to stop the server." -ForegroundColor Yellow
Write-Host "======================================================================" -ForegroundColor Cyan

& $javaCmd -jar $jarPath --spring.profiles.active=dev
