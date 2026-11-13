# Download site

The page people download Nhạc Nhẽo from, and where the Android app looks for updates. A Cloudflare
Pages project serves the page from `public/`, and `public/_worker.js` streams the files from an R2 bucket.

URL: https://nhacnheo.pages.dev

## Setup (once)

R2 has to be enabled once in the Cloudflare dashboard (R2 Object Storage). It asks for a payment
method even on the free plan: 10 GB of storage, and downloads are free.

```bash
cd scripts/download-site
npx wrangler login
npx wrangler r2 bucket create nhac-nheo-files
npx wrangler pages project create nhacnheo --production-branch main --force  # --force, or Wrangler makes a Worker with a long address
npx wrangler pages deploy
```

## Publishing

Android: `scripts/release-bao.sh "what changed"` does everything.

Windows and Mac, from the machine that built them:

```bash
node scripts/download-site/publish.mjs --version 2.5.0.11 windows=SimpMusic-Windows-installer.zip
node scripts/download-site/publish.mjs --version 2.5.0.11 mac-arm=arm.dmg mac-intel=intel.dmg
```

File names may hold only letters, digits, dots and dashes. Each file must be under 315 MB, wrangler's
upload limit. The site's buttons and versions follow by themselves.

## Changing the page

Edit `public/index.html`, then `npx wrangler pages deploy` in this folder. Publishing files needs no deploy.

## Lyrics fixes

`/lyrics/…` in the same Worker holds lyrics fixes for every install: which source has the right
lyrics for a song, "show no lyrics", or lyrics a user made. Users send them from the app (⋯ → Lyrics →
Share this fix with everyone); nothing reaches other users until you approve it. Until the two steps
below are done, `/lyrics/…` answers 503 and the app simply behaves as before.

Once:

```bash
cd scripts/download-site
npx wrangler d1 create nhacnheo-lyrics          # prints a database_id
```

Add this to `wrangler.toml`, with that id:

```toml
[[d1_databases]]
binding = "LYRICS"
database_name = "nhacnheo-lyrics"
database_id = "<the id>"
```

```bash
openssl rand -hex 24                            # your admin key; keep it, never put it in the app
npx wrangler pages secret put LYRICS_ADMIN_KEY  # paste it
npx wrangler pages deploy
```

Tables are created on the first request.

- **Review:** open https://nhacnheo.pages.dev/lyrics/admin, enter the key, and approve or reject.
  A source pick shows what the sender's device displayed, but only the pick is published; each device
  then fetches the words from that source itself, so a provider's lyrics are never republished here.
- **Your own devices:** enter the key under Settings → Playback sync → Lyrics fixes admin key (hidden
  tools). "Share this fix" then publishes at once.
- **Limits:** 30 suggestions per install and 60 per network address a day. A new suggestion from the same
  install for the same song replaces its last one. Addresses are stored only as a hash salted with the key.
- **Taking a fix down:** `curl -X DELETE -H "Authorization: Bearer <key>" https://nhacnheo.pages.dev/lyrics/admin/fix/<videoId>`.
- An approval can take up to 10 minutes to reach every device: answers, misses included, are cached
  at the edge for that long.

## Checking

```bash
node scripts/download-site/check.mjs                         # the live site
npx wrangler pages dev --ip 127.0.0.1 --port 8799             # or a local copy, on an empty local bucket
node scripts/download-site/check.mjs http://127.0.0.1:8799
```
