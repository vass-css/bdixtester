## Summary

<!-- Briefly describe what this pull request changes and why. -->

## Type of Change

- [ ] Bug fix (non-breaking fix resolving an issue)
- [ ] New feature or UI enhancement
- [ ] CI/CD, signing, or build tooling update
- [ ] Documentation or localization (`values` / `values-bn`) update

## Checklist

- [ ] Preserves `applicationId = "com.jubayerhossain.bdixtester"` and `metadata.json` capability flags
- [ ] Preserves curated built-in catalog verbatim unless an explicit catalog change was approved
- [ ] Zero hardcoded secrets, passwords, API keys, or `.keystore`/`.jks` files committed
- [ ] Added or updated English & Bangla strings for all user-facing text
- [ ] Local validation checks ran cleanly (`validate-catalog`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:compileReleaseKotlin`)
