# Release Checklist — BDIX Tester v1.0 (GitHub Releases)

- **Package Name:** `com.jubayerhossain.bdixtester`
- **Target Version:** `v1.0` (`versionName = "1.0"`, `versionCode = 1`)
- **Primary Artifact:** `BDIX-Tester-v1.0.apk` (APK-only GitHub Release)
- **Maintainer:** `Jubayer Hossain` (`jubayerhossainbarisal@gmail.com`)

---

## 1. Local Code, Tests & Security Pre-Flight (Workspace Complete)

- [x] `applicationId`, `versionName` (`1.0`), `versionCode` (`1`), and `metadata.json` (`BDIX Tester`, `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API`) verified intact.
- [x] `LICENSE` updated to `Copyright (c) 2026 Jubayer Hossain` (MIT).
- [x] Placeholder community link replaced with real `mailto:jubayerhossainbarisal@gmail.com` Developer Support row (`SettingsScreen.kt`, `values/strings.xml`, `values-bn/strings.xml`).
- [x] AdMob / tracking SDKs absent (`releaseRuntimeClasspath` audited).
- [x] Curated catalog preserved verbatim (`752` BDIX entries, `26` OTT entries; `tools.catalog_verifier` `0` errors, `24/24` verifier tests passing).
- [x] `gradle :app:lintDebug` passes with `0 errors, 2 warnings`.
- [x] `gradle :app:testDebugUnitTest` passes `65 / 65` tests across `18` classes (`0` failed, `0` skipped).
- [x] `gradle :app:compileReleaseKotlin`, `:app:lintVitalRelease`, and `:app:minifyReleaseWithR8` succeed.
- [x] Negative release signing gate verified: `:app:assembleRelease` fails cleanly at `:app:packageRelease` without a keystore (zero debug fallback).
- [x] `.gitignore` excludes `*.jks`, `*.keystore`, `*.p12`, `*.pkcs12`, `*.pem`, `.env`, `*.apk`, `*.aab`, and temporary build artifacts.
- [x] GitHub Actions workflows (`android-ci.yml`, `android-release.yml`, `dependency-audit.yml`, `release-integrity.yml`, `catalog-live-verification.yml`) use least-privilege permissions and 40-char immutable commit SHAs.
- [x] Privacy Policy (`docs/privacy/PRIVACY_POLICY.md`) and Copyright/Data Risk Audit (`docs/privacy/PRIVACY_AND_DATA_HANDLING.md`) reflect actual permissions and catalog contents.

## 2. Maintainer Keystore & GitHub Repository Setup (Required Before Release Tag)

- [ ] Generate offline RSA-4096 upload keystore (`my-upload-key.jks`, alias `upload`) outside the repository using `keytool`.
- [ ] Record the production certificate SHA-256 fingerprint and store two encrypted offline backups (never committed to Git).
- [ ] Initialize Git locally (`git init -b main`), verify `git status` excludes `.jks` / `.env` / build artifacts, and push initial commit to GitHub.
- [ ] Confirm GitHub repository URL links inside `README.md`, `SECURITY.md`, `.github/ISSUE_TEMPLATE/config.yml`, and `.github/workflows/android-release.yml` match your actual GitHub username/slug (`jubayerhossain` / `bdix-tester` or updated handle).
- [ ] Create protected GitHub Environment `android-release` with required reviewer approval.
- [ ] Add GitHub Environment secrets in `android-release`: `KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` (`base64 -w 0`).
- [ ] Verify `.github/workflows/android-ci.yml`, `dependency-audit.yml`, and `release-integrity.yml` finish green on `main` via GitHub Actions.

## 3. Release Execution & Post-Build Gate

- [ ] Ensure `app/build.gradle.kts` `versionName` matches tag `vX.Y.Z` (currently `1.0` -> `v1.0`).
- [ ] Push signed/annotated release tag (`git tag -a v1.0 -m "Release BDIX Tester v1.0"` && `git push origin v1.0`) ONLY after user approval and green CI.
- [ ] Approve the `publish_release` deployment gate in GitHub Environments (`android-release`).
- [ ] Verify release workflow outputs all four artifacts on GitHub Releases:
  - `BDIX-Tester-v1.0.apk`
  - `SHA256SUMS.txt`
  - `release-manifest.json`
  - `RELEASE-NOTES.md`
- [ ] Inspect workflow logs to verify `apksigner verify --print-certs` reports your production certificate (never `CN=Android Debug`) and `aapt2 dump badging` confirms `com.jubayerhossain.bdixtester`, `versionCode=1`, `versionName=1.0`, and `application-debuggable` is absent.
- [ ] Download the published `BDIX-Tester-v1.0.apk`, verify `sha256sum -c SHA256SUMS.txt`, and install/smoke-test on a physical Android device.
- [ ] Capture physical device screenshots and update `README.md` Screenshots section in a follow-up commit if desired.

## 4. Current Publication Decision

- **Local Workspace & Git Push (`main` + CI Dry Run):** **GO**
- **Automated Tag Release Publication (`v1.0` -> GitHub Releases):** **NO-GO until offline keystore generation, Git remote push, and `android-release` GitHub Environment secrets are provisioned and CI passes on `main`.**
