# Background and Battery Optimization

## Existing Controls

Remote catalog synchronization uses WorkManager. Active scans use application-level coroutine jobs and an interval-throttled progress publisher.

## Implemented Control

The startup catalog and scheduler initialization no longer runs eagerly during application creation; it begins after the main looper becomes idle.

## Remaining Validation

Measure stuck wake locks, background duration, network bytes, and idle power drain on a representative Android device with Android Studio Energy Profiler and Android Vitals.
