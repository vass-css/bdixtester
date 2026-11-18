# Screen-by-Screen UX Analysis

**Evidence basis:** Android source and selected tests; no device interaction. For paths and methods, use the application evidence register in [14-research-evidence-register.md](./14-research-evidence-register.md).

## 1. Navigation and screen inventory

Root `NavHost` wires five destinations: Home, Servers, Favorites, History and Settings. `ScanScreen` exists but is not a root destination; scans can still be started from Home and Servers (`NAV-001`). No full-screen Server Details route was found. Existing server interactions open the endpoint externally; preserve this decision (`UX-002`).

| Screen / state | Purpose and main task | Secondary tasks / actual flow | Strengths | Gaps and edge cases | Recommendation / confidence |
|---|---|---|---|---|---|
| Home Dashboard | Orient the user and start/understand diagnostics | Navigate to five destinations; scan can be started from Home | Established home destination; scan progress state exists in source | Live announcements have not been tested with TalkBack | Existing scan entry is reachable; no separate root route is justified (`NAV-001`, `NOT_A_DEFECT`). |
| Servers | Discover and filter catalog entries | Category, status, text search; save/pin; open endpoint; sync; scan can be started | Main server catalogue has category/status/search and sort state in its ViewModel | No sort affordance in screen; ISP attribution hint omitted; primary latency may use heterogeneous methods | Sort and ISP-hint changes remain optional; do not imply different measurement methods are equivalent (`UX-001`, `UX-007`, `BDIX-001`). |
| Saved/Favorites | Return to saved or pinned servers | Search; open externally; remove/save toggle | Saved task and list state exist | Empty-state copy mentions a details page that is not present; query state is shared with History | Correct stale copy (`UX-002`); shared query is not established as a defect (`UX-006`). |
| History | Revisit scan/test results | Search/filter and delete individual/all | History items and `hasAnyHistory` exist; bulk deletion confirmation | UI uses filtered `historyItems.isEmpty()` for the no-history copy; single delete has no Undo | Separate true-empty/no-match copy (`UX-003`); Undo remains optional (`UX-008`). |
| Settings | Set theme/language and manage catalog/custom endpoint | Manual update/sync actions; open custom-server dialog | Central preference destination; configurable theme and language exist | “New Server Add Request” wording could imply a remote request; sync actions are contextual | Copy refinement is optional (`UX-005`); no evidence establishes harmful sync duplication (`UX-004`). |
| Scan screen | Run scan, view progress/results, control scan | Dedicated screen composable exists; scan can also start from Home and Servers | Scan UI and coordinator exist | Dedicated screen is not wired to root navigation; no hands-on process interruption/notification test; background durability unestablished | Core task is reachable; do not add a route or service absent a product requirement and test evidence (`NAV-001`, `BDIX-003`). |
| Server details | None identified as a separate destination | Card/endpoint opens externally | No duplicate in-app detail screen | Existing product decision is direct browser handoff | Preserve the current handoff; do not add a details page without evidence and authorization. |

## 2. Cross-screen flows and state

### Find and open a server

Servers screen → category/status/search → result/card → external app/browser. A sort state/API exists in the ViewModel but has no corresponding screen control; search includes source attribution while the placeholder omits ISP (`UX-001`, `UX-007`). The outward handoff path is covered by a navigation UI test, but external-app/device availability and failure UX were not exercised (`UX-002` evidence boundary).

### Save and revisit

Servers result → save/pin → Favorites → external open. Root shares query state across Favorites/History (`UX-006`). This is verified state coupling; whether it surprises users is not known. Do not treat it as a defect without user evidence.

### Scan and inspect history

Scan UI/state and history persistence exist, but scan screen is absent from root destinations. History’s filtered-empty rendering does not use `hasAnyHistory` to distinguish no saved result from no matching result. User success depends on discoverable scan access and clear empty-state explanation.

## 3. Screen-state coverage gaps

| State | Existing evidence | Need to validate |
|---|---|---|
| Loading | Scan and data states in source | Visible/announced progress, cancel availability, no indefinite spinner |
| Empty catalogue | Screen has empty handling | Distinguish no catalog, offline/sync error, and active filters |
| Empty Favorites | Resource copy and screen branch | Accurate description; no nonexistent Details route |
| Empty History | `hasAnyHistory` state exists | Distinguish never-run from filtered-to-zero |
| Error / timeout | Probe result statuses exist | Plain language cause and retry guidance; don't equate each failure with server offline |
| Offline/network changed | Network probe infrastructure exists | Recover on network transition; user-visible action and scan consistency |
| Destructive action | Bulk-clear confirmation | Single-row deletion recovery/Undo; focus restoration |
| External open unavailable | Intent logic and test exist | Device without compatible handler, invalid URL and cancellation |

## 4. Existing decisions preserved

This analysis does not recommend a new navigation model, hamburger menu, new details destination, replacement of current theme presets, or removal of direct external open. Scan is reachable through existing screens; no route change is recommended. The source-supported targeted corrections and their priorities are in [13-final-prioritized-recommendations.md](./13-final-prioritized-recommendations.md).

