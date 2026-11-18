# Release Artifacts & Signing Verification — BDIX Tester

**Generated:** 2026-11-18  
**Application ID:** `com.jubayerhossain.bdixtester`  
**Version:** `1.0` (`versionCode = 1`)  
**SDK Configuration:** `minSdk = 24`, `compileSdk = 36`, `targetSdk = 36`

---

## 1. Produced Artifacts & Compilation Checks

| Artifact / Task                                                                     | Status                                 | Path / Verification Output                                                                                                                          |
| ----------------------------------------------------------------------------------- | -------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Debug APK (`:app:assembleDebug`)                                                    | **PRODUCED**                           | `app/build/outputs/apk/debug/app-debug.apk` (`variantName = "debug"`, `versionCode = 1`, `versionName = "1.0"`, `minSdkVersionForDexing = 24`)      |
| Unit & Robolectric Suite (`:app:testDebugUnitTest`)                                 | **PASSED**                             | `33` executed, `33` passed, `0` failed, `0` skipped (`app/build/reports/tests/testDebugUnitTest/index.html`)                                        |
| Android Lint (`:app:lintDebug`)                                                     | **PASSED**                             | `0 errors, 62 warnings` (`app/build/reports/lint-results-debug.html`)                                                                               |
| Release Compile & Vital Lint (`:app:compileReleaseKotlin`, `:app:lintVitalRelease`) | **PASSED**                             | Release bytecode & bundle pre-packaging succeeded                                                                                                   |
| Pre-Sign Release Bundle (`:app:packageReleaseBundle`)                               | **BUILT BEFORE SIGNING**               | Succeeded prior to `:app:signReleaseBundle` gate                                                                                                    |
| Signed Release Bundle (`:app:bundleRelease` -> `:app:signReleaseBundle`)            | **REJECTED AS EXPECTED (NO KEYSTORE)** | Failed cleanly at `:app:signReleaseBundle` because `/app/applet/my-upload-key.jks` (`KEYSTORE_PATH`) is absent — confirming zero debug-key fallback |

## 2. Producing Signed Production AAB & APK in CI / Local Release Environment

Set the required secrets before running release packaging:

```bash
export KEYSTORE_PATH=/path/to/upload-keystore.jks
export STORE_PASSWORD=...
export KEY_PASSWORD=...
gradle :app:bundleRelease :app:assembleRelease --console=plain
```

(Or trigger `.github/workflows/android-ci.yml` `workflow_dispatch` with `UPLOAD_KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` GitHub Secrets configured.)
