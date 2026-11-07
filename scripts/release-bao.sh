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

# Releases go to the showcase repo, where people download the app. Source stays public in baotrq/SimpMusic.
REPO=baotrq/simpmusic-bao-showcase
commit_time=$(git log -1 --format=%ct)
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

if ! grep -qE '^BAO_KEYSTORE=.+' local.properties 2>/dev/null; then
  echo "BAO_KEYSTORE is not set in local.properties. Without the shared key, phones could not install this update." >&2
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
# The same arm64 APK under a name that never changes, so one link always downloads the newest:
# https://github.com/baotrq/simpmusic-bao-showcase/releases/latest/download/SimpMusic-Bao.apk
cp "$out/androidApp-arm64-v8a-release.apk" "$staging/SimpMusic-Bao.apk"

# Published as the latest release, not a pre-release: /releases/latest only ever points at a release.
gh release create "$tag" "$staging"/*.apk \
  -R "$REPO" \
  --latest \
  --title "SimpMusic (Bao) $day.$n — $summary" \
  --notes "Personal build of the fork (package \`com.maxrave.simpmusic.bao\`)${previous:+. Supersedes \`$previous\`}.

commit-time: $commit_time

Phones already on a bao- build offer this update on their next launch. To install by hand, most phones want \`arm64-v8a\`; \`universal\` works on any device but is larger.

Signed with the shared bao key (\`BAO_KEYSTORE\` in local.properties). A phone holding a build signed with a different key cannot update: back up in Settings, uninstall, install, then restore."

# ponytail: builds before 2.5.0.5 look for updates in the source repo. Drop this once no phone runs one.
gh release create "$tag" "$staging"/*.apk \
  -R baotrq/SimpMusic \
  --target "$(git rev-parse HEAD)" \
  --title "SimpMusic (Bao) $day.$n — $summary" \
  --notes "Moved: new releases live at https://github.com/$REPO/releases"

echo "Published $tag"
