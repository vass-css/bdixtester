# GitHub Actions Diagnostic Report — BDIX Tester

- **Audit Date:** 2026-11-19
- **Workspace Environment:** AI Studio Cloud Android Container (`/app/applet`)

## 1. Environment & CLI Availability Findings

| Check | Command | Actual Output / Status |
| --- | --- | --- |
| Git Repository Check | `git status` | **ABSENT:** `fatal: not a git repository (or any of the parent directories): .git` |
| Git Remote / Tags Check | `git remote -v` / `git tag -l` | **BLOCKED:** Not a Git repository in this workspace |
| GitHub CLI Check | `gh --version` | **ABSENT:** `sh: 1: gh: not found` |
| Remote Workflow Run History | `gh run list` | **NOT EXECUTABLE:** Requires local workstation with Git remote + `gh` authentication |

Because this container has no `.git` history, no GitHub remote, and no `gh` CLI binary, live GitHub Actions execution telemetry cannot be fetched or re-run automatically from inside the container.

## 2. Static Workflow Architecture Verification

All five workflows under `.github/workflows/` were inspected and validated locally:

| Workflow File | Trigger Scope | Permissions | Key Purpose |
| --- | --- | --- | --- |
| `.github/workflows/android-ci.yml` | `push`/`pull_request` (`main`), `workflow_dispatch` | `contents: read` | Catalog invariants, Python tests, JSON check, `:app:compileDebugKotlin`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:compileReleaseKotlin`, test timing summary |
| `.github/workflows/android-release.yml` | `push` tags `v*`, `workflow_dispatch` (dry-run only) | Top-level `contents: read`; `publish_release` job `contents: write` | Tag-to-`versionName` parity, `android-release` secret check, unit tests + `:app:lintVitalRelease` + `:app:assembleRelease` (APK-only), `apksigner` + `aapt2` verification, `SHA256SUMS.txt` + `release-manifest.json` + `RELEASE-NOTES.md`, protected release creation |
| `.github/workflows/dependency-audit.yml` | Gradle file changes (`push`/`PR`), weekly cron, `workflow_dispatch` | `contents: read` | Exports `releaseRuntimeClasspath` dependencies, blocks unapproved ad/telemetry SDKs, checks for tracked secrets/keystores and `dependenciesInfo.includeInApk = false` |
| `.github/workflows/release-integrity.yml` | Workflow/manifest/release-doc changes (`push`/`PR`), `workflow_dispatch` | `contents: read` | Enforces 40-char SHA pinning across all workflow `uses:`, validates `metadata.json` capability flag, audits `AndroidManifest.xml` permissions, optionally verifies published release checksums |
| `.github/workflows/catalog-live-verification.yml` | Weekly cron, `workflow_dispatch` | `contents: read` | Conservative DNS/RDAP & ISP-probe evidence audit (`tools.catalog_verifier verify-live`) |

## 3. Immutable Action Pinning Verification

All third-party actions across `.github/workflows/*.yml` are pinned to full 40-character commit SHAs:

- `actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1` (`v7.0.1`)
- `actions/setup-java@77dd5b2ce225ea11469928ac317850245a74ae78` (`v6.2.0`)
- `actions/setup-python@ece7cb06caefa5fff74198d8649806c4678c61a1` (`v6.3.0`)
- `actions/setup-node@249970729cb0ef3589644e2896645e5dc5ba9c38` (`v6.5.0`, runs conditionally only when `package-lock.json` exists)
- `actions/upload-artifact@043fb46d1a93c77aae656e7c1c64a875d1fc6a0a` (`v7.0.2`)
- `actions/download-artifact@37930b1c2abaa49bbe596cd826c3c89aef350131` (`v7.0.0`)

## 4. Defect Identified & Fixed in Legacy `android-ci.yml`

- **Previous Root Cause Defect:** Legacy `android-ci.yml` enabled `cache: npm` and ran `npm ci` unconditionally even though no `package-lock.json` exists in the repository root, which would cause immediate runner failure before Gradle validation starts.
- **Fix Applied:** Gated Node/npm formatting steps on `hashFiles('package-lock.json') != ''`, and added deterministic Python JSON and catalog verification steps that execute cleanly with zero Node lockfile dependency.
- **Legacy `android-release.yml` Defect Fixed:** Previous `android-release.yml` built an unrequested AAB bundle (`bundleRelease`), omitted `apksigner` / `aapt2` verification, omitted `SHA256SUMS.txt` and `release-manifest.json`, and granted `contents: write` at top-level instead of scoping write privileges solely to the `publish_release` job.

## 5. Local Maintainer GitHub CLI (`gh`) Monitoring & Verification Commands

Run these commands on your workstation after pushing the repository to GitHub (`gh auth login` first):

```bash
# 1. Inspect configured workflows
gh workflow list

# 2. Trigger Android CI manually and watch progress
gh workflow run android-ci.yml -f suite=full
gh run list --limit 10
gh run watch

# 3. Inspect a failed run's logs
gh run view <RUN_ID> --log-failed

# 4. Run a non-publishing release build & signing dry-run (after setting secrets in android-release environment)
gh workflow run android-release.yml -f dry_run_note="Pre-release dry run"
gh run watch

# 5. Audit release integrity against a published tag
gh workflow run release-integrity.yml -f release_tag=v1.0
```
