# Repository architecture

## Top-level ownership

| Path | Responsibility |
| --- | --- |
| `app/src/main/java/me/avinas/tempo/` | Android production source |
| `app/src/main/res/` | Strings, icons, layouts and Android configuration resources |
| `app/src/test/` | Local JVM tests and fixtures |
| `app/src/androidTest/` | Android/Room/platform integration tests |
| `app/schemas/` | Room schema history; preserve for migration validation |
| `browser-extension/src/` | Content probes, tracking/sync background code and popup UI |
| `gradle/` | Wrapper, toolchain and dependency catalog |
| `.github/` | Manual build/validation workflows, templates and maintenance scripts |
| `docs/` | Current guides, references, audit records and release notes |

## Android package map

| Package | Boundary |
| --- | --- |
| `ui/` | Compose screens/components, appearance/theme and screen ViewModels |
| `data/local/` | Room entities, DAOs, migrations and local query helpers |
| `data/repository/` | Persistence orchestration, statistics/caches and refresh coordination |
| `data/remote/`, provider packages under `data/` | Provider auth/APIs, imports and enrichment |
| `data/reset/` | OS-owned local reset request and observable failure/retry state |
| `domain/` | Existing insight/domain logic |
| `service/`, `receiver/` | Playback observation, session recovery and platform entry points |
| `worker/` | Deferrable import/enrichment/backup/maintenance work |
| `desktop/` | Phone-side companion pairing/discovery/HTTP ingestion |
| `widget/` | Home-screen widget rendering/update work |
| `di/`, `utils/` | Dependency wiring and shared helpers |

The current tree is an incremental MVVM/repository architecture, not a completed strict module split. Keep provider logic out of Composables and blocking I/O out of lifecycle callbacks. Reuse current repositories before introducing parallel implementations.

## Change boundaries

- Treat package/application ID, OAuth callbacks, Room file/schema, serialization keys and signing identity as compatibility contracts.
- Keep source observations separate from committed listening events and imported estimates.
- Make writes durable before acknowledging them; performance improvements must preserve pending events.
- Scope screen work to visibility and coordinate background ownership. See the [performance plan](https://github.com/matchalattewithbanana/Tempo/issues/85).
- Use theme roles and shared accessible controls. See the [UI audit](https://github.com/matchalattewithbanana/Tempo/issues/68) and [Profile proposal](https://github.com/matchalattewithbanana/Tempo/issues/74).

## Focused backup and account boundaries

`data/importexport/BackupSnapshotWriter.kt` owns the Room transaction and streamed JSON snapshot. `ImportExportManager` owns operation progress, temporary-file lifecycle, ZIP/image I/O and restore orchestration. `RestoreArtworkStaging` owns only the current restore's artwork files.

`data/remote/spotify/AccountSessionGate.kt` serializes short credential/state commits and invalidates obsolete token calls. The refresh mutex coalesces refreshes; it is not held by disconnect while waiting for network I/O.

## Tracking boundaries and organization backlog

`service/PlaybackSession.kt` owns per-play accounting, pause/seek state and recovery snapshots.
`service/TrackingRuntimePolicy.kt` owns package eligibility, manual content corrections,
podcast/audiobook filtering and duration policy from immutable Room/preference snapshots.
`service/PlaybackTransitionPolicy.kt` owns pure same-track/replay/new-session decisions.
`service/LocalMediaMetadata.kt` is the normalization boundary for MediaSession and notification
metadata. Rich artwork extraction is kept off the callback hot path.

`MusicTrackingService` remains the Android adapter/orchestrator: it owns notification and
MediaController callbacks, synchronization, active-session membership, foreground lifecycle and
scheduling persistence. New business branching should not be added directly to that service when
it belongs to one of the policy/session models above.

The statistics layer still has a broad contract: `StatsRepository` exposes more than sixty
operations spanning overview, rankings, history, details, image enrichment, editing and cache
control. `RoomStatsRepository` has removed duplicated artist-detail assembly and dead legacy
helpers, but capability-sized repository interfaces remain architectural debt. Split that contract
only together with DI/consumer migration; adding facade-only subinterfaces would move declarations
without reducing dependency breadth.

Large remaining files should be reduced by information-hiding boundaries and behavior tests, not
by arbitrary line-count splits. A file split is not considered an architectural improvement when
callers still need to understand the same mutable state or policy details.

Generated `build/`, `.gradle/` and `node_modules/` output stays outside version control. The extension keeps selected generated Chrome and Firefox files under `browser-extension/dist/`. Rebuilding those packages in the checkout can update the committed copies. Historical audits describe their original revision.
