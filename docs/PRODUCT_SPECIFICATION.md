# Product Specification — BDIX Tester

**Version:** 1.0 (Phase 1 Baseline)  
**Date:** 2026-11-18  
**Application Name:** BDIX Tester  
**Application ID / Namespace:** `com.jubayerhossain.bdixtester`  
**Developer:** Jubayer Hossain (`JubayerHossain`)

---

## 1. Product Overview & Audience

**BDIX Tester** is a lightweight, privacy-conscious native Android utility built in Kotlin and Jetpack Compose (Material 3) for Bangladesh broadband and mobile-internet users to discover, inspect, and test locally reachable BDIX-related servers.

### Core Product Principles

1. **No Account Required:** Fully usable without registration, login, or cloud tracking.
2. **Offline-Resilient Catalog:** Built-in `FtpLinks` catalog and cached remote catalog remain browsable offline.
3. **Transparent Diagnostics:** Strictly distinguish _Reachability_, _Response Latency (ms)_, and _Download Throughput (Mbps)_.
4. **ISP-Aware Accuracy:** Report working hosts as **"Reachable from current network"**, never claiming universal _"BDIX Verified"_ status without documented evidence.
5. **Low Data Consumption:** Lightweight `HEAD` / bounded `GET` checks and compact conditional HTTPS JSON catalog updates.
6. **Privacy-First Local Storage:** Favorites, custom endpoints, and testing history stay on-device with user-controlled clearing and SAF export.

---

## 2. Server Categories & Classification

Category (content/purpose) and Protocol (URL scheme) are strictly decoupled:

| Category      | Purpose                       | Notes                                                     |
| ------------- | ----------------------------- | --------------------------------------------------------- |
| `All`         | Complete catalog view         | Filter aggregate                                          |
| `FTP & Files` | File sharing and FTP portals  | Includes HTTP directory indexes and actual `ftp://` hosts |
| `Movies`      | Movie and video portals       | Media index websites                                      |
| `Live TV`     | Television / IPTV portals     | Link directories only (no in-app streaming)               |
| `Software`    | Software and ISO repositories | Local mirrors                                             |
| `Web`         | General local web portals     | —                                                         |
| `Other`       | Unclassified / pending review | Default when metadata is absent                           |

**Featured Attribute:** `isFeatured: Boolean` is a curated metadata flag independent of category or protocol.

---

## 3. Core Screens & Functional Requirements

### 3.1 Dashboard (Home)

Displays real, non-fabricated catalog and session telemetry:

- Current device connectivity state (`ConnectivityManager` read-only check; no SSID/location collection).
- Catalog statistics: Total Servers, Categories count, Last Catalog Update timestamp, Last Scan timestamp.
- Test outcome counters (shown as `0` / `Not tested` prior to scans): Tested, Reachable, Restricted/Error, Unreachable, Unsupported.
- Action cards for **Quick Scan** and **Full Scan** with clear behavioral explanations.
- Category shortcuts, Featured servers preview, and Recently tested servers (honest empty state before testing).

### 3.2 Categorized Server Directory

- Horizontal category filter chips (`All`, `FTP & Files`, `Movies`, `Live TV`, `Software`, `Web`, `Other`).
- `Featured only` filter toggle, Protocol filter (`HTTP`, `HTTPS`, `FTP`, `FTPS`, `UNSUPPORTED`), Status filter, and Sorting (Name, Category, Status, Latency).
- Real-time search across server name, original URL, and parsed host.
- High-performance `LazyColumn` rendering reusable `ServerCard` components.
- Clear empty states distinguishing _"Catalog source awaiting import"_ vs. _"No servers match active filters"_.

### 3.3 Reusable Server Card

Displays only verified/available fields:

- Display Name & Original URL / Hostname
- Category chip, Protocol badge, Optional `Featured` & `Private IPv4` indicators
- Distinct multi-modal `StatusBadge` (Icon + Text + Semantic Color — never color alone):
  `Not tested`, `Testing`, `Reachable`, `Restricted`, `Unreachable`, `Timed out`, `Unsupported`, `Cancelled`
- Response latency (`ms`) only when measured; Download speed (`Mbps`) only when separately measured
- Last tested timestamp (if tested) and `Visit Website` external browser action

### 3.4 Testing Controls & Modes (Engine Scheduled for Phases 5–6)

- **Quick Scan:** Low-overhead connectivity probe using shorter timeouts, no download-throughput transfer; explicitly warns users that slow endpoints may appear inconclusive.
- **Full Scan:** Tests the entire selected scope using standard configured timeouts, returning per-endpoint protocol metrics and a complete summary.
- **Controls:** Test single server, category, selected group, or full catalog; Stop/Cancel active scan (safely closing pending sockets); Retry failed endpoints.
- **Progress Interface:** Circular/linear progress indicator with live counts for Total, Completed, Remaining, Reachable, Unreachable, Restricted/Error, Unsupported, and Cancelled.

### 3.5 Server Details

- Full endpoint breakdown: Name, Original URL (verbatim), Parsed Host, Explicit Port (if specified), Category, Protocol, Private IPv4 notice (if RFC 1918), Catalog Source (`Built-in` / `Remote` / `Custom`), Verification State & Last Reviewed date (never fabricated).
- Latest connectivity observation and historical timeline (empty state in Phase 3).
- `Visit Website` action opening the device's default external browser (`ACTION_VIEW`), never an in-app `WebView`.

### 3.6 Favorites & Testing History (Persistence Scheduled for Phase 9)

- **Favorites:** Pin/bookmark servers independently of remote catalog updates.
- **History:** Local retention-bounded log storing Server ID, Original URL at test time, Timestamp, Protocol, Scan Mode, Result Classification, Measured Latency, Optional Download Throughput, Failure Reason, and optional user-entered Network Profile label. User can clear history at any time.

### 3.7 Export & Custom Servers (Scheduled for Phase 9)

- **Export:** CSV, JSON, and Plain Text via Android Storage Access Framework (`CreateDocument`) with zero broad storage permissions.
- **Custom Endpoints:** Add/edit/delete personal endpoints stored separately from official/built-in catalog so remote updates never overwrite user entries.

### 3.8 Settings

- **Appearance (Active in Phase 3):** System / Light / Dark theme selection.
- **Planned Settings Shells (Phases 5–9):** Connection/Read timeouts, Max Concurrency, Quick Scan timeout policy, Auto/Wi-Fi-only Catalog Updates, Max Speed-Test Data Cap, History Retention, Privacy Disclosure, and About (`BDIX Tester` by Jubayer Hossain).

---

## 4. Non-Functional & Policy Boundaries

- **Zero Prohibited Capabilities:** No port scanning, IP-range discovery, directory/credential brute forcing, hidden background scanning, or unauthorized bulk file downloading.
- **External Browser Only:** `Visit Website` delegates to Android `Intent.ACTION_VIEW`.
- **Phase Authorization Gate:** Phases 1–3 authorize specification, architecture, catalog preservation, and Compose UI shell only. Real network probing, Room persistence, remote sync, speed tests, and exports require Phase 4+ authorization.
