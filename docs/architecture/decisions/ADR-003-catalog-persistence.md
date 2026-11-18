# ADR-003: Catalog Persistence Strategy

**Status:** Accepted (Phase 2 — Persistence Implementation Deferred to Phase 4)  
**Date:** 2026-11-18

## Context

BDIX Tester must maintain a large built-in catalog, remote updates, user favorites, and custom servers without erasing user data during catalog refreshes.

## Requirements

- Offline availability of built-in `FtpLinks` catalog.
- Fast filtering, searching, and sorting across hundreds of servers.
- Strict separation between built-in source, remote catalog, and custom user endpoints.

## Alternatives

1. Storing serialized JSON or DataStore preference keys for all servers.
2. Bundled `BuiltInCatalog` fallback + Room SQLite database (Phase 4) + DataStore Preferences for scalar settings.

## Selected Approach

In Phase 3, `BuiltInCatalog` provides immutable in-memory parsed endpoints from `FtpLinks`. In Phase 4, Room will persist catalog entries, favorites, and custom endpoints, while DataStore Preferences stores settings.

## Reason

Storing large queryable server catalogs in DataStore key-value pairs degrades filtering/sorting performance and risks atomic corruption.

## Trade-offs

Room adds KSP annotation processing in Phase 4 (kept commented out in Phase 3 to minimize build cost).

## Future Consequences

Catalog initialization must use idempotent upsert keys so app restarts never duplicate built-in entries.
