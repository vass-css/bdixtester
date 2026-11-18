# BDIX Tester — Design System Specification

**Design Direction:** _Four Curated Visual Themes · Material Design 3_
**Approved Themes:** `Midnight Indigo` _(Default)_ · `Graphite & Amber` · `Deep Emerald` · `Warm Premium`
**Appearances:** Full Light, Dark, and System Default support (8 configurations)  
**Dynamic Color:** Permanently disabled (`false`) to preserve deterministic WCAG 2.2 AA contrast.

---

## 1. Color System & Verified WCAG 2.2 AA Contrast Matrix

The previous single-palette color system has been permanently removed and replaced by four complete Light/Dark theme specifications in `ui/theme/Color.kt` and `ui/theme/Theme.kt`.

- Full semantic token architecture: `docs/design/color-system-specification.md`
- Complete 8-configuration HEX matrix: `docs/design/light-dark-theme-matrix.md`
- Verified WCAG 2.2 AA contrast measurements: `docs/design/accessibility-contrast-report.md`

### 1.1 Alpha Compositing & Non-Color Redundancy Rules

- Never lower body/caption text opacity below `1.0f` when it drops composited contrast below `4.5:1`.
- Every status indicator pairs **semantic color + distinct icon + localized text label** (`CheckCircle` / `Warning` or `Lock` / `Cancel` or `ErrorOutline`).

---

## 2. Typography System (`ui/theme/Type.kt`)

### 2.1 Font Pairing (Unchanged)

- **English UI:** `Plus Jakarta Sans` (`res/font/plus_jakarta_sans*.ttf`)
- **Bengali UI:** `Noto Sans Bengali` (`res/font/noto_sans_bengali*.ttf`)
- **Numeric / URL / IP / Ping:** `JetBrains Mono` (`res/font/jetbrains_mono*.ttf`)

### 2.2 Font Weight Resolution Strategy (Fixes Verified Static-TTF Bug)

- **Audit Finding:** Existing `.ttf` files are static `usWeightClass = 400` (`variable = False`).
- **Implementation Plan:**
  - Verify in Phase 2 whether multi-weight TTFs (`400/500/600/700`) can be installed for the exact same 3 families via `font-util` (without changing families).
  - If separate weights are available, map each `FontWeight` to its true `.ttf` resource.
  - Otherwise, declare only `Font(resId = ..., weight = FontWeight.Normal)` inside each `FontFamily` (removing no-op `FontVariation.Settings`) so Android's font engine renders true visual weight hierarchy via synthetic emboldening instead of forcing 400 Regular everywhere.

### 2.3 Type Scale & Bengali Script Rules

| Style Role             |      Size | Weight   | English LH | Bengali LH | English Tracking |         Bengali Tracking | Font Family                   |
| ---------------------- | --------: | -------- | ---------: | ---------: | ---------------: | -----------------------: | ----------------------------- |
| `MonoHeroNumberStyle`  |   `44.sp` | Bold     |    `52.sp` |    `52.sp` |        `-1.0.sp` | `-1.0.sp` (ASCII digits) | `JetBrainsMono` (`tnum`)      |
| `headlineMedium`       |   `24.sp` | Bold     |    `32.sp` |    `36.sp` |        `-0.2.sp` |               **`0.sp`** | Body Family                   |
| `headlineSmall`        |   `22.sp` | SemiBold |    `28.sp` |    `32.sp` |        `-0.1.sp` |               **`0.sp`** | Body Family                   |
| `titleLarge`           |   `20.sp` | SemiBold |    `26.sp` |    `30.sp` |           `0.sp` |               **`0.sp`** | Body Family                   |
| `titleMedium`          |   `16.sp` | SemiBold |    `22.sp` |    `24.sp` |         `0.1.sp` |               **`0.sp`** | Body Family                   |
| `titleSmall`           |   `14.sp` | SemiBold |    `20.sp` |    `22.sp` |         `0.1.sp` |               **`0.sp`** | Body Family                   |
| `bodyLarge`            |   `15.sp` | Normal   |    `22.sp` |    `24.sp` |           `0.sp` |               **`0.sp`** | Body Family                   |
| `bodyMedium`           |   `14.sp` | Normal   |    `20.sp` |    `22.sp` |           `0.sp` |               **`0.sp`** | Body Family                   |
| `bodySmall`            | `12.5.sp` | Normal   |    `18.sp` |    `20.sp` |           `0.sp` |               **`0.sp`** | Body Family                   |
| `labelLarge`           |   `14.sp` | SemiBold |    `20.sp` |    `22.sp` |         `0.1.sp` |               **`0.sp`** | Body Family                   |
| `labelMedium`          |   `12.sp` | Medium   |    `16.sp` |    `18.sp` |        `0.15.sp` |               **`0.sp`** | Body Family                   |
| `labelSmall`           |   `11.sp` | SemiBold |    `16.sp` |    `17.sp` |        `0.15.sp` |               **`0.sp`** | **Body Family** (NO Mono!)    |
| `MonoUrlStyle`         | `12.5.sp` | Normal   |    `18.sp` |    `18.sp` |           `0.sp` |                   `0.sp` | `JetBrainsMono` (LTR)         |
| `MonoBadgeStyle` (NEW) |   `11.sp` | SemiBold |    `16.sp` |    `16.sp` |           `0.sp` |                   `0.sp` | `JetBrainsMono` (`tnum`, LTR) |

---

## 3. Iconography System (`navigation/BdixNavRoutes.kt`)

| Destination | Selected (`Icons.Rounded.*`) | Unselected (`Icons.Outlined.*`)                         | Dependency Required                |
| ----------- | ---------------------------- | ------------------------------------------------------- | ---------------------------------- |
| Home        | `Icons.Rounded.Home`         | `Icons.Outlined.Home`                                   | Existing `material-icons-extended` |
| Servers     | `Icons.Rounded.Dns`          | `Icons.Outlined.Dns`                                    | Existing `material-icons-extended` |
| Saved       | `Icons.Rounded.Bookmark`     | `Icons.Outlined.BookmarkBorder`                         | Existing `material-icons-extended` |
| History     | `Icons.Rounded.History`      | `Icons.Outlined.Schedule` (or `Icons.Outlined.History`) | Existing `material-icons-extended` |
| Settings    | `Icons.Rounded.Settings`     | `Icons.Outlined.Settings`                               | Existing `material-icons-extended` |

_Note:_ Verify exact visual distinctness between `Icons.Rounded.History` and `Icons.Outlined.History` vs `Icons.Outlined.Schedule`; paired with bold label + tonal pill indicator so active state never relies on color alone.

---

## 4. Spacing, Shape & Elevation Tokens (`ui/theme/Shape.kt`)

| Token Group   | Token Name                                 | Value                          | Usage                                                               |
| ------------- | ------------------------------------------ | ------------------------------ | ------------------------------------------------------------------- |
| **Spacing**   | `BdixSpacing.xs / sm / md / lg / xl / xxl` | `4 / 8 / 12 / 16 / 24 / 32.dp` | Consistent 4/8dp grid across all screens                            |
| **Shapes**    | `BdixCardShapes.chip`                      | `RoundedCornerShape(8.dp)`     | Category/region/latency badges                                      |
|               | `BdixCardShapes.iconSquircle`              | `RoundedCornerShape(12.dp)`    | 44dp card icon badge & 36dp settings icon                           |
|               | `BdixCardShapes.input`                     | `RoundedCornerShape(14.dp)`    | Search fields & modal text inputs                                   |
|               | `BdixCardShapes.richCard`                  | `RoundedCornerShape(20.dp)`    | Unified Servers, OTT, Saved, History & Settings groups              |
|               | `BdixCardShapes.profileHeader`             | `RoundedCornerShape(24.dp)`    | Settings App Profile Header Card                                    |
|               | `BdixCardShapes.pill`                      | `RoundedCornerShape(50)`       | Primary CTA, Open ↗ pill, IP pill                                   |
| **Elevation** | `BdixElevation.flat / bar / card / modal`  | `0 / 2 / 2 / 6.dp`             | Cards always pair `2.dp` elevation + `1.dp` `outlineVariant` border |

---

## 5. Unified Card & Motion Architecture

### 5.1 Shared URL Card Components (`ui/components/RichServerCardComponents.kt`)

- `BdixCategoryIconBadge(icon, contentDescription = null, size = 44.dp)`
- `RichCardUrlFooter(url, urlA11y, testTag, onClick, onLongClick = null)` — `surfaceContainer` strip, `minHeight = 48.dp`, LTR `MonoUrlStyle` URL + `Open ↗` pill.
- Used consistently across: `ServerListCard` (`Servers`), `OttPlatformCard` (`OTT`), `ServerCard` (`Saved`), and `HistoryRowCard` (`History`).

### 5.2 Motion & Reduced Motion Governance (`ui/components/BdixMotion.kt`)

- `LocalReducedMotion = staticCompositionLocalOf { false }` provided at `BdixTesterTheme` / `MainActivity` root.
- Effective value = `settings.reduceAnimations || systemAnimatorDurationScale == 0f`.
- **Keep:** Spring press (`0.96f`/`0.975f`), `220ms` tab crossfade (instant when reduced), `animateItem()`, bookmark/pin spring, active scan progress sweep.
- **Remove/Gate:** Idle sonar waves, idle orbital rotation, idle top-bar radar spin, idle CTA shimmer sweep, idle empty-state bobbing when `reducedMotion == true`, idle settings sync rotation.
