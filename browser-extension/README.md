# Tempo browser companion

Records supported web-player observations and syncs queued plays to a paired Tempo Android app. This is the browser companion for [this fork](https://github.com/matchalattewithbanana/Tempo), not a music streaming client.

## Install from GitHub

Beginning with Tempo 4.9.9, each GitHub release supplies both browser ZIPs
alongside the matching Android APK. Verify the chosen archive against
`SHA256SUMS.txt`, then extract it so `manifest.json` is at the root of the folder.

- **Chromium** — Enable Developer mode on the browser's Extensions page, choose
  **Load unpacked**, and select the extracted folder.
- **Firefox 140+** — Open `about:debugging`, choose **This Firefox → Load Temporary
  Add-on**, and select the extracted `manifest.json`. The unsigned ZIP loads
  temporarily and Firefox removes it after a restart.

These are GitHub packages. Marketplace distribution has its own review and signing.
[Latest stable release](https://github.com/matchalattewithbanana/Tempo/releases/latest).

## Build from source

Use Node.js 20:

```bash
npm ci
npm run typecheck
npm run smoke
npm run clean
npm run build:all
```

Load `dist/chrome/` through the Chromium extension developer page. For Firefox, load/test the built `dist/firefox/manifest.json` using the browser's temporary-extension tooling. The generated Firefox manifest targets Firefox 140+. This repository tracks selected generated package files under `dist/`. Make source edits under `src/` and use `build:chrome` or `build:firefox` to regenerate the corresponding package.

## Pairing and use

Open the popup's Pairing section and follow its QR workflow with Tempo's **Settings → Sync with Desktop** screen. Manual recovery accepts a phone IP; port 8765 is managed by the current popup flow. Both devices should use a trusted Wi-Fi/LAN/hotspot.

Review host-permission prompts. A grant for one endpoint may not cover an address changed by discovery. Keep pairing tokens private; remove pairing and reconnect if they may have been exposed.

Supported-site detection is defined by `manifest.json` and `src/background/site-detect.ts`, including dedicated music players and opt-in ordinary YouTube channel rules. A supported domain does not guarantee that every page/player exposes usable timing. Inspect the popup's status, queue and failed records if sync stops.

## Data and protocol

The tracker combines observed media state with bounded timing rules. It can queue offline, record pause/seek/replay evidence and retry eligible transmissions. No claim of perfect timing or zero resource cost is made.

HTTP requests use timestamp, nonce and HMAC signatures; supported payload paths also use encrypted envelopes. Real-time WebSocket controls require signed acknowledgements/messages in the extension, but phone-peer interoperability needs separate validation. These mechanisms are not a blanket TLS or local-subnet security guarantee. See [privacy](PRIVACY.md) and [security reporting](../SECURITY.md).

Queued/failed records are preserved during automatic retention cleanup. Review storage growth during long offline periods and export before deleting records. Removing pairing does not erase previously synced phone history.

## Source map

| Path | Responsibility |
| --- | --- |
| `src/content/` | Page/player probes |
| `src/background/tracker.ts` | Per-tab playback state machine |
| `src/background/storage.ts` | IndexedDB and browser storage |
| `src/background/sync.ts` | Batch sync, retry and address recovery |
| `src/background/websocket.ts` | Real-time channel state |
| `src/shared/security.ts` | Request signing/envelopes |
| `src/popup/` | Status, pairing, queue and settings UI |

Existing checks cover types and smoke fixtures; they do not certify every browser/site/network or accessibility scenario. Follow the [root contributor guide](../CONTRIBUTING.md) and retained [license](../LICENSE).
