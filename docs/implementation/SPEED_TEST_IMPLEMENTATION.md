# Speed Test Implementation (Phase 7)

## Dedicated Opt-In Throughput Tester (`SpeedTestEngine`)

- **Strict Separation:** Never executes automatically during Quick Scan or Full Scan. Runs solely when a user explicitly initiates a speed test from `ServerDetailsScreen` using a configured `speedTestUrl` or an explicit user-provided HTTP/HTTPS resource URL.
- **Uncompressed Transfer Fidelity:** Sends `Accept-Encoding: identity` and rejects unexpected `Content-Encoding` (such as `gzip`/`br`/`deflate`) so HTTP compression does not inflate throughput numbers.
- **Fixed 32 KiB Buffer Loop & Monotonic Clock:** Uses `System.nanoTime()` and a fixed `32 * 1024` byte read loop that stops immediately upon reaching `maxBytes` (without reading byte `maxBytes + 1` to probe EOF).
- **Five Distinct Outcome States (`SpeedTestOutcome`):**
  1. `TRANSFER_COMPLETED`: Stream hit EOF cleanly above minimum sample thresholds (`>= 64 KiB` and `>= 100 ms`).
  2. `CAP_REACHED`: Transfer stopped cleanly at the configured byte cap (`2 / 5 / 10 / 20 MiB`) above sample thresholds.
  3. `INSUFFICIENT_SAMPLE`: Stream ended or hit byte cap too quickly for reliable Mbps calculation (retaining `BYTE_CAP_REACHED` vs `EOF_BEFORE_MIN_SAMPLE` in `speedTerminationReason` and keeping `downloadMbps = null`).
  4. `TRANSFER_FAILED`: HTTP error, timeout, unexpected `Content-Encoding`, HTTPS->HTTP downgrade block, or I/O failure.
  5. `TRANSFER_CANCELLED`: User cancelled transfer cleanly.
- **Redirect Safety:** Blocks HTTPS->HTTP downgrade redirects and prompts explicit user confirmation (`CrossHostRedirectRequiresConfirmation` dialog) before following redirects to a different host.
