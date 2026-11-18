# Testing Strategy — BDIX Tester

**Date:** 2026-11-18

---

## 1. Test Pyramid (Phases 1–3 Baseline)

1. **Pure JVM Unit Tests (`app/src/test/...`):**
   - `UrlParserTest`: Verifies strict scheme-based protocol classification (`http`, `https`, `ftp`, `ftps`, unsupported), port extraction, IPv4 RFC 1918 private-range detection (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), and zero mutation of original URL strings.
   - `BuiltInCatalogAuditTest`: Verifies idempotent catalog parsing, exact duplicate auditing without silent removal of distinct paths/ports, and consistency with `FtpLinks.links`.
2. **Robolectric & Compose UI Tests (`app/src/test/...`):**
   - `ExampleRobolectricTest`: Validates application context, package ID, and `BDIX Tester` string resource.
   - `NavigationUiTest`: Validates adaptive navigation shell (`Home`, `Servers`, `Scan`, `Saved`, `Settings`), empty-state honesty, and accessibility testTags using `createComposeRule()` + `RobolectricTestRunner`.
3. **Static Verification:**
   - Kotlin compilation (`gradle :app:compileDebugKotlin`)
   - Unit + Robolectric suite (`gradle :app:testDebugUnitTest`)
   - Android Lint (`gradle :app:lintDebug`)
   - Debug APK assembly (`gradle :app:assembleDebug`)

## 2. Environment & Future Policy

- No emulator/ADB instrumented tests are executed in this environment.
- Future Phase 5–6 network engine tests will use local `MockWebServer` / loopback fixtures rather than hammering public BDIX servers in CI.
- Physical-device ISP testing will never be claimed unless actually executed on a physical device.
