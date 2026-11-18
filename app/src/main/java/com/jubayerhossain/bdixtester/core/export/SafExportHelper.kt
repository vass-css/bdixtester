package com.jubayerhossain.bdixtester.core.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.jubayerhossain.bdixtester.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object SafExportHelper {
  suspend fun writeUtf8ToUri(
    context: Context,
    uri: Uri,
    content: String,
  ): Boolean =
    withContext(Dispatchers.IO) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
          stream.write(content.toByteArray(Charsets.UTF_8))
          stream.flush()
        } ?: return@withContext false
        true
      } catch (_: IOException) {
        false
      } catch (_: SecurityException) {
        false
      }
    }

  fun showWriteFeedback(
    context: Context,
    success: Boolean,
  ) {
    val msg =
      if (success) {
        context.getString(R.string.export_success)
      } else {
        context.getString(R.string.export_failed)
      }
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
  }
}
