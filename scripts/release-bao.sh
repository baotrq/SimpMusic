#!/usr/bin/env bash
# Builds the personal release APK and publishes it as bao-YYYY.MM.DD.N: to the download site (R2,
# scripts/download-site), then a copy to GitHub. Phones running a bao- build pick it up on their next
# launch (ForkUpdater in androidApp). Builds from before the site only look at the GitHub copy.
#
#   scripts/release-bao.sh "what changed"
#
# The commit must be pushed first: the release is tagged on it, and the updater compares that
# commit's time with the one each installed build was made from.
set -euo pipefail
cd "$(dirname "$0")/.."

# Releases go to the showcase repo, where people download the app. GPL-3.0 requires the source of
# each release to stay available, so the two source links in its notes must stay public.
REPO=baotrq/simpmusic-bao-showcase
SITE=https://nhacnheo.pages.dev
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
git -C core fetch -q --all
if [[ -z "$(git -C core branch -r --contains HEAD)" ]]; then
  echo "core's commit is not on GitHub yet. Push it first: the release links to it as its source." >&2
  exit 1
fi

if ! grep -qE '^BAO_KEYSTORE=.+' local.properties 2>/dev/null; then
  echo "BAO_KEYSTORE is not set in local.properties. Without the shared key, phones could not install this update." >&2
  exit 1
fi

# The site decides the next tag, so a GitHub outage cannot stop a release.
previous=$(curl -fsS "$SITE/latest.json" | node -pe 'JSON.parse(require("fs").readFileSync(0)).android.tag')
day=$(date +%Y.%m.%d)
n=1
if [[ "$previous" == "bao-$day."* ]]; then n=$((${previous##*.} + 1)); fi
tag="bao-$day.$n"
version=$(sed -n 's/^version-name = "\(.*\)"/\1/p' gradle/libs.versions.toml)
title="SimpMusic (Bao) $day.$n — $summary"

./gradlew :androidApp:assembleRelease

out=androidApp/build/outputs/apk/release
staging=$(mktemp -d)
trap 'rm -rf "$staging"' EXIT
cp "$out/androidApp-arm64-v8a-release.apk" "$staging/SimpMusic-$tag-arm64-v8a.apk"
cp "$out/androidApp-universal-release.apk" "$staging/SimpMusic-$tag-universal.apk"
# The same arm64 APK under a name that never changes, so one link always downloads the newest:
# https://github.com/baotrq/simpmusic-bao-showcase/releases/latest/download/SimpMusic-arm64-v8a.apk
cp "$out/androidApp-arm64-v8a-release.apk" "$staging/SimpMusic-arm64-v8a.apk"

node scripts/download-site/publish.mjs \
  --version "$version" --tag "$tag" --commit-time "$commit_time" --title "$title" \
  "android=$staging/SimpMusic-$tag-arm64-v8a.apk" \
  "android-universal=$staging/SimpMusic-$tag-universal.apk"
echo "Published $tag to the site"

# The copy for builds from before the site. It runs last and may fail: the release is already out.
# Published as the latest release, not a pre-release: /releases/latest only ever points at a release.
gh release create "$tag" "$staging"/*.apk \
  -R "$REPO" \
  --latest \
  --title "$title" \
  --notes "Personal build of the fork (package \`com.maxrave.simpmusic.bao\`)${previous:+. Supersedes \`$previous\`}.

commit-time: $commit_time

Phones already on a bao- build offer this update on their next launch. To install by hand, most phones want \`arm64-v8a\`; \`universal\` works on any device but is larger.

Signed with the shared bao key (\`BAO_KEYSTORE\` in local.properties). A phone holding a build signed with a different key cannot update: back up in Settings, uninstall, install, then restore.

## Source

GPL-3.0. Source: [app](https://github.com/baotrq/project-2b/tree/$(git rev-parse HEAD)) and [core](https://github.com/baotrq/core/tree/$(git -C core rev-parse HEAD)). Based on [SimpMusic](https://github.com/maxrave-dev/SimpMusic) by maxrave-dev and contributors." \
  || echo "WARNING: the GitHub copy failed. $tag is live on the site, but builds from before the site won't see it." >&2

echo "Published $tag"
