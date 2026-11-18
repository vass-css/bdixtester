package com.jubayerhossain.bdixtester.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "servers",
  indices = [
    Index(value = ["sourceType", "sourceIndex"], unique = true),
    Index(value = ["category"]),
    Index(value = ["protocol"]),
    Index(value = ["originalUrl"]),
  ],
)
data class ServerEntity(
  @PrimaryKey val id: String,
  val sourceType: String,
  val sourceIndex: Int?,
  val feedId: String?,
  val remoteEntryId: String?,
  val name: String,
  val originalUrl: String,
  val host: String,
  val explicitPort: Int?,
  val protocol: String,
  val category: String,
  val isFeatured: Boolean,
  val description: String?,
  val sourceAttribution: String?,
  val verificationState: String,
  val lastReviewedAtEpochMs: Long?,
  val speedTestUrl: String?,
  val speedTestMaxBytes: Long?,
  val isPrivateIp: Boolean,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
)

@Entity(
  tableName = "favorites",
  indices = [
    Index(value = ["isPinned", "displayOrder"]),
  ],
)
data class FavoriteEntity(
  @PrimaryKey val serverId: String,
  val isPinned: Boolean,
  val displayOrder: Int,
  val addedAtEpochMs: Long,
  val note: String?,
)

@Entity(
  tableName = "test_history",
  indices = [
    Index(value = ["serverId", "testedAtEpochMs"]),
    Index(value = ["testedAtEpochMs"]),
    Index(value = ["scanRunId"]),
    Index(value = ["probeGroupId"]),
  ],
)
data class TestHistoryEntity(
  @PrimaryKey(autoGenerate = true) val historyId: Long = 0L,
  val serverId: String,
  val scanRunId: String?,
  val probeGroupId: String?,
  val isCoalescedReplica: Boolean,
  val serverNameSnapshot: String,
  val urlSnapshot: String,
  val protocol: String,
  val scanMode: String,
  val status: String,
  val errorClassification: String,
  val testedAtEpochMs: Long,
  val dnsTimeMs: Long?,
  val tcpConnectTimeMs: Long?,
  val tlsHandshakeTimeMs: Long?,
  val httpResponseLatencyMs: Long?,
  val ftpGreetingLatencyMs: Long?,
  val ftpGreetingCode: Int?,
  val httpStatusCode: Int?,
  val finalUrl: String?,
  val failureMessage: String?,
  val downloadMbps: Double?,
  val downloadMibPerSec: Double?,
  val transferredBytes: Long?,
  val speedElapsedNanos: Long?,
  val speedTestState: String?,
  val speedTerminationReason: String?,
  val networkProfileLabel: String?,
  val isQuickScanInconclusive: Boolean,
)

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
  @PrimaryKey val scanRunId: String,
  val scanMode: String,
  val scopeLabel: String,
  val state: String,
  val totalCatalogEntries: Int,
  val uniqueExactUrlCount: Int,
  val executedUniqueProbeCount: Int,
  val mappedSourceEntryCount: Int,
  val startedAtEpochMs: Long,
  val updatedAtEpochMs: Long,
  val finishedAtEpochMs: Long?,
)

@Entity(tableName = "catalog_metadata")
data class CatalogMetadataEntity(
  @PrimaryKey val feedId: String,
  val sourceUrl: String?,
  val keyId: String?,
  val catalogSequence: Long,
  val schemaVersion: Int,
  val minAppVersionCode: Int,
  val payloadSha256: String?,
  val payloadSizeBytes: Long?,
  val etag: String?,
  val lastModified: String?,
  val generatedAtIso: String?,
  val publishedAtIso: String?,
  val lastSyncAttemptAtEpochMs: Long?,
  val lastSuccessfulSyncAtEpochMs: Long?,
  val lastSyncStatus: String,
  val lastSyncMessage: String?,
  val entryCount: Int,
)
