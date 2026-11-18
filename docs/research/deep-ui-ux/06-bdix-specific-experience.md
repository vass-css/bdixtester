# BDIX-Specific Diagnostic Experience

## 1. Core semantics: do not collapse different measurements

| Concept | Meaning | Evidence in this app | What the interface must not imply |
|---|---|---|---|
| Server availability/reachability | Whether a probe can establish a usable response under a particular protocol/timeout | Protocol-specific probe outcome/status (`BDIX-001`) | A failed probe conclusively proves the server is permanently offline |
| Ping / ICMP RTT | Round-trip time for ICMP echo when that protocol is actually used | No evidence that `primaryLatencyMs` is ICMP ping | Calling all latency fields “ping” |
| HTTP response latency | Time to HTTP response/header using HTTP endpoint tester | Used as one possible primary latency (`BDIX-001`) | Treating it as download throughput or ICMP |
| FTP greeting/connect latency | Time associated with FTP control connection/greeting | Used as one possible primary latency | Comparing it as if measured by the same method as HTTP |
| TCP connect time | Time to establish TCP connection, used as fallback | Used as one possible primary latency | Calling it response time for a complete application transaction |
| Download throughput | Data transfer rate from a download test | Distinct from the primary response-latency field | Inferring throughput from a successful/fast availability probe |

`ServerTestResult.primaryLatencyMs` chooses HTTP response latency, else FTP connect/greeting latency, else TCP connect time (`BDIX-001`). Thus cross-protocol comparisons can combine unlike measurements. The UI labels this value “Latency,” not “Ping”; no user-facing mislabel or demonstrated harm was found (`PARTIALLY_CONFIRMED`, Low severity / P3). If cross-protocol ranking is presented, disclose the basis or compare like-for-like probes; first validate the intended product meaning.

## 2. What status means

The Servers view maps `Restricted`, `Timed out`, `Unsupported`, `Cancelled`, and `Unreachable` into a shared `NOT_WORKING` filter bucket (`BDIX-002`). Individual statuses remain available on result cards. This is a grouping choice, not evidence that result details are hidden or that users confuse the outcomes (`OPTIONAL_IMPROVEMENT`, Low severity / P3). Consider different wording only if comprehension testing identifies a problem.

Prefer explanatory outcome language that is faithful to the test: e.g., “No response before timeout” rather than “server is down,” when only a timeout is known. Do not prescribe exact copy until each protocol failure maps to a verified cause and Bengali technical terminology is reviewed.

## 3. Task and failure analysis

| User task / case | Current evidence | Risk or unanswered question | Research / design response |
|---|---|---|---|
| Find a working BDIX server | Catalog, test results and status filters exist | “Working” can mean reachable under one probe, not general ISP BDIX access or media playback | State which test succeeded and when; distinguish catalog metadata from a live result |
| Find fastest server | Sort logic exists but UI sort affordance is missing | Cross-protocol `primaryLatencyMs` can mix HTTP/FTP/TCP methods | Make latency basis visible and validate like-for-like ordering; no “fastest Internet” claim |
| Identify ISP association | Server `sourceAttribution` is searchable | Search prompt omits ISP; catalog attribution may not be current or verified | Make source attribution and provenance visible; allow correction/report only if product scope permits |
| Timeout or blocked route | `Timed out` / `Restricted` result states exist | Timeout is influenced by device/network/server and does not prove permanent outage | Give neutral cause, retry, and network context where supported |
| Unsupported protocol | Status bucket includes `Unsupported` | A user may interpret as server failure | Separate capability mismatch from unavailable endpoint |
| Failed connection | TCP/HTTP/FTP probes exist | Error detail and recovery not confirmed across all exceptions | Audit each error mapping; provide actionable retry without hiding technical detail |
| Network change during scan | Scan coordinator and network probe exist | Wi-Fi/mobile switch, DNS and route changes not hands-on tested | Test transition mid-scan and determine whether results are tagged to network snapshot |
| Offline | No device-level behavior verified | Cached catalog may be confused with current testability | Test explicit offline state; do not imply catalog data is live |
| Cancel | Scan UI/coordinator implement cancellation path | Cancellation versus timeout/unreachable semantics need verification | Represent user cancellation distinctly; never aggregate it as server failure without explanation |
| Background | Coordinator uses application-process scope; manifest has no scan service/notification declaration | Process scope survives Activity recreation in principle but not process death guarantee; no notification guarantee | Decide required lifecycle. If persistent user-visible background scan is a requirement, evaluate current Android work/FGS rules and notification UX. |
| Retry/history | History repository and saved outcomes exist | History may contain heterogeneous measures/timestamps | Keep method, endpoint and timestamp with result; compare only compatible data |

## 4. ISP attribution and catalog trust

The app's search matching includes `sourceAttribution`, but the placeholder does not advertise ISP search (`UX-007`). This is a possible discoverability improvement, not proof that users fail to find results. ISP-to-server mapping can become stale, vary by route, or depend on the user's actual network. Any future UI should distinguish catalog claim/provenance from the device's measured result, and avoid inferring the current ISP unless reliable implementation evidence exists.

## 5. Recommended result model (design direction, not implementation)

For each probe result, future UX should make these concepts separable:

1. Server identity, protocol and catalog/source attribution.
2. Outcome: reachable / no response / restricted / unsupported / cancelled, with timestamp.
3. Exact measurement name and method (if measured).
4. Latency value and unit only when available; no mislabeled “ping.”
5. Download speed only after the separate throughput test runs.
6. Retry or open action appropriate to the outcome.

The interface should use “fastest responding server for this probe” rather than broad “fastest BDIX” unless the probe and scope are explicit.

