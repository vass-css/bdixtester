package com.jubayerhossain.bdixtester.navigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.di.AppContainer
import com.jubayerhossain.bdixtester.core.model.SyncStatus
import com.jubayerhossain.bdixtester.core.util.BrowserLaunchResult
import com.jubayerhossain.bdixtester.core.util.ExternalBrowserHelper
import com.jubayerhossain.bdixtester.feature.dashboard.DashboardScreen
import com.jubayerhossain.bdixtester.feature.dashboard.DashboardViewModel
import com.jubayerhossain.bdixtester.feature.favorites.FavoritesViewModel
import com.jubayerhossain.bdixtester.feature.favorites.HistoryScreen
import com.jubayerhossain.bdixtester.feature.favorites.SavedServersScreen
import com.jubayerhossain.bdixtester.feature.governance.CatalogGovernanceScreen
import com.jubayerhossain.bdixtester.feature.governance.CatalogGovernanceViewModel
import com.jubayerhossain.bdixtester.feature.scan.ScanViewModel
import com.jubayerhossain.bdixtester.feature.servers.ServersScreen
import com.jubayerhossain.bdixtester.feature.servers.ServersViewModel
import com.jubayerhossain.bdixtester.feature.settings.SettingsScreen
import com.jubayerhossain.bdixtester.feature.settings.SettingsSyncFeedback
import com.jubayerhossain.bdixtester.feature.settings.SettingsViewModel
import com.jubayerhossain.bdixtester.ui.components.BdixMotionSpecs
import com.jubayerhossain.bdixtester.ui.components.CustomServerDialog
import com.jubayerhossain.bdixtester.ui.components.LocalReducedMotion
import com.jubayerhossain.bdixtester.ui.theme.BdixBorder
import com.jubayerhossain.bdixtester.ui.theme.BdixIconSize
import com.jubayerhossain.bdixtester.ui.theme.LocalBdixSemanticColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun BdixTesterAppRoot(
  appContainer: AppContainer,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val navController = rememberNavController()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val topLevelRoutes = remember { TopLevelDestination.entries.map { it.route }.toSet() }
  val isTopLevelDestination = currentRoute == null || currentRoute in topLevelRoutes

  var showAddServerDialog by remember { mutableStateOf(false) }

  val dashboardViewModel: DashboardViewModel =
    viewModel(
      factory =
        DashboardViewModel.factory(
          catalogRepository = appContainer.catalogRepository,
          connectivityObserver = appContainer.connectivityObserver,
          settingsRepository = appContainer.settingsRepository,
        ),
    )
  val serversViewModel: ServersViewModel =
    viewModel(
      factory =
        ServersViewModel.factory(
          catalogRepository = appContainer.catalogRepository,
          scanCoordinator = appContainer.scanCoordinator,
          settingsRepository = appContainer.settingsRepository,
        ),
    )
  val scanViewModel: ScanViewModel =
    viewModel(
      factory =
        ScanViewModel.factory(
          catalogRepository = appContainer.catalogRepository,
          scanCoordinator = appContainer.scanCoordinator,
          settingsRepository = appContainer.settingsRepository,
        ),
    )
  val favoritesViewModel: FavoritesViewModel =
    viewModel(factory = FavoritesViewModel.factory(appContainer.catalogRepository))
  val settingsViewModel: SettingsViewModel =
    viewModel(factory = SettingsViewModel.factory(appContainer))
  val catalogGovernanceViewModel: CatalogGovernanceViewModel =
    viewModel(factory = CatalogGovernanceViewModel.factory(appContainer, context))

  val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
  val serversUiState by serversViewModel.uiState.collectAsStateWithLifecycle()
  val ftpUnsupportedMessage = stringResource(R.string.details_open_browser_ftp_unsupported)
  val invalidUrlMessage = stringResource(R.string.details_open_browser_invalid_scheme)
  val browserErrorMessage = stringResource(R.string.details_open_browser_error)
  val browserLaunchFailedMessage = stringResource(R.string.details_open_browser_failed)

  fun navigateToTopLevel(destinationRoute: String) {
    if (currentRoute == destinationRoute) return
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    navController.navigate(destinationRoute) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  fun copyUrlWithSnackbar(rawUrl: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("BDIX Server URL", rawUrl))
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    val copiedMessage = context.getString(R.string.servers_url_copied, rawUrl)
    scope.launch {
      snackbarHostState.showSnackbar(
        message = copiedMessage,
        duration = SnackbarDuration.Short,
      )
    }
  }

  fun openExternalUrl(rawUrl: String) {
    val result = ExternalBrowserHelper.launchUrl(context, rawUrl)
    if (result !is BrowserLaunchResult.Launched) {
      val errorMessage =
        when (result) {
          BrowserLaunchResult.UnsupportedFtp -> ftpUnsupportedMessage
          BrowserLaunchResult.InvalidUrl -> invalidUrlMessage
          BrowserLaunchResult.NoBrowserAvailable -> browserErrorMessage
          BrowserLaunchResult.LaunchFailed -> browserLaunchFailedMessage
          BrowserLaunchResult.Launched -> browserLaunchFailedMessage
        }
      scope.launch {
        snackbarHostState.showSnackbar(
          message = errorMessage,
          duration = SnackbarDuration.Short,
        )
      }
    }
  }

  LaunchedEffect(settingsViewModel) {
    settingsViewModel.syncFeedback.collect { feedback ->
      val feedbackText =
        when (feedback) {
          is SettingsSyncFeedback.BuiltInRefreshed -> {
            context.getString(
              R.string.settings_sync_builtin_verified,
              feedback.bdixCount,
              feedback.ottCount,
            )
          }

          is SettingsSyncFeedback.RemoteSyncCompleted -> {
            when (feedback.status) {
              SyncStatus.UPDATED -> {
                context.getString(R.string.settings_sync_feedback_updated, feedback.entryCount)
              }

              SyncStatus.NOT_MODIFIED -> {
                context.getString(R.string.settings_sync_feedback_not_modified)
              }

              SyncStatus.WIFI_REQUIRED -> {
                context.getString(R.string.settings_sync_feedback_wifi_required)
              }

              else -> {
                context.getString(R.string.settings_sync_feedback_failed, feedback.message)
              }
            }
          }
        }
      snackbarHostState.showSnackbar(
        message = feedbackText,
        duration = SnackbarDuration.Short,
      )
    }
  }

  fun triggerCatalogSyncFromToolbar() {
    scope.launch {
      val remoteUrl = appContainer.settingsRepository.settings.first().remoteCatalogUrl
      if (remoteUrl.isNotBlank()) {
        settingsViewModel.syncCatalogNow(remoteUrl)
      } else {
        settingsViewModel.refreshBuiltInCatalogNow()
      }
    }
  }

  val reducedMotion = LocalReducedMotion.current

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    val navHostContent: @Composable (Modifier) -> Unit = { contentModifier ->
      NavHost(
        navController = navController,
        startDestination = BdixRoutes.HOME,
        enterTransition = {
          if (reducedMotion) {
            EnterTransition.None
          } else {
            fadeIn(animationSpec = tween(220)) +
              slideInVertically(animationSpec = tween(220)) { it / 32 }
          }
        },
        exitTransition = {
          if (reducedMotion) ExitTransition.None else fadeOut(animationSpec = tween(160))
        },
        popEnterTransition = {
          if (reducedMotion) {
            EnterTransition.None
          } else {
            fadeIn(animationSpec = tween(220)) +
              slideInVertically(animationSpec = tween(220)) { it / 32 }
          }
        },
        popExitTransition = {
          if (reducedMotion) ExitTransition.None else fadeOut(animationSpec = tween(160))
        },
        modifier = contentModifier.fillMaxSize(),
      ) {
        composable(BdixRoutes.HOME) {
          DashboardScreen(
            uiState = dashboardUiState,
            onViewServers = {
              serversViewModel.prepareForViewing()
              navigateToTopLevel(BdixRoutes.SERVERS)
            },
            onStartScan = { mode ->
              scanViewModel.startScan(overrideMode = mode)
            },
            onStopScan = {
              scanViewModel.stopScan()
            },
          )
        }

        composable(BdixRoutes.SERVERS) {
          ServersScreen(
            uiState = serversUiState,
            onSelectStatusTab = serversViewModel::selectStatusTab,
            onSelectBottomTab = serversViewModel::selectBottomTab,
            onSelectOttRegion = serversViewModel::selectOttRegion,
            onSearchQueryChange = serversViewModel::updateSearchQuery,
            onClearFilters = serversViewModel::clearFilters,
            onToggleFavorite = serversViewModel::toggleFavorite,
            onAddCustomServerClick = {
              serversViewModel.clearCustomServerError()
              showAddServerDialog = true
            },
            onSyncCatalogClick = ::triggerCatalogSyncFromToolbar,
            onStartTestClick = {
              navigateToTopLevel(BdixRoutes.HOME)
              scanViewModel.startScan(overrideMode = dashboardUiState.selectedScanMode)
            },
            onOpenUrl = ::openExternalUrl,
            onCopyUrl = ::copyUrlWithSnackbar,
            onTestSingleServer = serversViewModel::testSingleServer,
          )
        }

        composable(BdixRoutes.FAVORITES) {
          val favoritesUiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
          SavedServersScreen(
            uiState = favoritesUiState,
            onSearchQueryChange = favoritesViewModel::updateSearchQuery,
            onToggleFavorite = favoritesViewModel::toggleFavorite,
            onSetPinned = favoritesViewModel::setPinned,
            onOpenUrl = ::openExternalUrl,
            onExportFavorites = favoritesViewModel::exportFavorites,
            onCopyUrl = ::copyUrlWithSnackbar,
            onTestSingleServer = serversViewModel::testSingleServer,
          )
        }

        composable(BdixRoutes.HISTORY) {
          val favoritesUiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
          HistoryScreen(
            uiState = favoritesUiState,
            onSearchQueryChange = favoritesViewModel::updateSearchQuery,
            onSelectStatusFilter = favoritesViewModel::selectHistoryStatusFilter,
            onSelectProtocolFilter = favoritesViewModel::selectHistoryProtocolFilter,
            onOpenUrl = ::openExternalUrl,
            onDeleteHistoryEntry = favoritesViewModel::deleteHistoryEntry,
            onClearAllHistory = favoritesViewModel::clearAllHistory,
            onExportHistory = favoritesViewModel::exportHistory,
            onCopyUrl = ::copyUrlWithSnackbar,
          )
        }

        composable(BdixRoutes.SETTINGS) {
          val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
          SettingsScreen(
            uiState = settingsUiState,
            onSelectThemePreset = settingsViewModel::selectThemePreset,
            onSelectTheme = settingsViewModel::selectTheme,
            onSelectLanguage = settingsViewModel::selectLanguage,
            onReduceAnimationsChange = settingsViewModel::setReduceAnimations,
            onSelectDefaultScanMode = settingsViewModel::selectDefaultScanMode,
            onMaxConcurrencyChange = settingsViewModel::setMaxConcurrency,
            onSpeedTestMaxBytesChange = settingsViewModel::setSpeedTestMaxBytes,
            onManualNetworkLabelSave = settingsViewModel::setManualNetworkLabel,
            onRemoteCatalogUrlSave = settingsViewModel::setRemoteCatalogUrl,
            onSaveRemoteCatalogUrlAndSync = settingsViewModel::saveRemoteUrlAndSync,
            onRefreshBuiltInCatalog = settingsViewModel::refreshBuiltInCatalogNow,
            onAutoUpdatesChange = settingsViewModel::setAutoCatalogUpdates,
            onWifiOnlyChange = settingsViewModel::setWifiOnlyCatalogUpdates,
            onCrossOriginChange = settingsViewModel::setAllowCrossOriginPayload,
            onSyncNowClick = settingsViewModel::syncCatalogNow,
            onHistoryRetentionChange = settingsViewModel::setHistoryRetentionDays,
            onClearHistory = settingsViewModel::clearAllHistory,
            onClearFavorites = settingsViewModel::clearAllFavorites,
            onDeleteCustomServers = settingsViewModel::deleteAllCustomServers,
            onResetAllData = settingsViewModel::resetAllLocalData,
            onUpdateServerListClick = ::triggerCatalogSyncFromToolbar,
            onAddServerRequestClick = {
              serversViewModel.clearCustomServerError()
              showAddServerDialog = true
            },
            onOpenCatalogGovernance = {
              navController.navigate(BdixRoutes.CATALOG_GOVERNANCE) {
                launchSingleTop = true
              }
            },
            onOpenExternalUrl = ::openExternalUrl,
          )
        }

        composable(BdixRoutes.CATALOG_GOVERNANCE) {
          val governanceUiState by catalogGovernanceViewModel.uiState.collectAsStateWithLifecycle()
          CatalogGovernanceScreen(
            uiState = governanceUiState,
            onNavigateBack = { navController.popBackStack() },
            onSelectFilter = catalogGovernanceViewModel::selectFilter,
            onSearchQueryChange = catalogGovernanceViewModel::updateSearchQuery,
            onRunVerification = { catalogGovernanceViewModel.runSafeVerification() },
            onImportIspProbesJson = catalogGovernanceViewModel::importIspProbesJson,
            onOpenEvidence = catalogGovernanceViewModel::openEvidenceSheet,
            onDismissEvidence = catalogGovernanceViewModel::closeEvidenceSheet,
            onVerifySingle = catalogGovernanceViewModel::verifySingleEndpoint,
            onMarkCandidate = catalogGovernanceViewModel::markRemovalCandidate,
            onArchive = catalogGovernanceViewModel::archiveEndpoint,
            onRestore = catalogGovernanceViewModel::restoreEndpoint,
            observeEventsFor = catalogGovernanceViewModel::observeEventsFor,
          )
        }
      }
    }

    if (useNavigationRail) {
      Row(modifier = Modifier.fillMaxSize()) {
        if (isTopLevelDestination) {
          BdixNavigationRail(
            currentRoute = currentRoute,
            onSelectDestination = ::navigateToTopLevel,
          )
        }
        Scaffold(
          modifier =
            Modifier
              .weight(1f)
              .fillMaxHeight()
              .imePadding(),
          containerColor = MaterialTheme.colorScheme.background,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          snackbarHost = { BdixThemedSnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
            navHostContent(Modifier)
          }
        }
      }
    } else {
      Scaffold(
        modifier =
          Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { BdixThemedSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          AnimatedVisibility(
            visible = isTopLevelDestination,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
          ) {
            BdixBottomNavigationBar(
              currentRoute = currentRoute,
              onSelectDestination = ::navigateToTopLevel,
            )
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
          navHostContent(Modifier)
        }
      }
    }
  }

  if (showAddServerDialog) {
    CustomServerDialog(
      initialEndpoint = null,
      errorMessage = serversUiState.customServerError,
      onDismiss = {
        showAddServerDialog = false
        serversViewModel.clearCustomServerError()
      },
      onSave = { existingId, name, rawUrl, category, speedUrl ->
        serversViewModel.saveCustomServer(
          existingId = existingId,
          name = name,
          rawUrl = rawUrl,
          category = category,
          speedTestUrl = speedUrl,
        ) {
          showAddServerDialog = false
        }
      },
    )
  }
}

@Composable
private fun BdixThemedSnackbarHost(hostState: SnackbarHostState) {
  val semantic = LocalBdixSemanticColors.current
  SnackbarHost(hostState = hostState) { data ->
    Snackbar(
      snackbarData = data,
      containerColor = semantic.snackbarBackground,
      contentColor = semantic.onSnackbar,
      actionColor = MaterialTheme.colorScheme.primaryContainer,
    )
  }
}

@Composable
private fun BdixBottomNavigationBar(
  currentRoute: String?,
  onSelectDestination: (String) -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  val activeRoute = currentRoute ?: BdixRoutes.HOME
  Column(modifier = Modifier.fillMaxWidth()) {
    HorizontalDivider(
      thickness = BdixBorder.hairline,
      color = MaterialTheme.colorScheme.outlineVariant,
    )
    NavigationBar(
      containerColor = semantic.navBackground,
      tonalElevation = 0.dp,
      windowInsets = WindowInsets.navigationBars,
      modifier = Modifier.testTag("main_bottom_navigation_bar"),
    ) {
    val colors =
      NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = semantic.navIndicator,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
      )

    val reducedMotion = LocalReducedMotion.current
    TopLevelDestination.entries.forEach { destination ->
      val selected = activeRoute == destination.route
      val label = stringResource(destination.labelRes)
      val iconScale by animateFloatAsState(
        targetValue = if (selected && !reducedMotion) 1.12f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BdixMotionSpecs.bouncySpring,
        label = "nav_bar_icon_scale_${destination.route}",
      )
      NavigationBarItem(
        selected = selected,
        onClick = { onSelectDestination(destination.route) },
        icon = {
          Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = label,
            modifier =
              Modifier
                .size(BdixIconSize.lg)
                .graphicsLayer {
                  scaleX = iconScale
                  scaleY = iconScale
                },
          )
        },
        label = {
          Text(
            text = label,
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
          )
        },
        alwaysShowLabel = true,
        colors = colors,
        modifier = Modifier.testTag(destination.testTag),
      )
    }
  }
  }
}

@Composable
private fun BdixNavigationRail(
  currentRoute: String?,
  onSelectDestination: (String) -> Unit,
) {
  val semantic = LocalBdixSemanticColors.current
  val activeRoute = currentRoute ?: BdixRoutes.HOME
  Row(modifier = Modifier.fillMaxHeight()) {
    NavigationRail(
      containerColor = semantic.navBackground,
      modifier =
        Modifier
          .fillMaxHeight()
          .statusBarsPadding()
          .testTag("main_navigation_rail"),
    ) {
    Spacer(modifier = Modifier.weight(0.5f))
    val colors =
      NavigationRailItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = semantic.navIndicator,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    val reducedMotion = LocalReducedMotion.current
    TopLevelDestination.entries.forEach { destination ->
      val selected = activeRoute == destination.route
      val label = stringResource(destination.labelRes)
      val iconScale by animateFloatAsState(
        targetValue = if (selected && !reducedMotion) 1.12f else 1f,
        animationSpec = if (reducedMotion) tween(0) else BdixMotionSpecs.bouncySpring,
        label = "nav_rail_icon_scale_${destination.route}",
      )
      NavigationRailItem(
        selected = selected,
        onClick = { onSelectDestination(destination.route) },
        icon = {
          Icon(
            imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = label,
            modifier =
              Modifier
                .size(BdixIconSize.lg)
                .graphicsLayer {
                  scaleX = iconScale
                  scaleY = iconScale
                },
          )
        },
        label = {
          Text(
            text = label,
            style =
              MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
        colors = colors,
        modifier = Modifier.testTag(destination.testTag),
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    }
    VerticalDivider(
      thickness = BdixBorder.hairline,
      color = MaterialTheme.colorScheme.outlineVariant,
    )
  }
}
