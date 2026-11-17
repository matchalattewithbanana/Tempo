# Tempo visual-system postmortem — Home redesign, appearance, and navigation

**Date:** 2026-11-17  
**Audit type:** Repository-wide UI source inspection plus comparison with the illustrative **“Your listening”** mockup supplied in chat.  
**Scope:** Home, Now Playing, navigation, appearance studio, shared surfaces, Stats, History, Settings, Profile, song/artist/album details, Today overlay, onboarding and Spotlight.  
**Status:** Audit and proposed corrections only. **No APK release, no visual sign-off, no rebuild initiated by this audit.**

## Integrity of the review

The supplied image shows a ChatGPT/browser mockup with **illustrative** numbers; it is *not* an in-app Tempo capture. We can substantiate component composition, data flow, sizing and theme gaps from source, but we cannot assert how the current APK looks on the user's specific device. A genuine rendering/side-by-side screenshot must be obtained before declaring the replacement aesthetically ready. Earlier automated tests proved build/runtime paths, **not visual fidelity or polish**.

## Executive verdict

**Rework the Home architecture and shared UI hierarchy instead of stacking more style patches.** The initial redesign reused a generic, embedded Material3 Now Playing card inside a separate dashboard Surface while retaining the older Hero, Today, Spotlight and stats screens underneath. The app therefore has at least four competing visual vocabularies: legacy glass, new Material3 surfaces, editorial artwork rooms, and Spotlight/canvas themes. New Light palettes improve token contrast but not structural consistency. This is a **design-system and interaction-contract regression**, not principally a palette problem.

## Confirmed findings

| ID | Priority | Subsystem | Source-backed finding | Impact / correction |
| --- | --- | --- | --- | --- |
| V01 | P0 | Home / Today | `HomeListeningDashboard` opens the Today overlay immediately on metric tap, while `HomeViewModel.fetchData` only loads top tracks, artists, hourly distribution and period comparison if `loadExtended` is true. `HomeScreen` passes the still-empty lists directly to the overlay. | The default collapsed dashboard can open a **partially empty** Today detail view. Fetch on demand when the sheet is opened, with a loading/empty/error state and cancellation safety. |
| V02 | P1 | Home semantics | The dashboard labels the second metric `Tracks played today` but uses `todayOverview.uniqueTracksCount`, not `totalPlayCount`. | Distinct tracks are not total plays. Choose and label the desired metric explicitly. Avoid claiming 38 plays when 38 is just the number of distinct tracks. |
| V03 | P1 | Home design | The illustrative mock has a lean single-panel hierarchy; code wraps the entire feed in a Material `Surface`, uses three header `IconButton`s, embeds `LiveNowPlayingCard`, and adds nested `Surface` metrics with unrelated geometry. | The composition is not a faithful translation of the intended visual language. Create a Home-specific composition with a unified grid/type/space system rather than composing old generic widgets. |
| V04 | P1 | Home expansion | `HomeScreen` brings back legacy `HeroCard`, `TodaysListenWidget`, Spotlight and quick-stat cards when `showExtendedInsights` is true, even though the main card already shows a weekly chart and Today stats. Expansion is persisted. | Duplicated information and clashing design languages. Retain *additional* insights only, remove redundant summaries, and separate analytics navigation from optional Home sections. |
| V05 | P1 | Navigation | `TempoBottomNavigation` uses 72dp content height **plus 24dp padding above and below** and system navigation insets. The Home feed independently reserves 116/176dp of fixed bottom padding. | Overlarge nav footprint, inefficient small-screen use and brittle scroll/inset spacing. Use measured/scaffold insets and a more compact navigation structure. |
| V06 | P1 | Appearance contract | `AppearanceSettings.shape` is applied by `appearanceCardShape()`, but Home metric tiles, header section, navigation, mini-player and preview use fixed 20/22/36dp corner sizes. Density changes shared GlassCard padding but not the new dashboard's fixed 18/20dp layout. | Personalization controls do not consistently affect the surface people see most. Apply shared shape/density tokens to new components. |
| V07 | P1 | Light preview | `AppearancePresetCard` puts `onAccent` (white in Light) over `palette.accent.copy(alpha=0.77f)`. Against the pale/white preview surface, the effective text contrast is roughly **4.0–4.4:1** across the six Light accents, beneath the normal 4.5:1 threshold. Existing tests only check fully opaque accent. | Text can be hard to read even in the Appearance Studio gallery. Use an opaque accent block or recompute foreground against the **composited** color; add tests. |
| V08 | P1 | Visual test coverage | Existing CI runs compilation, unit tests, Android lint and emulator instrumentation. There is no accepted screen-render/golden baseline for the new Home in 320dp/360dp, Dark, Light and large-font configurations. The gallery preview contains a miniature generic song card, not the new Home layout. | Green automated tests cannot verify design fidelity. Add screenshot capture/comparison plus an explicit human-review gate before shipping more visual work. |
| V09 | P2 | Home chart | The seven-day chart has a fixed 120dp track and uses abbreviated weekday labels. The `This week` label is plain text (not a selector). | Inflexible sizing and a misleading interaction affordance compared with the mock. Use a scalable chart and either a functional period selector or non-dropdown label. |
| V10 | P2 | Playback | The embedded `LiveNowPlayingCard` shows extra status/source labels, a separate live-session estimate and “saved after playback ends” copy. Its idle state adds two instructional paragraphs. | Noisy visual hierarchy; the mock instead uses cover/title/artist, unobtrusive status, progress and duration. Give Home a dedicated compact status treatment; keep diagnostic provenance behind a detail or tracking-health action. |
| V11 | P2 | Empty state | The dashboard renders regardless of history and already prints “No listening recorded this week”, while `HomeScreen` independently mounts an additional `EmptyState` of about 70% screen height when no data exists. | A new user's entry screen can become excessive and repetitive. One purposeful, compact and actionable empty state is sufficient. |
| V12 | P2 | Palette richness | `AppearancePalette.materialColorScheme()` assigns the same accent to primary/secondary/tertiary, with several Material surface containers collapsed to the same background. | Six nominal themes often differ chiefly by hue, with weak hierarchy and few supporting tones. Define deliberate secondary/tertiary and elevation roles per look without sacrificing contrast. |
| V13 | P2 | Dark/light migration | SpotlightScreen uses its own dark-specific visual tokens; Profile, artist details, share canvas and onboarding include substantial hardcoded artwork/gradient colors. These may be intentional, but they are not globally governed by the six-preset theme system. | Tone and artwork treatments are unpredictable between app destinations. Classify branded/immersive intentional exceptions versus ordinary components, and harmonize only ordinary UI. |
| V14 | P2 | Background layering | `AppNavigation` already wraps its content in `DeepOceanBackground`. Home, Settings, Stats and several other routes apply it again. | Redundant full-screen gradient drawing and an ambiguous layer contract. The root should own application background; inner destinations should draw only purposeful local surfaces. |
| V15 | P2 | Typography | `AppearanceTypography` switches fonts for display/headlines and `titleLarge` but not `titleMedium`/smaller labels. Many screens also override fonts or text sizes directly. | Inconsistent typographic voice within the same theme. Build a small controlled type scale and document intentional editorial exceptions. |
| V16 | P2 | Navigation labeling | The Home header magnifier invokes `onNavigateToHistory`; it does not focus a dedicated search field, and the 3×48dp trailing actions squeeze the heading on compact devices. | An ambiguous Search action and cramped top row. Reduce primary actions to Search/Settings and make Search land in focused History search, if feasible. |
| V17 | P2 | OLED behavior | `AppearancePaletteResolver` makes OLED background and sunken color black, but other surfaces remain chromatic; `DeepOceanBackground` blends `topBackground`, `surface` and `surfaceSunken`. | “OLED” is not consistently flat black. Clarify that it means black background with raised cards, or enforce black structural surfaces. |
| V18 | P2 | State refresh | Home computes `LocalDate.now()` for the chart during recomposition and relies on listening updates for data reloads. There is no dedicated midnight/week-change invalidation in this UI. | A long-open Home can show yesterday's chart/period after local midnight until another data refresh. Trigger lightweight date-boundary refresh and test timezone changes. |
| V19 | P2 | Code maintainability | The main Home view still contains legacy section implementation and design logic alongside the new screen; it conditionally renders old cards while `HomeListeningDashboard` mixes navigation, rendering and formatting responsibilities. | Visual fixes require editing several scattered systems and can accidentally restore duplicate cards. Extract small pure view models and design components with bounded public APIs. |

### Scope distinctions

- Artwork/story share cards **can** use special dark or high-chroma gradients; their deliberate artistic treatments should not be blindly replaced with light semantic tokens.
- Fixed colors in decorative effects do **not automatically** represent accessibility failures; audit text-over-background pairing and actual rendered pixels before changing them.
- No assertion of current-device screen appearance is made from the browser screenshot. Device capture and physical review are still required.

## Proposed redesign: not another round of palette patches

1. **Capture truth first.** Obtain actual app screenshots (dark and light) on an emulator and at least one physical phone; include empty / playing / paused states, collapsed / expanded Home, 320dp and 360dp width, 1.3× and 2× text scale. Compare to the supplied illustrative mock side-by-side.
2. **Choose one layout contract.** Use an open Home header with two functional actions; a standalone, visually integrated Now Playing row and progress; two aligned Today metrics; a compact seven-day chart; then a restrained “More insights” affordance. Avoid an additional heavy card border around everything unless it materially helps hierarchy.
3. **Reduce the number of systems.** One spacing scale (4/8/12/16/24/32dp), one type hierarchy, one navigation/inset policy, common card-shape and density roles, and a small number of semantic surface elevations. No gradients for ordinary stat tiles by default.
4. **Eliminate duplicate analytics.** Keep older Hero/stat widgets off the default feed or remove them when their information is covered. The expanded area should contain only genuinely additional listening insights and Spotlight.
5. **Make interactions real.** Today detail must fetch its own data when opened; Search should open search; period labels should behave as labeled; listening counts should be semantically correct.
6. **Respect theme exceptions.** Harmonize ordinary UI under semantic roles while preserving intentional full-bleed creative experiences such as Spotlight/story/canvas. Preview actual Home (not only a generic sample card) in all six looks.
7. **Gate quality on visible output.** Build screenshots at multiple densities/font scales; review rendered screenshots against a reference and accessibility checks; unit tests/lint alone are insufficient. No release until the user approves the visual result.

## Acceptance criteria before asking for a new APK

- Home has no duplicate Today/week summary when expanded, no blank Today drilldown, no broken or misleading controls.
- The UI reads coherently at 320dp, 360dp, 411dp and large-font accessibility sizes; bottom navigation and safe insets never occlude content.
- In the common idle state, Home makes music-tracking readiness and lack of history clear without occupying most of the screen with stacked placeholders.
- At least one screenshot per Dark, Light and OLED style, plus playing/idle states, is visually reviewed. User-selected shape/density/accent visibly and predictably influence the design.
- Real Today/weekly values are preserved; no decorative demo numbers are inserted into production UI.
- Background tracking, playback duration accounting, OAuth/Drive configuration and imported history remain untouched by visual-only work unless independently proven defective.
- APK build/publish remain opt-in; a new release must not be created as a side effect of this audit.

## Files reviewed

- Home: `HomeScreen.kt`, `HomeListeningDashboard.kt`, `HomeViewModel.kt`, `WeeklyListeningChart.kt`, `HomeComponents.kt`, `TodaysListenWidget.kt`, `TodaysOverviewSheet.kt`.
- Navigation: `TempoBottomNavigation.kt`, `AppNavigation.kt`.
- Appearance/shared: `AppearanceScreen.kt`, `AppearancePalette.kt`, `AppearanceSettings.kt`, `AppearanceTypography.kt`, `Theme.kt`, `GlassCard.kt`, `LiveNowPlayingCard.kt`, `DeepOceanBackground.kt`, `TempoDialogSurface.kt`, `ArtAtmosphere.kt`.
- App surfaces: `StatsScreen.kt`, `HistoryScreen.kt`, `SettingsScreen.kt`, `ProfileScreen.kt`, `SongDetailsScreen.kt`, `ArtistDetailsScreen.kt`, `AlbumDetailsScreen.kt`, `SpotlightScreen.kt`, `SpotlightStoryScreen.kt`, `ShareCanvasScreen.kt`, `WelcomeScreen.kt`, `HowItWorksScreen.kt`.
- QA: existing appearance palette tests, Android build workflows and screen-free unit/emulator test coverage.

**This report does not certify the APK's visual quality. It explains why the existing process failed to do so.**
