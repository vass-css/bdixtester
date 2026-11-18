package com.jubayerhossain.bdixtester.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.bdixSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "bdix_tester_settings",
)

interface SettingsRepository {
  val settings: Flow<AppSettings>

  suspend fun setThemePreset(preset: AppThemePreset)

  suspend fun setThemePreference(preference: ThemePreference)

  suspend fun setAppLanguage(language: AppLanguage)

  suspend fun setDefaultScanMode(scanMode: ScanMode)

  suspend fun setFullScanTimeouts(
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
  )

  suspend fun setQuickScanDeadline(deadlineMs: Int)

  suspend fun setMaxConcurrency(maxConcurrency: Int)

  suspend fun setAutoCatalogUpdates(enabled: Boolean)

  suspend fun setWifiOnlyCatalogUpdates(wifiOnly: Boolean)

  suspend fun setRemoteCatalogUrl(url: String)

  suspend fun setAllowCrossOriginRemotePayload(allow: Boolean)

  suspend fun setSpeedTestMaxBytes(maxBytes: Long)

  suspend fun setHistoryRetentionDays(days: Int)

  suspend fun setManualNetworkLabel(label: String)

  suspend fun setReduceAnimations(enabled: Boolean)

  suspend fun resetSettings()
}

class SettingsDataStoreRepository(
  private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
  constructor(context: Context) : this(context.applicationContext.bdixSettingsDataStore)

  private object Keys {
    val THEME_PRESET = stringPreferencesKey("theme_preset")
    val THEME = stringPreferencesKey("theme_preference")
    val LANGUAGE = stringPreferencesKey("app_language")
    val DEFAULT_SCAN_MODE = stringPreferencesKey("default_scan_mode")
    val FULL_CONNECT_MS = intPreferencesKey("full_connect_timeout_ms")
    val FULL_READ_MS = intPreferencesKey("full_read_timeout_ms")
    val QUICK_DEADLINE_MS = intPreferencesKey("quick_scan_deadline_ms")
    val MAX_CONCURRENCY = intPreferencesKey("max_concurrency")
    val AUTO_UPDATES = booleanPreferencesKey("auto_catalog_updates")
    val WIFI_ONLY_UPDATES = booleanPreferencesKey("wifi_only_catalog_updates")
    val REMOTE_CATALOG_URL = stringPreferencesKey("remote_catalog_url")
    val ALLOW_CROSS_ORIGIN = booleanPreferencesKey("allow_cross_origin_remote_payload")
    val SPEED_TEST_MAX_BYTES = longPreferencesKey("speed_test_max_bytes")
    val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
    val MANUAL_NETWORK_LABEL = stringPreferencesKey("manual_network_label")
    val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
  }

  override val settings: Flow<AppSettings> =
    dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }.map { prefs ->
        val themePreset = migrateThemePreset(prefs[Keys.THEME_PRESET])
        val theme = migrateThemePreference(prefs[Keys.THEME])

        val language =
          prefs[Keys.LANGUAGE]?.let { name ->
            AppLanguage.entries.firstOrNull { it.name == name }
          } ?: AppLanguage.SYSTEM

        val defaultScanMode =
          prefs[Keys.DEFAULT_SCAN_MODE]?.let { name ->
            ScanMode.entries.firstOrNull { it.name == name }
          } ?: ScanMode.QUICK_SCAN

        AppSettings(
          themePreset = themePreset,
          themePreference = theme,
          appLanguage = language,
          defaultScanMode = defaultScanMode,
          fullScanConnectTimeoutMs =
            (
              prefs[Keys.FULL_CONNECT_MS]
                ?: AppSettings.DEFAULT_FULL_CONNECT_TIMEOUT_MS
            ).coerceIn(2_000, 15_000),
          fullScanReadTimeoutMs =
            (
              prefs[Keys.FULL_READ_MS]
                ?: AppSettings.DEFAULT_FULL_READ_TIMEOUT_MS
            ).coerceIn(2_000, 15_000),
          quickScanDeadlineMs =
            (
              prefs[Keys.QUICK_DEADLINE_MS]
                ?: AppSettings.DEFAULT_QUICK_SCAN_DEADLINE_MS
            ).coerceIn(1_500, 5_000),
          maxConcurrency =
            (
              prefs[Keys.MAX_CONCURRENCY]
                ?: AppSettings.DEFAULT_MAX_CONCURRENCY
            ).coerceIn(AppSettings.MIN_CONCURRENCY, AppSettings.MAX_CONCURRENCY),
          autoCatalogUpdates = prefs[Keys.AUTO_UPDATES] ?: false,
          wifiOnlyCatalogUpdates = prefs[Keys.WIFI_ONLY_UPDATES] ?: true,
          remoteCatalogUrl = prefs[Keys.REMOTE_CATALOG_URL].orEmpty(),
          allowCrossOriginRemotePayload = prefs[Keys.ALLOW_CROSS_ORIGIN] ?: false,
          speedTestMaxBytes =
            (
              prefs[Keys.SPEED_TEST_MAX_BYTES]
                ?: AppSettings.DEFAULT_SPEED_TEST_MAX_BYTES
            ).coerceIn(1L * 1024L * 1024L, AppSettings.MAX_SPEED_TEST_CAP_BYTES),
          historyRetentionDays =
            (
              prefs[Keys.HISTORY_RETENTION_DAYS]
                ?: AppSettings.DEFAULT_HISTORY_RETENTION_DAYS
            ).coerceIn(1, 365),
          manualNetworkLabel = prefs[Keys.MANUAL_NETWORK_LABEL].orEmpty(),
          reduceAnimations = prefs[Keys.REDUCE_ANIMATIONS] ?: false,
        )
      }

  override suspend fun setThemePreset(preset: AppThemePreset) {
    dataStore.edit { it[Keys.THEME_PRESET] = preset.name }
  }

  override suspend fun setThemePreference(preference: ThemePreference) {
    dataStore.edit { it[Keys.THEME] = preference.name }
  }

  companion object {
    internal fun migrateThemePreset(raw: String?): AppThemePreset {
      if (raw.isNullOrBlank()) return AppThemePreset.MIDNIGHT_INDIGO
      return AppThemePreset.entries.firstOrNull {
        it.name.equals(raw.trim(), ignoreCase = true)
      } ?: AppThemePreset.MIDNIGHT_INDIGO
    }

    internal fun migrateThemePreference(raw: String?): ThemePreference {
      if (raw.isNullOrBlank()) return ThemePreference.SYSTEM
      val normalized = raw.trim().uppercase()
      return when (normalized) {
        "SYSTEM", "SYSTEM_DEFAULT", "FOLLOW_SYSTEM", "AUTO", "DEFAULT" -> ThemePreference.SYSTEM
        "LIGHT", "DAY" -> ThemePreference.LIGHT
        "DARK", "NIGHT" -> ThemePreference.DARK
        else -> ThemePreference.SYSTEM
      }
    }
  }

  override suspend fun setAppLanguage(language: AppLanguage) {
    dataStore.edit { it[Keys.LANGUAGE] = language.name }
  }

  override suspend fun setDefaultScanMode(scanMode: ScanMode) {
    dataStore.edit { it[Keys.DEFAULT_SCAN_MODE] = scanMode.name }
  }

  override suspend fun setFullScanTimeouts(
    connectTimeoutMs: Int,
    readTimeoutMs: Int,
  ) {
    dataStore.edit {
      it[Keys.FULL_CONNECT_MS] = connectTimeoutMs.coerceIn(2_000, 15_000)
      it[Keys.FULL_READ_MS] = readTimeoutMs.coerceIn(2_000, 15_000)
    }
  }

  override suspend fun setQuickScanDeadline(deadlineMs: Int) {
    dataStore.edit {
      it[Keys.QUICK_DEADLINE_MS] = deadlineMs.coerceIn(1_500, 5_000)
    }
  }

  override suspend fun setMaxConcurrency(maxConcurrency: Int) {
    dataStore.edit {
      it[Keys.MAX_CONCURRENCY] =
        maxConcurrency.coerceIn(AppSettings.MIN_CONCURRENCY, AppSettings.MAX_CONCURRENCY)
    }
  }

  override suspend fun setAutoCatalogUpdates(enabled: Boolean) {
    dataStore.edit { it[Keys.AUTO_UPDATES] = enabled }
  }

  override suspend fun setWifiOnlyCatalogUpdates(wifiOnly: Boolean) {
    dataStore.edit { it[Keys.WIFI_ONLY_UPDATES] = wifiOnly }
  }

  override suspend fun setRemoteCatalogUrl(url: String) {
    dataStore.edit { it[Keys.REMOTE_CATALOG_URL] = url.trim() }
  }

  override suspend fun setAllowCrossOriginRemotePayload(allow: Boolean) {
    dataStore.edit { it[Keys.ALLOW_CROSS_ORIGIN] = allow }
  }

  override suspend fun setSpeedTestMaxBytes(maxBytes: Long) {
    dataStore.edit {
      it[Keys.SPEED_TEST_MAX_BYTES] =
        maxBytes.coerceIn(1L * 1024L * 1024L, AppSettings.MAX_SPEED_TEST_CAP_BYTES)
    }
  }

  override suspend fun setHistoryRetentionDays(days: Int) {
    dataStore.edit { it[Keys.HISTORY_RETENTION_DAYS] = days.coerceIn(1, 365) }
  }

  override suspend fun setManualNetworkLabel(label: String) {
    dataStore.edit { it[Keys.MANUAL_NETWORK_LABEL] = label.trim().take(48) }
  }

  override suspend fun setReduceAnimations(enabled: Boolean) {
    dataStore.edit { it[Keys.REDUCE_ANIMATIONS] = enabled }
  }

  override suspend fun resetSettings() {
    dataStore.edit { it.clear() }
  }
}
