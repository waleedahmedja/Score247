#!/bin/sh
# Copies the top-level documents into the app's assets so the in-app viewer shows the same text
# as GitHub. Run from the repository root after editing any of them.
set -e
dest=app/src/main/assets/docs
mkdir -p "$dest"
cp PRIVACY.md          "$dest/privacy.md"
cp TERMS.md            "$dest/terms.md"
cp LICENSE             "$dest/license.md"
cp CHANGELOG.md        "$dest/changelog.md"
cp CONTRIBUTING.md     "$dest/contributing.md"
cp CODE_OF_CONDUCT.md  "$dest/code_of_conduct.md"
cp SECURITY.md         "$dest/security.md"
echo "Docs synced to $dest"
