package com.jubayerhossain.bdixtester.data.remote

import com.jubayerhossain.bdixtester.core.model.SyncStatus

data class SignedCatalogManifest(
  val keyId: String,
  val catalogSequence: Long,
  val schemaVersion: Int,
  val minAppVersionCode: Int,
  val payloadUrl: String,
  val payloadSize: Long,
  val sha256: String,
  val generatedAt: String,
  val publishedAt: String,
)

data class RemoteCatalogEntryDto(
  val id: String,
  val name: String?,
  val url: String,
  val category: String?,
  val isFeatured: Boolean,
  val verificationState: String?,
  val description: String?,
  val speedTestUrl: String?,
  val speedTestMaxBytes: Long?,
)

data class RemoteCatalogPayload(
  val schemaVersion: Int,
  val catalogSequence: Long,
  val entries: List<RemoteCatalogEntryDto>,
)

data class RemoteSyncResult(
  val status: SyncStatus,
  val catalogSequence: Long,
  val entryCount: Int,
  val message: String,
)
