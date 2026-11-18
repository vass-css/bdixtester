# Endpoint Catalog Audit — BDIX Tester

**Audit Date:** 2026-11-18  
**Target Kotlin Source:** `app/src/main/java/com/jubayerhossain/bdixtester/data/catalog/FtpLinks.kt`  
**Audit Engine:** `BuiltInCatalog.computeAuditSummary(FtpLinks.links)` and `UrlParser`

---

## 1. Source Verification & Count Discrepancy Report

The user-supplied Dart `FtpLinks.links` array was imported verbatim into `FtpLinks.kt` without adding, removing, reordering, or rewriting any URL.

- **Expected Count (Amendment A / Lock-In Addendum):** `371`
- **Actual Supplied Source Count (`FtpLinks.links.size`):** `369` (indices `0..368`)
- **Discrepancy:** `-2` entries (2 fewer entries in the supplied Dart list than the expected `371`).
- **Policy Enforced:** Per Amendment A (_"If the actual supplied file does not contain exactly 371 entries, stop catalog completion and report the discrepancy rather than modifying the data to force the expected count"_), zero entries were fabricated. All `369` supplied entries are preserved verbatim with deterministic `builtin:<sourceIndex>` IDs (`builtin:0` through `builtin:368`), `ServerCategory.UNCATEGORIZED`, `isFeatured = false`, and `VerificationState.UNKNOWN`.

---

## 2. Exact Computed Audit Metrics

| Audit Metric                      | Measured Count | Notes                                                                      |
| --------------------------------- | -------------: | -------------------------------------------------------------------------- |
| Expected source entry count       |          `371` | Amendment A specification target                                           |
| Actual supplied source entries    |          `369` | Discrepancy = `-2` (`369` vs `371`)                                        |
| Successfully parsed entries       |          `369` | 100% valid HTTP/HTTPS URLs                                                 |
| Unique exact URL strings          |          `365` | Used as scan-time probe coalescing key                                     |
| Exact duplicate rows              |            `4` | Preserved as independent auditable DB rows                                 |
| Invalid / unparseable URLs        |            `0` | Verified by `BuiltInCatalogAuditTest`                                      |
| `http://` endpoints               |          `365` | Tested by `HttpEndpointTester`                                             |
| `https://` endpoints              |            `4` | System TLS trust anchors only                                              |
| `ftp://` endpoints                |            `0` | Supported in `FtpControlTester` for custom/remote entries                  |
| `ftps://` endpoints               |            `0` | Explicitly classified `UNSUPPORTED` without plain-FTP fallback             |
| Unsupported schemes               |            `0` | —                                                                          |
| Hostname endpoints                |          `184` | Display name derived from hostname                                         |
| Public IPv4 endpoints             |          `116` | Non-RFC-1918 IPv4                                                          |
| Private IPv4 endpoints (RFC 1918) |           `69` | Flagged `isPrivateIp = true` (`10/8`, `172.16/12`, `192.168/16`)           |
| Entries with explicit ports       |           `19` | Distinct port variants kept separate (`host:port`)                         |
| Entries with non-root paths       |           `97` | Includes unusual double slash at index `116` (`http://172.27.27.26//2TB4`) |

---

## 3. Exact Duplicate Rows Preserved

| Duplicate URL                 |    First Index (`id`) | Duplicate Index (`id`) | Scan Behavior                                                                                         |
| ----------------------------- | --------------------: | ---------------------: | ----------------------------------------------------------------------------------------------------- |
| `http://10.16.100.202/ftps10` |   `45` (`builtin:45`) |    `81` (`builtin:81`) | Single network probe per scan; 2 history rows via `scanRunId` + `probeGroupId` (`isCoalescedReplica`) |
| `http://10.16.100.206/ftps3`  |   `46` (`builtin:46`) |    `82` (`builtin:82`) | Single network probe per scan; 2 auditable DB records                                                 |
| `http://59.152.101.198`       | `153` (`builtin:153`) |  `280` (`builtin:280`) | Single network probe per scan; 2 auditable DB records                                                 |
| `http://moviehaat.net`        | `154` (`builtin:154`) |  `281` (`builtin:281`) | Single network probe per scan; 2 auditable DB records                                                 |

---

## 4. Programmatic Source-to-Database Integrity Verification

`RoomCatalogRepository.ensureInitialized()` runs `compareSourceToDatabase()` after seeding inside `database.withTransaction`:

1. Verifies `builtInSourceCount == builtInDatabaseCount` (`369 == 369`).
2. Queries built-in rows via `ORDER BY sourceIndex ASC`.
3. Verifies exact index/ID (`builtin:$i`) and verbatim `originalUrl` equality for every row `0..368`.
4. Surfaces any mismatch in `CatalogIntegrityReport` without silently overwriting existing rows.
