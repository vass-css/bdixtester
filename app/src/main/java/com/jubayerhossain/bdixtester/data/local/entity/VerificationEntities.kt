package com.jubayerhossain.bdixtester.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "endpoint_governance",
  indices = [
    Index(value = ["lifecycleStatus"]),
    Index(value = ["lastVerifiedAtEpochMs"]),
  ],
)
data class EndpointGovernanceEntity(
  @PrimaryKey val serverId: String,
  val lifecycleStatus: String,
  val confidence: String,
  val reason: String,
  val recommendedAction: String,
  val removalEligible: Boolean,
  val normalizedDomain: String?,
  val dnsSummary: String,
  val registrationStatus: String,
  val evidenceJson: String,
  val checkedSourcesCsv: String,
  val missingEvidenceCsv: String,
  val recheckAfterEpochMs: Long?,
  val lastVerifiedAtEpochMs: Long?,
  val archivedAtEpochMs: Long?,
  val archivedByUser: Boolean,
  val updatedAtEpochMs: Long,
)

@Entity(
  tableName = "verification_events",
  indices = [
    Index(value = ["serverId", "createdAtEpochMs"]),
    Index(value = ["createdAtEpochMs"]),
  ],
)
data class VerificationEventEntity(
  @PrimaryKey(autoGenerate = true) val eventId: Long = 0L,
  val serverId: String,
  val previousStatus: String,
  val newStatus: String,
  val confidence: String,
  val triggerSource: String,
  val summary: String,
  val detailsJson: String,
  val createdAtEpochMs: Long,
)
