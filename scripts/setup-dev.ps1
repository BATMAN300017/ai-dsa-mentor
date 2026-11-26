$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Write-Host "=== AI DSA Mentor: Windows Dev Setup ===" -ForegroundColor Cyan

# Check Java 21+
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd -and $env:JAVA_HOME) {
    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    if (Test-Path $javaExe) { $javaCmd = $javaExe }
}
if (-not $javaCmd) {
    throw "Java 21+ is required. Set JAVA_HOME or install JDK 21+."
}
$javaVersionOutput = (& java -version 2>&1) -join " "
Write-Host "Detected Java: $javaVersionOutput"

# Check Node.js
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js 20+ is required for apps/frontend and apps/browser-extension."
}
Write-Host "Detected Node: $(node -v)"
Write-Host "Detected npm:  $(npm -v)"

# Install frontend dependencies
Write-Host "`nInstalling frontend dependencies..." -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "apps\frontend")
npm install
Pop-Location

# Install browser-extension dependencies
Write-Host "`nInstalling browser-extension dependencies..." -ForegroundColor Cyan
Push-Location (Join-Path $RepoRoot "apps\browser-extension")
npm install
Pop-Location

# Build Java monorepo using Maven Wrapper or system mvn
Write-Host "`nBuilding Maven modules and running tests..." -ForegroundColor Cyan
Push-Location $RepoRoot
if (Test-Path ".\mvnw.cmd") {
    .\mvnw.cmd test-compile
} elseif (Get-Command mvn -ErrorAction SilentlyContinue) {
    mvn test-compile
} else {
    throw "Neither mvnw.cmd nor system mvn was found."
}
Pop-Location

Write-Host "`nSetup complete! Next steps:" -ForegroundColor Green
Write-Host "  1. Start backend:  .\mvnw.cmd -pl apps/backend -am spring-boot:run"
Write-Host "  2. Start frontend: cd apps/frontend; npm run dev"
Write-Host "  3. Start desktop:  .\mvnw.cmd -pl apps/desktop -am javafx:run"
