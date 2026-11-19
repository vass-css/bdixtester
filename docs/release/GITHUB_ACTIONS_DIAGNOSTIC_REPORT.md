# GitHub Actions Diagnostic Report - BDIX Tester

- Audit date: 2026-11-19
- Repository: `vass-css/bdixtester`
- Repository URL: https://github.com/vass-css/bdixtester
- Default branch: `main`
- Audited HEAD: `47ea1a69e2dce9b62d5b219eb60f0a275313605e`

## GitHub CLI

- GitHub CLI: `gh 2.103.3`
- Authentication: GitHub account `vass-css`
- Repository access and workflow history were available.

## Workflows Inspected

- `.github/workflows/android-ci.yml`
- `.github/workflows/android-release.yml`
- `.github/workflows/catalog-live-verification.yml`
- `.github/workflows/dependency-audit.yml`
- `.github/workflows/release-integrity.yml`

Third-party actions are pinned to full commit SHAs. The release publication job alone requests `contents: write`; other CI jobs use `contents: read`.

## Recent Runs

- Android CI run `44487701540`
  - Branch/HEAD: `main` / `47ea1a69e2dce9b62d5b219eb60f0a275313605e`
  - Event: `push`
  - Conclusion: `failure`
  - Job: `Build, test, lint, and catalog audit`
  - Failed step: `Validate Gradle build, unit tests, lint, and debug APK`
  - Failure: 65 tests ran; `NavigationUiTest.homeStaysMinimalAndServerQuickInspectSheetWorksEndToEnd` failed with `java.lang.AssertionError at NavigationUiTest.kt:266`
- Android CI run `44495226091`
  - Branch/HEAD: `main` / `8dbbfbccc22268e5fcb322eacf6a3c9ae4ae2a42`
  - Conclusion: `failure`
  - Failed step: `Validate catalogs and governance manifest`
  - Exact errors: canonical FTP/Movies and Live TV JSON datasets were missing; the `Generate test timing summary` step also failed because `.github/scripts/summarize_gradle_test_results.py` was missing.
- Dependency & License Audit run `44487701325`: success
- Release Integrity Audit run `44487701353`: success

No Android Release run or GitHub Release was found for this audit.

## Failure Classification

Application test failure. The test assumed the built-in server inspect control was already in the Compose viewport. The UI implementation is unchanged; the test now scrolls to the stable `servers_list` test tag before clicking `server_inspect_builtin:0`.

The subsequent run failed because commit `01dce6e` removed the canonical catalog JSON inputs and the test timing summary helper while the CI workflow and catalog verifier still required them. The three required files were restored byte-for-byte from the previous revision where they were tracked. Local offline catalog validation and all 24 catalog verifier unit tests pass after restoration.

## Signing Configuration

- Existing keystore: `my-upload-key.p12`
- Format: PKCS12
- Alias: `upload`
- Gradle fallback: `${rootDir}/my-upload-key.p12`
- Gradle `storeType`: `PKCS12`
- Existing public certificate fingerprint: `E2:1B:27:62:1C:99:86:50:B5:7F:F7:C7:49:A2:63:F5:29:2B:B2:E1:7D:1F:BB:4F:08:59:1E:3A:8B:69:AA:7A`
- The public certificate fingerprint matched `upload-certificate.pem`.
- The private keystore and public certificate remain ignored by Git.
- Signing passwords were not requested or exposed.

GitHub Environment secret names:

- `KEYSTORE_BASE64`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

The Environment API did not expose `android-release` or its secret configuration to the authenticated token. A protected Environment named `android-release` and these Environment Secrets must be configured before a release build.

## Files Changed

- `app/src/main/java/com/jubayerhossain/bdixtester/feature/servers/ServersScreen.kt`
- `app/src/test/java/com/jubayerhossain/bdixtester/NavigationUiTest.kt`
- `docs/release/APP_SIGNING_STRATEGY.md`
- `docs/release/SIGNING_AND_KEY_MANAGEMENT.md`
- `docs/release/GITHUB_ACTIONS_DIAGNOSTIC_REPORT.md`
- `.github/scripts/summarize_gradle_test_results.py` (restored)
- `public/assets/aistudio/bdix_ftp_movies_catalog_4acd8ccb.json` (restored)
- `public/assets/aistudio/bdix_live_tv_catalog_70b3542f.json` (restored)

## Fixes Applied

- Explicitly configured Gradle and `keytool` to use PKCS12.
- Preserved alias `upload`.
- Added APK signer fingerprint comparison against the existing certificate fingerprint.
- Added a stable `servers_list` test tag and updated the navigation test to scroll before clicking.
- Updated release signing documentation to reference the existing `.p12` keystore.
- Restored CI's required catalog verifier inputs and Gradle test timing helper.

## Local Validation

- Gradle wrapper tasks were discovered, including `assembleRelease`, `lintVitalRelease`, and `testDebugUnitTest`.
- The Gradle wrapper is executable.
- `git diff --check` passed.
- The public certificate SHA-256 fingerprint matched the expected fingerprint.
- Private signing files remain ignored.
- `python3 -m tools.catalog_verifier validate-catalog --repo-root .`: passed (752 curated entries, 26 OTT entries, 667 FTP/Movies and 90 Live TV endpoints, zero validation errors).
- Catalog verifier unit tests: 24 passed.
- Test timing helper: `py_compile` passed.
- Android SDK validation is blocked because no SDK location is configured in this environment.
- Local tests, lint, APK assembly, and `apksigner` verification could not complete without an Android SDK.

## APK and Release Status

- Expected artifact: `BDIX-Tester-v1.0.apk`
- APK signature verification: not executed because no signed release APK or Android SDK was available locally.
- Observed APK signer fingerprint: unavailable.
- GitHub Release publication: not attempted.
- Published GitHub Release URL: none.

## Remaining Blockers

1. Configure protected GitHub Environment `android-release`.
2. Add `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` as Environment Secrets without exposing values.
3. Confirm the environment deployment approver policy.
4. Push the reviewed changes and rerun Android CI; the new run has not yet been executed.
5. Trigger Android Release only on a verified `v1.0` tag after CI passes and environment secrets are available.
6. Monitor with `gh run watch` and independently verify the published APK fingerprint.
