# Privacy Policy — BDIX Tester

**Effective Date:** 2026-11-18  
**Developer:** Jubayer Hossain (`JubayerHossain`)  
**Application ID:** `com.jubayerhossain.bdixtester`

## 1. Overview

BDIX Tester is a local-first network diagnostic utility for testing HTTP, HTTPS, and FTP connectivity to Bangladesh broadband (BDIX) servers and user-defined custom endpoints from your active network connection.

## 2. Data We Do Not Collect

- No user accounts, sign-in, or personal profiles.
- No analytics SDKs, crash telemetry, or third-party advertising SDKs.
- No location permissions (`ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`) and no collection of Wi-Fi SSID, BSSID, MAC addresses, or IMEI/hardware identifiers.
- No broad storage permissions (`READ_EXTERNAL_STORAGE` / `READ_MEDIA_*`); exports use Android's user-driven Storage Access Framework (`CreateDocument`).

## 3. Data Stored Locally on Device

- Built-in endpoint catalog, optional user-created custom endpoints, favorites/pins, scan/speed test history, optional manual network profile labels, and app preferences (Room + Jetpack DataStore).
- Users may export or permanently clear history, favorites, custom servers, or reset all local user data at any time from Settings.

## 4. Network Connections Initiated

- **Endpoint Probes & Optional Speed Tests:** Only initiated when requested by the user against endpoints in the catalog or custom endpoints entered by the user.
- **Optional Remote Catalog Updates:** Only enabled when the user configures an HTTPS manifest feed URL; verified via detached ECDSA P-256 signature and SHA-256 payload digest.

## 5. Contact

Developer: Jubayer Hossain (`JubayerHossain`)  
Public Privacy Policy Hosting Status: Pending publication at a public HTTPS URL prior to Google Play submission.
