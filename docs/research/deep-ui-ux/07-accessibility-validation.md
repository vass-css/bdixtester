# Accessibility Validation Audit

**Status:** Source-based review. No physical-device, TalkBack, Switch Access, keyboard, Accessibility Scanner, or rendered contrast test was performed.

## 1. Verified evidence and limitations

| Topic | Source evidence | What is established | What is not established |
|---|---|---|---|
| Compose semantics | `StatusBadge` supplies a hard-coded English content description (`A11Y-001`) | At least one semantic string bypasses localization | Whether it is announced in correct order or duplicates visible text |
| Status distinction | Badge combines text, icon and color | Status is not represented by color alone in that component | Contrast, icon meaning, TalkBack announcement and all status variants |
| Scan announcements | Dashboard progress/current-server semantics use polite live-region behavior (`A11Y-003`) | Semantics declarations exist in source | Actual TalkBack rate, interruption, repeated announcements or user impact (unverified) |
| Resource parity | `values/strings.xml` and `values-bn/strings.xml`: 305 keys each | Same key count and parity in inspected resource sets | Naturalness/accuracy of every translation and dynamic string coverage |
| Large font | Compose/android supports scaling; no app-specific device evidence | Platform guidance exists (`ANDROID-002`) | No proof the app avoids clipping/overlap at large font |
| Contrast | Theme test checks selected configured token pairs ([`ThemeSystemContrastAndMigrationTest.kt:70-185`](../../../app/src/test/java/com/jubayerhossain/bdixtester/ui/theme/ThemeSystemContrastAndMigrationTest.kt#L70-L185)) | Automated checks cover those pairs | No full component matrix, rendered alpha/overlay states, or WCAG conformance |
| Navigation/test | Compose UI tests cover navigation and external-open behavior (`NAV-001`, `UX-002`) | Selected routes/assertions pass in their test environment | TalkBack, physical focus order, touch sizes, switch/keyboard input |
| Bengali reading | Bangla UI render smoke test exists ([`LocalizationAccessibilityUiTest.kt:21-74`](../../../app/src/test/java/com/jubayerhossain/bdixtester/LocalizationAccessibilityUiTest.kt#L21-L74)) | A basic locale render path is covered | Pronunciation, glyph shaping on devices/fonts, long-text layout and mixed-script comprehension |

## 2. Risk register

| Finding | Severity / priority | Evidence and confidence | Needed validation |
|---|---|---|---|
| A11Y-001: English status description bypasses locale | Medium / P2 source-level localization gap; spoken effect untested | `A11Y-001`, `LOC-001` | Localize the semantic description; then TalkBack in both locales to check order, repetition, and pronunciation |
| A11Y-002: touch-target/focus-order coverage absent | Informational / P3 verification gap; **not a confirmed defect** | No rendered/device evidence; Android guidance `ANDROID-001`, `ANDROID-004` | Measure actual clickable bounds; test keyboard, D-pad and Switch Access order |
| A11Y-003: scan progress announcement may be noisy | Informational / P3 research question; **unverified** | `A11Y-003`; no TalkBack run | Perform quick/full scan with TalkBack; record announcement count, timing, interruption and completion |
| LOC-001: mixed-language dynamic label/date paths | Medium / P2 source-level localization gap; user impact unmeasured | `LOC-001`, `A11Y-001`; resource-key parity does not cover hard-coded values | Localize dynamic labels and use locale-aware formats; review terminology, screen reader locale, number/date conventions, Bengali/Latin pronunciation |
| Large text / overflow | Informational / P3 verification gap; no confirmed defect | `ANDROID-002`; no rendered device test | System font scale from default through maximum supported, smallest device width and long URL/server name |
| Color-independent status | Existing badge includes text/icon/color, positive observation | `A11Y-001`; only one component inspected | Audit every state in light/dark and non-color perception simulation |

## 3. WCAG and Android testing mapping

WCAG 2.2 (`WCAG-001`) is a useful reference, but native Android conformance must be judged with applicable Android semantics and device behavior; this report does not claim certification. Android accessibility guidance (`ANDROID-001`, `ANDROID-004`, `ANDROID-006`) supports these checks:

- Every action has meaningful accessible name, role and state.
- Decorative graphics are not announced; status and progress are announced once, at useful intervals.
- TalkBack reading order matches visual/action order; dialogs return focus to their invoker.
- Target area is measured on the rendered screen, including small icon-only controls.
- Text scales without truncating endpoint, result or error meaning.
- Color is never the sole indicator; disabled, selected and error states remain distinguishable.
- Keyboard/D-pad and Switch Access can reach every task and operate filters/actions.
- Motion obeys reduced-animation expectations and does not obscure changing status.

## 4. Test protocol

1. English and Bengali locale, TalkBack on/off.
2. Home → scan start → progress → cancel/completion → result; note announcements and focus.
3. Search, clear query, category/status filters, sort (if exposed), save/pin, external open.
4. True-empty and filtered-empty Favorites/History; delete one/all and restore if available.
5. Custom-server dialog validation, keyboard, error announcement and focus return.
6. Maximum font scale, display size change, long Bengali text, long hostname/URL, light/dark.
7. Keyboard/D-pad, Switch Access, color-vision simulation, touch-target measurement.

Record device/model/API, app version, locale, font scale, exact steps and observed result. A test failure should be linked to a reproducible control/state, not generalized into app-wide noncompliance.

The source-only observations above do not establish TalkBack failure, inadequate touch targets, or general non-conformance. No accessibility redesign is recommended from the available evidence. The specific localized source corrections and evidence status are listed in [14-research-evidence-register.md](./14-research-evidence-register.md).

