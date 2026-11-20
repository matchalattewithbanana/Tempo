# Release procedure

## Version and channel

Use **`MAJOR.MINOR.PATCH-beta`** for beta display versions and **`vMAJOR.MINOR.PATCH-beta`** for GitHub tags. Beta titles are `Tempo X.Y.Z Beta — Description`. Stable versions omit `-beta` and use `Tempo X.Y.Z — Description`. There is one named beta per base version; if another public beta is necessary, assign a new bounded base version rather than publishing `beta.1` or `beta.2`. Historical numbered beta and `4.8.16-fork.*` tags remain unchanged and readable.

For new releases, use **bounded carry-over versioning**. The patch component
ranges from **0 through 99**; each full 100 patches increments the minor
component by one and keeps the remainder. The minor component ranges from
**0 through 10 inclusive**; when it would become 11, increment the major and
reset the minor to zero. Preserve any patch remainder. The rule is enforced
by `.github/scripts/tempo_versioning.py` and checked during release validation.

Examples:
- `4.9.125` normalizes to `4.10.25`.
- `4.10.125` normalizes to `5.0.25`.
- `5.0.205` normalizes to `5.2.5`.
- `4.10.99 + 1 patch` becomes `5.0.0`.

Continue to determine the patch increment from **commits since the last stable
tag, measured before release-preparation commits**. Use
`git rev-list --count LAST_STABLE_TAG..main` and include merged commits.
Before publishing a release candidate, calculate and **normalize** the
candidate version. For example, `v4.9.9..main` was **116 commits** before
the 21 November 2026 preparation: `4.9.(9 + 116) = 4.9.125` normalizes
to `4.10.25-beta`. The browser extension uses `4.10.25.0`; Android
uses monotonically increasing `versionCode=4821` (independent of the
display version). Once a candidate is prepared, subsequent fixes should
retain its base version unless an explicit new version is chosen.

These bounds apply to **new releases**, not published historical tags;
never rename or retag old APKs or silently change signing identity.

Browser extension manifests require a numeric version, even though Android
and public GitHub tags use the unnumbered `-beta` label. To preserve forward
update ordering, use **`X.Y.Z.0` internally for beta manifests** and
**`X.Y.Z.1` for the subsequent stable manifest**. These numeric fourth
components are **not beta release numbers**. Set the source manifest's
`version_name` to the corresponding public `X.Y.Z-beta` or `X.Y.Z` label.
The Chromium bundle retains `version_name`; the Firefox bundle strips the
unsupported field and Tempo's popup displays the human-readable version.
The npm `package.json` and root lockfile version use actual SemVer:
`X.Y.Z-beta` for a beta, `X.Y.Z` for stable. The release validator verifies
both manifest and package versions. This prevents a stable extension being
treated as an older version than the beta.

For the current candidate: Android `4.10.25-beta`, versionCode `4821`,
browser manifest `4.10.25.0`, browser npm package `4.10.25-beta`.
The future stable manifest would be `4.10.25.1` and stable npm package
`4.10.25`; the stable APK must use a **newer Android versionCode**.

Android versionCode increases for each new installable build. Do not change package ID, OAuth callbacks, signing identity or stored-data formats as a release-label cleanup.

## Prepare

1. Resolve the intended scope against `main`; distinguish fixes from open proposals.
2. Update Android's version/versionCode, the companion version, `Changelog.md`, and the versioned note under `docs/releases/`. The workflow reads these version fields and checks the generated APK and browser manifests.
3. Document source commit, minimum Android version, APK type, signing continuity, upgrade steps, checks actually performed and remaining device limitations.
4. Configure the established keystore in GitHub Actions secrets `TEMPO_DEBUG_KEYSTORE_BASE64`, `TEMPO_DEBUG_STORE_PASSWORD`, `TEMPO_DEBUG_KEY_ALIAS`, and `TEMPO_DEBUG_KEY_PASSWORD`. Obtain the expected public certificate fingerprint from a trusted previously distributed APK with `apksigner verify --print-certs trusted.apk`. The stable workflow pins that fingerprint. The beta workflow reads it from `TEMPO_BETA_SIGNER_SHA256`. Use 64 hexadecimal digits, with optional colon separators.

## Validate and publish

Build/CI and publication require the relevant explicit request. Ordinary main commits run neither. Manually dispatch validation from main, inspect unit/lint/platform/extension results and keep test artifacts. Device visual, accessibility, tracking and battery checks are separate from green automation.

Only publish after the requested checks succeed and publication is authorized. The stable workflow's `publish_stable` input and the beta workflow's `publish_beta` input control publication. Keep the relevant input false for validation only. Publication requires the established signing configuration and checks the single APK signer after assembly and again after downloading the artifact. For throwaway debug APKs use the separate **Build debug APK (manual)** workflow. Google Play publication is a separate decision.

Create one new release containing the APK, Chromium ZIP, Firefox ZIP, and a
`SHA256SUMS.txt` entry for each asset. Set the prerelease status correctly and
identify the matching source commit. Keep the historical tags and their assets
attached to their original builds.

A public commit SHA identifies the source revision. Asset SHA-256 checksums
let users check the bytes they downloaded, and the public Android certificate
fingerprint identifies the signer. Keep the private signing key, keystore
contents, passwords, and tokens in GitHub Actions secrets. An asset checksum
by itself provides no proof of who built or signed the asset.

## Catalog and corrections

Maintain [the release catalog](releases/README.md). Correct misleading titles, links or notes without claiming the attached binary changed. Record the date of substantive metadata corrections. Older previews stay visible as historical prereleases, not competing current recommendations.

Published artifacts reflect their tagged source. An unreleased documentation/copy fix is not part of an older APK. No store listing, Play Console declaration, provider OAuth setting or upstream website is updated by changing repository documents.
