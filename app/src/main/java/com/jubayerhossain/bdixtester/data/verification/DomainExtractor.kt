package com.jubayerhossain.bdixtester.data.verification

import com.jubayerhossain.bdixtester.core.validation.UrlParser
import java.net.IDN

object DomainExtractor {
  private val MULTI_PART_SUFFIXES =
    setOf(
      "com.bd",
      "net.bd",
      "org.bd",
      "edu.bd",
      "gov.bd",
      "ac.bd",
      "mil.bd",
      "info.bd",
      "co.uk",
      "org.uk",
      "com.au",
      "net.au",
      "co.jp",
      "co.in",
    )

  fun isIpLiteral(host: String): Boolean {
    val clean = host.trim().trimStart('[').trimEnd(']')
    return UrlParser.isValidIpv4Host(clean) || clean.contains(':')
  }

  /**
   * Extracts a normalized ASCII/IDNA registrable domain for hostname endpoints,
   * returning `null` for IP literals.
   */
  fun extractNormalizedDomain(host: String): String? {
    val clean =
      host
        .trim()
        .trimEnd('.')
        .lowercase()
    if (clean.isEmpty() || isIpLiteral(clean)) {
      return null
    }
    val asciiHost = runCatching { IDN.toASCII(clean).lowercase() }.getOrDefault(clean)
    val labels = asciiHost.split('.').filter { it.isNotEmpty() }
    if (labels.size < 2) {
      return null
    }
    val tail2 = "${labels[labels.size - 2]}.${labels[labels.size - 1]}"
    return if (tail2 in MULTI_PART_SUFFIXES && labels.size >= 3) {
      "${labels[labels.size - 3]}.$tail2"
    } else {
      tail2
    }
  }

  fun isDotBdDomain(domain: String?): Boolean {
    if (domain.isNullOrBlank()) return false
    val clean = domain.trim().trimEnd('.').lowercase()
    return clean == "bd" || clean.endsWith(".bd")
  }
}
