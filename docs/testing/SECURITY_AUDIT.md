# Security & Merged Manifest Audit — BDIX Tester

- **Audit Date:** 2026-11-19
- **Target Package:** `com.jubayerhossain.bdixtester`
- **Audit Scope:** Merged AndroidManifest, Network Security Config, Cryptographic Protocol, CSV/Intent Safety, and Source Secret Scan

---

## 1. Merged Manifest & Permission Audit

Inspected artifacts: `app/src/main/AndroidManifest.xml` and `aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk`.

- **SDK Versions Verified:** `minSdkVersion="24"`, `targetSdkVersion="36"`, `versionCode="1"`, `versionName="1.0"`.
- **Explicit Declared Permissions (`app/src/main/AndroidManifest.xml:4-6`):**
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.INTERNET`
  - `android.permission.POST_NOTIFICATIONS` (optional runtime notification permission on Android 13+; checked inline at `VerificationNotificationHelper.kt:88-96,118-126` before `NotificationManagerCompat.notify`)
- **Transitive Library Permissions (Jetpack WorkManager):**
  - `android.permission.WAKE_LOCK`
  - `android.permission.RECEIVE_BOOT_COMPLETED`
  - `android.permission.FOREGROUND_SERVICE`
  - `com.jubayerhossain.bdixtester.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (`signature` protection level)
- **Zero Sensitive Permissions Confirmed:**
  - No `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_WIFI_STATE`, `READ_EXTERNAL_STORAGE`, or `READ_MEDIA_*` appear anywhere in source or merged manifest.
- **Component Exposure:** Only launcher `.MainActivity` is exported (`android:exported="true"`); internal providers, services, and receivers remain unexported (`exported="false"` or `BIND_JOB_SERVICE`-protected).

## 2. Dynamic Cleartext Limitation & Code-Level Isolation

- **Technical Reality (`network_security_config.xml`):** Because the bundled `752`-entry BDIX catalog + `26`-entry OTT directory (including LAN/BDIX `http://` hosts and RFC 1918 IPv4s) and user-added custom endpoints have arbitrary user-tested hosts, Android's static XML `<domain-config>` cannot enumerate all future endpoints ahead of time.
- **Accurately Documented Mitigation:**
  1. `network_security_config.xml` declares `<base-config cleartextTrafficPermitted="true">` restricted strictly to `<certificates src="system" />` (never trusting user-installed certs or disabling hostname verification).
  2. Application-controlled remote catalog sync (`RemoteCatalogSyncManager`) rejects non-HTTPS URLs (`SyncStatus.REJECTED_ORIGIN`) and disables automatic OkHttp redirects (`followRedirects(false)`), manually blocking any HTTPS-to-HTTP downgrade.
  3. `HttpEndpointTester` and `SpeedTestEngine` also disable automatic redirects, enforce SSRF guards (`GovernanceDecisionEngineTest`), and reject HTTPS-to-HTTP downgrades (`PROTOCOL_DOWNGRADE_BLOCKED` / `HTTPS_DOWNGRADE_BLOCKED`).
  4. Unauthenticated probes and speed tests never transmit credentials over HTTP or FTP.

## 3. Signed Remote Catalog Cryptographic Audit

- **Algorithm:** Standard Android/JCA `SHA256withECDSA` (P-256) in `CatalogSignatureVerifier`.
- **Canonical Verification:** Decodes bytes via strict `UTF-8` (`CodingErrorAction.REPORT`), rejects BOM (`0xEF 0xBB 0xBF` / `\uFEFF`), reads only untrusted `keyId`, and verifies the detached signature over the **exact raw UTF-8 manifest bytes** prior to JSON parsing or normalization.
- **Byte Ceilings Enforced During Streaming:** Manifest `64 KiB`, Signature `512 B`, Payload `2 MiB`.
- **Anti-Rollback & Cache Rules:** Higher `catalogSequence` required for acceptance; equal sequence + differing SHA-256 rejected (`REJECTED_CONFLICT`); lower sequence rejected (`REJECTED_ROLLBACK`); HTTP `304 Not Modified` reuses verified cache only and never advances `catalogSequence`.
- **Pre-Publication Operator Prerequisite (Optional Remote Feed):** `TrustedKeyRegistry.DEFAULT_KEYS` (`bdix-catalog-2026-v1`, `CatalogSignatureVerifier.kt:53`) ships a placeholder SPKI public key entry for offline-testable schema readiness and zero private keys. Offline bundled catalogs require no change; if hosting a live signed feed later, generate an offline ECDSA P-256 keypair and replace that SPKI constant before release.

## 4. Export, Intent & Source Secret Scan

- **CSV Formula Neutralization:** `CatalogExportSerializer.neutralizeCsvCell` prefixes `=`, `+`, `-`, `@`, `\t`, `\r` (and leading-whitespace formula triggers) with `'` and applies RFC-4180 quoting; JSON preserves verbatim URLs.
- **Browser Intent Safety:** `ExternalBrowserHelper` strictly allows `http://` and `https://` with non-blank hosts, blocking `ftp://`, `ftps://`, `file://`, and `javascript:`.
- **Play Dependency Info Strip (`app/build.gradle.kts`):** `dependenciesInfo { includeInApk = false; includeInBundle = true }` ensures GitHub Release APKs contain no proprietary Google Play dependency-signing block.
- **Source Secret Grep Audit:** Ran `grep -rnE "PRIVATE KEY|BEGIN RSA|BEGIN EC|ACCESS_FINE_LOCATION|ACCESS_COARSE_LOCATION|ACCESS_WIFI_STATE|READ_EXTERNAL_STORAGE|READ_MEDIA_" app/src/main` -> **0 matches**.
