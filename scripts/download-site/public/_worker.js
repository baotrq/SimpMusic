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
  async fetch(request, env) {
    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response("Method not allowed", { status: 405, headers: { Allow: "GET, HEAD" } });
    }
    const path = new URL(request.url).pathname;

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
