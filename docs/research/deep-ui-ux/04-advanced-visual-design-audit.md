# Advanced Visual Design and Component Audit

## 1. Scope and confidence

This is a **source-based** design-system audit, not a rendered visual inspection. No current BDIX Tester screenshot was available for inspection in this phase; the 15 pre-existing screenshot deletions remain untouched. Therefore no claim is made about pixel-level spacing, exact contrast ratios of rendered states, visual balance, line wrapping on-device, icon alignment, or actual Bengali font rendering.

## 2. Current system inventory

| Area/component | Source observation | Strength | Gap / audit status |
|---|---|---|---|
| Theme and color | `ui/theme/Color.kt` defines semantic color/theme configuration; light/dark theme support exists | Centralized theme source is preferable to screen-local colors | Token values alone do not prove rendered contrast or color-blind safety. Inspect all semantic state combinations in UI. |
| Typography | `ui/theme/Type.kt` defines app typography configuration | A common type system exists | Typeface, Bengali shaping, scale behavior, line height and overflow need rendered/device verification. |
| Material components | Compose Material 3 is used in the app | Platform-consistent foundation | Specific component states, density and accessibility still need testing. Library adoption does not prove correct usage. |
| Status badge | `StatusBadge.kt` shows a status badge with icon/color/text | More than color alone communicates status | Accessibility description is hard-coded English (`A11Y-001`); icon/state semantics and contrast need validation. |
| Servers screen | Category/status controls, search and server results exist | Supports main discovery facets | ViewModel sort state exists without a screen control (`UX-001`); this is an optional affordance, not a proven user problem. Search hint does not mention ISP (`UX-007`), also optional. |
| Saved/History | Common screen/state infrastructure with filters/search | Existing saved and prior-result tasks are covered | Shared query state and empty-state distinctions require validation (`UX-002`, `UX-003`, `UX-006`). |
| Settings | Theme/language and data-management actions exist | Preferences and sync are discoverable | Multiple sync actions and mislabeled custom-server action (`UX-004`, `UX-005`). |
| Scan controls/progress | Scan screen/state and progress exist; Home and Servers can start scans | Existing scan task is reachable without a dedicated root route | `ScanScreen` itself is not wired as a root destination, but this is not an accessibility or task-blocking defect (`NAV-001`). Rendered progress accessibility remains untested. |
| Navigation | Five top-level destinations: Home, Servers, Favorites, History, Settings | Keeps the established five-destination structure | Preserve; no user evidence justifies replacing it with a drawer or different structure. |
| Dialogs | Custom server dialog and destructive-history confirmation are wired | Confirmation protects bulk clear; custom server input is scoped | Input validation/error/focus behavior not hands-on verified. Single-item delete lacks UI Undo (`UX-008`). |
| Empty/error/loading states | Source branches exist in multiple screens | Basic states are represented | Copy, distinct empty/no-results/offline states and announcement behavior need user/device tests. |

## 3. Visual dimension assessment

### Color and status

Semantic theme configuration and a multi-channel status badge (text + icon + color) are positive foundations. However, the current automated contrast test covers selected token pairs and migration behavior ([`ThemeSystemContrastAndMigrationTest.kt:70-185`](../../../app/src/test/java/com/jubayerhossain/bdixtester/ui/theme/ThemeSystemContrastAndMigrationTest.kt#L70-L185)); it does not exhaustively sample every rendered component/state, disabled text, overlays, focus, selection, or high contrast needs. Run rendered contrast measurement on all relevant light/dark surfaces. Avoid red/green-only encoding; preserve an explicit text/icon status.

### Typography and bilingual layout

The central typography configuration is maintainable, but source inspection cannot establish correct Bangla glyph shaping or the chosen Bengali fallback font on Android. Long host names/URLs, mixed Latin protocol labels, status text, and 200% font scaling need screenshot/device coverage. Keep URLs distinguishable and copyable; do not break or translate protocol syntax.

### Layout, icons, density and motion

No evidence supports a pixel-specific spacing or icon-style defect. Evaluate touch target bounds, baseline alignment, list density and 320dp/large-window layout on rendered UI. Verify motion respects system animation settings and does not add avoidable workload during frequent scan updates.

## 4. Component inventory and validation plan

| Component | Current role | Validate |
|---|---|---|
| Bottom navigation | Five primary destinations | Selected semantics, labels, focus, window-size behavior |
| Search field | Filter server/saved/history results | Label, clear action, keyboard action, no-result feedback |
| Filter chips/tabs | Category and status filtering | Selected state semantics and clear/reset discoverability |
| Server card/row | Server identity, status, measurement and open action | Reading order, target bounds, overflow, measurement label, external-open result |
| Status badge | Status as text/icon/color | Locale, contrast, TalkBack name/state |
| Progress indicator | Scan progress/state | Determinate/indeterminate semantics and announcement cadence |
| Buttons/icon buttons | Scan, sync, save/pin, copy/open, delete | Target size, disabled/loading feedback, destructive confirmation |
| Dialog | Custom server entry / bulk-clear confirmation | Focus trap/return, validation, keyboard, screen reader |
| Snackbars/feedback | Operation feedback if surfaced by action | Duration, action reachability, accessibility announcement |
| Theme/language controls | User preference | Persisted state, locale resources and system app-language behavior |

## 5. Conclusion

The strongest substantiated visual-system observations are structural: theme and typography are centralized, Material 3 is present, and status is not color-only. Specific visual defects are **unverified**, not proven absent. No redesign is justified by this source-only review. Preserve existing screens and visual identity; perform rendered/device checks only where a concrete concern or supported-device requirement calls for them. The evidence-backed content/state corrections are listed in [13-final-prioritized-recommendations.md](./13-final-prioritized-recommendations.md).

