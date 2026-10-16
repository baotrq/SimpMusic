# AutoMix Queue Order: where we left off

_Last worked on: 2026-10-16_

## What it is
When AutoMix is on, a radio queue now picks the next song by how well its tempo and key mix with the song playing. Before this, AutoMix only smoothed the jump between whatever two songs the radio gave it.

**AutoMix is on when** Settings → Playback has **Crossfade** on and **Crossfade Duration** set to **Auto**.

## How it works
1. A song starts in a radio queue: a tapped song, Start radio, or a mix.
2. The app looks at the playing song and the next 5 songs.
3. It gets each song's BPM and key, from the saved stream data if the song has played before, otherwise from a Tidal search. Each song is looked up once per app run.
4. Each candidate gets a score: tempo gap in % (double/half time counts as a match), plus 3 for each step apart on the Camelot key wheel. Lower is better.
5. If the song already next scores 9 or less, nothing changes.
6. Otherwise, the best of the other 4 moves to play next, but only if it scores 9 or less.
7. A next song with **no BPM data is never pushed back**. Otherwise videos and songs Tidal doesn't know would never play.

The window slides: when the next song starts, it weighs the following 5 against that song.

**It never touches** playlists, albums, shuffle, or Listen Together rooms.

## Code
- `core/domain/.../mediaservice/handler/AutoMixPick.kt`: the scoring and the pick rule. Pure code, tested in `AutoMixPickTest` (6 tests, all pass).
- `core/data/.../mediaservice/AutoMixQueueOrder.kt`: the lookups, the safety checks and the move (through "Move to play next").
- Both player handlers call it when a song changes (`onMediaItemTransition`).
- `tidalSearchQuery()` is now shared with stream resolution in `StreamRepositoryImpl`.

## Testing status
- Installed on the Oppo on 2026-10-16. Settings were confirmed right: Crossfade on, Auto, DJ mode on.
- Tidal lookups work, for example Uptown Funk = 115 BPM C major and At My Worst = 92 BPM C major.
- After one song change, there was **no log line yet**. The first version logged only when it moved a song, so a decision to keep the order was silent.
- **Since that test**, I added debug lines, which are committed but **not yet installed on the phone**:
  - `Weighing after <song> (<bpm> <key>): A=<score>, B=?, …`, where `?` means no data
  - `Not a radio (<type> <id>), order kept`

## Next time
1. Build and install the debug-log build: `./gradlew :androidApp:assembleRelease`, then `adb -s 3B15AA00BF100000 install -r androidApp/build/outputs/apk/release/androidApp-arm64-v8a-release.apk`.
2. Play a radio for 3–4 songs, then check the log for `AutoMixOrder`. See whether moves happen, and whether the scores look sensible.
3. Tune if needed:
   - `GOOD_COST = 9` may be too strict or too loose.
   - `WINDOW = 5` sets how many songs it looks ahead.
4. Once it works, ship it with `scripts/release-bao.sh` so both phones update.

## Known limits
- The first song of a new radio is never reordered, because its radio hasn't loaded yet when it starts.
- A failed Tidal lookup marks that song "no data" until the app restarts.
- Tempo is matched, but beats aren't lined up (Tidal gives no beat positions).

## Ideas raised, not done
- Long Auto fades sweep the DJ filter in steps you can hear: the fade always uses 50 steps, so a 30s fade changes every 600ms. A fix: base the step count on time, every 20–30ms.
- Auto can end the old song early. If the next song's BPM isn't loaded when the fade is timed, it falls back to 30s and starts about 33s before the end; the real fade can then be shorter, about 20s, which cuts off the old song's last ~13s.
