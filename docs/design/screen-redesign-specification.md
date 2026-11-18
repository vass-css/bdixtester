# BDIX Tester — Screen-by-Screen Specification (Evidence-based Audit, 2026-11-18)

> **Current source of truth:** নিচের “বর্তমান যাচাইকৃত specification” ও finding ID-গুলো। এই ফাইলের আগের Phase 1 draft-টি নিচে historical হিসাবে রাখা হয়েছে; সেখানে থাকা unverified proposal-গুলো বর্তমান implementation বা অনুমোদিত কাজ নয়।

## বর্তমান যাচাইকৃত specification

### Product boundary (অপরিবর্তনীয়)

- Top-level navigation পাঁচটি: Home, Servers, Saved, History, Settings।
- Full-screen Server Details Page, Hamburger Menu, Home-এর বড় Quick/Full toggle, পুরোনো Wi-Fi/IP summary card ফিরিয়ে আনা হবে না।
- Server card tap ও URL footer tap external browser launch করে; এটি `NavigationUiTest`-এ verified।
- এই audit recommendation-only; Kotlin, test, dependency বা architecture পরিবর্তন করা হয়নি।

### Home — `feature/dashboard/DashboardScreen.kt`

- Brand/network pill, current working/total ring, View Servers এবং Start/Stop scan action।
- Home-এ explicit scan-mode toggle অনুপস্থিত—এটি approved decision; default mode Settings থেকে বদলায়।
- Active scan-এ percent, working count ও currently testing label; reduced motion-এ infinite scan motion বন্ধ।
- Wi-Fi unavailable হলে warning flow।
- **Issue:** detailed mode/scope/progress screen `ScanScreen.kt` আছে, কিন্তু NavHost-এ route নেই (`BDX-01`)। Completion notification নেই (`BDX-02`)।
- **Spec:** পাঁচ-tab nav না বাড়িয়ে Home/Servers থেকে reachable secondary scan surface; idle visual calm রাখুন। Screenshot/hierarchy unverified।

### Servers — `feature/servers/ServersScreen.kt`

- Status tabs: Working, Not Tested, Not Working; category chips FTP, Movies, TV, OTT; expandable search; sync/add actions।
- OTT region filters; server cards URL/footer tap-এ external browser।
- Search index source attribution/ISP text নেয়, কিন্তু placeholder-এ ISP নেই (`BDX-10`)।
- ViewModel-এ latency/name/category/protocol sort আছে, visible sort control নেই (`BDX-03`)।
- “Not Working” bucket-এ Unsupported/Cancelled-ও পড়ে (`BDX-05`)।
- **Spec:** sort action discoverable করা; valid latency-first; no result reason স্পষ্ট করা; নতুন All tab বা details destination যুক্ত নয়।

### Scan — `feature/scan/ScanScreen.kt` (বর্তমানে route নয়)

- Quick/Full segmented control; scan scope/category; unique URL/catalog/probe metrics; progress ring; active/queued counts; Start/Stop/export।
- Scan screen code ও ViewModel আছে; app navigation থেকে screen খোলা যায় না, তাই reachable UI feature হিসেবে ঘোষণা করা যাবে না (`BDX-01`)।
- Scope/status labels enum থেকে English আসতে পারে; localized mapping যাচাই প্রয়োজন (`BDX-04`)।
- **Spec:** existing secondary task flow-তে যুক্ত করার প্রস্তাব; নতুন top-level tab নয়।

### Saved — `feature/favorites/FavoritesScreen.kt`

- Search, export, ServerCard, pin/unpin ও unbookmark; pinned items আগে sort হয়।
- Empty state “details screen/page”-এ bookmark করতে বলে, যা আর নেই (`BDX-11`)।
- **Spec:** copy বর্তমান card-only action-এ সীমাবদ্ধ করুন। Pinned section/grid/Undo redesign এখনও user-tested নয়; roadmap-এ প্রয়োজন validation।

### History — `feature/favorites/FavoritesScreen.kt`

- Search, status/protocol chips, export; single row delete, clear-all confirmation, URL direct browser।
- `historyItems.isEmpty()` original empty ও filtered empty-কে এক করে (`BDX-06`); Saved/History একই query state share করে (`BDX-07`); date `Locale.US` (`BDX-04`); single-delete Undo নেই (`BDX-12`)।
- **Spec:** no-history/no-match আলাদা, locale-aware date, predictable query state, single delete undo।

### Settings — `feature/settings/SettingsScreen.kt`

- Appearance/language/reduced-motion, theme presets, default scan mode, test engine, remote catalog, data retention/delete ও privacy।
- Manual catalog sync Settings quick action এবং Remote Catalog section-এ উভয় আছে; Servers-এ contextual sync-ও আছে (`BDX-08`)।
- “New Server Add Request” local custom server dialog খোলে (`BDX-09`)।
- **Spec:** wording action-এর সঙ্গে মেলানো; duplicate Settings sync consolidate; বর্তমান theme/motion controls অক্ষুণ্ণ।

## Shared design & accessibility contract

- 4 curated palettes × light/dark; `ThemeSystemContrastAndMigrationTest`-এর নির্বাচিত token pairs pass। “সব rendered screen WCAG compliant” বলা যাবে না।
- Noto Sans Bengali, Plus Jakarta Sans, JetBrains Mono metadata verified; Bengali line height 1.14/letter spacing 0।
- 48dp+ chip/button target patterns ও adaptive 600dp rail / 700dp grid source-এ আছে; large font/TalkBack/device validation বাকি।
- Direct browser launch, status icon+label, URL LTR, reduced-motion branch বজায়।
- Progress live region TalkBack-এ অতিরিক্ত announcement দেয় কি না Hypothesis (`BDX-13`)।

## Historical Phase 1 draft (পর্যালোচিত; বর্তমান recommendation নয়)

নীচের পুরোনো draft-টি audit-এর আগে লেখা হয়েছিল। “P0 contrast fix”, category `All`, already-existing touch-target/motion changes, dual-pane/diagnostic sheet/long-press copy, pin sections, history date grouping, adaptive 2-column Saved grid, Home scan toggle/caption—এসব বর্তমান validated requirement নয়। সংশ্লিষ্ট বর্তমান সত্য/সুপারিশ উপরের sections-এ আছে; নিচের draft কেবল provenance-এর জন্য রাখা।

Every screen preserves 100% of existing business logic, ViewModels, Room entities, and direct External Browser launch. Zero separate Server Metadata / Details screen is re-introduced.

---

## 1. Home Screen (`feature/dashboard/DashboardScreen.kt`)

### Preserved Elements

- `HomeBrandTopBar` with brand badge + live IP pill (`home_top_ip_pill`).
- `HeroTesterRing` (`home_tester_ring`) with `workingCount / totalCount`, live `%` during scan, and `VIEW` button (`home_view_servers_button`).
- `PrimaryTestActionButton` (`home_start_test_button` / `home_stop_test_button`) + Wi-Fi warning dialog.
- Permanently absent: `home_menu_button`, `home_scan_mode_selector`, `home_network_summary_card`.

### Specification Changes

1. **Calm-Idle / Energetic-Active Ring & Top Bar (P1):**
   - **Idle:** Static precision outer gradient ring + crisp 36-tick telemetry ring + soft static radial backdrop. **Zero** continuous sonar pulses, **zero** orbital spin, **zero** top-bar icon spin, **zero** CTA shimmer loop.
   - **Active Scan (`isScanning || isStarting`):** If `!LocalReducedMotion.current`, run orbital sweep, luminous comet head, expanding sonar waves, and gentle top-bar radar rotation. If `LocalReducedMotion.current`, render deterministic animated progress arc without continuous spin/sonar loops.
   - **Completion:** Animations settle cleanly to idle state.
2. **WCAG AA CTA Gradient & Text (P0):**
   - Two-tone theme CTA gradients (`semantic.ctaGradientStart → semantic.ctaGradientEnd`) with `semantic.ctaContent` across all 4 approved themes (`Midnight Indigo`, `Graphite & Amber`, `Deep Emerald`, `Warm Premium`), every gradient stop verified ≥ `4.5:1`.
   - Light/Dark Stop CTA (`semantic.stopGradientStart → semantic.stopGradientEnd`) with `semantic.stopContent` (`≥ 4.5:1`).
3. **Subtle Scan Mode & Post-Scan Feedback Caption (P3):**
   - Compact non-interactive status caption below the ring or CTA sourced strictly from `DashboardUiState`:
     - Idle (never tested): `Quick Scan · 369 Servers` / `Full Scan · 369 Servers` (localized).
     - Completed (`!isScanning && testedCount > 0`): `Scan Complete · 41 Working` (+ `· Avg 12 ms` only when `averageReachableLatencyMs != null`).
4. **Responsive Layout (P1):**
   - Compact (`< 600dp`): full-height vertical portrait canvas (`isCompactHeight < 500dp` scrollable fallback).
   - Medium / Expanded (`>= 600dp`): balanced 2-column split paired with `NavigationRail`.

---

## 2. Servers & Bangladesh OTT Screen (`feature/servers/ServersScreen.kt`, `ServersViewModel.kt`)

### Preserved Elements

- 3 Status Tabs (`Working`, `Not Tested`, `Not Working`) with live tabular counts (`servers_tab_*`).
- Category filter bar (`All`, `OTT`, `TV`, `FTP`, `Movie`), search toggle, sync button, add custom server dialog.
- 26 verified Bangladesh & Global OTT platforms (`BdOttPlatforms.kt`) with region filter chips.
- Direct External Browser launch on every card & URL footer tap.

### Specification Changes

1. **Touch Targets (P1):** Raise `ott_region_*` `FilterChip` minimum height from `40.dp` to `48.dp`.
2. **Status & Latency Contrast (P0):** Inherit theme-verified `semantic.reachable`, `semantic.restricted`, and `semantic.unreachable` tokens (all ≥ `4.5:1` across all 8 configurations); use `MonoBadgeStyle` for numeric latency chips (`12 ms`) and `labelSmall` (body font) for Bengali/English text badges (`বাংলাদেশ`, `খুলুন`, category labels).
3. **Fastest Ping & A–Z Sorting (P2):**
   - Wire existing `ServersViewModel.updateSortOrder(...)` to a compact `>= 48.dp` sort action/menu in BDIX mode (`Default`, `Fastest Ping`, `A–Z`).
   - Harden `ServerSortOrder.LATENCY` so `null` or `< 0L` latencies always sort after valid numeric pings (`primaryLatencyMs?.takeIf { it >= 0L } ?: Long.MAX_VALUE`).
4. **Shared Card Footer & Optional Productivity Actions (P2–P3):**
   - Extract `RichCardUrlFooter` to shared `ui/components/RichServerCardComponents.kt`.
   - Support long-press on `RichCardUrlFooter` (or card copy button) to copy URL to clipboard + Snackbar confirmation.
   - Optional `ModalBottomSheet` diagnostics preview (only for servers with real `ServerTestResult != null`, triggered via explicit long-press/overflow—never blocking regular tap-to-browser).
   - Add `TopAppBarDefaults.enterAlwaysScrollBehavior()` with `nestedScroll` so the list gains vertical space while scrolling without breaking sticky status/category usability.

---

## 3. Saved Servers Screen (`feature/favorites/FavoritesScreen.kt`, `ui/components/ServerCard.kt`)

### Preserved Elements

- Search, Pin/Unpin, Unbookmark, Export (JSON/CSV/TXT), Direct External Browser launch.
- Existing `testTag`s (`favorites_screen`, `saved_search_input`, `favorites_empty_state`, `server_card_*`, `pin_toggle_*`, `favorite_toggle_*`, `visit_website_button_*`).

### Specification Changes

1. **Unified 20dp Elevated Rich Card (P2):**
   - Upgrade `ServerCard` to `20.dp` corner radius, `2.dp` elevation + `1.dp` border, `12.dp` category squircle badge, unified status/latency chips, and shared `RichCardUrlFooter` (`Open ↗` pill).
2. **Sectioned Layout & Smart Search Visibility (P2):**
   - Split `favoriteItems` into `Pinned` and `All Saved` sections using `BdixSectionHeader` (render section headers **only** when pinned items exist).
   - Hide `saved_search_input` when total favorites are empty and `searchQuery` is blank, eliminating dead vertical space above the empty state.
3. **Undo Snackbar (P2):**
   - Removing a favorite or unpinning triggers a Snackbar with `Undo` that calls `favoritesViewModel.restoreFavorite(snapshot)` to restore exact DB state (including `isPinned`, `displayOrder`, `addedAtEpochMs`).
4. **Adaptive Grid (P2):**
   - Use 2-column `LazyVerticalGrid` when `maxWidth >= 700.dp` (matching `ServersScreen`).

---

## 4. Testing History Screen (`feature/favorites/FavoritesScreen.kt`, `FavoritesViewModel.kt`)

### Preserved Elements

- Search, Status & Protocol `FilterChip`s, `Clear History` confirmation dialog, Export, Single Delete, Direct External Browser launch.
- Existing `testTag`s (`history_screen`, `history_search_input`, `history_clear_all_button`, `history_empty_state`, `history_item_*`, `history_delete_*`, `history_url_link_*`).

### Specification Changes

1. **Unified 20dp Elevated Rich Card (P2):**
   - Upgrade `HistoryRowCard` to `20.dp` Elevated Rich Card with protocol/category squircle icon badge, `StatusBadge`, tabular metrics, and shared `RichCardUrlFooter`.
2. **Real Date Grouping (P2):**
   - Group `historyItems` by real `entry.testedAtEpochMs` relative to local device midnight: `Today`, `Yesterday`, `Earlier` (localized in EN/BN). Never fabricate timestamps if missing.
3. **Undo Snackbar on Single Delete (P2):**
   - Deleting a single history record caches the `ServerTestResult` snapshot and shows an `Undo` Snackbar; confirming Undo re-inserts the record via `catalogRepository.restoreHistoryEntry(entry)`.
4. **Smart Empty/Filter States (P2):**
   - Differentiate "No history recorded yet" vs "No history matches current filters" (with `Clear filters` action).

---

## 5. Settings Screen (`feature/settings/SettingsScreen.kt`, `SettingsComponents.kt`, `SettingsViewModel.kt`)

### Preserved Elements

- App Profile & Live IP Header Card (`settings_app_header_card`), Theme & Language modal pickers, Default Scan Mode picker, Max Concurrency slider, Speed-Test Cap picker, Manual Network Profile Label, Remote Signed Catalog Sync settings, History Retention & Destructive Bulk Actions with confirmation dialogs, Privacy Policy modal, Facebook Community link, Share App chooser, Developer row, Version/Build footer.

### Specification Changes

1. **Color & Contrast Fixes (P0):**
   - App Profile Header icon badge uses `semantic.ctaGradientStart` → `semantic.ctaGradientEnd` + `semantic.ctaContent` (`≥ 4.5:1`).
   - Live IP text uses `semantic.ipAccent` (≥ `4.5:1` on `surfaceContainer` across all 8 configurations).
   - Version Footer `APPLICATION_ID` uses full-opacity `onSurfaceVariant` (≥ `4.5:1` across all 8 configurations).
2. **Consolidate Duplicate Sync Action & Rich Sync Telemetry (P2):**
   - Deduplicate `Update Server List` vs `Sync Catalog Now`: clarify Quick Actions vs Remote Signed Catalog Sync so there are no confusing duplicate sync buttons.
   - Display formatted `Last Successful Sync` timestamp from `sync.lastSuccessfulSyncAtEpochMs` when available, plus clear inline error surface + `Retry Sync` label when `lastSyncStatus` indicates failure.
   - Gate sync icon rotation inside `if (uiState.isSyncing && !LocalReducedMotion.current)`.
3. **Reduce Animations Preference (P1–P2):**
   - Add `SettingsSwitchRow` for `Reduce Animations` (`settings_reduce_animations_switch`) inside `Appearance & Language` card, persisted via `SettingsDataStoreRepository`.

---

## 6. Root Navigation (`navigation/BdixNavRoutes.kt`, `navigation/BdixTesterAppRoot.kt`)

1. **Filled vs Outlined Navigation Icons (P1):** Distinct selected/unselected icons for all 5 destinations.
2. **600dp Adaptive Breakpoint (P1):** Switch from `NavigationBar` to `NavigationRail` at `maxWidth >= 600.dp` (Medium & Expanded Window Size Classes).
3. **Reduced-Motion Tab Transitions (P1):** Switch `NavHost` enter/exit transitions to `EnterTransition.None` / `ExitTransition.None` (or subtle fade) when `LocalReducedMotion.current == true`, and disable bouncy nav-icon scale pop when reduced motion is active.
4. **Tap Active Tab to Scroll to Top (P3):** Re-tapping the currently selected tab emits an incremented scroll-to-top counter consumed cleanly by `Servers`, `Saved`, `History`, and `Settings` lazy lists without resetting tab back-stack state.
