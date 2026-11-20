# Changelog

Fork-specific changes. Published tags and artifacts are listed in the [release catalog](docs/releases/README.md). Inherited upstream history is preserved separately in the [archive](docs/archive/UPSTREAM_CHANGELOG.md).

## Unreleased

### Beta release candidate — 4.10.25-beta.1 (2026-11-21)

- Prepared a coordinated **Android + Chromium + Firefox** prerelease candidate on `main` for the source changes since stable v4.9.9; this is *not* the completion of the future Material 3 Expressive roadmap.
- Version derived from the bounded rollover rule: **4.9.(9 + 116) = 4.9.125 → 4.10.25-beta.1** (commit count taken before release-preparation commits). Android `versionCode` advanced **4820 → 4821**; browser packages use **4.10.25.1**.
- Added [the staged Expressive implementation roadmap](docs/MATERIAL3_EXPRESSIVE_ROADMAP.md), acceptance criteria, performance/accessibility baselines and beta exit gates.
- Release assets, signing continuity and publication must pass the manual beta workflow; code preparation alone is **not** a published or tested APK.
- [Beta notes](docs/releases/v4.10.25-beta.1.md).


### Optional Expressive Effects Studio, floating dock and custom palettes

- Added three **independent opt-in** effects, all off by default: continuous low-alpha ambient tonal animation, foreground-only sensor-driven featured-card tilt, and bounded entrance motion for lazy music items.
- Added Gentle / Expressive intensity choices and a native floating Material navigation toolbar as an alternative to the short bar. Expanded screens continue using their navigation rail.
- Hard-gated all expensive effects against Android animation settings, reduced motion, battery saver, low RAM, thermal limits and app backgrounding. A single lifecycle-managed, throttled gravity sensor listener is shared; no permissions, workers or background polling were added.
- Applied optional effects to Home, Stats, History, Profile and Song/Artist/Album detail screens, with no effects for existing users unless explicitly enabled.
- Built a three-seed custom palette maker (primary, secondary, tertiary), automatic contrast adjustments, and live previews across Light, Dark and OLED tones. Existing saved looks and imports retain backward-compatible defaults; appearance schema version increased to 5.
- Improved backup/restore headers and loading states, and added tests covering default-off behavior, hard safety gates, color contrast, and serialization.
- Source committed to `main` using `[skip ci]`. Compilation, accessibility/device visuals, battery impact, and frame-time benchmarking still pending.


### Material 3 Expressive — deeper Orion design review

- Audited Orion's native Settings hub, palette editor, navigation docks, motion code, content skeletons, segmented sheets, details, and onboarding in addition to screenshots.
- Improved shared clickable response with ripple, focused-state feedback and native fast spatial animation; replaced standard reduced-motion fallback with six snap-based Material motion specs.
- Filled the remaining expressive typography roles and propagated custom fonts/bold-text choices; registered experimental Material3 API opt-ins centrally in the app Gradle module.
- Built a music-first tonal Settings bento with a direct Appearance Studio hero, responsive secondary tiles, and original static Material polygon accents. Added matching hero headers to the five Settings subpages.
- Replaced blank initial-load areas with accessible static music-card skeletons on Home, Stats and History.
- Added fast selected-corner feedback to appearance presets, themed mini album tiles and restrained expand/collapse transitions.
- Added Settings bento breakpoint regression tests and expanded visual audit documentation.
- All source-only commits carry `[skip ci]`: no Android build, release, device screenshots or performance claims.


### Orion-inspired Material 3 Expressive visual refinement

- Audited Orion Store's native Compose components and screenshot gallery as design references, creating original Tempo-owned primitives rather than copying GPL source.
- Added reusable static Material-shape identity emblems and editorial insight headings; refreshed setup, errors, empty Stats, quests and badges with coherent shapes and typography.
- Rebuilt Home Spotlight's artwork frame with correctly keyed, theme-derived tonal overlay and expressive icon indicator instead of fixed pink/purple gradients that failed to update when switching palettes.
- Re-themed Home's identity pill and level ring in Light/Dark/OLED, removing a costly 10dp shadow and fixed pale/pink colors.
- Reorganized Settings overview into five tactile category tiles; polished top-ranked Stats cards, time badges, summary corners and native centered header.
- Limited the Home, Stats and History content width on larger screens without changing navigation, data loading or listening tracking. Added source review and device validation notes in `docs/EXPRESSIVE_DESIGN_REFERENCE.md`.
- Committed with `[skip ci]`. Android compilation, device visuals and performance benchmarking remain pending; no build or release was initiated.


### Material 3 Expressive — adaptive navigation and shared components

- Added an M3 Expressive collapsed **WideNavigationRail** for primary destinations on windows at least 840dp wide. The phone short navigation bar remains for smaller windows; both retain Home/Stats/History back-stack state, and Compact Now Playing appears in either layout.
- Converted legacy dialogs from multi-layer gradient surfaces and 28dp shadows to native tonal Material `Surface` and expressive primary/tonal buttons.
- Rebuilt shared snackbars with Material-provided action and dismiss buttons, keyboard/TalkBack semantics and tonal surfaces while retaining severity icons.
- Improved **Manage Supported Apps** search: native expressive search field and a narrow loading indicator replace the full-screen loading flash while filtering. Initial device scanning still indicates loading explicitly.
- Replaced the remaining hand-built **Stats** and **History** search fields with native Material text fields, standard IME/focus behavior, 48dp clear/close actions, and the selected Material tonal appearance.
- Implemented native connected button-group shapes for time periods, History date filters, Update Center channels and Appearance Studio choices; rows reflow on narrow phones and larger font scales without squeezing five options into tiny targets.
- Improved Home/Stats/History load errors with a readable Material error container and native Retry button. Fixed the year displayed in empty-state guidance and replaced a hardcoded success green with theme roles.
- Added pure layout policies and unit tests for responsive connected-button columns and 840dp navigation-rail cutoff.
- Source changes only: no Android build, instrumented tests or release was triggered.


### Material 3 Expressive foundation and whole-app controls

- Upgraded to stable **Compose BOM 2026.11.01 / Material3 1.5.0** with compile SDK 37.1, AGP 9.5.1, Gradle 9.8.2 and Lifecycle 2.11.0. Kept target SDK 36.
- Switched the root app theme to `MaterialExpressiveTheme`; opt into the native expressive motion scheme only when Android animation, power, thermal and Tempo motion preferences allow it.
- Implemented increased Material shapes and emphasized typography styles consistent with all saved appearance presets and Tempo's existing fonts.
- Replaced custom bottom navigation with official adaptive `ShortNavigationBar`; moved Stats and Profile to Material `PrimaryTabRow` and History/Home time filters to expressive buttons.
- Standardized Settings on Material `ListItem`, added expressive app-appearance choices and Update Center channels, improved Settings search field affordances and flexible Update Center heading.
- Unified loading indicators across Home, Stats, History, Appearance and Update Center with wavy variants when allowed, and standard fallback when reduced-motion or constrained. Stat-card entry animations use Material's fast spatial/effects specs instead of slow bouncy springs.
- Preserved music-specific visualizations, playback and data flows. Added tests and migration documentation. No builds, releases or device-performance claims; verification is pending.


### Launcher artwork: faithful concept assets

- Replaced all four provisional vector launcher illustrations with the **actual approved generated badge artwork**: Pulse Mint, Tempo Clock, Violet Trail, and Vinyl Night.
- Bundled optimized 432 px WebP adaptive-icon foregrounds with transparent spacing inside Android's safe area, rather than approximate hand-traced icons.
- Added matching monochrome silhouettes for Android themed icons. Classic remains the original selectable fallback, and the existing Settings icon picker and About page continue working without migration.
- Icons are entirely bundled and work offline. No listening-data or update-center changes.


### Material launcher icon picker

- Added four new native adaptive launcher designs—Pulse Mint, Tempo Clock, Violet Trail, and Vinyl Night—and kept the original Tempo icon as Tempo Classic.
- Added a dedicated picker with real launcher-resource previews, accessibility labels, selected-state indicators and Settings search integration. It is reachable from General Settings and Appearance Studio.
- Android PackageManager controls enabled launcher aliases without resetting listening history or restarting the tracking service. Android 13+ changes aliases atomically; older supported versions use a safe sequential switch.
- Includes monochrome themed icon variants and a synchronized About-page icon. No network calls or extra permissions.
- This code requires an Android build and launcher/device testing before release.


### About screen
- Added a Material 3 About header with Tempo identity, installed version and a concise project description.
- Added exactly two linked credit entries: matchalattewithbanana (fork maintainer) and avinaxhroy (original creator). Existing upstream license and attribution notices remain unchanged.


### Update Center

- Added an in-app Material 3 Update Center with installed/latest versions, Stable and Preview channels, release notes, manual checks, downloadable APKs and explicit installation confirmation.
- Added opt-in daily WorkManager release checks with deduplicated notifications and a per-release mute option. No automatic download or installation.
- Verify exact official GitHub APK asset URLs, size and release SHA-256 digest. Check the downloaded APK's package name, Android version code and signing certificate before launching Android's system installer.
- Persistent WorkManager downloads display progress and survive leaving the screen; cancelled and incomplete transfers are kept separate.
- Added Settings entry/search, update notification deep linking, private FileProvider paths, version/release parsing tests and documentation. **Android build and device installation tests remain pending.**


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
