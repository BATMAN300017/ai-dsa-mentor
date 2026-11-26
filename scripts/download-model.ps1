$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ModelInfoPath = Join-Path $RepoRoot "resources\models\qwen2.5-coder-3b\model-info.json"
$TargetDir = Join-Path $env:LOCALAPPDATA "AI DSA Mentor\models\qwen2.5-coder-3b"

if (-not (Test-Path $ModelInfoPath)) {
    throw "Model metadata descriptor not found: $ModelInfoPath"
}

$info = Get-Content $ModelInfoPath -Raw | ConvertFrom-Json
$targetFile = Join-Path $TargetDir $info.expectedFilename
$partFile = "$targetFile.part"
$downloadUrl = if ($info.downloadUrl) {
    $info.downloadUrl
} else {
    "https://huggingface.co/$($info.huggingFaceRepo)/resolve/$($info.revision)/$($info.expectedFilename)"
}

New-Item -ItemType Directory -Force -Path $TargetDir | Out-Null

Write-Host "=== AI DSA Mentor Model Downloader ===" -ForegroundColor Cyan
Write-Host "Model:             $($info.modelName) ($($info.expectedQuantization) $($info.expectedFileFormat))"
Write-Host "Pinned Revision:   $($info.revision)"
Write-Host "Expected Size:     $($info.expectedSizeBytes) bytes"
Write-Host "Upstream SHA-256:  $($info.upstreamChecksum)"
Write-Host "Destination:       $targetFile"
Write-Host ""

if (Test-Path $targetFile) {
    $existingSize = (Get-Item $targetFile).Length
    if ($existingSize -eq [int64]$info.expectedSizeBytes) {
        Write-Host "Existing file found. Verifying SHA-256..." -ForegroundColor Yellow
        $actualHash = (Get-FileHash -Path $targetFile -Algorithm SHA256).Hash.ToLower()
        if ($actualHash -eq $info.upstreamChecksum.ToLower()) {
            Write-Host "Model already installed and verified against upstream SHA-256: $actualHash" -ForegroundColor Green
            exit 0
        }
        Write-Warning "Existing model SHA-256 mismatch ($actualHash). Re-downloading..."
    }
}

$curlExe = Get-Command curl.exe -ErrorAction SilentlyContinue
if ($curlExe) {
    Write-Host "Downloading via curl.exe (resumable .part file)..." -ForegroundColor Cyan
    & curl.exe -fL --retry 3 -C - -o $partFile $downloadUrl
    if ($LASTEXITCODE -ne 0) {
        throw "curl.exe download failed with exit code $LASTEXITCODE"
    }
} else {
    Write-Host "Downloading via Invoke-WebRequest..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $downloadUrl -OutFile $partFile -UseBasicParsing
}

$actualSize = (Get-Item $partFile).Length
if ($info.expectedSizeBytes -gt 0 -and $actualSize -ne [int64]$info.expectedSizeBytes) {
    throw "Downloaded file size mismatch: expected $($info.expectedSizeBytes) bytes, got $actualSize bytes."
}

Write-Host "Computing local SHA-256 checksum..." -ForegroundColor Cyan
$actualSha = (Get-FileHash -Path $partFile -Algorithm SHA256).Hash.ToLower()
if (-not $info.upstreamChecksum) {
    throw "No trusted upstreamChecksum configured in model-info.json. Local SHA-256 is $actualSha."
}
if ($actualSha -ne $info.upstreamChecksum.ToLower()) {
    Remove-Item $partFile -Force
    throw "SHA-256 mismatch! Expected $($info.upstreamChecksum), got $actualSha. Corrupt .part removed."
}

Move-Item -Path $partFile -Destination $targetFile -Force
Write-Host "Success! Model verified against upstream SHA-256 and saved to:" -ForegroundColor Green
Write-Host "  $targetFile"
exit 0
