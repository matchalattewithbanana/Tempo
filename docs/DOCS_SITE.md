# Documentation site development

Tempo's documentation remains plain Markdown under `docs/`. The repository-level `docmd.config.json` adds a local/static documentation-site view without changing the Android or browser-extension runtime.

## Requirements

- Node.js 20 or newer.
- No global docmd installation is required.

The Android project does not depend on docmd. The browser extension keeps its own Node package and lockfile; do not add docmd to that package just to build the documentation.

## Preview locally

From the repository root:

```bash
npx @docmd/core dev
```

docmd serves the documentation locally at its default development address.

## Validate documentation links

```bash
npx @docmd/core validate
```

Run this after changing internal Markdown links or reorganizing documentation.

## Build the static site

```bash
npx @docmd/core build
```

The repository config writes generated output to `docs-dist/`, which is ignored by Git.

## Source-of-truth rules

- Existing Markdown documents remain authoritative; the docmd configuration is a presentation/build layer.
- Prefer links over copying large sections into new landing pages.
- Do not move or rename established documents casually: the README, issues, release notes, and external links may depend on their current paths.
- Historical audits and plans must remain clearly labeled as historical/proposed when they do not describe current shipped behavior.
- Documentation-only changes do not require an APK build, but internal-link validation is expected when docmd is available.

## Search and machine-readable outputs

docmd includes offline search support. It can also generate outputs such as `llms.txt`, `llms-full.txt`, sitemap, and SEO metadata.

Those absolute-link outputs should be configured only after the documentation site has a stable public deployment URL. Do not publish a guessed canonical URL in the repository configuration.
