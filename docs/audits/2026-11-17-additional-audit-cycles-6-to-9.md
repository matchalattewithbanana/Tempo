# Tempo secondary audit — cycles 6–9 (2026-11-17)

**Baseline:** `main` commit `1f0decf6e02fbf95dca64e4f9fb1bb85ee025b9e` at start of this audit. A previous cycle created issues #6–#22 and master #23. This follow-up reviews data semantics, defensive persistence, UX/accessibility, localization and product architecture. **It is not a full line-by-line or dynamic audit** of all 925 repository paths.

**Scope:** Additional risk-based review only; no app code changed, no CI/APK workflow invoked, no release published. These two planning documents are authored separately: [Product roadmap](../PRODUCT_EVOLUTION_ROADMAP_2026.md) and [UI v2 RFC](../LISTENING_JOURNAL_DESIGN_SYSTEM_RFC.md).

## Cycle 6: event semantics and live integration

### A6.1: Daily totals misattribute playback across local midnight — P1 #24

- `StatsDao.getDailyListening` groups by local day computed from `le.timestamp` (event start) and sums all `playDuration`. Today/period overview also filters by event start timestamp.
- `MusicTrackingService.insertListeningEvent` writes `timestamp=session.startTimestamp` and `endTimestamp=currentTime`, so a session spanning midnight lands entirely on its start date. This is an *actual SQL behavior*, not a proven customer bug.
- Correct fix needs a specified per-source time attribution and active-play duration policy; naively prorating by wall-clock span would be wrong for pauses, seeks and imported end-time-only records.
- Issue: https://github.com/matchalattewithbanana/Tempo/issues/24

### A6.2: Stale Spotify playback across same-named recordings — P2 #25

- `SpotifyBroadcastBridge` invalidates inherited `playing` only on title or artist change, ignoring changed album and duration; then immediately publishes the new metadata plus prior playing status.
- Real duplicates/radio edits can have the same title/artist but differ otherwise. Existing trust check against Spotify MediaSession should be preserved.
- Issue: https://github.com/matchalattewithbanana/Tempo/issues/25

### Existing coverage not duplicated

- Event dedup and source authority in `ListeningEventDao`.
- Spotify API verification and broadcast readiness; known CI and tracking issues #7, #9, #10.
- Date rollover invalidation issue #15 already exists; #24 instead addresses how events are **attributed** to days.
- Check for actual effect via synthetic fixtures before claiming live/user data corruption.

## Cycle 7: pending-play durability and source recovery

### A7.1: Corrupt offline queue load is silently replaced by empty — P1 #26

- `OfflineEventStore.load()` returns an empty list on **any** JSON/read error. `MusicTrackingManager` treats this as an empty in-memory queue and starts normal processing, later saving current queue snapshot back over the file.
- This creates a potential destructive recovery path on truncated/corrupt durable pending-events storage; the source doesn't preserve corrupted bytes for repair.
- Issue: https://github.com/matchalattewithbanana/Tempo/issues/26

### Existing coverage not duplicated

- Extension cap discarding unsent rows is already #8.
- Offline serial backoff delays already #10.
- Backup restore uncompressed JSON resource constraints already #17.
- Migration partial snapshots already #19; analytics (not listening records) silent writes already #20.
- Google authentication and token storage independently reviewed in first cycles.

## Cycle 8: compositional accessibility and localization

### A8.1: Reduced motion still constructs skeleton animation — P2 #27

- `ProfileLoadingSkeleton` unconditionally builds an infinite transition; when reduced motion is true, it simply displays a fixed value, while the animation still exists.
- Another profile component already uses the safer conditional animation-creation pattern. Fix all equivalent cases in core screens after scanning; do not overclaim power savings without profiler.
- Issue: https://github.com/matchalattewithbanana/Tempo/issues/27

### A8.2: User-facing strings bypass localized resources — P2 #28

- `ProfileScreen` contains English screen-reader labels ("Back", "Settings") and content ("Listening rhythm", "Streak at risk").
- `SpotlightScreen` hardcodes loading, empty and descriptive text.
- `BackgroundProtectionScreen` hardcodes device/MIUI copy and steps while the app already has `values-de/fr/hu/pt/ru`.
- Not all hard-coded strings are user-facing; protocol IDs, code constants, test strings and nonlocalizable brand/legal names must be classified first.
- Issue: https://github.com/matchalattewithbanana/Tempo/issues/28

### Verified good patterns

- `DashboardWeekChart` uses `clearAndSetSemantics` and a per-day duration/play-count description; do not file a claim that all bars are inaccessible.
- `MastheadHeroMetric` checks reduced motion before constructing looping animation; use as reference.
- `HistoryScreen` has a swipe-delete confirmation flow; do not label all swipe actions destructive without verifying their dialogs.
- `AppearanceScreen` uses resource strings extensively; most preset descriptions are localized. The problem is uneven coverage, not an entirely untranslated app.
- Existing massive visual-design epic #5 covers layout fidelity, light palettes, navigation padding and touch targets; no duplicate issue for every aesthetic mismatch.

## Cycle 9: architecture, permissions, visual governance and identity/licence

### A9.1: App's architecture has enough capability, but no enforceable design contract

- `AppNavigation` has Home, Stats, History, Settings, Appearance, Spotlight, Profile, data import, backup and multiple detail routes; UI composition and shared components overlap. Existing visual postmortem #5 is primary source.
- `AppearanceSettings` already supports six looks, light/system/OLED, shape/density, Home section ordering and Stats default tabs. A new design must **migrate** appearance settings rather than replace them.
- `AppNavigation` owns a root `DeepOceanBackground` and several inner screens apply it again; already logged in #5.
- Root background/inset hierarchy and screenshot-based visual QA should be treated as **architecture**, not fixed with ad hoc pixel adjustments.
- Existing #16 covers screenshot and extension typecheck/smoke gate; #11 covers non-gating Android lint. No duplicate issue filed.

### A9.2: Licence is a release blocker for a new independent public identity

- The repository's `LICENSE` begins with custom noncommercial and *no rebranding/white-labeling* restrictions.
- Existing `BREADCRUMBS_REBRAND_PLAN.md` correctly requires written rights authorization and proper trademark/name clearance before publicly adopting a new brand. Ownership/permissions aren't established by this audit.
- An original app layout can be designed as an idea, and improvements can be planned under the existing visible identity, but **a distributed rebrand cannot be treated as approved**.
- Proposed decision gate in product roadmap and identity epic; not categorized as a code-security bug.
- Preserving application ID/signature, OAuth redirect, Room history, backup and browser pairing remains a hard functional requirement if authorized.

### A9.3: External UX research is guidance, not executable proof

- Google's Material 3 Expressive (shape, hierarchy, motion, containment) is a useful pattern library, not a default skin.
- Android edge-to-edge on API 35+, predictive back on 36+, window size classes and 48dp targets should be part of scaffold contracts.
- W3C contrast should evaluate composited colors (Light preview swatches already failed earlier review).
- NN/g progressive disclosure and complex-app guidance motivate moving repeated analytics *out of* Home.
- stats.fm and Last.fm are feature-comparison references, not permission to reproduce designs or claim feature parity.

Links:
https://design.google/library/expressive-material-design-google-research
https://developer.android.com/develop/ui/compose/system/setup-e2e
https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps
https://developer.android.com/guide/topics/ui/accessibility/apps
https://www.w3.org/TR/WCAG22/
https://www.nngroup.com/articles/progressive-disclosure/
https://www.nngroup.com/articles/complex-application-design/
https://support-new.stats.fm/hc/en-us/articles/59061730156441-Charts-and-the-stats-screen

## Severity summary of new findings

| Issue | Severity | Failure class | Source proof | Runtime verified? |
|---|---|---|---|---|
| #24 | P1 | Daily stats semantics | SQL + event timestamp assignments | No synthetic/device run |
| #25 | P2 | Metadata staleness | Broadcast identity check | No real Spotify stream |
| #26 | P1 | Offline durable queue loss | Decode fallback + manager init/save | No corrupt file injection |
| #27 | P2 | Reduced-motion resource use | Unconditional transition + constant-alpha display | No profiling |
| #28 | P2 | Localization/a11y copy | Hardcoded user-visible strings in five-language app | Not manually tested in each locale |

**Next evaluation gates:** establish unit fixtures and runtime test harnesses with explicit user permission to run them, then implement prioritized fixes in small batches. Do not publish visual or functional claims from source inspection alone.

## Repository files sampled in follow-up

- `service/MusicTrackingService.kt`, `service/SpotifyBroadcastBridge.kt`, `service/OfflineEventStore.kt`, `service/MusicTrackingManager.kt`.
- `data/local/dao/StatsDao.kt`, `data/local/LocalTimeSql.kt`, `data/local/entities/ListeningEvent.kt`, `data/repository/RoomStatsRepository.kt`.
- `ui/home/HomeScreen.kt`, `ui/home/HomeViewModel.kt`, `ui/home/HomeListeningDashboard.kt`, `ui/home/WeeklyListeningChart.kt`.
- `ui/navigation/AppNavigation.kt`, `ui/navigation/TempoBottomNavigation.kt`, `ui/appearance/AppearanceSettings.kt`, `ui/appearance/AppearanceScreen.kt`.
- `ui/profile/ProfileScreen.kt`, `ui/spotlight/SpotlightScreen.kt`, `ui/stats/StatsScreen.kt`, `ui/history/HistoryScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/BackgroundProtectionScreen.kt`, `ui/theme/Modifiers.kt`.
- Existing plans: `BREADCRUMBS_REBRAND_PLAN`, `ARTWORK_IDENTITY_PLAN`, `APPEARANCE_PERSONALIZATION_PLAN`, `REALTIME_TRACKING_PIPELINE`, `PERFORMANCE_AUDIT`, `BUILD_AND_RELEASE_POLICY`, and `LICENSE`.

No global claim of zero additional bugs follows from these cycles; they are bounded, reproducible source findings with known testing gaps.
