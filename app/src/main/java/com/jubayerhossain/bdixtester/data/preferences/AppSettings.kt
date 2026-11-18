package com.jubayerhossain.bdixtester.data.preferences

import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ThemePreference

data class AppSettings(
  val themePreset: AppThemePreset = AppThemePreset.MIDNIGHT_INDIGO,
  val themePreference: ThemePreference = ThemePreference.SYSTEM,
  val appLanguage: AppLanguage = AppLanguage.SYSTEM,
  val defaultScanMode: ScanMode = ScanMode.QUICK_SCAN,
  val fullScanConnectTimeoutMs: Int = DEFAULT_FULL_CONNECT_TIMEOUT_MS,
  val fullScanReadTimeoutMs: Int = DEFAULT_FULL_READ_TIMEOUT_MS,
  val quickScanDeadlineMs: Int = DEFAULT_QUICK_SCAN_DEADLINE_MS,
  val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
  val autoCatalogUpdates: Boolean = false,
  val wifiOnlyCatalogUpdates: Boolean = true,
  val remoteCatalogUrl: String = "",
  val allowCrossOriginRemotePayload: Boolean = false,
  val speedTestMaxBytes: Long = DEFAULT_SPEED_TEST_MAX_BYTES,
  val historyRetentionDays: Int = DEFAULT_HISTORY_RETENTION_DAYS,
  val manualNetworkLabel: String = "",
  val reduceAnimations: Boolean = false,
) {
  companion object {
    const val DEFAULT_FULL_CONNECT_TIMEOUT_MS = 5_000
    const val DEFAULT_FULL_READ_TIMEOUT_MS = 6_000
    const val DEFAULT_FULL_REQUEST_DEADLINE_MS = 15_000
    const val DEFAULT_QUICK_SCAN_DEADLINE_MS = 2_500
    const val DEFAULT_MAX_CONCURRENCY = 3
    const val MIN_CONCURRENCY = 1
    const val MAX_CONCURRENCY = 5
    const val DEFAULT_SPEED_TEST_MAX_BYTES = 5L * 1024L * 1024L
    const val MAX_SPEED_TEST_CAP_BYTES = 20L * 1024L * 1024L
    const val DEFAULT_HISTORY_RETENTION_DAYS = 30
  }
}
