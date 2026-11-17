# Tempo → Listening Journal: product evolution roadmap
**Status:** RFC / planning, 2026-11-17. No code, application name, logo, package ID, domain or published product has been changed by this roadmap. The proposed new identity is **not cleared for distribution**. Implement only with explicit task authorization and separate build/release approval.

## Executive decision

Evolve Tempo from a visually inconsistent fork of a background music stats app into a high-trust, local-first **personal listening journal**. Its point of difference is *accurate multi-source listening history, transparent provenance and reflective stories*, not another streaming player or a clone of stats.fm. A new UI should be distinctly authored in-house while retaining Android platform conventions. **Reliability, data ownership and legibility outrank animations, social features and any rebranding.**

The current baseline has 925 tracked repository paths, 554 Kotlin sources, a TypeScript Chrome/Firefox extension, Room/WorkManager, Android MediaSession/NotificationListener and Spotify broadcast/API reconciliation, imports (Spotify, Last.fm, Deezer, YouTube, Musicolet, browser extension), Drive/local backup, a personalized Appearance Studio, Spotlight and share cards.

This roadmap unifies: the visual postmortem (#5), the five-cycle audit (#23), new audit findings (#24-#28), the appearance RFC, prior performance and artwork-identity plans, and the license-gated Breadcrumbs rebrand proposal. It is **not** a list of promised release dates.

## Legal and identity gate — must not be skipped

Repository [LICENSE](../LICENSE) is headed "Tempo Custom License" and expressly prohibits **commercial use** and **distribution of rebranded or white-label derivatives**, despite embedding AGPLv3 terms. [BREADCRUMBS_REBRAND_PLAN.md](BREADCRUMBS_REBRAND_PLAN.md) already flags this. There may also be questions about license-term enforceability requiring professional advice. **Do not interpret a plan as permission.**

- **Allowed planning intent, subject to rights review:** Develop original visual systems and noncommercial improvements while keeping licensed attribution and product identity, prototype a proposed separate brand privately, document continuity requirements.
- **Blocked until written rights authorization and trademark review:** Advertising or publicly publishing a renamed derivative as an independent product, deleting original trademark/logo/credit, launching monetization or subscriptions, relabeling the repository/store presence as an original new product.
- **If permission cannot be obtained:** Continue legitimate Tempo improvements under applicable terms or build a genuinely independent product from suitably licensed or original code, with separate distribution. Do not use copy-paste cosmetic relabeling to bypass the restriction.
- **A future authorized visual/name transition should keep:** Android application ID/signing, Room tables/schemas, old prefs, OAuth callbacks, backup formats, foreground service component name, browser extension pairing protocol and old-data import support until validated migrations say otherwise.
- Working codename "Listening Journal"; "Breadcrumbs" is *not* a settled public name, and brand availability is not established.

## Product principles (nonnegotiable)

1. **Evidence over estimates.** Show observed playback seconds, completed persisted events, imported API history and inferred data as distinct states. No fictitious progress or fabricated play statistics.
2. **A first glance must make sense.** One Now Listening surface, two meaningful Today values, one week trend and a single suggested next action/insight. Deeper analysis lives off Home.
3. **One language, many expressions.** Shared color/type/space/component semantics across everyday screens; artistic Spotlight and share cards may be special, but consistent controls and accessibility still apply.
4. **User ownership.** No silent history deletion; preview changes, back up, export, inspect source, undo/correct where feasible.
5. **Local first.** Offline history is complete and useful. Sync and external APIs enhance reliability, never block the main view.
6. **No fake music player.** Tempo observes other playback apps; mini-player shows source/status and optional "Open in [source app]" but no fabricated pause/next buttons.
7. **Accessible and adaptable.** 48dp Android touch targets, font scaling, contrast tested on composited pixels, screen-reader chart summaries, reduced motion and edge-to-edge insets.
8. **Performance-conscious beauty.** Prefer solid surfaces and small localized artwork atmosphere; no full-screen animated mesh or heavy backdrop blur in lists.
9. **Gradual and reversible.** Feature-flag new UI, preserve prior data and rollback, measure in controlled builds, keep privacy and visual identity changes distinct.

## UX research behind choices

- Google Material 3 Expressive: shape, size, motion, containment and color can improve target discovery, but should articulate hierarchy, not embellish every card. https://design.google/library/expressive-material-design-google-research
- Android Compose edge-to-edge / insets and API 35–36 behavior: https://developer.android.com/develop/ui/compose/system/setup-e2e ; https://developer.android.com/about/versions/16/behavior-changes-16
- Compose adaptive navigation / window-size classes: https://developer.android.com/develop/adaptive-apps/guides/get-started-with-adaptive-apps
- Android 48dp touch target and accessibility semantics: https://developer.android.com/guide/topics/ui/accessibility/apps
- W3C WCAG 2.2 text/nontext contrast, no color-only communication: https://www.w3.org/TR/WCAG22/
- NN/g progressive disclosure; avoid nested, overwhelming secondary controls: https://www.nngroup.com/articles/progressive-disclosure/
- NN/g complex-app design and constrained dashboards: https://www.nngroup.com/articles/complex-application-design/
- stats.fm documents listening clocks, timeline, import confidence and records: https://support-new.stats.fm/hc/en-us/articles/59061730156441-Charts-and-the-stats-screen ; https://support-new.stats.fm/hc/en-us/articles/59096385357593-About-importing-your-streaming-history
- Last.fm's multi-source scrobbling and reports: https://cbsi.my.salesforce-sites.com/lastfm/articles/LastFM/What-is-scrobbling?template=template_lastfm

**Inspiration is feature research, not a licence to reproduce another product's layouts, artwork, wording, assets or distinctive brand system.** Reject clones of Spotify Wrapped or stats.fm.

## Execution order / release trains

Each milestone is a **decision gate**, not a commitment to a date or a background task.

### Gate A — Reliability and ownership (blocking foundation)

**Dependency:** Security/data-loss fixes and validation infrastructure.

| Priority | Work | Existing issue | Exit gate |
|---|---|---|---|
| P0 | Eliminate Spotify plaintext OAuth credential fallback | #6 | Failure injection proves no plaintext secrets; reconnect is recoverable |
| P1 | Require verified signed browser WS control messages | #7 | Unsigned/replayed controls cannot change pairing |
| P1 | Do not evict queued extension events | #8 | All unsynced IDs retained past 5k cap |
| P1 | Remove DAO waits in tracking service lifecycle | #9 | Startup delayed DAO test stays responsive |
| P1 | Drain offline event retries in fair due batches | #10 | 100 eligible events drain promptly without duplicate writes |
| P1 | Preserve corrupt pending-events queue | #26 | Original corrupt bytes quarantined, recoverable |
| P1 | Remove implicit old-history deletion from maintenance | #13 | Lifetime history survives maintenance |
| P1 | Verify calendar boundary listening semantics | #24 / #15 | Today/week totals consistent and time-conserving |
| P1 | Harden full backups/imports and large Spotify inputs | #17 / #21 | Bounded heap/temporary storage; no half-restores |
| P1 | Repair manual lint and crash-rate monitor gates | #11 / #18 | CI/manual monitoring failures visible, not silently green |

**Additional acceptance:** baseline playback minutes reproducible from event log; notification/broadcast/API precedence documented; source-specific dedup verified; no app version/signing/DB migration churn during cosmetic work.

### Gate B — New visual foundation (before replacing Home)

- Create a *single* semantic token graph for background, surface layers, contrast roles, chart series, success/warning/error, focus, border, surface tint, type scale, spacing, shape, density and motion. Do not rely on global fixed dark colors from the original theme.
- Build a small Compose component library: `AppScaffold`, `JournalTopBar`, `ListeningStatus`, `ListeningNow`, `MetricTile`, `PeriodPicker`, `AccessibleTrendChart`, `RankedMediaRow`, `EmptyState`, `ErrorState`, `SourceChip`, `JournalBottomBar`, `Dialog/Sheet`.
- Add actual screen previews/goldens (Home playing/paused/idle, Stats, History, Settings, Appearance) across Night/Day/OLED and 320/360/411dp plus 200% font, RTL, TalkBack.
- Introduce one screen-level background owner to avoid redundant gradients, and measured Scaffold window insets instead of manual magic padding.
- Maintain a **feature-flagged 'Journal UI' preview** isolated from tracking/data code. New design remains unavailable by default until user approves real screenshots.
- Bring Appearance Studio into scope: preview the *actual* components; meaningful compact/comfortable density and corner families; simple looks first, experimental effects subordinate.
- Finish source text localization and large text handling (#28), and remove pointless reduced-motion animation clocks (#27).
- Validate current Light preset text+graphics on actual layered backgrounds, not just base accent color.

**Exit gate:** real on-device light/dark/large-font screenshots signed off, tappable layouts, no cropped headings/overlapping nav, no unlocalized launch-critical screens, no memory/jank regression against baseline.

### Gate C — Core UX migration (the first visible distinct experience)

1. **Home → Listening Today:** two-action top bar (Search, Settings/Profile), *one* truthful compact Now Listening card, Today's listened time and **play count** or unique-track count accurately labeled, interactive seven-day trend with numeric accessible summaries, one rotating grounded insight, one affordance to full Insights. No nested legacy Hero/Todays widget underneath.
2. **Insights (formerly Stats):** clear timeline range chips (Day / Week / Month / Year / All Time and optional custom); summary + ranked tracks/artists/albums; listening clock and discovery trends in dedicated tabs/panes rather than all on Home; compare periods with caveats about missing/imported data.
3. **History → Timeline:** searchable chronological feed with day headers, source icons, play length, gap/session grouping, source filtering, and focused search. Explicit export, delete/undo and correction affordances. Do not merge or delete events implicitly.
4. **Artist/Album/Track:** cover-first editorial header, verified vs inferred art/source provenance, listening totals and timeline, artist/album identity corrections, easy back navigation, safe loading/error states.
5. **Profile and Spotlight:** separate personal history/milestones from motivational gamification. Spotlight becomes an optional, factual story gallery with accessible/shareable static assets. No weekly engagement nags by default.
6. **Settings → Preferences, Tracking, Connections, Data & Backup, Appearance, About:** task-oriented entry points, helpful diagnostics and inline explanations. Keep advanced provider/developer toggles out of novice path.

**Rollout:** per-screen internal preview flag → side-by-side artifact QA → small opted-in tester gate → user sign-off → default per screen. Do not rename icons, app package, OAuth callback or extension while migrating.

**Exit gate:** five core journeys (open app → understand tracking; find last play; inspect a track; explain count; export/restore) complete without confusing states or unnecessary navigation.

### Gate D — Trust-centered quality-of-life features

**First-order, high utility:**
- **Tracking health center:** current permission status, last seen callback, last recorded event, tracking source and latency, warnings if OEM battery kills the service, manual safe retry, self-test guidance. Never imply Play Services or Spotify API are always live.
- **Data provenance inspector:** on every event show Live Android / Spotify broadcast / Spotify API / browser / Last.fm / import and confidence; differentiate *observed seconds* from reconstructed stats. Explain deduplication and missing-source coverage.
- **Conflict review inbox:** flagged duplicates, suspect 60-second imported values, mismatched track identity, unknown content types, wrong artwork; preview fix and revert before mutation. Prior artwork plan: local cover priority, artist identity verification, user overrides.
- **Backup health hub:** last successful *verified* local/Drive backup, size and age, OAuth readiness, restore verification, and explicit export with user control; failure states understandable and actionable.
- **Search everywhere:** artist/album/track/history with source/type/time filters and recent searches stored on device; debounced indexed search and actionable empty states.
- **Import wizard:** provider choice, preview event count/time range, warnings on uncertain timestamps, dry-run duplicate estimate, progress, partial failure report, cancellation-safe checkpoint. No "success" until DB writes/reconciliation finish.
- **Listening calendar:** day heatmap, day detail, playback sessions, start/end and time-of-day. These complement, never duplicate, the single Home weekly chart.
- **Small widgets / notification:** optional today total, last track and tracking status; no unconsented player controls; update only on meaningful change.
- **Privacy and consent:** explicit analytics switches and offline-only data use, clear cache/data deletion effects, app connection removal with predictable local-history retention.

### Gate E — Distinctive, opt-in features after stable data + UX

**Recommended differentiators:**
- **Listening notes:** optional private comments/tags on a *day, moment, session or track*. Local encrypted/private backup behavior explicit; do not turn into social posting.
- **Moments and trails:** automatically find *verifiable* replays, first-discovered artists, forgotten favorites, listening rituals, seasonal changes; reveal methodology and allow hiding stories.
- **Listening rooms:** calm editorial album artwork and personal collections from *history*, not music streaming controls.
- **Year / month stories:** offline generated factual recaps with cards and static export, quiet animations and no third-party share SDK required.
- **Personal listening comparisons:** week-over-week, discovery, repeat affinity, continuity vs novelty, only when baseline coverage is sufficient.
- **Source coverage map:** explain which services are tracked by device, browser and authenticated APIs; show gaps and uncertain minutes.
- **Share kit:** high-resolution, accessible captioned, privacy-aware personal cards with private/anonymous mode and explicit data boundaries.

**Deferred unless user research shows strong need:** accounts, friend/social feeds, collaborative challenges, cloud backend, AI-generated mood claims, location listening maps, fingerprinting entire libraries, cross-platform native desktop clients, playlists playback, ads, monetization. These introduce privacy/rights/maintenance costs and distract from core trust.

### Gate F — Authorized identity transition only after rights clearance

- Build original logo/icon/wordmark explorations independently from Tempo artwork and stats.fm/Spotify assets. Perform trademark and distribution checks before selecting any new name.
- Prepare visible brand tokens and text keys; isolate references to `Tempo` into categories: visible product labels vs compatibility identifiers vs upstream attribution.
- Once authorized, transition through **companion descriptors** (e.g. "Tempo — Listening Journal"), then new approved app display name while retaining legal attribution; **not** a sudden independent-logo swap.
- Preserve users' data and update channel: package ID, signing, Room, preferences, OAuth schemes, historical backup names and extension pairing. Separate upgrades only if original rights demand them and migration is user-initiated.
- Publish migration/rollback instructions only at the time an explicitly authorized release is approved. Never imply the rebrand is already licensed.

## Information architecture and navigation policy

Keep three primary destinations during the redesign:
1. **Today** (existing Home route): now listening, today metrics, week chart, notable insight and shortcuts.
2. **Insights** (existing Stats route): top items, time-range comparisons, deeper charts, listening clocks, discovery, records.
3. **Timeline** (existing History route): exact events, filters, source, sessions and corrections.

Top-level Search should open a real search field (not merely History's unfiltered list). Profile/Settings remain top-bar destinations. Large screens use navigation rail + two-pane media/detail layouts; compact landscape, tablets and foldables adapt from window-size class, not device-model checks. Navigation must respect predictive back and Android system bars.

## Metrics and event truth specification

| Term in UI | Definition / caveat |
|---|---|
| Play count | Count of persisted eligible listening events after dedup; not a distinct-track count |
| Tracks explored | Distinct canonical tracks in selected period |
| Listening time | Sum of eligible observed listen durations; disclose reconstructions and cross-midnight policy |
| Live session | Provisional seconds accrued while actively playing; may be revised upon save |
| Completed play | Persisted event; today's historic sums shouldn't double-add the live estimate |
| Source | Capturing system or import provider; must be visible near diagnostic details |
| Data confidence | Observed vs verified/reconciled vs inferred/reconstructed; no arbitrary single numerical confidence score |
| Empty period | No captured eligible events **in this period**, which is different from no app permissions or all-history empty |

Before adding any new cards/insights, write the SQL/query contracts plus synthetic test fixtures. Test repeat/replay semantics, pause, seeking, 2× playback speed, podcasts filtering, multi-source overlap, timezone and DST boundaries, device restart, and imports from older app formats.

## UX acceptance journeys (manual+test)

1. **First run with no permission:** short value statement, progressive permission request, clear disabled/error state and source list; app still navigable.
2. **Currently playing but not persisted:** correct artwork/track title source, moving elapsed session, provisional status; Today stays correct.
3. **Paused and backgrounded:** no fake playing state, app resumes gracefully and does not create phantom plays.
4. **One-minute / unknown-length track:** counted real listening time; uncertainty surfaced; stats update after save with correct units.
5. **Week/year change:** Today and period charts refresh without new playback; prior events retain local date identity.
6. **Search/dedupe:** user can find and inspect cross-source events without deleting valid repeats.
7. **Large restore:** bounded memory, cancel and retry, validation report, old backup readable.
8. **Battery restriction / OEM kill:** obvious tracking health warning with actionable paths and no false guarantees.
9. **Light/OLED:** screens and text legible including overlays, charts, bars, system UI and translated 200% text.
10. **Visual personalization:** appearance preview matches actual UI; no reset of event history, playback tracking or OAuth tokens.

## Guardrails and monitoring

- Track **consistency**, not vanity analytics: time to first valid callback; event capture→DB p50/p95 latency; offline queue age and unsynced count; daily vs All Time reconciliation; backup verification success; import batch peak heap and correct count; ANR and UI frame jank.
- Keep metrics local unless users consent to minimal diagnostic analytics. Logging must omit auth secrets, raw listening history and high-precision identifiers.
- Use Macrobenchmark/Baseline Profiles, Compose layout inspection, snapshot screenshots, SQLite migration tests and browser fake-socket tests.
- Performance targets are **budgets for future measurement**, not measured achievements: no persistent nav overdraw, no per-position DB/HTTP reads, no application-level animation loops in reduced-motion mode, bounded capture/import heap and no regression against pre-change p95.
- Governance: short feature RFC → tests → manual build authorization → real screenshots + data fixture review → user sign-off → separately authorized beta/release.
- Respect [BUILD_AND_RELEASE_POLICY.md](BUILD_AND_RELEASE_POLICY.md). No automated CI/APK/publishing on push.

## Delivery backlog, dependency gates and exclusions

**Blocking:** #6 #7 #8 #9 #10 #11 #13 #16 #17 #18 #21 #24 #26 and visual consistency #5.

**Follow-on:** #12 #15 #25 #27 #28 #14 #19 #20 #22 and artwork identity RFC; several are subsumed into the epics created with this roadmap.

**Success is:** a coherent, reliable, original-feeling *listening journal* that users trust and want to explore. It does **not** depend on an immediate new name, copying a competitor, adding social features or turning Tempo into a streaming player.

**Review owners / status:** Planning only. New issues and docs are the deliverables of this session; no code implementation, test run, APK or release has been authorized.
