# Material 3 Expressive migration — Tempo

**Status:** implementation committed to `main`, **not compiled or device-tested**. No workflows, builds or releases were dispatched.

## Baseline

- Compose BOM `2026.11.01` — Compose 1.13 and stable Material3 1.5.0.
- Android Gradle Plugin `9.5.1`, Gradle `9.8.2`, compile SDK `37.1`, Lifecycle `2.11.0` (required by Compose's current lifecycle/lint integration).
- Android target SDK remains `36`; any change to this is a separate compatibility task.
- `MaterialExpressiveTheme` wraps every Compose screen. This is not a second theme engine.
- Material motion is expressive by default, but `AppearanceRuntimePolicy.reduceMotion` activates the standard motion scheme for system-disabled animations, reduced-motion preferences, power-save, thermal limits or background/low-resource constraints.

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
| Shared dialogs | `Surface`, `Button`, `FilledTonalButton` | remove extra gradients/large shadows, follow themed shapes |
| Snackbar feedback | `Snackbar` with native actions | focus/semantics and severity icon, no custom shadow stack |
| Supported Apps search | `TextField` + `TempoLinearLoadingIndicator` | keep results visible while off-main query projection catches up |
| Stats / History search | `TextField` with Material toneroles | reliable IME/focus, accessible clear/close, no custom glass shadow field |
| Load / error / empty states | Material error/secondary containers | readable contrast in Light/Dark/OLED and a native Retry action |

The existing `GlassCard` and music-specific surfaces are retained where they represent song art, statistics and listening history better than generic cards. They already use theme palette, corner and solid-surface policies. Material3 components are used in shared controls, not as a reason to add unrelated UI.

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
