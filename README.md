# Tempo

<p align="center">
  <img src="TempoICON.png" width="108" alt="Tempo app icon" />
</p>

<p align="center">
  <strong>A local-first music journal for Android.</strong><br />
  Track your listening, explore your habits, and keep your history under your control.
</p>

<p align="center">
  <a href="https://github.com/matchalattewithbanana/Tempo/actions/workflows/ci.yml"><img alt="Android CI" src="https://github.com/matchalattewithbanana/Tempo/actions/workflows/ci.yml/badge.svg" /></a>
  <a href="LICENSE"><img alt="License: modified AGPL-3.0" src="https://img.shields.io/badge/license-modified_AGPL--3.0-blue" /></a>
  <a href="https://developer.android.com"><img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84" /></a>
</p>

> **About this fork:** This repository builds on [avinaxhroy/Tempo](https://github.com/avinaxhroy/Tempo). The [upstream Google Play listing](https://play.google.com/store/apps/details?id=me.avinas.tempo) is **not** a distribution channel for this fork's APKs. Fork-specific behavior may differ from the upstream release.

## Overview

Tempo records music playback observed on your Android device and organizes completed listens into a searchable history. Your primary library and listening statistics are stored locally in Room.

### What you can do

- **Track listening:** Android notification and MediaSession tracking for supported music players; optional Spotify Device Broadcast Status for faster Spotify playback changes.
- **Explore statistics:** listening time, play counts, charts, heatmaps, artist/album details, and optional listening challenges.
- **Import your history:** Spotify extended streaming-history JSON, Last.fm, YouTube Music Takeout, Deezer, Musicolet, and supported backup formats. Data quality depends on each export.
- **Create and share:** listening summaries, Spotlight stories, and themed statistics cards.
- **Use a browser companion:** the optional Chrome/Firefox extension can record web playback and sync to the Android app over a paired local network connection.
- **Keep control:** local export/restore, optional Google Drive backups, and data/analytics settings.

Not every player exposes reliable playback position or pause events. Imported history can also lack observed listening duration or completion; Tempo should not interpret missing values as full plays.

## Screenshots

<p align="center">
  <img src="Screenshots/S1.png" width="23%" alt="Home" />
  <img src="Screenshots/S3.png" width="23%" alt="Spotlight" />
  <img src="Screenshots/S5.png" width="23%" alt="Artist details" />
  <img src="Screenshots/S8.png" width="23%" alt="Widgets" />
</p>

## How tracking works

**Local tracking is primary.** Notification access and Android MediaSession provide playback observations. If enabled in both Spotify and Tempo, Spotify Device Broadcast Status supplies additional local metadata and play/pause events. A shared playback session reconciles those signals; completed listens are saved after meeting the configured minimum duration.

**Spotify API history sync is secondary.** For connected and authorized accounts, a WorkManager job requests recently played tracks approximately every **20 minutes**, subject to Android background scheduling, network access, and Spotify API availability. It reconciles with locally observed plays instead of replacing live tracking. The API does **not** report observed listening duration. Tempo no longer makes continuous foreground playback-state API requests.

The **Now Playing** card on Home, Profile, and Settings displays provisional playback information. A live playing timer is not a committed database statistic until the play is finalized. This is **not Discord Rich Presence**.

See [Tracking and import accuracy](docs/TRACKING_AND_IMPORT_ACCURACY.md) and [Real-time tracking pipeline](docs/REALTIME_TRACKING_PIPELINE.md) for source precedence, limitations, and troubleshooting.

## Install or build

**Beta APK:** [Tempo 4.8.16-fork.2 Beta 1](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.8.16-fork.2-beta.1) is a prerelease for appearance and usability testing. Back up your listening data before installing; Android requires the APK signer to match for in-place updates.

**Requirements:** Android 8.0+ (API 26) for the app; JDK 21 and Android SDK 36 for source builds. Use a recent Android Studio version with compatible Android Gradle Plugin support.

1. Clone **this fork** and enter the project:
   ```bash
   git clone https://github.com/matchalattewithbanana/Tempo.git
   cd Tempo
   ```
2. Add optional developer configuration in the project-root `local.properties`:
   ```properties
   SPOTIFY_CLIENT_ID=your_spotify_client_id
   LASTFM_API_KEY=your_lastfm_api_key
   GOOGLE_WEB_CLIENT_ID=your_google_web_oauth_client_id
   ```
3. Run tests and build a debug APK:
   ```bash
   ./gradlew :app:testDebugUnitTest
   ./gradlew :app:assembleDebug
   ```
4. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`.

The GitHub Actions [debug APK workflow](.github/workflows/build-apk.yml) is also available for manual builds. Its signing certificate may differ from local or previously installed APKs. **Back up your data before changing signing keys or uninstalling.**

### Spotify account integration

Spotify OAuth uses a public client ID with PKCE. If your build does not supply one, Tempo can accept a client ID at runtime. Register the redirect URI `tempo://spotify-callback` with the matching Spotify Developer application. Access is subject to Spotify's current developer-app restrictions and permissions.

Enable **Device Broadcast Status** under Spotify's playback settings *and* in Tempo for local Spotify broadcasts, if supported by your installed Spotify version. MediaSession and notification tracking remain the fallback.

### Google Drive backup setup

Google sign-in does not work automatically in a fork-built APK. Configure the Google Cloud OAuth consent screen, Drive API, a Web OAuth client ID, and the **Android OAuth client registered to the actual APK signing SHA-1**. For reproducible GitHub builds, use a persistent signing key.

See [Google Drive setup](docs/GOOGLE_DRIVE_SETUP.md). The repository deliberately does not include upstream credentials or signing keys.

### Browser extension

The optional companion has a [separate README](browser-extension/README.md). To develop it:

```bash
cd browser-extension
npm ci
npm run build
npm run typecheck
npm run smoke
```

Load the generated Chrome/Firefox extension from `browser-extension/dist` following the companion guide.

## Privacy and data

Listening history is kept locally unless you explicitly export, back up, or pair a browser companion. Optional analytics, if configured in a distribution, are documented in [Analytics and consent](docs/ANALYTICS.md); source builds with an empty analytics endpoint do not send analytics. Review [Privacy](PRIVACY.md) for details.

## Documentation

| Guide | Contents |
| --- | --- |
| [Tracking and import accuracy](docs/TRACKING_AND_IMPORT_ACCURACY.md) | Recording sources, duplicate handling and historical data quality |
| [History data quality](docs/HISTORY_DATA_QUALITY.md) | Preview and optionally correct unsupported legacy Spotify API estimates |
| [Real-time tracking pipeline](docs/REALTIME_TRACKING_PIPELINE.md) | Session timing, persistence and device test checklist |
| [Google Drive setup](docs/GOOGLE_DRIVE_SETUP.md) | OAuth, signing, backup setup |
| [Analytics](docs/ANALYTICS.md) | App-health data and consent |
| [Performance audit](docs/PERFORMANCE_AUDIT.md) | Earlier engineering findings and fixes |
| [Maintenance audit](docs/MAINTENANCE_AUDIT_2026-11-16.md) | Current maintenance changes and remaining device-test risks |
| [Browser companion](browser-extension/README.md) | Browser extension setup and pairing |
| [Changelog](Changelog.md) | Upstream historical release notes |

## Contributing

Read [CONTRIBUTION.md](CONTRIBUTION.md) before submitting changes. For playback fixes, include the player, Android version, expected versus actual session timing, and a reproduction scenario. Run unit tests and relevant instrumented tests where possible.

## License and attribution

Tempo originates from [avinaxhroy/Tempo](https://github.com/avinaxhroy/Tempo). This fork retains the project's [LICENSE](LICENSE), which contains modifications and restrictions beyond standard AGPL-3.0 terms. Review that file before redistribution or commercial use. All third-party trademarks remain with their respective owners.
