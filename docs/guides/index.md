# Using Tempo

This index points to the current user-facing documentation without duplicating the detailed source material.

## Tracking music

Start with [Tracking and import accuracy](../TRACKING_AND_IMPORT_ACCURACY.md).

Tempo can combine Android MediaSession/notification observations, optional Spotify device broadcasts, and optional Spotify API history reconciliation. A **Now Playing** card is provisional presence; completed listening history depends on a finalized, persisted event.

For continuous tracking problems, review notification access, enabled apps, Android/OEM background restrictions, and the tracking-health information in Settings.

## Importing listening history

The [tracking and import accuracy guide](../TRACKING_AND_IMPORT_ACCURACY.md) documents Spotify, Apple Music, Last.fm, YouTube Music, Deezer, browser-extension, and backup reconciliation behavior.

Structured imports pass events through Tempo's transactional reconciliation layer. **Skip Duplicates** keeps equivalent existing records; **Replace Existing** can refresh exact/same-source matches while still protecting higher-authority measured live events. Neither mode is intended to erase unrelated history.

For legacy inflation or suspicious duplicate counts, use [Listening-history data quality](../HISTORY_DATA_QUALITY.md) before performing any explicit repair.

## Backups and restore

For Google Drive configuration and signing requirements, use [Google Drive backup setup](../GOOGLE_DRIVE_SETUP.md).

Tempo backup archives contain library data and optional images. They are not password-encrypted by the app. Export a verified backup before uninstalling, changing signing identities, or using destructive history-repair/reset actions.

## Browser companion

The browser companion records supported web-player observations and syncs queued plays to a paired Android device.

Read the [browser companion guide](https://github.com/matchalattewithbanana/Tempo/blob/main/browser-extension/README.md) for installation, pairing, queue behavior, and the current security/protocol notes.

## Troubleshooting

Use [Support](https://github.com/matchalattewithbanana/Tempo/blob/main/SUPPORT.md) for the common troubleshooting checklist and bug-report requirements.

When reporting timing problems, include observed play/pause/seek times rather than only a rounded total. Do not post OAuth tokens, signing keys, pairing secrets, raw databases, full backups, or unredacted listening history.
