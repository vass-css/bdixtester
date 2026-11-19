# GitHub Release Guide — BDIX Tester

This guide documents the end-to-end procedure for building, verifying, and publishing signed APK releases of **BDIX Tester** (`com.jubayerhossain.bdixtester`) via GitHub Releases.

## 1. Distribution Scope & Policy

- **Primary Release Channel:** GitHub Releases (no Google Play AAB published in this cycle; `.github/workflows/android-release.yml` builds and signs the release APK only).
- **Package Name:** `com.jubayerhossain.bdixtester`
- **Versioning Rule:** Git release tags must match `versionName` in `app/build.gradle.kts` (`v<versionName>`, e.g., `v1.0` for `versionName = "1.0"`, `versionCode = 1`).
- **Signing Requirement:** Releases must be signed using the maintainer's offline RSA-4096 upload keystore (`keyAlias = "upload"`). Debug-signed or unsigned APKs must never be published.

## 2. Standard Release Asset Set

Every public GitHub Release must include exactly these verified files:

1. `BDIX-Tester-v<versionName>.apk` — Minified, resource-shrunk, aligned, V2/V3 signed release APK.
2. `SHA256SUMS.txt` — Standard SHA-256 digest file matching the release APK filename.
3. `release-manifest.json` — Machine-readable provenance record.
4. `RELEASE-NOTES.md` — Human-readable release summary and checksum verification commands.

### Do Not Publish

- Debug APKs (`app-debug.apk`)
- Keystores (`*.jks`, `*.keystore`, `*.p12`) or signing passwords
- `.env` or `local.properties` files
- Temporary Gradle/build outputs or unverified AAB bundles

## 3. `release-manifest.json` Schema

```json
{
  "application_name": "BDIX Tester",
  "package_name": "com.jubayerhossain.bdixtester",
  "version_name": "1.0",
  "version_code": 1,
  "min_android_sdk": 24,
  "target_android_sdk": 36,
  "build_variant": "release",
  "artifact_filename": "BDIX-Tester-v1.0.apk",
  "signing_certificate_sha256": "<64-hex-chars>",
  "apk_sha256": "<64-hex-chars>",
  "build_timestamp_utc": "YYYY-MM-DDTHH:MM:SSZ",
  "source_commit": "<40-char-git-sha>",
  "git_tag": "v1.0"
}
```

## 4. `RELEASE-NOTES.md` Template

```markdown
# BDIX Tester v1.0

- **Package Name:** `com.jubayerhossain.bdixtester`
- **Version:** `1.0` (`versionCode = 1`)
- **Compatibility:** Android 7.0+ (API 24) · Target SDK 36
- **Artifact:** `BDIX-Tester-v1.0.apk`
- **APK SHA-256:** `<apk_sha256>`
- **Signing Certificate SHA-256:** `<signing_cert_sha256>`

## Verification Before Installing

```bash
sha256sum -c SHA256SUMS.txt
apksigner verify --verbose --print-certs BDIX-Tester-v1.0.apk
```

See `CHANGELOG.md` and `README.md` for full details, permissions explanations, and Play Protect sideload guidance.
```

## 5. Pre-Tag Approval Checklist

1. Confirm `versionCode` (monotonically incremented on updates) and `versionName` in `app/build.gradle.kts`.
2. Ensure `CHANGELOG.md` and `README.md` reflect the release notes accurately.
3. Verify `Android CI` (`android-ci.yml`), `Dependency & License Audit` (`dependency-audit.yml`), and `Release Integrity Audit` (`release-integrity.yml`) pass on `main`.
4. Confirm GitHub `android-release` protected environment holds `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` with required reviewer approval enabled.
5. Push annotated tag `v<versionName>` only after maintainer sign-off; approve the `android-release` environment deployment gate once `build_and_verify` finishes cleanly.
