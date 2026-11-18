# Database Optimization

## Existing Persistence

Room provides catalog, latest results, favorites, history, scan sessions, and remote catalog metadata persistence.

## Audit

- Startup catalog consistency is checked through an integrity report.
- Catalog initialization is moved to the idle phase and remains on the IO dispatcher.
- History pruning and catalog seeding are not part of first-frame synchronous work.

## Benchmark

The existing unit benchmark covers Room cold seed and warm verification for 752 entries. Physical-disk SQLite timings are still required to confirm release behavior.
