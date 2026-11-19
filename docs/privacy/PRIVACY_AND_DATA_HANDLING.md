# Privacy, Data Handling & Catalog Legal Risk Audit — BDIX Tester

- **Audit Date:** `2026-11-19`
- **Developer:** Jubayer Hossain (`jubayerhossainbarisal@gmail.com`)
- **Application ID:** `com.jubayerhossain.bdixtester`

---

## 1. Local-First & Open-Source Release Guarantees

- **Zero Accounts or Remote Profiles:** All diagnostic workflows run locally without sign-in or backend user accounts.
- **Zero Ad SDKs / Trackers in Current Codebase:** Inspected `gradle/libs.versions.toml`, `app/build.gradle.kts`, and merged manifests — zero `play-services-ads`, `ads-mobile-sdk`, `user-messaging-platform`, Firebase Analytics, or `AD_ID` permissions exist in the codebase.
- **Zero Location / Wi-Fi SSID / Broad Storage Access:** Neither `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_WIFI_STATE`, `READ_EXTERNAL_STORAGE`, nor `READ_MEDIA_*` appears anywhere in source or merged manifest.
- **Optional Future Hybrid Architecture Boundary:** Any future Play Store / AdMob edition remains unimplemented and explicitly gated behind separate consent, `app-ads.txt`, and policy review.

## 2. Permissions & Background Services Audit

| Permission | Justification | Source |
| :--- | :--- | :--- |
| `android.permission.ACCESS_NETWORK_STATE` | Read-only transport check via `ConnectivityManager` to surface connection status and enforce strict Wi-Fi-only remote sync (`StrictWifiValidator`). | `app/src/main/AndroidManifest.xml:4` |
| `android.permission.INTERNET` | User-initiated HTTP/HTTPS/FTP endpoint probes, opt-in speed tests, optional signed HTTPS catalog sync, and DoH/RDAP governance lookups. | `app/src/main/AndroidManifest.xml:5` |
| `android.permission.POST_NOTIFICATIONS` | Optional completion summary notification on Android 13+ after manual/background catalog verification (`VerificationNotificationHelper`), gracefully requested in `CatalogGovernanceScreen` with silent fallback if denied. | `app/src/main/AndroidManifest.xml:6` |

Transitive Jetpack WorkManager permissions (`WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `DYNAMIC_RECEIVER_NOTED_EXPORTED_PERMISSION`) are isolated strictly to `CatalogSyncWorker` and `CatalogVerificationWorker`.

## 3. Data Stored Locally on Device

| Store | Verified Contents | User Controls |
| :--- | :--- | :--- |
| Room (`bdix_tester.db`, schema v2) | `752` curated built-in BDIX endpoints (`BundledBdixCatalog`), optional signed remote catalog cache, user `USER_CUSTOM` servers, `favorites` (pin + order), `test_history`, `scan_sessions`, `catalog_metadata`, `endpoint_governance`, and `verification_events`. | Archive/restore governance items, delete individual history rows, clear History/Favorites/Custom Servers, Reset All Local Data, configurable retention (`7 / 30 / 90 / 365` days). |
| DataStore (`bdix_settings.preferences_pb`) | Theme preset, Light/Dark preference, app language (`SYSTEM/EN/BN`), reduced animations, concurrency cap (`1–5`), timeouts, speed-test cap, Wi-Fi-only sync toggle, optional remote URL, manual network label. | Editable in Settings; reset via Settings reset action. |

## 4. Network & External Service Audit

- **Endpoint Probing (`HttpEndpointTester`, `FtpControlTester`):** User-initiated only; blocks automatic HTTPS-to-HTTP redirects (`PROTOCOL_DOWNGRADE_BLOCKED`) and rejects `ftps://` cleanly without plain-FTP downgrade.
- **Opt-In Speed Test (`SpeedTestEngine`):** Requires explicit user confirmation, forces `Accept-Encoding: identity`, stops cleanly at configured `maxBytes`, and prompts on cross-host redirects.
- **Catalog Governance Verification (`DohDnsResolver`, `RdapLookupClient`):** Contacts `https://cloudflare-dns.com/dns-query`, `https://dns.google/resolve`, `https://dns9.quad9.net:5053/dns-query`, `https://data.iana.org/rdap/dns.json`, and TLD RDAP registries over HTTPS solely on user-triggered or enabled governance runs.
- **Remote Catalog Sync (`RemoteCatalogSyncManager`):** Disabled by default (`autoCatalogUpdates = false`, empty URL); requires HTTPS + detached ECDSA P-256 verification (`CatalogSignatureVerifier`). **Operator Prerequisite:** `TrustedKeyRegistry.DEFAULT_KEYS` (`CatalogSignatureVerifier.kt:53`) currently holds a placeholder SPKI public key and zero private keys; replace it before publishing a live remote manifest feed.
- **CSV Export Neutralization:** `CatalogExportSerializer` neutralizes leading formula tokens (`=`, `+`, `-`, `@`, `\t`, `\r`) before writing via SAF `CreateDocument`.

---

## 5. Evidence-Based Catalog Copyright, Piracy & Distribution Policy Risk Report

Per maintainer directive, **all `752` curated BDIX entries and `26` OTT entries remain preserved verbatim** in source code. None were silently removed. However, any public GitHub repository, Google Play submission, or future AdMob integration must account for the following policy risks verified by code inspection:

### A. Licensed / Official Directory Tier (Low Policy Risk)

- **`BdOttPlatforms.kt` (`26` HTTPS platforms):** Curated official websites of Bangladeshi and international OTT providers (`chorki.net`, `hoichoi.tv`, `toffeelive.com`, `bioscopelive.com`, `tsports.com`, `netflix.com`, `primevideo.com`, `zee5.com`, `tv.apple.com`, `crunchyroll.com`, etc.). Note trademark accuracy only (do not impersonate these brands in store graphics).

### B. ISP Infrastructure & Generic BDIX Portals (Low-to-Moderate Availability Risk)

- **`BundledFtpCatalog.kt` & `BundledFtpCatalogPart2.kt` (`542` FTP/Files entries, including RFC 1918 subnets):** Majority are local Bangladesh ISP FTP indexes and private peering IPv4 gateways (`103.58.73.6`, `172.16.50.4`, `ftp.daffodilnet.com`, etc.). Availability depends entirely on user ISP peering, and `550 / 752` curated entries use plain `http://` (`network_security_config.xml:10` permits cleartext globally so dynamic hosts can be probed).

### C. High Copyright / Piracy-Associated Domains Flagged for Future Store Submissions (CRITICAL POLICY RISK)

By direct inspection of source code, the following categories of entries exist verbatim inside the bundled catalog:

1. **Public / Known Unauthorized Streaming & Movie Piracy Domains:**
   - `https://genvideos.org` (`BundledMediaCatalog.kt:184`, attributed `"Unknown FTP Server"`)
   - `https://moviescounterhd.club` (`BundledMediaCatalog.kt:137`, `"MovieCounter"`)
   - `https://mlwbd.mobi` (`BundledFtpCatalogPart2.kt:19`, `"MLWBD"`)
   - `https://crichd.tv` (`BundledMediaCatalog.kt:289`, `"Others Live TV Server"`)
   - `https://moviefreak.xyz` (`BundledMediaCatalog.kt:147`), `http://filmguro.site` (`BundledMediaCatalog.kt:115`)
2. **ISP-Hosted Unlicensed VOD / Emby Media Indexes & Third-Party Paste/IPTV Links:**
   - Emby `:8096` and VOD portals in `BundledMediaCatalog.kt:16–185` (e.g., `dhakamovie.com`, `ctgmovies.com`, `khulnaflix.net`, `moviehaat.net`, `moviedom.live`, `dflix.live`, `bdiptv.stream`, `bdiptv.net`, etc.)
   - `FtpLinks.kt` verbatim audit links include third-party link aggregators such as `pastebin.com` / dead/parked legacy URLs.

### D. Legal & Distribution Guidance

- **For MIT GitHub Releases (Current Scope):** BDIX Tester does **not** scrape, stream, cache, or download copyrighted media—it only checks TCP/HTTP/FTP reachability (`HEAD` / bounded 1 KiB `Range GET`) and hands `http`/`https` URLs to the user's external browser. Maintain clear disclaimers in `README.md` and `PRIVACY_POLICY.md` stating the app is a network reachability diagnostic tool and directory only.
- **If Later Submitted to Google Play or Linked to AdMob:** Google Play Intellectual Property / Unauthorized Streaming policies and AdMob Prohibited Content rules enforce strict liability even for **app-bundled directories linking externally** to piracy-associated domains (`mlwbd.mobi`, `crichd.tv`, `genvideos.org`, `moviescounterhd.club`, unauthorized Emby/IPTV indexes). **Before** applying for Google Play or AdMob monetization in the future, plan an explicit operator review separating pure ISP diagnostic/institutional hosts from flagged streaming/movie domains.
