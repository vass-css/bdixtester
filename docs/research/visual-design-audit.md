# BDIX Tester — Visual Design Audit

**তারিখ:** 2026-11-18
**Evidence rule:** app screenshot না থাকায় নিচের audit Compose source, tokens ও test থেকে; “first fixation”, visual balance এবং rendered density সরাসরি দেখা হয়েছে বলে দাবি করা হচ্ছে না।

## বর্তমান visual foundation

- **Color:** `ui/theme/Color.kt`-এ semantic Material color roles ও BDIX status colors; চার preset, light/dark—মোট 8 scheme। Core token-pair contrast test pass (`ThemeSystemContrastAndMigrationTest`)।
- **Typography:** `Type.kt` English `Plus Jakarta Sans`, Bengali `Noto Sans Bengali`, numeric/URL `JetBrains Mono`; Bengali tracking বন্ধ ও line-height 1.14। Binary metadata-তে তিনটিই static Regular; heavier hierarchy synthetic weight-এ নির্ভরশীল। Script shaping/weight screenshots-এ যাচাই হয়নি।
- **Hierarchy:** Home-এ brand/network pill → working/total progress ring → View Servers/Start বা Stop; Servers-এ status tabs → category chips → list. এটি utility task-এর সঙ্গে source-level ভাবে সামঞ্জস্যপূর্ণ।
- **Components:** M3 TopAppBar, FilterChip, Card, OutlinedTextField, SettingsGroupCard, StatusBadge ও semantic colors পুনঃব্যবহার।
- **Density:** Server list card-এ নাম, category, IP/host, latency, bookmark, status, URL; wide width ≥700dp-তে দুই কলাম। Catalog-heavy utility-এর জন্য প্রয়োজনীয় তথ্য আছে, কিন্তু small width/font scale-এ screenshot verification দরকার।
- **Motion:** Home radar/glow/36-tick/particles scan চলাকালেই সক্রিয়; idle-এ animation স্থির। Reduced-motion active হলে infinite scan motion বন্ধ হয়। এটি purposeful হলেও low-end frame/battery profiling হয়নি।

## Gestalt ও composition review

| নীতি | বর্তমান evidence | মূল্যায়ন |
|---|---|---|
| Proximity | Settings section/card group; status/category controls আলাদা rows | Grouping যুক্তিসঙ্গত; rendered spacing screenshot ছাড়া অজানা |
| Similarity | shared FilterChip, ServerCard, StatusBadge, Settings rows | component reuse ভালো; Saved ও Servers card visual consistency screenshot-এ যাচাই দরকার |
| Contrast | semantic status colors; finite contrast unit test | core pairs verified; alpha-composited badge/icon/focus/disabled combinations সম্পূর্ণ pixel audit নয় |
| Hierarchy | Home CTA ও progress prominent; Servers live counts | source structure-এ কাজ-ভিত্তিক; first-fixation unverified |
| Balance/white space | Home full-bleed radar backdrop; server cards `surfaceContainerLow` | screenshot অনুপস্থিত—balanced/crowded বলে নিশ্চিত করা যাবে না |
| Rhythm/alignment | spacing tokens, max content widths 720–840dp, dynamic row/list | token consistency source-verified; exact alignment rendered view-এ Unverified |

## ইতিবাচক visual choices

1. Reachable/Restricted/Unreachable আলাদা color ও icon status; color একমাত্র code নয়।
2. Numeric latency ও URL-এর LTR/monospaced presentation ভাষা মিশ্রণে সহায়ক।
3. Home-এর scan CTA action-state অনুযায়ী Start/Stop; status ও progress এক জায়গায়।
4. Saved/History data-তে URL ও result metadata একসঙ্গে; direct launch নীতি পরিষ্কার।
5. Theme preset-ভিত্তিক palette থাকলেও semantic role ব্যবহার; অননুমোদিত theme redesign দরকার নেই।

## Findings cross-reference

- **BDX-03 (P1):** Fastest Ping sorting-এর strings/state আছে, visible UI নেই—visual hierarchy-তে discoverability issue।
- **BDX-04 (P1):** Enum-driven status/protocol/category English; bilingual visual system সম্পূর্ণ নয়।
- **BDX-05 (P2):** “Not Working” tab-এর broad grouping semantic expectation নষ্ট করে।
- **BDX-08/09 (P2):** sync duplicate ও “Add Server Request” label action hierarchy/meaning-এ দ্বৈততা।
- **BDX-10 (P2):** search-এ ISP matching আছে, কিন্তু placeholder-এ বোঝানো হয় না।
- **BDX-13 (P2, hypothesis):** live progress semantics speech overload ঘটায় কি না manual validation দরকার।
- **BDX-14 (P2):** পুরোনো design docs বর্তমান interface ভুল উপস্থাপন করে।

## Visual recommendations

- Existing palette, typography family, selected icons, card shape/elevation ও Material 3 pattern অক্ষুণ্ণ রেখে missing information discoverable করুন।
- Primary status/latency 11sp metadata treatment-এ চাপা না পড়ে; error/state text-এ যথেষ্ট size/line wrapping রাখুন।
- Status meaning icon + localized label + color একত্রে; completed/unknown/cancelled-কে “dead” appearance দেবেন না।
- Screenshot না পাওয়া পর্যন্ত “first fixation”, color balance বা one-handed placement-এর definitive visual judgment দেবেন না।
- ভবিষ্যৎ capture-এ Home idle/active/completed, Servers working/not tested/empty/OTT, Saved pinned/empty, History populated/no-match, Settings Bangla/English এবং সব theme/font-scale অন্তর্ভুক্ত করুন।

## Contrast evidence-এর সীমানা

Automated test 8 theme spec-এ onBackground/background, onSurface/surface variants, semantic reachable/restricted/unreachable, button gradients, nav/input/outline-সহ বহু নির্দিষ্ট pair যাচাই করে এবং বর্তমান run pass করেছে। এটি Compose-rendered screenshot, alpha overlay, focus ring, disabled/pressed state, font weight/size classification বা সব component-এ W3C audit নয়। তাই “সমস্ত screen WCAG compliant” বলা যাবে না।
