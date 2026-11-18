# ADR-002: MVVM with StateFlow and Unidirectional Data Flow

**Status:** Accepted (Phase 2)  
**Date:** 2026-11-18

## Context

Jetpack Compose performs best with immutable state snapshots and lifecycle-aware collection.

## Requirements

- Configuration-change survival.
- Clear separation between UI rendering and business/repository logic.
- Simple unit testing without Android UI instrumentation.

## Alternatives

1. Compose-local mutable state inside composables.
2. MVI with heavy reducer middleware.
3. Lightweight MVVM with `ViewModel` + `StateFlow` + `collectAsStateWithLifecycle()`.

## Selected Approach

Lightweight MVVM exposing immutable `UiState` data classes via `StateFlow` and accepting explicit user actions as ViewModel methods.

## Reason

Follows official Android App Architecture guidance without excessive ceremony.

## Trade-offs

Requires mapping mutable repository flows into immutable UI snapshots.

## Future Consequences

Phase 5–6 live scan progress streams will plug directly into `ScanViewModel` without rewriting Compose screens.
