$ErrorActionPreference = "Stop"

# Pinned official ggml-org/llama.cpp Windows CPU x64 release
$ReleaseTag = "b12760"
$AssetName = "llama-b12760-bin-win-cpu-x64.zip"
$ExpectedSha256 = "c82a83190be4f0506778d458db685b6ba5d1b06372b7f7083e6e76605c84cb97"
$DownloadUrl = "https://github.com/ggml-org/llama.cpp/releases/download/$ReleaseTag/$AssetName"

$RuntimeDir = Join-Path $env:LOCALAPPDATA "AI DSA Mentor\runtimes\llama.cpp"
$TempZip = Join-Path $env:TEMP $AssetName

Write-Host "=== AI DSA Mentor: Official llama.cpp Windows CPU Setup ===" -ForegroundColor Cyan
Write-Host "Release:         ggml-org/llama.cpp $ReleaseTag"
Write-Host "Asset:           $AssetName"
Write-Host "Trusted SHA-256: $ExpectedSha256"
Write-Host "Target Folder:   $RuntimeDir"
Write-Host ""

New-Item -ItemType Directory -Force -Path $RuntimeDir | Out-Null

Write-Host "Downloading official release archive..." -ForegroundColor Cyan
Invoke-WebRequest -Uri $DownloadUrl -OutFile $TempZip -UseBasicParsing

$ActualSha = (Get-FileHash -Path $TempZip -Algorithm SHA256).Hash.ToLower()
if ($ActualSha -ne $ExpectedSha256.ToLower()) {
    Remove-Item $TempZip -Force -ErrorAction SilentlyContinue
    throw "Integrity check FAILED! Expected $ExpectedSha256, got $ActualSha"
}
Write-Host "Verified SHA-256: $ActualSha" -ForegroundColor Green

Write-Host "Extracting binaries into AppData runtime directory..." -ForegroundColor Cyan
Expand-Archive -Path $TempZip -DestinationPath $RuntimeDir -Force
Remove-Item $TempZip -Force -ErrorAction SilentlyContinue

$ServerExe = Get-ChildItem -Path $RuntimeDir -Recurse -Filter "llama-server.exe" | Select-Object -First 1
if (-not $ServerExe) {
    throw "llama-server.exe was not found after extraction in $RuntimeDir"
}

Write-Host "llama-server.exe ready at: $($ServerExe.FullName)" -ForegroundColor Green
Write-Host "Note: Keep all accompanying *.dll files in the same directory as llama-server.exe."
exit 0
