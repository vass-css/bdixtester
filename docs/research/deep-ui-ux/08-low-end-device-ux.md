# Low-End Android UX and Performance Risk

## 1. Evidence classification

No on-device profiling, startup measurement, frame/jank recording, memory profiling, or battery measurement was performed. Findings below are **source risks** or **validation recommendations**, not measured performance defects.

| Area | Source evidence | Classification | User risk / next step |
|---|---|---|---|
| Large server lists | Server list and ViewModel sorting/filtering are present (`UX-001`); benchmark covers 752 catalog rows on JVM/Robolectric (`PERF-001`) | Existing benchmark result is measured only in its test environment; device scroll/frame performance is not measured | Profile cold/warm list display and scroll jank using a representative catalog and low-end device. |
| Filtering/sorting | Catalog query/filter/sort behavior is source-visible (`UX-001`, `UX-007`) | Source fact; no recomposition or CPU profile | Check allocations and main-thread duration during typing/filter changes; avoid premature memoization without profile. |
| Scan updates | `ScanCoordinator` throttles progress updates to roughly 250 ms (`A11Y-003`) | Source observation; not a performance result | Profile UI update/announcement behavior during Full Scan, including thermal/slow-network conditions. |
| Long-running scan | Work is held in an application-process coroutine scope and no scan service is declared (`BDIX-003`) | Lifecycle/capability risk, not measured speed issue | Decide background contract; measure battery/network traffic only for approved scenarios. |
| Lazy rendering | Compose server list implementation is present (`UX-001`) | Positive source pattern; not proof of stable keys or smoothness | Verify keys/item content and measure on devices; retain bounded UI work per item. |
| Animation/transition | No user-visible animation performance benchmark was available | Unverified | Inventory motion only after rendered inspection; respect system animation scale. |
| Startup/memory/battery | Existing benchmark is JVM/Robolectric, not device startup/memory/battery (`PERF-001`) | Not measured | Do not claim performance benefit or regression without a profile and reproducible device/method. |

## 2. Existing benchmark boundary

The existing benchmark exercises a 752-entry dataset in JVM/Robolectric conditions and records local execution timing. It can detect certain algorithmic regressions under that setup, but it does not establish:

- cold/warm Android app startup;
- Compose first-frame or frame-time distribution;
- scroll jank under GPU/CPU pressure;
- peak memory/GC pauses;
- real network latency or throughput;
- battery usage during scans;
- behavior after Activity recreation or process death.

## 3. Low-end device validation protocol

Use at least one lower-tier supported Android device and one current reference device. Record exact model, API, RAM, build type, catalog size, network, thermal state, locale, font scale, and repetitions. Test:

1. Cold launch and first meaningful content.
2. Server list first render and sustained scroll with full catalog.
3. Rapid search typing, category/status changes and sorting.
4. Quick and Full Scan with mixed fast, slow, timeout and unsupported endpoints in a controlled environment.
5. Cancel, switch apps, return, rotate/recreate if supported, network switch and process recreation.
6. Memory/GC, frame timing, CPU, network traffic and battery in separate runs.

Report p50/p95 frame and task timings, not only averages. No numerical target is asserted here because product/device support targets and baseline measurements have not been established.

## 4. UX recommendations

- Keep scan feedback useful but not excessively chatty; validate both visual update cadence and accessibility announcements.
- Avoid heavy per-row calculations during composition; measure before refactoring.
- Use lazy list primitives and stable item identity where appropriate; verify actual key behavior.
- Limit the scan's concurrency/network pressure according to a user-visible policy; no change is authorized by this report.
- State clearly whether a scan pauses or continues when the app is backgrounded.

