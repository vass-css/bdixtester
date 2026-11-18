# Database & Migration Testing (Phase 4)

- **Test Suite:** `app/src/test/java/com/jubayerhossain/bdixtester/RoomDatabaseMigrationTest.kt`
- **Verified Behaviors:**
  1. KSP exported Room schema `2.json` exists in test assets and parses cleanly with `database.version == 2`.
  2. Explicit non-destructive `MIGRATION_1_2` migrates a v1 SQLite database containing custom servers, favorites, and test history without data loss, adding `isQuickScanInconclusive` and `index_test_history_probeGroupId`.
  3. `RoomCatalogRepository.ensureInitialized()` seeds all `369` supplied `FtpLinks` entries deterministically (`ORDER BY sourceIndex ASC`, `builtin:0..368`), passes programmatic source-to-database comparison, and remains idempotent across repeat startups.
  4. Startup reconciles orphaned `ScanSessionEntity(state = "RUNNING")` records to `INTERRUPTED` without deleting completed history rows.
  5. Custom servers (`custom:<uuid>`), pinned favorites, and test history survive `ServerDao.replaceRemoteCatalogAtomically(...)`.
