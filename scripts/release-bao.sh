#!/usr/bin/env bash
# Builds the personal release APK and publishes it as a bao-YYYY.MM.DD.N pre-release on the fork.
# Phones running a bao- build pick it up on their next launch (ForkUpdater in androidApp).
#
#   scripts/release-bao.sh "what changed"
#
# The commit must be pushed first: the release is tagged on it, and the updater compares that
# commit's time with the one each installed build was made from.
set -euo pipefail
cd "$(dirname "$0")/.."

REPO=baotrq/SimpMusic
summary="${1:?Usage: scripts/release-bao.sh \"what changed\"}"

if [[ -n "$(git status --porcelain --untracked-files=no)" ]]; then
  echo "Uncommitted changes. Commit them first: the APK would not match the tagged commit." >&2
  exit 1
fi
git fetch -q origin
if [[ -z "$(git branch -r --contains HEAD)" ]]; then
  echo "This commit is not on GitHub yet. Push it first (git push)." >&2
  exit 1
fi

day=$(date +%Y.%m.%d)
n=1
while gh release view "bao-$day.$n" -R "$REPO" >/dev/null 2>&1; do n=$((n + 1)); done
tag="bao-$day.$n"
previous=$(gh release list -R "$REPO" -L 20 --json tagName -q '[.[] | select(.tagName | startswith("bao-"))][0].tagName')

./gradlew :androidApp:assembleRelease

out=androidApp/build/outputs/apk/release
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT
cp "$out/androidApp-arm64-v8a-release.apk" "$staging/SimpMusic-$tag-arm64-v8a.apk"
cp "$out/androidApp-universal-release.apk" "$staging/SimpMusic-$tag-universal.apk"

gh release create "$tag" "$staging"/*.apk \
  -R "$REPO" \
  --target "$(git rev-parse HEAD)" \
  --prerelease \
  --title "SimpMusic (Bao) $day.$n — $summary" \
  --notes "Personal build of the fork (package \`com.maxrave.simpmusic.bao\`)${previous:+. Supersedes \`$previous\`}.

Phones already on a bao- build offer this update on their next launch. To install by hand, most phones want \`arm64-v8a\`; \`universal\` works on any device but is larger.

Signed with a debug key: an older \`.bao\` build signed on another machine cannot be updated. Back up in Settings, uninstall, install, then restore."

echo "Published $tag"
