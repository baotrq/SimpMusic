#!/usr/bin/env node
// Checks a running site end to end. Reads only, so it is safe against the live one.
//   node check.mjs [http://127.0.0.1:8799]
import assert from "node:assert/strict";

const site = process.argv[2] ?? "https://nhacnheo.pages.dev";

const page = await fetch(site);
assert.equal(page.status, 200, "page");
assert.match(await page.text(), /Nhạc Nhẽo/);

const manifest = await (await fetch(`${site}/latest.json`)).json();
const platforms = Object.keys(manifest.files);
assert.ok(platforms.length > 0, "manifest lists files");
assert.ok(manifest.android?.commitTime > 0, "updater fields");

for (const platform of platforms) {
  const { key, name, size } = manifest.files[platform];

  const redirect = await fetch(`${site}/download/${platform}`, { redirect: "manual" });
  assert.equal(redirect.status, 302, `${platform} redirects`);
  assert.equal(redirect.headers.get("location"), `/f/${key}`);

  const head = await fetch(`${site}/f/${key}`, { method: "HEAD" });
  assert.equal(head.status, 200, `${platform} HEAD`);
  assert.equal(Number(head.headers.get("content-length")), size, `${platform} size matches the manifest`);
  assert.match(head.headers.get("content-disposition"), new RegExp(name.replace(/\./g, "\\.")));

  // The in-app updater refuses anything but a 200 here.
  const whole = await fetch(`${site}/f/${key}`);
  assert.equal(whole.status, 200, `${platform} plain GET`);
  assert.equal(Number(whole.headers.get("content-length")), size, `${platform} plain GET length`);
  await whole.body.cancel();

  // Resuming a download depends on these.
  const middle = await fetch(`${site}/f/${key}`, { headers: { Range: "bytes=2-5" } });
  assert.equal(middle.status, 206, `${platform} range`);
  assert.equal(middle.headers.get("content-range"), `bytes 2-5/${size}`);
  assert.equal((await middle.arrayBuffer()).byteLength, 4);

  const tail = await fetch(`${site}/f/${key}`, { headers: { Range: "bytes=-3" } });
  assert.equal(tail.status, 206, `${platform} suffix range`);
  assert.equal(tail.headers.get("content-range"), `bytes ${size - 3}-${size - 1}/${size}`);
  await tail.arrayBuffer();

  const etag = head.headers.get("etag");
  const cached = await fetch(`${site}/f/${key}`, { headers: { "If-None-Match": etag } });
  assert.equal(cached.status, 304, `${platform} conditional`);
  console.log(`ok ${platform}: ${name}, ${size} bytes`);
}

assert.equal((await fetch(`${site}/download/nope`)).status, 404);
assert.equal((await fetch(`${site}/f/nope`)).status, 404);
assert.equal((await fetch(`${site}/latest.json`, { method: "POST" })).status, 405);
console.log("All good.");
