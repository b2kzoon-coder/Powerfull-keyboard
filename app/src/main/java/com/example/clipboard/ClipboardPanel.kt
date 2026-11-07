package com.example.clipboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.theme.KeyboardPalette

@Composable
fun ClipboardPanel(
  entries: List<ClipboardEntry>,
  palette: KeyboardPalette,
  panelHeight: Dp,
  onPasteClip: (String) -> Unit,
  onTogglePin: (Long) -> Unit,
  onDeleteClip: (Long) -> Unit,
  onClearUnpinned: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .height(panelHeight)
        .background(palette.keyboardBackground)
        .padding(horizontal = 8.dp, vertical = 4.dp)
        .testTag("clipboard_panel")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(36.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClose)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("clipboard_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = stringResource(R.string.cd_close_panel),
          tint = palette.keyTextPrimary,
          modifier = Modifier.size(18.dp),
        )
        Text(
          text = "Clipboard (${entries.size})",
          color = palette.keyTextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.padding(start = 6.dp),
        )
      }

      if (entries.any { !it.isPinned }) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(6.dp))
              .clickable(onClick = onClearUnpinned)
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("clipboard_clear_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = stringResource(R.string.cd_clear_clipboard),
            tint = palette.keyTextSecondary,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }

    if (entries.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.clipboard_empty),
          color = palette.keyTextSecondary,
          fontSize = 13.sp,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(items = entries, key = { it.id }) { item ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(palette.keyBackground)
                .clickable { onPasteClip(item.text) }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("clipboard_item_${item.id}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = item.text,
              color = palette.keyTextPrimary,
              fontSize = 13.sp,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            Box(
              modifier =
                Modifier.size(32.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { onTogglePin(item.id) }
                  .testTag("clipboard_pin_${item.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector =
                  if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                contentDescription = "Pin clip",
                tint = if (item.isPinned) palette.accentKeyBackground else palette.keyTextSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
            Box(
              modifier =
                Modifier.size(32.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { onDeleteClip(item.id) }
                  .testTag("clipboard_delete_${item.id}"),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove clip",
                tint = palette.keyTextSecondary,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }
      }
    }
  }
}
