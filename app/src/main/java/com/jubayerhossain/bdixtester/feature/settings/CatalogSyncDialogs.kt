package com.jubayerhossain.bdixtester.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.ui.components.LtrContent
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing

@Composable
internal fun CatalogSyncOptionsDialog(
  totalBdixCount: Int,
  totalOttCount: Int,
  onRefreshBuiltIn: () -> Unit,
  onConfigureRemoteUrl: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.testTag("settings_sync_options_dialog"),
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BdixSpacing.md),
      ) {
        SettingsIconBadge(icon = Icons.Rounded.Sync)
        Text(
          text = stringResource(R.string.settings_sync_options_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.md),
      ) {
        Text(
          text = stringResource(R.string.settings_sync_options_body),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
          onClick = {
            onDismiss()
            onRefreshBuiltIn()
          },
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("sync_option_refresh_builtin"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(BdixSpacing.sm))
          Text(
            text = stringResource(R.string.settings_sync_option_builtin, totalBdixCount, totalOttCount),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        OutlinedButton(
          onClick = {
            onDismiss()
            onConfigureRemoteUrl()
          },
          shape = RoundedCornerShape(14.dp),
          modifier =
            Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("sync_option_configure_remote"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Link,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(BdixSpacing.sm))
          Text(
            text = stringResource(R.string.settings_sync_option_remote),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text(
          text = stringResource(R.string.action_cancel),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
}

@Composable
internal fun RemoteCatalogUrlDialog(
  initialUrl: String,
  onClearOrSaveUrl: (String) -> Unit,
  onSaveAndSync: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var draft by remember(initialUrl) { mutableStateOf(initialUrl) }
  val trimmed = draft.trim()

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    title = {
      Text(
        text = stringResource(R.string.settings_remote_url_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    },
    text = {
      LtrContent {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          placeholder = { Text(stringResource(R.string.settings_remote_url_dialog_hint)) },
          singleLine = true,
          trailingIcon = {
            if (draft.isNotEmpty()) {
              IconButton(
                onClick = { draft = "" },
                modifier = Modifier.minimumInteractiveComponentSize(),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Clear,
                  contentDescription = stringResource(R.string.action_clear_filters),
                )
              }
            }
          },
          shape = BdixCardShapes.input,
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("settings_remote_url_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (trimmed.isNotEmpty()) {
            onSaveAndSync(trimmed)
          } else {
            onClearOrSaveUrl("")
          }
          onDismiss()
        },
        colors =
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag("settings_remote_url_save"),
      ) {
        Text(
          text =
            if (trimmed.isNotEmpty()) {
              stringResource(R.string.settings_sync_save_and_sync)
            } else {
              stringResource(R.string.action_save)
            },
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text(
          text = stringResource(R.string.action_cancel),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
  )
}
