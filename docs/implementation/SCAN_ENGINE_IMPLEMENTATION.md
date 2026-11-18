# Scan Engine Implementation (Phase 6)

## Scan Coordinator (`ScanCoordinator`)

- **Single-Scan Serialization:** Protected by `Mutex`; overlapping `startScan()` calls return `false` while an active scan job is running.
- **Bounded Concurrency & Per-Host Throttling:** Uses coroutine `Semaphore` bounded to `1..5` (conservative default `3`) and a `ConcurrentHashMap<String, Mutex>` per lowercase host so concurrent requests never overload a single server.
- **Exact-URL Request Coalescing:** Groups target endpoints by exact `originalUrl` (e.g., `369` built-in entries -> `365` unique network probes). Each probe runs once per unique `originalUrl`, generates a shared `probeGroupId`, and writes auditable `TestHistoryEntity` rows for all matching source entries within the scan (`scanRunId`, `isCoalescedReplica = idx > 0`).
- **Session Recovery:** Persists `ScanSessionEntity` (`RUNNING` -> `COMPLETED` / `CANCELLED`). If process death interrupts a scan, `RoomCatalogRepository.ensureInitialized()` reconciles orphaned `RUNNING` rows to `INTERRUPTED` while preserving completed per-server history.
- **Live Telemetry:** Emits `ScanProgressState` containing `totalCatalogEntries`, `uniqueExactUrlCount`, `executedUniqueProbeCount`, `mappedSourceEntryCount`, `activeCount`, `queuedCount`, `reachableCount`, `unreachableCount`, `restrictedOrErrorCount`, `unsupportedCount`, `cancelledCount`, and `elapsedMs`.
