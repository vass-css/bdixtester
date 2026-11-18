# BDIX Tester — WCAG 2.2 AA Accessibility Contrast Report

## 1. Validation Methodology

Every color pair across all **eight theme configurations** (`4 Presets × Light & Dark`) was programmatically validated using `ThemeSystemContrastAndMigrationTest.allEightThemeConfigurationsMeetWcag22AaContrastRequirements` in accordance with **WCAG 2.2 AA** relative luminance mathematics:

$$L = 0.2126 R_{\text{lin}} + 0.7152 G_{\text{lin}} + 0.0722 B_{\text{lin}}, \quad CR = \frac{L_{\text{lighter}} + 0.05}{L_{\text{darker}} + 0.05}$$

- **Normal Text Threshold (SC 1.4.3):** ≥ `4.5:1`
- **Large Text & Non-Text UI / Borders / Focus / Gauge Threshold (SC 1.4.11):** ≥ `3.0:1`
- **Gradient Worst-Case Rule:** Tested `ctaContent` against `ctaGradientStart`, `ctaGradientEnd`, and 50% linear blend (`lerp`), plus `stopContent` against `stopGradientStart`, `stopGradientEnd`, and 50% blend.

---

## 2. Verified Contrast Ratios Across All Eight Configurations

| Configuration                |   Text / Bg | Primary / Nav Pill | Reachable Badge | Restricted Badge | Unreachable Badge | CTA Gradient Worst-Case | Outline / Bg (UI ≥ 3.0) | Result      |
| ---------------------------- | ----------: | -----------------: | --------------: | ---------------: | ----------------: | ----------------------: | ----------------------: | ----------- |
| **Midnight Indigo (Light)**  | **16.85:1** |         **6.03:1** |      **5.09:1** |       **5.01:1** |        **5.03:1** |              **5.80:1** |              **4.24:1** | **PASS AA** |
| **Midnight Indigo (Dark)**   | **17.18:1** |         **4.79:1** |      **5.93:1** |       **6.49:1** |        **5.42:1** |              **7.22:1** |              **5.29:1** | **PASS AA** |
| **Graphite & Amber (Light)** | **16.49:1** |         **4.91:1** |      **5.04:1** |       **5.51:1** |        **5.09:1** |              **5.03:1** |              **4.76:1** | **PASS AA** |
| **Graphite & Amber (Dark)**  | **16.73:1** |         **5.46:1** |      **5.94:1** |       **6.55:1** |        **5.41:1** |              **6.90:1** |              **4.19:1** | **PASS AA** |
| **Deep Emerald (Light)**     | **16.01:1** |         **5.22:1** |      **5.35:1** |       **4.89:1** |        **4.97:1** |              **5.80:1** |              **4.27:1** | **PASS AA** |
| **Deep Emerald (Dark)**      | **16.60:1** |         **5.48:1** |      **6.27:1** |       **6.49:1** |        **5.42:1** |              **7.49:1** |              **4.60:1** | **PASS AA** |
| **Warm Premium (Light)**     | **15.67:1** |         **7.65:1** |      **5.30:1** |       **5.10:1** |        **5.09:1** |              **7.54:1** |              **4.55:1** | **PASS AA** |
| **Warm Premium (Dark)**      | **16.30:1** |         **5.22:1** |      **5.88:1** |       **6.49:1** |        **5.42:1** |              **5.37:1** |              **4.29:1** | **PASS AA** |

---

## 3. Additional Checked Pairs (All 8 Configurations Verified ≥ Threshold)

- `onSurface` vs `surface`, `surfaceContainerLow`, `surfaceContainer`, `cardBackground`: **≥ 4.5:1**
- `onSurfaceVariant` vs `background`, `surfaceContainer`, `cardBackground`: **≥ 4.5:1**
- `onPrimary` / `primary`, `onPrimaryContainer` / `primaryContainer`: **≥ 4.5:1**
- `onSecondary` / `secondary`, `onSecondaryContainer` / `secondaryContainer`: **≥ 4.5:1**
- `onTertiary` / `tertiary`, `onTertiaryContainer` / `tertiaryContainer`: **≥ 4.5:1**
- `onError` / `error`, `onErrorContainer` / `errorContainer`: **≥ 4.5:1**
- `information` / `informationContainer`, `neutral` / `neutralContainer`: **≥ 4.5:1**
- `onSecondaryCta` / `secondaryCta`, `onFilterChipSelected` / `filterChipSelected`: **≥ 4.5:1**
- `onSnackbar` / `snackbarBackground`, `ipAccent` / `surfaceContainer`, `disabledContent` / `disabled`: **≥ 4.5:1**
- `stopContent` across `stopGradientStart → stopGradientEnd`: **≥ 4.5:1**
- `inputBorder` / `inputBackground`, `ringGradientStart` & `ringGradientEnd` / `background`: **≥ 3.0:1**

## 4. Non-Color Accessibility & Script Legibility

- Status badges always pair color with distinct icons (`CheckCircle`, `Lock`, `ErrorOutline`, `TimerOff`, `HourglassTop`, `Block`, `Cancel`) and localized labels.
- Bangla (`bn`) uses `Noto Sans Bengali` with `letterSpacing = 0.sp` and full `1.0f` text ink opacity on all captions and footers.
- System status bar and navigation bar icon polarity dynamically updates with `resolveUseDarkTheme(themePreference)` in `MainActivity`.
