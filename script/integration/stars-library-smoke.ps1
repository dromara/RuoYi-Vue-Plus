# Stars Library integration smoke test
# Usage: powershell -File script/integration/stars-library-smoke.ps1

$ErrorActionPreference = 'Stop'
$BaseUrl = if ($env:STARS_API_BASE) { $env:STARS_API_BASE } else { 'http://localhost:8080' }
$ClientId = 'e5cd7e4891bf95d1d19206ce24a7b32e'
$Username = if ($env:STARS_TEST_USER) { $env:STARS_TEST_USER } else { 'admin' }
$Password = if ($env:STARS_TEST_PASS) { $env:STARS_TEST_PASS } else { 'admin123' }

function Write-Step($msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }
function Pass($msg) { Write-Host "  [PASS] $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "  [FAIL] $msg" -ForegroundColor Red; exit 1 }
function Warn($msg) { Write-Host "  [WARN] $msg" -ForegroundColor Yellow }

$headers = @{ clientid = $ClientId; 'Content-Type' = 'application/json' }

try {
    Write-Step "Backend health $BaseUrl"
    $ping = Invoke-WebRequest -Uri $BaseUrl -UseBasicParsing -TimeoutSec 10
    Pass "HTTP $($ping.StatusCode)"

    Write-Step "GET /auth/code"
    $codeResp = Invoke-RestMethod -Uri "$BaseUrl/auth/code" -Headers @{ clientid = $ClientId } -Method Get
    if ($codeResp.code -ne 200) { Fail $codeResp.msg }
    $captchaEnabled = $codeResp.data.captchaEnabled
    Pass "captchaEnabled=$captchaEnabled"

    Write-Step "POST /auth/login ($Username)"
    $loginBody = @{
        clientId  = $ClientId
        grantType = 'password'
        tenantId  = '000000'
        username  = $Username
        password  = $Password
    }
    if ($captchaEnabled) {
        Warn "Captcha enabled - login may fail without valid code"
        $loginBody.code = '0'
        $loginBody.uuid = $codeResp.data.uuid
    }
    $loginResp = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post -Headers $headers -Body ($loginBody | ConvertTo-Json)
    if ($loginResp.code -ne 200) { Fail "login: $($loginResp.msg)" }
    $token = $loginResp.data.access_token
    Pass "token length $($token.Length)"

    $authHeaders = @{
        clientid       = $ClientId
        Authorization  = "Bearer $token"
        'Content-Type' = 'application/json'
    }

    $endpoints = @(
        @{ Name = 'github/status'; Url = '/stars/github/status' },
        @{ Name = 'repos list';    Url = '/stars/repos?pageNum=1&pageSize=10' },
        @{ Name = 'tags';          Url = '/stars/tags' },
        @{ Name = 'import jobs';   Url = '/stars/import/jobs' }
    )

    foreach ($ep in $endpoints) {
        Write-Step "GET $($ep.Url)"
        $resp = Invoke-RestMethod -Uri ($BaseUrl + $ep.Url) -Headers $authHeaders -Method Get
        if ($resp.code -eq 200) {
            Pass "$($ep.Name) OK"
        } elseif ($resp.code -eq 403) {
            Warn "$($ep.Name) 403 - assign stars:* permissions to admin role"
        } else {
            Fail "$($ep.Name): $($resp.msg)"
        }
    }

    Write-Step "Frontend proxy http://localhost:5173/dev-api/auth/code"
    try {
        $proxy = Invoke-RestMethod -Uri 'http://localhost:5173/dev-api/auth/code' -Headers @{ clientid = $ClientId } -TimeoutSec 5
        if ($proxy.code -eq 200) { Pass 'Vite proxy OK' } else { Warn "proxy code=$($proxy.code)" }
    } catch {
        Warn "Frontend not running: $($_.Exception.Message)"
    }

    Write-Host ""
    Write-Host "Smoke test finished." -ForegroundColor Green
} catch {
    Fail $_.Exception.Message
}
