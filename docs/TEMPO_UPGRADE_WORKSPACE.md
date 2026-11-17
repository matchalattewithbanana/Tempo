# tempo-upgrade — independent development workstream

**Created:** 2026-11-17 from `main` at `07a645f336b02d7a3075abd075accd0b8f262719`.
**Current policy:** All upgrade code and documentation stay on the `tempo-upgrade` branch. Do not merge into `main`, trigger CI/APK builds or publish a release unless the user separately authorizes those actions. Source commits use `[skip ci]` as an additional precaution; workflows on main remain manual-only.

This is a **long-lived branch inside the existing repository**, not a second GitHub repository or a separately licensed/distributed product. Its product vision is a private, data-first listening journal. The existing [repository licence](../LICENSE) restricts distribution of rebranded derivatives; public branding remains unchanged pending rights clearance. Existing Android application ID, signing, persisted history and backup formats are never renamed for visual reasons.

## Reference plans

- [Main product roadmap](PRODUCT_EVOLUTION_ROADMAP_2026.md)
- [Design language and screen contracts](LISTENING_JOURNAL_DESIGN_SYSTEM_RFC.md)
- [Source audit and issue ledger](audits/2026-11-17-additional-audit-cycles-6-to-9.md)
- [Original full audit index](https://github.com/matchalattewithbanana/Tempo/issues/23)
- [Master roadmap issue](https://github.com/matchalattewithbanana/Tempo/issues/38)

## Initial batch: source changes committed, tests NOT executed

| Issue | Initial implementation in this branch | Validation still required |
| --- | --- | --- |
| #6 P0 | Refuse plaintext Spotify fallback after Keystore errors; preserve encrypted source on transient failure, safe credential reads, explicit unavailable status and error in Settings; commit critical auth writes | Inject broken/locked Keystore and bad ciphertext on Android devices; prove no plaintext tokens; OAuth login/logout/restart |
| #8 P1 | Extension 5,000-row retention and seven-day cleanup only auto-prune **synced** records, preserving both queued and failed/retryable outbox items | Run `npm run typecheck` and `npm run smoke`; real IndexedDB >5,000 queued/failed/synced test |
| #10 P1 | Offline replay uses persisted per-event next-retry timestamp, skips non-due events instead of serial sleeps, compatible defaults for old JSON | Run unit suite and benchmark 100–1,000 due events, crash/restart, backoff, concurrent batch/queue paths |
| #13 P1 | Generic database maintenance no longer removes history older than 365 days; legacy cleanup API no-op | Unit/instrumented test old events retained after full maintenance; review any third-party/export retention |
| #25 P2 | Reset Spotify broadcast playing state when **album or duration** changes, even if title/artist remain the same | Run source unit tests and verify real Spotify same-title/album/edition/playback event order |
| #26 P1 | Quarantine malformed pending-play JSON before ordinary queue rewrite; flag recovery warning; refuse writes if durable old data couldn't be read or quarantined | Instrumented disk-full/corrupt preferences and reboot tests; add user-visible recovery UI and two-generation/atomic durable backup |
| #27 P2 | Avoid constructing Profile skeleton infinite animation in reduced-motion mode | Verify animation scale 0 and Power Saver on real device |
| #30 Design foundation | Add isolated editorial `ui/journal/JournalDesignTokens.kt`: warm Ink, Paper and OLED semantic roles, 4–32dp spacing, consistent shape mapping, contrast unit assertions | Review visual prototypes and screenshots. The tokens are **not wired into live screens** until visually approved |

**Status: source-only initial implementation.** Existing GitHub issues stay open until the appropriate tests and device feedback are completed. This document does not claim they are fully fixed in a shipped APK.

## Next implementation order

1. **Finish Gate A / source-of-truth reliability:** #7 WebSocket auth/control trust, #9 service startup, #26 recovery tool, #24 midnight/timezone stats, #17 archive memory limits, #21 streaming Spotify imports, #11 validation pipeline.
2. **Build reproducible fixtures:** pending-queue low-disk/corrupt recovery, Spotify metadata transitions, IndexedDB retention, long-duration midnight and URI source/stale artwork.
3. **Gate B visual prototypes:** one dark editorial Home and one paper-light Home using the new isolated component library; first demonstrate realistic long song names, no data, paused, compact screen and font scaling, not a screenshot borrowed from a competing app.
4. **Migrate screens behind a preview flag:** Today → Insights → Timeline → details → Appearance → Settings/Profile. Remove duplicated cards and magic bottom padding, preserve existing user data and Appearance look imports.
5. **Gate C/D enhancements:** tracking health, provenance, corrections, import/backup center and private listening memories, only after regression coverage.
6. **Identity gate:** request explicit written rights and name/trademark clearance before considering public rebranding; no package ID changes.

## Verification and release boundaries

- New Kotlin/JVM tests and extension smoke tests added to branch source **are not claims they have passed**.
- Prior successful APK builds tested older commits and cannot validate this branch.
- The next Android build and screenshot run requires explicit user authorization; publishing is a distinct separate approval.
- Never delete valuable history as a maintenance side effect, silently weaken encryption, or remove pending events because a soft cap was reached.
- Keep new feature changes small and reversible, and document any schema/database changes and old-client compatibility before merging.

## Branch-specific task completion policy

- **In source:** code and regression tests exist on this branch.
- **Validated:** appropriate tests, device cases and screenshot gate pass against the branch commit.
- **Released:** user explicitly authorizes and publishes a tested artifact. No issue is silently treated as released at the earlier stages.
