# Startup Optimization

## Change

`BdixtesterApplication.onCreate()` no longer starts catalog and scheduler initialization immediately. It registers a main-looper idle handler; initialization begins after the main queue is idle.

## Lazy Components

Database, settings repository, catalog repository, connectivity observer, HTTP/FTP testers, scan coordinator, speed-test engine, remote sync manager, signature verifier, and WorkManager scheduler are created only when accessed.

## Settings State

Theme, appearance, language, and animation settings now use `WhileSubscribed(5_000)`, avoiding eager settings collection while no observer needs the flow.

## Baseline Evidence

- App container construction benchmark: `4.67 ms`
- 752-entry catalog load benchmark: `20.20 ms`
- 752-entry audit benchmark: `25.29 ms`

These are JVM/Robolectric estimates, not cold-start measurements on physical devices.
