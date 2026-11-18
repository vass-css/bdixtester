# BDIX Tester — WCAG 2.2 AA & Android Accessibility Checklist

## 1. Color Contrast Audit (WCAG 2.2 SC 1.4.3 & SC 1.4.11)

All eight active theme configurations (`Midnight Indigo`, `Graphite & Amber`, `Deep Emerald`, `Warm Premium` × Light & Dark) are programmatically verified against WCAG 2.2 AA in `ThemeSystemContrastAndMigrationTest.kt` and documented in `docs/design/accessibility-contrast-report.md`.

| Check                                                                              | Status                | Verification Method                        |
| ---------------------------------------------------------------------------------- | --------------------- | ------------------------------------------ |
| Normal text on Background, Surface, and Cards (all 8 themes)                       | **PASS AA (≥ 4.5:1)** | Programmatic luminance test                |
| Primary / Secondary / Tertiary / Error pairs (all 8 themes)                        | **PASS AA (≥ 4.5:1)** | Programmatic luminance test                |
| Status Badges (`Reachable`, `Restricted`, `Unreachable`, `Information`, `Neutral`) | **PASS AA (≥ 4.5:1)** | Programmatic luminance test                |
| Two-Tone CTA & Stop Gradient Worst-Case Contrast (Start, Mid, End)                 | **PASS AA (≥ 4.5:1)** | Programmatic luminance test                |
| Outlines, Input Borders & Hero Ring Non-Text UI (all 8 themes)                     | **PASS AA (≥ 3.0:1)** | Programmatic luminance test                |
| System Status & Navigation Bar Icons on Manual Theme/Appearance Switch             | **PASS**              | Dynamic `SystemBarStyle` in `MainActivity` |

---

## 2. Touch Target Size (WCAG 2.2 SC 2.5.8 & Android 48×48dp Guideline)

| Interactive Component                      | File                                                      | Current Size                                              | Status            |
| ------------------------------------------ | --------------------------------------------------------- | --------------------------------------------------------- | ----------------- |
| Home `Start Test` / `Stop` CTA             | `DashboardScreen.kt`                                      | `minHeight = 58.dp`, full width                           | **Verified PASS** |
| Home `VIEW` button                         | `DashboardScreen.kt`                                      | `minHeight = 48.dp` + `minimumInteractiveComponentSize()` | **Verified PASS** |
| Servers Status Tabs                        | `ServersScreen.kt`                                        | `minHeight = 52.dp`                                       | **Verified PASS** |
| Servers BDIX Category Chips                | `ServersScreen.kt`                                        | `minHeight = 48.dp`                                       | **Verified PASS** |
| Servers OTT Region Chips                   | `ServersScreen.kt`                                        | `minHeight = 48.dp`                                       | **Verified PASS** |
| Card URL Footer (`RichCardUrlFooter`)      | `ServersScreen.kt`, `ServerCard.kt`, `FavoritesScreen.kt` | `minHeight = 48.dp`                                       | **Verified PASS** |
| Bookmark / Pin / Delete IconButtons        | All list screens                                          | `minimumInteractiveComponentSize()` (48dp)                | **Verified PASS** |
| Settings Navigation / Switch / Dialog Rows | `SettingsComponents.kt`                                   | `52–64.dp` min height                                     | **Verified PASS** |

---

## 3. Semantics, TalkBack & Screen Reader Support

| Requirement                                          | Current Implementation                                                                                                    | Gap / Action                                                                                                                   |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Meaningful `contentDescription` on interactive icons | Present on TopBar actions, Bookmark, Pin, Delete, Nav items                                                               | Verify new Sort selector, Copy URL action, and Diagnostics sheet close/trigger icons all receive localized EN/BN descriptions. |
| Decorative icons marked `contentDescription = null`  | Consistently applied on category/leading/chevron icons                                                                    | Maintain across all refactored unified cards.                                                                                  |
| Merged semantics on Hero Ring metrics                | `DashboardScreen.kt:994` uses `semantics(mergeDescendants = true)` + `LiveRegionMode.Polite`                              | Keep intact; avoid duplicate announcement on live progress ticks.                                                              |
| Non-color active state cues                          | Paired `Icons.Rounded.*` (selected) + `Icons.Outlined.*` (unselected) + bold label + indicator pill on all 5 destinations | **Verified PASS**                                                                                                              |
| LTR isolation for URLs, IPs, Ping, Timestamps        | `LtrContent { ... }` wraps URLs, IPs, numeric counters                                                                    | Maintain across all unified cards & new Sync timestamp / Scan summary captions.                                                |
| Physical TalkBack audio traversal                    | Headless JVM environment cannot run live TalkBack                                                                         | **Unverified** — Documented explicitly as requiring manual device verification.                                                |

---

## 4. Typography, Bengali Script & Dynamic Font Scaling (100%–200%)

| Requirement                      | Current State                                                                                 | Action                                                            |
| -------------------------------- | --------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| Bengali `letterSpacing = 0.sp`   | Forced to `0.sp` across all `BdixTypographyBengali` styles in `Type.kt`                       | **Verified PASS**                                                 |
| Bengali `labelSmall` font family | Uses `bodyFontFamily` (`NotoSansBengaliFamily`); numeric metrics isolated in `MonoBadgeStyle` | **Verified PASS**                                                 |
| Font weight hierarchy            | Single `FontWeight.Normal` binding allows clean synthetic weight hierarchy in `Type.kt`       | **Verified PASS**                                                 |
| Font scale 100%–200%             | Uses `heightIn(min = ...)` and flexible text layouts                                          | Tested in Robolectric EN & BN (`LocalizationAccessibilityUiTest`) |

---

## 5. Reduced Motion (WCAG 2.2 SC 2.2.2 & SC 2.3.3)

| Requirement                                       | Current State                                                                                | Target State                            |
| ------------------------------------------------- | -------------------------------------------------------------------------------------------- | --------------------------------------- |
| Respect system `ANIMATOR_DURATION_SCALE == 0f`    | Checked safely in `rememberReducedMotionEnabled` (`BdixMotion.kt`)                           | **Verified PASS**                       |
| In-app `Reduce Animations` setting                | DataStore boolean toggle in Settings (`settings_reduce_animations_switch`)                   | **Verified PASS**                       |
| Calm idle state (no continuous decorative motion) | Zero sonar/orbital/radar-spin/shimmer animations while idle or when Reduced Motion is active | **Verified PASS**                       |
| Low-end hardware GPU frame profiling              | Cannot run Perfetto/FrameMetrics on headless JVM                                             | **Unverified** — Documented explicitly. |
