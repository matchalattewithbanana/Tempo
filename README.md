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
- Appearance presets, saved looks, light and system themes, and customizable Stats and Profile views.
- Spotlight stories, share cards, and a customizable 3×3 album poster.
- Local export/restore, optional Google Drive backups and an optional browser companion.

Playback accuracy depends on the player's available signals. Now Playing is provisional; finished listens and imported records can have different timing evidence. Spotify API reconciliation requests a 20-minute background cadence, which Android may defer. Read [tracking accuracy](docs/TRACKING_AND_IMPORT_ACCURACY.md) before interpreting imported durations.

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
