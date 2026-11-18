package com.jubayerhossain.bdixtester.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jubayerhossain.bdixtester.BdixtesterApplication
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.data.preferences.AppSettings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class CatalogSyncWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? BdixtesterApplication ?: return Result.failure()
    val container = app.appContainer
    val settings = container.settingsRepository.settings.first()
    if (!settings.autoCatalogUpdates || settings.remoteCatalogUrl.isBlank()) {
      return Result.success()
    }
    val outcome = container.remoteCatalogSyncManager.syncCatalog(settings)
    return when (outcome.status) {
      SyncStatus.UPDATED,
      SyncStatus.NOT_MODIFIED,
      -> Result.success()
      SyncStatus.WIFI_REQUIRED,
      SyncStatus.NETWORK_ERROR,
      -> Result.retry()
      SyncStatus.REJECTED_SIGNATURE,
      SyncStatus.REJECTED_ROLLBACK,
      SyncStatus.REJECTED_CONFLICT,
      SyncStatus.REJECTED_SCHEMA,
      SyncStatus.REJECTED_SIZE,
      SyncStatus.REJECTED_ORIGIN,
      -> Result.failure()
      SyncStatus.IDLE,
      SyncStatus.SYNCING,
      -> Result.success()
    }
  }

  companion object {
    const val UNIQUE_WORK_NAME = "bdix_periodic_catalog_sync"
  }
}

class CatalogSyncScheduler(
  private val context: Context,
) {
  fun applySchedule(settings: AppSettings) {
    val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
    if (!settings.autoCatalogUpdates || settings.remoteCatalogUrl.isBlank()) {
      workManager.cancelUniqueWork(CatalogSyncWorker.UNIQUE_WORK_NAME)
      return
    }
    val constraints =
      Constraints
        .Builder()
        .setRequiredNetworkType(
          if (settings.wifiOnlyCatalogUpdates) NetworkType.UNMETERED else NetworkType.CONNECTED,
        ).setRequiresBatteryNotLow(true)
        .build()

    val request =
      PeriodicWorkRequestBuilder<CatalogSyncWorker>(24L, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    workManager.enqueueUniquePeriodicWork(
      CatalogSyncWorker.UNIQUE_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }
}
