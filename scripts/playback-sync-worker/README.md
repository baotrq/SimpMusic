# Playback sync server

A Cloudflare Worker that stores what each phone is playing, so another phone can continue from
there. It runs on the free plan and needs no credit card.

## Setup (once)

```bash
cd scripts/playback-sync-worker
npx wrangler login
npx wrangler d1 create simpmusic-sync      # copy the database_id it prints into wrangler.toml
openssl rand -hex 24                        # this is your sync key; keep it
npx wrangler secret put SYNC_KEY            # paste the key when asked
npx wrangler deploy                         # prints https://simpmusic-sync.<you>.workers.dev
```

Then on every phone, open Settings → Playback sync and enter:

- **Sync server**: the `https://….workers.dev` URL from `deploy`
- **Sync key**: the key from `openssl`

Use **Test connection** to check both.

## How it behaves

- A phone uploads its current song, queue and position after it has played something in this
  session: on every song change, on pause, and every 3 minutes while playing.
- When the app starts fresh (swiped away from Recents, killed, or after a reboot), it opens on the
  other phone's song, paused at the saved position, if that phone played more recently. Returning to
  an app that was still running keeps its own song, and Home shows "Continue from <phone>" on top
  instead: tap it to play from there, or ✕ to hide it.
- Only the last upload is kept. Whichever phone played last wins.
- Sign-ins (YouTube, Spotify, Discord, Last.fm) and the AI API key with its provider, model, base URL
  and headers are kept the same on every phone. Preferences stay per phone. They are encrypted on the
  phone with a key derived from the sync key, so the server only stores ciphertext. Each app open
  picks up the newest change. Sync only adds: signing out on one phone does not sign the other out.
- Lyrics translated by the AI are shared too, so a song is only paid for once across phones.

## Security

Anyone with the URL **and** the key can read and replace the snapshot, so treat the key like a
password. The snapshot holds song ids and titles, never sign-in cookies or tokens.
