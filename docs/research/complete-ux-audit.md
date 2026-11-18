# BDIX Tester — সম্পূর্ণ UX ও Product Experience Audit

**Audit date:** 2026-11-18<br>
**Evidence basis:** Repository source, current tests, existing docs, official web research এবং public competitor imagery।
**সীমা:** 15টি app screenshot working tree-তে অনুপস্থিত; physical device, TalkBack, real ISP scan ও user study করা হয়নি। তাই visual/runtime দাবিগুলো যেখানে প্রযোজ্য `Unverified`।

## Phase 0–14 সম্পন্নতার রেকর্ড

| Phase | কাজ | ফল |
|---|---|---|
| 0 | Repo/Git/docs/source inventory | সম্পন্ন; baseline-এ কেবল আগে থেকে থাকা 15টি screenshot deletion |
| 1 | Official UX/accessibility/platform research | সম্পন্ন; উৎস `world-class-ui-ux-research.md` |
| 2 | Candidate users ও task journey | সম্পন্ন; actual users/persona interview করা হয়নি |
| 3 | Nielsen 10 heuristic review | সম্পন্ন; নিচে finding IDs |
| 4 | Source-based visual review | সম্পন্ন; screenshot-dependent findings Unverified |
| 5 | Navigation ও IA | সম্পন্ন; পাঁচটি top-level destination verified |
| 6 | Interaction/state audit | সম্পন্ন; scan, server, saved, history, settings source review |
| 7 | Design-system audit | সম্পন্ন; চার preset × light/dark, semantic roles |
| 8 | Accessibility review | source/test audit সম্পন্ন; TalkBack/device audit Unverified |
| 9 | বাংলা typography/localization | bundled font metadata ও resource/source review সম্পন্ন |
| 10 | Adaptive layout | 600dp rail ও 700dp grid source verified; fold/resize device test নেই |
| 11 | Motion/performance UX | reduced-motion implementation verified; profiling Unverified |
| 12 | Public competitor benchmark | Fing, Ookla Speedtest, X-plore official pages/images পর্যালোচনা; apps hands-on test নয় |
| 13 | Alternative concepts ও design direction | `final-design-direction.md` |
| 14 | Priority, acceptance, consistency | এই report, roadmap ও cross-report ID মিলিয়ে final check |

## Product ও actual screen inventory

| Surface | Source / উদ্দেশ্য | Verified UI ও actions | State/সীমা |
|---|---|---|---|
| Home | `feature/dashboard/DashboardScreen.kt` | Brand/network pill, working/total ring, Start/Stop, View Servers; Wi-Fi warning; scan completion haptic | Active scan-এ progress%; background summary notification নেই। Home scan-mode toggle ইচ্ছাকৃতভাবে অনুপস্থিত |
| Servers | `feature/servers/ServersScreen.kt` | Working/Not Tested/Not Working tabs, FTP/Movies/TV/OTT category chips, expandable search, sync/add, URL/card direct external launch | OTT region filters; empty catalog/filter state আছে; sort UI নেই |
| Scan surface | `feature/scan/ScanScreen.kt` | Quick/Full, scope/category, progress, active/queued metrics, stop/export | Composable আছে, কিন্তু root `NavHost`-এ route নেই; product-accessible screen হিসেবে Unverified/অপ্রাপ্য |
| Saved | `feature/favorites/FavoritesScreen.kt` | Search/export, pin/unpin, remove, ServerCard direct launch | Pinned আগে sort হয়; পৃথক section নেই; empty text এখনও “details screen” বলে |
| History | একই `FavoritesScreen.kt` | Search/status/protocol filter, export, delete one, clear-all confirmation, direct URL launch | Filtered-empty ও no-history state একই; date `Locale.US`; single delete-এ Undo নেই |
| Settings | `feature/settings/SettingsScreen.kt` | Theme/language/reduce motion, default scan mode, concurrency/speed cap, network label, remote sync, retention/data deletion/privacy | Settings-এ update/sync action duplicate; “Add Server Request” local custom endpoint dialog খোলে |
| Shared/navigation | `BdixTesterAppRoot.kt`, `ui/components/*` | 5 destinations; 600dp rail; theme-aware status/card components | Scan top-level destination নয়; scan screen route-ও নেই |

### User-task journey summary

| Journey | Current path | Friction / finding |
|---|---|---|
| Start scan → progress → result | Home CTA → ring; Server CTA Home-এ ফিরিয়ে scan শুরু করে | Detailed scope/progress surface route-এ নেই (`BDX-01`); status notification নেই (`BDX-02`) |
| Find fastest working endpoint | Servers → Working → optional search → card | Sort state আছে, visible sort affordance নেই (`BDX-03`) |
| Find ISP endpoints | Servers → search | ISP attribution search index-এ আছে; placeholder ISP বলে না, dedicated discovery নেই (`BDX-10`) |
| Save/pin/open | Server bookmark → Saved → pin / direct browser | Details page নেই—এটি অনুমোদিত সিদ্ধান্ত; empty text সেই পুরোনো page-এ পাঠায় (`BDX-11`) |
| Review history | History → search/filter/export/delete | no-match বিভ্রান্তি, shared query, US date, immediate delete (`BDX-04`, `06`, `07`, `12`) |

## Nielsen Norman Group — 10 heuristic assessment

| Heuristic | Evidence-based assessment |
|---|---|
| 1. System status দৃশ্যমান | Home ring progress ও currently-testing label দেয়; background notification gap `BDX-02`; live-region rate TalkBack-এ hypothesis `BDX-13` |
| 2. Real-world language | Reachable/Restricted/Unreachable distinction ভালো; “Not Working” aggregate-এ Unsupported/Cancelled ঢোকে (`BDX-05`) |
| 3. User control/freedom | Scan stop আছে; clear-all confirm আছে; single history delete-এ Undo নেই (`BDX-12`) |
| 4. Consistency/standards | Material 3/theme tokens সমন্বিত; status, protocol, category enum English হওয়ায় Bangla UI অসম্পূর্ণ (`BDX-04`) |
| 5. Error prevention | Direct external browser behavior test-verified; destructive history row-এ recovery cue নেই; “server request” label local action-এর সঙ্গে অসঙ্গত (`BDX-09`, `12`) |
| 6. Recognition vs recall | Category/status tabs ও visible counts ভালো; sort/ISP capability দৃশ্যমান নয় (`BDX-03`, `10`) |
| 7. Flexibility/efficiency | Quick/Full default Settings থেকে বদলানো যায়; scoped scan controls থাকা ScanScreen ব্যবহারকারীর কাছে নেই (`BDX-01`) |
| 8. Aesthetic/minimalist design | Source-এ Home-এর radar/glow layer মূল scan action-কে support করে; actual visual balance screenshot ছাড়া Unverified |
| 9. Error recovery | sync status text আছে; filtered history-তে no-match state নেই (`BDX-06`); row delete Undo নেই (`BDX-12`) |
| 10. Help/documentation | Saved empty text non-existent details page বলে (`BDX-11`); repo `TESTING_STRATEGY.md` Scan top-level nav বলে, যা current root-এর সঙ্গে মেলে না (`BDX-14`) |

## Finding Register

Priority: P0 critical blocker, P1 high product impact, P2 medium usability/consistency, P3 polish/validation. এই audit-এ কোনো P0 প্রমাণিত হয়নি।

### BDX-01 — Scan controls/progress surface ব্যবহারকারীর কাছে অপ্রাপ্য
- **Priority / Severity:** P1 / High. **Screen:** Home, scan flow. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `ScanScreen.kt:67`-এ mode, scope, category, metrics ও export আছে; `BdixTesterAppRoot.kt:234–330`-এর NavHost-এ Home, Servers, Favorites, History, Settings আছে, `SCAN` composable নেই। `BdixNavRoutes.kt:21`-এ constant থাকলেও route wiring নেই।
- **Impact:** All/selected category/favorites/retry scope নির্বাচন ও detailed progress/metrics ব্যবহার করা যায় না; default scan mode কেবল Settings থেকে বদলানো যায়।
- **Recommendation / Benefit:** Scan-কে secondary task destination হিসেবে Home/Servers থেকে খোলা যায়—পাঁচ-tab IA অক্ষুণ্ণ রেখে। ব্যবহারকারী scope ও progress চিনতে পারবে।
- **Effort / dependency:** Medium; route/navigation + existing ScanViewModel integration। **Acceptance:** CTA থেকে screen reachable; Quick/Full, scope, progress, stop/export task end-to-end চলে; নতুন top-level tab নেই।

### BDX-02 — Background scan/notification capability source-এ নেই
- **Priority / Severity:** P1 / High. **Screen:** Scan/background. **Classification:** source-verified absence; process lifecycle runtime effect Unverified. **Confidence:** High.
- **Evidence:** `ScanCoordinator.kt:86` app-level `CoroutineScope(SupervisorJob() + ioDispatcher)` ব্যবহার করে; `AndroidManifest.xml:3–5` কেবল network permissions, scan service/notification declaration নেই। `CatalogSyncWorker.kt` periodic catalog sync করে, scan নয়।
- **Impact:** App background/process reclaimed হলে scan চালু থাকা ও completion alert পাওয়ার নিশ্চয়তা নেই; user “background scan” ধরে অপেক্ষা করতে পারে।
- **Recommendation / Benefit:** Platform policy মেনে user-visible scan lifecycle, ongoing progress/cancel notification, completion permission-aware notification এবং in-app result fallback নির্ধারণ। Persistent deferrable catalog sync আলাদা রাখুন।
- **Effort / dependency:** High; Android OS/API policy ও notification permission review। **Acceptance:** background, screen-off, notification denied, cancel, process stop/reopen scenario-তে ফলাফল/সীমা স্পষ্ট; false completion claim নেই।

### BDX-03 — Fastest Ping sorting implemented state, visible UI নয়
- **Priority / Severity:** P1 / High. **Screen:** Servers. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `ServersViewModel.kt:37,225–232,342`-এ SOURCE/NAME/CATEGORY/PROTOCOL/LATENCY order এবং update API আছে; `strings.xml:290–293`-এ sort labels আছে; `ServersScreen.kt:132`-এর screen contract/call-site-এ sort callback/control নেই।
- **Impact:** Fastest server user task সরাসরি করা যায় না; code capability থাকলেও affordance discoverable নয়।
- **Recommendation / Benefit:** BDIX list toolbar/menu-তে 48dp+ sort action; “Fastest Ping” কেবল valid non-negative tested latency sort করবে; unknown/unmeasured শেষে। User দ্রুত উপযোগী server পাবে।
- **Effort / dependency:** Low–Medium; ViewModel callback/UI localization। **Acceptance:** sort selected state TalkBack-এ বলা হয়; null/negative latency শেষে; sort category/status/search filter-এর সঙ্গে কাজ করে।

### BDX-04 — বাংলা locale-এ status, metadata, semantics ও date অসম্পূর্ণ
- **Priority / Severity:** P1 / High. **Screen:** Servers, History, Saved, Settings. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `Enums.kt`-এর `TestStatus`, `ServerProtocol`, `ServerCategory`, `SyncStatus` English `displayLabel/displayName`; `StatusBadge.kt:105,119` English visible/semantic text; History filters `FavoritesScreen.kt:267,285` enum labels ব্যবহার করে; `:489` date-এ `Locale.US`।
- **Impact:** Bengali language select করলেও diagnostic state, filters, protocol/category metadata, TalkBack status, history date English/US format-এ থাকতে পারে।
- **Recommendation / Benefit:** Localized string resources দিয়ে visible + accessibility labels গঠন; localized date/time; URL/IP/units-এ LTR/locale formatting সচেতনভাবে বজায় রাখুন।
- **Effort / dependency:** Medium; resource completeness + enum-to-string mapping। **Acceptance:** Bengali UI-তে critical state/action ইংরেজি নয় (প্রযুক্তিগত term exception); automated text assertion এবং Bengali TalkBack sample pass।

### BDX-05 — “Not Working” tab অপ্রাপ্য, বাতিল ও failed ফল একত্র করে
- **Priority / Severity:** P2 / Medium. **Screen:** Servers. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `ServersViewModel.kt:459–470`: REACHABLE ছাড়া RESTRICTED/TIMED_OUT/UNSUPPORTED/CANCELLED default `NOT_WORKING` bucket-এ পড়ে; card badge পৃথক state দেখাতে পারে।
- **Impact:** “Not Working” মানে server dead মনে হতে পারে, যদিও unsupported বা user-cancelled probe কখনও failed reachability প্রমাণ করে না।
- **Recommendation / Benefit:** tab-কে “Needs attention/সমস্যা” হিসেবে ব্যাখ্যা বা split-count/state explainer দিন; পৃথক card result বজায় রাখুন।
- **Effort / dependency:** Low; product wording decision। **Acceptance:** representative Unsupported/Cancelled/Restricted item থেকে user সঠিক অর্থ ব্যাখ্যা করতে পারে।

### BDX-06 — Filtered history empty-কে “কোনো history নেই” বলা হয়
- **Priority / Severity:** P2 / Medium. **Screen:** History. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `FavoritesViewModel.kt:33,115` `hasAnyHistory` রাখে; `FavoritesScreen.kt:317` শুধুমাত্র `historyItems.isEmpty()` দিয়ে original-empty ও filter-no-match একই EmptyState দেখায়।
- **Impact:** Filter/search mismatch-কে data loss বা scan না-হওয়া মনে হতে পারে।
- **Recommendation / Benefit:** `hasAnyHistory`-এ empty-history vs no-results আলাদা; no-results-এ Clear filters action।
- **Effort / dependency:** Low. **Acceptance:** seeded history + nonmatching search/filter-এ “কোনো মিল নেই”; সত্যিই history না থাকলে onboarding copy।

### BDX-07 — Saved/History search query state ভাগ করে
- **Priority / Severity:** P2 / Medium. **Screen:** Saved ↔ History. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** Root উভয় screen-এ একই `FavoritesViewModel::updateSearchQuery` দেয় (`BdixTesterAppRoot.kt:272–292`); `FavoritesViewModel.kt:29–31,126` এক `_searchQuery`।
- **Impact:** এক tab-এর পুরোনো query অন্য tab-এ অপ্রত্যাশিতভাবে ফল hide করতে পারে।
- **Recommendation / Benefit:** per-screen query রাখুন অথবা destination change-এ দৃশ্যমান, ব্যবহারকারী-নিয়ন্ত্রিত reset; current query label/clear affordance রাখুন।
- **Effort / dependency:** Low–Medium। **Acceptance:** Saved-এ query দিয়ে History খুললে history অকারণে filtered নয়; ফিরে এলে আচরণ predictable।

### BDX-08 — Catalog sync-এর manual entry point একাধিক
- **Priority / Severity:** P2 / Medium. **Screen:** Servers, Settings. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** Servers toolbar sync (`ServersScreen.kt`); Settings “Update Server List” row `SettingsScreen.kt:263`; remote catalog section-এ “Sync Now” button `:554`; root callbacks catalog sync-এ map।
- **Impact:** duplicate action/status affordance cognitive load ও sync state ambiguity বাড়ায়।
- **Recommendation / Benefit:** Settings-এ catalog configuration + canonical manual sync; Servers-এ contextual quick sync কেবল একই wording/status/disabled feedback দিয়ে রাখুন।
- **Effort / dependency:** Low. **Acceptance:** duplicate Settings action নেই; প্রতিটি action-এ একই started/success/error feedback।

### BDX-09 — “New Server Add Request” আসলে local custom server dialog
- **Priority / Severity:** P2 / Medium. **Screen:** Settings. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `SettingsScreen.kt:271` row `onAddServerRequestClick`; `NavigationUiTest`-এ test ওই row click করে `CustomServerDialog` খোলে; Bengali/English subtitle বলে custom endpoint যোগ।
- **Impact:** “Request” দেখে user catalog maintainers-কে submission পাঠানো হবে ভাবতে পারে; বাস্তবে local endpoint তৈরি।
- **Recommendation / Benefit:** “Add custom server” / “কাস্টম সার্ভার যোগ করুন” label; local-only behavior আগে জানানো।
- **Effort / dependency:** Low. **Acceptance:** label/action/confirmation local storage behavior-এর সঙ্গে মেলে।

### BDX-10 — ISP search সম্ভব, কিন্তু discoverability কম
- **Priority / Severity:** P2 / Medium. **Screen:** Servers. **Classification:** Verified behavior + Design Judgment. **Confidence:** High.
- **Evidence:** `ServersViewModel.kt:418–433` search text-এ `sourceAttribution` যোগ; bundled catalog-এ ISP name attribution আছে; `strings.xml:64` placeholder কেবল name/host/URL বলে।
- **Impact:** ISP name দিয়ে search কাজ করতে পারে, কিন্তু user ISP association খুঁজতে জানে না; dedicated ISP filter নেই।
- **Recommendation / Benefit:** Placeholder/help text-এ ISP/source অন্তর্ভুক্ত; ISP discovery frequency user test-এ বেশি হলে visible ISP filter ভাবুন, এখনই নতুন navigation নয়।
- **Effort / dependency:** Low; dedicated filter decision-এর আগে research। **Acceptance:** test participant ISP text দিয়ে server খুঁজে পায় এবং search scope বুঝতে পারে।

### BDX-11 — Saved empty state non-existent Details page উল্লেখ করে
- **Priority / Severity:** P2 / Medium. **Screen:** Saved. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `strings.xml` ও `values-bn/strings.xml:117`-এ card বা “detail screen/page”-এ bookmark বলা; `NavigationUiTest` direct browser launch test `server_details_screen` absent নিশ্চিত করে।
- **Impact:** empty state পুরোনো/অসম্ভব interaction-এ নির্দেশ দেয়।
- **Recommendation / Benefit:** copy-তে শুধু server card-এর bookmark এবং Servers tab বলা।
- **Effort / dependency:** Low. **Acceptance:** English/Bengali text সব বর্তমান affordance-এর সঙ্গে মেলে।

### BDX-12 — Single history delete-এ Undo নেই
- **Priority / Severity:** P2 / Medium. **Screen:** History. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `HistoryRowCard`-এ delete icon সরাসরি callback (`FavoritesScreen.kt:413–461`); ViewModel `deleteHistoryEntry` call করে; restore API আছে কিন্তু UI action/snackbar নেই (`FavoritesViewModel.kt:159–167`). Clear-all আলাদা confirm পায়।
- **Impact:** ভুল tap-এ একক historical evidence হারাতে পারে।
- **Recommendation / Benefit:** short Undo snackbar-এ deleted immutable result restore; restore failure হলে explicit error। Clear-all confirmation বজায় রাখুন।
- **Effort / dependency:** Medium; snackbar host/state + restore operation। **Acceptance:** Undo-এর আগে item অপসারিত, Undo-তে একই timestamp/status/latency-সহ ফিরে আসে।

### BDX-13 — Scan live region অতিরিক্ত TalkBack announcement দিতে পারে
- **Priority / Severity:** P2 / Medium. **Screen:** Home scan. **Classification:** Hypothesis, source evidence verified. **Confidence:** Medium.
- **Evidence:** `DashboardScreen.kt` ring description animated percent-এর সঙ্গে `LiveRegionMode.Polite`; currently-testing text-ও live region। TalkBack runtime test হয়নি।
- **Impact:** বহু endpoint-এর progress update accessibility speech বারবার interrupt/queue করতে পারে—এটি device test ছাড়া নিশ্চিত নয়।
- **Recommendation / Benefit:** TalkBack-এ পর্যবেক্ষণ; প্রয়োজন হলে meaningful milestone/per-endpoint update announce করুন, frame-level progress নয়।
- **Effort / dependency:** Low–Medium; device/TalkBack test। **Acceptance:** scan শুরু/গুরুত্বপূর্ণ milestone/সমাপ্তি বলা হয়; speech queue অনিয়ন্ত্রিতভাবে বাড়ে না।

### BDX-14 — বিদ্যমান docs-এর navigation/UI claim current source-এর সঙ্গে drift
- **Priority / Severity:** P2 / Medium. **Screen:** Documentation/QA. **Classification:** Verified Finding. **Confidence:** High.
- **Evidence:** `docs/testing/TESTING_STRATEGY.md:14` Scan-কে navigation destination বলে; root NavHost-এ পাঁচটি route, Scan নেই। Existing `screen-redesign-specification.md` পুরোনো category/UI/contrast recommendation-কে বর্তমান অবস্থা ধরে লেখা ছিল; এই audit-এ top section current evidence করে, পুরোনো draft-কে historical label দেওয়া হয়েছে।
- **Impact:** QA/design future work stale assumption-এ scope inflate বা regress করতে পারে।
- **Recommendation / Benefit:** next docs maintenance pass-এ testing strategy, old UI audit ও contrast doc-কে current source/test result-এ reconcile; history মুছবেন না।
- **Effort / dependency:** Low. **Acceptance:** docs-এ পাঁচটি nav, absent Scan route, current theme/test status একরূপ।

## যাচাইকৃত শক্তি

- পাঁচটি top-level destination এবং 600dp NavigationRail branch source/test verified।
- URL/card tap external browser-এ যায় এবং Details route নেই—`NavigationUiTest`-এ verified; এই আচরণ অক্ষুণ্ণ রাখতে হবে।
- 4টি theme preset × light/dark-এর finite token-pair contrast test pass।
- Noto Sans Bengali, Plus Jakarta Sans, JetBrains Mono font binary metadata `Regular` হিসেবে `fc-scan`-এ verified; Bengali typography config letter spacing `0`, line-height scale `1.14`।
- Reduce-animation preference ও active-only radar motion source-এ আছে; `PulsingStatusDot` reduced motion মানে।
- category chip 48dp, Settings row 64dp, icon button-এ minimum interactive component size source-এ আছে।
- `NavigationUiTest`, `LocalizationAccessibilityUiTest`, `ThemeSystemContrastAndMigrationTest` targeted run pass।

## Unverified ও Not Applicable

- **Unverified:** actual app screenshot visual hierarchy/contrast/spacing, real TalkBack speech, font 1.3x/2.0x, 320dp split-screen, foldable, physical one-hand reach, low-end GPU/battery, actual ISP/background lifecycle.
- **Not Applicable:** login/account journey; current local-first product-এ account screen নেই।
- **No observed user data:** frequency/severity user research দিয়ে প্রমাণিত নয়; `BDX-10`, `BDX-13` এবং visual judgments prototype/user validation চায়।

## Final consistency audit — concurrent source changes

Audit চলাকালে 21:02-এ `DashboardViewModel.kt`, `ServersViewModel.kt`, `BdixTesterAppRoot.kt`-এ transient working-tree change দেখা গিয়েছিল। এই audit ওই Kotlin files edit, stage বা revert করেনি। User-এর নির্দেশ অনুযায়ী সেগুলো untouched রাখা হয়; পরবর্তী final check-এ তিনটি source diff আর ছিল না। কেন পরিবর্তনগুলো অদৃশ্য হয়েছে, তা নিশ্চিত করা যায়নি।

Final check-এ HEAD `04d40d73a6decdbc1d277c564370ca8b06702a2e` অপরিবর্তিত; app source-এ staged/unstaged diff নেই। বর্তমান NavHost-এ পাঁচটি route, Scan route নেই; `ServersViewModel`-এ sort API আছে কিন্তু screen-এ visible sort control নেই—`BDX-01` ও `BDX-03` current source-এ পুনরায় যাচাই হয়েছে। এই audit কোনো commit করেনি।

Working tree-তে 15টি pre-existing screenshot deletion অক্ষত; report docs ছাড়া অন্য কোনো tracked source/test/dependency/architecture file পরিবর্তিত হয়নি। Final focused Gradle test run (theme contrast/migration, navigation, Bengali UI) current source-এ pass করেছে; `git diff --check` pass।
