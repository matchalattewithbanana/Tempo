# Tempo's Expressive visual direction — Orion Store reference

This is an **independent Tempo implementation** informed by a read-only review of [Orion Store's Compose UI](https://github.com/RookieEnough/Orion-Store/tree/main/app/src/main/java/com/orion/store/ui) and its [screenshot gallery](https://github.com/RookieEnough/Orion-Store#screenshots).

## What the reference does well

The visual appeal comes from a coherent system, not a single animation or gradient:

- **A distinct visual rhythm:** oversized editorial titles, concise supporting text and consistent horizontal insets.
- **Recognizable identity:** native Material shape families (sunny, cookie, clover, soft-burst) are used in a limited number of identity or empty-state surfaces, not behind every icon.
- **Layering with purpose:** tonal surface roles distinguish action, grouping and metadata without stacking glass effects and long shadows.
- **Actions that feel alive:** actual Material3 buttons, focus treatment, native ripples, connected choices and motion-scheme transitions.
- **Adaptation:** width-constrained readable content and different navigation placement on large windows.
- **Quiet performance:** static decorative artwork, cached data and restrained motion in long scrolling lists.

Sources reviewed: [Common.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Common.kt), [Root.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Root.kt), [StoreScreen.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/StoreScreen.kt), [Sheets.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Sheets.kt), [Theme.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Theme.kt).

## Tempo implementation

| Tempo area | Independent adaptation | Why it matters |
| --- | --- | --- |
| Listening insights | `TempoSectionHeading` + `TempoEmblem` | An instantly recognizable, consistent editorial hierarchy |
| Setup, empty Stats and quest states | Static Material polygon icon containers | More personality without periodic animation or network assets |
| Spotlight Story | Cached theme-keyed tonal overlay and prominent action glyph | Colors really respond to theme, including Light and OLED |
| User header and level ring | Dynamic Material containers and gradient color roles | Removes fixed white/pink styling and a 10dp shadow on every Home visit |
| Settings overview | `SettingsCategoryTile`: five distinct tactile navigation destinations | Easier scanning and consistent accessibility/ripple targets |
| Home, Stats and History timelines | Centered widths of 920–960dp on wider windows | Readable content rather than overextended full-width cards |
| Summary and quick-stat cards | Appearance-controlled corners, emphasized metrics and text hierarchy | Respects customization, sharper visual rhythm |
| Statistics top bar | `CenterAlignedTopAppBar` | Native balanced title without spacer hacks or a scroll shadow |

The existing expressive motion policy, source-detection controls, chart components, music artwork and accessibility preferences remain authoritative. This pass adds **no new API calls, tracking events, playback state or permissions**.

**Design reference is not a source transplant.** Orion Store's repository is GPL-3.0. Tempo has a different, custom source license. Its code and artwork were not copied into Tempo; only broadly applicable interface ideas and public Material3 APIs informed this original implementation. Respect both projects' license notices.

## Before shipping

This work has been committed to `main` without running Gradle, building an APK, or publishing. Static brace balance and string-resource checks do **not** establish compilation correctness.

Device review must include:
- Light, Dark, OLED, wallpaper-based color, and representative warm/cool Appearance Studio palettes.
- 320/360/412dp phones, 840dp navigation-rail breakpoint, and 1200dp tablet widths.
- 1.0×, 1.3× and 1.5× font scales, TalkBack, contrast and tappable controls.
- Home transition / scroll with active playback, no-notification state, empty statistics and numerous insights.
- Settings hub tap/focus targets, orientation changes, back navigation and existing search.
- Memory and frame-time comparison on the Home, Stats and History scrolling traces before claiming any measured performance improvement.

## Deeper audit of Orion's native UI (November 2026)

The review covered `Root.kt`, `Theme.kt`, `Common.kt`, `Settings.kt`, `PaletteMaker.kt`, `StoreScreen.kt`, `Detail.kt`, `Sheets.kt`, `Guide.kt`, `About.kt`, `Community.kt`, `MyApps.kt`, `Swipe.kt`, `Pixel.kt`, and relevant screens. Each technique below was checked in Orion's own source, not inferred from a screenshot.

| Reference pattern | Actual source | Independent Tempo decision |
| --- | --- | --- |
| Bento with a featured entry and compact secondary destinations | [Settings.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Settings.kt) `Hub` / `Bento` | Introduce `TempoSettingsBento` using existing destination callbacks, a real Appearance Studio hero, tonal tiles and 1-column fallback |
| Category-specific landing headers with native material polygons | `Settings.kt` `Page` | Add `TempoSettingsCategoryHero` to each existing Settings category with no extra navigation stack |
| Systematic motion with immediate accessibility fallback | [Theme.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Theme.kt) `InstantMotion` | Implement Tempo's own six-spec instant motion scheme; standard motion is **not** the same as zero motion |
| Click response that respects touch and keyboard focus | [Common.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Common.kt) `springClick` | Upgrade `premiumClickable` to fast Material spatial specs, ripple and focus response; skip scaling in reduced-motion mode |
| Source-consistent typography across all expressive styles | `Theme.kt` `typography` | Fill in remaining emphasized type roles and custom font transformations; opt into Material APIs in Gradle |
| Loading that preserves the next screen's layout | [StoreScreen.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/StoreScreen.kt) `StoreSkeleton` | Add static, accessible, music-specific skeletons for initial Home, Stats and History loads |
| Palette editor with live scheme preview | [PaletteMaker.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/PaletteMaker.kt) | Improve Tempo's already-present 18 theme previews via selected shape motion and music-shaped preview icons. A multi-seed palette editor needs a separate persisted-settings specification |
| Segmented list shapes and animated expandable content | [Sheets.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Sheets.kt) | Add short expressive transitions to Appearance Studio's expanding sections; keep long data lists static for responsiveness |
| Rich content detail and swipe affordances | [Detail.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Detail.kt), [Swipe.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Swipe.kt) | Consider art-led Song/Artist/Album pages and optional accessible swipe actions only after device UX testing |
| Multiple dock styles with visible labels | [Root.kt](https://github.com/RookieEnough/Orion-Store/blob/main/app/src/main/java/com/orion/store/ui/Root.kt) `FloatingNav` | Keep native short navigation / rail now; optional configurable floating dock requires insets, gesture and compact Now Playing validation |

### Current implementation safeguards

- **No Orion code or artwork was ported.** Orion's GPLv3 licensing and Tempo's custom licensing are not interchangeable.
- The new bento and category heroes use Material color roles and static shapes, not hardcoded gradients, periodic effects or image assets.
- Non-animated placeholders display only for empty, initial states; existing content and playback behavior remain unchanged.
- `MaterialTheme.motionScheme.fastSpatialSpec()` now drives brief selection feedback. All six Material animation families snap under Tempo's reduced-motion policy.
- Settings pages and Appearance Studio keep their existing routes, persisted settings and back-navigation.
- The Settings bento uses one column below 370dp or at font scales of 1.3× and higher; the pure breakpoint policy has new regression tests.

### Known validation gap

Changes are committed to `main` **without compiling, running instrumentation, or checking pixel output on an Android device**. Material API opt-ins make source intent explicit; they do not certify the dependency/toolchain combination. Device QA should inspect the bento at 320dp/370dp, 1.3× fonts and OLED; check theme previews, active-player scrolling, skeleton state transitions, and whether TalkBack correctly describes icon-only elements.

## Opt-in expressive extras implemented

Following the deeper Orion source audit, Tempo now offers **Effects Studio** in Appearance Studio. Ambient animation, gravity-based card tilt and item entrances are separately opt-in and always subject to the Android accessibility/power/thermal/low-RAM/foreground policy. The default presentation remains deliberately static. The floating navigation toolbar is also optional; normal short navigation remains the default, and wide rail navigation retains priority on large layouts.

Artwork-led Song/Artist/Album headers can respond to optional tilt and ambient accents; the Album, Artist and Profile collections use the same opt-in short item entrance pattern as History and Stats. Backup/restore gained a native expressive header and unified loading indicators. The custom palette maker supports three seeds with accessible contrast adjustment and previews for each display tone.

These are original Tempo implementations using Android/Compose APIs, not a source transfer from Orion Store. No device captures or benchmarking results are available yet.

## Next visual milestones

Retune the remaining visual duplicates on track/artist/album details, import/restore flows and Spotlight story editor. Choose deliberately where a shape or action morph helps recognition; do not add permanent shimmer, blur or animated gradients merely to look expressive.
