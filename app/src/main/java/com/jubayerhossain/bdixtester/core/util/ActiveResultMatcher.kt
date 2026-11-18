package com.jubayerhossain.bdixtester.core.util

import com.jubayerhossain.bdixtester.core.validation.UrlParser
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult

object ActiveResultMatcher {
  /**
   * Filters latest test results to only those that match an active catalog endpoint
   * with a matching canonical URL, preventing stale history from replaced built-in
   * indices or deleted custom servers from skewing live status counts.
   */
  fun matchActive(
    endpoints: List<ServerEndpoint>,
    results: Map<String, ServerTestResult>,
  ): Map<String, ServerTestResult> {
    if (endpoints.isEmpty() || results.isEmpty()) return emptyMap()
    val matched = LinkedHashMap<String, ServerTestResult>(endpoints.size)
    endpoints.forEach { endpoint ->
      val result = results[endpoint.id] ?: return@forEach
      if (result.urlSnapshot == endpoint.originalUrl ||
        UrlParser.canonicalKey(result.urlSnapshot) == UrlParser.canonicalKey(endpoint.originalUrl)
      ) {
        matched[endpoint.id] = result
      }
    }
    return matched
  }
}
