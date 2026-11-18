# BDIX Tester — Complete UI/UX Implementation Audit (Phase 1 — Superseded Color Context)

> **Historical Notice:** This Phase 1 audit predates the **Complete Color System Replacement**. The previous Cyan-Sky palette (`LightSemanticColors`, `DarkSemanticColors`, `NavyDeep`, etc.) documented below has been **permanently deleted** from the project. See `docs/design/color-system-specification.md`, `docs/design/light-dark-theme-matrix.md`, and `docs/design/old-theme-removal-report.md` for the active four-theme specification (`Midnight Indigo`, `Graphite & Amber`, `Deep Emerald`, `Warm Premium`).

**Audit Date:** 2026-11-18  
**Project:** `com.jubayerhossain.bdixtester` (Kotlin + Jetpack Compose + Material Design 3 + Room + DataStore + WorkManager)  
**Design Direction:** _Precision Diagnostic Minimalism — Material Design 3_
**Phase 1 Scope:** Read-only repository audit & evidence verification. **Zero source code modified.**

---

## 1. Repository & Environment State

| Area                          | Status       | Evidence / Details                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| ----------------------------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Git Repository State          | **Verified** | Workspace root is **not a Git repository** (`git status` returned `fatal: not a git repository`). No branch, commit log, or uncommitted git diff exists in this container. All source files are tracked directly on disk; no destructive operations performed.                                                                                                                                                                                             |
| Build & SDK Toolchain         | **Verified** | `app/build.gradle.kts` & `gradle/libs.versions.toml`: AGP `9.1.1`, Kotlin `2.4.21`, `compileSdk = 36` (minor 1), `targetSdk = 36`, `minSdk = 24`, Compose BOM `2024.09.00` (Material 3 `1.3.0`), `activity-compose 1.10.1`, `navigation-compose 2.8.9`, `room 2.7.0`, `datastore-preferences 1.1.7`, `work-runtime-ktx 2.10.0`, `okhttp 4.10.0`, `robolectric 4.16.1`.                                                                                     |
| Adaptive Nav Suite Dependency | **Verified** | `androidx.compose.material3:material3-adaptive-navigation-suite` is **not** declared in `libs.versions.toml` or `app/build.gradle.kts`. Existing `BdixTesterAppRoot.kt` uses `BoxWithConstraints` + M3 `NavigationBar` / `NavigationRail` natively with zero extra dependency overhead.                                                                                                                                                                    |
| CI Pipeline                   | **Verified** | `.github/workflows/android-ci.yml` runs `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:compileReleaseKotlin` on JDK 17 + Gradle `9.3.1`.                                                                                                                                                                                                                                                               |
| Core Catalog & OTT Assets     | **Verified** | `FtpLinks.kt` (369 verbatim built-in endpoints), `BdOttPlatforms.kt` (26 HTTPS platforms: 17 Bangladesh + 9 Global in BD), `RoomCatalogRepository.kt` (Room v1 + integrity verification), `ScanCoordinator.kt` / `HttpEndpointTester.kt` / `FtpControlTester.kt` / `SpeedTestEngine.kt`, `RemoteCatalogSyncManager.kt` (ECDSA detached sig + anti-rollback + Wi-Fi enforcement), `ExternalBrowserHelper.kt` (direct `ACTION_VIEW` + `CATEGORY_BROWSABLE`). |
| Existing Automated Tests      | **Verified** | 12 JVM / Robolectric test suites under `app/src/test/java/com/jubayerhossain/bdixtester/` (including `BuiltInCatalogAuditTest`, `NavigationUiTest`, `LocalizationAccessibilityUiTest`, `RoomDatabaseMigrationTest`, `ScanCoordinatorTest`, `SpeedTestEngineTest`, `CatalogSignatureAndSyncTest`, `ExportAndIntentSafetyTest`).                                                                                                                             |

---

## 2. Classification Legend

- **Verified:** Directly confirmed via source inspection, binary/font header inspection, or mathematical calculation.
- **Partially Verified:** Partially confirmed in source code; runtime/device aspect remains to be tested on hardware.
- **Unverified:** Cannot be directly measured in headless JVM environment (e.g., real TalkBack speech output, GPU frame pacing on physical low-end Android devices).
- **Design Recommendation:** Evidence-based UX/UI improvement proposed for user approval.
- **Not Applicable:** Item from generic guidance not applicable to current architecture.

---

## 3. Detailed Issue-by-Issue Audit Matrix

### P0 — Critical Accessibility, Contrast, System Bar & Bengali Typography Issues

#### P0-1: Primary CTA Button Fails WCAG 2.2 AA Contrast (Both Dark & Light Themes + Stop State)

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/feature/dashboard/DashboardScreen.kt` (lines 534–628) & `app/src/main/java/com/jubayerhossain/bdixtester/ui/theme/Color.kt` (lines 81–109)
- **Class / Composable:** `PrimaryTestActionButton`, `LightSemanticColors`, `DarkSemanticColors`
- **Current Behavior:**
  - Uses `Color.White` text/icon hardcoded on `semantic.ringGradientStart/Mid/End` and hardcoded `Color(0xFFDC2626)` for Stop state.
  - **Mathematical WCAG 2.2 AA Check (Normal Text threshold ≥ 4.5:1):**
    - Dark Start CTA (`#FFFFFF` on `#22D3EE` → `#38BDF8` → `#3B82F6`): **1.81:1** (start — FAIL), **2.14:1** (mid — FAIL), **3.68:1** (end — FAIL normal text).
    - Dark Stop CTA (`#FFFFFF` on `#F87171` → `#DC2626`): **2.77:1** (start — FAIL), **4.83:1** (end — PASS).
    - Light Start CTA (`#FFFFFF` on `#06B6D4` → `#0284C7` → `#2563EB`): **2.43:1** (start — FAIL), **4.10:1** (mid — FAIL), **5.17:1** (end — PASS).
  - Also affects `SettingsAppProfileHeaderCard` icon badge (`SettingsScreen.kt:885–906`), which draws `onPrimary` (`#FFFFFF` in Light) over `ringGradientStart` (`#06B6D4`, **2.43:1** — fails 3.0:1 non-text UI contrast).
- **Proposed Change:**
  - Add dedicated semantic tokens to `BdixSemanticColors` (`ctaGradientStart`, `ctaGradientMid`, `ctaGradientEnd`, `ctaContent`, `stopGradientStart`, `stopGradientEnd`, `stopContent`) and remove all hardcoded `Color.White` / `Color(0xFFDC2626)` from `DashboardScreen.kt`.
  - **Light Start CTA:** `#0369A1 → #105BBC → #1D4ED8` with `#FFFFFF` text (**5.93:1 to 6.70:1 — PASS AA**).
  - **Dark Start CTA:** `#22D3EE → #38BDF8 → #3B82F6` with `#041E2D` (`NavyDeep`) text (**9.46:1 start, 7.98:1 mid, 4.65:1 end — PASS AA across entire gradient**).
  - **Light & Dark Stop CTA:** `#C81E1E → #991B1B` (Light) / `#B91C1C → #991B1B` (Dark) with `#FFFFFF` text (**5.74:1 to 8.31:1 — PASS AA**).
  - Use CTA gradient + `ctaContent` on `SettingsAppProfileHeaderCard` brand icon badge.
- **Acceptance Criteria:**
  - Every gradient stop (start, midpoint, end) achieves ≥ 4.5:1 contrast with CTA label/icon in both Light and Dark themes.
  - Zero hardcoded `Color.White` or raw hex literals remain in `PrimaryTestActionButton`.

#### P0-2: Light Theme Primary, Semantic Status Tokens & Settings Footer Fail WCAG 2.2 AA

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/ui/theme/Color.kt` (lines 12, 30, 81–94) & `app/src/main/java/com/jubayerhossain/bdixtester/feature/settings/SettingsScreen.kt` (lines 977, 1016)
- **Class / Composable:** `CyanPrimaryLight`, `ErrorLight`, `LightSemanticColors`, `SettingsAppProfileHeaderCard`, `SettingsVersionFooter`
- **Current Behavior:**
  - `CyanPrimaryLight` (`#0277B5`) on `LightSurfaceContainer` (`#EBF1F7`) = **4.28:1 (FAIL AA)** — affects Bottom Nav labels, Settings IP row, and section headers.
  - `LightSemanticColors.reachable` (`#059669`) on `#D1FAE5` = **3.32:1 (FAIL AA)** — affects `StatusBadge` and working latency chips.
  - `LightSemanticColors.restricted` (`#D97706`) on `#FEF3C7` = **2.86:1 (FAIL AA)** — affects Restricted/Timed Out badges.
  - `LightSemanticColors.unreachable` (`#DC2626`) on `#FEE2E2` = **3.95:1 (FAIL AA)** — affects Unreachable badges.
  - `SettingsVersionFooter` (`SettingsScreen.kt:1016`) uses `onSurfaceVariant.copy(alpha = 0.72f)` on `#F4F7FA` → composited `#778695` = **3.47:1 (FAIL AA)**.
- **Proposed Change:**
  - Update Light palette tokens: `CyanPrimaryLight = #03669C` (**5.45:1** on `#EBF1F7`, **5.26:1** on composited Nav Pill, **6.20:1** on `#FFFFFF`), `ErrorLight = #C81E1E`, `LightSemanticColors.reachable = #047857` (**4.84:1** on `#D1FAE5`), `restricted = #B45309` (**4.51:1** on `#FEF3C7`), `unreachable = #C81E1E` (**4.70:1** on `#FEE2E2`), plus semantic `ipAccent` (`#035E91` in Light = **6.12:1** on `#EBF1F7`; `#38BDF8` in Dark = **7.71:1** on `#12202E`).
  - Remove `alpha = 0.72f` from `SettingsVersionFooter` `APPLICATION_ID` text (`onSurfaceVariant` at full opacity = **6.68:1** Light, **8.61:1** Dark).
- **Acceptance Criteria:**
  - All Light and Dark semantic foreground/background pairs (including alpha-composited nav indicators and footer labels) achieve ≥ 4.5:1 WCAG 2.2 AA contrast.

#### P0-3: Dynamic System Bar Icon Contrast Desync on Manual Theme Switch

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/MainActivity.kt` (lines 18–41) & `app/src/main/AndroidManifest.xml` (lines 31–41)
- **Class / Composable:** `MainActivity`
- **Current Behavior:**
  - `enableEdgeToEdge()` is invoked once in `onCreate()` with default `SystemBarStyle.auto(...)`, which only tracks system night-mode configuration.
  - When a user selects `ThemePreference.LIGHT` while the OS is in Dark Mode (or vice versa), status bar icons and 3-button/gesture navigation bar icons remain stuck in the OS polarity, becoming invisible or low-contrast.
  - Also `MainActivity` in `AndroidManifest.xml` does not specify `android:windowSoftInputMode="adjustResize"`, which official Android Edge-to-Edge documentation recommends alongside `imePadding()` for forms/search fields.
- **Proposed Change:**
  - Compute resolved `useDarkTheme` inside `MainActivity.setContent` and apply a `DisposableEffect(useDarkTheme)` calling `enableEdgeToEdge(statusBarStyle = ..., navigationBarStyle = ...)` using `SystemBarStyle.dark(Color.TRANSPARENT)` when `useDarkTheme == true` and `SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)` when `useDarkTheme == false`.
  - Add `android:windowSoftInputMode="adjustResize"` to `MainActivity` in `AndroidManifest.xml`.
- **Acceptance Criteria:**
  - Switching Theme between `System`, `Light`, and `Dark` in Settings immediately updates status bar and navigation bar icon polarity without restarting the Activity.

#### P0-4: Bengali Typography Letter-Spacing, `labelSmall` Mono Fallback & Static Font Weight Bug

- **Classification:** **Verified** (Binary TTF header inspection + Kotlin source audit)
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/ui/theme/Type.kt` (lines 14–177) & `app/src/main/res/font/*.ttf`
- **Class / Composable:** `PlusJakartaSansFamily`, `NotoSansBengaliFamily`, `JetBrainsMonoFamily`, `buildBdixTypography`, `BdixTypographyBengali`
- **Current Behavior:**
  1. `buildBdixTypography` applies non-zero `letterSpacing` (`-0.5sp`, `-0.3sp`, `-0.15sp`, `0.1sp`, `0.2sp`) to `BdixTypographyBengali`. Non-zero tracking breaks Bengali _matra_ (মাত্রা) continuity and conjunct (_যুক্তবর্ণ_) shaping.
  2. `labelSmall` hardcodes `fontFamily = JetBrainsMonoFamily` (lines 165–172). `jetbrains_mono.ttf` contains no Bengali glyphs, so Bengali badges/chips (`বাংলাদেশ`, `খুলুন`, category chips) fall back to the raw system font and inherit `letterSpacing = 0.2.sp`.
  3. **Binary TTF Discovery:** Inspecting `app/src/main/res/font/{plus_jakarta_sans,noto_sans_bengali,jetbrains_mono}.ttf` reveals all three files are **static `usWeightClass = 400` Regular fonts** (`variable = False`, no `fvar` table). Declaring `FontWeight.Medium`, `SemiBold`, and `Bold` with `FontVariation.Settings` against a non-variable TTF is a **no-op** and prevents Android from applying synthetic bold—meaning every heading and label currently renders in 400 Regular weight!
- **Proposed Change:**
  1. Add `isBengali: Boolean` parameter to `buildBdixTypography(...)`: force `letterSpacing = 0.sp` across every style when `isBengali == true` and apply calibrated Bengali line heights (from Section 4.2 specification).
  2. Change `labelSmall` to use `fontFamily = bodyFontFamily` (removing `JetBrainsMonoFamily` and `tnum` from generic `labelSmall`). Introduce dedicated `MonoBadgeStyle` (`JetBrainsMonoFamily`, `11.sp`, `tnum`) exclusively for numeric/ASCII badges (latency `ms`, counts, ports).
  3. Replace the single-weight font declarations with real weight files (`400/500/600/700` via `font-util` if supported, keeping the exact same 3 font families) OR, if only 400 TTFs are present, bind only `FontWeight.Normal` so Android properly synthesizes weight hierarchy rather than silently flattening all weights to 400.
- **Acceptance Criteria:**
  - `BdixTypographyBengali` has `letterSpacing == 0.sp` on all 12 Material 3 styles.
  - `BdixTypographyBengali.labelSmall.fontFamily == NotoSansBengaliFamily`.
  - Visual weight differentiation between `Normal`, `SemiBold`, and `Bold` works properly in both English and Bengali.

---

### P1 — Core Experience, Reduced Motion, Adaptive Navigation & Touch Targets

#### P1-1: Continuous Idle Infinite Animations & Missing Reduced Motion Support

- **Classification:** **Verified** (Code audit) / **Unverified** (Real GPU frame pacing on low-end hardware)
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/feature/dashboard/DashboardScreen.kt` (lines 158–167, 455–464, 592–596, 691–759), `app/src/main/java/com/jubayerhossain/bdixtester/ui/components/BdixMotion.kt` (lines 85–125, 247–320), `app/src/main/java/com/jubayerhossain/bdixtester/feature/settings/SettingsScreen.kt` (lines 472–481)
- **Class / Composable:** `DashboardScreen`, `HomeBrandTopBar`, `HeroTesterRing`, `PrimaryTestActionButton`, `PulsingStatusDot`, `ShimmerPillOverlay`, `FloatingMotionBox`
- **Current Behavior:**
  - While completely **idle**, Home simultaneously runs **9 infinite animations** (`ambientAlpha`, `radarRotation`, `ShimmerPillOverlay`, `PulsingStatusDot`, `scanSpinAngle`, `orbitAngle`, `tickAngle`, `sonarWave1`, `sonarWave2`, `glowAlpha`).
  - `SettingsScreen` also instantiates `rememberInfiniteTransition("settings_sync_spin")` unconditionally even when `uiState.isSyncing == false`.
  - No check for Android's `Settings.Global.ANIMATOR_DURATION_SCALE` (readable via `Settings.Global.getFloat(contentResolver, ANIMATOR_DURATION_SCALE, 1f)` with zero extra permissions on all API levels 24–36, wrapped safely in `runCatching`) and no in-app `Reduce Animations` preference.
- **Proposed Change:**
  - Add `LocalReducedMotion` CompositionLocal + `rememberReducedMotionEnabled(userReduceAnimations: Boolean)` combining DataStore preference `reduceAnimations` and system `ANIMATOR_DURATION_SCALE == 0f` (with safe fallback `false`).
  - **Calm-Idle / Energetic-Active Home:**
    - When **idle**: Zero sonar waves, zero continuous orbital rotation, zero top-bar radar icon spin, zero CTA shimmer loop. Render a crisp, calm precision ring with subtle static radial depth.
    - When **scanning** (and `!reducedMotion`): Activate orbital sweep, comet head, sonar waves, and live progress indicator; stop cleanly upon scan completion.
    - When **scanning + reducedMotion**: Render clean deterministic progress arc without spinning/sonar loops.
  - Gate `SettingsScreen` sync spin transition inside `if (uiState.isSyncing && !reducedMotion)`.
  - Gate `FloatingMotionBox` and `PulsingStatusDot` pulse halo behind `!reducedMotion`.
- **Acceptance Criteria:**
  - In idle state (or with `Reduce Animations` enabled), zero unnecessary `rememberInfiniteTransition` loops run on Home, Empty States, or Settings.
  - Active scan retains clear real-time progress ring + live counter updates in both normal and reduced-motion modes.

#### P1-2: Identical Selected/Unselected Icons in Bottom Navigation & Rail

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/navigation/BdixNavRoutes.kt` (lines 23–65)
- **Class / Composable:** `TopLevelDestination`
- **Current Behavior:**
  - 4 of 5 tabs (`HOME`, `SERVERS`, `HISTORY`, `SETTINGS`) assign identical `Icons.Rounded.*` vectors to both `selectedIcon` and `unselectedIcon`. Only `SAVED` distinguishes `Bookmark` vs `BookmarkBorder`.
  - Violates WCAG 1.4.1 (Use of Color) guidance for distinguishing active state by shape/fill in addition to color/pill.
- **Proposed Change:**
  - Use paired Filled (`Icons.Rounded.*`) vs Outlined (`Icons.Outlined.*`) from already-bundled `androidx.compose.material:material-icons-extended`:
    - `HOME`: `Icons.Rounded.Home` / `Icons.Outlined.Home`
    - `SERVERS`: `Icons.Rounded.Dns` / `Icons.Outlined.Dns`
    - `SAVED`: `Icons.Rounded.Bookmark` / `Icons.Outlined.BookmarkBorder`
    - `HISTORY`: `Icons.Rounded.History` / `Icons.Outlined.History` (note: Material `History` stroke is visually close to Rounded; combine with distinct selected pill + bold label + `Icons.Outlined.Schedule` or `Icons.Outlined.History` — verified available in `material-icons-extended`)
    - `SETTINGS`: `Icons.Rounded.Settings` / `Icons.Outlined.Settings`
- **Acceptance Criteria:**
  - Every top-level destination switches cleanly between distinct selected and unselected icons without adding any new icon dependency.

#### P1-3: Adaptive Navigation Rail Threshold Misaligned with M3 Medium Window Size Class

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/navigation/BdixTesterAppRoot.kt` (line 179)
- **Class / Composable:** `BdixTesterAppRoot`
- **Current Behavior:**
  - `val useNavigationRail = maxWidth >= 840.dp`, whereas `DashboardScreen.kt:193` switches to 2-column landscape/tablet layout at `maxWidth >= 600.dp`.
  - Between `600dp` and `839dp` (foldables unfolded, landscape phones, small tablets), the app shows a stretched bottom bar alongside a 2-column dashboard.
- **Proposed Change:**
  - Change breakpoint to `val useNavigationRail = maxWidth >= 600.dp` (matching official Android Window Size Class `Medium >= 600.dp` and `DashboardScreen`).
  - Keep existing `BoxWithConstraints` + M3 `NavigationBar` / `NavigationRail` rather than adding `material3-adaptive-navigation-suite`, preserving `saveState = true`, `restoreState = true`, and zero extra dependencies.
- **Acceptance Criteria:**
  - `< 600dp`: renders `NavigationBar` (`main_bottom_navigation_bar`).
  - `>= 600dp` (`600–839dp` Medium and `>= 840dp` Expanded): renders `NavigationRail` (`main_navigation_rail`).

#### P1-4: OTT Region FilterChip Touch Target Below 48dp Minimum

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/feature/servers/ServersScreen.kt` (line 417)
- **Class / Composable:** `ServersScreen` (OTT region filter `LazyRow`)
- **Current Behavior:**
  - `ott_region_*` `FilterChip` sets `.heightIn(min = 40.dp)` instead of `48.dp`.
- **Proposed Change:**
  - Update to `.heightIn(min = 48.dp)` matching `servers_category_*` chips.
- **Acceptance Criteria:**
  - Every interactive chip on `ServersScreen` (BDIX categories & OTT regions) meets `>= 48.dp` height.

---

### P2 — Component Consistency, Productivity & Settings Clarity

#### P2-1: Fragmented Card Language Across Servers/OTT vs Saved vs History

- **Classification:** **Verified**
- **File Path:**
  - `app/src/main/java/com/jubayerhossain/bdixtester/feature/servers/ServersScreen.kt` (lines 638–760, 847–1104 — `OttPlatformCard`, `ServerListCard`, `RichCardUrlFooter` is `private`)
  - `app/src/main/java/com/jubayerhossain/bdixtester/ui/components/ServerCard.kt` (lines 65–340 — flat `16.dp` radius, no squircle icon badge, divider-only URL row without tinted `Open ↗` pill)
  - `app/src/main/java/com/jubayerhossain/bdixtester/feature/favorites/FavoritesScreen.kt` (lines 389–529 — `HistoryRowCard` flat `16.dp` Surface, no squircle badge, divider-only URL row)
- **Current Behavior:**
  - `Servers` & `OTT` use `20.dp` Elevated Rich Cards with `12.dp` squircle category icon badge and tinted `RichCardUrlFooter` (`Open ↗` pill), whereas `Saved` (`ServerCard`) and `History` (`HistoryRowCard`) still use the older `16.dp` flat layout.
- **Proposed Change:**
  - Promote `RichCardUrlFooter` and `CategoryIconBadge` into shared `ui/components/RichServerCardComponents.kt`.
  - Upgrade `ServerCard` (`Saved`) and `HistoryRowCard` (`History`) to unified `20.dp` Elevated Rich Cards (`2.dp` elevation + `1.dp` outlineVariant) with `12.dp` category squircle badge and shared `RichCardUrlFooter` (preserving all existing testTags: `server_card_*`, `visit_website_button_*`, `history_item_*`, `history_url_link_*`, `pin_toggle_*`, `favorite_toggle_*`, `history_delete_*`).
  - On `SavedServersScreen`: group into `Pinned` and `All Saved` sections (hiding headers when empty), and hide the search field when `favoriteItems.isEmpty() && searchQuery.isEmpty()`.
  - On `HistoryScreen`: group entries by real `testedAtEpochMs` (`Today`, `Yesterday`, `Earlier`) using device local calendar boundaries (zero fake dates).
- **Acceptance Criteria:**
  - All four URL-centric lists (`Servers`, `OTT`, `Saved`, `History`) share identical `20.dp` card geometry, squircle icon language, and `>= 48.dp` `RichCardUrlFooter`.

#### P2-2: Duplicate Catalog Sync Action in Settings & Missing `Reduce Animations` Toggle

- **Classification:** **Verified**
- **File Path:** `app/src/main/java/com/jubayerhossain/bdixtester/feature/settings/SettingsScreen.kt` (lines 239–262, 439–516), `AppSettings.kt`, `SettingsDataStoreRepository.kt`, `SettingsViewModel.kt`, `BdixTesterAppRoot.kt`
- **Current Behavior:**
  - `Update Server List` (`settings_update_server_list_row`, lines 240–252) calls `onUpdateServerListClick` (which calls `settingsViewModel.syncCatalogNow(remoteUrl)` via `triggerCatalogSyncFromToolbar`) — the **exact same operation** as `Sync Catalog Now` (`settings_sync_now_button`, lines 483–514) inside the Remote Signed Catalog Sync card!
  - `Sync` status box shows raw `Seq #0 · 0 remote` without displaying `lastSuccessfulSyncAtEpochMs` (even though `CatalogSyncState` already persists `lastSuccessfulSyncAtEpochMs`) and without an inline retry action when sync fails.
  - No `Reduce Animations` preference exists in `AppSettings` / `SettingsDataStoreRepository`.
- **Proposed Change:**
  - Consolidate/clarify catalog sync: keep `New Server Add Request` in Quick Actions (and if `settings_update_server_list_row` is preserved for test compatibility or merged, ensure zero redundant confusion — specifically: either unify sync into the `Remote Signed Catalog Sync` card with rich Last Sync timestamp + status badge + Retry/Sync button, OR make Quick Actions clearly house `Add Custom Server` + jump/trigger with live sync state; recommended: **keep `settings_update_server_list_row` testTag or update `NavigationUiTest` cleanly while consolidating duplicate sync into one authoritative Remote Catalog Sync card**, and show `Last successful sync` formatted timestamp from `sync.lastSuccessfulSyncAtEpochMs` + contextual Retry label when `lastSyncStatus` is an error state).
  - Add `reduceAnimations: Boolean = false` to `AppSettings` + `SettingsDataStoreRepository` + `SettingsViewModel` and expose a `SettingsSwitchRow` in `Appearance & Language`.
- **Acceptance Criteria:**
  - Single clear sync workflow with real `lastSuccessfulSyncAtEpochMs` and Retry feedback on error.
  - `Reduce Animations` toggle persists across app restarts in DataStore and immediately toggles `LocalReducedMotion`.

#### P2-3: Undo Snackbar on Single Delete / Unfavorite (Database + UI Restoration)

- **Classification:** **Verified** (Repository audit)
- **File Path:** `CatalogRepository.kt`, `RoomCatalogRepository.kt`, `InMemoryCatalogRepository.kt`, `FavoritesViewModel.kt`, `FavoritesScreen.kt`, `BdixTesterAppRoot.kt`
- **Current Behavior:**
  - `RoomCatalogRepository.deleteHistoryEntry(historyId)` deletes the row permanently from Room with no restore method.
  - `RoomCatalogRepository.toggleFavorite(serverId)` deletes the `FavoriteEntity` (including its `isPinned`, `displayOrder`, `addedAtEpochMs`, `note`) with no restore method.
- **Proposed Change:**
  - Add `suspend fun restoreFavorite(favorite: FavoriteInfo)` and `suspend fun restoreHistoryEntry(entry: ServerTestResult)` to `CatalogRepository` (implemented cleanly in both `RoomCatalogRepository` using existing `favoriteDao.upsertFavorite` and `historyDao.insertHistory` and in `InMemoryCatalogRepository`). Zero Room schema migration needed!
  - Wire Undo `Snackbar` via `BdixTesterAppRoot`'s shared `snackbarHostState` when removing a Saved server, unpinning, or deleting a single History item.
- **Acceptance Criteria:**
  - Tapping Undo restores the exact `FavoriteInfo` (including pin state & timestamp) or `ServerTestResult` row in Room and updates UI flows immediately.

#### P2-4: Fastest Ping & A–Z Sorting on Servers Screen

- **Classification:** **Verified**
- **File Path:** `ServersViewModel.kt` (lines 35–41, 152–162, 256–258), `ServersScreen.kt`
- **Current Behavior:**
  - `ServersViewModel` **already defines** `ServerSortOrder` (`SOURCE_ORDER`, `NAME_ASC`, `LATENCY`, etc.) and `updateSortOrder(...)`, but `ServersScreen` has **no UI control** wired to change sort order!
  - Furthermore, in `ServersViewModel.kt:158–160`, `ServerSortOrder.LATENCY` sorts by `results[it.id]?.primaryLatencyMs ?: Long.MAX_VALUE` — which safely puts null latencies last, and should also filter out non-positive/invalid negative latencies (`takeIf { ms -> ms >= 0L } ?: Long.MAX_VALUE`) with deterministic tie-breaker by name.
- **Proposed Change:**
  - Expose a compact, accessible `>= 48.dp` sort selector (`Default`, `Fastest Ping`, `A–Z`) on `ServersScreen` (for BDIX mode; `Fastest Ping` highlighted on Working tab) wired to `serversViewModel::updateSortOrder`.
  - Guard `ServerSortOrder.LATENCY` against null or `< 0L` latencies so unknown pings never sort ahead of valid numeric latencies.
- **Acceptance Criteria:**
  - Working servers sort accurately by ascending valid numeric ping (`ms`), placing unknown/null latencies at the end.

---

### P3 — Optional Polish & Productivity Enhancements

#### P3-1: Subtle Scan Mode Visibility & Post-Scan Feedback on Home

- **Classification:** **Design Recommendation**
- **File Path:** `DashboardScreen.kt` (lines 304–370), `DashboardViewModel.kt` (already provides `selectedScanMode`, `totalServers`, `testedCount`, `reachableCount`, `averageReachableLatencyMs`)
- **Current Behavior:**
  - Home hides the current scan mode completely (after Quick/Full toggle was removed), and offers no subtle post-scan completion caption (e.g., avg ping).
- **Proposed Change:**
  - Below `PrimaryTestActionButton` (or directly below the ring), render a single subtle, non-interactive caption line sourced from live state:
    - Idle before scan: `Quick Scan · 369 Servers` (or `Full Scan · 369 Servers`).
    - Completed scan (`!isScanning && testedCount > 0`): `Scan Complete · 41 Working · Avg 12 ms` (omitting `Avg` if `averageReachableLatencyMs == null` — zero mock values).
  - Do **not** restore `home_scan_mode_selector` or `home_network_summary_card` (both explicitly asserted absent in `NavigationUiTest`).

#### P3-2: Copy URL Action, Collapsible Top Bar, Tap-Active-Tab-to-Top & Optional Diagnostics Sheet

- **Classification:** **Design Recommendation**
- **File Paths:** `ServersScreen.kt`, `FavoritesScreen.kt`, `ServerCard.kt`, `BdixTesterAppRoot.kt`
- **Verified Data Availability for Optional Diagnostics Bottom Sheet:**
  - `ServerTestResult` (`ServerModels.kt:44–77`) **already captures real probe diagnostics**: `dnsTimeMs`, `tcpConnectTimeMs`, `tlsHandshakeTimeMs`, `httpResponseLatencyMs`, `ftpConnectLatencyMs`, `ftpGreetingCode`, `httpStatusCode`, `finalUrl`, `errorClassification`, `failureMessage`, `downloadMbps`, `networkProfileLabel`.
  - **Strict UX Rule Preserved:** Regular tap on any card or URL footer continues to launch the Android External Browser directly. Long-press (or explicit overflow icon on tested cards) can offer `Copy URL` (with Snackbar confirmation) and—only when real `ServerTestResult != null`—an optional non-navigating `ModalBottomSheet` displaying those real recorded metrics without re-creating any Server Details screen or making new network calls.
- **Scroll Polish:**
  - Use `TopAppBarDefaults.enterAlwaysScrollBehavior()` on `ServersScreen` (with `nestedScroll`), and support tapping the already-selected bottom tab to smoothly scroll the active list back to item `0` via hoisted scroll trigger state.

---

## 4. Items Marked `Not Applicable` or Explicitly Excluded

| Item                                                            | Classification                 | Reason                                                                                                                                                                                       |
| --------------------------------------------------------------- | ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Re-introducing Server Details / Metadata Screen                 | **Not Applicable (Forbidden)** | Permanently removed per user decision; asserted absent by `NavigationUiTest`.                                                                                                                |
| Adding `material3-adaptive-navigation-suite` dependency         | **Not Applicable**             | Current Compose BOM `2024.09.00` does not bundle it; existing `BoxWithConstraints` `NavigationBar` + `NavigationRail` achieves identical 600dp adaptive behavior with zero dependency bloat. |
| Restoring Home Quick/Full Segmented Toggle or Bottom Wi-Fi Card | **Not Applicable (Forbidden)** | Explicitly removed per user request and asserted absent by `NavigationUiTest` & `LocalizationAccessibilityUiTest`.                                                                           |
| Modifying `ScanScreen.kt` (unrouted legacy composable)          | **Not Applicable**             | `ScanScreen` is not mounted in `BdixTesterAppRoot` `NavHost` (retained in source per rule 2.1 — do not delete, do not unnecessarily modify).                                                 |
| Modifying `FtpLinks.kt` (369 endpoints) or Room Schema v1       | **Not Applicable (Forbidden)** | Core catalog and DB schema must remain strictly intact.                                                                                                                                      |
