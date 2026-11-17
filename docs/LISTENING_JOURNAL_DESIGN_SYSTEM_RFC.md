# Listening Journal UI v2 — original design system and screen specifications
**Status:** Design RFC, 2026-11-17, not implemented. This is a UI architecture proposal for Tempo and a possible future identity **only if rights are cleared**. New visual assets, name and marketing remain speculative. This document is *not* a licence to rebrand or publish a differently branded derivative.

**Related:** [Product evolution roadmap](PRODUCT_EVOLUTION_ROADMAP_2026.md), [Visual postmortem](audits/2026-11-17-visual-system-postmortem.md), [Appearance RFC](APPEARANCE_PERSONALIZATION_PLAN.md), [Legal/brand gate](BREADCRUMBS_REBRAND_PLAN.md).

## Why the previous Home redesign failed

The illustrative "Your listening" mock was taken as a content checklist, not a coherent visual composition. The implementation wrapped the app in nested generic Material/Glass surfaces, carried old Hero/Today widgets below the new dashboard, retained huge nav vertical padding and hardcoded component sizes, and used theme accents without coordinated visual hierarchy. Automatic build/lint passes verified compilation, not screenshot fidelity. Do not repeat this by choosing arbitrary background hex colors or adding more glass panels.

## Comparative research synthesis (source → decision)

| External research / product | Useful lesson | What not to copy |
|---|---|---|
| Material 3 Expressive (Google Design) | Shape/size/containment/motion should signal priority and tap affordance; vary card intensity intentionally | Google's exact component proportions, animations or a clone of Pixel UI |
| Android window size classes, edge-to-edge | Window insets and responsive one-/two-pane layouts should be structural | Device-specific hardcoded padding and per-phone hacks |
| NN/g progressive disclosure | A simple Home with deeper inspectable analytics prevents cognitive overload | Deep nested menus or huge cards containing whole screens |
| W3C WCAG 2.2 + Android guidance | Test composited contrast, touch targets, labels, dynamic font scale, screen-reader chart summaries | Relying on raw color palette tests or color-only bars |
| stats.fm | A rich history can support listening clocks, top-item tables, period comparisons and imports | Cloning its green-on-black dashboard, fonts, category taxonomy or paywall |
| Last.fm | Cross-platform event capture and periodic summaries make a durable personal music record | Equating third-party scrobble semantics with observed Android listen seconds |

References:
- https://design.google/library/expressive-material-design-google-research
- https://developer.android.com/develop/ui/compose/system/setup-e2e
- https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps
- https://developer.android.com/guide/topics/ui/accessibility/apps
- https://www.w3.org/TR/WCAG22/
- https://www.nngroup.com/articles/progressive-disclosure/
- https://www.nngroup.com/articles/complex-application-design/
- https://support-new.stats.fm/hc/en-us/articles/59061730156441-Charts-and-the-stats-screen
- https://cbsi.my.salesforce-sites.com/lastfm/articles/LastFM/What-is-scrobbling?template=template_lastfm

## Design directions — original alternatives, not a borrowed visual template

### A. Listening Atlas / Editorial Journal — RECOMMENDED

- **Personality:** Calm, private, personal, contemporary editorial; music artwork is the emotional centre, statistics are instruments, not the wallpaper.
- **Forms:** Open canvas, fine typographic hierarchy and subtle containers, 12–20dp functional radii with occasional large artwork hero. Minimum ornament; **no global glass overlay**.
- **Proposed warm dark:** charcoal/ink with subtle violet slate supporting surfaces, apricot/amber signal accent, warm high-contrast off-white text; teal limited to explicit tracking-health status if visually appropriate.
- **Proposed warm light:** paper-white/ivory canvas, warm-ink type and slate/amber accents, airy neutral surfaces. No low-contrast pink-on-pink.
- **Assets:** album art as-is; abstract editorial linework and user-data visualizations as original static vector pieces. Only intentional small artwork ambient lights, not full-screen tint.
- **Signature interaction:** timeline as a personal record of listening moments, clear date headings, provenance detail, and a persistent but quiet "Listening now" status.
- **Reason:** Most distinct from inherited glass-heavy neon, scales from data-empty to long listening history, and works in light/dark/OLED.

### B. Signal Lab / Analytical Minimal

- Neutral dark charcoal, cool gray, restrained cobalt/lime accents; monospaced numerals + precise charts.
- Dense, powerful analytics; better for advanced users but cold as an emotional product. Offer as later optional preset, not the default.

### C. Record Room / Tactile Analog

- Warm paper and earth-tone surfaces, vinyl-inspired circular trend motifs, oversized album artwork and rich serif headline.
- Charming for story/share, but decorative gestures could obscure heavy data use. Use as a scoped editorial/story style, not an everyday performance baseline.

**Design decision to seek user review:** Choose the *A direction* as the default base. Retain six existing presets as compatibility choices until a clear migration maps them into unified v2 semantic roles. Do not remove saved looks. Real screenshots before approving colors or typography; exact hexes are **hypotheses**, not verified contrast tokens.

## One coherent UI architecture

- **Layout shell:** one root-scoped `JournalScaffold` with system-bar aware padding and an adaptive navigation structure. Screens must not add second full-screen background owner.
- **Content width:** on compact phones, 16–20dp horizontal margins and 8–12dp inter-component gutters; on expanded layouts cap readable prose and present paired list-detail views.
- **Spacing tokens:** 4, 8, 12, 16, 24, 32; exceptional 40/48 for section separation only; avoid randomly inserted 72dp empty gaps.
- **Shapes:** `radiusXs=8dp`, `S=12dp`, `M=16dp`, `L=20dp`, `full` for buttons/pills. User "Sharp/Moderate/Soft" remaps these categories consistently; not every screen may choose its own immutable 20/22/36dp values.
- **Surface levels:** canvas → quiet list row → elevated control → modal → artwork-focus stage. Ordinary metric tiles use tint/shadow **or** border, not every visual effect at once.
- **Typography roles:** display/headline for one main story, title for section/track, body for supporting detail, label for dates/filters, numeric metric style with tabular numbers. Support 200% dynamic font and long German/Russian labels. Do not force one custom family on everything.
- **Color roles:** canvas, onCanvas, mutedText, elevatedSurface, onSurface, accent, onAccent, accentContainer, onAccentContainer, separator, focusRing, selected, success, warning, error, chart1..chart6, artworkScrim. All computed across each tone, tested against *actual composited* surfaces.
- **Visual density:** comfortable default; compact trims vertical whitespace and optional metadata, never touch target size.
- **Motion:** short responsive transitions only on meaningful state changes. Reduced-motion or power-saver uses immediate/simplified updates. Skeleton static when reduced. No infinite canvas pulse.
- **Navigation semantics:** Home / Insights / Timeline (retain existing route names internally while user-facing labels evolve); Settings and Profile via top bar. Search is a separate focused task, not a decorative icon that navigates to a blank screen.
- **Media art:** prefer user/local embedded cover; reconcile uncertain remote artwork separately. Detail views distinguish verified/estimated; never cover text with untested vibrant imagery.
- **Status semantics:** `Observed live` vs `Paused` vs `Last seen [time]` vs `Saved` vs `Imported` and `Not capturing`. Source-status labels are plain language; diagnostics may reveal MediaSession/API details.

## Composition sketches (not app screenshots)

### Today / Home, compact phone

`[status/system insets]`
- Top line: `Your listening` or user-specific date/heading with searchable History icon and Profile/Settings action; two trailing icons max
- Quiet source/status line: `Listening from Spotify • observed just now`; if permission disabled, inline action `Enable tracking`
- Artwork (64–88dp) + 2-line title + 1-line artist; music-source icon; playback progress only when supported by actual media-position data; no invented scrubber
- Listening time today (including exact provisional value with disclosure) and play count (distinct tracks separately labeled when wanted)
- 7-day trend (bars/line; Monday–Sunday), readable labels; tap day opens day-filtered Timeline; a screen-reader summary and per-bar values
- One editorial fact/story card (optional); "Open Insights" link
- Bottom bar: compact, 3 labels and correct insets, no overlapping history rows
- Extended Home adds *only* genuinely new content: discovery/top song and Spotlight teaser, no repeated Hero/Today cards

**When paused:** Playback card changes to paused and progress stays frozen; if stale it becomes "Last detected" and ultimately disappears. **When empty:** one informative permission or "start listening" panel + previously available history; no massive stack of blank placeholder cards.

### Insights

- Headline and horizontal time range chips, stable across tabs
- Overview metrics: observed minutes, plays, tracks, artists; estimated/import coverage disclosed
- Main data visual: day/hour/week series with accessible table equivalent; allow tap/long-press exact values
- Three top lists (Tracks, Artists, Albums) as tabs, not duplicate vertical lists on Home
- Optional deeper tabs: Listening clock, discovery, comparison, source coverage, favorites
- Tap item opens proper Artist/Album/Track detail with back/scroll restoration
- "All time" exact sum and historically complete period explained

### Timeline

- Search field at top with auto-focus when launched from Search icon
- Day-grouped listening events; source and timestamp with badges for imported, interrupted or estimated
- Filter sheet: source, date range, artist/track, listened-duration range, captured/imported, skipped vs completed; defaults clear
- Daily summary header and session blocks where identifiable, with direct links to stats/track
- Swipe actions **always** preserve a confirmation or undo path for destructive operations
- Empty/no-results/error/permission-blocked states are distinct and easily recoverable

### Track / Artist / Album detail

- Artwork focus, title/subtitle hierarchy, meaningful provenance and corrected identity controls
- Primary metric pair (plays, listening time) plus last listened and calendar trend
- Timeline entry list, related albums and discovery date; no gratuitous faux-audio playback controls
- Art priority: user choice > verified local > verified remote > fallback monogram, per prior artwork RFC
- Long credits ellipsize but support full details on tap; TalkBack gets meaningful artist/cover descriptions

### Settings / Data hub

- Preferences, Tracking & Permissions, Connected sources, Imports, Backups & Restore, Appearance, Privacy, About
- Each connection has own state and a helpful cause/action: `Connected` / `Needs login` / `Permission missing` / `Last verified`
- Diagnose stale Now Playing in one step; show pending offline plays and last durable save
- Backup health shows last verified backup, destination, retention, checksum, one-tap export and clear restore outcome
- Appearance Studio shows a functional **Home preview**, not a decorative mini music tile

### Profile / Stories

- Personal history recap (days listened, discoveries, favorites, reliable milestones) > artificial gamification pressure
- Spotlight/share data-driven editorial story cards available offline
- Accessibility caption text exported with share cards when possible
- Hide/share control; defaults private

## Component contracts

| Composable | Input | Required behavior |
|---|---|---|
| `JournalTopBar` | title, action capability, destination | Two action slots, safe insets, semantic labels, no route masquerading as search |
| `ListeningNow` | presence+observedAt+source+position/duration | Freshness threshold, no fabricated playback controls; artwork fallback; proportional, frozen-on-pause progress |
| `MetricTile` | label, value, optional live qualifier, details action | Correct units, source caveat, no clipped value at 200% type, ≥48dp tap target |
| `PeriodSelector` | actual selected range, available ranges | Functional period changes, date boundaries, stable across navigation and rotates |
| `ListeningTrend` | localized days, durations, count, selection | Accessible descriptions + alternative table/list; distinguish zero data and missing data |
| `JournalTimelineRow` | saved event, confidence, app source, timestamps | Deterministic title/artist wrap, measured click target, source details on tap |
| `DataStatusChip` | source, age, capture status, health | No false "Live" after expiry, actionable rationale for health states |
| `EmptyState` / `ErrorState` | typed reason, retry/navigation | One purposeful message; never stack multiple loading/blank states |
| `JournalNavBar` | route + destination callbacks | Three names/icons, measured insets, 48dp targets, correct selected semantics, keyboard/foldable adaptability |
| `AppearancePreview` | draft palette/shape/density + *real* sample UI | Same components and tokens as production; Apply/Cancel atomic, no backend tracking |

## Accessibility baseline and QA matrix

**Targets (normative for v2):**
- Body text minimum WCAG AA 4.5:1; large text/icons/user-control boundaries 3:1 where applicable; test composited pixels, not just raw palette colors. Avoid color-only "playing" and data status.
- Touch targets 48dp (Android recommendation), focus rings/semantics and meaningful role/state for interactive custom Compose surfaces.
- Large text up to 200% without overlap, truncation that hides actions or invisible buttons. RTL/multi-lingual dates/localized durations.
- Charts expose numeric content, time scope and explicit zero vs unavailable state to TalkBack.
- Unanimated or low-motion fallback for all animated elements; no steady infinite animation for loading skeleton under reduced-motion policy.
- Android 15/16 edge-to-edge/predictive back: system bars, keyboard/IME insets and bottom navigation must not obstruct content.
- Contrast: test each existing six preset's Dark/Light/OLED; new two canonical editorial tones first; no bright accent text on translucent overlaid background unless result tested.
- Art effects: benchmark low-RAM/battery saver and long scrolling. No media-position event forces whole Home recomposition each second.

**Manual screenshot list for each prototype:**
- 320dp, 360dp and 411dp widths; tall, small and landscape; 1x, 1.5x and 2x system font.
- Dark, Light, System-light, System-dark, OLED; optional user accent modes.
- Empty history, permissions denied, playing, paused, remote stale, imported-only, huge history, long track titles/artists.
- History search keyboard open, scroll to last row with nav visible; permission explainers, backup error and successful backup state.
- Android 8/12/15/16, TalkBack on/off, animation scale 0, locale English/German/French and pseudo RTL.
- Compare actual bitmap/screen recording to signed-off editorial mock, and preserve before/after fixtures in review PR. Source compilation alone is not visual proof.

## Design acceptance test (ship gate)

1. On first launch, a user distinguishes observed music from saved history without asking.
2. Playback title/artist/cover are readable at 320dp with realistic long metadata and navbar not clipping content.
3. Home contains only one presentation of each Today/week summary; tapping the metric loads actual details (#12).
4. Navigation/search/period controls function as they look; nothing resembling a dropdown is static.
5. Personalized shapes and density work on Home, Stats, Timeline, Settings and compact strip, not just old GlassCard.
6. Every locale has usable labels; no raw English status text outside developer diagnostics.
7. Source-specific event counts and time accounting match tested database fixtures, including midnight and source dedup.
8. Revert flag returns to prior UI instantly without changing DB schema, stored preferences, import identity or OAuth tokens.
9. Screenshots are reviewed by a human on actual devices and accessibility scan is satisfactory; no unverified "complete" claims.

## Implementation boundary and migration plan

- Avoid rewriting service, Room or import code in the same PR as visual composition. Build one UI-specific state mapping layer from existing ViewModels/repos; extract pure formatting and date-period logic.
- Use real repository fixtures, not fake stats, outside debug-only design preview. Never imply demo data was recorded.
- Land tokens/components first, then replace Today, Insights and Timeline behind a feature flag, then details/settings/profile and Spotlight.
- Preserve older saved Appearance configurations and safe default while changing theme roles; no abrupt removal of preferences or user-chosen palette.
- Cross-screen creative exceptions allowed only when listed and reviewed. Do not blindly recolor artist imagery or share cards.
- Existing application branding stays intact until written license authorization (see legal gate). After clearance, a separate text/asset migration can happen without touching code-level IDs.
- CI/APK/test dispatch and release remain user-controlled according to `docs/BUILD_AND_RELEASE_POLICY.md`.

**Requested next design deliverables after this planning milestone:** original low-fidelity wireframes, visual prototypes in two contrasting directions, actual rendered screenshot comparison, implementation component checklist and a user-approved canonical Home composition. None are claimed to have been generated yet.
