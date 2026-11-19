# Catalog Governance, Restore & CI Verification Guide

## 1. Non-Destructive Governance Design

- **Canonical Source Preservation**:
  - `BundledFtpCatalog.kt`, `BundledFtpCatalogPart2.kt`, `BundledMediaCatalog.kt`, `BdOttPlatforms.kt`, `FtpLinks.kt`, and the JSON files in `public/assets/aistudio/` are **never** mutated or pruned when an endpoint becomes a removal candidate or gets archived.
- **Room v2 → v3 Migration (`MIGRATION_2_3`)**:
  - Adds `endpoint_governance` (keyed by `serverId`, indexed by `lifecycleStatus` and `normalizedDomain`) and `verification_events` (append-only evidence log indexed by `serverId` and `checkedAtEpochMs`).
  - Zero rows are dropped from `servers`, `favorites`, `test_history`, `scan_sessions`, or `catalog_metadata`.
- **Active Catalog Filtering vs. Restoration**:
  - Default active UI queries (`Dashboard`, `Servers`, `ScanCoordinator`) filter out endpoints whose governance status is `ARCHIVED`.
  - User favorites, notes, pins, and past `test_history` rows remain preserved in Room. Restoring an `ARCHIVED` (or `REMOVAL_CANDIDATE`) endpoint immediately returns it to `ACTIVE` / `PENDING_VERIFICATION` visibility with all original URL, category, ISP attribution, and favorite status intact.
- **Strict Two-Stage Confirmation**:
  1. **Mark as Removal Candidate**: Moves entry into `removal_candidate` review queue (reversibly). Does **not** hide the server from the catalog.
  2. **Archive (Confirmation Dialog Required)**: Explicitly prompts the user with server name, URL, and reason before moving to `archived`.
  3. **Restore**: One-tap recovery from **Settings → Catalog Governance & Restore** (and Undo confirmation snackbar).

---

## 2. In-App UI (`Settings → Catalog Governance & Restore`)

- Accessible directly from Settings with live summary badges (Total, Removal Candidates, Archived).
- Displays overview counters across all lifecycle buckets, filter chips (`All`, `Active`, `ISP Restricted`, `Unverified`, `Temp Unavailable`, `Expired Suspected`, `Removal Candidates`, `Archived`), and a background WorkManager verification trigger (`Run Safe Verification`) respecting network/battery constraints and completion notifications.
- Tapping any governance card opens an Evidence & Lifecycle Detail Sheet showing:
  - Domain/endpoint/protocol/category/ISP metadata & confidence badge
  - Plain-language classification explanation & missing evidence list
  - DNS observations, RDAP registration panel (with explicit `.bd` lookup limitation note), HTTP/FTP observations, and Bangladesh ISP probe results
  - Full verification history timeline and reversible action buttons.

---

## 3. Dual-Path GitHub Actions CI/CD Usage

1. **Required Deterministic CI (`.github/workflows/android-ci.yml`)**:
   - Triggers on push/PR to `main`.
   - Validates catalog JSON syntax, schema, canonical deduplication, URL/protocol hygiene, governance integrity, runs Python unit tests (`tools/catalog_verifier`), checks Prettier formatting, and runs Android unit/Robolectric tests, Lint, Debug APK, and Release Kotlin compile.
   - Fails PRs immediately on any deterministic regression; **never** depends on Bangladesh BDIX network reachability.
2. **Optional Live Verification (`.github/workflows/catalog-live-verification.yml`)**:
   - Runs manually via `workflow_dispatch` (with optional `isp_probes_path`, `recheck_hours`, `max_workers`) or weekly schedule.
   - Performs rate-limited public DNS, IANA RDAP bootstrap lookup, public HTTP/FTP checks (skipping RFC1918 private IPs with SSRF protections), and merges external BD ISP probe JSON if provided.
   - Uploads JSON and Markdown audit reports as workflow artifacts (`retention-days: 30`), never auto-deletes catalog entries, and never fails CI when BDIX hosts are unreachable from GitHub runners.

### Inspecting & Rerunning Workflows with GitHub CLI (`gh`)

```bash
# List recent deterministic CI and live audit runs
gh run list --workflow=android-ci.yml --limit 10
gh run list --workflow=catalog-live-verification.yml --limit 10

# Trigger optional live catalog verification manually
gh workflow run catalog-live-verification.yml -f max_workers=8 -f recheck_hours=24

# Inspect logs or download generated audit reports
gh run view <run-id> --log
gh run download <run-id> -n catalog-live-verification-report
```
