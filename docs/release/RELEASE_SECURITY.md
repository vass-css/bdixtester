# Release Security & Supply Chain Policy — BDIX Tester

- **Application ID:** `com.jubayerhossain.bdixtester`
- **License:** MIT (`LICENSE`)
- **Security Contact:** `jubayerhossainbarisal@gmail.com`

## 1. Signing & Key Boundary

- **Zero In-Repo Production Keys:** Neither the repository nor `.env.example` contains a production keystore or signing passwords. `app/build.gradle.kts` reads `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` exclusively from environment variables (`keyAlias = "upload"`).
- **Zero Debug Fallback on Release:** `buildTypes.release` uses `signingConfigs.getByName("release")`. Attempting `:app:assembleRelease` without a configured keystore fails immediately at `:app:validateSigningRelease`.
- **Single-Certificate Continuity:** Future APK updates distributed via GitHub Releases must be signed by the exact same upload certificate so Android allows in-place upgrades without uninstalling user favorites, custom servers, or scan history.
- **Automated Verification Before Publication:** `.github/workflows/android-release.yml` validates:
  - `apksigner verify --verbose --print-certs` passes and does **not** match `CN=Android Debug`.
  - `aapt2 dump badging` confirms package `com.jubayerhossain.bdixtester`, expected `versionCode`/`versionName`, and absence of `application-debuggable`.
  - Temporary decoded keystore files under `$RUNNER_TEMP` are removed unconditionally via `if: always()`.

## 2. GitHub Actions Hardening

- **Immutable Action Pinning:** Every third-party GitHub Action in `.github/workflows/*.yml` is pinned to a verified 40-character commit SHA and audited automatically by `.github/workflows/release-integrity.yml`.
- **Least-Privilege Permissions:** All CI, audit, and build jobs run with `permissions: contents: read`. Only the `publish_release` job in `android-release.yml` requests `contents: write`, and only on signed tag pushes gated by the `android-release` protected environment.
- **Pull Request Secret Isolation:** PR workflows (`android-ci.yml`, `dependency-audit.yml`, `release-integrity.yml`) have no access to `android-release` signing secrets.

## 3. Remote Catalog Cryptographic Boundary

- `CatalogSignatureVerifier.kt` verifies detached ECDSA P-256 (`SHA256withECDSA`) signatures directly against raw UTF-8 manifest bytes before JSON parsing, rejects UTF-8 BOMs, enforces HTTPS-only transport, rejects cross-host downgrades, and validates payload SHA-256 digests (`RemoteCatalogSyncManager.kt`).
- **Known Audit Blocker for Remote Feeds:** `TrustedKeyRegistry.DEFAULT_KEYS` currently ships a placeholder SPKI key (`bdix-catalog-2026-v1`). Remote feed updates remain disabled by default (`remoteCatalogUrl` unset); an independent offline P-256 keypair must be generated and its public SPKI compiled before hosting any remote catalog manifest.

## 4. Dependency & License Audit Summary

Runtime APK dependencies (`releaseRuntimeClasspath`, configured in `gradle/libs.versions.toml` and `app/build.gradle.kts`) are open-source and compatible with MIT redistribution:

| Library Group | Usage Scope | License | Notes |
| --- | --- | --- | --- |
| AndroidX (`core-ktx`, `activity-compose`, `navigation-compose`, `lifecycle-*`, `room-*`, `work-runtime-ktx`, `datastore-preferences`) | Runtime APK | Apache-2.0 | Standard Jetpack components |
| Jetpack Compose BOM (`2026.06.01`: `ui`, `material3`, `material-icons-*`) | Runtime APK | Apache-2.0 | UI toolkit |
| Kotlinx Coroutines (`core`, `android`) | Runtime APK | Apache-2.0 | Concurrency & Flow |
| Square OkHttp (`5.4.0`) | Runtime APK | Apache-2.0 | HTTP/TLS & DoH networking |
| JUnit 4 (`junit`) | `testImplementation` only | EPL-1.0 | Excluded from release APK |
| Robolectric (`4.18.1`), MockWebServer, Room/Work Testing | `testImplementation` only | MIT / Apache-2.0 | Excluded from release APK |

- `dependenciesInfo { includeInApk = false; includeInBundle = true }` prevents encrypted Google Play dependency metadata blocks from being embedded into GitHub-distributed APKs.
- No AdMob (`play-services-ads`), UMP consent, Firebase Analytics, or third-party telemetry SDKs are compiled into the APK.

## 5. End-User Verification & Play Protect Notice

- Users verifying APK authenticity should compare `sha256sum` against `SHA256SUMS.txt` and optionally inspect the signing certificate SHA-256 fingerprint via `apksigner verify --print-certs`.
- Because GitHub Releases distributes outside Google Play, Android's Play Protect may prompt for confirmation on first sideload; users should approve installation only for the verified APK and never disable Play Protect globally.
