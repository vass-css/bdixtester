package com.jubayerhossain.bdixtester.data.remote

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jubayerhossain.bdixtester.MainActivity
import com.jubayerhossain.bdixtester.R

object VerificationNotificationHelper {
  const val CHANNEL_ID = "bdix_verification_status"
  private const val SUMMARY_NOTIFICATION_ID = 4102

  fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java) ?: return
      if (manager.getNotificationChannel(CHANNEL_ID) != null) return
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          context.getString(R.string.governance_notif_channel_name),
          NotificationManager.IMPORTANCE_LOW,
        ).apply {
          description = context.getString(R.string.governance_notif_channel_desc)
          setShowBadge(false)
        }
      manager.createNotificationChannel(channel)
    }
  }

  fun canPostNotifications(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }.getOrDefault(false)
  }

  fun postSummaryNotification(
    context: Context,
    checkedCount: Int,
    candidateCount: Int,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannel(context)

    val launchIntent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    val pendingIntent =
      PendingIntent.getActivity(
        context,
        SUMMARY_NOTIFICATION_ID,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val bodyText =
      if (candidateCount > 0) {
        context.getString(R.string.governance_notif_summary_candidates, checkedCount, candidateCount)
      } else {
        context.getString(R.string.governance_notif_summary_clean, checkedCount)
      }

    val notification =
      NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(context.getString(R.string.governance_notif_title))
        .setContentText(bodyText)
        .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(pendingIntent)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
    }
  }

  fun postFailureNotification(
    context: Context,
    errorDetail: String,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannel(context)

    val bodyText = context.getString(R.string.governance_notif_failed, errorDetail)
    val notification =
      NotificationCompat
        .Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_error)
        .setContentTitle(context.getString(R.string.governance_notif_title))
        .setContentText(bodyText)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .build()

    runCatching {
      NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
    }
  }
}
