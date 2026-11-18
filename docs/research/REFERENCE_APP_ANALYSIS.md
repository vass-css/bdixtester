# Reference Application Analysis — BDIX Tester

**Research Date:** 2026-11-18  
**Target Product:** BDIX Tester (`com.jubayerhossain.bdixtester`)  
**Developer:** Jubayer Hossain (`JubayerHossain`)

---

## 1. Reference Application Identity

| Attribute                          | Information                                                                        | Source / Confidence                                                                                 |
| ---------------------------------- | ---------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Public Title                       | BDIX Tester : BD Movie servers                                                     | Verified public Play Store listing                                                                  |
| Package / Application ID           | `com.shubhobrata.roy.bdixtester`                                                   | Verified public Play Store URL                                                                      |
| Displayed Store Developer          | `Mr.Unpredictable`                                                                 | Verified public Play Store listing                                                                  |
| Publicly Attributed Developer Name | `Shubhobrata Roy`                                                                  | User brief & public profile reference (not independently verified on GitHub/LinkedIn on 2026-11-18) |
| Category                           | Tools                                                                              | Verified public Play Store listing                                                                  |
| Primary Store URL                  | https://play.google.com/store/apps/details?id=com.shubhobrata.roy.bdixtester&hl=en | Official listing reference                                                                          |

> **Scope Boundary:** This analysis is for product research and competitive improvement only. **BDIX Tester** does not copy source code, proprietary implementation, branding, logos, screenshots, or visual assets from the reference application.

---

## 2. Public Google Play Metadata

_Classification: Verified public listing observations (as recorded in the project brief and rechecked via public search on 2026-11-18)._

| Property              | Observed Value              | Notes                                                         |
| --------------------- | --------------------------- | ------------------------------------------------------------- |
| Category              | Tools                       | —                                                             |
| Content Rating        | Everyone                    | —                                                             |
| Monetization          | Contains ads                | BDIX Tester excludes ad SDKs in Phases 1–3                    |
| Downloads             | 500K+                       | Indicates substantial demand among Bangladesh broadband users |
| Displayed Rating      | ~4.4 stars (~8.81K reviews) | Historical aggregate rating                                   |
| Displayed Update Date | February 24, 2026           | Subject to change on future store updates                     |

---

## 3. Publicly Described Features

_Classification: Public listing claims (not independently verified at binary level)._

1. **Categorized Server Directory:** Large directory of BDIX-related endpoints organized by categories (including FTP/movie/TV server portals) and described as human-verified.
2. **Automatic Database Updates:** Claimed ability to refresh the server catalog without requiring a full Play Store APK update.
3. **Low Data Consumption:** Marketed as requiring minimal mobile/broadband data for normal operation.
4. **Server Speed Testing:** Described as supporting download-speed tests against listed BDIX servers.

---

## 4. Screenshot-Based UI Observations

_Classification: Screenshot observations (historical/indexed public store imagery; may not reflect the latest production build)._

Observed UI elements in publicly indexed screenshots:

- Category navigation (`All`, `TV`, `FTP`, `Movie`).
- Featured server entries.
- Per-item fields: server name, server URL, category, working status, speed readout, and `Visit Website` action.
- Testing progress interface: circular progress indicator, working servers found count, total servers in database, `Quick Search` option, and `Stop` action.

**Mandatory Guardrails for BDIX Tester:**

- Observed counts, percentages, and latencies in screenshots are illustrative only and are **never** hardcoded.
- BDIX Tester separates live reachability from catalog metadata and never fabricates working-server counts or throughput figures.

---

## 5. Public Review Findings

_Classification: User feedback from public Play Store reviews._

| Feedback Theme               | Observation                                                                                     | Product Implication for BDIX Tester                                                                                                                                          |
| ---------------------------- | ----------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **History & Cross-Checking** | Users requested a way to identify previously visited or tested URLs.                            | Plan structured local testing history (Phase 9) with timestamps and network labels.                                                                                          |
| **Bookmarks & Pinning**      | Users asked to pin or bookmark frequently used servers.                                         | Plan persistent favorites independent of catalog updates (Phase 9).                                                                                                          |
| **Export Functionality**     | Users requested an ability to export lists/results.                                             | Plan CSV, JSON, and plain-text export via Storage Access Framework (Phase 9).                                                                                                |
| **Catalog Freshness**        | A September 25, 2025 review complained that dead/unavailable websites remained in the database. | Design explicit catalog versioning, staleness tracking, verification provenance, and safe remote updates (Phase 8).                                                          |
| **Historical Streaming**     | A January 28, 2020 review discussed changes to an earlier streaming experience.                 | Historical feedback only. BDIX Tester intentionally excludes in-app media streaming, downloading, and embedded WebViews; external browser intents handle visiting endpoints. |

---

## 6. Publicly Described Technology Stack

_Classification: Developer claims (attributed to public developer profile in the project brief; web search on 2026-11-18 did not independently locate a matching public GitHub/LinkedIn repository linking Shubhobrata Roy to BDIX Tester)._

- Kotlin, Android SDK, Coroutines
- MVVM with Clean Architecture
- Dagger 2 migrated to Hilt
- JUnit 4, Espresso, UI Automator

_Recommendation:_ Treat as architectural inspiration only. BDIX Tester uses Jetpack Compose, Material 3, single-activity/single-module MVVM, and lightweight constructor injection without adding Hilt unnecessarily.

---

## 7. Data Safety & Privacy Observations

_Classification: Developer-provided Play Store declarations._

The reference listing declares collection/sharing indicators covering app activity, app info/performance, device or other IDs, third-party data sharing (consistent with ad monetization), encryption status, and data deletion disclosures.

**BDIX Tester Improvement:** Adopt a strictly local-first, privacy-conscious model:

- No user accounts or registration.
- No ad SDKs, analytics trackers, or device ID harvesting.
- No location or Wi-Fi SSID permissions.
- No hidden background server scans.

---

## 8. Important Unknowns

- Internal HTTP/FTP client configuration, timeout thresholds, and retry behavior of the reference app are unknown.
- Exact methodology used by the reference app to calculate "speed" (latency vs. byte-transfer throughput) is unverified.
- Exact human-verification workflow and update cadence of the reference app's backend are unknown.

---

## 9. Strengths, Weaknesses & Product Decisions

### Strengths Worth Learning From

1. Simple, focused core workflow: pick category → scan → see reachable endpoints → open in browser.
2. Over-the-air catalog freshness without forced app updates.
3. Low-bandwidth operation suitable for metered/mobile fallback checks.

### Weaknesses Worth Improving in BDIX Tester

1. **Stale Catalog Entries:** Introduce explicit `VerificationState`, `lastReviewedAt`, and atomic versioned remote catalog updates.
2. **Conflation of Reachability, Latency, and Speed:** Strictly separate DNS/TCP/HTTP latency from explicit user-initiated byte-transfer download throughput (Mbps).
3. **Protocol Ambiguity:** Classify strictly by URL scheme (`http://`, `https://`, `ftp://`, `ftps://`) rather than substring heuristics (e.g., `"ftp"` in a domain name).
4. **ISP Routing Nuance:** Label reachable endpoints accurately as _"Reachable from current network"_ rather than claiming universal _"BDIX Verified"_ status.
5. **Privacy & Ads:** Eliminate ad SDKs and device telemetry; keep favorites/history strictly local.

### Features to Implement (Phased Roadmap)

- Categorized server directory & search (Phase 3 shell → Phase 4 data layer)
- Quick Scan vs. Full Scan with explicit timeout/cancellation semantics (Phase 5–6)
- Dedicated opt-in server speed testing on known resources (Phase 7)
- HTTPS-only versioned remote catalog updates with conditional HTTP caching (Phase 8)
- Local favorites, custom endpoints, history, and SAF export (Phase 9)

### Features Intentionally Excluded

- In-app video/Live TV streaming or media downloading
- Embedded WebView browsing (use Android external browser intent instead)
- Port scanning, IP-range enumeration, directory/credential brute forcing
- Advertising SDKs, analytics trackers, and mandatory accounts

---

## 10. References

1. Google Play — _BDIX Tester : BD Movie servers_ (`com.shubhobrata.roy.bdixtester`): https://play.google.com/store/apps/details?id=com.shubhobrata.roy.bdixtester&hl=en (Checked 2026-11-18)
2. Public Developer Profile reference: https://bd.linkedin.com/in/shubhobrataroy (Provided in brief; unverified via public search on 2026-11-18)
3. Public GitHub Profile reference: https://github.com/shubhobrataroy (Provided in brief; unverified via public search on 2026-11-18)
