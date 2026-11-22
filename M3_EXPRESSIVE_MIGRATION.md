# Tempo — application-wide Material 3 Expressive migration

**Audit date:** 21 November 2026 · **Target:** Tempo `main` · **Reference:** [Orion Store](https://github.com/RookieEnough/Orion-Store) (`main`, Git tree `e561e91e9409d9d9b437ab7acf919cd115769d7e`).
This is an implementation contract, not a promise that every screen has already migrated.

## Purpose and boundaries

The goal is an **independently designed music-listening app with the same degree of visual cohesion and expressive interaction as Orion Store**. Do not transplant Orion's implementations, vector assets, fonts, gradients, animations, texts, brand identity or code: its [LICENSE](https://github.com/RookieEnough/Orion-Store/blob/main/LICENSE) is GPL-3.0, while Tempo has different licensing. General Material API choices and interaction principles are not proprietary artwork. Tempo's data model, tracking accuracy, user customizations and music-first experience remain authoritative.

The request is not just to add polygons or an expressive Settings hub. The same color roles, adaptive spacing, type scale, page scaffolds, navigation, segmented rows, dialogs, loading states, bottom sheets, focus/gesture affordances, and opt-in motion must cover all user-visible flows, including the extension and widgets where Android Material3 cannot run directly.

## Material 3 Expressive skill migration — cross-app foundation pass (21 November 2026)

**Basis:** the installed `m3-expressive` skill, including its current component-selection, containment, navigation, layout, motion, color, input, and feedback references. In particular, use native `MaterialExpressiveTheme`, tone-based surfaces, `onSurface/onSurfaceVariant` text roles, 48dp interactive targets, **spatial** springs for motion of elements, **effects** springs for color/opacity, and M3's named component semantics instead of custom fake controls. These are the Material rules. **Tempo-specific decisions** (not official M3 geometry): 360dp as the breakpoint for stacked dialog buttons, ≥1.3× font scale forcing full width, five muted accent tile themes, and the choice to keep music artwork visually editorial.

### Code committed on main in this cross-app pass

| Scope | Applied migration | Protected behaviors |
| --- | --- | --- |
| Default cards across the app | `GlassCard` now routes **unaccented** High and Low Prominence cards through native Material tonal `Surface` with semantic text colors, instead of expensive shadow/gradient/border stacks. Explicit art-led cards retain their intentional treatments | Card contents, specialized artwork, all callbacks and backing data |
| App bars, labels and metrics | Shared `FrostedIconButton` uses a native actionable 48dp Material surface. Section kicker and telemetry cells use semantic `outlineVariant`/type roles; numeric data allows two lines | Existing icon actions and labels |
| Dialogs and forms | `TempoDialogSurface` uses one bounded tonal Surface, semantic primary/danger buttons, Material TextField states and shape tokens. Two-action dialogs adapt from side-by-side to full-width stacked actions based on width and font scale; pure policy + unit test added | Existing confirm/dismiss callbacks; enabled and error states |
| Empty states | Both default empty-state layouts use native Material surfaces; potentially misleading “tracking active” replaced by neutral waiting text; perpetual radar animation removed | Supported-apps action and period-specific messages |
| Material tiles/bento | Metric tiles use actual clickable Material surfaces when actions are present; abstract shapes appear on icons rather than behind the data text. Settings bento retains its expressive and accessible adaptive navigation but avoids oversized ornamental backing shapes | Actual metrics, button actions, appearance density |
| Stats and history | Stats search expands via spatial springs with effects fade. History filter sheet uses native Material drag handle and Material sheet shape | History pagination, search, filter state, dialogs |
| Daily Listening and music details | Daily Listening section reveals no longer stagger, its sheet animation separates spatial movement and effects opacity. Song, Artist, Album collapsed headers use Material schemes | Live playback, audio preview, chart data, page actions |
| Connection and import journeys | Spotify, Apple, Deezer, Musicolet, Last.fm, YouTube Music and desktop routes inherit a single app background, instead of duplicate full-screen overdraw. Semantic supporting text no longer attenuates contrast unnecessarily | Import/restore state machines, file selection, refresh/cancel/undo |
| Onboarding and restore | Privacy, permission, battery and how-it-works copy uses theme roles. The restore accordion uses M3 spatial/effects springs, its Google/local backup and Last.fm options use readable theme-aware controls, with the existing operations unchanged | Permission requests, backup restore, conflict prompts, auth callbacks |
| Cover-art picker and Spotlight | Native Material bottom-sheet handle and tonal styling in cover picker; Spotlight overlays use Material transitions, one background, and native themed loading/empty states | Media artwork selection, story cards, exports and content |
| Android widgets | Glance Light/Dark ColorProviders now map semantic primary/secondary/tertiary and surface container roles, and weekly widget no longer hardcodes a dark background | Widget open actions, stored metrics and refresh schedule |
| Browser extension popup | Web-native equivalent semantic roles, accessible keyboard focus and 48px navigation tab targets; removed perpetual Pair CTA sheen/pulse so responsiveness does not depend on looping effects. Source CSS only; bundled `dist/` needs its usual build at release time | Extension tracking, pairing, connection status, security and existing popup JS |

**Source-level safety checks:** the modified Kotlin files have had delimiter/duplicate-import checks (still not an Android compiler). The pure dialog policy test is committed but not run. No CI, Gradle build, emulator/instrumented test, APK or GitHub release was started in this pass.

### Remaining work — necessary before claiming “everything upgraded”

- **Visual validation:** compile, run unit/instrumentation tests and screenshot comparisons in Light/Dark/OLED/custom, narrow/expanded widths and larger typography; verify 48dp touch targets, TalkBack, keyboard/IME, gestures, back stack and system-inset behavior. In particular, test older Android widgets and the 12+ splash.
- **Editorial media:** Spotlight story stages, `ShareCards.kt`, `ShareTheme.kt`, share canvas/export rendering and purpose-built badge artwork intentionally use explicit colors and backgrounds. Inspect each preview against exported content before changing them to Material roles; a global color replace would corrupt users' shared image styles.
- **Remaining specific layouts:** finish individual song/artist/album sections, Profile badges and gamification displays, remaining advanced settings flows, loading/error states and expanded-screen navigation beyond the shared primitives. Iterate screen-by-screen with screenshots.
- **Cross-platform companion:** browser-extension popup source now has Material-inspired role aliases and lower-motion CTA feedback; its remaining dark-only custom screen styles still need deliberate contrast review, real browser smoke tests, and a normal bundled `dist/` regeneration when building the next release.
- **Performance acceptance:** compare first meaningful frame, startup/Now Playing state updates, p50/p95 frame pacing on scrolling stats/history, artwork requests, memory and background tracking health against last tested build.

A complete app-wide visual redesign is **not yet verified or signed off**. Keep [#94](https://github.com/matchalattewithbanana/Tempo/issues/94) open until this matrix and the remaining art-heavy surfaces are validated.

## Follow-up — full skill-guided component and performance pass (21 November 2026)

This pass read and applied the installed `m3-expressive` skill's `tokens.md`,
`shape.md`, `motion.md`, `typography.md`, `color.md`, `layout.md`,
`platforms.md`, and component guidance for buttons, navigation, inputs,
containment, bars, menus and feedback. **Using the complete guidance does not
mean showing every Material component in Tempo**: choose components by user
intent, and avoid meaningless decorative density.

| M3 rule | Tempo implementation | Important distinction |
| --- | --- | --- |
| 10-step corner system (0/4/8/12/16/20/28/32/48/full) | All SOFT/MODERATE/SHARP `AppearanceMaterialShapes` choices now map to published radius steps. Selected preset cards morph between 20 and 28dp and their preview uses nested optical roundness | M3 supplies the **steps**; Tempo chooses which step each customization preset maps to |
| Connected groups for small single-choice sets; filter chips for more than five | Appearance controls now measure **actual pane width**, present selected check marks, use full-row switch semantics and switch six-option duration controls to filter chips | Tempo's 360dp/grouping heuristic remains a product layout choice, not a Material default |
| Baseline and emphasized type roles are different *weights*, not unrelated font families | Selected headings and menu items opt into emphasized roles; alternate heading fonts no longer leak into `titleMedium/SmallEmphasized` and body controls | Font selection and brand typography remain customizable |
| Purposeful motion, geometry→spatial and opacity→effects | Appearance accordion height uses spatial springs and opacity uses effects springs; insight card crossfade uses effects springs. Experimental Material APIs now have explicit opt-ins throughout affected routes | No rule mandates stagger animations; Tempo favors immediate content |
| Limit expressive hero moments, especially on dense analytics | Eliminated 16+18+14 independent decorative wave animations plus continuous radar/flame loops in Home insights; static bars are drawn without 48 graphics layers. The selected BPM pulse remains the intentional rhythmic hero | A static data-shaped history visualization is not represented as live playback |
| Inverse-surface snackbar and single action | `TempoSnackbar` now uses `inverseSurface/inverseOnSurface/inversePrimary`, omits status-icon decoration and keeps optional dismissal | Message classification remains for existing APIs, not for styling a second hero |
| Tonal vertical menus with explicit selected state | Shared menus now use Material surface roles, emphasized selected title, checked indicator and selection semantics; extra border/shadow layers removed | Temporary action menus are not a replacement for always-visible toolbars |
| One clearly high-emphasis destination | Settings bento keeps one colorful featured card; secondary destinations use calm tonal surfaces with colorful emblem icons | Color accents belong to informative hierarchy, not every card |
| Adaptive navigation and persistent Now Playing | Expanded route now reserves **measured rail width** and Now Playing strip height rather than relying only on a fixed 96dp inset; supporting labels use paired Material text roles | The initial 96dp width is a Tempo pre-measure fallback, not an official rail token |
| Semantic text pairs for secondary flows | Extension import button foregrounds use `onPrimary`; supported-app divider treatment uses `outlineVariant`; Settings search now has labeled clear action, search keyboard action and indexes more destinations | App-specific content and callbacks remain unchanged |
| Dialogs stack confirmations over dismissals | Corrected confirmation order for narrow/high-font dialog layouts and switched title hierarchy to headline | The responsive breakpoint remains Tempo policy |

**Changes are source-only** on `main`, committed with `[skip ci]`. Regression
tests were added or updated for shape presets, responsive connected-choice
selection, and readable typography roles. Source-level brace/delimiter and
duplicate-import checks passed for modified high-risk files; **neither Gradle
compile nor unit/instrumentation tests have run**. Do not release this work
without the existing acceptance matrix.

**Follow-up acceptance gates:** build validation, crash-free navigation
and restore journeys, compact/expanded layouts, extreme system font sizes,
TalkBack/focus behavior, Light/OLED/custom color screenshots, profiler traces,
and pixel-accurate share/export regression tests. Specialist artwork and export
surfaces should not be recolored mechanically. `m3-expressive` guidance
is a design/implementation reference, not a repository compiler or test runner.

## Reference code inspected

| Orion module | What it actually does | Original Tempo adaptation |
|---|---|---|
| [`Theme.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Theme.kt) and [`Palettes.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Palettes.kt) | A single `MaterialExpressiveTheme`, remembered light/dark/dusk/OLED/custom color schemes; system status/navigation bars follow the effective app color scheme; all six motion spec families snap when motion is disabled; custom font typography includes emphasized roles | Keep Tempo's already-present `TempoTheme`, `AppearancePaletteResolver`, multi-seed maker and bundled type; consolidate remaining hardcoded legacy tokens into native `MaterialTheme.colorScheme`; audit Light/OLED contrast and themed system bars |
| [`Common.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Common.kt) | Shared section hierarchy, empty state, read-width policy, sheets, pressure/focus affordances, shape morphs and grouped row shapes | Build on Tempo's existing `TempoEmblem`, `TempoSectionHeading`, `TempoDialogSurface`, `EmptyState`, `SettingsComponents`; keep all normal cards on tonal surfaces and only use artwork/glass when it helps identify music |
| [`Root.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Root.kt) | Navigation changes with available width and configuration: rail, bar, custom dock and expressive floating toolbar, all with selected-state semantics | Retain navigation destinations and saved back stack; cap dock footprint; support top/rail/floating options via a verified layout policy; reserve actual bar + optional now-playing height; never cover content |
| [`Settings.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Settings.kt) | Home hub with featured and compact configurable bento destinations, coherent detail pages, segmented rows, palette/layout previews and import settings | Keep Tempo's compact featured Appearance hub, use a small page context strip, logically grouped `SettingsOption`/`SettingsSwitch`, stable pinned search and shorter diagnostic cards; **no repeated header titles or overlapping controls** |
| [`PaletteMaker.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/PaletteMaker.kt) | Hue/chroma/tone-driven custom palettes and immediate light/dark/dusk/OLED previews | Expand Tempo's existing multi-seed `CustomPaletteMaker` previews and editable preset controls, using Tempo's own color algorithm; preserve existing saved looks and backup serialization |
| [`StoreScreen.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/StoreScreen.kt) | Adaptive tile/list layouts, category shapes, stable-geometry placeholders and selected state, Material motion specs | For listening lists, use stable compact/list and art-rich/grid layouts, artwork-preserving skeletons, meaningful selection state, and no forced reveal when data is ready |
| [`Detail.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Detail.kt) | Adaptive single/two-pane detail, segmented action groups, reusable version/detail sheets and artwork identity | Redesign Song, Artist and Album pages around real cover art, clear actions and native grouped metadata, respecting existing navigation/actions, especially 600–1200dp |
| [`Sheets.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Sheets.kt) and [`Common.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Common.kt) | Unified bottom sheet lead blocks, icon+headline hierarchy, bounded width, collection pickers and loading states | One Tempo modal/sheet policy for import/export, restore confirmation, supported app picker, cover art picker, share editor and details dialogs; ensure IME, TalkBack and 1.5× fonts work |
| [`MyApps.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/MyApps.kt) and [`Swipe.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Swipe.kt) | List selection, grouped surfaces, swipe actions and animated placement | Translate to History/library lists, with accessible non-swipe alternatives, undo for destructive edits, stable list keys, and reduced-motion gates |
| [`Pixel.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Pixel.kt), [`About.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/About.kt) and [`Guide.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Guide.kt) | Distinctive occasional character/shape interactions, feature explanation and accessible onboarding routes | Consider **optional** music-themed refresh/empty-state illustrations and richer onboarding/help; no endless animation, network assets or novelty that delays reading content |
| [`Community.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Community.kt), [`OrbitScreen.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/OrbitScreen.kt) and [`Xtra.kt`](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Xtra.kt) | Consistent repeated semantics for complex, nested tools and responsive panels | Apply the same surface/feedback system to Profile, badges, backup history, advanced tracking settings, analytics and data-repair flows rather than leaving utility screens visually different |

**Existing Tempo foundation, already in code:** `TempoTheme` uses `MaterialExpressiveTheme`, `MotionScheme.expressive()` and an instant-motion fallback; `AppearanceMaterialShapes`, `AppearanceTypography`, `AppearancePaletteResolver`, `CustomPaletteMaker`, `TempoEmblem`, `TempoSectionHeading`, `ListeningSkeletons`, a category bento and adaptive nav policy already exist. Do **not** re-implement these from scratch.

## Orion screenshot follow-up — profile and category bento (21 November 2026)

The reference screenshots show a soft light background, vivid pastel asymmetrical tiles, a distinct identity card, concise quick controls and a badge-collection panel. For Tempo the underlying product is music discovery and analysis, not app inventory: the equivalent priorities are listening period totals, top music, streaks, XP, artist/track stories and saved listening identity.

A new **original Tempo tonal tile system** now supplies five light/dark/OLED-aware multi-accent treatments (Aqua, Sky, Lilac, Rose, Amber) whose text color remains the semantic `onSurface` role. It uses Material polygon icons and restrained background marks; nothing was copied from Orion's GPL UI source.

- `TempoExpressiveTiles.kt`: reusable adaptive metric cards; compact-density preference respected.
- `SettingsBento.kt`: featured full-width Appearance tile; on 360dp+ and normal type, one tall category beside two compact categories, followed by paired categories; below 360dp or at 1.3×+ type, a full-width list. Distinct category colors and music tracking moved ahead of general settings. All cards remain real navigation actions.
- `ProfileScreen.kt`: listener identity is now a soft tonal 'Listening story' card; its level progress uses an ordinary Material surface. The compressed dark stat masthead is now a colorful responsive 2×2 bento, with a tappable badge-collection surface showing live progress and opening the existing Badges tab. Streak hiding remains respected.
- `StatsScreen.kt`: the selected period's actual listening time is the lead tile; plays and selected summary counts become responsive side-by-side colored metrics using the existing unit formatter.
- `HomeComponents.kt`: the weekly recap now uses the same coherent pastel language with real artwork and metrics.
- `SettingsBentoLayoutTest.kt`: updated pure threshold tests. No instrumentation run.

These are **source-level changes only**. A fresh Android build, screenshot testing on 320/360/393/412dp and high-font / light / OLED variants, and memory/frame-time checks remain mandatory. Do not equate committed UI with verified visual quality.

## Opening, transitions and Now Playing (21 November 2026)

This pass uses the user-provided Orion Store ZIP as **read-only reference**: `OrionActivity.kt` configures the system splash and animates its exit, while `Root.kt` uses separate motion patterns for navigation and overlays. Tempo implements its own, simpler behavior to avoid slowing the main music experience.

### Source changes on `main`

- **Android 12+ launch:** `values-v31/themes.xml` supplies the Tempo icon as the native system splash mark. `MainActivity` fades the native splash out with a short icon enlargement **after** Android has its first app frame; there is **no timer holding the splash** and no new permission/dependency. When Android system animations are disabled, removal is immediate. The 220ms View-property exit is a **Tempo design decision**, not an official Material spring token. Older Android versions retain their existing standard window launch behavior.
- **Navigation:** `AppNavigation` replaces fixed 120ms fades with the active `MaterialTheme.motionScheme`. Peer Home/Stats/History routes use effects-only transitions; entering a detail route uses a small forward slide on a spatial spring plus an effects fade; popping reverses direction. Both the app's instant navigation setting and reduced-motion policy retain nonanimated transitions. No new routes, destination callbacks or bottom-bar logic.
- **Onboarding:** `AnimatedOpacity` no longer staggers useful content with an artificial per-item delay; opacity uses an effects spring, and the short upward arrival uses a spatial spring. The step navigator uses spatial/effects springs in both directions, **but hands off to Home immediately** rather than fading first-screen content away again.
- **Live Now Playing:** `LiveNowPlayingCard` is a Material expressive card, with theme-aware `primaryContainer` when actively playing, `surfaceContainerLow` otherwise, a decorative Material shape, animated status icon, strong artwork and typography hierarchy, and a spatial resize on state changes. Status distinguishes **playing, paused, stale last-detected and idle**. No new player controls are implied; Spotify broadcasts, MediaSession art priority, API verification labels and the separate 1Hz live estimate are preserved.
- **Compact Now Playing:** `CompactNowPlayingStrip` now uses a Material `Surface`, native color roles and a small expressive state icon instead of a hardcoded bordered glass row. The once-per-minute freshness check now runs only while the app is STARTED; state changes animate with a Material effects spring. User-selected inline/compact/hidden policy is unchanged.
- **Localization:** new English base strings describe Now Playing state and album art; other locales temporarily receive Android's resource fallback until explicitly translated.

### Acceptance gates (not yet run)

- [ ] Android 12+ cold/warm start: icon visible, splash removed after first frame, no black flash on re-entry, no double animation, no delay added to Home; verify API 26–30 fallback launch.
- [ ] Set **system animator duration to Off**, plus Tempo **Reduce motion** and **Instant navigation** separately; platform splash exit must respect system Off, and in-app transitions must snap in both app modes. Verify known limitation: native pre-Compose exit does not yet consult app-only Reduce motion (it is read asynchronously from DataStore).
- [ ] Rapid Home ⇄ Stats ⇄ History taps, deep Song/Artist/Album routes, system predictive back, keyboard and tablet rail: no duplicate back-stack route, content flashing, or bottom-bar overlap.
- [ ] Now Playing: stopped/idle, playing, paused, stale 2–15 minutes, expired at 15 minutes, local MediaSession art, Spotify art fallback, missing artwork, source labels and stale API verification; no extra remote fetches.
- [ ] Full + compact layouts, 320/360/393/412dp, Light/Dark/OLED/custom colors, font scaling 1.3×/1.5×/2.0×, TalkBack announcements, contrast and touch semantics.
- [ ] Android compilation, app smoke tests, and frame/memory profiling with active playback. Do not call the changes verified until these pass.

The M3 rules used are spatial springs for movement/size and effects springs for opacity/color. Applied choreography and launch timing are Tempo choices, not specified M3 values. A music app benefits from a rare launch/Now Playing hero moment; repeated ornamental animations on every analytics card are deliberately avoided.

## Component contract — apply throughout the app

1. **Color**: text and controls use native semantic roles (`surfaceContainerLow/High`, `onSurface`, `onSurfaceVariant`, `primaryContainer`, `onPrimaryContainer`, etc.). Light, Dark, OLED and wallpaper/custom schemes are first-class. Artwork-derived accents must pass contrast checks and must not change functional semantics.
2. **Surfaces**: standard data cards use Material `Surface` / `Card`, not a custom gradient-border-glass stack. An explicit specialized music/Spotlight artwork card may retain tinted glass, but not entire Settings, forms or lists. Use one opaque app backdrop, not nested full-screen gradients.
3. **Typography**: emphasized hierarchy for meaningful titles, concise labels, body text for supporting descriptions. No forced `maxLines=1` for dynamic durations, song titles, numbers, translated text or narrow metrics. Shared 1.0×, 1.3×, 1.5× and 2.0× layout rules.
4. **Shape and density**: user-selected `AppearanceMaterialShapes` controls normal component shapes. Polygons identify an area or action, not every decorative background. Use compact grouped/segmented rows for dense settings, library, report and details. Preserve a valid tap target ≥48dp where actions exist.
5. **Navigation**: Home, Stats and History should be compact and stable insets-safe; expanded widths use an actual rail, never giant bars. Avoid app bars that duplicate a separate hero title. Optional floating dock needs a dedicated acceptance suite before enabling.
6. **Motion**: first data frame is immediate; no mandatory stagger, full-screen opacity gate or endless transition. Use `MaterialTheme.motionScheme` on short, user-initiated state changes; follow `LocalAppearanceRuntimePolicy`, system animation disable, reduced motion and power/thermal constraints.
7. **Data experience**: loading skeletons keep final geometry without hiding content, errors show retriable context, empty states explain the next action, success feedback is visible and saved filters/settings re-render instantly. Preserve accessibility and state across configuration changes.
8. **Art**: music artwork, editorial Spotlight stories and share canvases remain visually distinctive; do not flatten their important content into generic UI. Custom imagery and share output require separate light/dark contrast and screenshot checks.
9. **Performance**: avoiding background overdraw and repeated list recomposition is as important as styling. Remember immutable palettes/brushes, use stable item keys, thumbnail-sized artwork, scoped playback timers, no per-frame bitmap or network work.
10. **Localization, privacy and compatibility**: all new text belongs in string/plural resources, preserve database IDs and exported format, and retain undo and destructive-action confirmation. Do not copy GPL UI source.

## Migration scope and acceptance board

| Workstream | Tempo code areas | Minimum acceptance |
|---|---|---|
| Shared foundation | `ui/theme/`, `ui/components/`, `ui/effects/`, `ui/appearance/` | Theme roles and shape/density propagate to cards, dialogs, navigation and widgets; reduced motion truly snaps |
| Shell, Home and playback | `ui/navigation/`, `ui/home/`, `LiveNowPlayingCard` | Music begins immediately; navigation never obscures scroll; active/paused/idle states; predictable top/search/dock |
| Charts and analytics | `ui/stats/`, `ui/home/components/`, `ui/spotlight/` | Full durations, real unit changes, readable charts, 48dp accessible non-gesture alternatives and stable art/card exports |
| History and collection | `ui/history/`, details, collections, merges | Grouped rows, search+filters, correct counts, non-swipe actions, restore/back handling |
| Details | `ui/details/SongDetailsScreen.kt`, `ArtistDetailsScreen.kt`, `AlbumDetailsScreen.kt` and cover picker | Original artwork-led heroes, adaptive 1–2 pane, grouped metadata, no phantom network activity |
| Profile and gamification | `ui/profile/` | Single consistent surface hierarchy, badge identity, readable stats and customizable layout; avoid dark-only text |
| Settings and appearance | `ui/settings/`, `ui/appearance/`, branding, permissions | Search, app management, navigation, language, privacy, release options and previews work at all sizes |
| Imports/backup/onboarding | Spotify, Apple, Deezer, YouTube, Last.fm, Musicolet, desktop, backup and restore screens | One progress/error/undo/sheet system; no background work lost, scrollable large-font dialogs, accurate status |
| Share and Spotlight | `ui/spotlight/`, `ui/components/ShareCards.kt`, `ShareTheme.kt` | Layout choices and preset previews reflect theme; exported image and onscreen preview match |
| Widgets and browser extension | `widget/`, `extension/` (host-specific UI technology) | Consistent semantic colors/typography, action density and accessibility without falsely claiming native Compose M3E |
| QA and documentation | `app/src/test/`, `app/src/androidTest/`, `docs/` | Compiles, passes automated tests, screenshot matrix, measured frame pacing, no active regressions |

## Source changes in the first migration slice

- `GlassCard.kt`: unaccented `LowProminence` now uses one native Material tonal `Surface`; custom artwork, accent, and explicit background cards keep their specialized implementation for deliberate review. Reduces border/gradient/draw work.
- `HomeComponents.kt`: weekly listening digest now uses adaptive theme-tonal cards and text roles, with a small-screen fallback, and Home discovery/habit cards stop hardcoding white text and unrelated translucent fills.
- `LiveNowPlayingCard.kt`: an ordinary themed surface, still maintaining its isolated playback ticker.
- `CommonComponents.kt`: normal `StatCard` begins fully visible and only performs an entrance transition if the existing motion policy explicitly allows it.
- `WeekDigestLayout.kt` and unit test: responsive single/two-column layout policy including large type.
- `StatsScreen.kt`: ranked lists use native clickable Material surfaces, semantic color roles, two-line song/artist labels and a full-width duration line rather than glass/inner shadow stacks.
- `HistoryScreen.kt`: regular and archive rows use the new unaccented tonal path and native outline roles.
- `SongDetailsScreen.kt`, `ArtistDetailsScreen.kt`, `AlbumDetailsScreen.kt`: seven nested full-screen app backdrops removed; art-led content retains its separate overlays.
- `ProfileScreen.kt`: level, challenge and badge progress now use the centralized expressive motion scheme (including instant reduced-motion behavior) instead of fixed 900–1200ms tweens.
- `AppearanceScreen.kt`, `SupportedAppsScreen.kt` and Settings detail pages: shorter native headers and one root app backdrop.


Earlier screenshot corrections are in [`MATERIAL3_EXPRESSIVE_ROADMAP.md`](MATERIAL3_EXPRESSIVE_ROADMAP.md) and [#93](https://github.com/matchalattewithbanana/Tempo/issues/93).

### No-build verification and release gate

These edits are committed to `main` under `[skip ci]` but **not compiled or device-rendered**. Visual acceptance requires actual before/after screenshots at 320/360/393/412dp, 840dp, 1200dp, Light/Dark/OLED/custom/wallpaper palettes, 1.0×/1.3×/1.5×/2.0× type, TalkBack, compact/expanded navigation, keyboard, portrait/landscape, active playback/paused/idle, empty/loading/error, live time units and full backup/import flows.

Before any release: validate no UI overlap/ellipsis of critical metrics, no clipped buttons, accessible search/filter/switch semantics, correct bar/IME insets, preserved local data, no tracking or permission regressions. Baseline startup time, first meaningful draw, scroll frame time (p50/p95) and memory against the previous APK. A static delimiter balance is **not** compilation evidence. No CI, build, artifact or publication is authorized merely by this migration plan.

**Status: first cross-app foundation slice implemented; full conversion is not yet complete.**
