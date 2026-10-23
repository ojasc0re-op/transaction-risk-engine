$loginBody = @{
    username = "john_doe"
    password = "password"
} | ConvertTo-Json

Write-Host "Logging in as john_doe..."
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
Write-Host "Login successful!"
Write-Host "Token: $($loginRes.token.Substring(0, 20))..."
Write-Host "Roles: $($loginRes.roles -join ', ')"

$token = $loginRes.token
$headers = @{
    Authorization = "Bearer $token"
    "Content-Type" = "application/json"
}

# 1. Normal Transaction (Expected: LOW risk / APPROVED)
Write-Host "`n--- Testing Normal Transaction (LOW risk) ---"
$txn1 = @{
    amount = 50.00
    currency = "USD"
    merchant = "Amazon"
    transactionType = "PURCHASE"
    location = "New York, NY"
    latitude = 40.7128
    longitude = -74.0060
    deviceId = "device-known-001"
    ipAddress = "192.168.1.10"
} | ConvertTo-Json

$res1 = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" -Method Post -Headers $headers -Body $txn1
Write-Host "Txn 1 Status: $($res1.status), Risk Score: $($res1.riskScore), Level: $($res1.riskLevel)"

# 2. Suspicious Large + New Device Transaction (Expected: HIGH or CRITICAL risk / REVIEW or BLOCKED)
Write-Host "`n--- Testing Fraudulent Transaction (HIGH/CRITICAL risk) ---"
$txn2 = @{
    amount = 3500.00
    currency = "USD"
    merchant = "Online Casino Royal"
    transactionType = "PURCHASE"
    location = "Tokyo, Japan"
    latitude = 35.6762
    longitude = 139.6503
    deviceId = "device-unknown-attacker"
    ipAddress = "192.168.100.1"
} | ConvertTo-Json

$res2 = Invoke-RestMethod -Uri "http://localhost:8080/api/transactions" -Method Post -Headers $headers -Body $txn2
Write-Host "Txn 2 Status: $($res2.status), Risk Score: $($res2.riskScore), Level: $($res2.riskLevel)"

# 3. Check Risk Assessment Details
Write-Host "`n--- Querying Risk Assessment for Txn 2 ---"
$risk = Invoke-RestMethod -Uri "http://localhost:8080/api/risk/transaction/$($res2.id)" -Method Get -Headers $headers
Write-Host "Decision: $($risk.decision), Score: $($risk.riskScore), Reasons:"
$risk.reasons | ForEach-Object { Write-Host " - $_" }

# 4. Admin Dashboard Stats
Write-Host "`n--- Testing Admin Login & Dashboard Stats ---"
$adminBody = @{
    username = "admin"
    password = "password"
} | ConvertTo-Json

$adminRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json" -Body $adminBody
$adminHeaders = @{
    Authorization = "Bearer $($adminRes.token)"
}

$stats = Invoke-RestMethod -Uri "http://localhost:8080/api/dashboard/stats" -Method Get -Headers $adminHeaders
Write-Host "Total Txns: $($stats.totalTransactions)"
Write-Host "Approved: $($stats.approvedTransactions), Under Review: $($stats.reviewTransactions), Blocked: $($stats.blockedTransactions)"
Write-Host "Average Risk Score: $($stats.averageRiskScore)"
Write-Host "High Risk Count: $($stats.highRiskCount)"
Write-Host "Risk Distribution:"
$stats.riskDistribution.psobject.properties | ForEach-Object { Write-Host " - $($_.Name): $($_.Value)" }

$adminStats = Invoke-RestMethod -Uri "http://localhost:8080/api/admin/risk-statistics" -Method Get -Headers $adminHeaders
Write-Host "Admin Risk Stats call succeeded: Total $($adminStats.totalTransactions) transactions logged."

