package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.model.ServerCategory
import com.jubayerhossain.bdixtester.domain.model.ServerEndpoint
import com.jubayerhossain.bdixtester.ui.theme.BdixSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomServerDialog(
  initialEndpoint: ServerEndpoint?,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onSave: (
    existingId: String?,
    name: String,
    rawUrl: String,
    category: ServerCategory,
    speedTestUrl: String?,
  ) -> Unit,
) {
  var name by remember(initialEndpoint) { mutableStateOf(initialEndpoint?.name.orEmpty()) }
  var rawUrl by remember(initialEndpoint) { mutableStateOf(initialEndpoint?.originalUrl.orEmpty()) }
  var speedUrl by remember(initialEndpoint) { mutableStateOf(initialEndpoint?.speedTestUrl.orEmpty()) }
  val selectableCategories =
    remember {
      listOf(
        ServerCategory.FTP_FILES,
        ServerCategory.MOVIES,
        ServerCategory.LIVE_TV,
      )
    }
  var selectedCategory by remember(initialEndpoint) {
    mutableStateOf(
      initialEndpoint?.category?.takeIf { it in selectableCategories }
        ?: ServerCategory.FTP_FILES,
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag("custom_server_dialog"),
    title = {
      Text(
        stringResource(
          if (initialEndpoint == null) {
            R.string.servers_dialog_add_title
          } else {
            R.string.servers_dialog_edit_title
          },
        ),
      )
    },
    text = {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(BdixSpacing.sm),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text(stringResource(R.string.servers_dialog_name_label)) },
          singleLine = true,
          modifier =
            Modifier
              .fillMaxWidth()
              .testTag("custom_server_name_input"),
        )
        LtrContent {
          OutlinedTextField(
            value = rawUrl,
            onValueChange = { rawUrl = it },
            label = { Text(stringResource(R.string.servers_dialog_url_label)) },
            singleLine = true,
            modifier =
              Modifier
                .fillMaxWidth()
                .testTag("custom_server_url_input"),
          )
        }
        LtrContent {
          OutlinedTextField(
            value = speedUrl,
            onValueChange = { speedUrl = it },
            label = { Text(stringResource(R.string.servers_dialog_speed_url_label)) },
            singleLine = true,
            modifier =
              Modifier
                .fillMaxWidth()
                .testTag("custom_server_speed_url_input"),
          )
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          selectableCategories.forEach { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { selectedCategory = category },
              label = { Text(category.displayName) },
              modifier =
                Modifier
                  .heightIn(min = 48.dp)
                  .testTag("custom_server_category_${category.name.lowercase()}"),
            )
          }
        }

        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("custom_server_error_text"),
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            initialEndpoint?.id,
            name,
            rawUrl,
            selectedCategory,
            speedUrl.takeIf { it.isNotBlank() },
          )
        },
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag("custom_server_save_button"),
      ) {
        Text(stringResource(R.string.action_save))
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier =
          Modifier
            .heightIn(min = 48.dp)
            .testTag("custom_server_cancel_button"),
      ) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}
