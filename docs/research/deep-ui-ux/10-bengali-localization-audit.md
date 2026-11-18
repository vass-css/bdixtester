# Bengali Localization and Bilingual UX Audit

## 1. Resource findings

The English/default and `values-bn` string resources each contain 305 keys, with parity in the inspected resource files (`LOC-001`). This corrects the prior broad implication that Bangla resource coverage is absent. However, parity does not mean all visible UI is localized: enum labels and some semantic strings are generated in code, and History formats dates using `Locale.US` (`LOC-001`, `A11Y-001`).

| Surface | Verified observation | Impact / confidence |
|---|---|---|
| Resource key coverage | 305 default keys and 305 Bengali keys | High confidence for those files; not proof every visible string uses resources |
| Enum status/category/protocol labels | `Enums.kt` exposes English display labels used in UI (`LOC-001`) | High source confidence; user impact unmeasured |
| Status semantics | `StatusBadge` accessibility description is hard-coded English (`A11Y-001`) | High source confidence; spoken behavior and impact untested |
| History dates | `FavoritesScreen` formats with `Locale.US` | High source confidence; Bengali locale formatting not reflected |
| Custom action wording | “New Server Add Request” opens local custom-server dialog | High source confidence; Bengali/English terminology may imply an external request |
| Protocol and URL | English technical tokens/URLs occur in server data | Expected bilingual content; preserve protocol syntax and distinguish it from translated copy |
| Font shaping and large text | Not tested on device | Unverified; do not claim Bengali rendering defect or success |

## 2. Terminology glossary for research and future review

These are **candidate terms**, not approved final copy. Validate with Bengali-first users, especially users who do not know networking vocabulary.

| English concept | Candidate Bangla wording | Keep technical token? | Review note |
|---|---|---|---|
| Server | সার্ভার | Yes, common loanword | Confirm reader familiarity |
| Test / scan | পরীক্ষা / স্ক্যান | Maybe both | Use consistently; “scan” may imply security scanning |
| Available / reachable | সাড়া দিচ্ছে / সংযোগ করা যাচ্ছে | Context-dependent | Avoid implying permanent availability |
| Not working | কাজ করছে না / সাড়া পাওয়া যায়নি | Prefer exact outcome | “সাড়া পাওয়া যায়নি” is more accurate for timeout |
| Timed out | সময়সীমার মধ্যে সাড়া পাওয়া যায়নি | Preserve timeout detail | Explain retry/network dependency |
| Restricted | সীমাবদ্ধ / প্রবেশ সীমাবদ্ধ | Context-dependent | Verify what the code's restriction means before translating |
| Unsupported | সমর্থিত নয় | Yes, if protocol context | Distinguish capability from outage |
| Latency | সাড়া পেতে সময় / লেটেন্সি | Show both initially? | Must not call ICMP ping unless actual ping |
| Ping | পিং | Yes | Use only for ICMP RTT measurement |
| Download speed | ডাউনলোড গতি | No | Keep unit (Mbps) and test context |
| ISP | ইন্টারনেট সেবা প্রদানকারী (ISP) | Yes after first mention | Validate preferred local usage |
| FTP | FTP | Yes | Expand only if helpful; do not translate URL/protocol |
| Favorite / saved | সংরক্ষিত / পছন্দের | Choose distinct semantics | Avoid conflating save and pin |
| Pin | পিন করা | Yes | Test comprehension |
| History | ইতিহাস / পরীক্ষার ইতিহাস | Context-specific | Prefer task-specific wording |
| Copy URL | লিংক কপি করুন | URL token may remain | Announce copied state accessibly |

## 3. Localization recommendations

1. Resource-back all dynamic enum labels, status names, category/protocol labels and accessibility descriptions.
2. Use locale-aware date, time and number formatting while preserving measurement units and endpoint syntax.
3. Test Bangla-first layouts with long strings, mixed Bengali/Latin text, numbers, punctuation, URLs and visually similar Latin characters.
4. Check font fallback and Bengali conjunct shaping across supported Android versions/devices; avoid assuming the emulator's font represents all devices. This remains unverified, not a confirmed rendering defect.
5. Test screen reader speech with locale and pronunciation settings; semantic status should communicate cause, not just repeat a short badge.
6. Keep English protocol names searchable, and allow Bangla/English query terms where catalog data supports them.
7. Review machine-like technical language with Bengali-first users before finalizing terminology.

## 4. Validation matrix

Test English and Bangla with default/max font scale, compact width, long ISP/server names, long URLs, all result states, empty/error dialogs, scan progress and history dates. Record screenshot, device/API, locale, font, exact string, truncation/overlap and TalkBack output. Existing `LocalizationAccessibilityUiTest` is a rendering smoke test, not a pronunciation or device accessibility test.

