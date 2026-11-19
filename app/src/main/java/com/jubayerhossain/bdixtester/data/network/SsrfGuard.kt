package com.jubayerhossain.bdixtester.data.network

import okhttp3.HttpUrl
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

sealed class SsrfCheckResult {
  data object Allowed : SsrfCheckResult()

  data class Blocked(
    val reason: String,
  ) : SsrfCheckResult()
}

object SsrfGuard {
  private val ALLOWED_SCHEMES = setOf("http", "https")
  private val BLOCKED_HOSTNAMES =
    setOf(
      "localhost",
      "localhost.localdomain",
      "metadata.google.internal",
      "metadata",
      "instance-data",
    )

  /**
   * Checks whether an [InetAddress] belongs to non-public / internal / metadata ranges
   * (loopback, RFC1918 site-local, link-local 169.254.0.0/16, CGNAT 100.64.0.0/10,
   * IPv6 ULA fc00::/7, multicast, benchmark 198.18.0.0/15, or unspecified 0.0.0.0/::).
   */
  fun isNonPublicAddress(addr: InetAddress): Boolean {
    if (addr.isAnyLocalAddress ||
      addr.isLoopbackAddress ||
      addr.isLinkLocalAddress ||
      addr.isSiteLocalAddress ||
      addr.isMulticastAddress
    ) {
      return true
    }
    return when (addr) {
      is Inet4Address -> isNonPublicIpv4Bytes(addr.address)
      is Inet6Address -> isNonPublicIpv6Bytes(addr.address)
      else -> false
    }
  }

  /**
   * Parses a literal IP host (if it is an IP literal without performing blocking DNS) and
   * returns true if it falls inside loopback, RFC1918, link-local, metadata, or CGNAT space.
   */
  fun isNonPublicHostLiteral(host: String): Boolean {
    val clean = host.trim().trimStart('[').trimEnd(']').lowercase()
    if (clean in BLOCKED_HOSTNAMES || clean.endsWith(".local") || clean.endsWith(".internal")) {
      return true
    }
    val ipv4 = parseIpv4BytesOrNull(clean)
    if (ipv4 != null) {
      return isNonPublicIpv4Bytes(ipv4)
    }
    if (clean.contains(':')) {
      return runCatching {
        val addr = InetAddress.getByName(clean)
        isNonPublicAddress(addr)
      }.getOrDefault(true)
    }
    return false
  }

  /**
   * Validates an HTTP redirect target against scheme constraints and SSRF boundaries.
   *
   * Legitimate private BDIX endpoints (e.g. `http://10.16.100.244`) or localhost unit tests
   * are allowed to redirect only within the same non-public host/port origin.
   * Public hosts are strictly blocked from redirecting to loopback, RFC1918, link-local,
   * cloud metadata (`169.254.169.254`), or CGNAT targets.
   */
  fun validateRedirectTarget(
    fromUrl: HttpUrl,
    targetUrl: HttpUrl,
  ): SsrfCheckResult {
    val scheme = targetUrl.scheme.lowercase()
    if (scheme !in ALLOWED_SCHEMES) {
      return SsrfCheckResult.Blocked("Blocked redirect to unsupported scheme '$scheme'.")
    }
    if (targetUrl.username.isNotEmpty() || targetUrl.password.isNotEmpty()) {
      return SsrfCheckResult.Blocked("Blocked redirect containing embedded credentials.")
    }

    val originIsNonPublic = isNonPublicHostLiteral(fromUrl.host)
    val targetIsNonPublic = isNonPublicHostLiteral(targetUrl.host)

    if (!originIsNonPublic && targetIsNonPublic) {
      return SsrfCheckResult.Blocked(
        "Blocked SSRF redirect from public host '${fromUrl.host}' to non-public target '${targetUrl.host}'.",
      )
    }
    if (originIsNonPublic && targetIsNonPublic && !targetUrl.host.equals(fromUrl.host, ignoreCase = true)) {
      return SsrfCheckResult.Blocked(
        "Blocked cross-host redirect between private/internal targets ('${fromUrl.host}' -> '${targetUrl.host}').",
      )
    }

    // If target uses a hostname rather than IP literal and origin was public, also validate resolved IPs
    if (!originIsNonPublic && parseIpv4BytesOrNull(targetUrl.host) == null && !targetUrl.host.contains(':')) {
      val resolved =
        runCatching { InetAddress.getAllByName(targetUrl.host).toList() }.getOrElse { emptyList() }
      val unsafeIp = resolved.firstOrNull { isNonPublicAddress(it) }
      if (unsafeIp != null) {
        return SsrfCheckResult.Blocked(
          "Blocked SSRF redirect to '${targetUrl.host}' resolving to non-public address ${unsafeIp.hostAddress}.",
        )
      }
    }
    return SsrfCheckResult.Allowed
  }

  /**
   * Guards external governance HTTP probes so private, loopback, link-local, or unsafe-scheme
   * targets are never dispatched as public HTTP requests.
   */
  fun validatePublicGovernanceTarget(url: HttpUrl): SsrfCheckResult {
    val scheme = url.scheme.lowercase()
    if (scheme !in ALLOWED_SCHEMES) {
      return SsrfCheckResult.Blocked("Unsupported governance probe scheme '$scheme'.")
    }
    if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
      return SsrfCheckResult.Blocked("Blocked URL with embedded userinfo.")
    }
    if (isNonPublicHostLiteral(url.host)) {
      return SsrfCheckResult.Blocked("Host '${url.host}' is non-public/internal and exempt from public probing.")
    }
    return SsrfCheckResult.Allowed
  }

  private fun parseIpv4BytesOrNull(host: String): ByteArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val bytes = ByteArray(4)
    for (i in 0 until 4) {
      val value = parts[i].toIntOrNull() ?: return null
      if (value !in 0..255) return null
      bytes[i] = value.toByte()
    }
    return bytes
  }

  private fun isNonPublicIpv4Bytes(bytes: ByteArray): Boolean {
    if (bytes.size != 4) return true
    val b0 = bytes[0].toInt() and 0xFF
    val b1 = bytes[1].toInt() and 0xFF
    return when {
      b0 == 0 -> true // 0.0.0.0/8
      b0 == 10 -> true // 10.0.0.0/8 RFC1918
      b0 == 100 && b1 in 64..127 -> true // 100.64.0.0/10 CGNAT
      b0 == 127 -> true // 127.0.0.0/8 loopback
      b0 == 169 && b1 == 254 -> true // 169.254.0.0/16 link-local & cloud metadata
      b0 == 172 && b1 in 16..31 -> true // 172.16.0.0/12 RFC1918
      b0 == 192 && b1 == 168 -> true // 192.168.0.0/16 RFC1918
      b0 == 192 && b1 == 0 && (bytes[2].toInt() and 0xFF) == 0 -> true // 192.0.0.0/24
      b0 == 198 && b1 in 18..19 -> true // 198.18.0.0/15 benchmark
      b0 >= 224 -> true // multicast / reserved
      else -> false
    }
  }

  private fun isNonPublicIpv6Bytes(bytes: ByteArray): Boolean {
    if (bytes.size != 16) return true
    val first = bytes[0].toInt() and 0xFF
    // ULA fc00::/7
    if ((first and 0xFE) == 0xFC) return true
    // Link-local fe80::/10
    if (first == 0xFE && ((bytes[1].toInt() and 0xC0) == 0x80)) return true
    return false
  }
}
