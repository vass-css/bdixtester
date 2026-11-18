# Changelog — BDIX Tester

## [0.2.0] - 2026-11-18 (Phases 4–11)
### Added
- **Built-In Catalog Import:** Imported all `369` supplied entries verbatim in `FtpLinks.kt` (`0..368`, `365` unique URLs, `4` exact duplicates preserved) with deterministic `builtin:<index>` IDs and programmatic source-to-database verification. Documented the `369` vs `371` discrepancy in `ENDPOINT_CATALOG_AUDIT.md`.
- **Phase 4 Persistence:** `BdixDatabase` (`version = 2`, exported schema `2.json`, non-destructive `MIGRATION_1_2`), Room DAOs, `RoomCatalogRepository`, `SettingsDataStoreRepository`, and startup interrupted-session recovery.
- **Phase 5–6 Network & Scan Engines:** `HttpEndpointTester` (HEAD + bounded `Range` GET fallback, manual redirect & HTTPS-downgrade protection, per-call stage timings via `EventListener`), `FtpControlTester` (RFC 959 control probe + explicit `ftps://` unsupported handling), and `ScanCoordinator` (bounded concurrency default `3`, `1` per host, exact-URL request coalescing with `scanRunId` + `probeGroupId`).
- **Phase 7 Opt-In Speed Test:** `SpeedTestEngine` (`Accept-Encoding: identity`, 32 KiB bounded streaming loop, monotonic `System.nanoTime()`, 5 `SpeedTestOutcome` states, cross-host redirect user confirmation dialog).
- **Phase 8 Signed Remote Catalog Sync:** `CatalogSignatureVerifier` (P-256 ECDSA over exact raw UTF-8 manifest bytes, strict UTF-8 & BOM rejection), `RemoteCatalogSyncManager` (anti-rollback, 304 cache reuse, SHA-256 digest check, `StrictWifiValidator`, atomic Room swap), and WorkManager `CatalogSyncWorker`.
- **Phase 9–10 User Data, SAF Export & Localization:** Favorites/pinning, Custom Server CRUD, History filtering & retention pruning, CSV formula neutralization + canonical JSON/TXT exports via SAF, HTTP/HTTPS-only `ExternalBrowserHelper`, full Bangla (`values-bn`) translations + in-app language switcher, and LTR direction wrapping for URLs/IPs/metrics.
- **Phase 11 CI & Documentation:** Updated `.github/workflows/android-ci.yml` and added implementation, testing, security, privacy, and release documentation.

## [0.1.0] - 2026-11-18 (Phases 1–3)
### Added
- Project identity, research docs, Phase 1–2 architecture specs (`ADR-001..013`), `UrlParser`, Material 3 theme, adaptive launcher icon, and navigation shell.
