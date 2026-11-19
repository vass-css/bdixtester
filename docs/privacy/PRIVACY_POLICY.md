# Privacy Policy — BDIX Tester

- **Effective Date:** `2026-11-19`
- **Developer:** Jubayer Hossain (`jubayerhossainbarisal@gmail.com`)
- **Application ID:** `com.jubayerhossain.bdixtester`
- **License:** MIT License

## 1. Overview

BDIX Tester is a local-first network diagnostic and server discovery utility for Android (`minSdk 24`, `targetSdk 36`). It tests HTTP, HTTPS, and FTP reachability from your active device network to Bangladesh broadband (BDIX) servers and user-defined custom endpoints, and includes a curated directory of Bangladesh OTT platforms.

This policy applies to the open-source **GitHub Releases** APK build (`com.jubayerhossain.bdixtester`). No published website URL is claimed prior to repository hosting; this document is formatted for direct GitHub Pages or repository hosting once published.

## 2. Information We Do Not Collect

- **No Accounts or Login:** Every feature works without registration, sign-in, or cloud profiles.
- **No Analytics, Crash Telemetry, or Ad SDKs:** The MIT GitHub Releases build contains zero advertising SDKs, zero `AD_ID` permission declarations, zero behavioral analytics, and zero remote crash reporting.
- **No Location or Hardware Identifiers:** The app never requests `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_WIFI_STATE`, phone state, IMEI, or MAC addresses, and does not read your Wi-Fi SSID or BSSID.
- **No Broad Storage Access:** Exporting scan results (`CSV`, `JSON`, `TXT`) and importing external ISP probe JSON files use Android's user-driven Storage Access Framework (`CreateDocument` / `OpenDocument`), with zero `READ_EXTERNAL_STORAGE` or `READ_MEDIA_*` permissions.

## 3. Data Stored Locally on Your Device

All operational state remains strictly on your device inside Room (`bdix_tester.db`) and Jetpack DataStore (`bdix_settings.preferences_pb`):

- **Catalog & Custom Endpoints:** Built-in catalog (`752` BDIX entries), optional signed remote catalog cache, and user-created custom endpoints.
- **Favorites & Scan History:** Saved/pinned endpoints, connectivity latency results, scan sessions, opt-in download speed measurements, and optional manual network profile labels you type (for example, `"Home ISP"`).
- **Governance & Verification Evidence:** Local DNS/RDAP diagnostic status snapshots and verification event logs (`endpoint_governance`, `verification_events`).
- **Preferences:** Theme preset, appearance mode, app language (`System`, `English`, `Bangla`), reduced motion setting, scan concurrency cap, timeouts, speed-test byte cap, retention window (`7 / 30 / 90 / 365` days), and optional remote manifest URL.
- **User Control:** You can delete individual history entries, clear history/favorites/custom servers, archive/restore catalog entries, or reset all local user data anytime inside Settings.

## 4. Network Connections Initiated by the App

BDIX Tester makes network requests solely when triggered by user interaction or user-configured background WorkManager tasks:

- **Endpoint Connectivity Probes & Opt-In Speed Tests:** Connects directly over HTTP, HTTPS, or unauthenticated FTP control (`QUIT` after greeting) to the built-in catalog endpoints or custom URLs you test. Plain `http://` and `ftp://` endpoints transmit unencrypted requests directly to those endpoints because many BDIX ISP servers only expose plain HTTP/FTP; BDIX Tester never injects credentials into unauthenticated probes and blocks automatic `https://` to `http://` downgrades.
- **Catalog Governance Verification (Manual / Scheduled):** When you initiate governance checks, `DohDnsResolver` queries public DNS-over-HTTPS resolvers (`https://cloudflare-dns.com/dns-query`, `https://dns.google/resolve`, `https://dns9.quad9.net:5053/dns-query`) and `RdapLookupClient` queries IANA RDAP bootstrap (`https://data.iana.org/rdap/dns.json`) and authoritative registry RDAP endpoints over HTTPS solely for catalog domain health/expiration evidence.
- **Optional Signed Remote Catalog Sync:** Disabled by default (`autoCatalogUpdates = false`, blank URL). When configured with an `https://` manifest URL, WorkManager (`CatalogSyncWorker`) obeys Wi-Fi-only preferences and verifies a detached `SHA256withECDSA` (P-256) signature and SHA-256 digest before applying updates.
- **External Browser Handoff:** Tapping a server or OTT link opens your device's default browser via `Intent.ACTION_VIEW` (strictly `http://` or `https://`). Once opened externally, the destination website's own privacy policy governs your browsing session.

## 5. Permissions & Background Tasks

- **`android.permission.INTERNET`:** Executes user-requested connectivity probes, opt-in speed tests, optional HTTPS catalog sync, and DoH/RDAP governance checks.
- **`android.permission.ACCESS_NETWORK_STATE`:** Reads local OS connectivity transport (`Wi-Fi`, `Mobile Data`, `Ethernet`, `Offline`) to warn when off Wi-Fi and enforce Wi-Fi-only remote sync.
- **`android.permission.POST_NOTIFICATIONS`:** Optional notification permission requested on Android 13+ solely to display a low-priority completion summary when background catalog verification finishes. Verification works normally if denied.
- **WorkManager (`CatalogSyncWorker`, `CatalogVerificationWorker`):** Merges standard AndroidX WorkManager permissions (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`) used strictly for user-enabled catalog tasks.

## 6. Device Backup & Transfer

Android `allowBackup` is enabled with explicit `backup_rules.xml` and `data_extraction_rules.xml` configuration rules governing Room database and DataStore preferences on compatible Android versions. Users who want zero cloud backup can also disable OS device backup at the Android system level or export/reset data locally.

## 7. Future Hybrid Monetization Notice

No advertising SDKs or ad-tracking identifiers are present in this GitHub Releases version. Should any future distribution variant introduce Google AdMob or similar monetization, that variant will first require explicit consent management (such as Google UMP), updated manifest/Data Safety declarations, and an updated privacy policy specific to that variant.

## 8. Contact

For privacy questions, data-handling inquiries, or security disclosures, contact **Jubayer Hossain** at `jubayerhossainbarisal@gmail.com`.
