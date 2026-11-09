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

## Checking

```bash
node scripts/download-site/check.mjs                         # the live site
npx wrangler pages dev --ip 127.0.0.1 --port 8799             # or a local copy, on an empty local bucket
node scripts/download-site/check.mjs http://127.0.0.1:8799
```
