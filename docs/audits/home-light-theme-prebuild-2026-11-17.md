# Home / Light Theme prebuild audit — 2026-11-17

**Scope:** Source review of the post-Beta Home redesign, Compose appearance palette and primary surface consistency.  
**Constraints:** Build an APK for testing; **do not create a GitHub release or publish to Play Store**.

## Findings and fixes

| Priority | Finding in current code | Corrective source change |
| --- | --- | --- |
| P1 | All six Light presets shared just two background recipes (warm/cool); themes lacked independent light identities | Per-preset paper, surface, raised surface, border, muted and primary text palettes with 7:1 main text and 4.5:1 secondary/button contrast targets |
| P1 | Shared GlassCard still honored obsolete dark/frost surface arguments in Light, leading to dark islands and invisible borders | Map legacy dark surface/frost names to active light role surfaces and semantic borders |
| P1 | Detailed Home cards / full-screen Today overview used fixed dark-only text and background tokens | Convert to semantic text, border and surface roles; make the Today overlay use light surfaces and suppress dark artwork atmosphere behind light text |
| P2 | Daily cadence activity used fixed bright teal and white-placeholder bars in Light, making low-activity content hard to distinguish | Resolve bars from active theme primary and on-surface palette |
| P2 | Detail timelines selected a legacy fixed foreground based solely on artwork brightness | Keep artwork selection for dark visuals but use palette text for Light/System-light |
| P2 | Song/album/profile screens retained a few dark-only muted labels | Convert affected text and input borders to semantic Material colors |
| P2 | Full-screen artwork wash could lower contrast for Light over dark/saturated covers | Strengthen paper-colored scrim in Light and keep original dark treatment unchanged |

## Existing Home correctness reviewed

- Daily activity uses Room `StatsDao.getDailyListening()` with local-date aggregation and an explicit `THIS_WEEK` time range. The pure week mapper pads missing days to zero and prevents future-dated listening from appearing in the chart.
- Listening time is calculated from stored event durations; distinct tracks and play events are intentionally separate metrics.
- The selected appearance determines Inline/Compact/Hidden playback UI; the live player uses local presence and cached artwork, not an extra API poll.
- The default Home keeps detailed stats and Spotlight behind **More insights**, with optional Room queries loaded only on expansion.
- Background tracking services, OAuth configuration, database schema and backup formats were not changed by this audit.

## Validation status

The audit adds automated light-palette assertions for each preset: distinct backgrounds; primary text against background/surface/elevated surface; secondary text against surface; primary accent against surface; and button text against accent.

The explicitly requested, **non-publishing** one-shot GitHub Actions workflow **passed every gate**: unit tests, Android lint, debug APK assembly, APK signer verification, SHA-256 hashing, and Android emulator instrumentation. Run: https://github.com/matchalattewithbanana/Tempo/actions/runs/44062427544. Artifact: `tempo-home-light-audit-debug-apk`, 30-day retention. APK SHA-256: `61acd4cfda57ed60780b03aa2e20c0d6b4f61bb88e617b2cf389697b88c8d0d4`.

The temporary workflow has been removed from its build branch following successful validation. The normal `main` CI and APK workflows remain manual-only. No GitHub release was created.

Physical-device visual, font scaling/TalkBack, background playback endurance, Google Drive/OAuth configuration and install-over-existing-APK verification still require tester feedback. No production/stable release is authorized in this pass.
