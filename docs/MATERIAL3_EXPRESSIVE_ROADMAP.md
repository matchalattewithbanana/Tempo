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
