#!/usr/bin/env bash
# Builds signed, notarized SimpMusic DMGs for Apple Silicon and Intel Macs, into build/mac/.
#
#   scripts/build-mac.sh              # both
#   scripts/build-mac.sh aarch64      # Apple Silicon only (or: amd64)
#
# Conveyor builds and signs the .app. This script notarizes it, wraps it in the DMG CI uses
# (wrap-mac-dmg.sh), then signs, notarizes and staples the DMG.
#
# Notarization is not left to Conveyor (`make notarized-mac-app`): it polls Apple over one
# connection, and a new team's first submission sat in Apple's queue for an hour, long enough for
# that connection to reset and fail the build although Apple accepted the app.
#
# One-time setup on the Mac, all of it outside the repo:
#   - A "Developer ID Application" certificate in the login keychain (for the DMG).
#   - Conveyor on PATH, with its root key (`conveyor keys generate`) and, in
#     ~/Library/Preferences/Hydraulic/Conveyor/defaults.conf, app.mac.signing-key.file
#     (the same certificate as a .p12) and app.mac.certificate.
#   - A notarytool keychain profile: xcrun notarytool store-credentials simpmusic-notary ...
#   - brew install create-dmg imagemagick
set -euo pipefail
cd "$(dirname "$0")/.."

IDENTITY="${MAC_SIGN_IDENTITY:-Developer ID Application}"
NOTARY_PROFILE="${MAC_NOTARY_PROFILE:-simpmusic-notary}"
OUT=build/mac

archs=("$@")
[[ ${#archs[@]} -eq 0 ]] && archs=(aarch64 amd64)
for arch in "${archs[@]}"; do
  [[ "$arch" == aarch64 || "$arch" == amd64 ]] || { echo "Unknown architecture: $arch (use aarch64 or amd64)" >&2; exit 64; }
done

for tool in conveyor create-dmg magick; do
  command -v "$tool" >/dev/null || { echo "$tool is not installed. See the setup notes at the top of this script." >&2; exit 1; }
done
security find-identity -v -p codesigning | grep -q "$IDENTITY" \
  || { echo "No \"$IDENTITY\" certificate in the keychain." >&2; exit 1; }
xcrun notarytool history --keychain-profile "$NOTARY_PROFILE" >/dev/null \
  || { echo "notarytool profile \"$NOTARY_PROFILE\" is missing. See the setup notes at the top of this script." >&2; exit 1; }

json() { python3 -c 'import json,sys; print(json.load(sys.stdin).get(sys.argv[1], ""))' "$1"; }

# Submits a file and asks again every 30 s until Apple answers, so a dropped connection costs
# one poll instead of the build. Fails with Apple's log unless the submission is accepted.
notarize() {
  local id status
  id=$(xcrun notarytool submit "$1" --keychain-profile "$NOTARY_PROFILE" --output-format json | json id)
  echo "    submitted $(basename "$1") as $id"
  while :; do
    status=$(xcrun notarytool info "$id" --keychain-profile "$NOTARY_PROFILE" --output-format json 2>/dev/null | json status || true)
    [[ -z "$status" || "$status" == "In Progress" ]] || break
    sleep 30
  done
  if [[ "$status" != Accepted ]]; then
    echo "Notarization $status. Apple's log:" >&2
    xcrun notarytool log "$id" --keychain-profile "$NOTARY_PROFILE" >&2
    exit 1
  fi
}

version=$(sed -nE 's/^version-name *= *"(.*)"/\1/p' gradle/libs.versions.toml)
slices=$(for arch in "${archs[@]}"; do [[ "$arch" == aarch64 ]] && echo macos-arm64 || echo macos-x64; done | paste -sd, -)

# Conveyor is not run through Gradle, so the natives and its config have to be staged first,
# as CI does. Only the Mac slices: a Linux or Windows one republished upstream must not stop this.
# exportLibraryDefinitions writes into composeResources, so it gets its own run: in the same run
# as the compile, Gradle refuses the undeclared ordering between the two.
./gradlew exportLibraryDefinitions
./gradlew :composeApp:mpvSetupAll "-PmpvSlices=$slices" :desktopApp:writeConveyorConfig --no-configuration-cache

mkdir -p "$OUT"
magick composeApp/icon/dmg-background.png -filter Lanczos -resize 1400x800 "$OUT/dmg-bg.png"

for arch in "${archs[@]}"; do
  echo "==> mac.$arch: building and signing the app"
  conveyor --agree-to-license=1 "-Kapp.machines=mac.$arch" make unnotarized-mac-zip
  zip=$(find output -maxdepth 1 -name '*-mac-*.zip' | head -1)
  [[ -f "$zip" ]] || { echo "Conveyor produced no Mac .zip in output/" >&2; exit 1; }

  # Every Conveyor run empties output/, so keep this arch's app before the next one.
  rm -rf "${OUT:?}/$arch"
  mkdir -p "$OUT/$arch"
  ditto -x -k "$zip" "$OUT/$arch"
  app=$(find "$OUT/$arch" -maxdepth 2 -type d -name '*.app' | head -1)
  [[ -d "$app" ]] || { echo "No .app inside $zip" >&2; exit 1; }

  echo "==> mac.$arch: notarizing the app"
  notarize "$zip"
  xcrun stapler staple "$app"

  dmg="$OUT/SimpMusic-$version-mac-$arch.dmg"
  echo "==> mac.$arch: wrapping $dmg"
  scripts/wrap-mac-dmg.sh "$app" "$OUT/dmg-bg.png" composeApp/icon/rounded_app_icon.icns "$dmg"

  echo "==> mac.$arch: signing and notarizing the DMG"
  codesign --force --sign "$IDENTITY" --timestamp "$dmg"
  notarize "$dmg"
  xcrun stapler staple "$dmg"
  spctl -a -t open --context context:primary-signature -vv "$dmg"
done

ls -lh "$OUT"/*.dmg
