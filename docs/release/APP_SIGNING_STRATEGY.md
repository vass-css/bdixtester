# App Signing Strategy — BDIX Tester

- **Play App Signing:** Enabled for production distribution on Google Play.
- **Upload Key Configuration (`app/build.gradle.kts`):**
  - `storeFile` read from `KEYSTORE_PATH` environment variable (default `${rootDir}/my-upload-key.jks`)
  - `storePassword` read from `STORE_PASSWORD`
  - `keyAlias = "upload"`
  - `keyPassword` read from `KEY_PASSWORD`
- **Security Guarantee:** Zero upload keystores or passwords are committed in source control or `.env.example`. CI workflow injects secrets solely on protected manual release dispatch.
- **Catalog Manifest Signing Separation:** Remote catalog signing uses an independent P-256 ECDSA keypair (`TrustedKeyRegistry`) whose private key never touches the Android project or APK/AAB signing pipeline.
