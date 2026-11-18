package com.jubayerhossain.bdixtester.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.core.export.CatalogExportSerializer
import com.jubayerhossain.bdixtester.core.export.ExportDataset
import com.jubayerhossain.bdixtester.core.export.ExportFormat
import com.jubayerhossain.bdixtester.core.export.SafExportHelper
import kotlinx.coroutines.launch

@Composable
fun ExportMenuButton(
  dataset: ExportDataset,
  onGeneratePayload: (ExportFormat) -> String,
  modifier: Modifier = Modifier,
  buttonTestTag: String = "export_button",
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var menuExpanded by remember { mutableStateOf(false) }
  var pendingFormat by remember { mutableStateOf(ExportFormat.CSV) }

  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
      if (uri != null) {
        scope.launch {
          val payload = onGeneratePayload(pendingFormat)
          val ok = SafExportHelper.writeUtf8ToUri(context, uri, payload)
          SafExportHelper.showWriteFeedback(context, ok)
        }
      }
    }

  Box(modifier = modifier) {
    OutlinedButton(
      onClick = { menuExpanded = true },
      modifier =
        Modifier
          .heightIn(min = 48.dp)
          .testTag(buttonTestTag),
    ) {
      Icon(imageVector = Icons.Rounded.Download, contentDescription = null)
      Text(
        text = stringResource(R.string.action_export),
        modifier = Modifier.padding(start = 6.dp),
      )
    }
    DropdownMenu(
      expanded = menuExpanded,
      onDismissRequest = { menuExpanded = false },
    ) {
      ExportFormat.entries.forEach { format ->
        DropdownMenuItem(
          text = { Text("${format.name} (.${format.extension})") },
          onClick = {
            menuExpanded = false
            pendingFormat = format
            val suggested = CatalogExportSerializer.suggestedFileName(dataset, format)
            launcher.launch(suggested)
          },
          modifier = Modifier.testTag("${buttonTestTag}_${format.name.lowercase()}"),
        )
      }
    }
  }
}
