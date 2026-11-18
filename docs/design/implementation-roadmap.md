# BDIX Tester — Prioritized Implementation Roadmap (Phase 1 Plan)

> **Execution Rule:** No source code has been modified in Phase 1. Implementation will proceed strictly in priority order (**P0 → P1 → P2 → P3**) after user approval, with full build (`compile_applet`) and regression test (`gradle :app:testDebugUnitTest`) validation after each priority group.

---

## Group 1: P0 — Critical WCAG AA Contrast, Dynamic System Bars & Bengali Typography

| #    | Task                                                                                                                                                          | Target Files                                                                                            | Acceptance Criteria                                                                                                                       |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| P0.1 | Replace legacy palette with 4 curated Light/Dark themes (`Midnight Indigo`, `Graphite & Amber`, `Deep Emerald`, `Warm Premium`) + semantic CTA/Stop/IP colors | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                                                | All 8 Light & Dark semantic pairs ≥ 4.5:1 (`>= 3.0:1` UI/outline), two-tone theme CTA gradients + Stop gradients, dynamic color disabled. |
| P0.2 | Apply semantic CTA/Stop colors in Home & Profile Header + remove low-alpha footer text                                                                        | `feature/dashboard/DashboardScreen.kt`, `feature/settings/SettingsScreen.kt`                            | Zero hardcoded `Color.White`/hex literals in CTA; Settings footer AppId ≥ 4.5:1.                                                          |
| P0.3 | Dynamic `enableEdgeToEdge(statusBarStyle, navigationBarStyle)` + `adjustResize`                                                                               | `MainActivity.kt`, `AndroidManifest.xml`                                                                | Status & navigation bar icons immediately invert on manual Light/Dark theme switch.                                                       |
| P0.4 | Fix Bengali `letterSpacing = 0.sp`, `labelSmall` body font fallback, `MonoBadgeStyle`, and real font weight resolution                                        | `ui/theme/Type.kt`, `feature/servers/ServersScreen.kt`, `res/font/*` (if weights added via `font-util`) | `BdixTypographyBengali` has `0.sp` tracking everywhere, `NotoSansBengaliFamily` on `labelSmall`, and working weight hierarchy.            |

**Verification Gate 1:** `compile_applet` + `gradle :app:testDebugUnitTest` + contrast unit test assertions.

---

## Group 2: P1 — Calm-Idle Home, Reduced Motion, Adaptive Navigation & 48dp Touch Targets

| #    | Task                                                                                        | Target Files                                                                                                                                     | Acceptance Criteria                                                                                                |
| ---- | ------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------ |
| P1.1 | Add `LocalReducedMotion` + `rememberReducedMotionEnabled()` + DataStore `reduceAnimations`  | `data/preferences/AppSettings.kt`, `SettingsDataStoreRepository.kt`, `ui/components/BdixMotion.kt`, `core/di/AppContainer.kt`, `MainActivity.kt` | Respects both system `ANIMATOR_DURATION_SCALE == 0f` and in-app setting; persists across restarts.                 |
| P1.2 | Make Home Calm-Idle / Energetic-Active + gate Settings sync spin & empty-state bob          | `feature/dashboard/DashboardScreen.kt`, `feature/settings/SettingsScreen.kt`, `ui/components/EmptyStateView.kt`, `ui/components/BdixMotion.kt`   | Zero continuous decorative animations when idle or when Reduced Motion is active; active scan sweep works cleanly. |
| P1.3 | Pair Filled + Outlined icons across all 5 top-level destinations                            | `navigation/BdixNavRoutes.kt`                                                                                                                    | Distinct selected/unselected icon shapes for Home, Servers, Saved, History, Settings.                              |
| P1.4 | Align `NavigationRail` adaptive breakpoint to `600.dp` + reduced-motion NavHost transitions | `navigation/BdixTesterAppRoot.kt`                                                                                                                | `< 600dp` shows `NavigationBar`; `>= 600dp` shows `NavigationRail` with preserved tab state.                       |
| P1.5 | Raise OTT region `FilterChip` minimum height to `48.dp`                                     | `feature/servers/ServersScreen.kt`                                                                                                               | All OTT region chips ≥ 48dp touch target.                                                                          |

**Verification Gate 2:** `compile_applet` + `gradle :app:testDebugUnitTest` (add Robolectric tests for `w600dp` Rail vs `w411dp` BottomBar & 48dp touch targets).

---

## Group 3: P2 — Unified Rich Cards, Settings Consolidation, Undo Snackbar & Fastest Ping Sort

| #    | Task                                                                                                                                     | Target Files                                                                                                                                                                | Acceptance Criteria                                                                                        |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| P2.1 | Add unified shape/elevation tokens & shared `RichCardUrlFooter` + `BdixCategoryIconBadge`                                                | `ui/theme/Shape.kt`, `ui/components/RichServerCardComponents.kt`                                                                                                            | Single source of truth for 20dp Rich Cards & `Open ↗` footer.                                              |
| P2.2 | Upgrade `ServerCard` (Saved) & `HistoryRowCard` (History) + Pinned sections + Today/Yesterday/Earlier date grouping + smart empty search | `ui/components/ServerCard.kt`, `feature/favorites/FavoritesScreen.kt`, `feature/favorites/FavoritesViewModel.kt`, `res/values*/strings.xml`                                 | All 4 URL list screens share identical visual language; real timestamp grouping; existing testTags intact. |
| P2.3 | Implement Undo Snackbar restoration for Favorite removal & single History deletion                                                       | `domain/repository/CatalogRepository.kt`, `RoomCatalogRepository.kt`, `InMemoryCatalogRepository.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt`, `BdixTesterAppRoot.kt` | Undo restores exact DB row & UI state with zero Room schema migration.                                     |
| P2.4 | Wire Fastest Ping / A–Z / Default sort UI on `ServersScreen` + harden null/negative ping ordering                                        | `feature/servers/ServersViewModel.kt`, `feature/servers/ServersScreen.kt`, `res/values*/strings.xml`                                                                        | Valid numeric pings sort ascending; unknown/null pings always sort last.                                   |
| P2.5 | Consolidate Settings catalog sync clarity + display real `lastSuccessfulSyncAtEpochMs` + Retry state + `Reduce Animations` toggle        | `feature/settings/SettingsScreen.kt`, `SettingsViewModel.kt`, `res/values*/strings.xml`                                                                                     | Zero redundant sync confusion; clear last sync time & Retry UI; `Reduce Animations` switch functional.     |

**Verification Gate 3:** `compile_applet` + `gradle :app:testDebugUnitTest` (add unit/UI tests for ping sorting, Undo DB restore, and Settings Reduce Animations).

---

## Group 4: P3 — Optional Productivity & Polish

| #    | Task                                                                                           | Target Files                                                                                                                                        | Acceptance Criteria                                                                                                                                                |
| ---- | ---------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| P3.1 | Subtle Home Scan Mode / Post-Scan Summary caption (real state only)                            | `feature/dashboard/DashboardScreen.kt`, `res/values*/strings.xml`                                                                                   | Displays real mode (`Quick Scan · 369 Servers`) or real post-scan result (`Scan Complete · N Working · Avg X ms`), zero mock data, zero clutter.                   |
| P3.2 | Copy URL action (long-press footer / copy icon) with Snackbar feedback                         | `ui/components/RichServerCardComponents.kt`, `navigation/BdixTesterAppRoot.kt`, `res/values*/strings.xml`                                           | Copies exact URL to `ClipboardManager` and confirms via Snackbar.                                                                                                  |
| P3.3 | Collapsible TopAppBar (`enterAlwaysScrollBehavior`) on `ServersScreen` + Tap-Active-Tab-to-Top | `feature/servers/ServersScreen.kt`, `feature/favorites/FavoritesScreen.kt`, `feature/settings/SettingsScreen.kt`, `navigation/BdixTesterAppRoot.kt` | Smooth scroll behavior without resetting tab state on switch.                                                                                                      |
| P3.4 | Optional Diagnostics `ModalBottomSheet` for tested endpoints (long-press / overflow only)      | `feature/servers/ServersScreen.kt`, `ui/components/DiagnosticsBottomSheet.kt`                                                                       | Shows only real recorded `ServerTestResult` fields (HTTP status, DNS/TCP/TLS/HTTP latency, final URL, error); normal tap still launches external browser directly. |

**Final Verification Gate:**

- `compile_applet` (Debug build)
- `gradle :app:compileReleaseKotlin` (Release compile check)
- `gradle :app:testDebugUnitTest` (All existing 36 + new regression, accessibility, Bengali font-scale, and adaptive width tests)
- `lint_applet` (Final lint verification with explanation of any warnings)
- Update Phase 1 docs + deliver final Bengali Implementation Summary Report.
