# BDIX Tester — Deep UI/UX Research Methodology

**তারিখ:** 2026-11-18  
**অবস্থা:** ১৫টি নথির evidence register সম্পন্ন; source-backed findings পুনরায় যাচাই করা হয়েছে। Hands-on device বা participant research হয়নি।  
**পরিধি:** Research, inspection, analysis এবং documentation בלבד। কোনো application implementation করা হয়নি।

## Executive summary

**Design decision: MINOR TARGETED IMPROVEMENTS RECOMMENDED. NO REDESIGN REQUIRED.**

Preserve the current visual identity, screen layouts, navigation and features. Source inspection and existing tests do not justify a broad redesign or a new navigation destination. The dedicated `ScanScreen` is not a root route, but the scan task is already reachable from Home and Servers; the previous claim that core scanning was inaccessible is withdrawn.

The evidence register has 18 stable finding IDs: 5 confirmed, 5 partially confirmed, 2 unverified, 3 not-a-defect determinations and 3 optional improvements. Severity is separate from priority: Critical 0, High 0, Medium 2, Low 8, Informational 8; P0 0, P1 0, P2 3, P3 15. The P2 items are localized dynamic labels/accessibility text and History’s misleading filtered-empty copy. These counts describe conservative source-based triage, not observed user frequency.

Three small content/state corrections are supported by source evidence: localize enum/status semantics and locale-sensitive History output; distinguish History with no records from History with no matching filter result; and remove the obsolete “detail screen” wording from Saved’s empty state. None requires a layout or navigation redesign. Other findings are optional or require validation.

The evidence register records exact source paths/lines, test limits, severity, priority, evidence status, external links, and the task-start repository snapshot: [14-research-evidence-register.md](./14-research-evidence-register.md).

## 1. উদ্দেশ্য ও সীমা

এই phase-এর উদ্দেশ্য আগের নয়টি audit/report-কে স্বাধীনভাবে যাচাই করে বর্তমান BDIX Tester UI সংরক্ষণ-ভিত্তিক, evidence-backed সিদ্ধান্ত তৈরি করা। আগের conclusion-কে source of truth ধরা হয়নি; current source এবং test file থেকে পুনরায় পরীক্ষা করা হয়েছে।

এই phase-এ Kotlin/Java/XML, UI, navigation, behavior, tests, dependencies, Gradle/build configuration বা architecture পরিবর্তন করা হয়নি। নতুন report-গুলো কেবল `docs/research/deep-ui-ux/`-এ রাখা হয়েছে। Existing report, screenshot deletion বা অন্য user-owned change স্পর্শ করা হয়নি।

## 2. Baseline রেকর্ড

নিচের টেবিলটি আগের deep-research phase-এর historical snapshot; এটি এই final-verification task-এর initial working-tree snapshot নয়। Final-verification task-এর branch, HEAD, upstream, staged/unstaged files, deletions ও untracked files [evidence register](./14-research-evidence-register.md)-এ আলাদাভাবে সংরক্ষিত।

Project root: `/workspaces/codespaces-blank/bdixtester`

| Baseline item | Phase শুরুর পর্যবেক্ষণ |
|---|---|
| Branch | `perf/github-action` |
| HEAD | `04d40d73a6decdbc1d277c564370ca8b06702a2e` |
| Modified tracked file | `docs/design/screen-redesign-specification.md` |
| Deleted tracked assets | `public/assets/aistudio/`-এর ১৫টি `Screenshot_*.png` |
| Existing untracked docs | পূর্ববর্তী audit-এর আটটি report: design-এ ৩টি, research-এ ৫টি |
| Source/test/build/config status | আগের research phase-এর snapshot-এ clean বলে লেখা ছিল; এটি এই final-verification task-এর snapshot নয় |
| Existing docs inventory | `docs/`-এ ৬৩টি file; আগের ৯টি report উপস্থিত |
| Previously recorded baseline | ঐতিহাসিক prior-phase record; final-verification task-এর নিজস্ব initial status নিচের evidence register-এ আলাদাভাবে সংরক্ষিত |

Untracked report-গুলোর নাম: `docs/design/accessibility-and-inclusive-design-audit.md`, `design-system-recommendations.md`, `priority-improvement-roadmap.md`, এবং `docs/research/complete-ux-audit.md`, `final-design-direction.md`, `usability-testing-strategy.md`, `visual-design-audit.md`, `world-class-ui-ux-research.md`।

Deleted screenshot-গুলোর নাম (সবই pre-existing): `Screenshot_1_a4ae1f15.png`, `Screenshot_2_a35b0d9e.png`, `Screenshot_3_842c5431.png`, `Screenshot_4_7cd893ea.png`, `Screenshot_5_8ab5e4c5.png`, `Screenshot_6_0a225014.png`, `Screenshot_7_0aec8ed6.png`, `Screenshot_8_8c142243.png`, `Screenshot_9_2950e42b.png`, `Screenshot_10_0138ff7b.png`, `Screenshot_11_673b0377.png`, `Screenshot_12_08a6c65b.png`, `Screenshot_14_0efc5430.png`, `Screenshot_15_68012283.png`, `Screenshot_16_d00a22e2.png`।

## 3. Evidence ও confidence convention

- **Verified:** সরাসরি source/test বা authoritative source-এ যাচাইকৃত।
- **Partially verified:** মূল behavior/code প্রমাণিত, কিন্তু user impact/runtime behavior-এর অংশ যাচাই করা যায়নি।
- **Unverified:** device, screenshot, app installation, account, live network বা participant access অনুপস্থিত।
- **Recommendation:** evidence থেকে প্রস্তাব; বর্তমান behavior হিসেবে উপস্থাপিত নয়।
- **High confidence:** সরাসরি authoritative বা source evidence; **Medium:** সরাসরি observation আছে, প্রভাব/ব্যাখ্যায় অনিশ্চয়তা; **Low:** সীমিত/পরোক্ষ evidence।

Finding prefix: `UX-`, `A11Y-`, `LOC-`, `NAV-`, `PERF-`, `BDIX-`। Finding ID-গুলো স্থায়ী; report জুড়ে একই finding-এর একই ID। Official/competitor source IDs ও exact URLs evidence register-এ আছে।

Application source inspection কখনো hands-on usability/device test হিসেবে গণ্য হয়নি। Competitor listing-এ বর্ণিত feature publisher claim; operational truth নয়। Public screenshot URL থাকা মানে screenshot visual review হয়েছে—এমন নয়।

## 4. পদ্ধতি

1. Git branch/HEAD/status ও existing `docs/` inventory-এর read-only snapshot নেওয়া।
2. আগের নয়টি report পড়া; আগের Finding ID, priority, evidence এবং user-owned decisions-এর সঙ্গে তুলনা।
3. Android source, navigation, screens, state holders, resources, manifest, selected tests ও existing performance/QA documentation inspect করা।
4. Official platform/design sources এবং competitor publisher/Google Play pages-এর সুনির্দিষ্ট URL retrieve ও যাচাই করা; publisher listing-কে runtime proof ধরা হয়নি।
5. Source finding, external principle, impact, confidence, evidence status, severity, priority, recommendation এবং validation method আলাদা করে লেখা।
6. User study বা বাস্তব device measurement ছাড়াই scorecard-derived design ranking-কে সিদ্ধান্তের প্রমাণ হিসেবে ব্যবহার না করা।
7. ১৫টি report-এর cross-reference, findings, priorities, URLs, internal links, whitespace এবং initial/final Git status যাচাই করা।

## 5. সীমাবদ্ধতা

- BDIX Tester বা competitor app install করে device interaction করা হয়নি।
- BDIX Tester-এর usable screenshot এই working tree-তে নেই; পূর্বের screenshot deletion restore বা copy করা হয়নি।
- Physical Android device, TalkBack/Switch Access, Accessibility Scanner, low-end device profiler, power meter এবং real ISP access এই phase-এ উপলভ্য ছিল না।
- User interview/usability test হয়নি; impact frequency, comprehension, first-click success এবং preference score user-observed fact নয়।
- Google Play/official publisher listings are publisher-provided descriptions; actual app runtime, reviews, ratings and screenshot usability are not treated as verified evidence.
- Google search output is discovery only, not a feature verification source. Only directly opened product pages are cited for competitor claims.
- Live official documentation often has no displayed publication/update date; that absence is recorded rather than guessed.

## 6. Research inventory

- **35 external source URLs** are registered: Android/Compose 12, Material 3 1, WCAG 1, established UX/institutional sources 4, and competitor/publisher pages 17. Retrieval verifies the cited page, not every feature described by its publisher.
- **14 official Android/Material/W3C sources** are linked. Apple HIG, GOV.UK and NN/g are contextual references; they are not app-conformance evidence.
- **15 products/services** are benchmarked through publisher or Google Play pages: nine general diagnostics products, five Bangladesh-focused products discovered through Play search, and X-plore as an FTP/file-manager analogue. No hands-on competitor testing occurred.
- BDIX-focused Play search returned four distinct local testing/discovery products in addition to the prior BDIX Tester listing; this is a search sample, not a complete market census.

The precise source register, finding evidence, statuses, URLs and access date are in [14-research-evidence-register.md](./14-research-evidence-register.md).

## 7. Finding outcome vocabulary

Prior-report outcomes: **Confirmed**, **Partially confirmed**, **Not confirmed**, **Outdated**, **Contradicted**, **Unverified**। “Not confirmed” মানে পুরোনো claim current evidence-এ মেলেনি; “Unverified” মানে যথেষ্ট evidence/access ছিল না। Severity বদলালে rationale traceability report-এ আছে।

Final classifications use the explicit uppercase status vocabulary in the evidence register. Severity and priority are separate fields; no Critical/High or P0/P1 issue is substantiated. “Confirmed” may refer to a code or copy fact, not measured user harm.

