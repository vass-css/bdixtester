# Network Engine Optimization

## Existing Engine

The app routes HTTP/HTTPS probes and FTP/FTPS probes through the endpoint probe executor. Scans use bounded coroutines and a host mutex to serialize duplicate-host activity.

## Preserved Behavior

- Network accuracy and result classification are unchanged.
- Scan concurrency remains settings-controlled.
- Duplicate exact URLs are coalesced.
- Cancellation and completion states remain intact.

## Verification Needed

Physical-network benchmarks must measure request latency, timeout rate, failure rate, maximum active connections, and cancellation latency.
