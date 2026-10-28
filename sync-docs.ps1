# Copies the top-level documents into the app's assets so the in-app viewer shows the same text
# as GitHub. Run from the repository root (PowerShell) after editing any of them.
$dest = "app/src/main/assets/docs"
New-Item -ItemType Directory -Force -Path $dest | Out-Null
Copy-Item PRIVACY.md         "$dest/privacy.md"
Copy-Item TERMS.md           "$dest/terms.md"
Copy-Item LICENSE            "$dest/license.md"
Copy-Item CHANGELOG.md       "$dest/changelog.md"
Copy-Item CONTRIBUTING.md    "$dest/contributing.md"
Copy-Item CODE_OF_CONDUCT.md "$dest/code_of_conduct.md"
Copy-Item SECURITY.md        "$dest/security.md"
Write-Host "Docs synced to $dest"
