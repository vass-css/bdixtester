# BDIX Tester

A privacy-first, open-source Android utility built with Kotlin, Jetpack Compose, and Material 3 to discover, verify, and measure reachability to Bangladesh Internet Exchange (BDIX) servers, FTP portals, media directories, and Bangladesh OTT platforms directly from your active network connection.

- **Package ID:** `com.jubayerhossain.bdixtester`
- **Minimum Android:** Android 7.0 (API 24)
- **Target / Compile SDK:** Android 16 (API 36)
- **License:** MIT (`Copyright (c) 2026 Jubayer Hossain`)
- **Distribution:** Official [GitHub Releases](../../releases) (Zero cost, zero ad SDKs, zero analytics)

## Key Features

- **Curated & Verbatim Endpoint Catalog:** Includes `752` built-in BDIX entries (`FTP & Files`, `Movies`, `Live TV`), `26` official Bangladesh and global OTT directory links, plus full support for user-defined custom HTTP, HTTPS, and FTP endpoints.
- **Honest Connectivity Diagnostics:** Runs Quick Scan (`2.5s` budget) and Full Scan probes with per-host bounded concurrency (`1–5`, default `3`), exact-URL request coalescing, stage-by-stage timing (`DNS`, `TCP`, `TLS`, `HTTP`/`FTP`), and clear status badges (*Reachable from current network*, *Restricted*, *Unreachable*, *Unsupported*).
- **Opt-In Bounded Speed Test:** Explicit-consent HTTP/HTTPS throughput measurement (`2–20 MiB` cap, `Accept-Encoding: identity`) with cross-host redirect confirmation dialogs.
- **Signed Remote Catalog Governance:** Optional HTTPS manifest sync verified via detached `SHA256withECDSA` (P-256) signatures, strict UTF-8 validation, anti-rollback sequence checks, and multi-resolver DoH + RDAP catalog governance tools.
- **Local-First History, Favorites & Exports:** Room-backed favorites, pinning, retention-bounded history, and one-tap CSV/JSON/TXT exports using Android's Storage Access Framework (`CreateDocument`) with CSV formula-injection protection.
- **Localization & Visual Themes:** Full English and Bangla (`values-bn`) language support with runtime switching plus four Light/Dark Material 3 palettes (*Midnight Indigo*, *Graphite & Amber*, *Deep Emerald*, *Warm Premium*).

## Screenshots

> Physical-device and emulator screenshots are pending final release capture on Android hardware. No placeholder or mock screenshots are published in this repository.

Once captured after signed release verification, screenshots for Home (Orbital Radar), Categorized Servers & OTT Directory, Scan Diagnostics, and Settings (English/Bangla & Material 3 themes) will be added under `docs/assets/`.

## Download Instructions

1. Open the official **GitHub Releases** page for this repository (`../../releases`).
2. Select the latest release tag (`vX.Y.Z`).
3. Download these verification and installer files:
   - `BDIX-Tester-vX.Y.Z.apk`
   - `SHA256SUMS.txt`
   - `release-manifest.json`
4. Verify the APK checksum before installing (see **SHA-256 Verification** below).

Only trust APKs published under official tagged GitHub Releases matching the signing certificate SHA-256 fingerprint recorded in `release-manifest.json`.

## SHA-256 Verification Instructions

### Linux / macOS

```bash
sha256sum -c SHA256SUMS.txt
```

Compare the output hash and inspect the expected signing certificate SHA-256 fingerprint inside `release-manifest.json`.

### Windows (PowerShell)

```powershell
Get-FileHash .\BDIX-Tester-vX.Y.Z.apk -Algorithm SHA256
Get-Content .\SHA256SUMS.txt
```

### Android SDK Verification (Optional, for Developers)

```bash
apksigner verify --print-certs BDIX-Tester-vX.Y.Z.apk
```

## Installation Instructions

1. **Download Official Assets:** Obtain `BDIX-Tester-vX.Y.Z.apk` and `SHA256SUMS.txt` from the official GitHub Releases page and confirm the SHA-256 checksum matches.
2. **Allow Per-Source Installation:** When opening the APK on Android, grant permission solely for the browser or file manager you used to download the file (*Install unknown apps*) if Android prompts for it.
3. **Install & Play Protect Notice:** Because GitHub Releases APKs are distributed outside Google Play, Google Play Protect may display a standard unknown-developer or scanning prompt during sideloading. Allow the verified check/scan to complete; **never disable Play Protect or Android device security settings globally**.
4. **Safe Future Updates:** Always download newer versions from the same official GitHub Releases page. Android automatically verifies that the update uses the identical package ID (`com.jubayerhossain.bdixtester`) and developer signing certificate before replacing your existing installation while preserving local Room history, favorites, and settings.

## Android Compatibility

| Attribute | Value |
| :--- | :--- |
| Package ID | `com.jubayerhossain.bdixtester` |
| Minimum SDK | API `24` (Android 7.0 Nougat) |
| Target / Compile SDK | API `36` (Android 16) |
| Native Libraries | None (Pure Kotlin / Jetpack Compose) |
| Languages | English (`en`), Bangla (`bn`) |

## Release History

- **`v1.0` (Preparation Baseline):** Core BDIX diagnostics, `752` curated BDIX + `26` OTT directory entries, Room persistence, WorkManager sync/verification, ECDSA P-256 manifest verification, and English/Bangla Material 3 UI. Full phase notes are recorded in [`docs/CHANGELOG.md`](docs/CHANGELOG.md).

## Permissions Explanation

| Permission | Purpose | Scope |
| :--- | :--- | :--- |
| `android.permission.INTERNET` | User-initiated HTTP/HTTPS/FTP probes, opt-in speed tests, optional HTTPS catalog sync, and DoH/RDAP governance checks. | Runtime network I/O only on explicit user/scheduled action. |
| `android.permission.ACCESS_NETWORK_STATE` | Read-only transport inspection (Wi-Fi vs Cellular vs Offline) and strict Wi-Fi-only guard for remote catalog sync. | Local OS status only; zero Wi-Fi SSID or BSSID collection. |
| `android.permission.POST_NOTIFICATIONS` | Optional completion summary notification on Android 13+ after background catalog verification runs. | Requested gracefully with runtime fallback if denied. |

Jetpack WorkManager also merges standard internal scheduling permissions (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) solely for user-enabled background tasks. The app requests **zero location, contacts, camera, microphone, or broad storage permissions**.

## Privacy Information

- **Local-First Architecture:** Favorites, custom servers, scan history, and settings stay strictly on your device in Room and Jetpack DataStore.
- **Zero Trackers or Accounts:** The MIT GitHub Release includes zero accounts, zero analytics SDKs, zero behavioral telemetry, and zero advertising SDKs.
- **Browser Handoff:** Opening any server link delegates to your device's external web browser (`Intent.ACTION_VIEW`), never an embedded WebView.
- **Full Documentation:** Read [`docs/privacy/PRIVACY_POLICY.md`](docs/privacy/PRIVACY_POLICY.md) and [`docs/privacy/PRIVACY_AND_DATA_HANDLING.md`](docs/privacy/PRIVACY_AND_DATA_HANDLING.md).

## Known Limitations

- **ISP Peering Dependence:** BDIX FTP, Movie, and Live TV servers are frequently restricted to specific Bangladesh broadband ISPs or private peering subnets (`RFC 1918`). Reachability on one ISP does not guarantee reachability on mobile data or another ISP.
- **Physical-Device Verification:** Automated tests in this repository run under JVM and Robolectric (`SDK 36`); physical-device testing across diverse Android OEMs and live Bangladesh broadband connections requires manual hardware verification.
- **External Catalog Endpoints:** BDIX Tester acts strictly as a connectivity diagnostic tool and link directory. It does not host or stream media, and some historical ISP/media domains in community BDIX lists carry copyright/availability caveats documented in [`docs/privacy/PRIVACY_AND_DATA_HANDLING.md`](docs/privacy/PRIVACY_AND_DATA_HANDLING.md).
- **Remote Signed Catalog Key Prerequisite:** `TrustedKeyRegistry` currently holds a placeholder public SPKI; configure an operator-generated ECDSA P-256 keypair before hosting a live remote feed.

## Support and Issue Reporting

- **Bug Reports & Feature Requests:** Open an issue using the templates under [`.github/ISSUE_TEMPLATE/`](.github/ISSUE_TEMPLATE/bug_report.yml).
- **Security Vulnerabilities:** Do not post publicly; follow [`SECURITY.md`](SECURITY.md) or email `jubayerhossainbarisal@gmail.com`.
- **Contributions:** Review [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).

## License

Released under the [MIT License](LICENSE) — `Copyright (c) 2026 Jubayer Hossain`.
