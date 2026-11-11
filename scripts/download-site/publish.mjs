#!/usr/bin/env node
// Uploads release files to R2 and points the site's manifest at them. Runs on Linux, macOS and Windows.
//
//   node publish.mjs --version 2.5.0.10 windows=path/to/SimpMusic-Windows-installer.zip
//   node publish.mjs --version 2.5.0.10 mac-arm=a.dmg mac-intel=b.dmg
//   node publish.mjs --version 2.5.0.10 --tag bao-2026.11.09.1 --commit-time 1794136603 --title "..." \
//     android=arm64.apk android-universal=universal.apk android-vivo=vivo.apk
//
// The manifest is written only after every upload worked, so a failure leaves the site as it was.
// The previous file of each platform is kept, so a download already running is not cut off; the one
// before that is deleted, which keeps the bucket well inside R2's free 10 GB.
import { execFileSync } from "node:child_process";
import { mkdtempSync, rmSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { basename, dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const SITE = process.env.SITE_URL ?? "https://nhacnheo.pages.dev";
const BUCKET = "nhac-nheo-files";
const PLATFORMS = ["android", "android-universal", "android-vivo", "windows", "mac-arm", "mac-intel"];
const here = dirname(fileURLToPath(import.meta.url));

const { values: flags, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    version: { type: "string" },
    tag: { type: "string" },
    "commit-time": { type: "string" },
    title: { type: "string" },
  },
});

function fail(message) {
  console.error(message);
  process.exit(1);
}

if (!flags.version || positionals.length === 0) {
  fail('Usage: node publish.mjs --version <x.y.z.n> [--tag T --commit-time N --title "..."] <platform>=<file> ...');
}
const uploads = positionals.map((arg) => {
  const at = arg.indexOf("=");
  const platform = arg.slice(0, at);
  // Absolute, because wrangler runs from this script's folder: a path relative to where you are would miss.
  const path = resolve(arg.slice(at + 1));
  if (!PLATFORMS.includes(platform)) fail(`Unknown platform "${platform}". Use one of: ${PLATFORMS.join(", ")}`);
  if (!statSync(path, { throwIfNoEntry: false })?.isFile()) fail(`Not a file: ${path}`);
  if (!/^[\w.-]+$/.test(basename(path))) fail(`File name must be plain letters, digits, dots and dashes: ${path}`);
  return { platform, path };
});
const hasAndroid = uploads.some((u) => u.platform === "android");
if (hasAndroid && !(flags.tag && flags["commit-time"] && flags.title)) {
  fail("An android upload needs --tag, --commit-time and --title: the in-app updater reads them.");
}

function wrangler(...args) {
  // Windows can only start npx through a shell, and a shell splits unquoted paths at spaces.
  const windows = process.platform === "win32";
  execFileSync("npx", ["-y", "wrangler@4", ...(windows ? args.map((a) => `"${a}"`) : args)], {
    cwd: here,
    stdio: "inherit",
    shell: windows,
  });
}

// Anything but "no release yet" must stop here: carrying on would replace the manifest with an empty one.
const response = await fetch(`${SITE}/latest.json`);
if (!response.ok && response.status !== 404) fail(`Could not read the manifest: HTTP ${response.status}`);
const manifest = response.ok ? await response.json() : { files: {}, retired: {} };

// The time in the key means a key is never written twice, which is what lets the Worker cache it forever.
const id = `${flags.version}-${Date.now().toString(36)}`;
const toDelete = [];
for (const { platform, path } of uploads) {
  const name = basename(path);
  const key = `${platform}/${id}/${name}`;
  wrangler("r2", "object", "put", `${BUCKET}/${key}`, "--file", path, "--remote");
  if (manifest.retired[platform]) toDelete.push(manifest.retired[platform]);
  if (manifest.files[platform]) manifest.retired[platform] = manifest.files[platform].key;
  manifest.files[platform] = { key, name, size: statSync(path).size, version: flags.version };
}
if (hasAndroid) {
  manifest.android = {
    tag: flags.tag,
    title: flags.title,
    commitTime: Number(flags["commit-time"]),
    version: flags.version,
  };
}

const temp = mkdtempSync(join(tmpdir(), "nhac-nheo-"));
try {
  const file = join(temp, "latest.json");
  writeFileSync(file, JSON.stringify(manifest, null, 2));
  wrangler("r2", "object", "put", `${BUCKET}/latest.json`, "--file", file, "--remote");
} finally {
  rmSync(temp, { recursive: true, force: true });
}

// The release is live by now, so a leftover file is not worth failing over.
for (const key of toDelete) {
  try {
    wrangler("r2", "object", "delete", `${BUCKET}/${key}`, "--remote");
  } catch {
    console.warn(`Could not delete old file ${key}. It is harmless, but it still takes up space.`);
  }
}
console.log(`Published ${uploads.map((u) => u.platform).join(", ")} (${flags.version}) to ${SITE}`);
