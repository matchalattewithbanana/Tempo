# Developing Tempo

This page is the entry point for contributors and maintainers. Detailed behavior remains in the linked source documents.

## Set up the Android project

Use [Development setup](../DEVELOPMENT.md) for the required JDK, Android SDK, Gradle wrapper, local configuration, and validation commands.

The current architecture is an incremental MVVM/repository design. Review [Repository architecture](../ARCHITECTURE.md) before adding another service, global scope, persistence layer, or UI-owned network operation.

## Change boundaries

Treat these as compatibility contracts unless a reviewed migration explicitly changes them:

- package/application identity and OAuth callbacks;
- Room database/schema and serialized backup keys;
- signing identity for install compatibility;
- the distinction between provisional playback observations and committed listening events;
- import/reconciliation semantics for existing libraries.

UI work should use theme roles and shared accessible controls and must be checked in Light/Dark/System/OLED, large text, compact windows, RTL where relevant, and reduced-motion conditions.

## Builds and releases

Normal source commits do **not** authorize a build or release workflow.

Read [Build and release authorization policy](../BUILD_AND_RELEASE_POLICY.md) before dispatching CI or packaging artifacts, then use [Release procedure](../RELEASING.md) for the actual release sequence.

## Privacy and telemetry

Read [Analytics behavior](../ANALYTICS.md) and [Privacy declaration checklist](../PRIVACY_DECLARATIONS.md) before changing diagnostics, health reporting, provider integrations, backups, or data sent off-device.

The public [privacy policy](https://github.com/matchalattewithbanana/Tempo/blob/main/PRIVACY.md) must stay aligned with implemented behavior.

## Contributing

Use the repository [contribution guide](https://github.com/matchalattewithbanana/Tempo/blob/main/CONTRIBUTING.md). Keep changes focused, report the checks actually run, and do not mark roadmap work complete solely because source code compiles.
