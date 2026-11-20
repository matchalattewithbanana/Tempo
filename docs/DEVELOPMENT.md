# Development setup

## Toolchain

Use JDK 21 (also selected by the checked-in Gradle daemon criteria), Android SDK 37.1 for the current `main` branch, and the repository's Gradle wrapper. The app targets API 36 and supports Android API 26+. Exact dependency/plugin versions are in `gradle/libs.versions.toml`; use a compatible Android Studio release rather than a fixed outdated IDE recommendation.

```bash
git clone https://github.com/matchalattewithbanana/Tempo.git
cd Tempo
```

Add local SDK and optional public integration IDs to ignored `local.properties`:

```properties
sdk.dir=/path/to/Android/sdk
SPOTIFY_CLIENT_ID=your_public_client_id
LASTFM_API_KEY=your_key
GOOGLE_WEB_CLIENT_ID=your_web_oauth_client_id
```

Do not commit secrets, keystores, personal exports or reporting deployment credentials. Spotify can also accept a public client ID at runtime. Google sign-in requires signer-specific setup; see [Drive setup](GOOGLE_DRIVE_SETUP.md).

GitHub builds show a link to this fork's issue tracker and suppress Play Store review prompts. Builds explicitly intended for Play distribution can enable review actions with the Gradle property `-Ptempo.playStoreReviews=true`.

## Checks and local build commands

Run only the checks/builds appropriate and authorized for the task:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:assembleDebug
```

Instrumentation needs an emulator/device. The debug APK is normally under `app/build/outputs/apk/debug/`. Generated output is not source and should not be committed. A local debug signer may differ from published builds.

## Browser companion

Use Node.js 20 and install from the lockfile:

```bash
cd browser-extension
npm ci
npm run typecheck
npm run smoke
npm run build:all
```

The Chrome output is `dist/chrome/`; Firefox output is `dist/firefox/`. See the [companion guide](../browser-extension/README.md).

## Architecture and workflow

Read [ARCHITECTURE.md](ARCHITECTURE.md) before moving runtime packages. Preserve resource paths, Hilt bindings, Room schema/migrations, worker names and serialized identifiers. Organize source incrementally with focused validation rather than a broad rename during documentation maintenance.

GitHub workflows remain manual; commits do not dispatch validation or publish releases. [Release procedure](RELEASING.md).
