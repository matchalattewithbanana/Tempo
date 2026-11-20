# Material 3 Expressive migration — Tempo

**Status:** implementation committed to `main`, **not compiled or device-tested**. No workflows, builds or releases were dispatched.

## Baseline

- Compose BOM `2026.11.01` — Compose 1.13 and stable Material3 1.5.0.
- Android Gradle Plugin `9.5.1`, Gradle `9.8.2`, compile SDK `37.1`, Lifecycle `2.11.0` (required by Compose's current lifecycle/lint integration).
- Android target SDK remains `36`; any change to this is a separate compatibility task.
- `MaterialExpressiveTheme` wraps every Compose screen. This is not a second theme engine.
- Material motion is expressive by default, but `AppearanceRuntimePolicy.reduceMotion` activates a six-spec instantaneous motion scheme for system-disabled animations, reduced-motion preferences, power-save, thermal limits or background/low-resource constraints.

## Component migration

| Area | Expressive component / system | Intent |
| --- | --- | --- |
| App theme | `MaterialExpressiveTheme` + `MotionScheme` | coherent motion and current color/shape/type tokens |
| Main navigation | `ShortNavigationBar` + collapsed `WideNavigationRail` (≥840dp primary windows) | adaptive phone/tablet destinations with shared back stack and compact now-playing strip |
| Home / Stats periods | responsive connected `ToggleButton` groups | 2–5 controls per row based on available width and font scale |
| Stats / Profile tabs | `PrimaryTabRow` and `Tab` | accessible selection without shadow compositing |
| History filters | connected `ToggleButton` group | single-select date range, 48dp minimum tap heights |
| Settings | `ListItem`, native `Switch`, responsive search | consistent text hierarchy and taps |
| Appearance Studio | responsive connected `ToggleButton` groups, expanded shape scale | balanced option rows, avoid redundant apply on selected choice |
| Update Center | `MediumFlexibleTopAppBar`, connected `ToggleButton` group | coherent header and stable/preview channel selection |
| Loading | `CircularWavyProgressIndicator`, `LinearWavyProgressIndicator` | expressive but reduced-motion-safe loading states |
| Stat cards | `MaterialTheme.motionScheme.fastSpatialSpec/fastEffectsSpec` | consistent short entrance effects without bouncy slow springs |
| Settings bento / category hero | Tonal original Tempo surface composition + native static Material shapes | Featured Appearance shortcut, recognizable settings destinations, sensible 1–2 column fallback |
| Initial loading | `TempoHomeLoadingSkeleton`, `TempoStatsLoadingSkeleton`, `TempoHistoryLoadingSkeleton` | Stable layout geometry and a single accessible loading announcement instead of blank space |
| Reduced motion | `TempoInstantMotionScheme` (all six specs snap) | Actual no-motion behavior rather than replacing expressive animations with still-animated standard motion |
| Preset selection | `fastSpatialSpec`, expressive icon tile | Short selected-state corner animation that respects user motion settings |
| Settings subpages | `TempoSettingsCategoryHero` | Consistent screen identity with accessible emphasized titles |
| Shared dialogs | `Surface`, `Button`, `FilledTonalButton` | remove extra gradients/large shadows, follow themed shapes |
| Snackbar feedback | `Snackbar` with native actions | focus/semantics and severity icon, no custom shadow stack |
| Supported Apps search | `TextField` + `TempoLinearLoadingIndicator` | keep results visible while off-main query projection catches up |
| Stats / History search | `TextField` with Material toneroles | reliable IME/focus, accessible clear/close, no custom glass shadow field |
| Load / error / empty states | Material error/secondary containers | readable contrast in Light/Dark/OLED and a native Retry action |

The existing `GlassCard` and music-specific surfaces are retained where they represent song art, statistics and listening history better than generic cards. They already use theme palette, corner and solid-surface policies. Material3 components are used in shared controls, not as a reason to add unrelated UI.

## Visual implementation references

For the independent design review of Orion Store, the specific Tempo surfaces updated, and the remaining device-level visual checks, see [Expressive visual design reference](EXPRESSIVE_DESIGN_REFERENCE.md).

## Opt-in Effects Studio (unreleased)

Under **Appearance Studio → Effects**, three independent options are **off by default**:

- **Ambient animated accents** — two subtle, slowly moving tonal radial fields on main app screens. Animation state is read inside drawing, without per-frame recomposition of lazy music rows.
- **Move featured cards with device tilt** — one lifecycle-bound gravity-sensor listener per visible app theme, never a sensor per list row. `SensorManager.SENSOR_DELAY_UI`, a 50 ms event throttle and low-pass filtering drive only small `graphicsLayer` transforms. It is optional hardware; unsupported devices remain static.
- **Animate list items as they appear** — short, bounded staggered enter transitions for Home highlights, Stats, History, Profile collections and Album/Artist lists. Not a continuous animation per row.

**Gentle / Expressive** intensity and the optional **Short bar / Floating dock** navigation selection are saved in the same versioned appearance settings, backup and saved looks. Existing settings decode with every new effect off; the source-level version is now `AppearanceCodec.VERSION = 5`. The floating dock uses a native `HorizontalFloatingToolbar` on phones; expanded 840dp+ layouts retain their rail.

**Hard safety gates:** Optional ambient/list/tilt effects are denied when animations are system-disabled, Tempo's Reduce Motion is enabled, battery saver is on, the device is low-RAM or thermally limited, or the UI is not foreground. This remains true even if the older `adaptiveEffects` preference is disabled. The sensor is registered only while a supported, enabled app window is RESUMED and is unregistered on backgrounding, disposal, or disabling the option.

The **Custom palette maker** extends the existing primary custom accent with optional secondary and tertiary RGB seeds and Light/Dark/OLED preview cards. Optional roles use surface-aware contrast adjustment. Invalid hex input is not applied, and the full draft only persists on **Apply**. Preset surfaces and listening data remain unchanged.

No new Android permissions, background workers, networking, analytics, or album-art refreshes are added by the effect engine. It has not been compiled or benchmarked on a device.

## Compatibility and performance guardrails

1. No direct datastore or database reads in the composables added by this migration.
2. No extra music metadata requests or background workers.
3. Animate interactions only through standard Material controls; no per-item shadows or continuously running animations in scrolling lists.
4. Honor Android font scaling, screen readers, 48dp touch targets, Light / Dark / OLED themes, and the application's selected corner families.
5. Keep Compact Now Playing independent from navigation animation.
6. No change to existing appearance preference keys, listening records or import data.

## Required validation before a build/release

- Verify Gradle resolves the Android SDK 37.1 platform and all Compose 1.13 dependencies, including Material3 1.5.0.
- Compile all Kotlin source sets; run unit tests, especially `AppearanceMaterialShapesTest`, `ExpressiveLayoutPolicyTest`, `AdaptiveNavigationPolicyTest`, and runtime motion-policy tests.
- Validate the three-tab short navigation on phones and collapsed WideNavigationRail at **839/840/841dp**, including gesture/system insets, landscape, rotation and large fonts. Verify Compact Now Playing on both layouts.
- Inspect Home/Stats/History/Profile/Settings/Appearance/Update Center in Light, Dark, OLED and a dynamic wallpaper palette.
- Check TalkBack and accessibility touch sizes, especially label-hidden navigation and time selection.
- Test quick tab changes, large-history scrolling, and rapid Supported Apps, Stats and History searches for jank, memory churn, search-result freshness, and data refresh correctness.
- Check dialogs and snackbars in Light/Dark/OLED and at 1.3×/1.5× font scales: text wrapping, tap targets, accessibility focus, native action/dismiss behavior.
- Confirm app tracking and compact-now-playing behavior continues while UI navigation animates.

**Build policy:** Changes remain on `main` with `[skip ci]`. Do not dispatch build or release workflows without explicit user authorization.

## Official guidance

- [Material 3 Expressive](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose November 2026 release](https://developer.android.com/blog/posts/jetpack-compose-november-2026-release)
- [Short navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)
- [Wide navigation rail](https://developer.android.com/reference/kotlin/androidx/compose/material3/WideNavigationRail.composable)
- [Material connected button groups](https://developer.android.com/develop/ui/compose/components/button-group)
