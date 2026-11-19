# Production Test Matrix — BDIX Tester

- **Execution Command:** `rm -rf app/build/test-results/testDebugUnitTest && gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileReleaseKotlin :app:lintVitalRelease --console=plain`
- **Execution Date:** 2026-11-19
- **Result Summary:** `65` executed across `18` classes, `65` passed, `0` failed, `0` skipped (`BUILD SUCCESSFUL in 39s`)
- **Python Catalog Verifier:** `24` executed, `24` passed (`0.049s`), plus `validate-catalog` (`752` BDIX + `26` OTT entries, `0` errors)

---

## Automated Unit & Robolectric Test Suites (`app/build/reports/test-method-timings.csv`)

| Test Class | Count | Status | Key Behaviors Verified |
| --- | ---: | --- | --- |
| `BuiltInCatalogAuditTest` | 3 | PASSED | Verbatim preservation of all `752` curated entries (`builtin:0..751`) + `26` OTT entries, exact deduplication/ISP naming/category coverage, distinct port/path preservation, stale/deleted active result filtering |
| `CatalogGovernanceUiTest` | 1 | PASSED | End-to-end Compose governance screen rendering, `GovernanceEvidenceSheet` scrolling inspection, and Room-backed Archive Confirm & Restore flows |
| `CatalogSignatureAndSyncTest` | 4 | PASSED | ECDSA P-256 raw UTF-8 verification, BOM & reserialization rejection, revoked key rejection, `StrictWifiValidator`, `UPDATED` / `304` / `REJECTED_CONFLICT` / `REJECTED_ROLLBACK`, built-in refresh preserving user data |
| `ExampleRobolectricTest` | 1 | PASSED | Android application context `app_name == "BDIX Tester"` verification |
| `ExampleUnitTest` | 1 | PASSED | Baseline JVM unit sanity check |
| `ExportAndIntentSafetyTest` | 4 | PASSED | CSV formula neutralization (`=`, `+`, `-`, `@`, `\t`, `\r`, leading whitespace) vs verbatim JSON export, `ExternalBrowserHelper` `http/https`-only gate, typed safe error return when no browser handles URI |
| `GovernanceDecisionEngineTest` | 11 | PASSED | Multi-ISP active quorum, single-ISP / temporary DNS / HTTP `401/403/404/410/429/500` / TLS / FTP auth non-expiry safety, RFC 1918 exemptions, RDAP/DoH/ISP wire parsing, corroborated dead-domain confirmation & SSRF private-metadata guard |
| `LocalizationAccessibilityUiTest` | 1 | PASSED | Bangla (`bn-rBD`) configuration, OTT directory, and modern Settings rendering without crash |
| `NavigationUiTest` | 3 | PASSED | Full Robolectric Compose traversal across Home, Servers, Scan, Saved, OTT Directory, Settings, quick-inspect sheet, and direct external browser launching |
| `NetworkProtocolTest` | 8 | PASSED | HTTP HEAD `200`, `405 -> Range: bytes=0-1023 GET`, `401/403/404/500 -> RESTRICTED`, Quick Scan deadline `isQuickScanInconclusive`, FTP `220` + `QUIT`, `120 -> 220` & multiline `220-`, `421` / malformed banner / `> 8 KiB` flood / timeout, cancellation + `ftps:// -> UNSUPPORTED` |
| `PerformanceBenchmarkTest` | 8 | PASSED | `15` deterministic performance metrics covering curated load (`3.20ms`), URL/classifier (`0.13-1.10ms`), `AppContainer` sync ctor (`0.33ms`), cold Room seed (`88.10ms`) & warm verify (`0.16ms`), `ServersViewModel` filter/sort (`17.80ms`), 200-server scan, HTTP 60-probe throughput, and repeated scan/cancel safety |
| `RoomDatabaseMigrationTest` | 5 | PASSED | Exported Room `2.json` & `3.json` validation, non-destructive `MIGRATION_1_2` and `MIGRATION_2_3` preserving custom/favorites/history with v2 indices, idempotent `752`-row built-in seeding, `RUNNING -> INTERRUPTED` recovery, upgrade re-seed, and reversible Archive/Restore |
| `ScanCoordinatorTest` | 2 | PASSED | Duplicate URL coalescing (`scanRunId` + `probeGroupId`, `isCoalescedReplica`), bounded concurrency, mutex lock, graceful `stopScan()` -> `CANCELLED` |
| `SettingsCatalogSyncUiTest` | 1 | PASSED | Settings catalog sync button working without remote URL and supporting both built-in refresh and signed remote sync flows |
| `SpeedTestEngineTest` | 4 | PASSED | Exact byte-cap termination (`CAP_REACHED`, `Accept-Encoding: identity`), below-min-time `INSUFFICIENT_SAMPLE` retaining `BYTE_CAP_REACHED`, `EOF_REACHED -> TRANSFER_COMPLETED`, gzip rejection & cross-host redirect confirmation |
| `ThemeSwitchingUiTest` | 1 | PASSED | Selection and persistence across all four theme presets and Light/Dark appearance modes |
| `ThemeSystemContrastAndMigrationTest` | 2 | PASSED | WCAG 2.2 AA contrast validation across all eight theme configurations, legacy/corrupted preference migration to Midnight Indigo & System |
| `UrlParserTest` | 5 | PASSED | HTTP hostname containing `"ftp"` kept as HTTP, `ftp://` / `ftps://` separation, RFC 1918 private IPv4 ranges, unparseable input flagging without mutation |

## Verification Not Executable in Cloud Environment

- Live physical-device instrumentation (`connectedAndroidTest`), ADB sideload checks, and real Bangladesh ISP BDIX server scanning were **not** executed in this container because no Android Emulator, ADB daemon, or Bangladesh residential ISP uplink is available.
