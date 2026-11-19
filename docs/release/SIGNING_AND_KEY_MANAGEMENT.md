# Release Signing & Key Management — BDIX Tester

- **Updated:** `2026-11-19`
- **Package ID:** `com.jubayerhossain.bdixtester`
- **Distribution Target:** GitHub Releases (Signed APK Only)

---

## 1. Current Repository Signing State

Inspected `app/build.gradle.kts:28–76`, `.gitignore:13–16`, and `debug.keystore.base64`:

- **Release Signing Configuration (`app/build.gradle.kts:29–35, 69`):**
  - `storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.p12")`
  - `storeType = "PKCS12"`
  - `storePassword = System.getenv("STORE_PASSWORD")`
  - `keyAlias = "upload"` *(configurable or matched during keystore generation)*
  - `keyPassword = System.getenv("KEY_PASSWORD")`
  - **Fail-Closed Guarantee:** No debug keystore fallback exists on `release`. Building `assembleRelease` without valid credentials cleanly refuses/fails signing.
- **Workspace Keystore Status:** No `my-upload-key.jks`, `.keystore`, or private password exists in the repository (`BLOCKED` until the developer generates their real keystore offline).
- **Zero Debug Signing Risk:** `signingConfigs.debugConfig` applies exclusively to `buildTypes.debug`; `buildTypes.release` uses `signingConfigs.release` strictly.

## 2. Generating Your Real Production Release Keystore (Offline)

**Never** generate a fake/placeholder production key or commit a release keystore to Git. Generate your durable release keystore once on your personal trusted computer outside the Git working tree:

```bash
keytool -genkeypair -v \
  -keystore ~/bdixtester-keys/bdixtester-release.jks \
  -alias upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=Jubayer Hossain, OU=Android Release, O=JubayerHossain, L=Barisal, C=BD"
```

Record your **SHA-256 certificate fingerprint** immediately:

```bash
keytool -list -v \
  -keystore ~/bdixtester-keys/bdixtester-release.jks \
  -alias upload | grep -E "Alias|SHA256:"
```

## 3. Backup & Recovery Rules

Because GitHub Releases APK updates require the **exact same signing certificate** across every future release (`v1.0.0`, `v1.1.0`, `v2.0.0`...):

1. Losing the release keystore means **existing users cannot install APK updates** without uninstalling first (losing local Room history, favorites, and custom servers).
2. Store encrypted offline copies of `bdixtester-release.jks` plus `STORE_PASSWORD` and `KEY_PASSWORD` in at least two independent secure locations (e.g., an encrypted password manager vault + an offline encrypted USB/hardware backup).
3. Never paste passwords, keystores, or Base64 secrets into chat, Git commits, `.env.example`, or PR logs.

## 4. GitHub Actions Secrets Provisioning

Encode the keystore locally (without trailing newline surprises) and configure protected **GitHub Repository Environment (`android-release`) Secrets**:

```bash
# Linux / macOS:
base64 -w 0 ~/bdixtester-keys/bdixtester-release.jks
```

*(On macOS BSD `base64`, use `base64 -i ~/bdixtester-keys/bdixtester-release.jks | tr -d '\n'`.)*

Set these exact secrets in the **`android-release` GitHub Environment** (with *Required reviewers* enabled):

| GitHub Secret Name | Value |
| :--- | :--- |
| `KEYSTORE_BASE64` | Single-line Base64 output of your real `.jks` keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password for alias `upload` |

## 5. Mandatory Pre-Publication APK Verification

Before publishing any signed APK (locally or in `.github/workflows/android-release.yml`), verify all four checks using Android SDK `build-tools/36.0.0`:

```bash
# 1. Verify v2/v3 APK signature and print certificate SHA-256 fingerprint
"$ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner" verify --verbose --print-certs BDIX-Tester-v1.0.0.apk

# 2. Verify package ID, versionCode, versionName, minSdk, targetSdk, and non-debuggable status
"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump badging BDIX-Tester-v1.0.0.apk | head -n 20
"$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2" dump xmltree BDIX-Tester-v1.0.0.apk --file AndroidManifest.xml | grep -i debuggable || echo "OK: not debuggable"

# 3. Generate SHA-256 checksum
sha256sum BDIX-Tester-v1.0.0.apk > SHA256SUMS.txt
```

Confirm that:

- Certificate SHA-256 matches your recorded release keystore fingerprint (never Android Debug `CN=Android Debug`).
- `package: name='com.jubayerhossain.bdixtester'` matches unchanged.
- `android:debuggable` is **absent** (or `false`).
