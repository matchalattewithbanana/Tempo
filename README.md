# Tempo

<p align="center"><img src="TempoICON.png" width="96" alt="Tempo icon" /></p>

**A local-first listening journal for Android.** Track supported players, explore your history, import older records, and create listening summaries.

This repository is a maintained fork of [avinaxhroy/Tempo](https://github.com/avinaxhroy/Tempo). Its APKs are distributed through this repository, separately from the upstream Google Play app.

## Download

[Latest stable release](https://github.com/matchalattewithbanana/Tempo/releases/latest) · Android 8.0+ · signed debug-variant APK

Starting with 4.9.9, the same GitHub release includes Chromium and Firefox companion ZIPs. [Companion installation](browser-extension/README.md#install-from-github).

Export a backup before upgrading. An in-place update requires the same signing certificate as the installed app. Light/System appearance, background tracking and device performance still have open device-validation work.

See the [release catalog](docs/releases/README.md) for versions, checksums and upgrade guidance. Historical tags and APKs are retained.

## Features

- Local notification and MediaSession playback tracking, with optional Spotify device broadcasts.
- Listening history, charts, artist/album details, challenges and badges.
- Spotify, Apple Music, Last.fm, YouTube Music, Deezer, Musicolet and browser-history imports.
- Eighteen appearance presets, including ten curated light/dark editions, saved looks, configurable typography and layout, customizable Stats/Profile views, and precise listening-time display.
- Spotlight stories, share cards, and a customizable 3×3 album poster.
- Local export/restore, optional Google Drive backups and an optional browser companion.

Playback accuracy depends on the player's available signals. Now Playing is provisional; finished listens and imported records can have different timing evidence. Spotify API reconciliation requests a 20-minute background cadence, which Android may defer. Read [tracking accuracy](docs/TRACKING_AND_IMPORT_ACCURACY.md) before interpreting imported durations.

## Update Center

**Settings → Update Center** checks the maintained fork's published GitHub releases, compares the installed version with the **Stable** or opt-in **Preview** channel, shows release notes and starts an APK download only when you ask. **Automatic checks are off by default.** Enabling them schedules a network-aware daily WorkManager check; it may show an update notification, but it will never download or install in the background without your request. You can mute a particular release.

APK downloads run through WorkManager and show progress. Tempo accepts only its exact GitHub Release APK name and URL, a valid published asset digest, and an expected bounded APK size. Downloads are stored privately, SHA-256 verified, checked for correct package identity, a newer Android version code and the same signing certificate as the installed app, and then handed to Android's installer with user confirmation. If Android requires permission to install from Tempo, the Update Center opens the corresponding system settings. **No silent installs, forced updates or bypasses of Android safety controls.** If your installed APK uses an incompatible signing key, export a backup before considering a reinstall; the updater will refuse an in-place install.

The Update Center checks Android APK releases only. The Chromium/Firefox companion packages remain linked on each GitHub release page. An app version that has unreleased commits is not an available update until a new version is explicitly built and published.

## Material 3 Expressive (2026)

Tempo uses Google's stable Compose Material 3 Expressive 1.5.0 from BOM **2026.11.01**, while preserving the eighteen curated palettes and configurable corners/typography. Our app-level theme uses `MaterialExpressiveTheme`, the official motion scheme, and the corresponding increased shape and emphasized typography tokens. Core navigation now uses the official `ShortNavigationBar` and Material-selected destinations; History and Home use Material expressive toggle buttons for time ranges, and Profile/Stats have native tab rows. Settings uses `ListItem` and improved search, Appearance Studio uses expressive choices, and the Update Center has a flexible app bar and matching controls. Loading indicators are wavy only when motion is permitted by system accessibility and Tempo's low-resource policy.

These controls are selectively chosen for music tracking; not every Material 3 Expressive component is appropriate (for example, FAB menus and carousels should not be added where no corresponding workflow exists). Existing artwork-driven stats, share cards and player surfaces remain purpose-designed.

**Toolchain:** Compile SDK 37.1 / Android Gradle Plugin 9.5.1 / Gradle 9.8.2 / Compose BOM 2026.11.01 / Lifecycle 2.11.0. Target SDK remains 36. **Build, instrumented checks and physical-device validation are pending authorization.** See [Material 3 Expressive migration notes](docs/MATERIAL3_EXPRESSIVE.md).

## Material UI

Tempo uses its existing Material 3 interface rather than adding a separate style system. **Appearance Studio → Layout → Corners** now controls the shape family of standard Material buttons, chips, menus and sheets. Settings and Appearance Studio have cleaner grouped sections, comfortable touch targets and readable color roles. The bottom navigation has a more compact tonal appearance, and listening-period selection is lighter without costly selection shadows.

## Launcher icon personalization

The four alternate launcher icons use the **same rendered badge artwork displayed in the design concepts**, optimized as transparent WebP assets for Android adaptive launchers; they are not placeholder vector reinterpretations. Their themed monochrome counterparts use the matching symbols.

Select **Settings → General → App icon**, or **Appearance Studio → App icon**, to switch between the original Tempo icon and four Material-inspired designs: **Pulse Mint**, **Tempo Clock**, **Violet Trail**, and **Vinyl Night**. The gallery previews the actual launcher icons. Choices apply immediately and persist through Android's PackageManager, independent of listening records or appearance presets. Android 13+ supports monochrome themed variants. Some launchers take a moment to refresh their cached icon.

The original icon remains available as **Tempo Classic**. This feature does not change the app's package identity, need network access, or restart listening tracking.

## Appearance presets

Go to **Appearance Studio → Presets** for the new curated collection: Midnight Bloom, Sakura, Matcha, Cobalt, Porcelain, Terracotta, Citrine, Mocha, Lagoon and Lilac, alongside Tempo's eight signature looks. Choose Light, Dark, OLED or System tone first to see the corresponding miniature preview. The gallery uses lazy rows to keep scrolling responsive.

When enabled, **Match typography, corners and background to the preset** applies a coordinated heading font, shape, surface finish and backdrop. Disable it to preserve those personal settings when changing colors. Selecting a preset activates its intended accent; any custom accent value remains saved and can be reactivated from Accent Source. Previews are not applied to the app until you tap **Apply**.

## Listening-time display

In **Appearance Studio → Stats → Listening time units**, choose automatic, decimal minutes/hours/days, or clock-style hours/minutes (optionally including seconds). Decimal units represent fractions of a unit: **589.95 min = 9h 49m 57s**; `.95 min` means 57 seconds, not 95 seconds. Decimal precision is adjustable from 0 to 3 places. Formatting does not modify the stored millisecond totals.

## Privacy

The primary library is stored in app-private Room/SQLite storage. The app does not add database encryption or password encryption to backup archives. Metadata services receive search terms or music identifiers; account integrations, paired-device sync, backups and sharing have their own data flows. Health reporting is build-configurable and uses a random installation identifier when active.

Read the [Android privacy policy](PRIVACY.md), [browser policy](browser-extension/PRIVACY.md) and [analytics reference](docs/ANALYTICS.md) for controls and limitations.

## Build and contribute

Development uses JDK 21, Android SDK 36 and the checked-in Gradle wrapper. Browser development uses Node.js 20 and the npm lockfile. See [development setup](docs/DEVELOPMENT.md), [architecture](docs/ARCHITECTURE.md) and [contributing](CONTRIBUTING.md).

Build and validation workflows are manual. Ordinary commits do not build or publish APKs. See [release policy](docs/BUILD_AND_RELEASE_POLICY.md).

## Documentation and support

| Resource | Purpose |
| --- | --- |
| [Documentation index](docs/README.md) | User guides, development references and historical reports |
| [Support](SUPPORT.md) | Troubleshooting and useful bug reports |
| [Security](SECURITY.md) | Responsible vulnerability reporting |
| [Changelog](Changelog.md) | Fork releases and unreleased changes |
| [Current audit backlog](https://github.com/matchalattewithbanana/Tempo/issues) | Confirmed findings and proposed work |

Repository screenshots are illustrative historical assets. See the release notes for current validation.

## Attribution and license

Original project: [avinaxhroy/Tempo](https://github.com/avinaxhroy/Tempo). See [NOTICE.md](NOTICE.md) and the complete [LICENSE](LICENSE). The license contains additional restrictions beyond standard AGPL-3.0, including commercial-use and rebranding restrictions. The license text is retained unchanged. Third-party services and marks belong to their respective owners; this fork is not endorsed by those services.
