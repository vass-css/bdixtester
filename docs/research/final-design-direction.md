# BDIX Tester — Final Design Direction

**Audit date:** 2026-11-18
**Recommendation:** **Quiet Precision Diagnostic** — current product identity ও five-destination IA সংরক্ষণ করে তথ্যের সত্যতা, scan control, discoverable sorting এবং bilingual accessibility-কে visual flourish-এর আগে রাখা।

## বিকল্প concept

| Concept | লাভ | ঝুঁকি | সিদ্ধান্ত |
|---|---|---|---|
| Neon radar / animation-led | scan-এর প্রযুক্তিগত অনুভূতি; Home-এ strong brand memory | low-end GPU/battery, decorative layer attention নেয়; static screenshot নেই | বর্তমান radar-কে scan-এ purposeful রাখুন; আর animation যোগ নয় |
| Engineer console / dense telemetry | latency/probe details এক screen-এ power-user-এর জন্য | novice-এর cognitive load; phone density/বাংলা text; scan details এখন route-ই নয় | details দরকার হলে secondary scan surface-এ progressive disclosure, Home-এ নয় |
| Content marketplace / server browsing | category/OTT browsing পরিচিত | network diagnostic-এর urgency হারায়; irrelevant editorial treatment | reject |
| Quiet Precision Diagnostic | status, progress, latency, direct action-এ স্পষ্ট; current Material 3/theme re-use | sort/scan/localization gaps বাস্তব implementation কাজ চায় | **Selected** |

## Design philosophy

1. **State before decoration:** Home-এ idle, running, interrupted, completed—একই layout-এ স্পষ্ট state/primary action।
2. **Result is contextual:** “Reachable from current network” বলুন; global availability/ISP bandwidth দাবি নয়।
3. **Five destinations stay:** Home, Servers, Saved, History, Settings। Scan task route top-level nav নয়।
4. **Use visible options over memory:** sort, ISP search scope ও filter state স্পষ্ট।
5. **Bilingual by design:** Bangla body/status/semantics/date; domain URL/protocol/metric LTR।
6. **Undo where data is reversible:** single history delete undo; destructive bulk reset confirm।
7. **Motion with meaning:** scan active-এ progress cue, idle-এ calm; user/system reduced motion respect।
8. **Design system reuse:** current semantic palette/theme/font/components ধরে; new token only justified gap-এ।

## Visual identity recommendation

- চার curated palette ও Light/Dark mode বজায়; বর্তমান contrast test ফলকে সীমিত claim হিসেবে উপস্থাপন।
- `surfaceContainer*`, semantic status, primary action, typography roles ব্যবহার; additional gradients/ambient blur যোগ নয়।
- Primary scan action prominent, status/latency data secondary; category/search/sort tightly grouped but visually distinguishable।
- Status icon + localized text + color; “Restricted/Unsupported/Cancelled”কে একই dead-end appearance নয়।
- Cards/URL affordance direct external open-এর current meaning বজায়; metadata/details page নয়।
- Screenshot অনুপস্থিত থাকায় exact ring diameter, first fixation, spacing, visual saturation সম্পর্কে final aesthetic score দেওয়া যাবে না।

## Product strategy — staged

**Now (P1):** Scan surface reachable করা; background scan capability বাস্তবায়ন/সীমা নির্ধারণ; Fastest Ping UI; Bengali status and semantics.
**Next (P2):** History empty/recovery/query; sync/add-server wording; ISP search cue; obsolete empty copy; status group clarity.
**Then (P3):** Screenshot/device benchmark, adaptive/font-scale/TalkBack/performance measurement; evidence অনুযায়ী visual tuning.

## Long-term maintainability

- UI labels enum-এর English `displayLabel`-এ না রেখে localized resource mapping।
- Finding ID-র সঙ্গে string resources, UI tests, accessibility semantics ও documentation source links রাখুন।
- Theme contrast test + rendered state capture update policy নির্ধারণ।
- Scan engine lifecycle, notifications, UI progress এক state contract-এ; CatalogSync lifecycle থেকে আলাদা।
- Competitive product screenshot inspiration হিসেবে; direct visual copying নয়।
- কোনো implementation এখন করা হয়নি; এই direction কেবল পরবর্তী কাজের জন্য approval-seeking recommendation।
