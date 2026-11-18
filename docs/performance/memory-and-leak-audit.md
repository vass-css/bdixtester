# Memory and Leak Audit

## Implemented Resource Control

- Major app services use lazy construction.
- `AppContainer` settings flows stop upstream collection after subscribers detach.
- Scan jobs use `SupervisorJob` and structured coroutine scopes.

## Static Leak Risks Checked

- Application context is used for the application-level container.
- Scan and repository jobs are scoped to application lifetime.
- UI callbacks are passed to Compose and do not retain activity-specific services directly.

## Remaining Measurements

Heap growth, PSS, RSS, retained instances, and LeakCanary results require physical/emulator lifecycle testing across navigation and repeated scan/cancel cycles.
