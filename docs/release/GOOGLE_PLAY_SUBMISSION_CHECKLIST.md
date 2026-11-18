# Google Play Submission Checklist — BDIX Tester

**Updated:** 2026-11-18  
**Policy Reference Date:** 2026-11-18  
**Application ID:** `com.jubayerhossain.bdixtester`  
**Developer:** `Jubayer Hossain` (`JubayerHossain`)

---

## 1. Technical & Target API Compliance

| Item                                | Status       | Evidence / Notes                                                                                   |
| ----------------------------------- | ------------ | -------------------------------------------------------------------------------------------------- |
| `targetSdk` >= Android 16 (API 36)  | **VERIFIED** | `targetSdk = 36`, `compileSdk = 36` (`minorApiLevel = 1`) in `app/build.gradle.kts`                |
| `minSdk` compatibility              | **VERIFIED** | `minSdk = 24` verified across Room, OkHttp, WorkManager, ECDSA P-256 (`SHA256withECDSA`)           |
| Adaptive Launcher Icon              | **VERIFIED** | Custom adaptive icon configured via `@mipmap/ic_launcher` & `ic_launcher_round`                    |
| 64-bit / Native Lib Compatibility   | **VERIFIED** | Pure Kotlin/Compose app (`extractNativeLibs="false"` in merged manifest); zero custom native `.so` |
| `dependenciesInfo` in Bundle vs APK | **VERIFIED** | `includeInApk = false`, `includeInBundle = true` in `app/build.gradle.kts`                         |

## 2. Permissions, Privacy & Data Safety

| Item                                | Status                       | Evidence / Notes                                                                                                                                              |
| ----------------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Zero Location / SSID Permissions    | **VERIFIED**                 | Merged manifest contains neither `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, nor `ACCESS_WIFI_STATE`                                                    |
| Zero Broad Storage Permissions      | **VERIFIED**                 | Exports use `ActivityResultContracts.CreateDocument` (SAF); zero `READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`                                                     |
| Cleartext Traffic Scope & Rationale | **DOCUMENTED**               | `network_security_config.xml` documents why dynamic BDIX HTTP hosts prevent static `domain-config`; HTTP isolated to endpoint tester; catalog sync HTTPS-only |
| Data Safety Form Accuracy           | **VERIFIED (Local-Only)**    | Zero user accounts, zero ads, zero analytics, zero server-side telemetry                                                                                      |
| Public HTTPS Privacy Policy URL     | **PENDING EXTERNAL HOSTING** | Local draft at `docs/privacy/PRIVACY_POLICY.md`; must be hosted on public HTTPS URL before Play Console submission                                            |

## 3. Release Signing & Console Gates

| Item                                                  | Status                        | Notes                                                                                                                         |
| ----------------------------------------------------- | ----------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| Dedicated Upload Keystore (`KEYSTORE_PATH`)           | **PENDING USER PROVISIONING** | Never committed to repo; configured via CI env secrets                                                                        |
| Play App Signing Enrollment                           | **PENDING PLAY CONSOLE**      | Requires Google Play Console app creation & upload key certificate enrollment                                                 |
| Store Listing Metadata, Screenshots & Feature Graphic | **PENDING EXTERNAL ASSETS**   | App name (`BDIX Tester` <= 30 chars, compliant), localized EN/BN UI ready; device screenshots require emulator/device capture |
