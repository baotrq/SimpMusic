# Move downloads off GitHub (2026-11-09)

Nhạc Nhẽo is now downloaded from **https://nhacnheo.pages.dev**, and the Android app checks there for updates. GitHub Releases is a backup copy. First release through it: `bao-2026.11.09.1` (2.5.0.11).

## Why

GitHub's pages kept returning 504 Gateway Time-out for anyone not signed in, including people opening the showcase README in a private window or on a phone. Other repos returned the same 504, so the fault was on GitHub's side, but new users could not reach the download page.

## What changed

| Part | Before | Now |
|---|---|---|
| Download page | Showcase README on GitHub | `scripts/download-site/public/index.html`, in English and Vietnamese |
| Release files | GitHub Releases | R2 bucket `nhac-nheo-files` |
| `scripts/release-bao.sh` | GitHub only, and GitHub chose the next tag | Publishes to the site first and takes the next tag from its manifest. The GitHub copy runs last and may fail |
| `ForkUpdater` | Asks GitHub | Asks the site, and asks GitHub only if the site cannot answer |
| `bao.keystore` | Not ignored | Gitignored |

## How it works

A Cloudflare Pages project (`nhacnheo`) serves the page. `public/_worker.js` handles these paths and passes everything else to the static files:

- `/latest.json`: the manifest, with each platform's current file and what the updater reads.
- `/download/<platform>`: redirects to that platform's current file. The platforms are `android`, `android-universal`, `windows`, `mac-arm` and `mac-intel`.
- `/f/<key>`: streams the file from R2, with resume support.

`publish.mjs` uploads the files and writes the manifest last, so a failed upload leaves the site as it was. Each key contains the upload time and is never rewritten, so files can be cached forever. Each platform keeps its current and previous file, about 1.7 GB in total against R2's free 10 GB.

The site uses Pages rather than a plain Worker because Worker addresses always include the account name (`<worker>.<account>.workers.dev`). Pages gives the short address.

## Publishing

Android, from a clean, pushed commit:

```bash
scripts/release-bao.sh "what changed"
```

Windows and Mac, from the machine that built them:

```bash
node scripts/download-site/publish.mjs --version 2.5.0.11 windows=SimpMusic-Windows-installer.zip
node scripts/download-site/publish.mjs --version 2.5.0.11 mac-arm=arm.dmg mac-intel=intel.dmg
```

To change the page, edit `public/index.html` and run `npx wrangler pages deploy --branch main` in `scripts/download-site`.

To check the live site, run `node scripts/download-site/check.mjs`. It only reads.

## How phones move over

- Builds before 2.5.0.11 only read GitHub, so they find the update there. This is why the GitHub copy stays for now.
- From 2.5.0.11, a phone asks the site first. If the site answers, even with "nothing new", GitHub is not asked.
- If the update check works but the APK download fails, the phone shows an error and does not try GitHub. It tries again on the next check.
- The update prompt depends on the commit time, not the version. Versions are still bumped so that builds can be told apart.

## What was verified

- `check.mjs` passes on the live site for all 5 files: plain downloads, resuming, sizes and redirects.
- The APK downloaded from the site is 2.5.0.11 (code 70), signed with the same certificate as earlier releases.
- The GitHub copy is marked Latest and carries the commit time older builds need.

**Not yet verified:**

- A phone updating to 2.5.0.11.
- A phone downloading an update from the site, which first happens with the release after 2.5.0.11.
- `publish.mjs` on Windows or macOS. So far it has only run on Linux.

## Known limits

- **Windows and Mac** stay on 2.5.0.10 until new builds are published.
- **GitHub is still needed** for source code, `git push` and the `git fetch` in `release-bao.sh`. Only the website was failing; git kept working.
- **Classic Pages:** Wrangler now steers new Pages projects to Workers, so this project was created with `--force`. If Cloudflare retires classic Pages, this address is what is at risk. A custom domain would protect against that.
- **Login token:** `publish.mjs` can fail with a 401 when Wrangler's one-hour token has just expired. Nothing is published in that case, and rerunning works.
- **Unknown paths** such as `/nope` show the homepage instead of a 404.

## Still open

- Point the showcase README's download buttons at the site.
- Make `release-bao.sh` title releases "Nhạc Nhẽo …" instead of "SimpMusic (Bao) …".
- Make `publish.mjs` retry once, to cover the token failure.
