# Remote Signed Catalog Sync Implementation (Phase 8)

## Cryptographic Verification (`CatalogSignatureVerifier`)

- **Algorithm & Key Registry:** Uses P-256 ECDSA (`SHA256withECDSA`) with X.509 SPKI public keys in `TrustedKeyRegistry` supporting `ACTIVE`, `ROTATING`, and `REVOKED` key states. No private signing key is shipped in the Android codebase.
- **Canonical Raw-Byte Verification:** Decodes bytes using strict UTF-8 (`CodingErrorAction.REPORT`), rejects UTF-8 BOM (`0xEF 0xBB 0xBF` / `\uFEFF`), reads only untrusted `keyId` for lookup, and verifies the detached signature file (`${manifestUrl}.sig`) against the **exact raw downloaded UTF-8 manifest bytes** before any JSON parsing or reserialization.
- **Client Size Ceilings:** Enforces `64 KiB` manifest cap, `512 B` signature cap, and `2 MiB` payload cap while streaming responses (independent of server `Content-Length`).

## Manifest Schema & Anti-Rollback (`RemoteCatalogSyncManager`)

Manifest required fields: `keyId`, `catalogSequence`, `schemaVersion`, `minAppVersionCode`, `payloadUrl`, `payloadSize`, `sha256`, `generatedAt`, `publishedAt`.

- **Anti-Rollback Rules:**
  - `manifest.catalogSequence > storedSequence`: Download payload, verify exact byte count and raw-byte SHA-256, validate schema/URLs, commit atomically.
  - `manifest.catalogSequence < storedSequence`: Reject as `SyncStatus.REJECTED_ROLLBACK`.
  - `manifest.catalogSequence == storedSequence && sha256 == storedSha256`: Reuse existing verified catalog (`NOT_MODIFIED`).
  - `manifest.catalogSequence == storedSequence && sha256 != storedSha256`: Reject as `SyncStatus.REJECTED_CONFLICT`.
- **HTTP 304 Handling:** Conditional `If-None-Match` (`ETag`) and `If-Modified-Since` reuse previously verified data only and never independently advance `catalogSequence`.

## Strict Wi-Fi Enforcement & WorkManager (`CatalogSyncWorker`)

- Uses `StrictWifiValidator` checking active `NET_CAPABILITY_INTERNET` AND confirmed `TRANSPORT_WIFI`. Allows `TRANSPORT_WIFI + TRANSPORT_VPN`, rejects VPN-only ambiguous transports when Wi-Fi-only sync is enabled.
- Evaluates connectivity pre-flight, watches `ConnectivityManager.NetworkCallback` mid-flight to cancel active HTTP calls if Wi-Fi drops, and re-checks before committing to Room.
- Atomic Room swap replaces only `REMOTE_CATALOG` rows and `catalog_metadata`, leaving `BUILT_IN`, `USER_CUSTOM`, `favorites`, and `test_history` untouched.
