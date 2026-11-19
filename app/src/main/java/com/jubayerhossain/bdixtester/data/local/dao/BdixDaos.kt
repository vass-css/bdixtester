package com.jubayerhossain.bdixtester.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.jubayerhossain.bdixtester.data.local.entity.CatalogMetadataEntity
import com.jubayerhossain.bdixtester.data.local.entity.FavoriteEntity
import com.jubayerhossain.bdixtester.data.local.entity.ScanSessionEntity
import com.jubayerhossain.bdixtester.data.local.entity.ServerEntity
import com.jubayerhossain.bdixtester.data.local.entity.TestHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
  @Query(
    """
        SELECT * FROM servers
        ORDER BY
            CASE
                WHEN sourceType = 'BUILT_IN' THEN 0
                WHEN sourceType = 'REMOTE_CATALOG' THEN 1
                ELSE 2
            END ASC,
            COALESCE(sourceIndex, 2147483647) ASC,
            createdAtEpochMs DESC
        """,
  )
  fun observeAllServers(): Flow<List<ServerEntity>>

  @Query("SELECT * FROM servers WHERE sourceType = 'BUILT_IN' ORDER BY sourceIndex ASC")
  suspend fun getBuiltInServersOrdered(): List<ServerEntity>

  @Query("SELECT * FROM servers")
  suspend fun getAllServersOnce(): List<ServerEntity>

  @Query("SELECT * FROM servers WHERE id = :serverId LIMIT 1")
  suspend fun getServerById(serverId: String): ServerEntity?

  @Query("SELECT COUNT(*) FROM servers WHERE sourceType = 'BUILT_IN'")
  suspend fun countBuiltInServers(): Int

  @Query("SELECT COUNT(*) FROM servers WHERE sourceType = 'REMOTE_CATALOG'")
  suspend fun countRemoteServers(): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertAllStrict(servers: List<ServerEntity>)

  @Upsert
  suspend fun upsertServer(server: ServerEntity)

  @Upsert
  suspend fun upsertServers(servers: List<ServerEntity>)

  @Query("DELETE FROM servers WHERE sourceType = 'BUILT_IN'")
  suspend fun deleteAllBuiltInServers()

  @Query("DELETE FROM servers WHERE sourceType = 'REMOTE_CATALOG'")
  suspend fun deleteRemoteCatalogServers()

  @Query("DELETE FROM servers WHERE id = :serverId AND sourceType = 'USER_CUSTOM'")
  suspend fun deleteCustomServerById(serverId: String)

  @Query("DELETE FROM servers WHERE sourceType = 'USER_CUSTOM'")
  suspend fun deleteAllCustomServers()

  @Transaction
  suspend fun syncBuiltInServersAtomically(expectedServers: List<ServerEntity>) {
    deleteAllBuiltInServers()
    if (expectedServers.isNotEmpty()) {
      upsertServers(expectedServers)
    }
  }

  @Transaction
  suspend fun replaceRemoteCatalogAtomically(remoteServers: List<ServerEntity>) {
    deleteRemoteCatalogServers()
    if (remoteServers.isNotEmpty()) {
      upsertServers(remoteServers)
    }
  }
}

@Dao
interface FavoriteDao {
  @Query("SELECT * FROM favorites ORDER BY isPinned DESC, displayOrder ASC, addedAtEpochMs DESC")
  fun observeAllFavorites(): Flow<List<FavoriteEntity>>

  @Query("SELECT * FROM favorites WHERE serverId = :serverId LIMIT 1")
  suspend fun getFavorite(serverId: String): FavoriteEntity?

  @Upsert
  suspend fun upsertFavorite(favorite: FavoriteEntity)

  @Query("DELETE FROM favorites WHERE serverId = :serverId")
  suspend fun deleteFavorite(serverId: String)

  @Query("DELETE FROM favorites")
  suspend fun clearAllFavorites()
}

@Dao
interface TestHistoryDao {
  @Query("SELECT * FROM test_history ORDER BY testedAtEpochMs DESC, historyId DESC")
  fun observeAllHistory(): Flow<List<TestHistoryEntity>>

  @Query(
    """
        SELECT th.* FROM test_history th
        INNER JOIN (
            SELECT serverId, MAX(historyId) AS maxId
            FROM test_history
            GROUP BY serverId
        ) latest ON th.serverId = latest.serverId AND th.historyId = latest.maxId
        """,
  )
  fun observeLatestResultPerServer(): Flow<List<TestHistoryEntity>>

  @Query("SELECT * FROM test_history WHERE serverId = :serverId ORDER BY testedAtEpochMs DESC, historyId DESC")
  fun observeHistoryForServer(serverId: String): Flow<List<TestHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertHistory(entry: TestHistoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertHistoryBatch(entries: List<TestHistoryEntity>)

  @Query("DELETE FROM test_history WHERE historyId = :historyId")
  suspend fun deleteHistoryById(historyId: Long)

  @Query("DELETE FROM test_history WHERE testedAtEpochMs < :cutoffEpochMs")
  suspend fun deleteOlderThan(cutoffEpochMs: Long)

  @Query("DELETE FROM test_history")
  suspend fun clearAllHistory()
}

@Dao
interface ScanSessionDao {
  @Query("SELECT * FROM scan_sessions ORDER BY startedAtEpochMs DESC LIMIT 1")
  fun observeLatestSession(): Flow<ScanSessionEntity?>

  @Query("SELECT * FROM scan_sessions WHERE state = 'RUNNING'")
  suspend fun getRunningSessions(): List<ScanSessionEntity>

  @Upsert
  suspend fun upsertSession(session: ScanSessionEntity)

  @Query(
    """
        UPDATE scan_sessions
        SET state = :newState, updatedAtEpochMs = :updatedAtEpochMs, finishedAtEpochMs = :finishedAtEpochMs
        WHERE state = 'RUNNING'
        """,
  )
  suspend fun markRunningSessionsAs(
    newState: String,
    updatedAtEpochMs: Long,
    finishedAtEpochMs: Long,
  )

  @Query("DELETE FROM scan_sessions")
  suspend fun clearAllSessions()
}

@Dao
interface CatalogMetadataDao {
  @Query("SELECT * FROM catalog_metadata WHERE feedId = :feedId LIMIT 1")
  fun observeMetadata(feedId: String): Flow<CatalogMetadataEntity?>

  @Query("SELECT * FROM catalog_metadata WHERE feedId = :feedId LIMIT 1")
  suspend fun getMetadata(feedId: String): CatalogMetadataEntity?

  @Upsert
  suspend fun upsertMetadata(metadata: CatalogMetadataEntity)

  @Query("DELETE FROM catalog_metadata WHERE feedId = :feedId")
  suspend fun deleteMetadata(feedId: String)
}

@Dao
interface GovernanceDao {
  @Query("SELECT * FROM endpoint_governance")
  fun observeAllGovernance(): Flow<List<com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity>>

  @Query("SELECT * FROM endpoint_governance")
  suspend fun getAllGovernanceOnce(): List<com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity>

  @Query("SELECT * FROM endpoint_governance WHERE serverId = :serverId LIMIT 1")
  suspend fun getGovernanceByServerId(
    serverId: String,
  ): com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity?

  @Query("SELECT * FROM verification_events WHERE serverId = :serverId ORDER BY createdAtEpochMs DESC, eventId DESC")
  fun observeEventsForServer(
    serverId: String,
  ): Flow<List<com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity>>

  @Query("SELECT * FROM verification_events ORDER BY createdAtEpochMs DESC, eventId DESC LIMIT :limit")
  suspend fun getRecentEvents(limit: Int = 100): List<com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity>

  @Upsert
  suspend fun upsertGovernance(entity: com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity)

  @Upsert
  suspend fun upsertGovernanceBatch(
    entities: List<com.jubayerhossain.bdixtester.data.local.entity.EndpointGovernanceEntity>,
  )

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertEvent(event: com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity): Long

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertEventsBatch(events: List<com.jubayerhossain.bdixtester.data.local.entity.VerificationEventEntity>)

  @Query("DELETE FROM verification_events")
  suspend fun clearAllEvents()

  @Query("DELETE FROM endpoint_governance")
  suspend fun clearAllGovernance()
}
