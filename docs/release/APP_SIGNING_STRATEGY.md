# App Signing Strategy — BDIX Tester

- **Primary Release Distribution:** Signed release APK via GitHub Releases (`BDIX-Tester-v<versionName>.apk`). AAB bundling (`bundleRelease`) is omitted in this release cycle per maintainer preference unless future Google Play Store enrollment is requested.
- **Release Key Configuration (`app/build.gradle.kts`):**
  - `storeFile` read from `KEYSTORE_PATH` environment variable (fallback `${rootDir}/my-upload-key.p12`)
  - `storeType = "PKCS12"`
  - `storePassword` read from `STORE_PASSWORD`
  - `keyAlias = "upload"`
  - `keyPassword` read from `KEY_PASSWORD`
- **Security Guarantee:** Zero upload keystores or passwords are committed in source control or `.env.example`. CI injects `KEYSTORE_BASE64`, `STORE_PASSWORD`, and `KEY_PASSWORD` solely inside the protected `android-release` GitHub Environment and deletes `$RUNNER_TEMP` keystore copies unconditionally after building.
- **Certificate Continuity:** Re-use the same `upload` keystore for every future release so Android preserves existing user favorites, custom servers, settings, and history during in-place APK upgrades.
- **Catalog Manifest Signing Separation:** Remote catalog signing uses an independent P-256 ECDSA keypair (`TrustedKeyRegistry`) whose private key never touches the Android project or APK signing pipeline.
