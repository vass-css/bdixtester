# User Data, Export & Safety Features (Phase 9)

## Favorites & Pinning

- `FavoritesViewModel` & `RoomCatalogRepository` persist saved servers and pin states in Room (`favorites` table), surviving remote catalog syncs.
- Pinned endpoints sort ahead of unpinned favorites; searchable by name, URL, or host.

## Custom Servers (`USER_CUSTOM`)

- Users can create, edit, and delete custom endpoints via `CustomServerDialog` (`id = "custom:<uuid>"`).
- Validates `http://`, `https://`, `ftp://`, and `ftps://` schemes and optional HTTP/HTTPS `speedTestUrl`.
- Preserves original input string verbatim without rewriting schemes or ports.

## History, Manual Network Profile & Retention

- Users may configure an optional manual network label in Settings (e.g., `Home ISP`) stamped onto test history rows; no SSID, BSSID, MAC, or location permission is accessed.
- History retention (`7 / 30 / 90 / 365` days) prunes expired rows on startup and upon setting change.
- Users can delete single history entries or clear History, Favorites, Custom Servers, or all local user data behind explicit confirmation dialogs.

## Storage Access Framework Export (`CatalogExportSerializer` + `SafExportHelper`)

- Exports `CATALOG`, `FAVORITES`, `CUSTOM_SERVERS`, `HISTORY`, and `SCAN_SUMMARY` in `CSV`, `JSON`, or `TXT` via Android `ActivityResultContracts.CreateDocument` (zero storage permissions required).
- **CSV Formula Injection Neutralization:** Any CSV cell starting with `=`, `+`, `-`, `@`, `\t`, `\r` (or leading whitespace before `=`, `+`, `-`, `@`) is prefixed with `'` and RFC-4180 quoted.
- **Canonical JSON Export:** Preserves verbatim URLs and schema metadata (`id`, `sourceType`, `sourceIndex`, `scanRunId`, `probeGroupId`, latencies, throughput states) untouched.

## External Browser Intent Restriction (`ExternalBrowserHelper`)

- Strictly permits validated `http://` and `https://` URIs with non-blank hosts. Rejects `ftp://`, `ftps://`, `file://`, and `javascript:` URIs with explanatory user feedback.
