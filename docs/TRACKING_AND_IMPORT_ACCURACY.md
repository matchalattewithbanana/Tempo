# Tracking and import accuracy — Tempo fork

This document describes **observed plays**, **estimated metadata**, and how Spotify
sources are reconciled. The application is not an official Spotify client.

## Cooperative sources — no exclusivity

- **Android MediaSession / notification tracking** (default): near-real-time
  local playback, subject to Notification Access and Android battery restrictions.
- **Spotify Device Broadcast Status** (Tempo Settings and Spotify Settings →
  Playback): opt-in local broadcasts for metadata, play/pause and position.
  Tempo displays a local Now Playing card; this is not Discord Rich Presence.
  MediaSession remains available as a fallback. Different event types may arrive
  out of order, so they are evaluated separately and fresh broadcasts temporarily
  take priority over MediaSession snapshots.
- **Spotify API history sync**: uses Spotify OAuth + PKCE and automatically
  imports the recently-played feed roughly every 20 minutes via WorkManager,
  beginning immediately after a successful sign-in. It runs **alongside** local
  broadcasts and MediaSession, not instead of them. A manual **Sync Spotify
  history now** action is also available; WorkManager may be delayed by Android.
- **Spotify Connect API snapshot**: an on-demand endpoint remains available for
  optional diagnostics, but Tempo no longer polls it every 45 seconds while the
  app is open. Continuous local playback tracking never depends on Spotify's
  playback-state API. Already-granted OAuth scopes are not silently changed.

Spotify may require your account to be approved for your Spotify Developer app.
The OAuth redirect URI used by this fork is `tempo://spotify-callback`.

## Accuracy policy

1. The **Spotify extended JSON export** includes the exact number of
   milliseconds played and an end timestamp. Import these as reported listening
   time. It **does not** necessarily provide the catalog track duration, so import completion
   is *unknown*; the legacy non-null completion column stores 0 to avoid
   inaccurately reporting every imported play as 80% complete.
2. The **Spotify Web API recently-played feed** provides a played timestamp
   and the catalog duration, but **not observed listening duration or completion**.
   Tempo records an observed play with 0 unknown listening milliseconds, not a
   guessed 80%-length listen. Local tracking remains preferable for minutes and
   exact playback details.
3. **Liked tracks, top-track rankings, and yearly playlists** do not establish
   when or how many times a track was played. They can enrich the library,
   but generated listening events from those hints are now off by default.
   Use a timestamped export or scrobbles for verifiable history.
4. **Track identity**: authoritative Spotify, MusicBrainz and YouTube IDs
   that disagree must never be silently merged just because titles match.
   The shared track resolver applies this safeguard to automatic imports.
5. **Cross-source duplicates**: the Spotify API importer uses the shared
   authority-ranked ListeningEventDao. Reconciliation compares Spotify
   `played_at` (the end of a play) against the observed local session end,
   rather than comparing the API end to a local session start. A higher-fidelity
   locally observed play wins over an API history import, irrespective of insert order.

6. **YouTube Music Takeout** contributes listening time only when the export
   includes a recorded watch time. Timestamp-only entries count as plays with
   unknown duration and completion. Reimporting a timestamp-only entry can
   recognize its earlier copy by the original Takeout timestamp.
7. **Smart reconciliation** selects the closest compatible event for the same
   track and applies source authority to that match. Measured plays with
   disjoint playback intervals remain separate. Imported durations are bounded
   by their explicit start and end times when those bounds are available.

**Unknown completion must not count as a skip.** New statistics queries exclude
unknown, non-skipped import rows from completion averages and skip counts. This
corrects how existing rows are *interpreted* without rewriting them.

**Recently played limit:** Spotify limits each API response to 50 tracks, and
very long offline gaps can exceed the accessible history window. An automatic
sync cannot reconstruct every historical play; for comprehensive older data,
prefer the extended streaming-history export.

## Apple Music history

Request **Apple Media Services information** at [Apple Data and Privacy](https://privacy.apple.com/).
In Tempo, open **Settings → Import Apple Music** and select the downloaded ZIP files or the extracted
**Apple Music Play Activity** CSV. Select the companion history CSVs alongside an extracted activity
file to help recover missing artist credits.

Tempo imports timestamped playback rows using Apple's reported listening duration and UTC offset,
when available. The daily and recent history files provide track metadata only. Plays below your
minimum duration, duplicate events and rows with unresolved artists appear in the import summary.
A track split across multiple playback rows can count as multiple plays.

## Import deduplication, Drive conflict modes and existing history

Spotify's *ReadMeFirst_ExtendedStreamingHistory* describes the `ts` field as
the **UTC end time of the stream** and `ms_played` as the measured milliseconds
played. These are not equivalent to the song's catalog length. The importer
derives a nominal start time from end minus listened milliseconds; it never
invents completion or uses offline upload time as a replacement timestamp.
Older `StreamingHistory` exports have minute-level end times, so merging their
data with second-precision `endsong` requires corroborating duration and
playback-interval overlap, not end timestamps alone.

**Every active structured import** (Spotify, Apple, Deezer, Last.fm, YouTube,
browser extension and local/Drive ZIP restore) passes playback events through
the database's transactional reconciliation layer. YouTube's per-event failure
fallback now uses the same layer instead of raw inserts.

Each batch uses three protections:

1. Recompute a deterministic source/track/play fingerprint **after** mapping
   backup IDs to local tracks and normalizing inconsistent timing; check saved
   fingerprints in the live database, as well as repeats within that batch.
2. Resolve conservative, metadata-compatible track aliases in indexed batches.
   Do not merge differing known Spotify, YouTube or MusicBrainz identities,
   or explicitly incompatible albums. Compare equivalent-track events *together*
   inside the current batch and against existing database rows.
3. For measured streams, reconcile even if recorded starts are more than a
   minute apart when at least half the shorter playback interval genuinely
   overlaps. Disjoint / barely overlapping intervals, distinct live sessions,
   and independent phone/desktop playback remain separate. Unknown-duration
   scrobbles are not treated as sufficient evidence for broad temporal matches.

**Backup restore behavior:**

- **Skip Duplicates:** insert missing plays and entities; keep existing
  equivalent listening records and local profile/settings/gamification entries.
- **Replace Existing:** insert missing plays, refresh exact matched events,
  replace same-source temporal duplicates and apply backup preferences,
  profile and gamification values. Higher-authority measured live events are
  still protected from lower-authority imports; unrelated listening history
  is not erased.
- The result distinguishes genuinely **new plays**, **existing plays
  replaced**, and **incoming duplicates skipped**. The event reconciliation
  always queries the active database; it is not just a file-level check.

**Existing databases:** Re-importing a file prevents new duplicate accumulation,
but cannot prove that every older, differently sourced event was redundant.
Tempo intentionally avoids a destructive automatic global cleanup. In Settings
→ Data → **Check listening-history accuracy**, a read-only scan now counts
**exact repeated non-null content fingerprints**. After exporting a backup,
the user can explicitly delete those proven duplicates while retaining the
oldest event ID. Legacy NULL fingerprints, ambiguous temporal overlaps and
separate source-representations are **not** removed by that action. The existing
Spotify API-estimate repair remains a separate, explicit operation.

**Verification:** Unit policy tests cover measured overlaps, disjoint replays,
Spotify UTC end matching and independently observed sessions. Instrumented Room
tests cover alias IDs within and across batches, literal backup fingerprints,
conflict strategies, precise minute/second formats and the exact-duplicate
repair. These tests must pass on Android before claiming shipping readiness.

## Troubleshooting

- In Tempo, enable the **Music Tracking** service and give notification access.
- Disable restrictive background/battery suspension for Tempo when tracking
  continuously; Android OEM restrictions can still interrupt delivery.
- To use Spotify broadcasts, enable Device Broadcast Status **in both apps**.
- Spotify API requires an authorized account and active network; if your
  developer application is restricted, a 403 error will prevent import.
- For exact history, prefer extended Spotify JSON or timestamped Last.fm
  exports, not playlists or popularity rankings.
- A currently playing track can appear before its completed listening event
  is saved; current presence and completed history are different concepts.

## Google Drive backups

The original authentication and Drive backup code is unchanged. To avoid
“Google Sign-In not configured” in fork-built APKs, follow
[Google Drive setup](GOOGLE_DRIVE_SETUP.md), including your Web OAuth Client ID
and Android signing SHA-1. We cannot embed upstream signing keys.

## Remaining limitations

On-device playback accuracy still requires testing with actual Spotify releases,
Android versions, and OEM battery behavior. There is no Discord Rich Presence
publisher in this fork. Not all import sources contain enough fields to calculate
completion, listening minutes or true replay counts without estimates.
