package com.jubayerhossain.bdixtester.data.remote

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.jubayerhossain.bdixtester.BdixtesterApplication
import com.jubayerhossain.bdixtester.data.verification.CatalogGovernanceCoordinator
import java.util.concurrent.TimeUnit

class CatalogVerificationWorker(
  appContext: Context,
  params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    val app = applicationContext as? BdixtesterApplication ?: return Result.failure()
    val maxEndpoints = inputData.getInt(KEY_MAX_ENDPOINTS, CatalogGovernanceCoordinator.DEFAULT_BATCH_LIMIT)
    val triggerSource = inputData.getString(KEY_TRIGGER_SOURCE) ?: "workmanager_background"

    val outcome =
      app.appContainer.catalogGovernanceCoordinator.verifyBatch(
        maxEndpoints = maxEndpoints,
        triggerSource = triggerSource,
        onStepProgress = { processed, total, _ ->
          setProgress(
            workDataOf(
              KEY_PROGRESS_DONE to processed,
              KEY_PROGRESS_TOTAL to total,
            ),
          )
        },
      )

    return outcome.fold(
      onSuccess = { decisions ->
        val candidateCount = decisions.count { it.status.isRemovalCandidateOrConfirmed }
        VerificationNotificationHelper.postSummaryNotification(
          context = applicationContext,
          checkedCount = decisions.size,
          candidateCount = candidateCount,
        )
        Result.success(
          workDataOf(
            KEY_PROGRESS_DONE to decisions.size,
            KEY_CANDIDATES_FOUND to candidateCount,
          ),
        )
      },
      onFailure = { error ->
        VerificationNotificationHelper.postFailureNotification(
          context = applicationContext,
          errorDetail = error.message ?: "Verification error",
        )
        if (runAttemptCount < 2) Result.retry() else Result.failure()
      },
    )
  }

  companion object {
    const val ONE_TIME_WORK_NAME = "bdix_catalog_verification_now"
    const val PERIODIC_WORK_NAME = "bdix_catalog_verification_periodic"
    const val KEY_MAX_ENDPOINTS = "max_endpoints"
    const val KEY_TRIGGER_SOURCE = "trigger_source"
    const val KEY_PROGRESS_DONE = "progress_done"
    const val KEY_PROGRESS_TOTAL = "progress_total"
    const val KEY_CANDIDATES_FOUND = "candidates_found"
  }
}

class CatalogVerificationScheduler(
  private val context: Context,
) {
  fun enqueueImmediateVerification(maxEndpoints: Int = CatalogGovernanceCoordinator.DEFAULT_BATCH_LIMIT) {
    val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
    val constraints =
      Constraints
        .Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val request =
      OneTimeWorkRequestBuilder<CatalogVerificationWorker>()
        .setConstraints(constraints)
        .setInputData(
          workDataOf(
            CatalogVerificationWorker.KEY_MAX_ENDPOINTS to maxEndpoints,
            CatalogVerificationWorker.KEY_TRIGGER_SOURCE to "workmanager_manual",
          ),
        ).build()

    workManager.enqueueUniqueWork(
      CatalogVerificationWorker.ONE_TIME_WORK_NAME,
      ExistingWorkPolicy.KEEP,
      request,
    )
  }

  fun schedulePeriodicVerification() {
    val workManager = runCatching { WorkManager.getInstance(context) }.getOrNull() ?: return
    val constraints =
      Constraints
        .Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    val request =
      PeriodicWorkRequestBuilder<CatalogVerificationWorker>(7L, TimeUnit.DAYS)
        .setConstraints(constraints)
        .setInputData(
          workDataOf(
            CatalogVerificationWorker.KEY_MAX_ENDPOINTS to CatalogGovernanceCoordinator.DEFAULT_BATCH_LIMIT,
            CatalogVerificationWorker.KEY_TRIGGER_SOURCE to "workmanager_periodic",
          ),
        ).build()

    workManager.enqueueUniquePeriodicWork(
      CatalogVerificationWorker.PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      request,
    )
  }
}
