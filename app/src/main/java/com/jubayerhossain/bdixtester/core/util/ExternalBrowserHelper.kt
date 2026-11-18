package com.jubayerhossain.bdixtester.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import com.jubayerhossain.bdixtester.R

sealed interface BrowserLaunchResult {
  data object Launched : BrowserLaunchResult

  data object UnsupportedFtp : BrowserLaunchResult

  data object InvalidUrl : BrowserLaunchResult

  data object NoBrowserAvailable : BrowserLaunchResult

  data object LaunchFailed : BrowserLaunchResult
}

object ExternalBrowserHelper {
  fun parseValidBrowserUri(rawUrl: String): Uri? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null
    val uri =
      try {
        trimmed.toUri()
      } catch (_: Exception) {
        null
      } ?: return null
    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "http" && scheme != "https") return null
    if (uri.host.isNullOrBlank()) return null
    return uri
  }

  fun isAllowedBrowserUrl(rawUrl: String): Boolean = parseValidBrowserUri(rawUrl) != null

  fun createExternalBrowserIntent(
    rawUrl: String,
    requireNewTask: Boolean = false,
  ): Intent? {
    val uri = parseValidBrowserUri(rawUrl) ?: return null
    return Intent(Intent.ACTION_VIEW, uri).apply {
      addCategory(Intent.CATEGORY_BROWSABLE)
      if (requireNewTask) {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    }
  }

  fun launchUrl(
    context: Context,
    rawUrl: String,
  ): BrowserLaunchResult {
    val trimmed = rawUrl.trim()
    val parsed =
      try {
        trimmed.toUri()
      } catch (_: Exception) {
        null
      }
    val scheme = parsed?.scheme?.lowercase()
    if (scheme == "ftp" || scheme == "ftps") {
      return BrowserLaunchResult.UnsupportedFtp
    }

    val intent =
      createExternalBrowserIntent(
        rawUrl = trimmed,
        requireNewTask = context !is Activity,
      ) ?: return BrowserLaunchResult.InvalidUrl

    val resolved =
      try {
        intent.resolveActivity(context.packageManager)
      } catch (_: Exception) {
        null
      }
    if (resolved == null) {
      return BrowserLaunchResult.NoBrowserAvailable
    }

    return try {
      context.startActivity(intent)
      BrowserLaunchResult.Launched
    } catch (_: ActivityNotFoundException) {
      BrowserLaunchResult.NoBrowserAvailable
    } catch (_: SecurityException) {
      BrowserLaunchResult.LaunchFailed
    } catch (_: Exception) {
      BrowserLaunchResult.LaunchFailed
    }
  }

  @StringRes
  fun errorMessageRes(result: BrowserLaunchResult): Int? =
    when (result) {
      BrowserLaunchResult.Launched -> null
      BrowserLaunchResult.UnsupportedFtp -> R.string.details_open_browser_ftp_unsupported
      BrowserLaunchResult.InvalidUrl -> R.string.details_open_browser_invalid_scheme
      BrowserLaunchResult.NoBrowserAvailable -> R.string.details_open_browser_error
      BrowserLaunchResult.LaunchFailed -> R.string.details_open_browser_failed
    }

  fun openUrl(
    context: Context,
    rawUrl: String,
  ): Boolean {
    val result = launchUrl(context, rawUrl)
    if (result is BrowserLaunchResult.Launched) return true
    errorMessageRes(result)?.let { resId ->
      Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }
    return false
  }
}
