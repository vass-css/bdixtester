# Contributing to BDIX Tester

Thank you for helping improve **BDIX Tester** (`com.jubayerhossain.bdixtester`).

## Project Principles

- **Privacy-First Local Architecture:** Keep diagnostics user-initiated with zero analytics, zero location/Wi-Fi SSID permissions, and no tracking SDKs.
- **Catalog Integrity:** Never silently alter or delete entries in the curated BDIX (`BundledBdixCatalog` / `FtpLinks`) or OTT (`BdOttPlatforms`) datasets to force tests or builds to pass. Report stale/offline endpoints via evidence-based governance reviews instead.
- **Verified Claims Only:** Never commit unverified test results, mock telemetry, API keys, passwords, or `.keystore`/`.jks` files.
- **Package Identity:** Preserve `applicationId = "com.jubayerhossain.bdixtester"`.

## Local Development Setup

- **JDK:** Java 21 (Temurin recommended)
- **Android SDK:** Compile SDK `36` (`minorApiLevel = 1`), Min SDK `24`, Target SDK `36`
- **Python:** 3.11+ (for catalog invariants verifier and test timing scripts)
- **Node.js:** 24+ (for Prettier workflow checks, optional)

### Required Checks Before Opening a Pull Request

```bash
python3 -m tools.catalog_verifier validate-catalog --repo-root .
python3 -m unittest discover -s tools/catalog_verifier/tests -v
./gradlew :app:testDebugUnitTest :app:lintDebug :app:compileReleaseKotlin --console=plain
```

*(In environments where the system Gradle binary is pre-installed without wrapper cache, `gradle` may be used equivalently.)*

## Coding & Resource Standards

- Follow Kotlin official formatting (`.editorconfig`).
- Place user-facing UI strings in both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-bn/strings.xml`.
- Use Compose `LocalResources` or `stringResource(...)` rather than calling `context.getString(...)` directly inside composables.
- Assign deterministic `Modifier.testTag(...)` identifiers to interactive Compose elements and add Robolectric unit/UI tests for behavioral changes.
- Keep `metadata.json` capability fields unchanged.

## Pull Request Process

1. Fork or branch from `main` with a clear, scoped branch name (`fix/...`, `feat/...`, `docs/...`).
2. Complete all items in the Pull Request template.
3. Ensure all CI checks pass with zero suppressed lint or test failures.
4. Keep documentation accurate to actual code and test outputs.
