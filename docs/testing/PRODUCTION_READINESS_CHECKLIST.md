# Production Readiness Checklist — BDIX Tester

- **Audit Date:** 2026-11-19
- **Application ID:** `com.jubayerhossain.bdixtester`
- **Primary Distribution:** GitHub Releases (`BDIX-Tester-v1.0.apk`)

---

## Tiered Readiness Status

| Area | Status | Notes |
| --- | --- | --- |
| **Built-In Catalog & Integrity Audit** | **Implemented & Automated-Test Verified** | All `752` curated BDIX entries (`builtin:0..751`) + `26` OTT entries verified (`0` duplicates, `0` invalid URLs). Validated via Kotlin tests (`65/65`) and Python `tools.catalog_verifier` (`24/24`). |
| **Room Schema v3 & Non-Destructive Migrations** | **Implemented & Automated-Test Verified** | Schemas `2.json` and `3.json` exported and validated; `MIGRATION_1_2` and `MIGRATION_2_3` tested with v2 indices preserved, interrupted `RUNNING` session recovery, and reversible Archive/Restore tested. |
| **HTTP/HTTPS & FTP Probe Engines** | **Implemented & Automated-Test Verified** | Verified against local `MockWebServer` & `ServerSocket` (`HEAD`, `Range GET 0-1023`, `401/403/404/500`, `2.5s` quick deadline, `220/120/421/QUIT`, flood & cancellation guards, `ftps://` unsupported). |
| **Coalesced Multi-Worker Scan Engine** | **Implemented & Automated-Test Verified** | `scanRunId` + `probeGroupId` coalescing (`1` per host, default `3` concurrent, `isCoalescedReplica`), stop cancellation, and 200-server benchmark verified. |
| **Opt-In Monotonic Speed Test Engine** | **Implemented & Automated-Test Verified** | `Accept-Encoding: identity`, 32 KiB streaming stopping cleanly at `maxBytes`, 5-state outcomes, cross-host redirect guard. |
| **ECDSA P-256 Remote Signed Sync** | **Implemented & Automated-Test Verified** | Verified with test-generated P-256 keypair (`304`, rollback, conflict, BOM, revoked key, `StrictWifiValidator`). Live feed remains optional (`remoteCatalogUrl` defaults empty) and requires operator P-256 SPKI replacement in `CatalogSignatureVerifier.kt:53`. |
| **SAF Export, EN/BN Localization & LTR Safety** | **Implemented & Robolectric-Verified** | CSV neutralization, verbatim JSON, EN/BN strings (`LocalResources` provided in `MainActivity`), `LtrContent`, `NavigationUiTest`, `LocalizationAccessibilityUiTest`, `CatalogGovernanceUiTest`. |
| **Android Lint & Release Compile / R8** | **Verified (`0` errors)** | `lintDebug` (`0 errors, 2 warnings`), `lintVitalRelease`, `assembleDebug`, `compileReleaseKotlin`, and `:app:minifyReleaseWithR8` all pass cleanly. |
| **Release Signing Gate** | **Verified Strict Gate** | `:app:assembleRelease` fails cleanly at `:app:packageRelease` when `/app/applet/my-upload-key.jks` (`KEYSTORE_PATH`) is absent (zero debug-key fallback). |
| **Emulator / Physical Device / Live ISP Testing** | **Not Executed (Environment Blocked)** | No Android Emulator, ADB, or live Bangladesh BDIX ISP network available in this cloud build environment. |
| **External Keystore, GitHub Repo & Environment Secrets** | **Pending Maintainer Provisioning** | Offline RSA-4096 upload keystore (`my-upload-key.jks`), Git repository push, and `android-release` GitHub Environment secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) remain external actions. |
