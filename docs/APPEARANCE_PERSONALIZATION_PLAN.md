# Tempo — Appearance & Personalization Design RFC

**Status (2026-11-17, Beta 1):** Six appearance presets, Dark/OLED, **opt-in experimental Light/System**, typography/density/shape, adaptive effects, scoped wallpaper/artwork accents, Home arrangement, Stats defaults, saved looks and appearance-only import/export are implemented. The compact navigation Now Playing strip now includes cover art and freshness handling. The beta release requires unit-test, Android lint, APK assembly and emulator test success before publication. Physical-device appearance, accessibility and performance testing remain open; advanced blur/refraction remains deferred.

### Current delivery checklist

| Area | Source status | Remaining gate |
| --- | --- | --- |
| Six curated looks, Dark/OLED | Implemented; Abyss remains default | Device screenshots and contrast across all screens |
| Light/System | Opt-in beta tones available live; system follows OS dark preference | Physical device contrast review for overlays, imagery, large fonts and TalkBack |
| Typography, bold text, density, corners | Implemented in Material typography and shared cards; compact changes default card padding only | Legacy explicit font/shape overrides; 320dp, RTL and 2x font checks |
| Preview, Apply/Cancel, reset | Whole draft, atomic persistence, schema v2 with legacy fallback | Process-death, rotation and storage failure instrumentation |
| Wallpaper/artwork accents | API 31+ wallpaper; cache-only current-track artwork accent | Device wallpaper refresh, missing/stale artwork cases |
| Motion/effects | User/system motion, power/thermal/background gates, optional low-RAM adaptation, Solid/Satin | Benchmarks and navigation/legacy effect audit |
| Home and Stats arrangement | Reorder/hide optional cards; inline/compact/hidden Now Listening; initial Stats tab/range | Interactive navigation and accessibility checks |
| Saved looks and transfer | Up to 12 named looks; bounded versioned JSON; import edits draft only | Document-provider and old backup restore checks |
| Floating strip, progressive blur/refraction | Compact cover-art strip implemented with stale expiry; advanced blur/refraction deferred | Device inset/freshness checks and GPU performance measurements |
| Tests and release | Dedicated beta branch runs test/lint/build/emulator; prerelease only after success | Physical-device beta feedback and production-signing readiness |

**Next sequence:** collect physical-device beta feedback on Light/System contrast and compact playback insets; fix regressions before promoting beyond beta; consider advanced visual effects only after performance benchmarks.

**Scope:** Visual design system, appearance settings, performance profiles, accessibility, rollout, testing.
**Out of scope:** Rebranding, player controls, altering music-tracking/event persistence, changing Android app ID, generating/publishing APKs.
**Reference:** User-supplied screenshot of an Appearance page with system/light/dark themes, font, reduced animation, reduced blur, blend effects, "Liquid Glass", glass performance, progressive blur, and a floating mini player/nav. It is a feature reference, not an interface or artwork to copy.

## Product objective

Give Tempo a signature, visually memorable look while letting different listeners choose between restrained, vivid, light, OLED and artwork-responsive experiences. Appearance must never affect the accuracy of background music tracking, archive integrity, backup restore, or listener availability.

**Guiding rules**
- The default must remain very close to the current Tempo dark/teal appearance until replacements are tested.
- Provide polished, coherent *looks*, not a long wall of technical toggles.
- Separate visual identity (preset/palette), legibility (text/contrast/density) and GPU cost (motion/blur).
- Every expensive effect needs a flat/solid fallback and must stop when offscreen, backgrounded or power constrained.
- Presets must preview on representative actual components (Now Playing, stats cards, labels, navigation), not just colored swatches.
- Preserve the user's choice across restart and backup/restore where appropriate; allow Reset to Tempo Default and non-destructive previews.
- No copied Apple fonts/assets/proprietary glass shaders or claims of Apple-style native glass.

## Original baseline and migration targets

The following table records the pre-implementation baseline. Use the current checklist above for delivery status.

| Existing file | Reality now | Required change |
| --- | --- | --- |
| ui/theme/Theme.kt | DarkColorScheme is forced; LightColorScheme points to the dark scheme; darkTheme/dynamicColor parameters have no effect. | Implement true light/dark/system behavior, dynamic wallpaper opt-in, adaptive system-bar icon colors. |
| ui/theme/Color.kt | Many global immutable hard-coded dark/teal colors, aliases, elevation and glass tokens. | Separate semantics from constants, introduce role-based palette tokens. Migrate screens incrementally. |
| ui/theme/Type.kt | Plus Jakarta Sans in bundled weights, serif headings; typography is global/static. | Provide coherent, licensable typography presets with prebuilt cached Typography objects. Retain system font scaling. |
| ui/components/DeepOceanBackground.kt | Fixed near-black gradient is drawn under most screens. | Background-style compositor driven by palette/profile, static by default; artwork glow scoped to Now Playing. |
| ui/components/GlassCard.kt | Five existing glass/solid variants: HighProminence, LowProminence, QuietGlass, TintedSolid, Obsidian. | Map style tokens to variants; distinguish a translucent tint from genuine backdrop blur; make solid fallback automatic. |
| ui/theme/Modifiers.kt | glassBlur uses Android RenderEffect on API 31+; rememberReducedMotion checks animator_duration_scale once per composition lifetime. | One effective motion/effect policy, recheck system preference on resume, quality fallbacks and no unnecessary frame loops. |
| ui/navigation/TempoBottomNavigation.kt | Fixed rounded dark pill, 72dp height, shadow and hard-coded transparent-white overlay. | Theme-aware options; default keeps labeled destinations; no fake playback buttons on tracker. |
| ui/settings/SettingsScreen.kt | General group already exists but no dedicated Appearance page. | Add General -> Appearance -> separate screen and preview/preset picker, not 20 switches on the main Settings list. |
| app/build.gradle.kts | minSdk 26, targetSdk 36. | Every feature must work on API 26+; selectively enable newer effects. |
| MainActivity.kt | TempoTheme wraps AppNavigation. | Observe a versioned appearance StateFlow above MaterialTheme, splash-safe initial values, avoid theme-change activity restarts. |

### Platform facts to respect
- Material 3 wallpaper Dynamic Color is available from Android 12 (API 31); provide bundled fallback palettes on older Android.
- Android Compose Modifier.blur applies to *content* rendered in its own layer; this is **not automatically a blur of content behind a surface**.
- Uniform blur requires API 31+; Compose progressive blur as documented currently requires API 33+ and compatible library support. Verify library versions before implementing.
- Actual optical refraction / liquid distortion requires specialized effects; do not label a simple frosted panel "real refraction".
- Animations must respect system disabled animator settings, OS power saving and an explicit in-app Reduce Motion preference.

## 1. Six curated appearance presets

These are **design proposals**, with approximate colors for initial design review (final accessible tones subject to testing).

| Preset | Suggested background / accent / secondary | Mood and component treatment |
| --- | --- | --- |
| **Abyss** (current-compatible default) | #0A0E0E / #2FDBB8 / #9AA2A2 | Deep near-black, clean sea-glass teal, quiet satin cards, steady charts. |
| **Obsidian Gold** | #050608 / #DDBA70 / #F0E7D2 | OLED-black editorial look, fine warm-metal borders, low-glow, refined and high contrast. |
| **Aurora** | #0F102A / #A78BFA / #4AE4C4 | Deep indigo, violet-to-aqua halos, floating luminous highlights; accents localized, never glowing behind all text. |
| **Ember** | #1C0D15 / #FF758C / #F3A95C | Burgundy/ink, coral and amber, asymmetric highlight details; expressive but restrained on data-heavy screens. |
| **Glacier** | #EFF6FC / #166DA3 / #55B8C8 | True light-mode ice-white surfaces, navy text, airy shadows, subtle blue tint. |
| **Solstice** | #FFF7EA / #A6512B / #D59B43 | Warm ivory/terracotta, ink-gray copy, magazine-style editorial serif headers, minimal chrome. |

- Presets 1–4 are dark-first; 5–6 are light-first. Each can have an alternate tone in phase 3 if contrast rules pass; do not claim all 12 variants exist at launch.
- Add **System / Light / Dark / OLED Black** as tone options where the preset supports them. OLED Black is optional and should not be forced for every palette.
- **Wallpaper Colors** (Android 12+) and **Album Art Accent** are *accent sources*, not seventh/eighth whole-app themes; apply only to supported foreground surfaces. Album Art Accent changes when a new track starts, not every position update.
- Color blind-safe qualitative chart tokens must stay distinguishable from decorative primary accents.

## 2. User-facing Appearance page

Settings -> General -> **Appearance** (one compact entry with selected look summary). Proposed layout:

1. **Your look:** large 2-column gallery of six visual preset cards. Each card includes a miniature Now Playing cover tile, title/artist lines, small listening graph, navigation accent, and light/dark background. Active item has clear selected text and check mark.
2. **Theme behavior:** System / Dark / Light / OLED Black (unsupported combinations disabled with explanation), with a one-line preview.
3. **Accent source:** Preset / Wallpaper (API 31+) / Album art (only when a suitable cover is available; deterministic fallback).
4. **Typography & layout:** Font style [Tempo Sans, Modern Rounded, Editorial, Studio Mono for numerics], text emphasis [Balanced/Bold], card density [Comfortable/Compact], shapes [Soft/Moderate/Sharp]. Never override Android font scale or display size.
5. **Motion & surface:** "Reduce animation" (Follow system / On); "Background motion" (Off / Subtle / Expressive); "Glass finish" (Solid / Satin / Frosted / Experimental); "Tint icons from artwork" (On/Off, only on supported surfaces); "Adaptive effects" (On by default). Hide unsupported features rather than show broken toggles.
6. **Advanced graphics** (collapsed): reduce dynamic blur, prefer faster glass, progressive blur (only supported devices), quality tier, effect diagnostics. Avoid redundant, contradictory switches.
7. **Actions:** preview changes instantaneously in a sandboxed preview, Apply / Cancel, Restore Tempo Default, optionally save a named local preset in a later release.

### Reference screenshot -> Tempo translation

| Reference setting | Planned Tempo equivalent | Priority |
| --- | --- | --- |
| System / Light / Dark | True app color-mode choice and system-bar contrast | P0 foundation |
| Font | Four safe style presets; no Apple SF Pro bundling | P1 |
| Reduce animation | Respect OS reduced motion + app override | P1 |
| Reduce dynamic blur | Glass finish = Satin/Solid or quality override | P2 |
| Disable blend effects | "Artwork-colored accents" toggle; never tint body text with low-contrast artwork | P2 |
| Liquid Glass | "Experimental refractive surfaces" behind explicit opt-in; not promised before renderer and device benchmarks | P3 |
| Better Performance for Glass | Adaptive Effects on by default; turn off GPU-heavy blur when slow/low-power | P2 |
| Progressive Blur | Experimental background-only effect for compatible Android/Compose versions | P3 |
| Floating mini player | Compact **Now Listening** status strip only, optional; source-app deep-link when possible, no fabricated pause/skip controls | P2 |

## 3. UX details that make it *visually striking*

- Preset thumbnails must be tiny full-interface compositions, not solid-color dots.
- Larger typographic contrast: small utility labels paired with confident music titles and stats; conservative body text sizes, dynamic type safe.
- Use one focal gradient per screen max. Put ambient artwork light **behind Now Playing only**, not behind every stats card. Avoid color noise on History.
- Each visual preset gets a cohesive visual language across buttons, active filters, listening plots, progress indicators, empty states, toasts, bottom navigation and backup/diagnostic status badges.
- Optional Now Listening bar may use a small cached album thumbnail + title + source/app icon; when playing status is stale, show "Last detected" instead of implying live playback.
- Expressive preset animations should be brief and deterministic; no uncontrolled infinite background loops or constant full-screen redraws.
- Protect charts and badges from over-tinting with decorative palettes.
- Appearance preview is local: changing appearance does **not** request Spotify data, send analytics, or alter playback tracking.

## 4. Settings model and architecture

Introduce versioned persisted data in app DataStore, using enum IDs (unknown/removed IDs fall back to Abyss):
- appearance_schema_version
- appearance_preset_id
- appearance_tone (system/dark/light/oled)
- appearance_accent_source (preset/wallpaper/artwork)
- appearance_font_style (tempo/rounded/editorial/studio)
- appearance_card_density (comfortable/compact)
- appearance_shape_style (soft/medium/sharp)
- appearance_reduce_motion (follow_system/on)
- appearance_background_motion (off/subtle/expressive)
- appearance_surface_style (solid/satin/frosted/experimental)
- appearance_adaptive_effects (true default)
- appearance_artwork_tint (false by default)
- appearance_now_listening_strip (hidden/inline/floating)

Implementation modules:
1. **AppearancePreferencesRepository**: single DataStore flow, typed validation, atomic updates; no Room schema migration and no changes to existing history/backup logic.
2. **AppearancePaletteResolver**: pure conversion from preset+tone+source to Material3 ColorScheme and custom colors; unit tests for fallbacks/contrast.
3. **AppearanceRuntimePolicy**: pure effective setting = user preference AND supported API AND system animation/power/thermal/low-RAM conditions. Never override OS accessibility restrictions.
4. **TempoDesignTokens**: CompositionLocal or equivalent immutable tokens for surface, text, accent, chart, glass, blur, shape and transition; migrate callers without per-screen preference access.
5. **AppearancePreviewViewModel**: draft state separate from saved state; Apply/Cancel or auto-revert on system back; previews must not persist until applied.
6. **AppearanceScreen**: gallery and sections; lazy items, no artwork network requests.
7. **ArtworkAccentProvider**: extract a safe high-contrast palette from already-cached artwork, off main thread and once per track identity; don't use image URL or album art selection for telemetry.

Avoid circular initialization: use a static/remembered default appearance while reading preferences, then apply hydrated config without relaunching the Activity. Keep Android content descriptions and strings in resources for all six shipped locales. No external font download or restricted font redistribution.

## 5. Effect & performance policy

| Effect | High capability | Mid/low capability | Battery saver / reduced motion |
| --- | --- | --- | --- |
| Static palette / typography | Full | Full | Full |
| Background glow | Static or low-frequency draw | Static draw | Static/disabled |
| Frosted cards | Targeted RenderEffect/layer (API 31+) if empirically safe | Satin gradient/solid | Solid |
| Progressive blur | API 33+ compatible renderer, opt-in | Not offered | Disabled |
| Experimental refraction | Feature flag, measured, explicitly opt-in | Not offered | Disabled |
| Artwork tint | Palette from cached art, throttled per identity | Cached palette or preset | Preset fallback |
| Micro-interactions | Short draw-phase animation | Short simple transitions | Minimal/no motion |
| Floating status strip | Only if UI and data freshness tested | Inline/hidden fallback | Inline/hidden |

- Blur is not refraction. A background blur needs a correct backdrop-rendering architecture, not blindly blurring child text.
- Clamp bitmap edges and blur layers; avoid reading a high-resolution cover every animation frame.
- Do not allocate new shader/bitmap objects on each progress update. Never let an animation cause 1 Hz Now Playing position updates to recompose the Home list or app theme.
- Use frame-timing and startup benchmarks on a real device before defaulting to any costly effect. Record janky frames, P95/P99 frame times, memory, heat and battery; no unsupported FPS claims.
- With Reduce Motion enabled, previews and navigations still work; no rotating backgrounds, bouncing tabs, parallax, or endlessly drifting highlights.
- Test app background/foreground state and low-battery tracking; UI-only effects may stop, but music listening collection must continue independently.

## 6. Rollout sequence (separate reviewable changes)

### Phase A — Semantic theming foundation (required)
- Extract theme/color/shape/type tokens; implement real dark/light/system; maintain Abyss as exact initial default.
- Convert background, navigation, key cards, major Home/Stats/History/Settings surfaces from hard-coded globals to tokens.
- Add independent unit tests for ColorScheme, fallback and contrast; no public style toggles until common screens render correctly.

### Phase B — Usable Appearance screen
- Add route under General and a six-preset gallery with non-animated micro-previews.
- Save preference and survive process death; preview Apply/Cancel; restore defaults.
- Introduce high-quality light variants and typography/density/shape presets; RTL/text scaling/system-bar tests.

### Phase C — Motion, blur and artwork personality
- Add safe Reduce Motion/Adaptive Effects/Surface finish controls; integrate existing GlassCard and nav.
- Add artwork tint scoped to Now Playing and optional status strip; never add unverified transport controls.
- Add solid and satin fallback quality on older/low-end devices.

### Phase D — Experimental finish (optional, after benchmarks)
- Optional progressive blur, localized soft glow, higher-quality glass/refraction where technically supported.
- Device capability gating, settings explanation and user-controlled rollback/restore.
- Only enable defaults backed by measured frame/thermal/battery performance.

### Phase E — Personal arrangement (optional)
- Reorder/hide Home cards, personalize initial Stats tab and preferred date window.
- Local saved combinations of supported preset+accent+type+density (no user-generated shader configs).
- Import/export appearance preferences separately from listening history; never break old backup restore.

## 7. Validation gates

- **Build / CI are manual only until the user explicitly requests a build.** Do not publish a release without distinct user approval.
- Automated tests: unknown preset fallback; corrupted DataStore; preference migration; draft preview cancellation; existing database and backup restored; OS font scale and contrast.
- Visual snapshots: preset gallery, Home, Stats, History, Now Playing, Settings and dialogs for dark/light/OLED; no light-on-light text or invisible system bar icons; all supported locales.
- Device API matrix: 26, 29/30, 31/32, 33, 35/36; low-RAM and power-save modes; font scaling 1x/1.5x/2x; 320dp device; keyboard and landscape.
- Tracking invariants: no changes to listener permissions, event timestamps, duration arithmetic, source priority, API polling or background service lifecycle.
- Benchmark: baseline vs new for cold start, Home scroll, stats filtering, switching appearance, idling in a playing session and low-battery use.

## Decision point

Recommended first release bundle: **Phase A + Phase B** with six curated looks, working light/dark/system, typography and accessible density. **Phase C** follows only after the baseline remains stable. Do **not** put "Liquid Glass" on the first release checklist.

## Reference materials (platform, not design assets)

- Android Material 3 dynamic color and schemes: https://developer.android.com/develop/ui/compose/designsystems/material3
- Blur support and surface-vs-backdrop distinction: https://developer.android.com/develop/ui/compose/graphics/blur/overview
- Android ValueAnimator system animation setting: https://developer.android.com/reference/kotlin/android/animation/ValueAnimator
- Compose animation performance: https://developer.android.com/develop/ui/compose/animation/quick-guide
- Baseline Profiles / Compose performance: https://developer.android.com/develop/ui/compose/performance/baseline-profiles
