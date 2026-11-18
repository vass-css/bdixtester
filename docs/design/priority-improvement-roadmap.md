# BDIX Tester — Priority Improvement Roadmap

**তারিখ:** 2026-11-18
**গুরুত্ব:** P0 Critical / P1 High / P2 Medium / P3 polish বা validation। এই audit-এ P0 প্রমাণিত হয়নি।

## P1 — Core workflow ও ভাষাগত access

| ID | কাজ | Impact / Effort | Dependency | Acceptance criteria |
|---|---|---|---|---|
| BDX-01 | বিদ্যমান `ScanScreen`-কে Home/Servers থেকে secondary route-এ accessible করা; 5-tab nav অপরিবর্তিত | High / Medium | Navigation + ScanViewModel | Quick/Full, scope/category, progress, stop/export end-to-end; scan tab যোগ নয় |
| BDX-02 | Background scan lifecycle/ongoing notification/permission-denied fallback নকশা ও বাস্তবায়ন পরিকল্পনা | High / High | Android API policy, BDX-01, notification permission | background/screen-off/cancel/process stop states tested; result state মিথ্যা নয় |
| BDX-03 | Servers-এ visible sort selector; latency-first valid ping | High / Low–Medium | UI callback/ViewModel | selected state ঘোষণা; filters-এর সঙ্গে কাজ; missing/negative latency শেষে |
| BDX-04 | Status/category/protocol/sync strings, semantics ও History date locale-aware | High / Medium | resource/string mapping | Bengali screen ও TalkBack-এ local status/date; English technical terms যথাযথ |

## P2 — Clarity, recovery, discoverability

| ID | কাজ | Impact / Effort | Dependency | Acceptance criteria |
|---|---|---|---|---|
| BDX-05 | “Not Working” aggregate-এ Unsupported/Cancelled-এর meaning ব্যাখ্যা বা label বদল | Medium / Low | usability copy test | user bucket-কে dead endpoint হিসেবে ভুল ব্যাখ্যা করে না |
| BDX-06 | no-history এবং filtered-no-match পৃথক করা | Medium / Low | existing `hasAnyHistory` state | empty dataset ও no-match UI আলাদা; clear filters আছে |
| BDX-07 | Saved/History query state আলাদা বা predictable reset | Medium / Low–Medium | destination-state choice | tab switch-এ surprise filter নেই; query clear দৃশ্যমান |
| BDX-08 | Settings-এ duplicate manual sync action consolidate; contextual Servers sync consistent | Medium / Low | copy/status design | এক canonical Settings sync; sync state feedback একরূপ |
| BDX-09 | “Add Server Request” → “Add Custom Server”, local-only disclosure | Medium / Low | localized copy | label, dialog ও outcome মেলে |
| BDX-10 | Search placeholder/help-এ ISP/source inclusion; dedicated filter user research-এর পরে | Medium / Low | user validation for dedicated filter | ISP query discoverable এবং matching verified |
| BDX-11 | Favorites empty-state থেকে Details Page reference সরানো | Medium / Low | string resource update | সব locale বর্তমান bookmark path নির্দেশ করে |
| BDX-12 | Single history delete Undo recovery | Medium / Medium | snackbar + repository restore | Undo original snapshot metadata ফিরিয়ে আনে |
| BDX-13 | TalkBack scan live-region frequency পরীক্ষা ও প্রয়োজনে throttle | Medium / Low–Medium | physical TalkBack session | start/milestone/end announcements; speech flood নেই |
| BDX-14 | Testing strategy/legacy design docs current route/theme/test evidence অনুযায়ী reconcile | Medium / Low | docs review | docs পাঁচ-tab nav ও unattached Scan surface বলে; stale claim চিহ্নিত |

## P3 — Evidence/quality follow-up, defect হিসেবে প্রমাণিত নয়

| বিষয় | কাজ | Gate |
|---|---|---|
| Screenshot/visual evidence | User-owned deleted screenshot restore না করে approved নতুন capture plan | Home/server/saved/history/settings; English/Bengali; idle/running/error; 4 themes |
| Adaptive behavior | 320dp, compact-height, 600/700/840dp, split-screen/fold/orientation | clipped controls নেই; rail/grid state stable |
| Motion/performance | low/mid device-এ frame-time/jank/battery trace; reduced motion/system setting | refresh-rate frame budget মাপা; decorative motion scan task বাধা নয় |
| Font/semantics | 1.3×/2.0×, Bengali long string, URL, TalkBack/accessibility scanner | action/status reachable, clipped নয় |
| Contrast | Rendered alpha/focus/disabled/pressed states all theme | documented pair-by-pair contrast report; current token test-এর দাবি বাড়ানো নয় |

## Dependency order

1. BDX-01-এর reachability/scan state establish → BDX-02 lifecycle/notification design যাচাই।
2. BDX-04 localized labels/semantics → BDX-05 status wording এবং BDX-13 screen-reader script test।
3. BDX-03 sort controlকে filter/search state-এর পাশে যোগ; `ServersViewModel` API ও unit coverage পুনঃব্যবহার।
4. BDX-06/07/12 history state ও recovery একসঙ্গে prototype-test।
5. BDX-08/09/10/11 copy/discoverability changes localization review।
6. BDX-14 documentation update implementation/release note-এ source-of-truth parity-র পরে।

## Impact–effort rationale

P1-গুলো core task (scan, fastest server) বা Bengali access-এ বাধা দেয়; P2-গুলো বিভ্রান্তি, accidental data loss বা discoverability-তে প্রভাব ফেলে। P0 দেওয়া হয়নি, কারণ tested theme contrast pass করেছে এবং কোনো catastrophic defect প্রমাণিত হয়নি। Performance/visual screenshot limitation-কে product defect হিসেবে score করা হয়নি।
