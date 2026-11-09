#!/usr/bin/env bash
# Builds SimpMusic's Windows installer into build/windows/SimpMusic-Windows-installer.zip:
# an .msix plus the certificate it is signed with and an install.bat that trusts it, as upstream ships.
#
#   scripts/build-windows.sh            # build only
#   scripts/build-windows.sh --upload   # build, then attach the zip to the newest bao- release
#
# Why not the .msi (packageWindowsMsi): it is unsigned, and Smart App Control blocks unsigned apps
# with no way to run them anyway. An .msix whose certificate the PC trusts gets past it, and the
# certificate costs nothing: Conveyor derives it from its root key.
#
# Runs on the Mac: Conveyor cross-builds Windows. One-time setup, outside the repo:
#   - Conveyor on PATH, with its root key (`conveyor keys generate`, stored in
#     ~/Library/Preferences/Hydraulic/Conveyor/defaults.conf). Back that key up and never replace
#     it: the certificate comes from it, and a package signed with another one cannot update the
#     installed app.
set -euo pipefail
cd "$(dirname "$0")/.."

REPO=baotrq/simpmusic-bao-showcase
OUT=build/windows
ZIP_NAME=SimpMusic-Windows-installer.zip

upload=false
for arg in "$@"; do
  case "$arg" in
    --upload) upload=true ;;
    *) echo "Unknown option: $arg (use --upload)" >&2; exit 64 ;;
  esac
done

command -v conveyor >/dev/null || { echo "conveyor is not installed. See the setup notes at the top of this script." >&2; exit 1; }
$upload && { command -v gh >/dev/null || { echo "gh is not installed." >&2; exit 1; }; }

# Conveyor is not run through Gradle, so the natives and its config have to be staged first.
# exportLibraryDefinitions writes into composeResources, so it gets its own run (see build-mac.sh).
./gradlew exportLibraryDefinitions
./gradlew :composeApp:mpvSetupAll -PmpvSlices=windows-x64 :desktopApp:writeConveyorConfig --no-configuration-cache

# `site` is what writes simpmusic.crt next to the .msix; `windows-msix` alone does not.
# Its own output folder: every Conveyor run empties the one it writes to, and build-mac.sh uses output/.
conveyor_out="$OUT/conveyor"
rm -rf "$OUT"
mkdir -p "$OUT"
conveyor --agree-to-license=1 "-Kapp.machines=windows.amd64" make site -o "$conveyor_out"

msix=$(ls "$conveyor_out"/simpmusic-*.x64.msix 2>/dev/null | head -1)
[[ -f "$msix" ]] || { echo "Conveyor produced no simpmusic-*.x64.msix in $conveyor_out/" >&2; exit 1; }
[[ -f "$conveyor_out/simpmusic.crt" ]] || { echo "Conveyor produced no $conveyor_out/simpmusic.crt" >&2; exit 1; }

mkdir -p "$OUT/stage"
cp scripts/windows/fork/install.bat scripts/windows/fork/install.ps1 "$conveyor_out/simpmusic.crt" "$msix" "$OUT/stage/"
(cd "$OUT/stage" && zip -q -j "../$ZIP_NAME" ./*)
rm -rf "$OUT/stage"
ls -lh "$OUT/$ZIP_NAME"

if $upload; then
  tag=$(gh release list -R "$REPO" -L 20 --json tagName -q '[.[] | select(.tagName | startswith("bao-"))][0].tagName')
  [[ -n "$tag" ]] || { echo "No bao- release on $REPO to attach to." >&2; exit 1; }
  gh release upload "$tag" "$OUT/$ZIP_NAME" -R "$REPO" --clobber
  echo "Attached $ZIP_NAME to $tag"
fi
