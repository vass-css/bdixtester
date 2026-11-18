# BDIX Tester — Usability Testing Strategy

**তারিখ:** 2026-11-18
**অবস্থা:** প্রস্তাবিত protocol; এখনো কোনো participant-এর সঙ্গে চালানো হয়নি।

## উদ্দেশ্য ও গবেষণা প্রশ্ন

1. নতুন ব্যবহারকারী scan শুরু, progress ব্যাখ্যা, stop ও ফলাফল থেকে server open করতে পারে?
2. Power user working/fastest/ISP/category result কত ধাপে পায়?
3. Bengali UI-তে status, category, protocol, history date বোঝা যায়?
4. Background-এ scan চলা, notification, permission deny ও return-to-result প্রত্যাশা মেলে?
5. Saved/pinned/history/export/delete controls predictable ও recoverable?

## Participants

প্রথম formative round-এ 6–8 জন: বাংলা-প্রধান 3–4, bilingual 2, নিয়মিত FTP/BDIX বা network-utility ব্যবহারকারী 1–2 (overlap অনুমোদিত)। এটি representative population claim নয়। Accessibility round-এ TalkBack ব্যবহারকারী/low-vision participant আলাদা করে অন্তর্ভুক্ত করতে হবে। অংশগ্রহণকারী না পাওয়া গেলে simulated review-কে user research বলা যাবে না।

## Task scenarios

| ID | Scenario (facilitator neutral prompt) | Measure / observation |
|---|---|---|
| T1 | “এই নেটওয়ার্কে কোন সার্ভার ব্যবহারযোগ্য তা পরীক্ষা করুন” | scan start success, mode/default বোঝা, time-to-start, wrong taps |
| T2 | “পরীক্ষা চলাকালে অগ্রগতি দেখুন, তারপর বন্ধ করুন” | progress interpretation, cancel discovery, state after stop |
| T3 | “সচল server-গুলোর মধ্যে দ্রুততমটি খুঁজে খুলুন” | `BDX-03`; filter/sort success, time-to-result, mistaken browser launch |
| T4 | “একটি নির্দিষ্ট ISP-এর server খুঁজুন” | `BDX-10`; search term, query discoverability, result accuracy |
| T5 | “একটি server save ও pin করে পরে খুলুন” | bookmark/pin distinction, saved retrieval, direct browser behavior |
| T6 | “আগের test খুঁজে protocol/status filter দিন, export করুন” | `BDX-04/06/07`; no-result understanding, date comprehension |
| T7 | “একটি ভুল history entry মুছে ফেলুন” | `BDX-12`; undo/confirmation expectation, recovery |
| T8 | “Scan শুরু করে অন্য app-এ যান; ফলাফল ফিরে পান” | `BDX-02`; ongoing progress, notification permission, process/background truth |
| T9 | Bengali locale + large text-এ status/action খুঁজুন | `BDX-04/13`; TalkBack label, clipping, RTL/LTR switching |

## Metrics

- **Task success:** unassisted / assisted / failed; T1, T3, T4, T5, T6-এর completion।
- **Time-on-task:** start-to-progress, fastest server find, ISP discovery, result retrieval।
- **Errors:** wrong filter/sort, accidental external launch, lost query, mistaken “no history”, destructive mis-tap।
- **Comprehension:** participant “Restricted”, “Not Working”, latency, ISP attribution, scan completion কী বোঝায় তা paraphrase করবে।
- **Perceived effort:** task শেষে 1–7 Single Ease Question; qualitative reason নথিবদ্ধ।
- **Accessibility:** TalkBack focus/order/announcement count; 1.0×/1.3×/2.0× text; touch bounds।
- **Performance:** reference low/mid/high Android device-এ frame-time budget relative to refresh rate, jank, battery, scan state-update responsiveness; কোনো device measurement এখনো নেই।

## Proposed success gates (pilot-এর জন্য; observed result নয়)

- 6 জনের মধ্যে কমপক্ষে 5 জন T1/T3/T4/T5 সহায়তা ছাড়া শেষ করবে; critical data-loss বা accidental launch শূন্য।
- 6 জনের মধ্যে অন্তত 5 জন “Restricted/Not Working/Cancelled” পার্থক্য ঠিক ব্যাখ্যা করবে।
- Bengali TalkBack-এ primary controls-এর role/name/state বলা হবে; scan চলাকালে announcement queue ব্যবহারকারীকে বাধা দেবে না।
- 1.3×/2.0×-এ primary CTA, status, action ও destructive confirmation কাটা/অদৃশ্য হবে না।
- Background task product promise হলে permission denied/process interruption-এ UI স্পষ্ট fallback দেখাবে; success shape করে ভুল claim নয়।

## Moderation, privacy ও reporting

- Participant-এর ব্যক্তিগত ISP credentials/URLs সংগ্রহ নয়; test catalog/stub data ব্যবহার।
- User screen recording অনুমতি ছাড়া নয়; notes-এ anonymized participant IDs।
- প্রথমে current implementation prototype test; redesign comparison একই task/time এবং counterbalanced order-এ।
- Findings severity × frequency × task criticality দিয়ে; sample size-কে statistical proof বলা যাবে না।
- Automated test আলাদা: Compose semantics/localization, status mapping, filter no-result, sorting null/negative latency, undo, notification lifecycle, theme contrasts, font scaling।

## Completion status

এই protocol future validation-এর জন্য। বর্তমান audit-এ interviews, moderated usability tests, TalkBack device checks, screen reader user tests বা physical-device performance run হয়নি।
