# Build & Release Guide — BDIX Tester

## Target SDK & Toolchain Lock-In

- **Compile SDK:** API `36` (`compileSdk { version = release(36) { minorApiLevel = 1 } }`)
- **Target SDK:** API `36` (Android 16)
- **Min SDK:** API `24` (Android 7.0)
- **Package Name (`applicationId`):** `com.jubayerhossain.bdixtester`
- **Version:** `versionCode = 1`, `versionName = "1.0"`

## Release Signing Policy

- Release builds (`assembleRelease`) require `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` environment variables with `keyAlias = "upload"`.
- Zero debug-key fallback is configured on `buildTypes.release`.
- Absent keystore/credentials fail `:app:validateSigningRelease` cleanly by design.
- APK `dependenciesInfo { includeInApk = false; includeInBundle = true }`.
- Primary GitHub Release artifact is the signed release APK (`BDIX-Tester-v1.0.apk`) plus `SHA256SUMS.txt`, `release-manifest.json`, and `RELEASE-NOTES.md`.

## Build Commands

Note: On developer machines and GitHub Actions (`ubuntu-latest`), invoke `./gradlew`. Inside container environments pre-installed with system Gradle and without wrapper network download, invoke `gradle`.

```bash
./gradlew :app:compileDebugKotlin --console=plain
./gradlew :app:testDebugUnitTest --console=plain
./gradlew :app:lintDebug --console=plain
./gradlew :app:assembleDebug --console=plain
./gradlew :app:compileReleaseKotlin --console=plain
./gradlew :app:lintVitalRelease --console=plain
# Requires KEYSTORE_PATH, STORE_PASSWORD, KEY_PASSWORD:
./gradlew :app:assembleRelease --console=plain
```
