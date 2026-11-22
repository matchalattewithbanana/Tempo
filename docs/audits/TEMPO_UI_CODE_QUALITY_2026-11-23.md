# Tempo — strict code-quality and software-design audit
**Date:** 2026-11-23 · **Scope:** latest UI changes and surrounding architecture on `main`
**Methods:** repository tree and selected full Kotlin source inspection, comparison against the recent UI baseline, static references and structure checks.
**Validation:** this is a source-level review. Gradle, Android instrumentation, APKs and device frame traces were **not run**.

## Verdict

**Not yet release-validated.** The current UI work improved visual consistency, but a structural problem had appeared in Settings search: the search catalog independently hard-coded the positions of groups inside five different lazy lists. An otherwise harmless UI reordering or an optional Analytics group could send users to the wrong setting. This review fixed that issue instead of accepting another special-case offset.

**Resolved now — Settings group index coupling.** Search result targets use a typed `SettingsGroup` owned by Settings. The same semantic key is referenced by the rendered lazy-list item, and visible row positions are derived centrally from group order and conditional availability. The old `SettingsSearchEntry(groupIndex: Int)` contract is removed. The Analytics visibility rule is read from its group rather than duplicated in search-specific status logic. Source cross-check: **17/17 groups** have one backing item, with no undeclared/duplicate group key; all **29** indexed controls refer to a declared group. Added pure contract tests plus Compose search navigation coverage for podcast filtering and unauthenticated Spotify device broadcasts. Tests are committed, **not executed**.

## Outstanding structural findings (highest-priority first)

### P1 — `AppearanceScreen.kt` is one oversized stateful composable
- **Evidence:** 675 lines, one `fun AppearanceScreen`, seven remembered-state references, draft/save/exit dialog orchestration and many inlined appearance controls in the same function.
- **Cost:** changing a simple appearance control requires reconstructing transfer state, save behavior, dialogs, and page structure together. Rendering and workflow state are tightly coupled.
- **Code-judo direction:** keep `AppearanceScreen` as the single draft state owner, but extract **deep, purpose-named editor sections** with immutable draft/callback contracts. Keep persistence and exit-discard semantics at the screen boundary; do not add pass-through view models or one wrapper per button.

### P1 — `SpotlightStoryScreen.kt` combines playback orchestration and rendering
- **Evidence:** 726 lines, fourteen `remember` references, four `LaunchedEffect` sites, story paging/timing, audio controls, share-image capture and an offscreen renderer together.
- **Cost:** lifecycle, pause/resume, share transitions, and page navigation are interleaved with UI composition, making interruption bugs difficult to test.
- **Code-judo direction:** a focused playback-session state holder owns pause/advance/lifecycle rules; the UI observes a coherent state. Move capture orchestration behind a stable snapshot/capture interface. Preserve existing page timing and story exports; first add tests for pause, rapid navigation, cancellation and background/resume.

### P1 — Backup/restore orchestration approaches the 1,000-line gate
- **Evidence:** `BackupRestoreViewModel.kt` has **982 lines** and **37 functions** covering local transfer, Google authorization, cloud transfer, scheduling, state refresh and error paths.
- **Cost:** data-destructive restore paths share a broad mutable coordinator with UI status and routine scheduling.
- **Code-judo direction:** separate **operations by ownership** (authorization, local transfer, cloud transfer and scheduling), not merely into smaller same-shaped files. The ViewModel should orchestrate a small set of policy-level operations. **Do not rewrite until local/cloud restore regression tests and cancellation semantics are established.**

### P1 — Story generation is another near-threshold module
- **Evidence:** `SpotlightStoryGenerator.kt` has **981 lines**, including a `generateStory` function spanning roughly lines 44–676; many page-type branches are concentrated there.
- **Cost:** one new story card can perturb optional-slide sequencing, preview selection, page ordering and fallback behavior.
- **Code-judo direction:** identify an explicit data-preparation + story-selection boundary. Prefer a small number of cohesive page builders, not an extensible registry/framework unless it removes real branches. Lock down story ordering and optional page tests first.

### P2 — Top-level navigation is a wide composition boundary
- **Evidence:** `AppNavigation.kt` has **628 lines** with more than twenty route declarations in one `AppNavigation` composable.
- **Cost:** deep-link handling, top-level motion, feature callbacks and route declarations all require attention during minor navigation changes.
- **Direction:** group routes only where a feature graph meaningfully hides route-specific dependencies. Keep shared transitions and the primary navigation policy centralized. Splitting each `composable` into a pass-through wrapper would *increase* complexity.

### Separate, non-UI architectural risk — Tracking and statistics
- `MusicTrackingService.kt`: **3,414 lines**, 77 function declarations and around 195 conditional occurrences.
- `RoomStatsRepository.kt`: **2,468 lines**, 92 function declarations and around 146 conditional occurrences.
- These predate the current UI pass and deserve a separate behavior-preserving architecture project. In particular, **do not entangle tracking, session recovery or database work with appearance refactors**. Establish device and persistence baselines before touching them.

## What the strict review did *not* find
- None of the recent UI files inspected crossed from below 1,000 lines to above 1,000 lines.
- The new Spotlight, cover-source and backup composables use Material color roles instead of new global mutable state.
- Settings search remains a navigation projection; it does not become another preference owner.
- The changes above do **not** modify playback tracking, imported listening history, backup data formats or database schema.

## Release acceptance gate

1. Compile Kotlin and Android resources and execute unit + Compose instrumentation tests, including Settings group navigation, disabled Spotify API with broadcasts, Spotlight controls, and backup width policy.
2. Test 320/360/440dp, accessible font scaling, TalkBack and gesture-inset placement; verify search navigation survives every optional settings visibility combination.
3. Check Light/Dark/OLED/custom accents, rapid navigation, background/restore, and active-player UI with frame-time traces before asserting a performance gain.
4. Fix any compiler/runtime issues **before** enlarging the motion system. Keep `[skip ci]` commits on `main`; no build/release unless explicitly authorized.

## Advisor checkpoint limitation

The Advisor skill was read and its decision criteria applied. Its independent `advisor-subagent` Task mechanism is **not available** among the tools in this session; therefore no separate model verdict is claimed. An independent model review remains outstanding.
