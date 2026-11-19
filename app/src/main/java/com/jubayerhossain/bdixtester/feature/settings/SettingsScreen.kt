package com.jubayerhossain.bdixtester.feature.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.BuildConfig
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.AppLanguage
import com.jubayerhossain.bdixtester.core.model.AppThemePreset
import com.jubayerhossain.bdixtester.core.model.ScanMode
import com.jubayerhossain.bdixtester.core.model.ThemePreference
import com.jubayerhossain.bdixtester.core.network.NetworkSnapshot
import com.jubayerhossain.bdixtester.core.network.TransportType
import com.jubayerhossain.bdixtester.data.catalog.BdOttPlatforms
import com.jubayerhossain.bdixtester.ui.components.LocalReducedMotion
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.components.PulsingStatusDot
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing
import com.jubayerhossain.bdixtester.ui.theme.BdixTesterTheme
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import com.jubayerhossain.bdixtester.ui.theme.MonoUrlStyle
import com.jubayerhossain.bdixtester.ui.theme.resolveUseDarkTheme
import kotlin.math.roundToInt

private const val FACEBOOK_COMMUNITY_URL = "https://www.facebook.com/search/top/?q=BDIX%20Tester"

private enum class BulkDeleteAction {
  HISTORY,
  FAVORITES,
  CUSTOM_SERVERS,
  RESET_ALL,
}

private enum class ActiveSettingsDialog {
  THEME,
  LANGUAGE,
  SCAN_MODE,
  SPEED_CAP,
  NETWORK_LABEL,
  REMOTE_URL,
  SYNC_OPTIONS,
  RETENTION,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  uiState: SettingsUiState,
  modifier: Modifier = Modifier,
  onNavigateBack: (() -> Unit)? = null,
  onSelectThemePreset: (AppThemePreset) -> Unit = {},
  onSelectTheme: (ThemePreference) -> Unit,
  onSelectLanguage: (AppLanguage) -> Unit,
  onReduceAnimationsChange: (Boolean) -> Unit = {},
  onSelectDefaultScanMode: (ScanMode) -> Unit,
  onMaxConcurrencyChange: (Int) -> Unit,
  onSpeedTestMaxBytesChange: (Long) -> Unit,
  onManualNetworkLabelSave: (String) -> Unit,
  onRemoteCatalogUrlSave: (String) -> Unit,
  onSaveRemoteCatalogUrlAndSync: ((String) -> Unit)? = null,
  onRefreshBuiltInCatalog: () -> Unit = {},
  onAutoUpdatesChange: (Boolean) -> Unit,
  onWifiOnlyChange: (Boolean) -> Unit,
  onCrossOriginChange: (Boolean) -> Unit,
  onSyncNowClick: (String) -> Unit,
  onHistoryRetentionChange: (Int) -> Unit,
  onClearHistory: () -> Unit,
  onClearFavorites: () -> Unit,
  onDeleteCustomServers: () -> Unit,
  onResetAllData: () -> Unit,
  onUpdateServerListClick: () -> Unit = {},
  onAddServerRequestClick: () -> Unit = {},
  onOpenCatalogGovernance: () -> Unit = {},
  onOpenExternalUrl: (String) -> Unit = {},
) {
  val context = LocalContext.current
  val settings = uiState.settings
  val sync = uiState.catalogSyncState
  var activeDialog by remember { mutableStateOf<ActiveSettingsDialog?>(null) }
  var pendingBulkAction by remember { mutableStateOf<BulkDeleteAction?>(null) }
  var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
  val shareMessage = stringResource(R.string.settings_share_app_message)
  val shareTitle = stringResource(R.string.settings_share_app_title)
  val appName = stringResource(R.string.app_name)

  val currentCapMib = (settings.speedTestMaxBytes / (1024L * 1024L)).toInt()
  val scanModeIcon =
    if (settings.defaultScanMode == ScanMode.QUICK_SCAN) {
      Icons.Rounded.Bolt
    } else {
      Icons.Rounded.Radar
    }
  val scanModeSummary =
    if (settings.defaultScanMode == ScanMode.QUICK_SCAN) {
      "${stringResource(R.string.action_quick_scan)} · ${stringResource(R.string.settings_scan_mode_quick_desc)}"
    } else {
      "${stringResource(R.string.action_full_scan)} · ${stringResource(R.string.settings_scan_mode_full_desc)}"
    }

  Scaffold(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("settings_screen"),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          TopAppBar(
            title = {
              Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
              )
            },
            navigationIcon = {
              if (onNavigateBack != null) {
                IconButton(
                  onClick = onNavigateBack,
                  modifier =
                    Modifier
                      .minimumInteractiveComponentSize()
                      .testTag("settings_back_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                  )
                }
              }
            },
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
              ),
          )
          HorizontalDivider(
            thickness = BdixBorder.hairline,
            color = MaterialTheme.colorScheme.outlineVariant,
          )
        }
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .testTag("settings_list"),
      contentPadding =
        PaddingValues(
          start = BdixSpacing.lg,
          end = BdixSpacing.lg,
          top = BdixSpacing.md,
          bottom = BdixSpacing.xxl,
        ),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
    ) {
      // 0. App Profile & Live IP Header Card
      item {
        SettingsAppProfileHeaderCard(
          networkSnapshot = uiState.networkSnapshot,
          totalBdixServers = uiState.totalServerCount,
          totalOttPlatforms = BdOttPlatforms.all.size,
        )
      }

      // 1. Servers & Catalog Actions
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_quick_actions_section))
      }
      item {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = Icons.Rounded.Sync,
            title = stringResource(R.string.drawer_update_server_list),
            subtitle = stringResource(R.string.settings_update_server_list_subtitle),
            onClick = {
              if (settings.remoteCatalogUrl.isBlank()) {
                activeDialog = ActiveSettingsDialog.SYNC_OPTIONS
              } else {
                onUpdateServerListClick()
              }
            },
            testTag = "settings_update_server_list_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.AddCircleOutline,
            title = stringResource(R.string.drawer_add_server_request),
            subtitle = stringResource(R.string.settings_add_server_request_subtitle),
            onClick = onAddServerRequestClick,
            testTag = "settings_add_server_request_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.Shield,
            title = stringResource(R.string.settings_governance_row_title),
            subtitle =
              stringResource(
                R.string.settings_governance_row_subtitle,
                uiState.governanceCounts.activeCount,
                uiState.governanceCounts.removalCandidateCount,
                uiState.governanceCounts.archivedCount,
              ),
            onClick = onOpenCatalogGovernance,
            testTag = "settings_catalog_governance_row",
          )
        }
      }

      // 2. Appearance & Language
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_appearance_section))
      }
      item(key = "settings_appearance_group") {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = Icons.Rounded.DarkMode,
            title = stringResource(R.string.settings_appearance_mode_title),
            subtitle = "${presetTitle(
              settings.themePreset,
            )} · ${appearanceLabel(settings.themePreference)}",
            onClick = { activeDialog = ActiveSettingsDialog.THEME },
            testTag = "settings_theme_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.Translate,
            title = stringResource(R.string.settings_language_label),
            subtitle = settings.appLanguage.displayLabel,
            onClick = { activeDialog = ActiveSettingsDialog.LANGUAGE },
            testTag = "settings_language_row",
          )
          SettingsRowDivider()
          SettingsSwitchRow(
            icon = Icons.Rounded.MotionPhotosOff,
            title = stringResource(R.string.settings_reduce_animations_title),
            subtitle = stringResource(R.string.settings_reduce_animations_subtitle),
            checked = settings.reduceAnimations,
            onCheckedChange = onReduceAnimationsChange,
            testTag = "settings_reduce_animations_switch",
          )
        }
      }

      item(key = "settings_color_theme_header") {
        SettingsSectionHeader(title = stringResource(R.string.settings_color_theme_section_title))
      }

      AppThemePreset.entries.forEach { preset ->
        item(key = "theme_preset_${preset.name}") {
          val previewDarkMode = resolveUseDarkTheme(settings.themePreference)
          ThemePresetPreviewCard(
            preset = preset,
            isSelected = preset == settings.themePreset,
            previewDarkMode = previewDarkMode,
            onClick = { onSelectThemePreset(preset) },
            modifier = Modifier.widthIn(max = 720.dp),
          )
        }
      }

      // 2. Testing Engine & Network
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_network_section))
      }
      item {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = scanModeIcon,
            title = stringResource(R.string.settings_scan_mode_title),
            subtitle = scanModeSummary,
            onClick = { activeDialog = ActiveSettingsDialog.SCAN_MODE },
            testTag = "settings_scan_mode_row",
          )
          SettingsRowDivider()

          // Concurrency inline slider row with themed track & pill badge
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              SettingsIconBadge(icon = Icons.Rounded.Speed)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.settings_concurrency_title),
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                LtrContent {
                  Text(
                    text =
                      stringResource(
                        R.string.settings_timeouts_summary,
                        settings.fullScanConnectTimeoutMs / 1000,
                        settings.fullScanReadTimeoutMs / 1000,
                        "${settings.quickScanDeadlineMs / 1000.0}s",
                      ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
              ) {
                Text(
                  text = stringResource(R.string.settings_concurrency_badge, settings.maxConcurrency),
                  style =
                    MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontFeatureSettings = "tnum",
                    ),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
              }
            }

            Slider(
              value = settings.maxConcurrency.toFloat(),
              onValueChange = { onMaxConcurrencyChange(it.roundToInt()) },
              valueRange = 1f..5f,
              steps = 3,
              colors =
                SliderDefaults.colors(
                  thumbColor = MaterialTheme.colorScheme.primary,
                  activeTrackColor = MaterialTheme.colorScheme.primary,
                  inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer,
                  activeTickColor = MaterialTheme.colorScheme.onPrimary,
                  inactiveTickColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                ),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .testTag("settings_concurrency_slider"),
            )
          }

          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.DataUsage,
            title = stringResource(R.string.settings_speed_cap_title),
            subtitle = stringResource(R.string.settings_speed_cap_value, currentCapMib),
            onClick = { activeDialog = ActiveSettingsDialog.SPEED_CAP },
            testTag = "settings_speed_cap_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.Router,
            title = stringResource(R.string.settings_network_profile_title),
            subtitle =
              settings.manualNetworkLabel.ifBlank {
                stringResource(R.string.settings_network_profile_unset)
              },
            onClick = { activeDialog = ActiveSettingsDialog.NETWORK_LABEL },
            testTag = "settings_network_profile_row",
          )
        }
      }

      // 3. Remote Signed Catalog Sync
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_catalog_section))
      }
      item {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = Icons.Rounded.Link,
            title = stringResource(R.string.settings_remote_url_title),
            subtitle =
              settings.remoteCatalogUrl.ifBlank {
                stringResource(R.string.settings_remote_url_unset)
              },
            subtitleMonospace = settings.remoteCatalogUrl.isNotBlank(),
            onClick = { activeDialog = ActiveSettingsDialog.REMOTE_URL },
            testTag = "settings_remote_url_row",
          )
          SettingsRowDivider()
          SettingsSwitchRow(
            icon = Icons.Rounded.Autorenew,
            title = stringResource(R.string.settings_auto_sync_label),
            subtitle = stringResource(R.string.settings_auto_sync_subtitle),
            checked = settings.autoCatalogUpdates,
            onCheckedChange = onAutoUpdatesChange,
            testTag = "settings_auto_sync_switch",
          )
          SettingsRowDivider()
          SettingsSwitchRow(
            icon = Icons.Rounded.Wifi,
            title = stringResource(R.string.settings_wifi_only_label),
            subtitle = stringResource(R.string.settings_wifi_only_subtitle),
            checked = settings.wifiOnlyCatalogUpdates,
            onCheckedChange = onWifiOnlyChange,
            testTag = "settings_wifi_only_switch",
          )
          SettingsRowDivider()
          SettingsSwitchRow(
            icon = Icons.Rounded.Security,
            title = stringResource(R.string.settings_cross_origin_label),
            subtitle = stringResource(R.string.settings_cross_origin_subtitle),
            checked = settings.allowCrossOriginRemotePayload,
            onCheckedChange = onCrossOriginChange,
            testTag = "settings_cross_origin_switch",
          )
          SettingsRowDivider()

          // Tonal sync status badge + full-width single-line Sync action
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = BdixSpacing.lg, vertical = BdixSpacing.md),
            verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
          ) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
              Column(
                modifier = Modifier.padding(horizontal = BdixSpacing.md, vertical = BdixSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                LtrContent {
                  Text(
                    text = "${sync.lastSyncStatus.displayLabel} · Seq #${sync.catalogSequence} · ${sync.remoteEntryCount} remote",
                    style = MonoUrlStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                sync.lastSyncMessage?.let { msg ->
                  Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                } ?: uiState.lastBuiltInVerifiedEpochMs?.let {
                  Text(
                    text =
                      stringResource(
                        R.string.settings_sync_builtin_badge,
                        uiState.totalServerCount,
                        BdOttPlatforms.all.size,
                      ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }

            val reducedMotion = LocalReducedMotion.current
            val syncAngle =
              if (uiState.isSyncing && !reducedMotion) {
                val syncSpinTransition = rememberInfiniteTransition(label = "settings_sync_spin")
                syncSpinTransition
                  .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec =
                      infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                      ),
                    label = "sync_icon_angle",
                  ).value
              } else {
                0f
              }

            Button(
              onClick = {
                if (settings.remoteCatalogUrl.isBlank()) {
                  activeDialog = ActiveSettingsDialog.SYNC_OPTIONS
                } else {
                  onSyncNowClick(settings.remoteCatalogUrl)
                }
              },
              enabled = !uiState.isSyncing,
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                  disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                  disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("settings_sync_now_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = null,
                modifier =
                  Modifier
                    .size(18.dp)
                    .graphicsLayer {
                      rotationZ = syncAngle
                    },
              )
              Spacer(modifier = Modifier.width(BdixSpacing.sm))
              Text(
                text = stringResource(R.string.action_sync_now),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }

      // 4. History, Retention & Bulk Deletion
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_data_section))
      }
      item {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = Icons.Rounded.History,
            title = stringResource(R.string.settings_retention_title),
            subtitle =
              stringResource(
                R.string.settings_retention_days_value,
                settings.historyRetentionDays,
              ),
            onClick = { activeDialog = ActiveSettingsDialog.RETENTION },
            testTag = "settings_retention_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.DeleteSweep,
            title = stringResource(R.string.history_clear_all),
            subtitle = stringResource(R.string.settings_clear_history_subtitle),
            onClick = { pendingBulkAction = BulkDeleteAction.HISTORY },
            isDestructive = true,
            testTag = "settings_clear_history_button",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.BookmarkRemove,
            title = stringResource(R.string.settings_clear_favorites),
            subtitle = stringResource(R.string.settings_clear_favorites_subtitle),
            onClick = { pendingBulkAction = BulkDeleteAction.FAVORITES },
            isDestructive = true,
            testTag = "settings_clear_favorites_button",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.DeleteOutline,
            title = stringResource(R.string.settings_clear_custom),
            subtitle = stringResource(R.string.settings_clear_custom_subtitle),
            onClick = { pendingBulkAction = BulkDeleteAction.CUSTOM_SERVERS },
            isDestructive = true,
            testTag = "settings_clear_custom_button",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.RestartAlt,
            title = stringResource(R.string.settings_reset_all),
            subtitle = stringResource(R.string.settings_reset_all_subtitle),
            onClick = { pendingBulkAction = BulkDeleteAction.RESET_ALL },
            isDestructive = true,
            testTag = "settings_reset_all_button",
          )
        }
      }

      // 6. Community, Privacy & About
      item {
        SettingsSectionHeader(title = stringResource(R.string.settings_community_section))
      }
      item {
        SettingsGroupCard {
          SettingsNavigationRow(
            icon = Icons.Rounded.VerifiedUser,
            title = stringResource(R.string.drawer_privacy_policy),
            subtitle = stringResource(R.string.settings_privacy_policy_subtitle),
            onClick = { showPrivacyPolicyDialog = true },
            testTag = "settings_privacy_policy_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.Groups,
            title = stringResource(R.string.settings_facebook_page_title),
            subtitle = stringResource(R.string.settings_facebook_page_subtitle),
            onClick = { onOpenExternalUrl(FACEBOOK_COMMUNITY_URL) },
            testTag = "settings_facebook_row",
          )
          SettingsRowDivider()
          SettingsNavigationRow(
            icon = Icons.Rounded.Share,
            title = stringResource(R.string.settings_share_app_title),
            subtitle = stringResource(R.string.settings_share_app_subtitle),
            onClick = {
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(Intent.EXTRA_SUBJECT, appName)
                  putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
              try {
                context.startActivity(Intent.createChooser(shareIntent, shareTitle))
              } catch (_: ActivityNotFoundException) {
                // Ignore when no share target exists in headless/test environments
              }
            },
            testTag = "settings_share_app_row",
          )
          SettingsRowDivider()
          SettingsInfoRow(
            icon = Icons.Rounded.Person,
            title = stringResource(R.string.settings_developer_label),
            value = stringResource(R.string.settings_developer_value),
          )
        }
      }

      // 7. Clean Centered Version & Build Footer
      item {
        SettingsVersionFooter()
      }
    }
  }

  // Modal Pickers & Input Dialogs
  when (activeDialog) {
    ActiveSettingsDialog.THEME -> {
      SettingsRadioPickerDialog(
        title = stringResource(R.string.settings_appearance_mode_dialog_title),
        options =
          ThemePreference.entries.map { pref ->
            RadioPickerOption(
              value = pref,
              label = appearanceLabel(pref),
              testTag = "theme_option_${pref.name.lowercase()}",
            )
          },
        selectedValue = settings.themePreference,
        onSelect = onSelectTheme,
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.LANGUAGE -> {
      SettingsRadioPickerDialog(
        title = stringResource(R.string.settings_language_dialog_title),
        options =
          AppLanguage.entries.map { lang ->
            RadioPickerOption(
              value = lang,
              label = lang.displayLabel,
              testTag = "language_option_${lang.name.lowercase()}",
            )
          },
        selectedValue = settings.appLanguage,
        onSelect = onSelectLanguage,
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.SCAN_MODE -> {
      SettingsRadioPickerDialog(
        title = stringResource(R.string.settings_scan_mode_dialog_title),
        options =
          listOf(
            RadioPickerOption(
              value = ScanMode.QUICK_SCAN,
              label = stringResource(R.string.action_quick_scan),
              description = stringResource(R.string.settings_scan_mode_quick_desc),
              testTag = "scan_mode_option_quick",
            ),
            RadioPickerOption(
              value = ScanMode.FULL_SCAN,
              label = stringResource(R.string.action_full_scan),
              description = stringResource(R.string.settings_scan_mode_full_desc),
              testTag = "scan_mode_option_full",
            ),
          ),
        selectedValue = settings.defaultScanMode,
        onSelect = onSelectDefaultScanMode,
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.SPEED_CAP -> {
      SettingsRadioPickerDialog(
        title = stringResource(R.string.settings_speed_cap_dialog_title),
        options =
          listOf(2, 5, 10, 20).map { mib ->
            RadioPickerOption(
              value = mib,
              label = stringResource(R.string.settings_speed_cap_value, mib),
              testTag = "speed_cap_option_$mib",
            )
          },
        selectedValue = currentCapMib,
        onSelect = { mib -> onSpeedTestMaxBytesChange(mib.toLong() * 1024L * 1024L) },
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.NETWORK_LABEL -> {
      SettingsTextInputDialog(
        title = stringResource(R.string.settings_network_profile_title),
        initialValue = settings.manualNetworkLabel,
        placeholder = stringResource(R.string.settings_network_profile_dialog_hint),
        inputTestTag = "settings_network_label_input",
        saveTestTag = "settings_network_label_save",
        onSave = onManualNetworkLabelSave,
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.REMOTE_URL -> {
      RemoteCatalogUrlDialog(
        initialUrl = settings.remoteCatalogUrl,
        onClearOrSaveUrl = onRemoteCatalogUrlSave,
        onSaveAndSync = { url ->
          val handler = onSaveRemoteCatalogUrlAndSync
          if (handler != null) {
            handler(url)
          } else {
            onRemoteCatalogUrlSave(url)
            if (url.isNotBlank()) onSyncNowClick(url)
          }
        },
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.SYNC_OPTIONS -> {
      CatalogSyncOptionsDialog(
        totalBdixCount = uiState.totalServerCount,
        totalOttCount = BdOttPlatforms.all.size,
        onRefreshBuiltIn = onRefreshBuiltInCatalog,
        onConfigureRemoteUrl = { activeDialog = ActiveSettingsDialog.REMOTE_URL },
        onDismiss = { activeDialog = null },
      )
    }

    ActiveSettingsDialog.RETENTION -> {
      SettingsRadioPickerDialog(
        title = stringResource(R.string.settings_retention_dialog_title),
        options =
          listOf(7, 30, 90, 365).map { days ->
            RadioPickerOption(
              value = days,
              label = stringResource(R.string.settings_retention_days_value, days),
              testTag = "retention_option_$days",
            )
          },
        selectedValue = settings.historyRetentionDays,
        onSelect = onHistoryRetentionChange,
        onDismiss = { activeDialog = null },
      )
    }

    null -> Unit
  }

  // Full Privacy Policy Modal Dialog
  if (showPrivacyPolicyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyPolicyDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.testTag("settings_privacy_policy_dialog"),
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
        ) {
          SettingsIconBadge(icon = Icons.Rounded.VerifiedUser)
          Text(
            text = stringResource(R.string.drawer_privacy_policy),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
          Text(
            text = stringResource(R.string.settings_privacy_policy_full_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { showPrivacyPolicyDialog = false },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier =
            Modifier
              .heightIn(min = 48.dp)
              .testTag("settings_privacy_policy_close_button"),
        ) {
          Text(stringResource(R.string.privacy_dialog_close))
        }
      },
    )
  }

  // Destructive Action Confirmation Dialog
  pendingBulkAction?.let { action ->
    AlertDialog(
      onDismissRequest = { pendingBulkAction = null },
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
      title = {
        Text(
          text = stringResource(R.string.settings_confirm_action_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      },
      text = {
        Text(
          text =
            when (action) {
              BulkDeleteAction.HISTORY -> stringResource(R.string.history_clear_confirm_body)
              BulkDeleteAction.FAVORITES -> stringResource(R.string.settings_confirm_clear_favorites)
              BulkDeleteAction.CUSTOM_SERVERS -> stringResource(R.string.settings_confirm_clear_custom)
              BulkDeleteAction.RESET_ALL -> stringResource(R.string.settings_confirm_reset_all)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      },
      confirmButton = {
        Button(
          onClick = {
            when (action) {
              BulkDeleteAction.HISTORY -> onClearHistory()
              BulkDeleteAction.FAVORITES -> onClearFavorites()
              BulkDeleteAction.CUSTOM_SERVERS -> onDeleteCustomServers()
              BulkDeleteAction.RESET_ALL -> onResetAllData()
            }
            pendingBulkAction = null
          },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            ),
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_confirm))
        }
      },
      dismissButton = {
        TextButton(
          onClick = { pendingBulkAction = null },
          modifier = Modifier.heightIn(min = 48.dp),
        ) {
          Text(stringResource(R.string.action_cancel))
        }
      },
    )
  }
}

@Composable
private fun SettingsAppProfileHeaderCard(
  networkSnapshot: NetworkSnapshot,
  totalBdixServers: Int,
  totalOttPlatforms: Int,
) {
  val semantic = LocalBdixSemanticColors.current
  val isConnected = networkSnapshot.isConnected
  val transportLabel =
    when (networkSnapshot.transportType) {
      TransportType.WIFI -> stringResource(R.string.home_network_wifi)
      TransportType.CELLULAR -> stringResource(R.string.home_network_mobile)
      TransportType.ETHERNET -> stringResource(R.string.home_network_ethernet)
      else -> stringResource(R.string.home_network_offline)
    }
  val ipDisplay =
    networkSnapshot.localIpAddress?.takeIf { it.isNotBlank() }
      ?: stringResource(R.string.drawer_ip_unavailable)

  Surface(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .testTag("settings_app_header_card"),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 2.dp,
    border = BorderStroke(BdixBorder.hairline, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(BdixSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(54.dp)
              .background(
                brush =
                  Brush.linearGradient(
                    colors =
                      listOf(
                        semantic.ctaGradientStart,
                        semantic.ctaGradientEnd,
                      ),
                  ),
                shape = RoundedCornerShape(16.dp),
              ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Radar,
            contentDescription = null,
            tint = semantic.ctaContent,
            modifier = Modifier.size(30.dp),
          )
        }

        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
          ) {
            Text(
              text =
                stringResource(
                  R.string.settings_header_catalog_summary,
                  totalBdixServers,
                  totalOttPlatforms,
                ),
              style =
                MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.SemiBold,
                  fontFeatureSettings = "tnum",
                ),
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      // Live Network & IP Strip
      Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border =
          BorderStroke(
            BdixBorder.hairline,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
          ),
      ) {
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = BdixSpacing.md, vertical = BdixSpacing.sm),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            PulsingStatusDot(
              color = if (isConnected) semantic.reachable else semantic.unreachable,
              isLive = isConnected,
              dotSize = 8.dp,
            )
            Text(
              text = transportLabel,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          LtrContent {
            Text(
              text = stringResource(R.string.drawer_ip_label, ipDisplay),
              style = MonoUrlStyle.copy(fontWeight = FontWeight.Medium),
              color = semantic.ipAccent,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsVersionFooter() {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .padding(top = BdixSpacing.md, bottom = BdixSpacing.lg)
        .testTag("settings_version_footer"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text =
        stringResource(
          R.string.settings_footer_version_build,
          BuildConfig.VERSION_NAME,
          BuildConfig.VERSION_CODE,
        ),
      style =
        MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontFeatureSettings = "tnum",
        ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    LtrContent {
      Text(
        text = BuildConfig.APPLICATION_ID,
        style = MonoUrlStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
  BdixTesterTheme(
    themePreset = AppThemePreset.MIDNIGHT_INDIGO,
    themePreference = ThemePreference.DARK,
  ) {
    SettingsScreen(
      uiState = SettingsUiState(),
      onNavigateBack = {},
      onSelectThemePreset = {},
      onSelectTheme = {},
      onSelectLanguage = {},
      onSelectDefaultScanMode = {},
      onMaxConcurrencyChange = {},
      onSpeedTestMaxBytesChange = {},
      onManualNetworkLabelSave = {},
      onRemoteCatalogUrlSave = {},
      onAutoUpdatesChange = {},
      onWifiOnlyChange = {},
      onCrossOriginChange = {},
      onSyncNowClick = {},
      onHistoryRetentionChange = {},
      onClearHistory = {},
      onClearFavorites = {},
      onDeleteCustomServers = {},
      onResetAllData = {},
    )
  }
}
