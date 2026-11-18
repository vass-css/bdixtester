# BDIX Tester — UX Research ও Industry Benchmark

**অডিটের তারিখ:** 2026-11-18<br>
**পরিধি:** Native Android, Kotlin, Jetpack Compose, Material 3
**অবস্থা:** Source ও public research evidence পর্যালোচনা করা হয়েছে; কোনো user interview বা device lab study করা হয়নি।

## গবেষণা-পদ্ধতি ও সীমা

প্রথমে repository, বর্তমান UI source, relevant tests এবং পুরোনো design/research docs পর্যালোচনা করা হয়েছে। Official guideline-কে normative evidence, Nielsen Norman Group-কে UX research, এবং product-maker-এর public page/screenshot-কে সীমিত benchmark হিসেবে ব্যবহার করেছি। কোনো competitor app install করে interaction test করা হয়নি; ফলে নিচের competitor তুলনা “public product/marketing evidence”, hands-on usability result নয়। BDIX Tester-এর tracked product screenshots working tree-তে নেই; source-only finding-কে screenshot observation বলা হয়নি।

## Evidence Matrix

| স্তর / বিষয় | Source ও তারিখ | নীতির সারাংশ | BDIX Tester-এ প্রয়োগ |
|---|---|---|---|
| Standard: WCAG 2.2 | [W3C Recommendation](https://www.w3.org/TR/WCAG22/), 2024-12-12; access 2026-11-18 | Normal text 4.5:1, large text 3:1; color একমাত্র signal নয়; target size AA-তে 24×24 CSS px বা নির্দিষ্ট spacing exception | Native Android-এর নিজস্ব 48dp touch guidance-ও মানতে হবে; status-এ text+icon+color একসঙ্গে রাখা |
| Compose semantics | [Android Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), live doc; access 2026-11-18 | Role, state, label, action semantics TalkBack ও Compose tests-কে UI অর্থ বুঝতে দেয় | `StatusBadge`-এর English hard-coded description-কে localized semantics-এ রূপান্তর; live-region frequency TalkBack-এ যাচাই |
| Compose accessibility | [Android accessibility](https://developer.android.com/develop/ui/compose/accessibility), live doc; access 2026-11-18 | Built-in semantics-সহ custom component-এরও সঠিক label, role, state দরকার | Existing clickable rows, switches, URL action-এর semantics ও focus order manual audit |
| Scalable content | [Android scalable content](https://developer.android.com/develop/ui/compose/accessibility/scalable-content), live doc; access 2026-11-18 | Font scaling-এ content reflow, no clipping/overlap, scrollability নিশ্চিত করতে হবে | 1.0x/1.3x/2.0x font scale এবং বাংলা long-label test প্রয়োজন |
| Adaptive layout | [Android window size classes](https://developer.android.com/develop/adaptive-apps/guides/use-window-size-classes), live doc; access 2026-11-18 | App window width/height dynamic; split-screen/fold/unfold-এ class বদলায় | বর্তমান 600dp rail ও 700dp grid breakpoints source-এ আছে; resize/foldable runtime validation অনুপস্থিত |
| Background work | [Foreground services](https://developer.android.com/develop/background-work/services/fgs), live doc; access 2026-11-18 | ব্যবহারকারী-দৃশ্যমান ongoing work হলে FGS ও ongoing notification বিবেচ্য; task-এর ধরন ও platform restrictions মানতে হবে | Scan-এর app-scope coroutine background guarantee নয়; current manifest-এ scan service/notification নেই |
| Notification permission | [Android notification permission](https://developer.android.com/develop/ui/views/notifications/notification-permission), live doc; access 2026-11-18 | Android 13+ notification permission এবং permission-denied fallback UX বিবেচনা করতে হবে | Completion alert-কে permission state-নির্ভর করে, in-app result সর্বদা দেখাতে হবে |
| Persistent task policy | [Android persistent work](https://developer.android.com/develop/background-work/background-tasks/persistent), live doc; access 2026-11-18 | WorkManager persistent deferrable work-এর জন্য; user-noticeable immediate scan-এর lifecycle আলাদা বিবেচ্য | Catalog sync worker-কে scan worker/notification ধরে নেওয়া যাবে না |
| Compose Material 3 | [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3), live doc; access 2026-11-18 | M3 components/semantic color roles থেকে ধারাবাহিক UI গঠন | বিদ্যমান M3 components ও semantic tokens বজায় রাখা; current contrast tests পুনরায় pass করেছে |
| Usability heuristics | [NN/g: 10 heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/), মূল 1994, reviewed 2024-01-30 | System status, user control, consistency, recognition over recall, error recovery | scan state, sort/ISP discoverability, history empty-state, destructive delete পর্যালোচনা |
| Visual hierarchy | [NN/g: 5 visual principles](https://www.nngroup.com/articles/principles-visual-design/), 2020; page reviewed 2026-09-01 | Scale, hierarchy, balance, contrast, Gestalt proximity অর্থ-গঠন করে | Source layout-এ top filters ও card metadata দেখা যায়; screenshot না থাকায় actual balance যাচাই হয়নি |
| Recognition vs recall | [NN/g: Memory Recognition and Recall](https://www.nngroup.com/articles/recognition-and-recall/), 2024-01-15 | স্মৃতি থেকে খুঁজে বের করার চেয়ে দৃশ্যমান option চিনে নেওয়া সহজ | Fastest Ping control দৃশ্যমান না হলে ব্যবহারকারীকে Settings/লুকানো আচরণ মনে রাখতে হয় |
| Touch ergonomics | [Android accessibility guidance](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility), live doc; এবং [NN/g touch targets](https://www.nngroup.com/articles/touch-target-size/), 2019; access 2026-11-18 | Native touch target আরামদায়ক হওয়া উচিত; WCAG web minimum-কে Android 48dp-এর বিকল্প ভাবা যাবে না | বর্তমান chips 48dp ও Settings rows 64dp—ভালো source-level evidence; physical reachability test বাকি |
| Icon recognition | [NN/g icon usability](https://www.nngroup.com/articles/icon-usability/), 2014; access 2026-11-18 | পরিচিত icon-ও label/context ছাড়া অস্পষ্ট হতে পারে | Bottom navigation-এ text label রাখুন; selected-state শুধু color-নির্ভর নয় |
| Network diagnostic benchmark | [Fing Mobile](https://www.fing.com/app/), public product page/images; access 2026-11-18 | Marketing screen-এ scan summary, security/performance cards, prominent rescan দেখা যায় | একটি glanceable summary + প্রধান scan action utility-এর সঙ্গে মেলে; Fing account/remote features নকলের সুপারিশ নয় |
| Speed-test benchmark | [Ookla Speedtest Android](https://www.speedtest.net/apps/android), page access 2026-11-18 | Product description-এ ping stages, history, server selection, result sharing; official marketing image-এ একক GO action | Primary action ও result history discoverability benchmark; BDIX-specific status ও data-limit disclaimer আলাদা রাখতে হবে |
| File-server benchmark | [X-plore](https://www.lonelycatgames.com/apps/xplore), product page/screenshots; access 2026-11-18 | FTP/FTPS/LAN, favorites/search, dense dual-pane browsing | Power-user file browsing-এ density প্রয়োজন হতে পারে; BDIX discovery app-এ dual-pane UI সরাসরি নকল নয় |

### Material page limitation

`m3.material.io`-এর dynamic page থেকে এই অডিটে নির্দিষ্ট component rule নির্ভরযোগ্যভাবে extract করা যায়নি। তাই M3 সম্পর্কিত implementation claim Android-এর Compose Material 3 documentation ও বর্তমান source-এ সীমিত রাখা হয়েছে; dynamic color বা M3 Expressive-এর নতুন visual treatment-কে current project dependency-তে আছে বলে ধরা হয়নি।

## Product Design Principles

1. **পরিমাপকে সত্যভাবে উপস্থাপন:** “Reachable from current network” ফলাফল; সার্ভারের স্থায়ী uptime বা পুরো ISP bandwidth নয়।
2. **একটি স্পষ্ট প্রধান কাজ:** scan শুরু, progress বোঝা, stop/cancel, ফলাফল ব্যবহার—প্রতি state-এ primary action পরিষ্কার।
3. **দ্রুত শনাক্তযোগ্য ফল:** status, latency, category ও ISP/source attribution একই hierarchy-তে; color কখনো একমাত্র cue নয়।
4. **Task-based, পাঁচ-tab IA:** Home, Servers, Saved, History, Settings বজায়; scan controls প্রয়োজন হলে secondary task route, ষষ্ঠ top-level tab নয়।
5. **User control ও safe recovery:** cancel, filter clear, undo/confirmation; background কাজের বাস্তব lifecycle স্পষ্ট।
6. **Bilingual first:** বাংলা labels/status/semantics/date; URL, IP, protocol ও metric-এর LTR formatting।
7. **Calm by default:** idle screen স্থির; motion কেবল progress/feedback ব্যাখ্যা করলে, reduced-motion মান্য করে।
8. **Evidence before redesign:** Source-only finding, screenshot observation, research recommendation ও hypothesis পৃথক করে।

## Research থেকে পাওয়া সিদ্ধান্ত

- বর্তমান theme token contrast test 8টি theme configuration-এর নির্দিষ্ট text/UI pair-এ pass করেছে; এটি rendered screen-এর পূর্ণ WCAG audit নয়।
- Current UI-তে sort algorithm/strings আছে, কিন্তু visible sort control নেই—recognition over recall-এর ব্যর্থতা।
- Server search index ISP/source attribution অন্তর্ভুক্ত করে, কিন্তু placeholder-এ ISP উল্লেখ নেই; feature লুকানো।
- Background scan/notification একটি product promise হলে Android lifecycle-compliant UX দরকার; CatalogSync-এর WorkManager scan-কে cover করে না।
- User study হয়নি; persona, pain point frequency, visual hierarchy judgement ও acceptance result—সবই validation candidate, observed user truth নয়।

## প্রধান Source-এ প্রত্যক্ষ প্রয়োগ

এই report-এর design recommendation `complete-ux-audit.md`-এর `BDX-01`–`BDX-14` finding ID-তে রূপান্তরিত হয়েছে। কোনো competitor screen বা public page-কে BDIX Tester-এর design requirement হিসেবে কপি করা হয়নি।
