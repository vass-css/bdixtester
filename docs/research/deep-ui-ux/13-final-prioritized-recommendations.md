# Final Prioritized Recommendations

Priorities below are conservative action sequencing, separate from severity. They reflect source-supported consequence and uncertainty, not measured population frequency. P0/P1 and Critical/High findings are not supported. An optional improvement or verification task is not a confirmed defect.

## 1. Targeted actions supported by current evidence

| Finding | Status | Severity / priority | Action and rationale | Dependencies / regression risks | Verification |
|---|---|---|---|---|---|
| LOC-001 — Dynamic labels and locale-specific History formatting | `CONFIRMED` source-level gap | Medium / P2 | Move user-facing enum/status labels to localized resources and use locale-aware dates/numbers. Resource key parity alone does not cover hard-coded values. | Agree terminology; preserve protocol tokens, units, and search compatibility. Risk: mistranslation or layout overflow. | Resource-backed labels in both locales; UI render and locale/date-format checks; review with Bengali-first users. |
| A11Y-001 — English status semantic description | `CONFIRMED` source-level gap | Medium / P2 | Localize the status description. The code bypasses locale resources; actual TalkBack impact has not been tested. | Coordinate with LOC-001 so visible and spoken terms agree. Risk: duplicate or misleading speech. | Compose semantic assertions plus TalkBack checks in English and Bengali on device. |
| UX-003 — History filtered-empty copy | `CONFIRMED` state/copy mismatch | Low / P2 | Distinguish no stored history from no results after filtering; current empty copy can say no history when unfiltered records exist. | Keep the existing filter/reset behavior. Risk: confusing or inaccessible new copy. | UI test with stored history and zero filtered matches, and a truly empty history case. |
| UX-002 — Saved empty-state reference to details page | `CONFIRMED` content mismatch | Low / P3 | Correct only the stale empty-state wording; direct external opening and current layout remain. | Keep English and Bengali copy aligned. Risk: copy could imply an unsupported action. | Resource assertions and existing navigation behavior test. |

These are documentation findings and proposed future implementation changes; no application code was changed as part of this research task.

## 2. Optional improvements — validate demand first

| Finding | Status | Severity / priority | Optional action | Evidence needed before implementation |
|---|---|---|---|---|
| UX-001 — Sort state not exposed in Servers screen | `PARTIALLY_CONFIRMED` | Low / P3 | Consider a sort affordance only if users need ordering beyond current list behavior. | Task study of server discovery; define valid comparisons when latency sources differ. |
| UX-005 — English custom-server action phrasing | `PARTIALLY_CONFIRMED` | Low / P3 | Consider clearer local-entry wording; current action opens a custom endpoint dialog. | English and Bengali comprehension review; confirm persistence semantics. |
| UX-007 — ISP attribution search hint | `OPTIONAL_IMPROVEMENT` | Low / P3 | Consider naming ISP/source attribution in existing search guidance. | Search task evidence that users overlook attribution; verify catalog metadata quality. |
| UX-008 — Undo for single History deletion | `OPTIONAL_IMPROVEMENT` | Low / P3 | Consider using the existing restore capability if accidental deletion is a meaningful concern. | Observe deletion/recovery needs; define undo duration and lifecycle. |
| BDIX-002 — Broad “Not Working” filter label | `OPTIONAL_IMPROVEMENT` | Low / P3 | Consider a clearer grouping label only if users mistake the bucket for a diagnosis. Individual statuses remain on cards. | Comprehension test across timeout, restricted, unsupported, cancelled, and unreachable cases. |
| PERF-001 — Device performance profiling | `OPTIONAL_IMPROVEMENT` | Informational / P3 | Profile representative devices if users report jank or a supported device is slow; current benchmark is JVM/Robolectric, not device performance. | Real-device baseline and repeatable profiling protocol. |

## 3. No defect established / verify only if required

| Finding | Status | Severity / priority | Decision |
|---|---|---|---|
| NAV-001 — Dedicated ScanScreen not a root destination | `NOT_A_DEFECT` | Informational / P3 | Scans can start from Home and Servers. Do not add a route/tab absent evidence that the existing scan flow blocks users. |
| BDIX-001 — `primaryLatencyMs` uses different protocol timings | `PARTIALLY_CONFIRMED` | Low / P3 | It is not a demonstrated “ping” mislabel: the UI says “Latency.” Explain comparability only if cross-protocol ranking is presented. |
| BDIX-003 — Background-scan durability | `PARTIALLY_CONFIRMED` | Informational / P3 | Process-scoped work and no declared scan service are source facts; no product promise or device failure is established. Decide only if durable background scanning is a requirement. |
| UX-004 — Multiple catalog sync entry points | `NOT_A_DEFECT` | Informational / P3 | Preserve contextual entry points; there is no evidence of harmful duplication. |
| UX-006 — Saved and History share query state | `NOT_A_DEFECT` | Informational / P3 | Treat as existing behavior. Change only if user preference/testing supports per-screen state. |
| A11Y-002 — Device accessibility coverage | `UNVERIFIED` | Informational / P3 | Run TalkBack, Switch Access, touch-bound, font-scale, keyboard, and rendered contrast checks before claiming a failure or conformance. |
| A11Y-003 — Scan live-region cadence | `UNVERIFIED` | Informational / P3 | Test actual TalkBack announcement rate before changing semantics or progress cadence. |
| UX-009 — Existing testing-strategy navigation description | `CONFIRMED` documentation inconsistency | Informational / P3 | Existing document is outside the authorized research directory; update only with separate authorization. It is not an application defect. |

## 4. Severity and priority totals

The register contains 18 findings: **Critical 0, High 0, Medium 2, Low 8, Informational 8**. Priority totals: **P0 0, P1 0, P2 3, P3 15**. The two Medium/P2 findings are source-level localization gaps; the third P2 is the inaccurate filtered-History empty state. A recommendation's presence in a table does not mean an implementation change is required.

See the complete source evidence, exact line references, confidence, external references, and limitations in [14-research-evidence-register.md](./14-research-evidence-register.md). The design-preservation decision is in [12-design-direction-decision.md](./12-design-direction-decision.md).
