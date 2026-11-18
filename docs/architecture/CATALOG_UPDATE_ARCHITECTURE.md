# Catalog Update Architecture — BDIX Tester

**Phase:** 2 (Design Specification — Implementation Scheduled for Phase 8)  
**Date:** 2026-11-18

---

## 1. Four-Tier Catalog Separation

1. **Original Source Catalog (`FtpLinks.kt`):** Immutable, verbatim user-provided URL list compiled into the APK for offline fallback and audit provenance.
2. **Built-in Parsed Catalog (`BuiltInCatalog.kt`):** Idempotently parsed `ServerEndpoint` records derived from `FtpLinks.links`.
3. **Remote Catalog Cache (Phase 8):** Versioned JSON catalog fetched over **HTTPS-only**, validated before replacing the cached remote tier.
4. **User Custom Endpoints (Phase 9):** Stored separately in Room; **never** modified or deleted during remote catalog synchronization.

Favorites (`serverId` pins) are likewise stored independently so catalog refreshes never erase user bookmarks.

---

## 2. Remote Catalog JSON Schema (v1 Draft)

```json
{
  "schemaVersion": 1,
  "catalogVersion": 2026111801,
  "generatedAt": "2026-11-18T08:00:00Z",
  "publishedAt": "2026-11-18T08:05:00Z",
  "minAppVersionCode": 1,
  "servers": [
    {
      "id": "builtin-0001",
      "name": "Example Portal",
      "url": "http://example.local/",
      "protocol": "HTTP",
      "category": "FTP_FILES",
      "isFeatured": false,
      "description": "Optional short note",
      "sourceAttribution": "BDIX Tester Maintainer Catalog",
      "verificationState": "REVIEWED",
      "lastReviewedAt": "2026-11-18T00:00:00Z",
      "speedTestUrl": null
    }
  ]
}
```

_Dynamic live reachability and latency are strictly excluded from the static catalog JSON._

---

## 3. Hosting Options Evaluation (No Production URL Invented)

| Option                                                         | Availability & Caching                                                          | Maintenance & Security                                        | Assessment                                                |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------- | --------------------------------------------------------- |
| **Controlled GitHub Repository + Raw/Pages HTTPS**             | High CDN availability; supports `ETag` and `If-None-Match` (`304 Not Modified`) | Version-controlled PR review, signed commits, zero server ops | **Recommended** for initial open-source/indie maintenance |
| **Dedicated HTTPS API / Object Storage (Cloudflare R2 / CDN)** | Lowest latency, custom cache headers, `gzip`/`brotli`                           | Requires domain & hosting lifecycle management                | Suitable if catalog exceeds static repo needs             |

_No production endpoint is hardcoded or assumed in Phases 1–3._

---

## 4. Safe Update & Low-Data Pipeline (Phase 8)

1. **Conditional HTTP Requests:** Send `If-None-Match` (`ETag`) and `If-Modified-Since` headers over HTTPS-only; `304 Not Modified` consumes < 1 KB.
2. **Schema & Bounds Validation:** Reject responses whose `schemaVersion` is unsupported, `minAppVersionCode > BuildConfig.VERSION_CODE`, or `servers` list is empty/malformed.
3. **Atomic Swap & Last-Known-Good Fallback:** Write validated entries inside a single database transaction (`@Transaction`). If parsing or validation fails, retain the previous valid catalog unchanged.
4. **Background Scheduling:** Optional `WorkManager` `PeriodicWorkRequest` respecting user Wi-Fi-only (`NetworkType.UNMETERED`) and battery-not-low constraints. Never triggers background reachability scans.
