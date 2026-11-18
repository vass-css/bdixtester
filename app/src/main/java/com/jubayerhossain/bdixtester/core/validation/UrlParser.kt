package com.jubayerhossain.bdixtester.core.validation

import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import java.net.URI

data class ParsedEndpoint(
  val originalUrl: String,
  val isValid: Boolean,
  val protocol: ServerProtocol,
  val host: String?,
  val explicitPort: Int?,
  val isIpv4: Boolean,
  val isPrivateIp: Boolean,
  val hasNonRootPath: Boolean,
  val requiresManualReview: Boolean,
)

object UrlParser {
  private val ipv4Regex = Regex("""^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""")

  fun parse(rawUrl: String): ParsedEndpoint {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty() || trimmed != rawUrl) {
      return ParsedEndpoint(
        originalUrl = rawUrl,
        isValid = false,
        protocol = ServerProtocol.UNSUPPORTED,
        host = null,
        explicitPort = null,
        isIpv4 = false,
        isPrivateIp = false,
        hasNonRootPath = false,
        requiresManualReview = true,
      )
    }

    val uri =
      try {
        URI(rawUrl)
      } catch (_: Exception) {
        return ParsedEndpoint(
          originalUrl = rawUrl,
          isValid = false,
          protocol = ServerProtocol.UNSUPPORTED,
          host = null,
          explicitPort = null,
          isIpv4 = false,
          isPrivateIp = false,
          hasNonRootPath = false,
          requiresManualReview = true,
        )
      }

    val scheme = uri.scheme?.lowercase()
    val protocol =
      when (scheme) {
        "http" -> ServerProtocol.HTTP
        "https" -> ServerProtocol.HTTPS
        "ftp" -> ServerProtocol.FTP
        "ftps" -> ServerProtocol.FTPS
        else -> ServerProtocol.UNSUPPORTED
      }

    val host = uri.host?.takeIf { it.isNotBlank() }
    if (scheme == null || host == null) {
      return ParsedEndpoint(
        originalUrl = rawUrl,
        isValid = false,
        protocol = protocol,
        host = host,
        explicitPort = null,
        isIpv4 = false,
        isPrivateIp = false,
        hasNonRootPath = false,
        requiresManualReview = true,
      )
    }

    val explicitPort = uri.port.takeIf { it > 0 }
    val ipv4Match = ipv4Regex.matchEntire(host)
    val octets = ipv4Match?.groupValues?.drop(1)?.mapNotNull { it.toIntOrNull() }
    val isValidIpv4 = octets != null && octets.size == 4 && octets.all { it in 0..255 }
    val isPrivateIp =
      if (isValidIpv4) {
        isRfc1918PrivateIpv4(octets[0], octets[1])
      } else {
        false
      }

    val path = uri.rawPath.orEmpty()
    val hasNonRootPath = (path.isNotEmpty() && path != "/") || !uri.rawQuery.isNullOrEmpty()
    val requiresManualReview =
      protocol == ServerProtocol.UNSUPPORTED ||
        protocol == ServerProtocol.FTPS ||
        isPrivateIp

    return ParsedEndpoint(
      originalUrl = rawUrl,
      isValid = true,
      protocol = protocol,
      host = host,
      explicitPort = explicitPort,
      isIpv4 = isValidIpv4,
      isPrivateIp = isPrivateIp,
      hasNonRootPath = hasNonRootPath,
      requiresManualReview = requiresManualReview,
    )
  }

  fun isValidIpv4Host(host: String): Boolean {
    val trimmed = host.trim()
    val match = ipv4Regex.matchEntire(trimmed) ?: return false
    val octets = match.groupValues.drop(1).mapNotNull { it.toIntOrNull() }
    return octets.size == 4 && octets.all { it in 0..255 }
  }

  fun hostAndPortLabel(
    host: String,
    explicitPort: Int?,
  ): String = if (explicitPort != null) "$host:$explicitPort" else host

  /**
   * Produces a canonical deduplication key so superficial formatting differences
   * (case, trailing slash, redundant slashes, default port, or leading `www.`) do not create duplicates,
   * while preserving distinct schemes, non-default ports, and paths.
   */
  fun canonicalKey(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""
    val uri =
      try {
        URI(trimmed)
      } catch (_: Exception) {
        return trimmed.lowercase()
      }
    val scheme = uri.scheme?.lowercase() ?: return trimmed.lowercase()
    val host =
      uri.host
        ?.takeIf { it.isNotBlank() }
        ?.lowercase()
        ?.removePrefix("www.")
        ?: return trimmed.lowercase()
    val defaultPort =
      when (scheme) {
        "http" -> 80
        "https" -> 443
        "ftp" -> 21
        "ftps" -> 990
        else -> -1
      }
    val effectivePort = if (uri.port > 0 && uri.port != defaultPort) uri.port else -1
    val normalizedPath =
      uri.rawPath
        .orEmpty()
        .replace(Regex("/+"), "/")
        .trimEnd('/')
    val queryPart =
      uri.rawQuery
        ?.takeIf { it.isNotEmpty() }
        ?.let { "?$it" }
        .orEmpty()
    return buildString {
      append(scheme)
      append("://")
      append(host)
      if (effectivePort > 0) {
        append(':')
        append(effectivePort)
      }
      append(normalizedPath)
      append(queryPart)
    }
  }

  private fun isRfc1918PrivateIpv4(
    first: Int,
    second: Int,
  ): Boolean =
    when {
      first == 10 -> true
      first == 172 && second in 16..31 -> true
      first == 192 && second == 168 -> true
      else -> false
    }
}
