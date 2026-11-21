# Material 3 Expressive — Implementation roadmap

**Branch:** `main` · **Created:** 21 November 2026 · **Owner:** Tempo contributors
**Starting point:** Tempo **4.10.25-beta** *release candidate*, a snapshot of source already on `main`. This beta is for feedback; the projects below are future work, not features promised in that APK.

## Goals and non-goals

Build a distinctive, quick, delightful music journal with consistent Material 3 Expressive interactions across every workflow. Prioritize readable listening data, tactile but optional motion, artwork-led navigation, great Light/Dark/OLED presentation, and meaningful control over appearance.

- Preserve listening history, imported metadata, app identity, signer, backup contracts, and tracking reliability.
- **Performance is a feature.** Changes must not make Home/Profile slower, delay Now Playing, trigger network work during scrolling, or require high-end hardware.
- Keep experimental visual effects independent, **off by default**, and gated by reduced motion, power saver, low RAM, thermal conditions and foreground state.
- Avoid copying Orion Store source/artwork; use original Tempo designs and official Material APIs.
- Do **not** add carousels, floating actions or animation to places without a discoverable user purpose.
- Do not silently change notification access, background tracking permissions, or release signing.

## Source implementation checkpoint — 21 November 2026 (M3 Expressive skill follow-up)

**Source updates committed to `main` with `[skip ci]`, without changing tracking records, import deduplication, signing, version numbers, or release workflows.** These are genuine source changes, **not** a compilation/device validation certificate.

- **Motion and jank:** Profile badge detail, collapsing title, Song Details audio preview and level-up celebration now use theme motion schemes; reduced-motion mode snaps on supported interactions. Removed permanent Profile skeleton/hero pulses, Song Details skeleton/equalizer loops, Home Spotlight rotation/pulse, BPM ring loops, and delayed numerical/chart reveals in Song Details, Insight Feed, and the daily cadence chart. The level-up confetti now runs once instead of looping forever.
- **Adaptive navigation:** Compact primary destinations use `ShortNavigationBar` with `ShortNavigationBarArrangement.EqualWeight`. Floating destination selection has tab semantics. Existing navigation bar height/inset reservation remains in place. No new route or back-stack behavior.
- **Screen loading:** Settings now has 16 keyed lazy groups rather than one eagerly composed scroll column. Its search field and content are centered within a readable capped width on expanded layouts. Backup & Restore now has five independently composed lazy groups, while operation/confirmation dialogs remain independent of scrolling.
- **Theme consistency:** Custom tertiary container/on-container tokens and inverse snackbar-action tokens are paired. Color swatches for expressive tiles adapt text contrast. Artist fan-standing ranks retain accent hues but use theme tonal surfaces/text instead of fixed light-mode pastel cards. Level-up content now uses theme surface/text roles. Snackbar dismissal uses a localized description.
- **Regression sources:** Contrast tests cover Material inverse and tertiary role pairings across preset tones; expressive tile foreground tests cover bright and dark surfaces.

**Still blocking M3 Expressive sign-off:** Android Gradle compilation; full unit, Android lint and instrumented tests; screenshots of Home/Stats/History/Profile/Settings/Backup/Song/Artist across Light/Dark/OLED/System/custom colors; low-width/large-text/RTL and TalkBack input; short navigation and compact now-playing inset/selection behavior; restore and import interaction smoke tests; measured startup, jank, memory, and battery benchmarks. Source-level structural checks alone do not prove runtime correctness. The larger Phase 1–5 adaptive two-pane, layout-studio, share and screen-by-screen migration roadmap below remains open; do not label it completed.

## Roadmap overview

| Phase | Focus | Key deliverables | Exit gate |
| --- | --- | --- | --- |
| **0 — Stabilize** | Release build + compatibility | Compile current main, unit/lint/emulator checks, fix toolchain/API issues, screenshot baselines | One reproducible green beta; crash-free smoke tests |
| **1 — Immersive listening** | Song / Artist / Album | Native expressive detail headers, original artwork-derived surfaces, shared-element transitions, clear metadata/actions | Navigates smoothly, TalkBack and no-art fallback verified |
| **2 — Search + secondary UX** | Search, filters, import and restore | Unified search state, accessible filters, sheets, feedback, grouped list rows, resumable progress | No search lag or data-loss regressions |
| **3 — Layout Studio** | Actual layout freedom | Selectable Home/Stats/Profile layouts (compact, editorial, bento, immersive), layout preview and migration-safe persistence | Changes apply instantly; all density/font-scale checks pass |
| **4 — Large screens + advanced content** | Tablets, foldables, share | Adaptive list/detail panes, posture-aware navigation, purposeful music carousels, richer share flows | Rotation, resizable windows and back behavior verified |
| **5 — Polish + release** | Quality assurance | Contrast and typography sweep, optional motion tuning, benchmark comparisons, docs and beta-to-stable plan | All gates passed on representative devices |

Phases are **sequential by dependency, not calendar commitments**. Work within a phase may be delivered in smaller reviewable commits. Future preview releases should be scoped to verified changes rather than assuming the entire roadmap is complete.

## App-wide Orion reference integration — 21 November 2026

**Authoritative cross-app plan:** [Orion source comparison and Tempo implementation contract](ORION_M3_EXPRESSIVE_MIGRATION.md). Tracking epic: [#94](https://github.com/matchalattewithbanana/Tempo/issues/94). This extends the original roadmap to Home, Stats, History, all detail pages, Profile, Settings, Appearance, imports, backup/restore, share, Spotlight, widgets and the browser extension. It uses original Tempo-owned implementations, not GPL source copied from Orion.

**First additional source changes:** native low-prominence Material surfaces across shared `GlassCard`, adaptive theme-tonal Home Week/Discovery/Habits, live now-playing surface, immediate shared stat cards with optional motion, Native Material ranked Stats rows, compact Appearance Studio and Supported Apps top bars, and removal of duplicate Settings detail backdrops. Font-responsive weekly digest policy has a pure unit test. Previous screenshot corrections are separately documented below.

**Not yet ready for shipping:** no Gradle compile, screenshot review, emulator instrumentation or frame metrics have been performed; not all screen-specific legacy overrides are converted. Keep epic and screenshot regression issue open.

## Screenshot-driven corrective pass — started 21 November 2026

User feedback from the Settings and Daily Listening screenshots exposed a design-system split: large custom GlassCard diagnostics and oversized category tiles competed with Material 3 Expressive controls, Settings search disappeared on scroll, and a three-column daily metric rendered an ellipsized duration with its caption.

**Source updates committed to `main` (not built or visually verified yet):**

- [x] Settings: compact native tonal category destinations, a featured Appearance entry, readable single-column phone layout, and responsive two-column expanded layout. The categories now appear before diagnostics, backups and update shortcuts.
- [x] Settings: native surface containers for tracking health, backup and Update Center; a smaller fixed Material top bar; a search field that stays visible during scrolling; scroll reset when searching.
- [x] Daily Listening: theme-tonal summary/rhythm surfaces; duration value and caption are separate, multi-line text rather than a single forced ellipsis; label corrected to "per session."
- [x] Daily Listening: optional, policy-gated entrance motion instead of mandatory 50–320ms stagger plus 380ms reveal; skip sheet reveal motion when reduced motion is active.
- [x] Update pure Settings layout policy tests for 320/393/600/680/920dp and large font scales.
- [ ] **Required to claim fixed:** Android compilation, screenshot comparison against reported devices, semantics/tap targets and run-time navigation.
- [ ] Reproduce or rule out the screenshot's unusual 48 tracks / 1 album count via distinct album-key and missing-metadata checks. Don't change recorded listening data to make a card look right.
- [ ] Check whether one average session equaling the daily total is mathematically correct for the underlying session count; surface session count or an explanatory metric where useful.
- [ ] Confirm duration units in Settings immediately update every summary surface without waiting for a reload.

### App-wide expressive design contract

**Every Tempo screen** must share the same theme roles, typography hierarchy, shape families, native interaction semantics, adaptive window rules and motion policy. "Expressive" does **not** mean decorating every row or replacing useful data visualizations with generic Material cards.

| Surface | Material 3 Expressive target | Functional gate |
| --- | --- | --- |
| Home, Now Playing and Spotlight | Artwork-led hero, strong information hierarchy, cohesive native shortcuts and compact playback affordances | No blocked first content or extra artwork requests |
| Stats, rankings, trends and Daily Listening | Themed charts, consistent compact/expanded metric layouts, connected periods and native search | No truncated durations or stale filtered results |
| History, collections and library | Grouped native rows, coherent selection and undo, predictable sheets | Stable identity, correct pagination and no delete/data-loss regressions |
| Profile, badges and personalization | Themed editorial sections and adjustable density/layouts, not hardcoded gradients | Large-font and Light/OLED contrast screenshots |
| Settings, Appearance Studio and search | Fixed search, concise categories, native switches, previewable options, surfaced diagnostics | All settings discoverable, updates immediate |
| Import, restore, account links and Update Center | Shared native forms, progress, recovery, dialogs and error feedback | Long operations resumable and errors actionable |
| Song, Artist, Album and share/export | Native expressive detail scaffolds with original media treatments, real actions and flexible layouts | Artwork identity, back-stack, image export accuracy |
| Widgets, tablet, foldable and accessibility | Same semantics/colors adapted to each host surface | No fixed phone-width assumptions, 48dp targets, TalkBack |

**Design system rules:**

1. No new hardcoded per-screen gradients or `Color.White` labels where theme roles are available. Keep music/artwork exceptions deliberate, documented and tested.
2. Single source for 1.0×/1.3×/1.5× type, spacing, rounded shape, component surface elevation and pressed/selected states; support 320/360/393/412dp phones and wider windows.
3. Performance and motion are acceptance requirements: no forced reveal delay, looping motion or sensor work in ordinary scroll paths. Respect system reduced animation, app Reduce Motion, battery/thermal gating, and existing effects opt-ins.
4. Back/IME/search behavior and empty/loading/error content must match across Settings, Stats, History, imports and supported apps.
5. Before closing a screen's migration, compare before/after **actual screenshots** in Light/Dark/OLED, with 1.5× type and active playback; run compilation/unit tests and retain an explicit issue for any missing device check.

The screenshot-driven work above is deliberately *not* a claim that phases 0–5 are complete. Phase 0's build/test gate remains blocking; no APK or release has been authorized by this pass.

## Additional screenshot regressions — Settings subpages and bottom navigation (21 November 2026)

Five further user screenshots identify overlapping Appearance/App Icon options in General, duplicated giant title banners in General/Music Tracking/Backup, backup explanatory text repeated below Data Overview, very narrow Google Drive status next to an oversized Backup Now action, unusually large diagnostic disclosure cards, and a bottom navigation surface obscuring content.

**Source corrections now on main (review, not device validation):**

- [x] General Appearance rows explicitly wrapped in a Column. `GlassCard` is Box-based, so direct sibling `SettingsOption` calls overlapped in the screenshot.
- [x] `TempoSettingsCategoryHero` is a compact, native Material context strip; the top app bar remains the single page title. Backup's header likewise uses a compact `TopAppBar`.
- [x] Backups no longer repeat the appearance-scope description below the library-count summary. Drive backup action gets an independent full-width button row instead of crushing the status copy.
- [x] The "never backed up" status now explicitly describes **this device's** locally recorded Drive attempts, because remote Drive backup history can still contain earlier backups. English and the five existing locales updated.
- [x] Diagnostics disclosure panels use Material surface container roles, two-line collapsed descriptions and a policy-aware expand transition. Crucially, refresh is triggered in a `LaunchedEffect(expanded)`, not during composition.
- [x] Standard Material `NavigationBar` / `NavigationBarItem` temporarily replace the reported inflated `ShortNavigationBar` layout while keeping the app's theme colors and label preference. The root host reserves the bar's **measured** height, including the optional now-playing strip and gesture insets.
- [x] Settings shared rows use native Material switch colors, compact header spacing and readable supporting typography. Profile removal no longer crowds the name.
- [x] Backup and diagnostic pages inherit the existing root Appearance background instead of drawing a second full-screen gradient.
- [ ] **Validation required:** compile all affected screens, test real navigation height with/without compact Now Playing and both appearance navigation styles, review dark/light/OLED screenshots on 320/360/393/412dp, large fonts, keyboard and TalkBack, confirm restore/backup and diagnostics semantics.
- [ ] Revisit the native expressive short navigation only if library/API and real-device geometry can be confirmed. A stable compact Material control is preferable to an unverified expressive one that obscures music content.
- [ ] Establish screenshot/golden baseline to prevent future stacking regressions.

Do not claim the reported screenshot problems fully closed until a tested APK reproduces the corrected layouts. Existing source-only validation is not a substitute.

## Phase 0 — Critical: make the current beta testable

**Status:** release metadata prepared; compile/test/publish validation pending.

- [ ] Verify the `4.10.25-beta` candidate through the existing manual `beta-release.yml`: Android unit tests, Android lint, debug APK assembly, APK version/package/signature, emulator tests, Chromium/Firefox typecheck/build/smoke.
- [ ] Fix any compile issues from the large Compose 1.13 / Material 3 1.5 migration, SDK 37.1, AGP/Gradle upgrades, and newer `MotionScheme`/floating-toolbar APIs.
- [ ] Verify `versionName=4.10.25-beta`, `versionCode=4821`, browser `4.10.25.0`, and stable tag `v4.9.9` remains untouched.
- [ ] Confirm one signer fingerprint compatible with the previous GitHub APK before publishing; never rotate silently.
- [ ] Capture initial screenshots for Home, Stats, Profile, History, Settings, Appearance Studio, and Details in Light, Dark, OLED, and System tones.
- [ ] Validate initial loading, empty states, permission-denied routes, launcher icon picker, restored settings, overlays, widgets and browser pairing.
- [ ] Recheck screen-reader targets (48dp), contrast, 1.0/1.3/1.5x font scale, 320/360/412dp viewport widths, and gesture-navigation insets.
- [ ] Test foreground/background listening continuity, real-time time counters, now-playing, import dedupe and restore workflows with the UI visible and hidden.
- [ ] Establish **measured**, not assumed, frame time, startup, Home/Profile first content, peak memory and battery baselines against stable v4.9.9; record device/API conditions.
- [ ] Publish the prerelease only after validation, and label any unverified physical-device behavior clearly.

**Exit:** candidate APK and companion artifacts built from the same SHA; no critical compile/crash/signing defects, credible documented unresolved device limitations.

## Phase 1 — Music-first expressive details

**Scope:** `SongDetailsScreen`, `ArtistDetailsScreen`, `AlbumDetailsScreen`, navigation, artwork loaders.

- [ ] Define a reusable `MusicDetailScaffold` with flexible app bars, theme-derived gradients and a controlled artwork hero; avoid per-frame image palette extraction.
- [ ] Implement optional, reduced-motion-aware shared artwork transitions from a song/album/artist card to its detail page (stable keys, placeholders on missing images).
- [ ] Refresh metadata hierarchy: large track/artist/album headline, playing context, duration stats, ranked tracks and actions; avoid truncated titles.
- [ ] Provide consistent overflow/split actions only for relevant flows: share, open in player, export, manage metadata.
- [ ] Distinguish source-specific timing, unknown duration, estimated listening and imported data.
- [ ] Keep art loading cached and asynchronous; no extra network calls on recomposition.

**Exit:** clean back/predictive-back navigation, no blank art jumps, Light/OLED contrast, TalkBack reading order, no jank during active playback.

## Phase 2 — Search, sheets and workflow consistency

- [ ] Consolidate search patterns in Stats, History, Supported Apps, import and Settings. Include keyboard/focus/IME and clear-query behavior.
- [ ] Debounce remote or heavy filtering, cancel superseded requests; retain previous results during rapid input.
- [ ] Refine grouped list-item shapes and selection/undo semantics for track/history management; preserve established swipe behavior.
- [ ] Use purposeful Material sheets for filters, import-source choices and detailed actions; no nested modal stack or accidental data loss.
- [ ] Rework import/restore progress and conflict resolution with stable states, retry/recovery and accessible feedback.
- [ ] Upgrade onboarding and Update Center to consistent expressive hierarchy and error recovery.

**Exit:** UI remains interactive during 10k+ historical entries and overlapping import tasks; no duplicate writes, no lost navigation state.

## Phase 3 — Appearance and layouts, beyond colors

- [ ] Add persisted **layout presets**: compact, editorial, bento and immersive for Home, Stats and Profile.
- [ ] Provide live mock previews that represent actual component geometry and data, not decorative sample cards.
- [ ] Make density, album-art shape, corner family, section arrangement, typography sizing and card emphasis independently adjustable where meaningful.
- [ ] Respect and test the existing 18 palettes plus three-seed custom maker, launcher icons, Light/Dark/OLED/System tones and saved looks.
- [ ] Preserve old appearance JSON by additive decoding with safe defaults and explicit backup/restore coverage.
- [ ] Keep floating dock, ambient animation, tilt and list entrance motion opt-in with a clear reset/default path.

**Exit:** layout changes can be switched without reloading listening data or triggering network calls; no clipping at large text and reduced motion.

## Phase 4 — Adaptive and shareable

- [ ] Migrate expanded layouts from phone-width cards to Material 3 Adaptive `ListDetailPaneScaffold` or `SupportingPaneScaffold` where list/detail context genuinely helps. Consult stable [adaptive library releases](https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive).
- [ ] Keep compact screens in the current short-bar/floating-dock navigation and rail for expanded widths where appropriate; verify hinge/posture.
- [ ] Introduce an **optional** featured-album/listening-memory carousel with snap, pause and deterministic content; do not replace the main vertical scroll.
- [ ] Expand share-card layouts and album poster personalization with correct playback context and privacy-preserving exports.

**Exit:** tablet, foldable and phone layouts pass resizing/rotation/back and accessibility checks; no disproportionate CPU/GPU load.

## Phase 5 — Release hardening

- [ ] Stabilize all components against the pinned Material 3 API; no unexplained experimental version churn.
- [ ] Compare frame time/jank, cold/warm startup, Home/Profile content readiness and peak memory to phase-0 baselines. **Do not claim faster performance without measurements.**
- [ ] Run extended listening and battery-saver tests with and without optional effects.
- [ ] Audit screenshots and interactions in both high and low contrast environments, dark OLED, TalkBack, hardware keyboards and large-font configurations.
- [ ] Keep changelog, privacy, screenshots, asset credits, support and release catalog accurate; only publish a validated artifact with correct signing and checksums.

## Beta cadence and versioning

- Latest **published stable** before this candidate: `v4.9.9` (Android `versionCode=4820`).
- Commit count before release preparation: `v4.9.9..main = 116` (21 Nov 2026). New bounded carry rule: `4.9.(9 + 116) = 4.9.125 → 4.10.25`.
- **Current candidate:** Android `4.10.25-beta`, versionCode `4821`; browser manifest `4.10.25.0` (internal) with display `4.10.25-beta`, browser package/lock `4.10.25-beta`.
- Beta labels have **no sequence number**. Release one `4.10.25-beta`; any later preview should use a newly normalized base version, and every installable Android APK needs an increased versionCode. Browser manifest `.0` is the beta *internal ordering slot*, not a beta number.
- After successful beta and quality gates, stable promotion would be `4.10.25` (with another versionCode increment). The stable browser manifest becomes `4.10.25.1` and npm package `4.10.25`, ensuring it is newer than beta `.0`. Future releases use patch 0–99 and minor 0–10 inclusive; `4.10.125 → 5.0.25` and `5.0.205 → 5.2.5`.
- The beta is explicitly **not a stable release**. Keep older tags/assets immutable; publish only through the existing gated manual release workflow, including Android/browser packages and SHA256 checksums.

## References

- [Current Material migration](MATERIAL3_EXPRESSIVE.md)
- [Visual audit and Orion research](EXPRESSIVE_DESIGN_REFERENCE.md)
- [Release procedure](RELEASING.md)
- [Build and release authorization](BUILD_AND_RELEASE_POLICY.md)
- [Official Material 3 guidance](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Official adaptive list/detail guidance](https://developer.android.com/develop/adaptive-apps/guides/list-detail)

## Physical beta feedback follow-up — compact navigation, Profile and Settings

**Status:** Source repairs committed to `main` after the published `5.1.69-beta`; **not built or runtime verified**. Tracking issue: [#98](https://github.com/matchalattewithbanana/Tempo/issues/98).

Physical-device screenshots show four full-width oversized Profile progress tiles on a narrow phone, a redundant Badge Collection card before the Badges tab, inconsistent Home vs Stats short-bar item visibility, and large partly visible Settings bento tiles. Feedback also identifies unreliable overlapping imports and poor startup/tab motion.

This corrective pass:
- Replaces the experimental short bar with the well-established Material `NavigationBar`/item components so all three primary destinations remain allocated and labeled. Both regular and floating styles share the same rendering and selected state; the wide rail is unchanged.
- Reduces Profile to a responsive two-metric streak row, keeps level/XP in the identity hero, removes the duplicate Badge Collection entry, and shields the scrolling hero with a themed top-bar surface.
- Replaces the tall Settings staggered bento with compact adaptive category rows, moves manual service imports into Library & data, avoids repeating the Drive-backup success message, and reduces redundant overview tracking details.
- Uses system-controlled splash dismissal with system night-aware launch backgrounds, drops artificial splash icon zoom/220ms hold, and avoids double-animating top-level tab switches. Detail navigation retains Material motion tokens.
- Extends import reconciliation for saved live-source plays from Tempo ZIP restores and improves selection of compatible alternate track rows when a conflicting provider ID occupies the first title/artist match. Regression sources added; no existing user data is silently merged or deleted.

**Still blocking sign-off:** Kotlin/Room/Compose compilation, Android instrumented tests, rendered comparison at compact and large text widths, TalkBack and insets, real-world import/restore idempotence, and an investigation of the duplicate song shown as both Stats hero and #2. The screenshots may reflect duplicate track identities or old inflated imports; an apparently duplicated row must not be hidden without evidence. See [#98](https://github.com/matchalattewithbanana/Tempo/issues/98) and [#95](https://github.com/matchalattewithbanana/Tempo/issues/95).

