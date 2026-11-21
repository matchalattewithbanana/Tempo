# Tempo documentation

Tempo is a local-first Android listening journal with optional imports, account integrations, backups, browser/desktop companions, statistics, sharing, and personalization.

This documentation is split between **using Tempo**, **data accuracy and privacy**, and **development/release engineering**. The source files in this repository remain the authority; the docmd site is a navigable presentation of the same documentation.

## Start here

### Using Tempo

- [Tracking and import accuracy](TRACKING_AND_IMPORT_ACCURACY.md) — how local playback, Spotify signals, imports, and cross-source reconciliation differ.
- [Listening-history data quality](HISTORY_DATA_QUALITY.md) — what the history-quality scan can detect and which repairs require explicit confirmation.
- [Google Drive backup setup](GOOGLE_DRIVE_SETUP.md) — OAuth configuration, signing SHA-1 requirements, backup setup, and limitations.
- [Using Tempo guide index](guides/index.md) — a shorter map of common user tasks and troubleshooting references.
- [Browser companion](https://github.com/matchalattewithbanana/Tempo/blob/main/browser-extension/README.md) — installation, pairing, supported-player tracking, queueing, and protocol notes.
- [Support](https://github.com/matchalattewithbanana/Tempo/blob/main/SUPPORT.md) — useful bug reports, common checks, and reset guidance.

### Privacy and safety

- [Android privacy policy](https://github.com/matchalattewithbanana/Tempo/blob/main/PRIVACY.md)
- [Browser companion privacy](https://github.com/matchalattewithbanana/Tempo/blob/main/browser-extension/PRIVACY.md)
- [Security reporting](https://github.com/matchalattewithbanana/Tempo/blob/main/SECURITY.md)
- [Privacy declaration checklist](PRIVACY_DECLARATIONS.md)
- [Analytics behavior](ANALYTICS.md)

Tempo stores its primary listening library in app-private Room/SQLite storage. Optional integrations can send data to their respective providers. Backup archives are ZIP/JSON-based and are **not password-encrypted by Tempo**.

### Development

- [Developer guide index](development/index.md)
- [Development setup](DEVELOPMENT.md)
- [Repository architecture](ARCHITECTURE.md)
- [Build and release authorization policy](BUILD_AND_RELEASE_POLICY.md)
- [Release procedure](RELEASING.md)
- [Contributing](https://github.com/matchalattewithbanana/Tempo/blob/main/CONTRIBUTING.md)

### Releases and historical records

- [Release catalog](releases/README.md)
- [Fork changelog](https://github.com/matchalattewithbanana/Tempo/blob/main/Changelog.md)
- [Audit records](audits/)
- [Archived upstream changelog](archive/UPSTREAM_CHANGELOG.md)

Historical audits and plans describe the revision they were written for. They are useful engineering records, but a proposal or audit item is not evidence that a feature shipped.

## Documentation site

The repository includes a small docmd configuration so the Markdown tree can be browsed as a local static documentation site. See [Documentation site development](DOCS_SITE.md) for the commands and maintenance rules.
