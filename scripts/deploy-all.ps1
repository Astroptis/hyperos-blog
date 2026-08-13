param(
    [string]$ProjectName = "hyperos-blog"
)
$ErrorActionPreference = "Stop"

Write-Host "=== Step 1: Deploy Worker ==="
Push-Location worker
npx wrangler deploy
if ($LASTEXITCODE -ne 0) { Pop-Location; exit 1 }
Pop-Location

Write-Host "=== Step 2: Build Frontend ==="
.\gradlew.bat :composeApp:wasmJsBrowserDistribution
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "=== Step 3: Deploy Frontend to Pages ==="
npx wrangler pages deploy composeApp/build/dist/wasmJs/productionExecutable --project-name=$ProjectName
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host "Done!"