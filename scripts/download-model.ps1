$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$ModelInfoPath = Join-Path $RepoRoot "resources\models\qwen2.5-coder-3b\model-info.json"
$TargetDir = Join-Path $env:LOCALAPPDATA "AI DSA Mentor\models\qwen2.5-coder-3b"

Write-Host "=== AI DSA Mentor Model Downloader (Task 1 Placeholder) ===" -ForegroundColor Yellow
Write-Host "Automated model downloading will be implemented by the Model Manager in a later task."
Write-Host ""
Write-Host "Metadata file: $ModelInfoPath"
Write-Host "Planned OS AppData destination: $TargetDir"

if (Test-Path $ModelInfoPath) {
    $info = Get-Content $ModelInfoPath -Raw | ConvertFrom-Json
    Write-Host "Model:        $($info.modelName)"
    Write-Host "Repository:   $($info.huggingFaceUrl)"
    Write-Host "Quantization: $($info.expectedQuantization) ($($info.expectedFileFormat))"
    Write-Host "Filename:     $($info.expectedFilename)"
    Write-Host "Status:       $($info.downloadStatus)"
}
exit 0
