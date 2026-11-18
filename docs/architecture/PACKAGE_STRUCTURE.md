# Package Structure — BDIX Tester

**Base Package:** `com.jubayerhossain.bdixtester`  
**Module:** Single `:app` Gradle module  
**Date:** 2026-11-18

---

```text
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   └── java/com/jubayerhossain/bdixtester/
    │       ├── BdixtesterApplication.kt        # Lightweight AppContainer initialization
    │       ├── MainActivity.kt                 # Single Activity + enableEdgeToEdge()
    │       │
    │       ├── core/
    │       │   ├── di/                         # AppContainer constructor-DI wiring
    │       │   ├── model/                      # Protocol, Category, Status, Verification enums & audit summary
    │       │   ├── network/                    # Read-only ConnectivityObserver (Phase 3 network status)
    │       │   └── validation/                 # UrlParser (scheme, host, port, RFC 1918 IPv4 detection)
    │       │
    │       ├── data/
    │       │   ├── catalog/
    │       │   │   ├── FtpLinks.kt             # Canonical verbatim user-provided source list
    │       │   │   └── BuiltInCatalog.kt       # Idempotent parser & audit calculator
    │       │   └── repository/                 # InMemoryCatalogRepository (Phase 3)
    │       │
    │       ├── domain/
    │       │   ├── model/                      # ServerEndpoint, ServerTestResult, ScanProgressState
    │       │   └── repository/                 # CatalogRepository contract
    │       │
    │       ├── feature/
    │       │   ├── dashboard/                  # DashboardScreen & DashboardViewModel
    │       │   ├── servers/                    # ServersScreen, ServersViewModel & ServerDetailsScreen
    │       │   ├── scan/                       # ScanScreen & ScanViewModel (Phase 3 shell)
    │       │   ├── favorites/                  # FavoritesScreen (Favorites & History Phase 3 shell)
    │       │   └── settings/                   # SettingsScreen & SettingsViewModel
    │       │
    │       ├── navigation/                     # BdixNavRoutes & adaptive BdixTesterAppRoot
    │       │
    │       └── ui/
    │           ├── components/                 # ServerCard, StatusBadge, EmptyStateView, MetricTile
    │           └── theme/                      # Color, Type (Plus Jakarta Sans + JetBrains Mono), Shape, Theme
    │
    ├── test/java/com/jubayerhossain/bdixtester/
    │   ├── UrlParserTest.kt
    │   ├── BuiltInCatalogAuditTest.kt
    │   ├── ExampleUnitTest.kt
    │   ├── ExampleRobolectricTest.kt
    │   └── NavigationUiTest.kt
    │
    └── androidTest/java/com/jubayerhossain/bdixtester/
        └── ExampleInstrumentedTest.kt
```

> **Future Packages (Phases 4–9):** `data/local/` (Room DAOs), `data/remote/` (HTTPS catalog client), `data/preferences/` (DataStore), and `network/{http,ftp,timeout,classification}/` will be populated only when Phases 4–9 are authorized. Empty placeholder packages are intentionally avoided.
