package com.jubayerhossain.bdixtester.domain.model

import com.jubayerhossain.bdixtester.core.model.CatalogSourceType
import com.jubayerhossain.bdixtester.core.model.ErrorClassification
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ScanScope
import com.jubayerhossain.bdixtester.core.model.ScanSessionState
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.core.model.ServerProtocol
import com.jubayerhossain.bdixtester.core.model.SpeedTerminationReason
import com.jubayerhossain.bdixtester.core.model.SpeedTestOutcome
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.model.TestStatus
import com.jubayerhossain.bdixtester.core.model.VerificationState

data class ServerEndpoint(
  val id: String,
  val name: String,
  val originalUrl: String,
  val host: String,
  val explicitPort: Int?,
  val protocol: ServerProtocol,
  val category: ServerCategory,
  val isFeatured: Boolean,
  val sourceType: CatalogSourceType,
  val sourceIndex: Int? = null,
  val verificationState: VerificationState,
  val lastReviewedAtEpochMs: Long?,
  val description: String? = null,
  val sourceAttribution: String? = null,
  val speedTestUrl: String?,
  val speedTestMaxBytes: Long? = null,
  val isPrivateIp: Boolean,
)

data class FavoriteInfo(
  val serverId: String,
  val isPinned: Boolean,
  val displayOrder: Int,
  val addedAtEpochMs: Long,
  val note: String? = null,
)

data class ServerTestResult(
  val historyId: Long = 0L,
  val serverId: String,
  val scanRunId: String? = null,
  val probeGroupId: String? = null,
  val isCoalescedReplica: Boolean = false,
  val serverNameSnapshot: String = "",
  val urlSnapshot: String = "",
  val testedAtEpochMs: Long,
  val scanMode: ScanMode,
  val protocol: ServerProtocol,
  val status: TestStatus,
  val dnsTimeMs: Long? = null,
  val tcpConnectTimeMs: Long? = null,
  val tlsHandshakeTimeMs: Long? = null,
  val httpResponseLatencyMs: Long? = null,
  val ftpConnectLatencyMs: Long? = null,
  val ftpGreetingCode: Int? = null,
  val httpStatusCode: Int? = null,
  val finalUrl: String? = null,
  val errorClassification: ErrorClassification = ErrorClassification.NONE,
  val failureMessage: String? = null,
  val downloadMbps: Double? = null,
  val downloadMibPerSec: Double? = null,
  val transferredBytes: Long? = null,
  val speedElapsedNanos: Long? = null,
  val speedTestState: SpeedTestOutcome? = null,
  val speedTerminationReason: SpeedTerminationReason? = null,
  val networkProfileLabel: String? = null,
  val isQuickScanInconclusive: Boolean = false,
) {
  val primaryLatencyMs: Long?
    get() = httpResponseLatencyMs ?: ftpConnectLatencyMs ?: tcpConnectTimeMs
}

data class ScanProgressState(
  val scanRunId: String? = null,
  val sessionState: ScanSessionState = ScanSessionState.IDLE,
  val isScanning: Boolean = false,
  val scanMode: ScanMode? = null,
  val scanScope: ScanScope = ScanScope.ALL_SERVERS,
  val totalCount: Int = 0,
  val totalCatalogEntries: Int = 0,
  val uniqueExactUrlCount: Int = 0,
  val executedUniqueProbeCount: Int = 0,
  val mappedSourceEntryCount: Int = 0,
  val queuedCount: Int = 0,
  val activeCount: Int = 0,
  val completedCount: Int = 0,
  val reachableCount: Int = 0,
  val unreachableCount: Int = 0,
  val restrictedOrErrorCount: Int = 0,
  val unsupportedCount: Int = 0,
  val cancelledCount: Int = 0,
  val elapsedMs: Long = 0L,
  val lastScanCompletedAtEpochMs: Long? = null,
  val currentServerName: String? = null,
) {
  val remainingCount: Int
    get() = (totalCount - completedCount).coerceAtLeast(0)

  val progressFraction: Float
    get() = if (totalCount <= 0) 0f else (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}

data class CatalogSyncState(
  val feedId: String = "remote_primary",
  val sourceUrl: String = "",
  val catalogSequence: Long = 0L,
  val schemaVersion: Int = 0,
  val payloadSha256: String? = null,
  val etag: String? = null,
  val lastModified: String? = null,
  val lastSyncAttemptAtEpochMs: Long? = null,
  val lastSuccessfulSyncAtEpochMs: Long? = null,
  val lastSyncStatus: SyncStatus = SyncStatus.IDLE,
  val lastSyncMessage: String? = null,
  val remoteEntryCount: Int = 0,
)

data class CatalogIntegrityReport(
  val builtInSourceCount: Int,
  val builtInDatabaseCount: Int,
  val orderMatches: Boolean,
  val exactUrlsMatch: Boolean,
  val isConsistent: Boolean,
  val mismatchDetails: String? = null,
)
