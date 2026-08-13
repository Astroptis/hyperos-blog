param(
    [string]$OutputDir = "dist/frontend"
)
$ErrorActionPreference = "Stop"
Write-Host "Building wasmJs frontend..."
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
if ($LASTEXITCODE -ne 0) { exit 1 }
$src = "composeApp/build/dist/wasmJs/productionExecutable"
New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
Copy-Item -Path "$src\*" -Destination $OutputDir -Recurse -Force
Write-Host "Frontend built to $OutputDir"