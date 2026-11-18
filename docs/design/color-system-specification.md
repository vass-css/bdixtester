# BDIX Tester — Complete Semantic Color System Specification

## 1. Architecture Overview

The color system is built around two synchronized contracts per theme configuration (`BdixThemeSpec` in `ui/theme/Color.kt`):

1. **Material 3 Foundation (`ColorScheme`)** — drives standard Material 3 surfaces, typography defaults, dialogs, inputs, dividers, outlines, and navigation controls.
2. **Domain Semantic Tokens (`BdixSemanticColors` via `LocalBdixSemanticColors`)** — drives BDIX-specific status badges, interaction states, two-tone CTA & hero gauge gradients, IP telemetry accents, navigation bars, snackbars, and cards.

All 8 configurations (`AppThemePreset` × `{Light, Dark}`) are resolved deterministically by `BdixThemeRegistry.specFor(preset, isDark)`. Material You `dynamicColor` is permanently disabled.

---

## 2. Token Categories & Semantic Usage

### 2.1 Foundation Tokens (`MaterialTheme.colorScheme`)

| Token                                           | Semantic Purpose                                         | Primary Usage                                                                                              |
| ----------------------------------------------- | -------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `primary` / `onPrimary`                         | Brand/theme focal accent & text/icon on primary surfaces | Active nav icon/label, switches, focused borders, link text, featured star                                 |
| `primaryContainer` / `onPrimaryContainer`       | Tonal highlight container & foreground                   | Category squircle icon badges, selected radio pills, Open ↗ pill                                           |
| `secondary` / `onSecondary`                     | Complementary accent & foreground                        | Secondary controls, subtle highlights                                                                      |
| `secondaryContainer` / `onSecondaryContainer`   | Tonal secondary container                                | IP/Host monospace pills on server cards                                                                    |
| `tertiary` / `onTertiary` / `tertiaryContainer` | Supporting neutral-accent tone                           | Auxiliary metrics & subtle badge contrast                                                                  |
| `background` / `onBackground`                   | Root app canvas & primary body ink                       | Screen Scaffold backgrounds & primary text                                                                 |
| `surface` / `onSurface`                         | Base surface & primary text                              | Top-level surfaces & headings                                                                              |
| `surfaceVariant` / `onSurfaceVariant`           | Muted tonal surface & secondary ink                      | Metadata pills, subtitles, unselected nav icons, footer labels                                             |
| `surfaceContainerLowest`..`Highest`             | 5-step M3 tonal elevation hierarchy                      | Elevated cards (`surfaceContainerLow`), nav/IP strips (`surfaceContainer`), chips (`surfaceContainerHigh`) |
| `outline` / `outlineVariant`                    | Interactive control borders vs hairline dividers         | Outlined buttons/inputs (`outline`), card borders & row dividers (`outlineVariant`)                        |
| `scrim`                                         | Modal backdrop overlay                                   | Dialogs & sheets                                                                                           |

### 2.2 Status Tokens (`BdixSemanticColors`)

| Token Group              | Tokens                                                                           | Semantic Purpose                                                         |
| ------------------------ | -------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| **Reachable / Success**  | `reachable`, `onReachable`, `reachableContainer`, `onReachableContainer`         | Reachable BDIX servers, Wi-Fi/Ethernet live dots, latency chips          |
| **Restricted / Warning** | `restricted`, `onRestricted`, `restrictedContainer`, `onRestrictedContainer`     | HTTP 401/403 restricted, timed-out, mobile data warning, untested status |
| **Unreachable / Error**  | `unreachable`, `onUnreachable`, `unreachableContainer`, `onUnreachableContainer` | Unreachable servers, offline indicator, destructive actions              |
| **Information**          | `information`, `onInformation`, `informationContainer`, `onInformationContainer` | Informational banners, protocol/sync status indicators                   |
| **Neutral**              | `neutral`, `onNeutral`, `neutralContainer`, `onNeutralContainer`                 | Not tested, queued, cancelled, unsupported states                        |

### 2.3 Interaction Tokens (`BdixSemanticColors`)

| Token                          | Semantic Purpose                                                  |
| ------------------------------ | ----------------------------------------------------------------- |
| `hover`                        | Elevated tonal tint for pointer/desktop hover states              |
| `pressed`                      | Tactile pressed-state surface feedback                            |
| `focused`                      | Keyboard/accessibility focus outline accent                       |
| `selected`                     | Selected list item / active chip / active theme card highlight    |
| `disabled` / `disabledContent` | Disabled control container and ≥ 4.5:1 legible disabled label ink |
| `loading`                      | Indeterminate progress indicators & active scan spinner           |
| `active` / `inactive`          | Active toggle/probe state vs dormant indicator                    |

### 2.4 Component Tokens (`BdixSemanticColors`)

| Token                                                                | Component Mapping                                                              |
| -------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `ctaGradientStart`, `ctaGradientEnd`, `ctaContent`                   | Primary two-tone gradient CTA (`Start Test`) & Settings header emblem          |
| `secondaryCta`, `onSecondaryCta`                                     | Tonal secondary buttons & theme preview secondary actions                      |
| `stopGradientStart`, `stopGradientEnd`, `stopContent`                | Active-scan `Stop` destructive two-tone gradient button                        |
| `ringGradientStart`, `ringGradientEnd`, `ringTrack`, `ringCometHead` | Dashboard orbital radar ring, sweep progress arc, track, and comet head        |
| `ipAccent`                                                           | Live IP monospace indicator in Settings profile header                         |
| `cardBackground`, `cardBorder`                                       | Unified Server, OTT, Saved, History, and Settings group cards                  |
| `inputBackground`, `inputBorder`                                     | Search bars and Custom Server / Settings input fields                          |
| `filterChipSelected`, `onFilterChipSelected`, `filterChipUnselected` | Category, status, and OTT region filter chips                                  |
| `navBackground`, `navIndicator`                                      | Bottom `NavigationBar` and wide-screen `NavigationRail`                        |
| `dialogBackground`, `bottomSheetBackground`                          | Modal pickers, Custom Server dialog, Server Diagnostics bottom sheet           |
| `snackbarBackground`, `onSnackbar`                                   | App-wide high-contrast `Snackbar` notifications                                |
| `shimmerOverlay`                                                     | Active-scan CTA shimmer highlight (disabled when `LocalReducedMotion` is true) |
