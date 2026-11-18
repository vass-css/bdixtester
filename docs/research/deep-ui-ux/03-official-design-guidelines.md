# Official Design and Accessibility Guidance

**Access date for cited sources:** 2026-11-18. Cited pages were retrieved; exact URLs and evidence scope are listed in [14-research-evidence-register.md](./14-research-evidence-register.md). These are design references, not proof that this app conforms or fails.

## 1. Android and Compose guidance mapped to BDIX Tester

| Evidence | Primary guidance | Application | Evidence-backed audit / recommendation |
|---|---|---|---|
| ANDROID-001 | Compose accessibility semantics and testing | Every clickable icon, server card, scan status and filter | Audit semantics and focus order; a source-level `contentDescription` does not prove TalkBack usability. |
| ANDROID-002 | Support large font/display scaling | Server names, URLs, latency, settings and Bangla strings | Test the full app at enlarged system font; avoid fixed text height and clipped controls. |
| ANDROID-003 | Window size classes and adaptive layout | Tablets/foldables/large screens | Validate content width and navigation presentation at actual size classes; no current layout conclusion from source alone. |
| ANDROID-004 | Accessible touch target and contrast guidance | Icon actions, chips, navigation, status affordances | Measure rendered bounds/contrast; do not infer compliance from theme tokens alone. |
| ANDROID-005 | Material 3 components and Compose theming | Existing M3 app surfaces | Reuse coherent existing M3 components and tokens; redesign only where task evidence supports it. |
| ANDROID-006 | Accessibility testing principles/tools | Tests plus manual service testing | Add device/service checks to a future validation plan; existing Compose smoke tests are not TalkBack verification. |
| ANDROID-007 | Background work should match task and constraints | Long server scans | Decide whether user-visible scan must survive app background/process death; do not equate process scope with durable work. |
| ANDROID-008 | Foreground service requirements for user-noticeable work | Explicit ongoing scan, if required | If continuous user-started scanning must continue while app is backgrounded, evaluate current Android FGS policy/type/permission/notification requirements before implementation. This is conditional guidance, not a blanket recommendation to add a service. |
| ANDROID-009 | Compose localization and resource practices | Bangla/English content | Keep user-facing copy in locale resources; dynamic enum/status labels and semantics must not bypass resources. |
| ANDROID-010 | Per-app language support | Language setting | Verify Android app-language integration and in-app selector synchronization; current implementation was source-inspected, not device-tested. |
| ANDROID-011 | Compose performance and recomposition | Server lists, scan progress | Profile first; use stable state/keys and avoid expensive work during composition when supported by measured evidence. |
| ANDROID-012 | Lazy layouts for large collections | Server catalogue | Confirm actual lazy list implementation and stable keys; measure scroll behavior on representative low-end devices. |

## 2. Standards and institutional guidance

### WCAG 2.2 (`WCAG-001`)

Use WCAG success criteria as an audit reference, not as a claim that web conformance directly certifies a native Android app. Relevant principles include text/non-text contrast, color not being the sole cue, accessible names, target size, focus order/visibility, reflow/zoom concepts, and alternatives to dragging. Map each requirement to Android-specific platform guidance and test actual rendered UI.

### Material Design 3 (`M3-001`)

Material component guidance provides a coherent baseline for interactive state, color roles, typography, touch affordances and adaptive surfaces. It does not answer BDIX-specific measurement semantics or guarantee accessibility from using a component library. Preserve the existing M3-based system unless a demonstrated task gap warrants change.

### Apple Human Interface Guidelines (`UXSRC-003`)

Cross-platform principles such as clarity, direct manipulation, feedback and respecting platform conventions can inform evaluation, but iOS patterns must not override Android navigation or Compose/M3 conventions.

### GOV.UK Design System (`UXSRC-004`)

Its guidance on plain language, error messages and forms is useful as a content-design reference. It is not Android widget guidance; adapt only the principles (specific error, next step, user control) and validate Bengali technical language.

### Nielsen Norman Group (`UXSRC-001`, `UXSRC-002`)

Heuristics and progressive disclosure provide evaluation questions: system status visibility, match with user language, user control, consistency, and reducing unnecessary cognitive load. They are principles, not empirical proof that any specific BDIX change will improve outcomes.

### Baymard Institute (`UXSRC-005`)

Baymard’s public UX content is commerce-oriented. Its findings about discoverability and filtering are only analogies for a server catalogue; do not generalize e-commerce outcomes to this diagnostic utility without testing.

## 3. Derived audit checklist

1. **Name what was measured:** distinguish reachability, HTTP response latency, FTP greeting/connect latency, TCP connect fallback, and download throughput.
2. **Expose system status:** scan phase, progress, completion/cancellation, failure cause and recovery action should be understandable and announced appropriately.
3. **Maintain user control:** filters/search/sort, browser handoff, deletion, and scan cancellation require predictable outcomes and recovery where destructive.
4. **Accessible interaction:** names/roles/states, focus order, touch target size, text scaling, contrast, status icons and announcements need rendered/device validation.
5. **Localization:** use locale-sensitive resources and formatting; account for Bengali shaping, mixed-script URLs/English protocol names, expansion and screen-reader pronunciation.
6. **Performance:** measure list rendering, scan update cadence, memory, startup, frame time and battery on representative hardware before optimization claims.
7. **Adaptive layout:** test the actual supported window range, not just phone screenshots.

## 4. Standard versus product judgment

| Topic | Standard/guideline-backed | BDIX product decision still requiring research |
|---|---|---|
| Contrast/targets/semantics | Use applicable Android and WCAG guidance, then test | Which specific screen/control currently fails; no rendered audit was possible |
| Scan feedback | Show meaningful status and accessible state changes | Announcement cadence and whether background continuation is required |
| Localization | Format and expose localized text appropriately | Preferred Bangla technical terms for ISP/server latency |
| Catalog layout | Use lazy/adaptive patterns where suitable | Whether filter chips, advanced filter sheet or dedicated ISP filtering best supports users |
| Error recovery | Give understandable, actionable error feedback | Correct user-facing cause for each server/network failure; test against real outcomes |

