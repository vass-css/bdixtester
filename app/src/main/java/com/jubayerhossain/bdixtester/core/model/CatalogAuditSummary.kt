package com.jubayerhossain.bdixtester.core.model

data class CatalogAuditSummary(
  val expectedSourceCount: Int = 371,
  val originalEndpointCount: Int = 0,
  val discrepancyCount: Int = 0,
  val successfullyParsedCount: Int = 0,
  val uniqueEndpointCount: Int = 0,
  val exactDuplicateCount: Int = 0,
  val invalidUrlCount: Int = 0,
  val httpEndpointCount: Int = 0,
  val httpsEndpointCount: Int = 0,
  val ftpEndpointCount: Int = 0,
  val ftpsEndpointCount: Int = 0,
  val unsupportedSchemeCount: Int = 0,
  val hostnameEndpointCount: Int = 0,
  val publicIpv4EndpointCount: Int = 0,
  val privateIpv4EndpointCount: Int = 0,
  val explicitPortCount: Int = 0,
  val nonRootPathCount: Int = 0,
  val manualReviewCount: Int = 0,
)
