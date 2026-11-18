# BDIX Tester — Old Theme Removal Audit Report

## 1. Objective & Scope

Permanently remove every trace of the previous Cyan-Sky color identity (`NavyDeep`, `CyanPrimaryLight`/`Dark`, `TealSecondaryLight`/`Dark`, `SlateTertiaryLight`/`Dark`, `LightBackground`/`DarkBackground`, `LightSemanticColors`/`DarkSemanticColors`, legacy 3-stop cyan gradients, Material You dynamic color fallback, and template `colors.xml` purple/teal placeholders) across the application codebase, XML resources, previews, and active design specifications.

## 2. Deleted & Replaced Color Artifacts

| Legacy Artifact / Token                                                                                                        | Previous Location                                                                      | Action Taken                                                           |
| ------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| `NavyDeep` (`#041E2D`), `NavySurface` (`#0D1722`)                                                                              | `ui/theme/Color.kt`                                                                    | **Permanently Deleted**                                                |
| `CyanPrimaryLight` (`#03669C`), `CyanPrimaryContainerLight` (`#D6EEFF`)                                                        | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `CyanPrimaryDark` (`#38BDF8`), `CyanPrimaryContainerDark` (`#0C3B57`)                                                          | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `TealSecondaryLight` / `Dark` (`#0369A1`, `#7DD3FC`, `#E0F2FE`, `#10344A`)                                                     | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `SlateTertiaryLight` / `Dark` (`#475A6E`, `#9FB0C4`, `#E1EAF2`, `#26384B`)                                                     | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `LightBackground` (`#F4F7FA`)..`LightOutlineVariant` (`#DCE5EF`)                                                               | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `DarkBackground` (`#081018`)..`DarkOutlineVariant` (`#233649`)                                                                 | `ui/theme/Color.kt`, `ui/theme/Theme.kt`                                               | **Permanently Deleted**                                                |
| `LightSemanticColors` & `DarkSemanticColors` (Cyan/Blue 3-stop CTA/ring `#06B6D4`, `#0284C7`, `#2563EB`, `#22D3EE`, `#3B82F6`) | `ui/theme/Color.kt`                                                                    | **Permanently Deleted**                                                |
| `dynamicColor` (`dynamicDarkColorScheme` / `dynamicLightColorScheme`)                                                          | `ui/theme/Theme.kt`                                                                    | **Permanently Removed**                                                |
| Hardcoded `Color.White` defaults in `HeroCircularRing` comet head & `ShimmerPillOverlay`                                       | `DashboardScreen.kt`, `BdixMotion.kt`                                                  | **Replaced** with `semantic.ringCometHead` & `semantic.shimmerOverlay` |
| `ctaGradientMid` & `ringGradientMid` 3-stop cyan gradients                                                                     | `DashboardScreen.kt`, `SettingsScreen.kt`                                              | **Removed**; replaced with two-tone theme gradients                    |
| Template `purple_200`, `purple_500`, `purple_700`, `teal_200`, `teal_700`                                                      | `res/values/colors.xml`                                                                | **Permanently Deleted** & replaced with Midnight Indigo resources      |
| Old Cyan-Navy `#0B1D2E` adaptive launcher background                                                                           | `res/drawable/ic_launcher_background.xml`                                              | **Replaced** with Midnight Indigo `#0A0E1E`                            |
| Stale Cyan-Sky color tables                                                                                                    | `docs/design/design-system-specification.md`, `docs/design/accessibility-checklist.md` | **Replaced** with references to the 4-theme system                     |

## 3. Repository-Wide Verification Search

Executed `grep -rnE` across the entire `app/` source, resources, and tests for all legacy hex literals (`041E2D`, `0D1722`, `03669C`, `D6EEFF`, `38BDF8`, `0C3B57`, `0369A1`, `E0F2FE`, `7DD3FC`, `10344A`, `475A6E`, `E1EAF2`, `9FB0C4`, `26384B`, `F4F7FA`, `081018`, `050B11`, `12202E`, `182A3C`, `20364C`, `162636`, `334A61`, `233649`, `06B6D4`, `0284C7`, `2563EB`, `035E91`, `105BBC`, `1D4ED8`, `22D3EE`, `3B82F6`, `0B1D2E`) and legacy symbol names (`CyanPrimary`, `NavyDeep`, `NavySurface`, `TealSecondary`, `SlateTertiary`, `LightSemanticColors`, `DarkSemanticColors`, `purple_200`, `teal_200`, etc.).

- **Result in `app/`:** **0 matches** (exit code 1).
- **Fallback Verification:** `BdixThemeRegistry` only recognizes the four approved `AppThemePreset` entries; `SettingsDataStoreRepository.migrateThemePreset(...)` maps any unknown or legacy key to `AppThemePreset.MIDNIGHT_INDIGO`.
