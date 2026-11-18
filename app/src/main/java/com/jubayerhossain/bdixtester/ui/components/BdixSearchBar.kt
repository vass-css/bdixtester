package com.jubayerhossain.bdixtester.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jubayerhossain.bdixtester.R
import com.jubayerhossain.bdixtester.ui.theme.BdixCardShapes

/**
 * Shared M3 search input used consistently across Servers, Saved, and History screens.
 */
@Composable
fun BdixSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  placeholderRes: Int = R.string.servers_search_placeholder,
  maxWidth: Dp = 720.dp,
) {
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = maxWidth)
        .testTag(testTag),
    placeholder = {
      Text(
        text = stringResource(placeholderRes),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Rounded.Search,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
          Icon(
            imageVector = Icons.Rounded.Clear,
            contentDescription = stringResource(R.string.action_clear_filters),
          )
        }
      }
    },
    singleLine = true,
    shape = BdixCardShapes.input,
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
      ),
  )
}
