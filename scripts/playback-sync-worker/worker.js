// Playback sync for SimpMusic (fork): a tiny private store the app reads and writes as
// GET / PUT /<key> with "Authorization: Bearer <SYNC_KEY>". The app keeps one snapshot under
// "playback" (current song, queue, position); other keys are free for later (settings, history).
//
// D1 rather than KV: KV allows 1000 writes a day on the free plan and can show a stale value for
// up to 60 s in another location; D1 is strongly consistent and allows 100k writes a day.

const MAX_BODY_BYTES = 1_500_000;

export default {
  async fetch(request, env) {
    if (!authorized(request.headers.get("Authorization") ?? "", env.SYNC_KEY)) {
      return new Response("unauthorized", { status: 401 });
    }
    const key = new URL(request.url).pathname.slice(1);
    if (!/^[a-z0-9_-]{1,64}$/.test(key)) {
      return new Response("not found", { status: 404 });
    }
    await env.DB.prepare(
      "CREATE TABLE IF NOT EXISTS blobs (key TEXT PRIMARY KEY, value TEXT NOT NULL, updated INTEGER NOT NULL)",
    ).run();

    if (request.method === "GET") {
      const row = await env.DB.prepare("SELECT value FROM blobs WHERE key = ?").bind(key).first();
      return row
        ? new Response(row.value, { headers: { "content-type": "application/json" } })
        : new Response(null, { status: 404 });
    }
    if (request.method === "PUT") {
      const body = await request.text();
      if (new TextEncoder().encode(body).byteLength > MAX_BODY_BYTES) {
        return new Response("too large", { status: 413 });
      }
      await env.DB.prepare(
        "INSERT INTO blobs (key, value, updated) VALUES (?, ?, ?) " +
          "ON CONFLICT(key) DO UPDATE SET value = excluded.value, updated = excluded.updated",
      )
        .bind(key, body, Date.now())
        .run();
      return new Response(null, { status: 204 });
    }
    return new Response("method not allowed", { status: 405 });
  },
};

// Constant-time compare, so the key cannot be guessed byte by byte from response timing.
function authorized(header, secret) {
  if (!secret) return false;
  const encoder = new TextEncoder();
  const given = encoder.encode(header);
  const expected = encoder.encode(`Bearer ${secret}`);
  if (given.byteLength !== expected.byteLength) return false;
  return crypto.subtle.timingSafeEqual(given, expected);
}
