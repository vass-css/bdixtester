# ADR-007: Bounded Concurrency and Cancellation

**Status:** Accepted (Phase 2 — Engine Deferred to Phase 5–6)  
**Date:** 2026-11-18

## Context

Testing dozens or hundreds of endpoints concurrently can exhaust file descriptors, trigger OS socket limits, or distort latency metrics due to bandwidth contention.

## Requirements

- Bounded global and per-host concurrency.
- Deterministic cancellation on user Stop or ViewModel clear.
- Per-endpoint error isolation.

## Alternatives

1. Unbounded `async { ... }` fan-out.
2. Sequential 1-by-1 scanning (too slow for large catalogs).
3. `Semaphore`-bounded coroutine workers inside `supervisorScope`.

## Selected Approach

Bounded `Semaphore` worker pool (default 8 for Quick Scan, 6 for Full Scan; max 1 concurrent probe per host) with `Dispatchers.IO`, `supervisorScope`, and single-active-scan `Mutex`.

## Reason

Balances scan speed against measurement accuracy and mobile resource constraints.

## Trade-offs

Large catalogs take proportionally longer than unbounded fan-out, but latency readings remain far more trustworthy.

## Future Consequences

Users can tune maximum concurrency safely within documented bounds in Settings (Phase 6+).
