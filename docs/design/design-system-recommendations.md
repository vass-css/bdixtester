# BDIX Tester — Design System Recommendations

**তারিখ:** 2026-11-18 | **উদ্দেশ্য:** বর্তমান system audit ও পরবর্তী design work-এর সীমা; implementation নির্দেশ নয়।

## 1. System-এ যা ইতিমধ্যে আছে

- Material 3 `ColorScheme`, semantic reachability/status tokens এবং 4 curated preset × Light/Dark (`ui/theme/Color.kt`, theme registry)।
- `BdixSpacing`, `BdixBorder`, `BdixIconSize`, shared `StatusBadge`, `ServerCard`, Settings row/group এবং motion helpers।
- `Plus Jakarta Sans`, `Noto Sans Bengali`, `JetBrains Mono`; Bengali line-height multiplier 1.14 ও letter spacing 0।
- Current selected/unselected navigation icon, status tab, region chip ও scan-state patterns।
- 8 scheme-এর নির্বাচিত text/UI contrast pairs-এর automated test pass।

## 2. সুপারিশকৃত token contract

| Token family | নীতি |
|---|---|
| Color | `primary/onPrimary`, `surface/onSurface`, `surfaceContainer*`, `outline*`, `error*` ও semantic reachable/restricted/unreachable ব্যবহার; নতুন screen-এ raw hex নয় |
| State | status-এর color-এর সঙ্গে icon + localized text; `Unknown/Unsupported/Cancelled`-কে unreachable-এর সমার্থক করবেন না |
| Typography | Material type role; বাংলা paragraph body ideally 14–16sp range-এ যাচাই, 11–12sp কেবল secondary metadata; `sp` scaling ও wrapping বজায় |
| LTR data | URL, IP, protocol, latency-তে `LtrContent`; বাংলা বাক্যে technical token-এর bidirectional boundary যাচাই |
| Spacing | বর্তমান `BdixSpacing` tokens-ই canonical; নতুন ad-hoc spacing আগে existing token-এ map |
| Touch | interactive target 48dp Android guideline aim; current `minimumInteractiveComponentSize`, 48dp chips, 64dp Settings rows বজায় |
| Shape/elevation | বর্তমান 20dp card/Material surface hierarchy বজায়; card elevation/outline একসঙ্গে অতিরিক্ত জোর না দেওয়া |
| Motion | idle-এ স্থির; purpose-bound transition; reduced-motion মান্য; progress animation test/profiling-এর আগে বাড়ানো নয় |
| Icon | Material Icons family; tab label ও accessible action name বজায়; শুধু outline/filled icon দিয়ে meaning নির্ভর নয় |

## 3. Component state inventory (প্রস্তাবিত)

| Component | State/variant অবশ্যই নকশা/পরীক্ষা করতে হবে |
|---|---|
| StatusBadge | Not tested, queued, testing, reachable, restricted, unreachable, timeout, unsupported, cancelled; বাংলা/ইংরেজি semantics |
| Server card | untested/working/restricted/error, no latency/valid latency, saved/pinned/custom, long name/URL, external open |
| Search/filter | collapsed/expanded, active query, no result, reset, category + status |
| Scan action/progress | idle/start, running, cancellation, completed, interrupted, no target, background/permission-denied |
| Empty state | truly empty vs filtered-no-match; clear action; no stale Details Page reference |
| Sync state | idle/syncing/updated/not modified/wifi required/rejected/network error; one understandable retry |
| Settings picker | selected/unselected, long translated text, system/light/dark, permission/error dialog |

## 4. Quality gates

1. Theme contrast tests সব 8 palette/mode-এ চলবে; report-এ tested pairs ও untested rendered states আলাদা থাকবে।
2. String resource parity check; enum display labels view-এ সরাসরি ব্যবহার না করে localized mapping-এর design decision।
3. Font-scale 1.0/1.3/2.0, বাংলা long-label, URL/IP LTR, TalkBack role/state/label tests।
4. Touch measurement source code-এ নয়, rendered bounds/Compose semantics/device-এও validate।
5. Reduced motion-এ scan status/progress তথ্য হারাবে না; শুধু ornamental loops কমবে।
6. Adaptive validation current window width/height, split-screen/fold state-এ।
7. UI screenshot baseline user-owned image restore না করে, explicit new capture workflow/approval ছাড়া তৈরি নয়।

## 5. Decisions explicitly not recommended

- নতুন theme/color palette বা Android dynamic color যোগ করা—এ অডিটে user benefit প্রমাণিত নয়, curated palette decision বজায়।
- Full-screen Server Details, hamburger menu, Home-এর বড় Quick/Full toggle, পুরোনো Wi-Fi/IP summary card ফিরিয়ে আনা।
- Server card tap behavior বদলে metadata page খোলা; বর্তমান direct external browser flow test-verified।
- M3 Expressive/নতুন API যোগ করা dependency/version verification ছাড়া।
- Card redesign, grid conversion, pin section, bottom sheet/copy action—user evidence ছাড়া scope বৃদ্ধি নয়।
