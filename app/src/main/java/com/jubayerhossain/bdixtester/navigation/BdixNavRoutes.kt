package com.jubayerhossain.bdixtester.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.jubayerhossain.bdixtester.R

object BdixRoutes {
  const val HOME = "dashboard"
  const val SERVERS = "servers"
  const val SCAN = "scan"
  const val FAVORITES = "favorites"
  const val HISTORY = "history"
  const val SETTINGS = "settings"
  const val CATALOG_GOVERNANCE = "catalog_governance"
}

enum class TopLevelDestination(
  val route: String,
  @StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
) {
  HOME(
    route = BdixRoutes.HOME,
    labelRes = R.string.nav_home,
    selectedIcon = Icons.Rounded.Home,
    unselectedIcon = Icons.Outlined.Home,
    testTag = "nav_home",
  ),
  SERVERS(
    route = BdixRoutes.SERVERS,
    labelRes = R.string.nav_servers,
    selectedIcon = Icons.Rounded.Dns,
    unselectedIcon = Icons.Outlined.Dns,
    testTag = "nav_servers",
  ),
  SAVED(
    route = BdixRoutes.FAVORITES,
    labelRes = R.string.nav_favorites,
    selectedIcon = Icons.Rounded.Bookmark,
    unselectedIcon = Icons.Outlined.BookmarkBorder,
    testTag = "nav_saved",
  ),
  HISTORY(
    route = BdixRoutes.HISTORY,
    labelRes = R.string.nav_history,
    selectedIcon = Icons.Rounded.History,
    unselectedIcon = Icons.Outlined.History,
    testTag = "nav_history",
  ),
  SETTINGS(
    route = BdixRoutes.SETTINGS,
    labelRes = R.string.nav_settings,
    selectedIcon = Icons.Rounded.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    testTag = "nav_settings",
  ),
}
