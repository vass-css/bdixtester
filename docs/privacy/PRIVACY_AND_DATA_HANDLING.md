# Privacy and Data Handling — BDIX Tester

**Updated:** 2026-11-18  
**Developer:** Jubayer Hossain (`JubayerHossain`)  
**Application ID:** `com.jubayerhossain.bdixtester`

---

## 1. Local-First Privacy Guarantees

- **No Accounts or Login:** Every feature operates strictly offline/local-first without registration, Google Sign-In, or remote user profiles.
- **No Ad SDKs, Trackers, or Crash Telemetry:** Zero advertising networks, analytics libraries, or device-fingerprinting identifiers.
- **Zero Location or Wi-Fi SSID Access:** Does not declare or request `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, or `ACCESS_WIFI_STATE`. Users may optionally enter a manual network profile label (e.g., `"Home ISP"`) stamped locally onto scan history records.
- **Zero Broad Storage Permissions:** File exports (`CSV`, `JSON`, `TXT`) use Android's Storage Access Framework (`ActivityResultContracts.CreateDocument`), never requesting `READ_EXTERNAL_STORAGE` or `READ_MEDIA_*`.

## 2. Permissions & Merged Manifest Audit

| Permission                                | Justification                                                                                                                                                                                                          | Source                             |
| ----------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- |
| `android.permission.ACCESS_NETWORK_STATE` | Read-only check via `ConnectivityManager` to report active connectivity and enforce strict Wi-Fi-only remote sync (`TRANSPORT_WIFI`, recognizing `TRANSPORT_WIFI + TRANSPORT_VPN` while rejecting VPN-only ambiguity). | `app/src/main/AndroidManifest.xml` |
| `android.permission.INTERNET`             | User-initiated HTTP/HTTPS/FTP endpoint probes, opt-in speed tests, and optional HTTPS remote catalog sync.                                                                                                             | `app/src/main/AndroidManifest.xml` |

Transitive merged manifest entries added by Jetpack WorkManager (`WAKE_LOCK`, `FOREGROUND_SERVICE` / constraint receivers) are documented in `/docs/testing/SECURITY_AUDIT.md`.

## 3. Data Stored Locally on Device

| Store                                      | Contents                                                                                                                                                                                                                                     | User Controls                                                                                                                                                     |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Room (`bdix_tester.db`)                    | Built-in `369`-entry catalog (`BUILT_IN`), signed `REMOTE_CATALOG`, user `USER_CUSTOM` endpoints, `favorites` (pin + order), `test_history` (snapshot URL/name, latencies, status, speed test outcome), `scan_sessions`, `catalog_metadata`. | Delete individual history rows; clear History, Favorites, Custom Servers, or Reset All Local Data in Settings; configurable retention (`7 / 30 / 90 / 365` days). |
| DataStore (`bdix_settings.preferences_pb`) | Theme (`SYSTEM/LIGHT/DARK`), language (`SYSTEM/EN/BN`), concurrency cap (`1–5`, default `3`), timeouts, speed-test byte cap (default `5 MiB`), Wi-Fi-only sync toggle, optional remote manifest URL, manual network label.                   | Editable in Settings; reset via Settings clear actions.                                                                                                           |

## 4. Network Connections & Export Safety

- **Scanning & Speed Test:** Initiated exclusively on user tap; `ftp://` uses unauthenticated RFC 959 control greeting + `QUIT` only; `ftps://` fails cleanly as `UNSUPPORTED` without plain-FTP downgrade; speed tests require explicit opt-in confirmation, force `Accept-Encoding: identity`, stop cleanly at `maxBytes` without extra EOF read, and prompt before following cross-host redirects.
- **Remote Catalog Sync:** Off by default (`autoCatalogUpdates = false`, empty URL). Uses HTTPS-only feeds authenticated by detached P-256 ECDSA signature over raw UTF-8 manifest bytes + SHA-256 payload digest.
- **CSV Export Neutralization:** Neutralizes leading formula characters (`=`, `+`, `-`, `@`, `\t`, `\r`) with a leading `'` to prevent spreadsheet formula injection while canonical JSON preserves verbatim URLs.
