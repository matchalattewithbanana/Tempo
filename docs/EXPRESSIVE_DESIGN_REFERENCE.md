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

## Next visual milestones

Retune the remaining visual duplicates on track/artist/album details, import/restore flows and Spotlight story editor. Choose deliberately where a shape or action morph helps recognition; do not add permanent shimmer, blur or animated gradients merely to look expressive.
