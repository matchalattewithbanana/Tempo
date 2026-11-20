# Changelog

Fork-specific changes. Published tags and artifacts are listed in the [release catalog](docs/releases/README.md). Inherited upstream history is preserved separately in the [archive](docs/archive/UPSTREAM_CHANGELOG.md).

## Unreleased

### Material UI refinement

- Made Tempo's corner preference control standard Material 3 shapes while preserving custom album-art silhouettes.
- Redesigned Appearance Studio's expandable sections as tonal cards with readable headings and selection chips.
- Improved Settings section headings, row spacing and touch targets; simplified the listening-period rail to avoid shadow layers.
- Refined the Home/Stats/History bottom navigation into a compact tonal bar with a clear selected-pill indicator and fewer decorative drawing layers.
- Added shape-scale regression coverage. Android build and on-device visual validation remain pending.


### Appearance Studio — curated preset collection

- Added **ten complete presets** (Midnight Bloom, Sakura, Matcha, Cobalt, Porcelain, Terracotta, Citrine, Mocha, Lagoon and Lilac), bringing the library to 18.
- Designed separate Light and Dark palettes for each edition, including canvas, raised and inset cards, border, readable primary/secondary text, and accent/on-accent combinations.
- Turned the preset collection into lazily rendered, grouped rows with realistic miniatures for the selected tone. This avoids composing all cards at once.
- Added an opt-out setting to apply coordinated heading typography, shape, finish, and backdrop when choosing an edition. Original presets preserve their existing layout behavior.
- Fixed default frosted icon-button contrast in Light mode and restored preset accent behavior while keeping the saved custom accent value available.
- Added palette contrast and style-selection regression tests; Android build and on-device visual review are still pending.


### Added

- Choose clock-style listening durations (hours/minutes, with optional seconds), or decimal minutes/hours/days with 0–3 decimal places. Appearance Studio shows a live example and explains fractional minutes.
- Apply the saved listening-time preference to Home, Stats, song/artist/album details, Spotlight, shares, and Android widget totals.

### Fixed

- Home daily-average and peak-day durations now retain full milliseconds until display, avoiding whole-minute truncation.
- Decimal duration values truncate at the selected precision instead of rounding up across hour/minute boundaries. Millisecond listening records are unchanged.

### Performance

- Compose Appearance Studio sections lazily; stop Supported Apps state collection while off-screen.
- Defer extended Today rankings until the overview is opened, cancelling obsolete requests; cache localized duration number formatters and avoid scheduling widget refreshes with no widgets placed.

Tests were added for conversion boundaries, Home averages, and appearance settings backup compatibility; Android build/device verification is still pending.

## [4.9.9]

Nine commits after 4.9.0, counted before release preparation. Android version
code 4820. Chromium and Firefox companion packages use version 4.9.9.

### Added

- Import Apple Music Play Activity CSVs and Apple privacy-export ZIPs.
- Create and share a customizable 3×3 album poster from your library, catalog search, or Stats.
- Save share-card PNGs or share text where available, with new cards for albums, profiles, completed challenges, and recent history.
- Customize Stats ranking, duration units, and summary counts, plus Profile defaults and the share-card theme.

### Changed

- Improved Light and System appearance, Stats refreshes, and shared artwork loading.
- Updated Android home-screen widgets with clearer labels, listening totals, and shortcuts to the matching Stats period.
- Improved import matching by timing and artist credit. YouTube entries without recorded watch time contribute a play with unknown duration.

[Release notes](docs/releases/v4.9.9.md).

## [4.9.0] — 2026-11-19

First stable GitHub release. Android version code 4819; source commit
`e0887afed227bee245e468325b7f61c6264b0c08`.

### Fixed

- Improved Daily Listening contrast for Light, Dark and System appearance settings.
- Added Appearance Studio settings and saved looks to library backups.
- Checkpointed offline listening events incrementally and surfaced quarantined records in diagnostics.
- Routed GitHub build feedback to the maintained fork, reduced redundant statistics and gamification work, and improved screen lifecycle handling.
- Preserved local data during restore, reset, account disconnect and backup retention operations.
- Clear all data now requests Android’s full app-data reset, replacing partial database/preference deletion while producers remain alive. Confirmation explains closure and scope in all six locales; rejected requests show an error without navigating to onboarding.
- Backup export stages database JSON in one Room transaction, validates references and copies images/compresses the ZIP after releasing the database transaction.
- Spotify disconnect, new login and client-ID changes invalidate pending token operations; cancelled or stale responses cannot restore credentials or overwrite newer auth state.
- Automatic local backup retention now records per-document creation order and always protects the verified destination. Undated legacy files are retained conservatively instead of sorted by random UUID.
- Backup restore stages artwork privately, validates the archive before promotion and uses a unique destination namespace. Rejected archives and transaction-body failures cannot overwrite existing artwork.
- Improved recoverable Home, History and Profile errors, guarded restore and editor exits, and preserved Share Canvas drafts.
- Improved Profile contrast and modal behavior, chart accessibility, Spotlight lifecycle handling and responsive dialogs.

### Maintenance

- Reorganized project, developer, support and release documentation around the maintained fork.
- Corrected privacy/storage/reporting declarations and in-app privacy descriptions.
- Added architecture guidance, contribution/report templates and a release catalog.
- Restored manual-only beta workflow triggers and repaired the companion build-all command.

### Validation

- [Release workflow 44372248786](https://github.com/matchalattewithbanana/Tempo/actions/runs/44372248786) passed Android unit tests, lint, APK assembly, emulator tests and browser companion checks.
- The published APK checksum and signing certificate were verified. The signer matches 4.9.0 Beta 1.
- Physical-device visual, accessibility, tracking and battery checks remain to be completed.

## [4.9.0-beta.1] — 2026-11-17

**Reliability and import beta.** Starts the SemVer release series; Android version code 4818.

### Fixed
- Preserved unsynced browser plays during pruning and durable queued plays during offline recovery; replay now retries bounded due batches without per-event sleeps.
- Removed blocking Room reads from notification-listener startup and prevented routine maintenance from pruning listening history.
- Made Spotify credentials fail closed on secure-store errors; made migration snapshots verify copies and analytics queue writes atomic.
- Rejected unsigned, malformed and replayed WebSocket control frames, withheld pairing tokens from the WebSocket handshake, and removed the extension's global host grant.
- Bounded backup/Spotify/Musicolet import memory and disk usage; Spotify history is staged to disk and replayed in small batches.
- Corrected Home unique-track semantics, calendar invalidation and explicit session-start day attribution; gated decorative animation for reduced motion.

### Validation
- Browser extension typecheck and smoke checks passed locally.
- Published by successful [beta workflow run 44112413379](https://github.com/matchalattewithbanana/Tempo/actions/runs/44112413379) at tagged commit `289fcd0`; its gates included unit tests, lint, APK assembly, emulator tests and extension checks. Earlier local Android validation was unavailable.
- Physical-device visual, accessibility, playback and battery checks remain beta feedback items.

## [4.8.16-fork.2-beta.1] — 2026-11-17

**Appearance Studio beta.** Six curated looks, experimental Light/System, OLED,
typography and accessibility preferences, layout controls and saved looks.

### Added
- Abyss, Obsidian Gold, Aurora, Glacier, Ember and Solstice themes, with OLED
  and opt-in Light/Follow System beta tones.
- Typography, bold text, density, corner and finish options; optional wallpaper
  and cache-only current-artwork accents; adaptive effects and reduce motion.
- Home layout preferences, opening Stats filters, and a compact playback strip
  with thumbnail artwork and stale-status handling.
- Named appearance looks and bounded appearance-only JSON import/export.

### Improved
- Semantic text, surface and accent colors across navigation, history, stats,
  settings, backup/import screens, dialogs and shared components.
- Prevented duplicate Now Playing UI and reserved safe scroll space.

### Known limitations
- Debug signing may differ from prior APKs; export data before reinstalling.
- Light/System still need physical-device visual, large-font and accessibility feedback.
- Google OAuth/Drive backup requires correct per-signer configuration.
- GPU-heavy progressive blur/refraction and rebranding are not included.

## [4.8.16-fork.1] — 2026-11-16

**First fork preview** of Tempo with more responsive local music tracking and
safer reconciliation between Android, Spotify and the browser companion.

### Added
- Provisional live-session listening timer and privacy-conscious playback timing diagnostics.
- Opt-in, backup-confirmed history-quality scan and correction for unverifiable
  legacy Spotify API estimates.
- Spotify recently-played page traversal with safeguards against missing pages
  and incorrect cursor advancement.
- Regression coverage for replay protection, Spotify metadata validation, fast
  repeat listening, concurrent devices and historical session aggregation.

### Changed
- Improved elapsed-time accounting across pause/resume, seeking and screen-off playback.
- Reduced completed-play queueing latency and refresh delays in Home, History and Stats.
- Changed Spotify API history reconciliation to approximately 20-minute
  WorkManager scheduling; removed the 45-second foreground playback-state poll.
- Corrected average/longest session calculations and unknown-completion/skip statistics.
- Reorganized the README and removed checked-in generated browser dependencies.

### Fixed and hardened
- Centralized listening-event deduplication across local and imported sources.
- Hardened Spotify broadcast validation and browser companion HTTP request replay protection.
- Corrected stalled/retryable HTTP response handling, importer concurrency and track identity conflicts.

### Known limitations
- Debug-signed preview APK; in-place upgrades require the same signing certificate.
- Google Drive requires per-fork OAuth configuration and actual APK signing SHA-1.
- Spotify history availability is constrained by Spotify's own server-side window.
- Real-device verification of the earlier one-minute-per-song symptom is still pending.

Read the [complete release notes](docs/releases/v4.8.16-fork.1.md).

---
