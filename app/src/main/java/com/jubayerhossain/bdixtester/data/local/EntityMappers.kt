package com.jubayerhossain.bdixtester.data.local

import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.SpeedTerminationReason
import com.jubayerhossain.bdixtester.core.model.SpeedTestOutcome
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState
import com.jubayerhossain.bdixtester.data.local.entity.CatalogMetadataEntity
import com.jubayerhossain.bdixtester.data.local.entity.FavoriteEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.local.entity.TestHistoryEntity
import com.jubayerhossain.bdixtester.domain.model.CatalogSyncState
import com.jubayerhossain.bdixtester.domain.model.FavoriteInfo
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.domain.model.ServerTestResult

fun ServerEntity.toDomain(): ServerEndpoint =
  ServerEndpoint(
    id = id,
    name = name,
    originalUrl = originalUrl,
    host = host,
    explicitPort = explicitPort,
    protocol = parseEnum(protocol, ServerProtocol.UNSUPPORTED),
    category = parseEnum(category, ServerCategory.UNCATEGORIZED),
    isFeatured = isFeatured,
    sourceType = parseEnum(sourceType, CatalogSourceType.BUILT_IN),
    sourceIndex = sourceIndex,
    verificationState = parseEnum(verificationState, VerificationState.UNKNOWN),
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    description = description,
    sourceAttribution = sourceAttribution,
    speedTestUrl = speedTestUrl,
    speedTestMaxBytes = speedTestMaxBytes,
    isPrivateIp = isPrivateIp,
  )

fun ServerEndpoint.toEntity(
  nowEpochMs: Long,
  feedId: String? = null,
  remoteEntryId: String? = null,
  createdAtEpochMs: Long = nowEpochMs,
): ServerEntity =
  ServerEntity(
    id = id,
    sourceType = sourceType.name,
    sourceIndex = sourceIndex,
    feedId = feedId,
    remoteEntryId = remoteEntryId,
    name = name,
    originalUrl = originalUrl,
    host = host,
    explicitPort = explicitPort,
    protocol = protocol.name,
    category = category.name,
    isFeatured = isFeatured,
    description = description,
    sourceAttribution = sourceAttribution,
    verificationState = verificationState.name,
    lastReviewedAtEpochMs = lastReviewedAtEpochMs,
    speedTestUrl = speedTestUrl,
    speedTestMaxBytes = speedTestMaxBytes,
    isPrivateIp = isPrivateIp,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = nowEpochMs,
  )

fun FavoriteEntity.toDomain(): FavoriteInfo =
  FavoriteInfo(
    serverId = serverId,
    isPinned = isPinned,
    displayOrder = displayOrder,
    addedAtEpochMs = addedAtEpochMs,
    note = note,
  )

fun TestHistoryEntity.toDomain(): ServerTestResult =
  ServerTestResult(
    historyId = historyId,
    serverId = serverId,
    scanRunId = scanRunId,
    probeGroupId = probeGroupId,
    isCoalescedReplica = isCoalescedReplica,
    serverNameSnapshot = serverNameSnapshot,
    urlSnapshot = urlSnapshot,
    testedAtEpochMs = testedAtEpochMs,
    scanMode = parseEnum(scanMode, ScanMode.FULL_SCAN),
    protocol = parseEnum(protocol, ServerProtocol.UNSUPPORTED),
    status = parseEnum(status, TestStatus.NOT_TESTED),
    dnsTimeMs = dnsTimeMs,
    tcpConnectTimeMs = tcpConnectTimeMs,
    tlsHandshakeTimeMs = tlsHandshakeTimeMs,
    httpResponseLatencyMs = httpResponseLatencyMs,
    ftpConnectLatencyMs = ftpGreetingLatencyMs,
    ftpGreetingCode = ftpGreetingCode,
    httpStatusCode = httpStatusCode,
    finalUrl = finalUrl,
    failureMessage = failureMessage,
    errorClassification = parseEnum(errorClassification, ErrorClassification.NONE),
    downloadMbps = downloadMbps,
    downloadMibPerSec = downloadMibPerSec,
    transferredBytes = transferredBytes,
    speedElapsedNanos = speedElapsedNanos,
    speedTestState = speedTestState?.let { parseNullableEnum<SpeedTestOutcome>(it) },
    speedTerminationReason = speedTerminationReason?.let { parseNullableEnum<SpeedTerminationReason>(it) },
    networkProfileLabel = networkProfileLabel,
    isQuickScanInconclusive = isQuickScanInconclusive,
  )

fun ServerTestResult.toEntity(): TestHistoryEntity =
  TestHistoryEntity(
    historyId = historyId,
    serverId = serverId,
    scanRunId = scanRunId,
    probeGroupId = probeGroupId,
    isCoalescedReplica = isCoalescedReplica,
    serverNameSnapshot = serverNameSnapshot,
    urlSnapshot = urlSnapshot,
    protocol = protocol.name,
    scanMode = scanMode.name,
    status = status.name,
    errorClassification = errorClassification.name,
    testedAtEpochMs = testedAtEpochMs,
    dnsTimeMs = dnsTimeMs,
    tcpConnectTimeMs = tcpConnectTimeMs,
    tlsHandshakeTimeMs = tlsHandshakeTimeMs,
    httpResponseLatencyMs = httpResponseLatencyMs,
    ftpGreetingLatencyMs = ftpConnectLatencyMs,
    ftpGreetingCode = ftpGreetingCode,
    httpStatusCode = httpStatusCode,
    finalUrl = finalUrl,
    failureMessage = failureMessage,
    downloadMbps = downloadMbps,
    downloadMibPerSec = downloadMibPerSec,
    transferredBytes = transferredBytes,
    speedElapsedNanos = speedElapsedNanos,
    speedTestState = speedTestState?.name,
    speedTerminationReason = speedTerminationReason?.name,
    networkProfileLabel = networkProfileLabel,
    isQuickScanInconclusive = isQuickScanInconclusive,
  )

fun CatalogMetadataEntity.toDomain(): CatalogSyncState =
  CatalogSyncState(
    feedId = feedId,
    sourceUrl = sourceUrl.orEmpty(),
    catalogSequence = catalogSequence,
    schemaVersion = schemaVersion,
    payloadSha256 = payloadSha256,
    etag = etag,
    lastModified = lastModified,
    lastSyncAttemptAtEpochMs = lastSyncAttemptAtEpochMs,
    lastSuccessfulSyncAtEpochMs = lastSuccessfulSyncAtEpochMs,
    lastSyncStatus = parseEnum(lastSyncStatus, SyncStatus.IDLE),
    lastSyncMessage = lastSyncMessage,
    remoteEntryCount = entryCount,
  )

private inline fun <reified T : Enum<T>> parseEnum(
  raw: String,
  fallback: T,
): T =
  enumValues<T>().firstOrNull {
    it.name == raw
  } ?: fallback

private inline fun <reified T : Enum<T>> parseNullableEnum(raw: String): T? =
  enumValues<T>().firstOrNull {
    it.name == raw
  }

fun com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity.toDomain():
  com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord {
  val payload = VerificationEvidenceJson.decode(evidenceJson)
  return com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord(
    serverId = serverId,
    lifecycleStatus =
      com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
        .fromWireOrName(lifecycleStatus),
    confidence =
      com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
        .fromWireOrName(confidence),
    reason = reason,
    recommendedAction = recommendedAction,
    removalEligible = removalEligible,
    normalizedDomain = normalizedDomain,
    dnsSummary =
      com.jubayerhossain.bdixtester.core.model.DnsSummary
        .fromWire(dnsSummary),
    registrationStatus =
      com.jubayerhossain.bdixtester.core.model.RegistrationLookupStatus
        .fromWire(registrationStatus),
    checkedSources = checkedSourcesCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() },
    observedEvidence = payload.observedEvidence,
    missingEvidence = missingEvidenceCsv.split("|").map { it.trim() }.filter { it.isNotEmpty() },
    dns = payload.dns,
    registration = payload.registration,
    http = payload.http,
    ftp = payload.ftp,
    ispProbes = payload.ispProbes,
    recheckAfterEpochMs = recheckAfterEpochMs,
    lastVerifiedAtEpochMs = lastVerifiedAtEpochMs,
    archivedAtEpochMs = archivedAtEpochMs,
    archivedByUser = archivedByUser,
    updatedAtEpochMs = updatedAtEpochMs,
  )
}

fun com.jubayerhossain.bdixtester.core.model.EndpointGovernanceRecord.toEntity():
  com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity =
  com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity(
    serverId = serverId,
    lifecycleStatus = lifecycleStatus.wireName,
    confidence = confidence.wireName,
    reason = reason,
    recommendedAction = recommendedAction,
    removalEligible = removalEligible,
    normalizedDomain = normalizedDomain,
    dnsSummary = dnsSummary.wireName,
    registrationStatus = registrationStatus.wireName,
    evidenceJson =
      VerificationEvidenceJson.encode(
        StoredEvidencePayload(
          observedEvidence = observedEvidence,
          dns = dns,
          registration = registration,
          http = http,
          ftp = ftp,
          ispProbes = ispProbes,
        ),
      ),
    checkedSourcesCsv = checkedSources.joinToString("|"),
    missingEvidenceCsv = missingEvidence.joinToString("|"),
    recheckAfterEpochMs = recheckAfterEpochMs,
    lastVerifiedAtEpochMs = lastVerifiedAtEpochMs,
    archivedAtEpochMs = archivedAtEpochMs,
    archivedByUser = archivedByUser,
    updatedAtEpochMs = updatedAtEpochMs,
  )

fun com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity.toDomain():
  com.jubayerhossain.bdixtester.core.model.VerificationEventRecord =
  com.jubayerhossain.bdixtester.core.model.VerificationEventRecord(
    eventId = eventId,
    serverId = serverId,
    previousStatus =
      com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
        .fromWireOrName(previousStatus),
    newStatus =
      com.jubayerhossain.bdixtester.core.model.VerificationLifecycleStatus
        .fromWireOrName(newStatus),
    confidence =
      com.jubayerhossain.bdixtester.core.model.ConfidenceLevel
        .fromWireOrName(confidence),
    triggerSource = triggerSource,
    summary = summary,
    detailsJson = detailsJson,
    createdAtEpochMs = createdAtEpochMs,
  )

fun com.jubayerhossain.bdixtester.core.model.VerificationEventRecord.toEntity():
  com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity =
  com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity(
    eventId = eventId,
    serverId = serverId,
    previousStatus = previousStatus.wireName,
    newStatus = newStatus.wireName,
    confidence = confidence.wireName,
    triggerSource = triggerSource,
    summary = summary,
    detailsJson = detailsJson,
    createdAtEpochMs = createdAtEpochMs,
  )
