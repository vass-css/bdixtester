# Changelog — BDIX Tester

All notable changes to **BDIX Tester** (`com.jubayerhossain.bdixtester`) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and release tags follow `v<versionName>` parity with `app/build.gradle.kts`.

## [Unreleased]

### Added

- GitHub Release preparation documentation suite under `docs/release/`, `docs/privacy/`, `README.md`, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and `.github/` issue/PR templates.
- Dedicated `.github/workflows/dependency-audit.yml` and `.github/workflows/release-integrity.yml` workflows with immutable SHA-pinned GitHub Actions.
- APK-only release packaging pipeline in `.github/workflows/android-release.yml` generating `BDIX-Tester-v<versionName>.apk`, `SHA256SUMS.txt`, `release-manifest.json`, and `RELEASE-NOTES.md` after `apksigner` and `aapt2` verification.

### Changed

- Removed the Python catalog-verification tooling; catalog checks now run through Kotlin tests and Android CI.
- Updated `LICENSE` copyright attribution to `Jubayer Hossain`.
- Replaced placeholder community search URL in `SettingsScreen` with a direct `mailto:jubayerhossainbarisal@gmail.com` Developer Support action (EN/BN localized).
- Fixed Android Lint errors across `VerificationNotificationHelper` (`POST_NOTIFICATIONS` inline check), `BdixTesterAppRoot`, and `CatalogGovernanceScreen` (`LocalResources.current` synced with `MainActivity` locale override).
- Relocated `lint { ... }` block cleanly under `android { ... }` and configured `maxHeapSize = "2g"` for Robolectric test execution in `app/build.gradle.kts`.
- Hardened `.gitignore` and `.gitattributes` for Android release safety.

### Fixed

- Fixed scroll/async timing assertions in `CatalogGovernanceUiTest` so bottom-sheet DNS/RDAP sections and Room Archive/Restore flows pass reliably under Robolectric SDK 36.

## [1.0] - Initial Catalog Governance & Release Baseline (`versionCode = 1`)

### Added

- **Curated Catalog & Bangladesh OTT Directory:** Preserved `752` curated BDIX endpoints and `26` Bangladesh/regional OTT platform entries verbatim with deterministic `builtin:<index>` identifiers and offline invariant verification (`tools.catalog_verifier`).
- **Room Persistence & Recovery:** `BdixDatabase`, non-destructive migrations, `RoomCatalogRepository`, `SettingsDataStoreRepository`, and startup interrupted-scan recovery.
- **Network & Speed Engine:** `HttpEndpointTester`, `FtpControlTester`, `ScanCoordinator` (bounded concurrency, request coalescing), `SpeedTestEngine` (opt-in bounded download probe), `DohDnsResolver`, and `RdapLookupClient`.
- **Catalog Governance & Restore:** Multi-layer evidence viewer (`GovernanceEvidenceSheet`), non-destructive archival with explicit confirmation dialog, 1-tap restore, ISP probe JSON import, and optional `CatalogVerificationWorker` summary notifications (`POST_NOTIFICATIONS` on Android 13+).
- **Signed Remote Sync:** Optional P-256 ECDSA (`SHA256withECDSA`) verification over exact raw UTF-8 manifests (`CatalogSignatureVerifier`), anti-rollback checks, and `StrictWifiValidator`.
- **Material 3 & Bilingual UI:** Four curated Light/Dark theme presets, adaptive navigation rail/bar, EN/BN runtime language switching, SAF exports with CSV formula neutralization, and custom adaptive launcher icon.
