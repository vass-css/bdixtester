# Release Readiness Audit — BDIX Tester

- **Audit Date:** 2026-11-19
- **Application Name:** BDIX Tester
- **Package Name (`applicationId`):** `com.jubayerhossain.bdixtester`
- **Version:** `versionName = "1.0"` · `versionCode = 1`
- **SDK Targets:** `minSdk = 24`, `compileSdk = 36` (minor 1), `targetSdk = 36`
- **License Intention:** MIT (`LICENSE`)

## 1. Repository, Git History & Environment Audit

| Inspection Area | Command / File Checked | Verified Finding |
| --- | --- | --- |
| Git Repository Status | `git status` | **Blocker (External Setup Required):** `fatal: not a git repository (or any of the parent directories): .git`. No local Git history, branches, or tags exist in this workspace. |
| GitHub CLI Availability | `gh --version` | **Not Present:** `sh: 1: gh: not found`. Remote GitHub Actions runs/releases must be pushed and monitored from the maintainer's local workstation. |
| Project Metadata Sync | `metadata.json:2,5`, `values/strings.xml:2`, `settings.gradle.kts:25` | Matches `BDIX Tester`; `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` preserved untouched in `metadata.json:5`. |
| `.gitignore` / `.gitattributes` | `.gitignore`, `.gitattributes` | **Fixed:** Expanded `.gitignore` to exclude `.env`, `*.p12`, `*.pkcs12`, `*.pem`, `*.apk`, `*.aab`, `.idea/`, `release-assets/`, and `__pycache__/`; cleaned `.gitattributes` for Android/Kotlin/Gradle/Python text and binary assets. |

## 2. Build, Signing & Security Audit

| Inspection Area | File & Line Reference | Baseline Finding & Remediation |
| --- | --- | --- |
| Release Versioning & App ID | `app/build.gradle.kts:16-21` | Confirmed `applicationId = "com.jubayerhossain.bdixtester"`, `minSdk = 24`, `targetSdk = 36`, `versionCode = 1`, `versionName = "1.0"`. |
| Release Signing Config | `app/build.gradle.kts:28-35,76` | Reads `KEYSTORE_PATH` (fallback `${rootDir}/my-upload-key.jks`), `STORE_PASSWORD`, `keyAlias = "upload"`, `KEY_PASSWORD`. Zero debug-key fallback on `release`. |
| Release Keystore Presence | Workspace root (`/app/applet`) | **Blocker (User Action Required):** Neither `my-upload-key.jks` nor production signing passwords exist in the repository (by design). User will generate an offline RSA-4096 keystore locally per `docs/release/SIGNING_AND_KEY_MANAGEMENT.md`. |
| ProGuard / R8 Optimization | `app/build.gradle.kts:71-75`, `app/proguard-rules.pro:1-17` | `isMinifyEnabled = true`, `isShrinkResources = true`, `isCrunchPngs = false`. Retains `SourceFile`/`LineNumberTable`, Room entities/DAOs, OkHttp, and WorkManager workers. |
| Lint DSL Placement | `app/build.gradle.kts:37-52` -> `:60-75` | **Fixed:** Baseline `lint { ... }` block was nested inside `signingConfigs { ... }`; relocated cleanly directly under `android { ... }`. |
| Play Dependency Info Block | `app/build.gradle.kts:100-103` | `dependenciesInfo { includeInApk = false; includeInBundle = true }` ensures no Play-encrypted dependency block is embedded in GitHub APKs. |
| Secrets & Key Hygiene | `.env.example`, `.gitignore`, source scan | Zero hardcoded API keys, AdMob publisher IDs, Firebase project credentials, or private keystores in source code. |
| Remote Catalog ECDSA Key | `CatalogSignatureVerifier.kt:50-56` | **Blocker for Remote Feeds Only:** `TrustedKeyRegistry.DEFAULT_KEYS` ships a placeholder P-256 public key (`bdix-catalog-2026-v1`). Offline bundled catalog (`752` BDIX + `26` OTT) operates without it; generate a real offline P-256 keypair before enabling any hosted manifest feed. |
| Network Security Config | `app/src/main/res/xml/network_security_config.xml:10-14` | Permits system-CA-trusted cleartext HTTP strictly so user-triggered BDIX LAN/FTP/HTTP probes work on arbitrary local hosts and RFC 1918 IPs; remote catalog sync enforces HTTPS-only URLs and rejects HTTPS-to-HTTP downgrades. |
| Permissions Audit | `app/src/main/AndroidManifest.xml:4-6` | Only `ACCESS_NETWORK_STATE`, `INTERNET`, and `POST_NOTIFICATIONS` (runtime-requested on Android 13+). Zero location, Wi-Fi state, or broad storage permissions. |

## 3. Baseline Verification Defects Identified & Fixed

1. **Android Lint Baseline Failure (`11 errors, 2 warnings`):**
   - `VerificationNotificationHelper.kt:89,113`: `[MissingPermission]` flagged `NotificationManagerCompat.notify` because Lint did not trace delegation to `canPostNotifications()`. **Fixed** by adding inline `POST_NOTIFICATIONS` permission checks at both call sites.
   - `BdixTesterAppRoot.kt:171,205,215,219,223,227` and `CatalogGovernanceScreen.kt:131,139,149`: `[LocalContextGetResourceValueCall]` flagged `context.getString(...)` inside Composables. **Fixed** by reading `LocalResources.current` and providing `LocalResources provides localizedContext.resources` in `MainActivity.kt` so runtime EN/BN localization remains intact.
2. **Robolectric UI Test Failure (`CatalogGovernanceUiTest`):**
   - Baseline run failed at `CatalogGovernanceUiTest.kt:41` (`ComposeTimeoutException`) because `governance_item_builtin:0` sits below the fold in alphabetically sorted `LazyColumn`, and subsequent bottom-sheet assertions lacked `performScrollTo()` and Room async waits. **Fixed** with zero weakening of functional assertions.
   - Configured `maxHeapSize = "2g"` on Gradle `Test` tasks in `app/build.gradle.kts` to prevent Robolectric SDK 36 multi-suite OOM stalls under Gradle's default 512 MB worker heap.
3. **Attribution & Placeholder Links:**
   - `LICENSE:3` previously credited `Sobji Dokan` instead of `Jubayer Hossain`. **Fixed.**
   - `SettingsScreen.kt:112` previously linked `settings_facebook_row` to a generic Facebook search query URL. **Fixed** by routing to a real `mailto:jubayerhossainbarisal@gmail.com` Developer Support intent and updating EN/BN strings.
4. **Outdated Workflow & Docs Claims:**
   - `android-ci.yml` unconditionally invoked `npm ci` without `package-lock.json` and used mutable action tags; `android-release.yml` built unrequested AABs, lacked `apksigner`/`aapt2`/checksum/manifest checks, and held broad `contents: write`; `docs/release/RELEASE_ARTIFACTS.md` falsely reported old `33/33` tests and `0 errors` lint before fixes. **All updated.**

## 4. Curated Catalog & Dependency License Audit

- **Catalog Integrity (`tools.catalog_verifier`):** `752` curated BDIX entries and `26` OTT entries verified (`0` duplicates, `0` invalid URLs, `24/24` verifier unit tests pass). Preserved verbatim per maintainer directive; legal/copyright risk analysis documented in `docs/privacy/PRIVACY_AND_DATA_HANDLING.md`.
- **Dependencies (`releaseRuntimeClasspath`):** Exclusively AndroidX, Compose BOM, Kotlinx Coroutines, and Square OkHttp (all Apache-2.0 — compatible with MIT distribution). JUnit (EPL-1.0) and Robolectric (MIT) remain scoped to `testImplementation`.

## 5. Blocking Severity Summary

| Severity | Blocker | Owner & Resolution |
| --- | --- | --- |
| **Critical (Pre-Release)** | No production release keystore (`upload` alias) in environment | Maintainer generates offline RSA-4096 `.jks` keystore locally and stores `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` in GitHub `android-release` environment secrets |
| **Critical (Pre-Release)** | Workspace is not yet a pushed Git repository | Maintainer initializes Git repo locally, commits audited state, pushes to GitHub, and verifies CI green before pushing tag `v1.0` |
| **Medium (Feature-Scoped)** | Placeholder ECDSA SPKI key in `CatalogSignatureVerifier.kt:53` | Only blocks hosted signed remote catalog feeds (`remoteCatalogUrl` defaults empty); replace SPKI key if hosting remote manifest JSON in a future release |
| **Medium (Legal/Policy)** | Curated catalog includes third-party pirate/streaming domains | Catalog preserved verbatim as requested; domain risk audit and store compliance caveats recorded in `docs/privacy/PRIVACY_AND_DATA_HANDLING.md` |
