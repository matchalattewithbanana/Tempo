# Home redesign — compact listening dashboard

**Date:** 2026-11-17
**Target branch:** `main`
**Build / release policy:** Source-only updates. No APK assembly, lint, unit-test execution, emulator CI, publishing or release/tag creation during this change.

## Implemented

The initial Home surface is a unified, theme-aware **Your listening** dashboard with:

- A header with accessible History/Search, Profile and Settings destinations.
- An embedded version of the existing real-time `LiveNowPlayingCard` with artwork, playback status and progress, only when the appearance preference is **Inline**. The **Compact** mini strip above navigation and **Hidden** option remain mutually exclusive with this card.
- Two small metrics based on the existing Room `ListeningOverview(TimeRange.TODAY)`: actual listened time and the count of **distinct tracks** played today. These are not preview/demo values. The today card opens the existing detailed today overlay where available; the tracks card opens History.
- A fixed **Monday–Sunday calendar-week** activity chart, with zero bars for missing/future days; data comes from `getDailyListening(TimeRange.THIS_WEEK, 7, withLeeway = false)` and never from API polling. The current day uses the active appearance accent. Individual bars expose weekday, real listening time and play count to accessibility services.
- **More insights** expands the existing optional cards and period selector. **View all** goes to Stats. Expansion retains the user's persisted Home preference, and individual detailed sections retain saved Appearance order/visibility.
- An empty-library help state still appears when appropriate. Pull-to-refresh, tracking-permission guidance, walkthroughs and consent/review overlays remain reachable.

## Query/performance scope

- The standard selected range is the current week, so the seven-day chart **reuses** the same Room query as the detailed period view: no duplicate week read on the common path.
- Period comparison, active-day/peak-day summaries, top songs/artists, audio-feature aggregate, detailed today stats and profile identity are now fetched only when the corresponding detailed cards are expanded.
- The base daily overview, overall data-availability check, weekly history, first-listen date and essential preferences are still fetched normally.
- The live playback card keeps its existing 1 Hz position ticker in its own composable, with no network polling added to the Home screen.

## Not included

The chart is view-only, not a draggable search/selection control. The Search icon opens History, which already has a search field; it does not claim to auto-focus the search field. The screenshot's illustrative listening numbers and bar heights are intentionally **not** hardcoded. No new backend, database migration, audio tracking algorithm, streaming service connection, package identity, or release artifact is introduced.

## Source review / pending validation

- [x] Kotlin source delimiter review
- [x] Verified distinct-track versus total-play semantics
- [x] Week projection regression tests added for Monday–Sunday alignment, future dates, missing days, negative totals and duration truncation
- [x] Localized dashboard labels for the six existing locales
- [ ] Compile, lint, unit-test execution and emulator tests — **not run** (explicit no-build/no-CI instruction)
- [ ] Physical 320dp / 360dp / tablet / landscape screenshots; large fonts and TalkBack
- [ ] Verify all six styles, light/system/OLED, colored wallpaper and cached album art
- [ ] Real playback (resume, pause, seeking, stale status, missing cover, screen off/on)
- [ ] Verify manual refresh, Today overlay, search/history/profile navigation and persisted expansion state
- [ ] Check stats counts and week boundaries around local midnight, Sunday/Monday and timezone changes

The beta previously published is unchanged; these source improvements will be tested in a separately authorized build.
