# Release catalog

**Reviewed:** 21 November 2026. See [Latest stable release](https://github.com/matchalattewithbanana/Tempo/releases/latest) for the current published download.

| Version | Android code | Published | Purpose | Source / notes |
| --- | --- | --- | --- | --- |
| [4.10.25-beta](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.10.25-beta) | 4821 | 21 Nov 2026 IST | Material 3 Expressive and personalization beta with coordinated Android/browser packages | `fbdfd57` · [notes](v4.10.25-beta.md) · [validation](https://github.com/matchalattewithbanana/Tempo/actions/runs/44825331651) |
| 4.9.9 | 4820 | See GitHub | First joint Android and browser bundle | [notes](v4.9.9.md) |
| [4.9.0](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.9.0) | 4819 | 19 Nov 2026 IST | First stable GitHub release | `e0887af` · [notes](v4.9.0.md) |
| [4.9.0-beta.1](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.9.0-beta.1) | 4818 | 17 Nov 2026 | Historical beta: reliability and import hardening | `289fcd0` · [notes](v4.9.0-beta.1.md) |
| [4.8.16-fork.2-beta.1](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.8.16-fork.2-beta.1) | 4817 | 17 Nov 2026 | Historical Appearance Studio beta | `f99c502` · [notes](v4.8.16-fork.2-beta.1.md) |
| [4.8.16-fork.1](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.8.16-fork.1) | 4816 | 16 Nov 2026 | Historical first fork preview | `11000a1` · [notes](v4.8.16-fork.1.md) |

## Current beta

[Tempo 4.10.25 Beta](https://github.com/matchalattewithbanana/Tempo/releases/tag/v4.10.25-beta) is published as a GitHub prerelease with the Android APK, Chromium ZIP, Firefox ZIP and SHA-256 checksum file. The release was built from source commit [`fbdfd57`](https://github.com/matchalattewithbanana/Tempo/commit/fbdfd5769f8bf26c1720c49fafb956a5399a0f4f) and passed the gated [validation run](https://github.com/matchalattewithbanana/Tempo/actions/runs/44825331651). The version was calculated from 116 source commits after `v4.9.9`, using bounded carry-over versioning.

This is a public-testing beta, not a stable Play Store release. Physical-device checks remain outstanding.

## Install and verify

Download the APK or browser ZIP and `SHA256SUMS.txt` from the selected release. See [browser installation](../../browser-extension/README.md#install-from-github) for the Chromium and Firefox ZIPs. Compare the downloaded file's SHA-256 with its checksum. A checksum detects file mismatch; compare the signing certificate separately when assessing an in-place update. Do not assume two debug builds share a signer.

Export and verify a backup before reinstalling or changing signing identity. The package is `me.avinas.tempo`; minimum Android version is 8.0/API 26. GitHub APKs are separate from upstream Google Play distribution and require their own provider configuration.

## Release status

Version labels/tags identify the actual binary source. Current documentation and issue fixes may be newer than any published APK. Light/System, background playback, companion interoperability and performance require physical-device validation. See the [issue indexes](../README.md#current-audit-and-proposal-indexes).

Metadata corrections may improve titles/notes but do not move tags or replace APK/checksum assets. The [release procedure](../RELEASING.md) describes future versioning and publication.

Release metadata corrections must preserve existing tags, APKs and checksums.
