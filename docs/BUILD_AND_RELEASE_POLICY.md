# Build and release authorization policy

**Reviewed:** 18 November 2026. Active development uses `main`.

## Default

Ordinary commits and pull requests do not start CI, lint, emulator tests or APK builds. `ci.yml`, `build-apk.yml`, `stable-release.yml` and `beta-release.yml` use `workflow_dispatch` only. The earlier release-only push exception has been removed.

The scheduled Play crash-rate monitor is separate: it may query configured Android vitals and open an issue; it does not compile or publish the app. It is inert without its required credentials.

## Scope of authorization

An instruction to audit, edit, commit, push, merge or fix issues does not authorize a build or release. A build request does not authorize publication. Do not dispatch workflows, push publishing tags or upload APK/AAB assets without the corresponding request.

When a request already identifies the intended version/channel/action, carry it out without asking for the same decision again. Resolve routine implementation choices within the authorized scope. Verify the result and report failure honestly.

Release metadata and documentation can be maintained independently of a binary release. Preserve existing tags and assets. The [release procedure](RELEASING.md) defines versioning and evidence requirements.

## Re-enabling automation

Only restore push/pull-request build triggers after an explicit request for automatic CI. Preserve this policy when merging workflow changes.
