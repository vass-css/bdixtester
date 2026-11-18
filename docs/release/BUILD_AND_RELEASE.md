# Build & Release Guide — BDIX Tester

## Target SDK & Toolchain Lock-In

- **Compile SDK:** API `36` (`compileSdk { version = release(36) { minorApiLevel = 1 } }`)
- **Target SDK:** API `36` (Android 16 — meets current Google Play new-app target API requirement)
- **Min SDK:** API `24` (Android 7.0)
- **Package Name (`applicationId`):** `com.jubayerhossain.bdixtester`
- **Version:** `versionCode = 1`, `versionName = "1.0"`

## Release Signing Policy

- Release builds (`assembleRelease` / `bundleRelease`) require CI environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) with key alias `upload`.
- Zero debug-key fallback is configured on `release` build type.
- Absent keystore/credentials fail release signing cleanly as designed.
- APK `dependenciesInfo { includeInApk = false; includeInBundle = true }`.

## Build Commands

```bash
gradle :app:compileDebugKotlin --console=plain
gradle :app:testDebugUnitTest --console=plain
gradle :app:lintDebug --console=plain
gradle :app:assembleDebug --console=plain
gradle :app:compileReleaseKotlin --console=plain
# Requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD:
gradle :app:bundleRelease :app:assembleRelease --console=plain
```
