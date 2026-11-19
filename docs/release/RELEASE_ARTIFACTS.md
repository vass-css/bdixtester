# Release Artifacts & Signing Verification — BDIX Tester

- **Updated:** 2026-11-19
- **Application ID:** `com.jubayerhossain.bdixtester`
- **Version:** `1.0` (`versionCode = 1`)
- **SDK Configuration:** `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`
- **Distribution Channel:** GitHub Releases (APK-only public release)

---

## 1. Local Artifacts & Compilation Checks

| Artifact / Task | Status | Path / Verification Output |
| --- | --- | --- |
| Debug APK (`:app:assembleDebug`) | **PRODUCED** | `app/build/outputs/apk/debug/app-debug.apk` (`com.jubayerhossain.bdixtester`, `versionCode = 1`, `versionName = "1.0"`, `minSdk = 24`, `targetSdk = 36`, signed with `CN=Android Debug`) |
| Unit & Robolectric Suite (`:app:testDebugUnitTest`) | **PASSED (`65/65`)** | `65` executed across `18` classes, `65` passed, `0` failed, `0` skipped (`app/build/reports/test-method-timings.csv`, `app/build/reports/tests/testDebugUnitTest/index.html`) |
| Python Catalog Verifier (`tools.catalog_verifier`) | **PASSED** | `752` curated BDIX + `26` OTT entries, `0` errors; `24/24` verifier unit tests passed |
| Android Lint (`:app:lintDebug`) | **PASSED (`0` errors)** | `0 errors, 2 warnings` (`app/build/reports/lint-results-debug.txt`; both warnings are informational AGP/Gradle version notices) |
| Release Compile, Vital Lint & R8 (`:app:compileReleaseKotlin`, `:app:lintVitalRelease`, `:app:minifyReleaseWithR8`) | **PASSED** | Release bytecode, R8 shrinking/obfuscation, resource optimization, and vital lint all succeeded |
| Signed Release APK Gate (`:app:assembleRelease` -> `:app:packageRelease`) | **REJECTED AS EXPECTED (NO KEYSTORE)** | Fails cleanly at `:app:packageRelease` because `/app/applet/my-upload-key.jks` (`KEYSTORE_PATH`) is unprovisioned — confirming zero debug-key fallback |

## 2. Target GitHub Release Assets

When `.github/workflows/android-release.yml` runs on an approved `vX.Y.Z` tag with GitHub Environment `android-release` secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) configured, it verifies `apksigner` + `aapt2` and publishes exclusively:

- `BDIX-Tester-v1.0.apk` (release-signed with `upload` alias, R8-minified, `dependenciesInfo.includeInApk = false`, non-debuggable)
- `SHA256SUMS.txt`
- `release-manifest.json`
- `RELEASE-NOTES.md`

AAB bundles, debug APKs, keystores, passwords, and local configuration files are never uploaded to GitHub Releases.

## 3. Producing a Signed Release APK Locally (Maintainer Environment)

Set the signing environment variables pointing to your offline keystore before running release packaging:

```bash
export KEYSTORE_PATH=/secure/path/to/my-upload-key.jks
export STORE_PASSWORD='your-strong-store-password'
export KEY_PASSWORD='your-strong-key-password'
gradle :app:lintVitalRelease :app:assembleRelease --console=plain
```

Then verify with Android SDK `apksigner` and `aapt2` as documented in [BUILD_AND_RELEASE.md](BUILD_AND_RELEASE.md) and [GITHUB_RELEASE_GUIDE.md](GITHUB_RELEASE_GUIDE.md).
