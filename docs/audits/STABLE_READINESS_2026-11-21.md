# Tempo stable readiness — import, database, Material 3 Expressive and performance

**Date:** 2026-11-21  
**Target:** `main` at the latest commit for this audit.  
**Framework:** Kotlin, Jetpack Compose/Material 3 Expressive, Room/SQLite, WorkManager, extension TypeScript.  
**Method:** source-level end-to-end tracing; source inspection of key screens and integration tests; SQLite `EXPLAIN QUERY PLAN` on a representative in-memory schema.  
**Result:** **BLOCK stable publication** until the build/test/device gates below are satisfied. No APK, CI workflow, or release was triggered by this audit. All source updates use `[skip ci]`.

This is a **full source/UI review** in the interface-polish skill's terminology, not an instrumented or screenshot review of every screen. Treat uninspected runtime states as **Not verified**.

## Source coverage and findings

| Area | Files / evidence inspected | Result |
|---|---|---|
| Import and backup semantics | `ListeningImportPolicy`, `ListeningEventDao`, `EventFingerprint`, `SourceAuthority`, `ImportExportManager`, `TempoExportJsonCodec`, Spotify JSON/API, Apple, Deezer, Last.fm, YouTube, Musicolet, browser-extension services | Preventive reconciliation covers primary importers. Room tests expanded. End-to-end *actual* ZIP/Drive and multi-service tests still required. |
| Database and migration | Room entity/DAO, schema version 58, migration chain test, indexed SQL lookups and staging strategy | No schema change introduced. Representative SQLite `EXPLAIN QUERY PLAN` uses indexed track title+artist, (track, timestamp), and fingerprint lookups. Real-device query timings and giant-library stress not measured. |
| Typography | Home Insights, Profile, History, Stats, Appearance, Settings, detail screens, shared tiles | Fixed undersized informational labels in Profile/History/Insights. Other small ornamental labels require screenshot/large-font review. |
| Surfaces and color | `Theme`, `AppearancePalette`, `TempoExpressiveTiles`, History, Home, Profile, share overlays | Native theme pairing mostly present. Fixed one History status badge that used an unmatched fill/text pair. Light/OLED/custom snapshots have not been taken. |
| Motion and animations | `AppearanceRuntimePolicy`, Home/Insight Feed, Profile, Stats search, detail screens, celebration overlays | Motion gating exists and permanent routine loops have largely been removed. Fixed fixed-delay Stats search focus. Special celebration orbit animation is intentionally localized to rare overlays; device motion review pending. |
| Icons, semantics, touch | `TempoBottomNavigation`, `TempoMainNavigationRail`, History and UI overlays, Supported Apps | Short bar and wide rail use native Material components. Label-hidden mode is a user option; TalkBack, keyboard, 48dp hit targets, font scale/RTL and floating navigation need device testing. |
| Performance/refresh | Keyed Settings/Backup lazy lists, Home/ViewModel repository reads, import cache invalidation, search, backdrop/effects policy | Source-level startup/scroll problems mitigated. No macrobenchmark, FrameTimeline, memory, cold-start, battery or ANR comparison yet. |
| Distribution | `ci.yml`, `stable-release.yml`, `release-bundle.py`, `app/build.gradle.kts`, version catalogs, Android/extension tests | Manual-only workflows are available. **Current version is `4.10.25-beta`; `release-bundle.py stable` rejects a `-beta` version** until stable metadata and release notes are deliberately synchronized. |

## Findings and exact actions (highest risk first)

| Severity | Location | Before | After / required action | Why |
|---|---|---|---|---|
| **HIGH — fixed in source** | `data/local/dao/ListeningEventDao.kt` (candidate matching loop) | An anonymous title/artist alias could *transitively* connect different known Spotify IDs | Require direct compatible identity before matching a candidate; added Android Room regression | Different recordings must not lose plays because a middle alias lacks an ID. |
| **HIGH — pending execution** | `data/importexport/ImportExportManager.kt`, `ListeningReconciliationIntegrationTest.kt`, `BackupSnapshotIntegrationTest.kt` | Cross-backup and source-overlap code was changed without fresh runtime tests | Added two-backup-with-newer-local data test; **still must execute** Room/emulator, multiple ZIP files, Drive SKIP/REPLACE, interruption/restore tests | Source correctness does not prove idempotence under transaction rollback and actual archive parsing. |
| **HIGH — pending execution** | `app/build.gradle.kts`, `.github/workflows/ci.yml`, `.github/workflows/stable-release.yml` | No successful post-change compilation/lint/instrumentation result | Manually run `ci.yml` on the final commit and inspect all 3 jobs, logs and artifacts; do not publish | Kotlin/Room/Compose changes are not validated by prior successful beta builds. |
| **HIGH — release preparation** | `app/build.gradle.kts`, browser extension manifests/package and `docs/releases/` | Stable validation would see the current `4.10.25-beta` string | On stable authorization, choose unused stable SemVer and monotonically higher versionCode; sync extension manifest/package/lock/notes, run `release-bundle.py stable` | The stable release validator intentionally requires matching non-beta metadata. |
| **MEDIUM — fixed in source** | `ui/stats/StatsScreen.kt` search focus | Search waited a fixed 280ms before keyboard focus | Focus on the next Compose frame instead; field remains opt-in | Artificial delay made frequent searching feel slow. |
| **MEDIUM — fixed in source** | `ui/history/HistoryScreen.kt`, `ui/profile/ProfileScreen.kt`, `ui/home/components/InsightFeed.kt` | 9/10sp informational captions; history badge paired arbitrary dark gold with `onPrimary`; selected insight text forced white | Use Material `labelSmall` scale and named scheme color-role pairings | Helps font scaling, readability and light-theme contrast; live screenshot checks still required. |
| **MEDIUM — open** | `ui/navigation/TempoBottomNavigation.kt`, `TempoMainNavigationRail.kt` | Optional icon-only navigation can hide visible labels | Preserve preference with icon descriptions; verify 2x font scale, TalkBack, nav item state, screen reader, insets, 839/840dp breakpoint | M3 recommends visible labels; user customization is a deliberate exception requiring accessibility proof. |
| **MEDIUM — open** | `data/local/dao/ListeningEventDao.kt` and historic databases | Source/timing ambiguity means some older duplicated plays with different metadata are not provable | Keep historical fuzzy cleanup behind a future review/preview + exported backup; exact hash cleanup stays explicit only | Avoid deleting genuine repeats or independent device sessions. |
| **MEDIUM — open** | `data/lastfm`, `data/musicolet`, imported metadata/Stats | Some providers give scrobbles/reconstructed counts rather than measured listening intervals | Keep the source-specific uncertainty visible; compare a user-provided overlapping history corpus before any estimated-time repair | Raw duration addition can misstate listening time where there is no reliable evidence. |
| **MEDIUM — pending screenshots** | Home, Stats, History, Profile, Settings, Appearance, Backup, Song, Artist, Album, share | No post-change screenshots or performance trace | Inspect Light/Dark/OLED/System/custom, landscape/tablet, large type, reduced-motion, slow/failed/empty/loading states, sharing and import progress | Static Kotlin review cannot establish rendered UI quality or jank. |
| **MEDIUM — pending operational** | Google Drive, Android OAuth signing, extension pairing and tracked background sessions | No actual updated signed APK or real-account Drive restore tested | Test known OAuth signer, cloud download/upload, on-device backup/restore, session tracking while app backgrounded, extension imports | A stable release must not regress sign-in, restore or live tracking. |

## Considered but rejected

1. **Delete all historical events whose time intervals overlap:** rejected. Parallel devices, different real recordings and adjacent replays can share timestamps.
2. **Force arbitrary 48dp visual sizes on every icon/chart mark:** rejected. Some are decorative, not interactive; audit the actual semantic hit target on-device instead.
3. **Remove rare milestone celebration graphics because they animate:** rejected without evidence. They are context-specific, not scroll-loop animations; still test reduced motion and lifecycle teardown.
4. **Change database schema/version just to accelerate the new alias join:** rejected until Android query profiling. A representative SQLite planner uses existing indexes, and adding an index now requires migration/snapshot validation.

## Verification evidence and limits

- Inspected code and tests for Spotify UTC end anchors, `ms_played`, authority, cross-source overlap, exact content hashes and ID remapping.
- SQLite `EXPLAIN QUERY PLAN` on a matching in-memory schema: **track alias join uses primary-key + (title, artist) index; event window uses (track_id, timestamp) index; fingerprint lookup and exact duplicate count use fingerprint index**. This is **planner evidence only**; no representative data volume or Android timings.
- Verified the `formatStatsDuration` source uses **60,000ms/minute**, **3,600,000ms/hour**, **86,400,000ms/day**, with tests for precision, boundaries and non-premature rounding.
- Checked GitHub Actions history: the latest reported successful beta workflow ran on **2026-11-20** at SHA `fbdfd5769f...`, prior to this audit and earlier import/UI commits. **It is not evidence that current main builds.**
- No local Android SDK/Gradle execution, Android screenshot review, on-device TalkBack, Google Drive real-account restore, extension typecheck/smoke or cold-start/frame metrics were executed during this audit.

## Required gates before stable

1. Run **CI (manual)** workflow on the final main commit: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:lintDebug`, `:app:connectedDebugAndroidTest`, extension `typecheck`, `build:all`, `smoke`. Fix all failures and rerun on new HEAD.
2. Instrumented restore matrix: overlapping Backup A and B, old ID/fingerprint remapping, same ZIP twice, SKIP/REPLACE semantics, local post-backup plays, mixed Spotify/Apple/Deezer/Last.fm/YouTube/Musicolet/extension; inspect counts and **total listening milliseconds**, not only import summaries.
3. Visual/audio/UX matrix: phone compact and expanded/foldable; Light/Dark/OLED/system and custom tones; reduced animation; 1.5x/2x font; TalkBack; icon-only/floating/bar/rail; empty/error/progress/backup dialogs; Now Playing and background tracking.
4. Performance gates: query plan on realistic history size, restore memory and transaction latency, cold startup, Home/Profile/Stats frame traces, no unwanted recomposition loops or network wait on UI.
5. Verify Google account/OAuth signer SHA, Drive backup/restore, rollback/recovery and privacy disclosures; retain a safe user backup.
6. On **explicit stable-release approval only**, update stable app+extension versions and release notes, validate immutable package, signing, checksums, and publish once. Current workflow has a `publish_stable=false` validation-only switch, but requires stable-version metadata first.

**Verdict: BLOCK**. Source fixes and new tests are not a substitute for the above runtime results. Trackers: [Import verification #95](https://github.com/matchalattewithbanana/Tempo/issues/95), [M3 Expressive #94](https://github.com/matchalattewithbanana/Tempo/issues/94).
