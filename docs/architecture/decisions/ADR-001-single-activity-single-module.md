# ADR-001: Single Activity and Single Gradle Module

**Status:** Accepted (Phase 2)  
**Date:** 2026-11-18

## Context

BDIX Tester is a focused network utility with 5 primary navigation destinations and 1 detail screen. Multi-module Gradle builds add configuration overhead and slower incremental syncs in resource-constrained environments.

## Requirements

- Fast local and CI builds.
- Clean feature separation without unnecessary build complexity.
- Seamless Compose Navigation backstack management.

## Alternatives

1. Multi-module feature architecture (`:core`, `:data`, `:feature:*`).
2. Single `:app` module with package-by-layer/feature organization and one `MainActivity`.

## Selected Approach

Single Activity (`MainActivity`) and single `:app` Gradle module with strict logical package boundaries (`core/`, `data/`, `domain/`, `feature/`, `navigation/`, `ui/`).

## Reason

Reduces Gradle configuration time, avoids cross-module resource/DI boilerplate, and matches the scope of a lightweight Android utility.

## Trade-offs

Compile-time module visibility boundaries are enforced by package discipline and code review rather than Gradle module boundaries.

## Future Consequences

If the app grows significantly in later phases, `:core` or `:network` can be extracted cleanly because domain interfaces are already decoupled.
