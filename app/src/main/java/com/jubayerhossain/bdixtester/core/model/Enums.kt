package com.jubayerhossain.bdixtester.core.model

enum class ServerProtocol(
  val displayLabel: String,
) {
  HTTP("HTTP"),
  HTTPS("HTTPS"),
  FTP("FTP"),
  FTPS("FTPS"),
  UNSUPPORTED("Unsupported"),
}

enum class ServerCategory(
  val displayName: String,
) {
  ALL("All"),
  UNCATEGORIZED("Uncategorized"),
  FTP_FILES("FTP & Files"),
  MOVIES("Movies"),
  LIVE_TV("Live TV"),
  SOFTWARE("Software"),
  WEB("Web"),
  OTHER("Other"),
}

enum class VerificationState(
  val displayLabel: String,
) {
  UNKNOWN("Unknown"),
  UNVERIFIED("Unverified"),
  COMMUNITY_SUBMITTED("Community Submitted"),
  REVIEWED("Catalog Reviewed"),
  FLAGGED_STALE("Flagged Stale"),
}

enum class CatalogSourceType(
  val displayLabel: String,
) {
  BUILT_IN("Built-in Catalog"),
  REMOTE_CATALOG("Remote Catalog"),
  USER_CUSTOM("Custom Endpoint"),
}

enum class TestStatus(
  val displayLabel: String,
) {
  NOT_TESTED("Not tested"),
  QUEUED("Queued"),
  TESTING("Testing…"),
  REACHABLE("Reachable from current network"),
  RESTRICTED("Responding — Restricted"),
  UNREACHABLE("Unreachable"),
  TIMED_OUT("Timed out"),
  UNSUPPORTED("Unsupported"),
  CANCELLED("Cancelled"),
}

enum class ScanMode(
  val displayLabel: String,
) {
  QUICK_SCAN("Quick Scan"),
  FULL_SCAN("Full Scan"),
}

enum class ScanScope(
  val displayLabel: String,
) {
  ALL_SERVERS("All Servers"),
  SELECTED_CATEGORY("Selected Category"),
  FAVORITES("Favorites Only"),
  CUSTOM_SERVERS("Custom Servers Only"),
  RETRY_FAILED("Retry Unreachable / Timed Out"),
}

enum class ScanSessionState {
  IDLE,
  RUNNING,
  COMPLETED,
  CANCELLED,
  INTERRUPTED,
}

enum class ErrorClassification(
  val displayLabel: String,
) {
  NONE("None"),
  HTTP_REDIRECT("HTTP Redirect"),
  HTTP_AUTH_REQUIRED("HTTP 401 Auth Required"),
  HTTP_FORBIDDEN("HTTP 403 Forbidden"),
  HTTP_NOT_FOUND("HTTP 404 Not Found"),
  HTTP_CLIENT_ERROR("HTTP 4xx Client Error"),
  HTTP_SERVER_ERROR("HTTP 5xx Server Error"),
  DNS_FAILURE("DNS Failure"),
  CONNECTION_REFUSED("Connection Refused"),
  TIMEOUT("Timeout"),
  TLS_FAILURE("TLS Handshake Failure"),
  PROTOCOL_DOWNGRADE_BLOCKED("HTTPS Downgrade Blocked"),
  FTP_PROTOCOL_ERROR("FTP Protocol Error"),
  INVALID_URL("Invalid URL"),
  UNSUPPORTED_SCHEME("Unsupported Scheme"),
  CANCELLED("Cancelled"),
  UNKNOWN_FAILURE("Unknown Failure"),
}

enum class SpeedTestOutcome(
  val displayLabel: String,
) {
  TRANSFER_COMPLETED("Transfer Completed"),
  CAP_REACHED("Transfer Cap Reached"),
  INSUFFICIENT_SAMPLE("Insufficient Sample"),
  TRANSFER_FAILED("Transfer Failed"),
  TRANSFER_CANCELLED("Transfer Cancelled"),
}

enum class SpeedTerminationReason {
  NONE,
  EOF_REACHED,
  BYTE_CAP_REACHED,
  EOF_BEFORE_MIN_SAMPLE,
  USER_CANCELLED,
  UNEXPECTED_CONTENT_ENCODING,
  HTTPS_DOWNGRADE_BLOCKED,
  CROSS_HOST_REDIRECT_DECLINED,
  NETWORK_ERROR,
  HTTP_ERROR,
  TIMEOUT,
}

enum class SyncStatus(
  val displayLabel: String,
) {
  IDLE("Not synced"),
  SYNCING("Syncing…"),
  UPDATED("Catalog updated"),
  NOT_MODIFIED("Up to date (304)"),
  WIFI_REQUIRED("Waiting for Wi-Fi"),
  REJECTED_SIGNATURE("Signature verification failed"),
  REJECTED_ROLLBACK("Rejected older catalog sequence"),
  REJECTED_CONFLICT("Rejected sequence digest conflict"),
  REJECTED_SCHEMA("Incompatible catalog schema or app version"),
  REJECTED_SIZE("Exceeded remote catalog size ceiling"),
  REJECTED_ORIGIN("Cross-origin payload blocked"),
  NETWORK_ERROR("Network error"),
}

enum class AppThemePreset(
  val displayLabel: String,
) {
  MIDNIGHT_INDIGO("Midnight Indigo"),
  GRAPHITE_AMBER("Graphite & Amber"),
  DEEP_EMERALD("Deep Emerald"),
  WARM_PREMIUM("Warm Premium"),
}

enum class ThemePreference(
  val displayLabel: String,
) {
  SYSTEM("Follow System"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class AppLanguage(
  val tag: String?,
  val displayLabel: String,
) {
  SYSTEM(null, "System Default"),
  ENGLISH("en", "English"),
  BANGLA("bn", "বাংলা (Bangla)"),
}
