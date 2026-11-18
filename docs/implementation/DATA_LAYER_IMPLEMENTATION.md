# Data Layer Implementation (Phase 4)

## Room Database (`BdixDatabase`, Schema Version 2)

- **Location:** `app/src/main/java/com/jubayerhossain/bdixtester/data/local/BdixDatabase.kt`
- **Exported Schema:** `exportSchema = true` -> KSP output at `app/schemas/com.jubayerhossain.bdixtester.data.local.BdixDatabase/2.json`.
- **Non-Destructive Upgrade Policy:** Explicit `MIGRATION_1_2` alters `test_history` to add `isQuickScanInconclusive` and `index_test_history_probeGroupId`; `fallbackToDestructiveMigration()` is never used.
- **Entities (`BdixEntities.kt`):**
  - `servers`: Stores `BUILT_IN`, `REMOTE_CATALOG`, and `USER_CUSTOM` records. Enforces unique composite index `(sourceType, sourceIndex)` so built-in `0..368` indices cannot collide or collapse duplicates. Built-in queries always sort with `ORDER BY sourceIndex ASC`.
  - `favorites`: Keyed by `serverId` with `isPinned`, `displayOrder`, `addedAtEpochMs`, and optional `note`. Preserved across catalog syncs and schema upgrades.
  - `test_history`: Auto-incrementing `historyId`, `serverId`, `scanRunId`, `probeGroupId`, `isCoalescedReplica`, `serverNameSnapshot`, `urlSnapshot`, `protocol`, `scanMode`, `status`, `errorClassification`, separate stage latency columns (`dnsTimeMs`, `tcpConnectTimeMs`, `tlsHandshakeTimeMs`, `httpResponseLatencyMs`, `ftpGreetingLatencyMs`), `ftpGreetingCode`, `httpStatusCode`, `finalUrl`, speed-test columns (`downloadMbps`, `downloadMibPerSec`, `transferredBytes`, `speedElapsedNanos`, `speedTestState`, `speedTerminationReason`), `networkProfileLabel`, and `isQuickScanInconclusive`.
  - `scan_sessions`: Persists active/completed/cancelled/interrupted scan run state so `ensureInitialized()` marks orphaned `RUNNING` runs as `INTERRUPTED` cleanly on startup.
  - `catalog_metadata`: Tracks remote feed `catalogSequence`, `schemaVersion`, `minAppVersionCode`, `keyId`, `payloadSha256`, `payloadSizeBytes`, `etag`, `lastModified`, and sync timestamps.

## DataStore Preferences (`SettingsDataStoreRepository`)

Stores typed preferences under `bdix_tester_settings`:

- `theme_preference` (`SYSTEM`, `LIGHT`, `DARK`)
- `app_language` (`SYSTEM`, `ENGLISH`, `BANGLA`)
- `full_connect_timeout_ms` (`5000` default, bounded `2000..15000`)
- `full_read_timeout_ms` (`6000` default, bounded `2000..15000`)
- `quick_scan_deadline_ms` (`2500` default, bounded `1500..5000`)
- `max_concurrency` (`3` default, bounded `1..5`)
- `auto_catalog_updates` (`false`), `wifi_only_catalog_updates` (`true`), `allow_cross_origin_remote_payload` (`false`), `remote_catalog_url`
- `speed_test_max_bytes` (`5 MiB` default, capped `1 MiB..20 MiB`)
- `history_retention_days` (`30` default, `1..365`)
- `manual_network_label` (optional user-entered label, never SSID/location)
