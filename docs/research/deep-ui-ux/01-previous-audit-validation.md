# আগের UI/UX Audit-এর স্বাধীন যাচাই

**পর্যালোচিত নথি:** আগের নয়টি report।  
**Method:** Current Kotlin/Compose/resources/tests ও live public sources; আগের report নিজে evidence নয়।  
**Finding ID:** নতুন ID canonical; পুরোনো `BDX-01`…`BDX-14` কেবল trace key।

## 1. Finding traceability

| নতুন ID | আগের finding | Current source evidence | Outcome | Priority পুনর্মূল্যায়ন | Final decision |
|---|---|---|---|---|---|
| NAV-001 | BDX-01 Dedicated ScanScreen root route নেই | `ScanScreen` root destination নয়; Dashboard ও Servers callback দিয়ে scan শুরু করে | `NOT_A_DEFECT` | Informational / P3 | Withdraw “core scan task inaccessible” and P1. A separate progress screen is not shown to be needed. |
| BDIX-003 | BDX-02 Background scan/notification নেই | `ScanCoordinator` process-scoped coroutine; manifest-এ scan service নেই | `PARTIALLY_CONFIRMED` | Informational / P3 | Background durability is neither a verified user promise nor a tested runtime defect; decide only if requirement exists. |
| UX-001 | BDX-03 Fastest sort UI নেই | `ServersViewModel` sort state/logic আছে; `ServersScreen`-এ sort control নেই | `PARTIALLY_CONFIRMED` | Low / P3 | Control gap is source-confirmed; user need and confusion are not. Optional unless task research supports it. |
| LOC-001 / A11Y-001 | BDX-04 Bengali dynamic labels/semantics | 305/305 resource keys match; enum labels and StatusBadge semantics remain English; History date/number use `Locale.US` | `CONFIRMED` source-level localization gap | Medium / P2 | Resource-key parity is not localization completeness. User impact and TalkBack speech require device/user validation. |
| BDIX-002 | BDX-05 “Not Working” aggregates states | Several outcomes share a filter bucket; cards retain the specific status | `OPTIONAL_IMPROVEMENT` | Low / P3 | No evidence that result details are hidden or users misinterpret this filter. |
| UX-003 | BDX-06 History filtered-empty matches true-empty | `hasAnyHistory` reflects raw history; screen checks filtered `historyItems.isEmpty()` | `CONFIRMED` | Low / P2 | The empty-state copy can be factually wrong when filters yield no match; a targeted state/copy correction is justified. |
| UX-006 | BDX-07 Saved/History share query | Root uses the same ViewModel callback/state | `NOT_A_DEFECT` | Informational / P3 | Shared query is observable behavior; no evidence establishes unwanted surprise. Preserve unless users request different behavior. |
| UX-004 | BDX-08 multiple catalog sync entry points | Settings offers a quick action and a catalog-settings action | `NOT_A_DEFECT` | Informational / P3 | They are contextual entry points; no evidence shows harmful duplication. |
| UX-005 | BDX-09 “New Server Add Request” wording | English resource label opens the local custom endpoint dialog; Bangla label says “নতুন সার্ভার যোগ করুন” | `PARTIALLY_CONFIRMED` | Low / P3 | English wording may imply a request submission; no comprehension evidence. Optional wording refinement. |
| UX-007 | BDX-10 ISP search discoverability | Search corpus includes `sourceAttribution`; hint does not name ISP | `OPTIONAL_IMPROVEMENT` | Low / P3 | No evidence users fail to find ISP-associated records; do not add a filter without validated need. |
| UX-002 | BDX-11 Saved empty state mentions details screen | Resource says “detail screen”; UI test confirms direct external open and no details destination | `CONFIRMED` | Low / P3 | Correct stale copy only; preserve direct-open behavior and layout. |
| UX-008 | BDX-12 single History delete has no Undo | UI deletes one row directly; repository offers restore, but screen has no Undo | `OPTIONAL_IMPROVEMENT` | Low / P3 | Accidental deletion frequency/impact is unknown; no mandatory Undo requirement established. |
| A11Y-003 | BDX-13 live-region speech flood | Polite live-region semantics are present; no TalkBack run | `UNVERIFIED` | Informational / P3 | Announcement rate or harm is not established. |
| UX-009 | BDX-14 documentation drift | Existing `TESTING_STRATEGY.md` names Scan as a nav destination; root route list does not | `CONFIRMED` documentation inconsistency | Informational / P3 | Existing document is outside this phase’s authorized edit scope; no claim that application behavior is defective. |

## 2. Previous report quality and conflicts

1. Earlier report IDs were consistently `BDX-01`…`BDX-14` but did not follow the newly required prefix/sequence format. This package maps each to one canonical new ID above; repeated cross-category use is explicit.
2. Earlier report described Bengali localization broadly as incomplete. Current inspection found **305 default string keys and 305 Bengali keys with exact key parity**. The valid source-level gap is not missing keys: English enum/status labels and semantics bypass localized resources, while History uses `Locale.US`. Device/user impact was not measured.
3. Earlier background finding risked implying app-backgrounding necessarily kills the scan. The source-owned coroutine scope is independent of Activity scope, but still process-bound and not a durable background-work guarantee. Actual lifecycle outcomes were not tested.
4. Earlier severity for TalkBack announcement flooding is unsupported by device evidence. It is now a test question, not a confirmed issue.
5. `Latency` findings need protocol-aware qualification: `primaryLatencyMs` can be HTTP response, FTP connection/greeting, or TCP connection time. The UI labels it “Latency,” not ICMP ping. Mixed measurement basis is a technical caveat, not evidence of a current mislabeled ping defect.
6. Earlier “source change” narrative documented in `complete-ux-audit.md` is historical provenance. The initial snapshot for the final-verification task is recorded in [14-research-evidence-register.md](./14-research-evidence-register.md); this report neither attributes nor alters its pre-existing dirty-tree changes.

## 3. Prior reports examined

All nine were read: `world-class-ui-ux-research.md`, `complete-ux-audit.md`, `visual-design-audit.md`, `design-system-recommendations.md`, `screen-redesign-specification.md`, `accessibility-and-inclusive-design-audit.md`, `usability-testing-strategy.md`, `priority-improvement-roadmap.md`, and `final-design-direction.md`.

The screen specification’s labeled historical draft remains historical. This phase did not remove, reorder, rewrite, or overwrite any of those nine documents.

## 4. Traceability constraints

- **Keep:** five top-level destinations; no hamburger menu; no full-screen Server Details page; server card/URL direct external-browser behavior; curated theme presets; Home mode-setting decision.
- **Do not infer:** a feature is product behavior because an old report or candidate checklist names it.
- **Do not overstate:** source absence is not a measured runtime failure; public Play description is not an implemented workflow; accessibility source code is not TalkBack conformance.
- Updated classifications and consolidated actions are in [13-final-prioritized-recommendations.md](./13-final-prioritized-recommendations.md) and [14-research-evidence-register.md](./14-research-evidence-register.md).

