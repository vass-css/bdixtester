# Result Classification Specification — BDIX Tester

**Phase:** 2 (Design Specification)  
**Date:** 2026-11-18

---

## 1. Top-Level Status Taxonomy

Every server card and detail view renders status using **Icon + Text Label + Semantic Color** (never color alone):

| `TestStatus`  | User-Facing Label                | Meaning                                                                  | UI Semantics                |
| ------------- | -------------------------------- | ------------------------------------------------------------------------ | --------------------------- |
| `NOT_TESTED`  | `Not tested`                     | No probe executed on current session/catalog                             | Neutral Slate outline icon  |
| `TESTING`     | `Testing…`                       | Probe actively in flight                                                 | Animated/progress indicator |
| `REACHABLE`   | `Reachable from current network` | Valid HTTP `2xx`/`3xx` or FTP `220` greeting received                    | Emerald check-circle        |
| `RESTRICTED`  | `Responding — Restricted`        | Host responded over HTTP (`401`, `403`, `404`, other `4xx`/`5xx`)        | Amber warning/lock icon     |
| `UNREACHABLE` | `Unreachable`                    | DNS lookup failed or TCP connection refused/reset                        | Coral error/off icon        |
| `TIMED_OUT`   | `Timed out`                      | Connect or read timeout expired before response                          | Amber/Coral timer-off icon  |
| `UNSUPPORTED` | `Unsupported`                    | Invalid URL or scheme not supported by active engine (`ftps://`, custom) | Neutral help/block icon     |
| `CANCELLED`   | `Cancelled`                      | User stopped scan before endpoint completed                              | Neutral cancel icon         |

---

## 2. Granular Error Classification (`ErrorClassification`)

| Condition                                | Classification                       | Distinction Rationale                                      |
| ---------------------------------------- | ------------------------------------ | ---------------------------------------------------------- |
| HTTP `200..299`                          | `NONE` (`REACHABLE`)                 | Endpoint reachable and serving content                     |
| HTTP `300..399`                          | `HTTP_REDIRECT` (`REACHABLE`)        | Endpoint reachable; redirect target recorded               |
| HTTP `401`                               | `HTTP_AUTH_REQUIRED` (`RESTRICTED`)  | Host reachable; requires credentials                       |
| HTTP `403`                               | `HTTP_FORBIDDEN` (`RESTRICTED`)      | Host reachable; access denied (distinct from DNS failure!) |
| HTTP `404`                               | `HTTP_NOT_FOUND` (`RESTRICTED`)      | Host reachable; specific path missing                      |
| Other HTTP `4xx`                         | `HTTP_CLIENT_ERROR` (`RESTRICTED`)   | Host reachable; client-side status                         |
| HTTP `5xx`                               | `HTTP_SERVER_ERROR` (`RESTRICTED`)   | Host reachable; upstream server error                      |
| `UnknownHostException`                   | `DNS_FAILURE` (`UNREACHABLE`)        | Hostname failed DNS resolution on current ISP              |
| `ConnectException`                       | `CONNECTION_REFUSED` (`UNREACHABLE`) | Target port actively refused TCP connection                |
| `SocketTimeoutException`                 | `TIMEOUT` (`TIMED_OUT`)              | No response within configured timeout                      |
| `SSLException` / `SSLHandshakeException` | `TLS_FAILURE` (`UNREACHABLE`)        | TLS certificate or handshake failure                       |
| Malformed syntax                         | `INVALID_URL` (`UNSUPPORTED`)        | URL failed parser validation                               |
| Non-HTTP/FTP scheme                      | `UNSUPPORTED_SCHEME` (`UNSUPPORTED`) | Scheme outside engine scope                                |
| `CancellationException`                  | `CANCELLED` (`CANCELLED`)            | Aborted by user action                                     |

---

## 3. Private IPv4 & ISP Context Rule

If `ServerEndpoint.isPrivateIp == true` (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`) and the result is `UNREACHABLE` or `TIMED_OUT`, the detail view explicitly informs the user:

> _"This endpoint uses a private IPv4 address and may only be reachable inside a specific ISP, LAN, or VPN routing domain."_
