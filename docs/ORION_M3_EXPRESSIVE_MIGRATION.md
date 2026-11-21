# Tempo — application-wide Material 3 Expressive migration

**Audit date:** 21 November 2026 · **Target:** Tempo `main` · **Reference:** [Orion Store](https://github.com/RookieEnough/Orion-Store) (`main`, Git tree `e561e91e9409d9d9b437ab7acf919cd115769d7e`).
This is an implementation contract, not a promise that every screen has already migrated.

## Purpose and boundaries

The goal is an **independently designed music-listening app with the same degree of visual cohesion and expressive interaction as Orion Store**. Do not transplant Orion's implementations, vector assets, fonts, gradients, animations, texts, brand identity or code: its [LICENSE](https://github.com/RookieEnough/Orion-Store/blob/main/LICENSE) is GPL-3.0, while Tempo has different licensing. General Material API choices and interaction principles are not proprietary artwork. Tempo's data model, tracking accuracy, user customizations and music-first experience remain authoritative.

The request is not just to add polygons or an expressive Settings hub. The same color roles, adaptive spacing, type scale, page scaffolds, navigation, segmented rows, dialogs, loading states, bottom sheets, focus/gesture affordances, and opt-in motion must cover all user-visible flows, including the extension and widgets where Android Material3 cannot run directly.

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
