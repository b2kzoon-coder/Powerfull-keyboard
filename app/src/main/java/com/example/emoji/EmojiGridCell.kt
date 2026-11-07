package com.example.emoji

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EmojiCellShape = RoundedCornerShape(6.dp)

/**
 * Isolated, skippable emoji cell composable for [EmojiPanel]'s LazyVerticalGrid.
 * All parameters are stable so unaffected cells skip recomposition during search or state updates.
 */
@Composable
fun EmojiGridCell(
  emoji: String,
  isCompact: Boolean,
  onSelectEmoji: (String) -> Unit,
  onLongPressSkinTone: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val supportsTone = remember(emoji) { EmojiData.supportsSkinTone(emoji) }
  val currentOnSelectEmoji by rememberUpdatedState(onSelectEmoji)
  val currentOnLongPressSkinTone by rememberUpdatedState(onLongPressSkinTone)
  val scope = rememberCoroutineScope()

  Box(
    modifier =
      modifier
        .size(if (isCompact) 38.dp else 44.dp)
        .focusProperties { canFocus = false }
        .clip(EmojiCellShape)
        .pointerInput(emoji, supportsTone) {
          awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var longPressTriggered = false
            var longPressJob: Job? = null

            if (supportsTone) {
              longPressJob =
                scope.launch {
                  delay(400L)
                  longPressTriggered = true
                  currentOnLongPressSkinTone(emoji)
                }
            }

            val up = waitForUpOrCancellation()
            longPressJob?.cancel()

            if (up != null && !longPressTriggered) {
              currentOnSelectEmoji(emoji)
            }
          }
        }
        .testTag("emoji_item_$emoji"),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = emoji,
      fontSize = if (isCompact) 21.sp else 24.sp,
      textAlign = TextAlign.Center,
    )
  }
}
