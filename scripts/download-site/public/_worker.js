// Every request comes through here (Cloudflare Pages' _worker.js). Release files are served from R2,
// and everything else, the page included, falls through to the static files beside this one.
//   /latest.json          the manifest publish.mjs writes
//   /download/<platform>  a stable link: redirects to that platform's current file
//   /f/<key>              the file. Keys never change content, so they cache forever and resume safely.

const CONTENT_TYPES = {
  apk: "application/vnd.android.package-archive",
  zip: "application/zip",
  dmg: "application/x-apple-diskimage",
};

export default {
  async fetch(request, env, ctx) {
    const path = new URL(request.url).pathname;
    // Lyrics fixes take POST/PUT/DELETE too, so they are routed before the GET-only rule below.
    if (path.startsWith("/lyrics/")) return lyrics(request, env, ctx, path.slice("/lyrics".length));
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405, headers: { Allow: "GET, HEAD" } });
    }

    if (path === "/latest.json") {
      const manifest = await env.FILES.get("latest.json");
      if (!manifest) return new Response("No release yet", { status: 404 });
      return new Response(manifest.body, {
        headers: { "Content-Type": "application/json", "Cache-Control": "no-cache" },
      });
    }

    if (path.startsWith("/download/")) {
      const manifest = await env.FILES.get("latest.json");
      const file = manifest && (await manifest.json()).files?.[path.slice("/download/".length)];
      if (!file) return new Response("Not found", { status: 404 });
      return new Response(null, {
        status: 302,
        headers: { Location: `/f/${file.key}`, "Cache-Control": "no-store" },
      });
    }

    if (path.startsWith("/f/")) return serveFile(request, env, path.slice("/f/".length));

    return env.ASSETS.fetch(request);
  },
};

async function serveFile(request, env, key) {
  let object;
  try {
    object =
      request.method === "HEAD"
        ? await env.FILES.head(key)
        : await env.FILES.get(key, { onlyIf: request.headers, range: request.headers });
  } catch {
    // R2 rejects a range it cannot satisfy by throwing.
    return new Response("Range not satisfiable", { status: 416 });
  }
  if (!object) return new Response("Not found", { status: 404 });

  const name = key.slice(key.lastIndexOf("/") + 1);
  const headers = new Headers({
    "Accept-Ranges": "bytes",
    "Cache-Control": "public, max-age=31536000, immutable",
    "Content-Disposition": `attachment; filename="${name}"`,
    "Content-Type": CONTENT_TYPES[name.slice(name.lastIndexOf(".") + 1)] ?? "application/octet-stream",
    ETag: object.httpEtag,
  });

  if (request.method === "HEAD") {
    headers.set("Content-Length", object.size);
    return new Response(null, { headers });
  }
  // A failed precondition comes back as an object with no body.
  if (!object.body) {
    const notModified = request.headers.has("If-None-Match") || request.headers.has("If-Modified-Since");
    return new Response(null, { status: notModified ? 304 : 412, headers });
  }
  // R2 reports a range even when none was asked for, and a client that asked for the whole file wants a 200.
  if (!object.range || !request.headers.has("Range")) return new Response(object.body, { headers });

  const { offset, length, suffix } = object.range;
  const start = suffix !== undefined ? object.size - suffix : offset;
  const end = length !== undefined ? start + length - 1 : object.size - 1;
  headers.set("Content-Range", `bytes ${start}-${end}/${object.size}`);
  return new Response(object.body, { status: 206, headers });
}

// ---------------------------------------------------------------------------------------------
// Lyrics fixes: which source has the right lyrics for a song, or lyrics a user made, shared with
// every Nhạc Nhẽo install. Anyone can suggest a fix; nothing reaches other users until the owner
// approves it on /lyrics/admin. D1 (binding LYRICS) holds both tables; LYRICS_ADMIN_KEY is a
// Pages secret and never ships in the app.
//   GET    /lyrics/fix/<videoId>          the approved fix, or 404. Public, cached briefly.
//   POST   /lyrics/suggest                a suggestion from the app. Public, rate limited.
//   GET    /lyrics/admin                  the review page (asks for the key itself).
//   GET    /lyrics/admin/pending          pending suggestions          } Bearer LYRICS_ADMIN_KEY
//   POST   /lyrics/admin/approve/<id>     publish one, as it was sent  }
//   POST   /lyrics/admin/reject/<id>                                   }
//   PUT    /lyrics/admin/fix/<videoId>    publish directly (the owner's own devices)
//   DELETE /lyrics/admin/fix/<videoId>    take a fix down

const LYRICS_SOURCES = ["simpmusic", "lrclib", "youtube", "better_lyrics", "lrc_red", "kugou", "megalobiz", "youtube_music"];
const LYRICS_KINDS = ["source", "lyrics", "none"];
const VIDEO_ID = /^[A-Za-z0-9_-]{11}$/;
const INSTALL_ID = /^[0-9a-f]{16,64}$/;
const MAX_SUGGESTION_BYTES = 300_000;
const MAX_LINES = 3000;
const PER_INSTALL_PER_DAY = 30;
const PER_IP_PER_DAY = 60;
// Long enough to spare D1 most reads, short enough that an approval reaches phones within minutes.
const FIX_CACHE_SECONDS = 600;

let lyricsSchemaReady = false;

async function lyricsSchema(db) {
  if (lyricsSchemaReady) return;
  await db.batch([
    db.prepare(
      "CREATE TABLE IF NOT EXISTS fixes (video_id TEXT PRIMARY KEY, kind TEXT NOT NULL, source TEXT, lyrics TEXT, " +
        "title TEXT, artist TEXT, updated INTEGER NOT NULL)",
    ),
    db.prepare(
      "CREATE TABLE IF NOT EXISTS suggestions (id INTEGER PRIMARY KEY AUTOINCREMENT, video_id TEXT NOT NULL, " +
        "kind TEXT NOT NULL, source TEXT, lyrics TEXT, title TEXT, artist TEXT, duration INTEGER, " +
        "install_id TEXT NOT NULL, ip_hash TEXT, created INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'pending')",
    ),
    db.prepare("CREATE INDEX IF NOT EXISTS suggestions_status ON suggestions (status, created)"),
    db.prepare("CREATE INDEX IF NOT EXISTS suggestions_install ON suggestions (install_id, created)"),
    db.prepare("CREATE INDEX IF NOT EXISTS suggestions_ip ON suggestions (ip_hash, created)"),
  ]);
  lyricsSchemaReady = true;
}

async function lyrics(request, env, ctx, path) {
  if (!env.LYRICS) return new Response("Lyrics fixes are not set up", { status: 503 });
  if (path === "/admin" && request.method === "GET") {
    return new Response(ADMIN_PAGE, { headers: { "Content-Type": "text/html; charset=utf-8", "Cache-Control": "no-store" } });
  }
  await lyricsSchema(env.LYRICS);

  if (path.startsWith("/fix/") && request.method === "GET") return getFix(request, env, ctx, path.slice("/fix/".length));
  if (path === "/suggest" && request.method === "POST") return suggest(request, env);

  if (path.startsWith("/admin/")) {
    if (!adminAuthorized(request.headers.get("Authorization") ?? "", env.LYRICS_ADMIN_KEY)) {
      return new Response("unauthorized", { status: 401 });
    }
    const rest = path.slice("/admin/".length);
    if (rest === "pending" && request.method === "GET") return pending(env);
    if (rest.startsWith("approve/") && request.method === "POST") return decide(request, env, rest.slice("approve/".length), true);
    if (rest.startsWith("reject/") && request.method === "POST") return decide(request, env, rest.slice("reject/".length), false);
    if (rest.startsWith("fix/")) {
      const videoId = rest.slice("fix/".length);
      if (!VIDEO_ID.test(videoId)) return new Response("bad video id", { status: 400 });
      if (request.method === "PUT") {
        const body = await readFixBody(request);
        if (body.error) return new Response(body.error, { status: 400 });
        const text = (value) => (typeof value === "string" ? value.slice(0, 300) : null);
        await publish(env, request, videoId, body.fix, text(body.input.title), text(body.input.artist));
        return new Response(null, { status: 204 });
      }
      if (request.method === "DELETE") {
        await env.LYRICS.prepare("DELETE FROM fixes WHERE video_id = ?").bind(videoId).run();
        await purgeFix(request, videoId);
        return new Response(null, { status: 204 });
      }
    }
  }
  return new Response("not found", { status: 404 });
}

function fixUrl(request, videoId) {
  return new URL(`/lyrics/fix/${videoId}`, request.url).toString();
}

async function purgeFix(request, videoId) {
  // The edge cache is per data centre, so a phone served elsewhere may see the old answer for up
  // to FIX_CACHE_SECONDS.
  await caches.default.delete(fixUrl(request, videoId));
}

async function getFix(request, env, ctx, videoId) {
  if (!VIDEO_ID.test(videoId)) return new Response("not found", { status: 404 });
  const cacheKey = fixUrl(request, videoId);
  const cached = await caches.default.match(cacheKey);
  if (cached) return cached;
  const row = await env.LYRICS.prepare("SELECT kind, source, lyrics, updated FROM fixes WHERE video_id = ?").bind(videoId).first();
  const headers = { "Cache-Control": `public, max-age=${FIX_CACHE_SECONDS}` };
  // Misses are cached too: most songs have no fix, and every play asks.
  const response = row
    ? new Response(
        JSON.stringify({
          kind: row.kind,
          source: row.source ?? null,
          lyrics: row.lyrics ? JSON.parse(row.lyrics) : null,
          updatedAt: row.updated,
        }),
        { headers: { ...headers, "Content-Type": "application/json" } },
      )
    : new Response(null, { status: 404, headers });
  ctx.waitUntil(caches.default.put(cacheKey, response.clone()));
  return response;
}

/** Checks a fix the way it will be served: kind, a known source, and lyrics that look like lyrics. */
function checkFix(input) {
  const kind = input?.kind;
  if (!LYRICS_KINDS.includes(kind)) return { error: "bad kind" };
  const fix = { kind, source: null, lyrics: null };
  if (kind === "source") {
    if (!LYRICS_SOURCES.includes(input.source)) return { error: "bad source" };
    fix.source = input.source;
  }
  if (input.lyrics != null) {
    const lines = input.lyrics.lines;
    if (!Array.isArray(lines) || lines.length === 0 || lines.length > MAX_LINES) return { error: "bad lyrics" };
    if (!lines.every((l) => l && typeof l.words === "string" && typeof l.startTimeMs === "string")) {
      return { error: "bad lyrics" };
    }
    fix.lyrics = {
      lines: lines.map((l) => ({
        startTimeMs: l.startTimeMs,
        endTimeMs: typeof l.endTimeMs === "string" ? l.endTimeMs : "0",
        syllables: [],
        words: l.words,
      })),
      syncType: ["LINE_SYNCED", "RICH_SYNCED", "UNSYNCED"].includes(input.lyrics.syncType) ? input.lyrics.syncType : "UNSYNCED",
    };
  }
  if (kind === "lyrics" && !fix.lyrics) return { error: "lyrics missing" };
  return { fix };
}

async function readFixBody(request) {
  const text = await request.text();
  if (new TextEncoder().encode(text).byteLength > MAX_SUGGESTION_BYTES) return { error: "too large" };
  let input;
  try {
    input = JSON.parse(text);
  } catch {
    return { error: "bad json" };
  }
  const checked = checkFix(input);
  if (checked.error) return checked;
  return { fix: checked.fix, input };
}

async function suggest(request, env) {
  const body = await readFixBody(request);
  if (body.error) return new Response(body.error, { status: 400 });
  const { input, fix } = body;
  if (!VIDEO_ID.test(input.videoId ?? "")) return new Response("bad video id", { status: 400 });
  if (!INSTALL_ID.test(input.installId ?? "")) return new Response("bad install id", { status: 400 });

  // Salted with the admin key so the stored hash cannot be reversed by trying every address.
  const ipHash = await sha256(`${request.headers.get("CF-Connecting-IP") ?? ""}|${env.LYRICS_ADMIN_KEY ?? ""}`);
  const dayAgo = Date.now() - 86_400_000;
  const [byInstall, byIp] = await env.LYRICS.batch([
    env.LYRICS.prepare("SELECT COUNT(*) AS n FROM suggestions WHERE install_id = ? AND created > ?").bind(input.installId, dayAgo),
    env.LYRICS.prepare("SELECT COUNT(*) AS n FROM suggestions WHERE ip_hash = ? AND created > ?").bind(ipHash, dayAgo),
  ]);
  if (byInstall.results[0].n >= PER_INSTALL_PER_DAY || byIp.results[0].n >= PER_IP_PER_DAY) {
    return new Response("too many suggestions today", { status: 429 });
  }
  const text = (value, max) => (typeof value === "string" ? value.slice(0, max) : null);
  // One pending suggestion per install and song: a new one replaces the last.
  await env.LYRICS.batch([
    env.LYRICS.prepare("DELETE FROM suggestions WHERE install_id = ? AND video_id = ? AND status = 'pending'").bind(
      input.installId,
      input.videoId,
    ),
    env.LYRICS.prepare(
      "INSERT INTO suggestions (video_id, kind, source, lyrics, title, artist, duration, install_id, ip_hash, created) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
    ).bind(
      input.videoId,
      fix.kind,
      fix.source,
      fix.lyrics ? JSON.stringify(fix.lyrics) : null,
      text(input.title, 300),
      text(input.artist, 300),
      Number.isInteger(input.duration) ? input.duration : null,
      input.installId,
      ipHash,
      Date.now(),
    ),
  ]);
  return new Response(null, { status: 202 });
}

async function pending(env) {
  const { results } = await env.LYRICS.prepare(
    "SELECT s.id, s.video_id, s.kind, s.source, s.lyrics, s.title, s.artist, s.duration, s.created, " +
      // How many installs sent the same fix: a hint, not a rule.
      "(SELECT COUNT(DISTINCT o.install_id) FROM suggestions o WHERE o.video_id = s.video_id AND o.kind = s.kind " +
      "AND IFNULL(o.source, '') = IFNULL(s.source, '') AND o.status = 'pending') AS agreeing, " +
      "(SELECT f.kind || ':' || IFNULL(f.source, '') FROM fixes f WHERE f.video_id = s.video_id) AS current " +
      "FROM suggestions s WHERE s.status = 'pending' ORDER BY s.created LIMIT 200",
  ).all();
  return Response.json(
    results.map((r) => ({ ...r, lyrics: r.lyrics ? JSON.parse(r.lyrics) : null })),
    { headers: { "Cache-Control": "no-store" } },
  );
}

async function decide(request, env, id, approve) {
  const row = await env.LYRICS.prepare("SELECT * FROM suggestions WHERE id = ? AND status = 'pending'").bind(Number(id)).first();
  if (!row) return new Response("not found", { status: 404 });
  if (!approve) {
    await env.LYRICS.prepare("UPDATE suggestions SET status = 'rejected' WHERE id = ?").bind(row.id).run();
    return new Response(null, { status: 204 });
  }
  // A source pick is published as the pick alone: each phone fetches the words itself, so the
  // lyrics a provider served (lrc.red's are Apple's) are never republished from here.
  const fix = {
    kind: row.kind,
    source: row.source,
    lyrics: row.kind === "lyrics" && row.lyrics ? JSON.parse(row.lyrics) : null,
  };
  await publish(env, request, row.video_id, fix, row.title, row.artist);
  await env.LYRICS.batch([
    env.LYRICS.prepare("UPDATE suggestions SET status = 'approved' WHERE id = ?").bind(row.id),
    env.LYRICS.prepare("UPDATE suggestions SET status = 'superseded' WHERE video_id = ? AND status = 'pending'").bind(row.video_id),
  ]);
  return new Response(null, { status: 204 });
}

async function publish(env, request, videoId, fix, title = null, artist = null) {
  const lyricsJson = fix.kind === "lyrics" && fix.lyrics ? JSON.stringify(fix.lyrics) : null;
  await env.LYRICS.prepare(
    "INSERT INTO fixes (video_id, kind, source, lyrics, title, artist, updated) VALUES (?, ?, ?, ?, ?, ?, ?) " +
      "ON CONFLICT(video_id) DO UPDATE SET kind = excluded.kind, source = excluded.source, lyrics = excluded.lyrics, " +
      "title = COALESCE(excluded.title, fixes.title), artist = COALESCE(excluded.artist, fixes.artist), updated = excluded.updated",
  )
    .bind(videoId, fix.kind, fix.kind === "source" ? fix.source : null, lyricsJson, title, artist, Date.now())
    .run();
  await purgeFix(request, videoId);
}

async function sha256(text) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

// Constant-time compare, so the key cannot be guessed byte by byte from response timing.
function adminAuthorized(header, secret) {
  if (!secret) return false;
  const given = new TextEncoder().encode(header);
  const expected = new TextEncoder().encode(`Bearer ${secret}`);
  if (given.byteLength !== expected.byteLength) return false;
  return crypto.subtle.timingSafeEqual(given, expected);
}

// The review page. It holds no secret: it asks for the admin key and sends it with every call.
const ADMIN_PAGE = String.raw`<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Lyrics fixes</title>
<style>
:root{--bg:#fff;--fg:#111;--muted:#666;--card:#f3f3f3;--accent:#1a7f37;--danger:#b42318}
@media (prefers-color-scheme:dark){:root{--bg:#121212;--fg:#eee;--muted:#999;--card:#1f1f1f;--accent:#3fb950;--danger:#f97066}}
body{margin:0 auto;padding:16px;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,sans-serif;max-width:860px}
.card{background:var(--card);border-radius:12px;padding:12px 14px;margin:12px 0}
.meta{color:var(--muted);font-size:13px}
pre{white-space:pre-wrap;max-height:240px;overflow:auto;font-size:13px;margin:8px 0}
button{font:inherit;padding:6px 14px;border-radius:8px;border:0;cursor:pointer;margin-right:8px;color:#fff}
.ok{background:var(--accent)}.no{background:var(--danger)}
input{font:inherit;padding:6px;width:min(100%,360px)}
a{color:inherit}
</style></head><body>
<h1>Lyrics fixes</h1>
<p id="login"><input id="key" type="password" placeholder="Admin key"> <button class="ok" onclick="save()">Open</button></p>
<p id="status" class="meta"></p>
<div id="list"></div>
<script>
let key = "";
try { key = localStorage.getItem("lyricsAdminKey") || ""; } catch (e) {}
function save() { key = document.getElementById("key").value.trim(); try { localStorage.setItem("lyricsAdminKey", key); } catch (e) {} load(); }
function esc(s) { return String(s ?? "").replace(/[&<>"']/g, c => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c])); }
function time(ms) { ms = Number(ms) || 0; const s = Math.floor(ms / 1000); return String(Math.floor(s / 60)).padStart(2, "0") + ":" + String(s % 60).padStart(2, "0"); }
function describe(s) {
  if (s.kind === "none") return "Show no lyrics";
  if (s.kind === "lyrics") return "Their own lyrics";
  return "Use source: " + s.source;
}
async function call(path, method) {
  return fetch("/lyrics/admin/" + path, { method: method || "GET", headers: { Authorization: "Bearer " + key } });
}
async function load() {
  const status = document.getElementById("status");
  if (!key) { status.textContent = "Enter the admin key."; return; }
  const res = await call("pending");
  if (res.status === 401) { status.textContent = "Wrong key."; document.getElementById("login").style.display = ""; return; }
  const items = await res.json();
  document.getElementById("login").style.display = "none";
  status.textContent = items.length ? items.length + " waiting" : "Nothing waiting.";
  document.getElementById("list").innerHTML = items.map(s => {
    const all = (s.lyrics && s.lyrics.lines) || [];
    const lines = all.slice(0, 40).map(l => (s.lyrics.syncType === "UNSYNCED" ? "" : "[" + time(l.startTimeMs) + "] ") + l.words.replace(/<\d+:\d+\.\d+>/g, "")).join("\n");
    const preview = s.lyrics ? "<pre>" + esc(lines) + (all.length > 40 ? "\n…" : "") + "</pre>" +
      (s.kind === "source" ? "<div class='meta'>What their device showed. Only the source pick is published.</div>" : "") : "";
    return "<div class='card'><b>" + esc(s.title || s.video_id) + "</b> · " + esc(s.artist) +
      "<div class='meta'><a href='https://music.youtube.com/watch?v=" + esc(s.video_id) + "' target='_blank' rel='noopener'>" + esc(s.video_id) + "</a>" +
      (s.duration ? " · " + time(s.duration * 1000) : "") + " · " + new Date(s.created).toLocaleString() +
      " · sent by " + s.agreeing + " install(s)" + (s.current ? " · live now: " + esc(s.current) : "") + "</div>" +
      "<p>" + esc(describe(s)) + "</p>" + preview +
      "<button class='ok' onclick='act(" + s.id + ", true)'>Approve</button><button class='no' onclick='act(" + s.id + ", false)'>Reject</button></div>";
  }).join("");
}
async function act(id, approve) {
  const res = await call((approve ? "approve/" : "reject/") + id, "POST");
  if (!res.ok) alert("Failed: " + res.status);
  load();
}
load();
</script></body></html>`;
