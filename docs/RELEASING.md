# Release procedure

## Version and channel

Use `MAJOR.MINOR.PATCH-beta.N` for beta display versions and `vMAJOR.MINOR.PATCH-beta.N` for tags. Stable versions omit the suffix. Titles follow `Tempo X.Y.Z Beta N — Description` or `Tempo X.Y.Z — Description`. Legacy `4.8.16-fork.*` tags retain their original identities.

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
to `4.10.25-beta.1`. The browser extension uses `4.10.25.1`; Android
uses monotonically increasing `versionCode=4821` (independent of the
display version). Once a candidate is prepared, subsequent fixes should
retain its base version unless an explicit new version is chosen.

These bounds apply to **new releases**, not published historical tags;
never rename or retag old APKs or silently change signing identity.

The extension uses the same numeric version as Android for stable releases.
For a beta, use the numeric browser form `X.Y.Z.N` for Android `X.Y.Z-beta.N`.
Bump the extension manifest, package, and lockfile together.

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
