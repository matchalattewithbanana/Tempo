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
| Main navigation | `ShortNavigationBar` and `ShortNavigationBarItem` | accessibility semantics, active indicator, small/medium layouts |
| Home / Stats periods | `ToggleButton` | tactile selected states with readable labels on narrow screens |
| Stats / Profile tabs | `PrimaryTabRow` and `Tab` | accessible selection without shadow compositing |
| History filters | `ToggleButton` | clear range selection, adequate hit area |
| Settings | `ListItem`, native `Switch`, responsive search | consistent text hierarchy and taps |
| Appearance Studio | `ToggleButton`, expanded shape scale | easy previewable selections |
| Update Center | `MediumFlexibleTopAppBar`, `ToggleButton` | coherent header/channel selection |
| Loading | `CircularWavyProgressIndicator`, `LinearWavyProgressIndicator` | expressive but reduced-motion-safe loading states |
| Stat cards | `MaterialTheme.motionScheme.fastSpatialSpec/fastEffectsSpec` | consistent short entrance effects without bouncy slow springs |

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
- Compile all Kotlin source sets; run unit tests, especially `AppearanceMaterialShapesTest` and runtime motion-policy tests.
- Validate the three-tab navigation on small phones and >=600dp layouts, gesture/system insets, landscape, screen rotation and large fonts.
- Inspect Home/Stats/History/Profile/Settings/Appearance/Update Center in Light, Dark, OLED and a dynamic wallpaper palette.
- Check TalkBack and accessibility touch sizes, especially label-hidden navigation and time selection.
- Test quick tab changes and large-history scrolling for jank, memory churn, and data refresh correctness.
- Confirm app tracking and compact-now-playing behavior continues while UI navigation animates.

**Build policy:** Changes remain on `main` with `[skip ci]`. Do not dispatch build or release workflows without explicit user authorization.

## Official guidance

- [Material 3 Expressive](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Compose November 2026 release](https://developer.android.com/blog/posts/jetpack-compose-november-2026-release)
- [Short navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar)
