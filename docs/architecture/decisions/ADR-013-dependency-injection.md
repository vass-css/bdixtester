# ADR-013: Lightweight Constructor Dependency Injection

**Status:** Accepted (Phase 2)  
**Date:** 2026-11-18

## Context

While the reference developer used Dagger 2 / Hilt, BDIX Tester is a single-module application with straightforward dependencies.

## Requirements

- Minimal build times and APK footprint.
- Easy unit/UI testing with fake repositories.

## Alternatives

1. Hilt / Dagger 2.
2. Koin.
3. Manual constructor injection with a typed `AppContainer` in `BdixtesterApplication`.

## Selected Approach

Manual constructor injection via `AppContainer` and standard `ViewModelProvider.Factory` helpers.

## Reason

Eliminates annotation-processor overhead and keeps dependency wiring completely transparent.

## Trade-offs

Adding new repositories requires wiring one parameter in `AppContainer`.

## Future Consequences

Can migrate to Hilt later without touching domain/UI contracts if project complexity ever warrants it.
