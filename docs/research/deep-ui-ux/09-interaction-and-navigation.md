# Interaction and Navigation Audit

## 1. Current navigation facts

`BdixTesterAppRoot.kt` installs five root routes: Home, Servers, Favorites, History and Settings. The `SCAN` route constant and Scan screen exist, but no Scan destination is registered in the root NavHost; scans can nevertheless be started from Home and Servers (`NAV-001`). There is no source evidence for a separate full-screen Server Details route; server handoff is direct to an external browser/application (`UX-002`).

**Decision:** retain the five top-level destinations and existing scan entry points. `NAV-001` is `NOT_A_DEFECT` for task access; there is no evidence to add a root route or separate screen. Do not introduce a hamburger menu or full-screen details page.

## 2. Interaction inventory

| Interaction | Current behavior evidenced | UX/accessibility questions |
|---|---|---|
| Search | Servers and Saved/History have query state; server search includes source attribution | Search scope/clear state; whether query intentionally persists across Saved/History |
| Category/status filters | Servers screen exposes categories/status filters | Selected state semantics, reset, no-match explanation |
| Sorting | ViewModel supports sort choices; no Servers screen control found | Discoverable choice, selected sort label, null/mixed protocol handling |
| Save/pin | Server save/pin state is supported | Distinguish saved vs pinned; confirmation/feedback and TalkBack state |
| External open | Endpoint directly opens via external intent; test covers expected handoff | No compatible app, invalid URL, user cancel and return behavior |
| Scan progress/cancel | Scan UI/coordinator implement progress and cancel-related state | Scan destination access, announcement frequency, completion/failure distinction |
| History delete | Per-entry delete; bulk clear confirmation; restore API exists but no per-row Undo UI | Reversible deletion, clear confirmation count and focus return |
| Theme/language | Preference controls and theme presets are present | Persisted state and Android app-language synchronization on device |
| Catalog sync | Settings has duplicate sync entry points and Servers contextual sync | Make scope, progress, failure and retry consistent; avoid deleting useful shortcut without data |
| Custom server | “New Server Add Request” launches a custom endpoint dialog | Label describes local action; explain validation and persistence before save |

## 3. State transition and feedback requirements

### Scan

Expected conceptual transition: idle → selected scan mode → active/progress → completed/partial/cancelled/failed. Preserve distinct terminal outcomes. A timeout is not cancellation; unsupported is not offline. On cancellation, make it clear which results are retained. The source has scan state/progress but no verified device transition test for every edge.

### Search and filters

Expected interaction: query/filter update → result count/content update → explicit no-match feedback → reset/clear. Test keyboard action, clear button, combined category/status/query filters, and a switch between Saved and History with non-empty query. The current shared query is code fact; whether it is desirable is not known.

### External handoff

Preserve the chosen direct-open behavior. Give understandable failure feedback when no handler exists or endpoint cannot be opened, and preserve the original screen/scroll position on return. Do not interpose an in-app details page absent separate approval.

### Destructive actions

Bulk clear already confirms. Single history deletion is not undoable in UI though restore support exists. Evaluate a snackbar action or confirmation using frequency/impact testing; avoid a confirmation dialog for every low-risk deletion if it adds disproportionate friction.

## 4. Navigation test scenarios

1. From each of the five destinations, reach the main action and return without losing context; verify existing scan starts from Home and Servers.
2. Locate a server through category, status, ISP text and query; identify sort control.
3. Reach scan flow from the existing product entry point and finish/cancel.
4. Save/pin a server, find it in Saved, open externally, return.
5. View previous history; distinguish no history from no filter match; delete and recover.
6. Change language/theme and verify consistent navigation labels and status semantics.
7. Interrupt scan with app backgrounding, network change, Activity recreation and process death; compare behavior to the explicitly defined promise.

## 5. Recommendation boundary

The existing five-destination architecture and scan entry points are retained. The audit identifies a missing sort control, targeted copy/state issues, and unverified error/feedback behavior; it does not establish a blocked scan task. Validate user need before changing component structure or navigation. See [13-final-prioritized-recommendations.md](./13-final-prioritized-recommendations.md) for priorities.

