# Security & Merged Manifest Audit — BDIX Tester

**Audit Date:** 2026-11-18  
**Target Package:** `com.jubayerhossain.bdixtester`  
**Audit Scope:** Merged AndroidManifest, Network Security Config, Cryptographic Protocol, CSV/Intent Safety, and Source Secret Scan

---

## 1. Merged Manifest & Permission Audit

Inspected artifact: `app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`

- **SDK Versions Verified:** `minSdkVersion="24"`, `targetSdkVersion="36"`, `versionCode="1"`, `versionName="1.0"`.
- **Explicit Declared Permissions:**
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.INTERNET`
- **Transitive Library Permissions (Jetpack WorkManager):**
  - `android.permission.WAKE_LOCK`
  - `android.permission.RECEIVE_BOOT_COMPLETED`
  - `android.permission.FOREGROUND_SERVICE`
  - `com.jubayerhossain.bdixtester.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (`signature` protection level)
- **Zero Sensitive Permissions Confirmed:**
  - No `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_WIFI_STATE`, `READ_EXTERNAL_STORAGE`, or `READ_MEDIA_*` appear anywhere in source or merged manifest.
- **Component Exposure:** Only launcher `.MainActivity` is exported (`android:exported="true"`); internal providers/services/receivers (`androidx.startup.InitializationProvider`, `SystemAlarmService`, `SystemForegroundService`, `MultiInstanceInvalidationService`) remain `exported="false"` (with `SystemJobService` protected by `android.permission.BIND_JOB_SERVICE`).

## 2. Dynamic Cleartext Limitation & Code-Level Isolation

- **Technical Reality (`network_security_config.xml`):** Because the built-in 369-entry catalog (`365` `http://` endpoints including RFC 1918 IPv4s) and user-added custom endpoints have dynamic hosts, Android's static XML `<domain-config>` cannot pre-authorize arbitrary future endpoints.
- **Accurately Documented Mitigation:**
  1. `network_security_config.xml` declares `<base-config cleartextTrafficPermitted="true">` restricted strictly to `<certificates src="system" />` (never trusting user-installed certs or disabling hostname verification).
  2. Application-controlled remote catalog sync (`RemoteCatalogSyncManager`) rejects non-HTTPS URLs (`SyncStatus.REJECTED_ORIGIN`) and disables automatic OkHttp redirects (`followRedirects(false)`), manually blocking any HTTPS-to-HTTP downgrade.
  3. `HttpEndpointTester` and `SpeedTestEngine` also disable automatic redirects and reject HTTPS-to-HTTP downgrades (`PROTOCOL_DOWNGRADE_BLOCKED` / `HTTPS_DOWNGRADE_BLOCKED`).
  4. Unauthenticated probes and speed tests never transmit credentials over HTTP or FTP.

## 3. Signed Remote Catalog Cryptographic Audit

- **Algorithm:** Standard Android/JCA `SHA256withECDSA` (P-256) in `CatalogSignatureVerifier`.
- **Canonical Verification:** Decodes bytes via strict `UTF-8` (`CodingErrorAction.REPORT`), rejects BOM (`0xEF 0xBB 0xBF` / `\uFEFF`), reads only untrusted `keyId`, and verifies the detached signature over the **exact raw UTF-8 manifest bytes** prior to JSON parsing or normalization.
- **Byte Ceilings Enforced During Streaming:** Manifest `64 KiB`, Signature `512 B`, Payload `2 MiB`.
- **Anti-Rollback & Cache Rules:** Higher `catalogSequence` required for acceptance; equal sequence + differing SHA-256 rejected (`REJECTED_CONFLICT`); lower sequence rejected (`REJECTED_ROLLBACK`); HTTP `304 Not Modified` reuses verified cache only and never advances `catalogSequence`.
- **Important Pre-Publication Operator Prerequisite:** `TrustedKeyRegistry.DEFAULT_KEYS` (`bdix-catalog-2026-v1`) contains a placeholder public key entry for schema/infrastructure readiness and zero private keys. Before publishing a live remote catalog feed, the operator must replace that public SPKI entry with their offline-generated P-256 ECDSA public key.

## 4. Export, Intent & Source Secret Scan

- **CSV Formula Neutralization:** `CatalogExportSerializer.neutralizeCsvCell` prefixes `=`, `+`, `-`, `@`, `\t`, `\r` (and leading-whitespace formula triggers) with `'` and applies RFC-4180 quoting; JSON preserves verbatim URLs.
- **Browser Intent Safety:** `ExternalBrowserHelper` strictly allows `http://` and `https://` with non-blank hosts, blocking `ftp://`, `ftps://`, `file://`, and `javascript:`.
- **Source Secret Grep Audit:** Ran `grep -rnE "PRIVATE KEY|BEGIN RSA|BEGIN EC|secret|password|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|ACCESS_WIFI_STATE|READ_EXTERNAL_STORAGE|READ_MEDIA_" app/src/main` -> **0 matches** (`exit code 1`).
