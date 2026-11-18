# Architecture — BDIX Tester

**Phase:** 2 (Architecture & Technical Design)  
**Date:** 2026-11-18  
**Application ID:** `com.jubayerhossain.bdixtester`

---

## 1. Architectural Style

**BDIX Tester** uses a **Single-Activity, Single-Module (`:app`) MVVM architecture** with unidirectional data flow (UDF):

- **UI Layer (Jetpack Compose + Material 3):** Stateless/declarative composables observe immutable `StateFlow` UI states via `collectAsStateWithLifecycle()` and emit sealed/method-based user events to `ViewModel`s.
- **Domain Layer:** Pure Kotlin domain models (`ServerEndpoint`, `ServerTestResult`, `ScanProgressState`), protocol/category enums, and repository/tester contracts isolated from Android framework APIs.
- **Data Layer:** Separates the immutable built-in `FtpLinks` source catalog, parsed `BuiltInCatalog`, future Room persistence (Phase 4/9), future remote JSON sync (Phase 8), and DataStore preferences.
- **Network Layer (Interface-defined in Phase 2; implemented in Phases 5–7):** Protocol-specific testers (`HttpEndpointTester`, `FtpEndpointTester`), bounded coroutine concurrency scheduler, and result classifier.

Reference: https://developer.android.com/topic/architecture

---

## 2. Dependency Injection Strategy

Lightweight **constructor injection** wired through a single `AppContainer` owned by `BdixtesterApplication`. Heavyweight DI code-generation frameworks (Hilt/Dagger) are intentionally omitted so build times stay fast and dependencies remain explicit and easy to swap with test fakes.

---

## 3. Core Models & Separation of Concerns

1. **Configuration vs. Mutable State:** `ServerEndpoint` holds static catalog metadata (`id`, `name`, `originalUrl`, `host`, `explicitPort`, `protocol`, `category`, `isFeatured`, `sourceType`, `verificationState`, `lastReviewedAtEpochMs`, `speedTestUrl`, `isPrivateIp`). It never embeds live reachability or speed numbers.
2. **Nullable Diagnostic Metrics:** `ServerTestResult` uses nullable fields (`dnsTimeMs: Long?`, `tcpConnectTimeMs: Long?`, `httpResponseLatencyMs: Long?`, `ftpConnectLatencyMs: Long?`, `httpStatusCode: Int?`, `downloadMbps: Double?`, `transferredBytes: Long?`). Unavailable measurements remain `null` — never coerced to `0`.

---

## 4. Persistence Strategy (Phase 4+ Target)

- **Settings (`DataStore Preferences`):** Small key-value preferences (theme mode, timeouts, concurrency limit, Wi-Fi-only update toggle, speed-test data cap, history retention days).
- **Catalog, Favorites, Custom Endpoints & History (`Room`):** Justified in Phase 4/9 for indexed filtering/sorting over hundreds of endpoints, relational separation of built-in vs. remote vs. custom entries, and retention-bounded testing history.
- **Phase 3 Boundary:** In Phase 3, `InMemoryCatalogRepository` serves `BuiltInCatalog` directly with zero fabricated test results or premature Room/network wiring.

---

## 5. Lifecycle & Navigation

- `MainActivity` calls `enableEdgeToEdge()` and hosts `BdixTesterAppRoot`.
- `NavHost` manages top-level routes (`Home`, `Servers`, `Scan`, `Saved`, `Settings`) and detail route (`ServerDetails/{serverId}`).
- Adaptive navigation switches between bottom `NavigationBar` (Compact width `< 600.dp`) and `NavigationRail` (Medium/Expanded width `>= 600.dp`).
