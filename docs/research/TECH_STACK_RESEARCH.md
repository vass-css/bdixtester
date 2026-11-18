# Technology Stack Research — BDIX Tester

**Research Date:** 2026-11-18  
**Target Platform:** Native Android (Min SDK 24, Target/Compile SDK 36)

---

## 1. Core Language & Build System

| Layer            | Selected Technology                    | Verified Repo Version                       | Rationale & Official Reference                                                                                                                                   |
| ---------------- | -------------------------------------- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Language         | Kotlin                                 | `2.4.21` (via project `libs.versions.toml`) | Official primary language for modern Android; coroutines and null-safety prevent runtime NPEs on nullable network metrics. https://kotlinlang.org/docs/home.html |
| Build System     | Gradle Kotlin DSL + Version Catalog    | AGP `9.1.1`                                 | Existing project baseline; keeps dependencies centralized in `gradle/libs.versions.toml`.                                                                        |
| Module Structure | Single Activity + Single `:app` module | N/A                                         | Minimizes build overhead and cognitive complexity for a focused network utility.                                                                                 |

---

## 2. UI, Design System & Navigation

| Component       | Selected Technology                                                  | Rationale                                                                                                                                                                        |
| --------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| UI Toolkit      | Jetpack Compose + Material 3 (`composeBom 2024.09.00`)               | Declarative UI with `LazyColumn` efficiency for large server catalogs and native M3 light/dark/dynamic themes. https://developer.android.com/develop/ui/compose                  |
| Navigation      | `androidx.navigation:navigation-compose` (`2.8.9`)                   | Single-activity backstack management across Home, Servers, Scan, Favorites/History, Server Details, and Settings. https://developer.android.com/develop/ui/compose/navigation    |
| Adaptive Layout | `BoxWithConstraints` + `NavigationBar` / `NavigationRail`            | Adapts cleanly across compact phones, foldables, and tablets without hardcoded screen-dimension math. https://developer.android.com/docs/quality-guidelines/adaptive-app-quality |
| Typography      | Bundled `.ttf` Google Fonts (`Plus Jakarta Sans` + `JetBrains Mono`) | Local resource fonts ensure deterministic offline rendering and crisp alignment for URLs, IPv4 addresses, and latency numbers.                                                   |

---

## 3. State Management, Concurrency & Dependency Injection

- **MVVM + Unidirectional Data Flow:** `ViewModel` exposes immutable UI state via `StateFlow` collected with `collectAsStateWithLifecycle()`. https://developer.android.com/topic/architecture/ui-layer
- **Coroutines & Flow:** Structured concurrency (`viewModelScope`, bounded dispatchers/semaphores in Phase 5–6) ensures clean scan cancellation and socket closure. https://kotlinlang.org/docs/coroutines-overview.html
- **Constructor Injection + Lightweight `AppContainer`:** Avoids Hilt/Dagger code-generation and APK size overhead while remaining fully testable with fake repositories.

---

## 4. Planned Future Phase Stack (Phases 4–9 — Not Enabled in Phase 3)

| Future Capability                       | Recommended Stack                                                           | Target Phase      | Official Reference                                                                |
| --------------------------------------- | --------------------------------------------------------------------------- | ----------------- | --------------------------------------------------------------------------------- |
| Structured Catalog, Favorites & History | Room (`2.7.0` with KSP)                                                     | Phase 4 / Phase 9 | https://developer.android.com/training/data-storage/room                          |
| User Settings                           | Jetpack DataStore Preferences (`1.1.7`)                                     | Phase 4 / Phase 9 | https://developer.android.com/topic/libraries/architecture/datastore              |
| HTTP/HTTPS Probing & Speed Test         | OkHttp (`4.10.0`) with bounded timeouts & streaming bodies                  | Phase 5 / Phase 7 | https://developer.android.com/develop/connectivity/network-ops/connecting         |
| Remote Catalog JSON Parsing             | Moshi / `kotlinx.serialization` over HTTPS-only                             | Phase 8           | https://developer.android.com/privacy-and-security/security-config                |
| Periodic Catalog Sync                   | WorkManager (constraints: unmetered/connected)                              | Phase 8           | https://developer.android.com/develop/background-work/background-tasks/persistent |
| Export                                  | Android Storage Access Framework (`ActivityResultContracts.CreateDocument`) | Phase 9           | Zero storage permission compliance per Play policy                                |

---

## 5. Testing Stack

- **Unit Tests:** JUnit 4 + `kotlinx-coroutines-test` for URL parsing, protocol classification, duplicate auditing, and ViewModel state transitions.
- **UI & Android Resource Tests:** Robolectric (`4.16.1`) + `androidx.compose.ui:ui-test-junit4` for fast JVM Compose navigation and accessibility verification without requiring an emulator.

---

## 6. Dependencies Intentionally Excluded in Phases 1–3

- **Firebase / Cloud Sync / Ads:** Excluded per strict Phase 1–3 scope and privacy-first specification.
- **Hilt / Dagger:** Unnecessary for a single-module lightweight utility.
- **WebView / Media3 / ExoPlayer:** Excluded because external browser intents handle visiting endpoints and media streaming is out of scope.
- **Broad Storage / Location Permissions:** Excluded to comply with Google Play least-privilege policies.
