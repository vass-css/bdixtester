# Security Policy — BDIX Tester

## Supported Versions

Security fixes and vulnerability patches are applied on the active `main` branch and published in subsequent signed GitHub Releases for Android 7.0 (`minSdk 24`) through Android 16 (`targetSdk 36`).

| Version Track | Package ID | Support Status |
| :--- | :--- | :--- |
| Latest `v*` GitHub Release | `com.jubayerhossain.bdixtester` | Active security support |
| Older pre-release snapshots | `com.jubayerhossain.bdixtester` | Upgrade to latest signed APK |

## Reporting a Vulnerability

Please do **not** open public GitHub issues for suspected security vulnerabilities, cryptographic weaknesses, or remote catalog signature bypasses.

- **Primary Method:** Use GitHub Security Advisories (*Report a vulnerability*) on the official repository once hosted.
- **Direct Maintainer Email:** `jubayerhossainbarisal@gmail.com`

Include:

1. App version (`versionName` / `versionCode`) and Android version/device details.
2. Clear reproduction steps or proof of concept.
3. Affected component (for example: HTTP/FTP probe engine, speed test limiter, remote catalog ECDSA verifier, or Room/DataStore storage).
4. Assessment of impact and suggested remediation, if known.

## Response Targets

- **Acknowledgement:** Within 72 hours.
- **Triage & Initial Assessment:** Within 7 days.
- **Fix & Advisory:** Coordinated prior to public disclosure when verified.

## Security Design Boundaries

- **Release Signing Integrity:** Official GitHub Release APKs are signed exclusively by the maintainer's private release keystore outside source control. Verify downloaded APKs against `SHA256SUMS.txt` and the signing certificate fingerprint in `release-manifest.json`.
- **Signed Remote Catalog Updates:** Optional remote catalog manifests require `https://` URLs and detached `SHA256withECDSA` (P-256) verification over exact raw UTF-8 bytes (`CatalogSignatureVerifier`). Remote sync stays disabled unless the user configures a trusted manifest URL.
- **Network Scope:** BDIX Tester probes only built-in catalog endpoints and user-added custom endpoints when triggered by the user. `HttpEndpointTester` and `SpeedTestEngine` block automatic `https://` to `http://` protocol downgrades.
- **Out-of-Scope:** Unreachable BDIX servers caused by ISP peering restrictions, third-party websites opened in the external browser, or modifications made in unofficial APK repacks signed with an untrusted certificate.
