<#
  Wealth Portal - one-click setup and start for Windows.

  What it does:
    1. Checks Java 25 (through Maven), Node and Docker
    2. Stops an old backend / frontend still running on ports 8090 / 4200
    3. Extracts wealth-portal.zip from Downloads into D:\Projects (replacing files)
    4. Starts Docker Desktop if needed, then PostgreSQL and Keycloak
    5. Starts the backend and the frontend, each in its own window
    6. Waits until everything answers, then opens the app and Swagger UI

  Run it from any PowerShell window:
    powershell -ExecutionPolicy Bypass -File "$env:USERPROFILE\Downloads\setup-wealth.ps1"

  Options:
    -SkipExtract      start what is already in D:\Projects (no unzip)
    -KeepKeycloak     do not reset Keycloak (keeps changes made in its admin console)
    -Stop             stop everything and exit
#>
param(
    [string]$Zip = "$env:USERPROFILE\Downloads\wealth-portal.zip",
    [string]$Target = "D:\Projects",
    [switch]$SkipExtract,
    [switch]$KeepKeycloak,
    [switch]$Stop
)

# "Continue" so tools that write progress to stderr (docker, mvn) are not treated as failures;
# each step checks its own result instead.
$ErrorActionPreference = "Continue"
$Project = Join-Path $Target "wealth-portal"

function Step($text) { Write-Host ""; Write-Host "==> $text" -ForegroundColor Cyan }
function Ok($text)   { Write-Host "    OK  $text" -ForegroundColor Green }
function Warn($text) { Write-Host "    !!  $text" -ForegroundColor Yellow }
function Fail($text) { Write-Host ""; Write-Host "FAILED: $text" -ForegroundColor Red; exit 1 }

function Test-Url($url) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        return ($r.StatusCode -ge 200 -and $r.StatusCode -lt 400)
    } catch { return $false }
}

function Wait-Url($url, $name, $minutes) {
    Write-Host "    waiting for $name " -NoNewline
    $deadline = (Get-Date).AddMinutes($minutes)
    while ((Get-Date) -lt $deadline) {
        if (Test-Url $url) { Write-Host ""; Ok "$name is up ($url)"; return $true }
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 5
    }
    Write-Host ""
    return $false
}

# Stops whatever listens on a port, but never Docker's own processes.
function Stop-Port($port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        $p = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
        if ($p -and $p.ProcessName -notmatch "docker|wslrelay|vpnkit") {
            Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
            Ok "stopped $($p.ProcessName) on port $port"
        }
    }
}

function Docker-Running {
    docker info *> $null
    return ($LASTEXITCODE -eq 0)
}

# ------------------------------------------------------------------ stop mode
if ($Stop) {
    Step "Stopping the wealth portal"
    Stop-Port 8090
    Stop-Port 4200
    if ((Test-Path $Project) -and (Docker-Running)) {
        Push-Location $Project
        docker compose stop
        Pop-Location
    }
    Ok "stopped"
    exit 0
}

# ------------------------------------------------------------------ 1. tools
Step "Checking tools"
foreach ($tool in @("mvn", "node", "npm", "docker")) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { Fail "'$tool' is not installed or not on PATH." }
}
$mvnInfo = (mvn -v 2>&1 | Out-String)
if ($mvnInfo -notmatch "Java version: 2[5-9]") {
    Fail "Maven is not using Java 25. Set JAVA_HOME to your JDK 25 folder, reopen PowerShell, and run again."
}
Ok "Maven uses Java 25"
$nodeVersion = (node -v).TrimStart("v")
$parts = $nodeVersion.Split(".")
if ([int]$parts[0] -lt 22 -or ([int]$parts[0] -eq 22 -and [int]$parts[1] -lt 22)) {
    Fail "Node $nodeVersion is too old. Install Node 24 LTS from nodejs.org."
}
Ok "Node $nodeVersion"

# ------------------------------------------------------------------ 2. stop old
Step "Stopping an old backend / frontend if running"
Stop-Port 8090
Stop-Port 4200

# ------------------------------------------------------------------ 3. extract
if (-not $SkipExtract) {
    Step "Extracting $Zip into $Target"
    if (-not (Test-Path $Zip)) { Fail "Can't find $Zip. Download wealth-portal.zip to your Downloads folder first." }
    New-Item -ItemType Directory -Force -Path $Target -ErrorAction Stop | Out-Null
    try {
        Expand-Archive -Path $Zip -DestinationPath $Target -Force -ErrorAction Stop
    } catch {
        Fail "Couldn't extract the zip: $($_.Exception.Message). Close any program using files in $Project and try again."
    }
    Ok "extracted to $Project"
}
if (-not (Test-Path (Join-Path $Project "docker-compose.yml"))) { Fail "$Project doesn't look like the project folder." }
Set-Location $Project

# ------------------------------------------------------------------ 4. docker
Step "Starting Docker"
if (-not (Docker-Running)) {
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\DockerDesktop\Docker Desktop.exe",
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe"
    )
    $exe = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (-not $exe) { Fail "Docker Desktop isn't running and I can't find it. Start it yourself, then run again." }
    Start-Process $exe
    Write-Host "    waiting for the Docker engine " -NoNewline
    $deadline = (Get-Date).AddMinutes(4)
    while (-not (Docker-Running)) {
        if ((Get-Date) -gt $deadline) { Write-Host ""; Fail "Docker didn't start. Open Docker Desktop, wait for 'Engine running', then run again." }
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 5
    }
    Write-Host ""
}
Ok "Docker engine running"

if (-not $KeepKeycloak) {
    Step "Resetting Keycloak so the latest realm and users load"
    docker compose rm -sf keycloak *> $null
    docker volume rm wealth-portal_keycloakdata *> $null
    Ok "Keycloak data cleared"
}

Step "Starting PostgreSQL and Keycloak"
docker compose up -d
if ($LASTEXITCODE -ne 0) { Fail "docker compose up failed (see the messages above)." }
if (-not (Wait-Url "http://localhost:8180/realms/wealth/.well-known/openid-configuration" "Keycloak" 4)) {
    Warn "Keycloak is slow or failed. Last log lines:"
    docker compose logs keycloak --tail 30
    Fail "Keycloak didn't start."
}

# ------------------------------------------------------------------ 5. backend
Step "Starting the backend (new window: 'Wealth backend')"
$backendCmd = "`$Host.UI.RawUI.WindowTitle = 'Wealth backend'; Set-Location '$Project\backend'; mvn spring-boot:run"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd
if (-not (Wait-Url "http://localhost:8090/actuator/health" "Backend" 8)) {
    Fail "The backend didn't start. Look at the 'Wealth backend' window and send the ERROR lines."
}

# ------------------------------------------------------------------ 6. frontend
Step "Starting the frontend (new window: 'Wealth frontend')"
$needInstall = -not (Test-Path "$Project\frontend\node_modules")
if (-not $needInstall) {
    $lock = (Get-Item "$Project\frontend\package-lock.json").LastWriteTime
    $mods = (Get-Item "$Project\frontend\node_modules").LastWriteTime
    $needInstall = $lock -gt $mods
}
$install = ""
if ($needInstall) { $install = "npm install; " }
$frontendCmd = "`$Host.UI.RawUI.WindowTitle = 'Wealth frontend'; Set-Location '$Project\frontend'; $install npm start"
Start-Process powershell -ArgumentList "-NoExit", "-Command", $frontendCmd
if (-not (Wait-Url "http://localhost:4200" "Frontend" 8)) {
    Fail "The frontend didn't start. Look at the 'Wealth frontend' window and send the error."
}

# ------------------------------------------------------------------ done
Start-Process "http://localhost:4200"
Start-Process "http://localhost:8090/swagger-ui.html"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " Wealth portal is running" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " App          http://localhost:4200"
Write-Host " Swagger UI   http://localhost:8090/swagger-ui.html"
Write-Host " Keycloak     http://localhost:8180   (admin / admin)"
Write-Host ""
Write-Host " Sign in as   rm1 / Manager#2026     (relationship manager)"
Write-Host "              client1 / Client#2026  (client)"
Write-Host "              admin1 / Admin#2026x   (administrator)"
Write-Host ""
Write-Host " Stop everything:"
Write-Host "   powershell -ExecutionPolicy Bypass -File `"$Project\setup-wealth.ps1`" -Stop"
Write-Host " Start again later (no unzip, keep Keycloak):"
Write-Host "   powershell -ExecutionPolicy Bypass -File `"$Project\setup-wealth.ps1`" -SkipExtract -KeepKeycloak"
Write-Host ""
