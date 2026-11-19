# Repository Audit Report — BDIX Tester

**Audit Scope**: Complete source repository, bundled catalogs, canonical JSON datasets, Android persistence/networking/UI layers, Python scripts, and GitHub Actions workflows.
**Git Status Note**: Container workspace has no `.git` metadata (`git status` unavailable); all counts and capabilities below were verified directly from files on disk.

---

## 1. Catalog Inventory & Exact Counts

### A. Bundled Android Curated Catalog (`BundledBdixCatalog` / `BuiltInCatalog`)

| Source File | Raw Entries | Category Breakdown | Notes |
|---|---:|---|---|
| `BundledFtpCatalog.kt` | 328 | 328 `FTP_FILES` | Primary ISP FTP & Media endpoints |
| `BundledFtpCatalogPart2.kt` | 214 | 214 `FTP_FILES` | Additional ISP FTP endpoints |
| `BundledMediaCatalog.kt` | 210 | 121 `MOVIES`, 89 `LIVE_TV` | Movie & Live TV endpoints |
| **Total Kotlin Entries** | **752** | **542 FTP, 121 Movies, 89 Live TV** | Zero canonical duplicates after `UrlParser.canonicalKey` |

- **Raw JSON Source Reference**: `BuiltInCatalog.RAW_JSON_SOURCE_COUNT = 758` (`BuiltInCatalogAuditTest` verifies 752 curated entries, 6 deduplicated cross-source overlaps, 0 discrepancies, 0 invalid URLs).
- **Protocol Distribution (752 curated)**: `550 HTTP`, `202 HTTPS` (`0 FTP`, `0 FTPS`, `0 Unsupported`).
- **Host / Network Classification (752 curated)**:
  - **Hostname endpoints**: `571` (`540` unique `host` values normalized without `www.`), `30` hosts shared across multiple distinct ports/paths/schemes.
  - **Public IPv4 endpoints**: `130`.
  - **Private RFC1918 IPv4 endpoints**: `51` (`34` in `172.16.0.0/12`, `14` in `10.0.0.0/8`, `3` in `192.168.0.0/16`).
  - **Top TLDs**: `.com` (266), `.net` (168), `.bd` (48), `.live` (46), `.xyz` (8), `.online` (4), `.digital` (4), `.tv` (4), `.info` (3), `.org` (3), `.stream` (3), `.me` (2), `.club` (2), `.top` (2), `.tk` (1).
  - **Malformed / Unparsed entries**: `0`.
  - **ISP Attribution**: All 752 entries carry non-blank `ispName` / `sourceAttribution`.

### B. Bangladesh OTT Directory (`BdOttPlatforms`)

- **Total Platforms**: **26** (`26` unique domains, 100% `HTTPS`).
  - **Bangladeshi & Regional**: **17** (includes 2 `.bd` domains: `iscreen.com.bd`, `www.banglaflix.com.bd`).
  - **International Available in BD**: **9**.

### C. Legacy Verbatim URL List (`FtpLinks`)

- **Total Entries**: **369** raw strings (`EXPECTED_SOURCE_COUNT = 371`, `365` unique URLs) preserved solely for raw-list audit & probe-coalescing compatibility tests.

### D. Canonical JSON Datasets (`public/assets/aistudio/`)

1. `bdix_ftp_movies_catalog_4acd8ccb.json`: `394` ISP records (`393` numbered, `1` unnumbered), `667` classified server endpoints (`545` FTP, `122` Movie), `832` total preserved links (server endpoints + ISP websites/social pages).
2. `bdix_live_tv_catalog_70b3542f.json`: `52` provider records (`50` numbered, `1` Unknown group, `1` Others group), `90` canonical Live TV endpoints + `1` documented duplicate source occurrence (`91` source occurrences).

---

## 2. Android Architecture & Persistence Audit

- **Architecture**: Clean MVVM with manual DI (`AppContainer`), Jetpack Compose + Material 3 (4 Light/Dark presets, `BdixSemanticColors`, English `en` + Bangla `bn`), `Navigation Compose`, Coroutines/`StateFlow`.
- **Database**: **Room v2** (`BdixDatabase`, schema exported at `app/schemas/.../2.json`) with 5 tables: `servers`, `favorites`, `test_history`, `scan_sessions`, `catalog_metadata`. Tested `MIGRATION_1_2` preserves all user data.
  - *Gap*: No `endpoint_governance` or `verification_events` tables for reversible archival, removal candidate tracking, multi-layer evidence, or verification history.
- **Networking**:
  - `HttpEndpointTester`: OkHttp `HEAD` + bounded `1024`-byte `Range` `GET`, max 5 redirect hops, blocks HTTPS-to-HTTP downgrades, records DNS/TCP/TLS/HTTP stage latencies.
    - *Gaps*: No SSRF redirect target check (loopback/RFC1918/link-local/metadata IP blocking on redirect), HTTP `410` grouped into generic `HTTP_CLIENT_ERROR` instead of distinct `HTTP_GONE`.
  - `FtpControlTester`: RFC 959 control-port probe (`220`/`421`/`QUIT`), blocks FTPS downgrade, never attempts credentials or brute force.
  - *Missing*: Multi-resolver/authoritative DNS health differentiation (`NXDOMAIN` vs `SERVFAIL` vs `Timeout`), ICANN RDAP registration lookup (with `.bd` limitation awareness), external ISP probe ingestion, conservative 15-status decision engine.
- **Background & Notifications**:
  - `CatalogSyncWorker` runs 24h periodic catalog JSON sync via WorkManager.
  - *Missing*: Periodic/on-demand catalog domain verification worker with progress persistence, `POST_NOTIFICATIONS` permission, notification channel, and deduplicated summary notifications.
- **Settings & Governance UI**: `SettingsScreen` + `SettingsViewModel` provide theme, language, scan mode, concurrency, remote sync, retention, and destructive clear actions. No Catalog Governance & Restore screen exists yet.

---

## 3. Tooling & CI/CD Audit

- **Python Tooling**: Only `.github/scripts/summarize_gradle_test_results.py` exists (`Python 3.11.2`, stdlib only; `dnspython`, `requests`, `jsonschema` are not installed).
- **GitHub Actions Workflows**:
  - `.github/workflows/android-ci.yml`: Runs Prettier format check, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:compileReleaseKotlin`, Python test timing summary, and artifact upload.
  - `.github/workflows/android-release.yml`: Release workflow.
  - *Gap*: No deterministic catalog JSON/schema/duplicate/governance validation in CI, and no separate optional/scheduled live verification workflow isolated from build gating.
