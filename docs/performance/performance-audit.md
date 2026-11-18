# Performance Audit

## Scope

Static audit and local benchmark evidence for the BDIX Tester Android application.

## Baseline Findings

1. **Application startup**: startup initialization previously ran synchronously in `BdixtesterApplication.onCreate()`. Room, DataStore, catalog, history pruning, and WorkManager scheduling were reachable from the application critical path.
2. **Dependency construction**: non-critical services were eagerly constructed by `AppContainer`.
3. **Settings flows**: settings-derived `StateFlow` instances used eager sharing.
4. **Catalog processing**: built-in 752-entry catalog loading and audit computation are benchmarked in unit tests.
5. **Scan progress**: scan progress updates are already interval-throttled in `ScanCoordinator`.

## Implemented P0/P1 Improvement

- Deferred startup initialization to the main looper idle state.
- Made major `AppContainer` components lazy.
- Changed settings-derived flows to `WhileSubscribed(5_000)` so inactive screens do not retain upstream collection.

## Verification

- Debug APK assembled successfully.
- Android lint completed successfully.
- Targeted catalog and app-container benchmarks passed.
- Full real-device startup, frame, memory, CPU, GPU, battery, and network measurements remain to be performed on representative devices.
