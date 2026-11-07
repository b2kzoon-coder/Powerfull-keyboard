package com.example.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.layout.EnterActionType
import com.example.layout.KeyAction
import com.example.layout.KeyRole
import com.example.layout.KeySpec
import com.example.layout.ShiftState
import com.example.theme.KeyboardPalette
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.BengaliFontFamily

private val BaseCenteredTextStyle =
  TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    textAlign = TextAlign.Center,
  )

private val SpacebarIndicatorShape = RoundedCornerShape(1.dp)

private fun sanitizeDisplayLabel(label: String): String {
  if (label.isEmpty() || '\u25CC' !in label) return label
  return label.replace("\u25CC", "")
}

@Composable
fun KeyButton(
  spec: KeySpec,
  shiftState: ShiftState,
  enterActionType: EnterActionType,
  spacebarLabel: String,
  cutCopyPasteEnabled: Boolean,
  palette: KeyboardPalette,
  onKeyAction: (KeyAction) -> Unit,
  onLongPressSecondary: ((String) -> Unit)? = null,
  rowHeightDp: Float = 50f,
  horizontalPaddingDp: Float = 1.8f,
  modifier: Modifier = Modifier,
) {
  var isPressed by remember { mutableStateOf(false) }
  val currentOnKeyAction by rememberUpdatedState(onKeyAction)
  val currentOnLongPress by rememberUpdatedState(onLongPressSecondary)
  val currentCutCopyPasteEnabled by rememberUpdatedState(cutCopyPasteEnabled)
  val scope = rememberCoroutineScope()

  val density = androidx.compose.ui.platform.LocalDensity.current
  val fontScale = density.fontScale

  val isShifted = shiftState != ShiftState.LOWERCASE
  val rawDisplayText =
    when (spec.action) {
      is KeyAction.Shift ->
        when (shiftState) {
          ShiftState.LOWERCASE -> "⇧"
          ShiftState.SHIFTED -> "⬆"
          ShiftState.CAPS_LOCK -> "⇪"
        }
      is KeyAction.Space -> if (spec.role == KeyRole.SPACEBAR) spacebarLabel else spec.primaryLabel
      is KeyAction.Enter -> enterActionType.label
      else -> if (isShifted) spec.shiftedLabel else spec.primaryLabel
    }
  val displayText = remember(rawDisplayText) { sanitizeDisplayLabel(rawDisplayText) }

  val activeEditHint =
    if (cutCopyPasteEnabled) {
      when (spec.longPressEditAction) {
        KeyAction.Cut -> "Cut"
        KeyAction.Copy -> "Copy"
        KeyAction.Paste -> "Paste"
        else -> null
      }
    } else {
      null
    }

  val topRightHint =
    if (isShifted) {
      null
    } else {
      val rawHint = spec.secondaryHint ?: activeEditHint
      remember(rawHint) { rawHint?.let { sanitizeDisplayLabel(it) } }
    }

  val bgColor =
    when (spec.role) {
      KeyRole.ACCENT ->
        if (isPressed) palette.keyPressedBackground else palette.accentKeyBackground
      KeyRole.FUNCTIONAL ->
        if (spec.action is KeyAction.Shift && shiftState == ShiftState.CAPS_LOCK) {
          palette.accentKeyBackground
        } else if (isPressed) {
          palette.functionalKeyPressedBackground
        } else {
          palette.functionalKeyBackground
        }
      KeyRole.CHARACTER,
      KeyRole.SPACEBAR ->
        if (isPressed) palette.keyPressedBackground else palette.keyBackground
    }

  val textColor =
    when {
      spec.role == KeyRole.ACCENT -> palette.accentKeyText
      spec.action is KeyAction.Shift && shiftState == ShiftState.CAPS_LOCK -> palette.accentKeyText
      spec.role == KeyRole.SPACEBAR -> palette.keyTextSecondary
      else -> palette.keyTextPrimary
    }

  val heightScale = (rowHeightDp / 50f).coerceIn(0.60f, 1.80f)
  val cornerRadius = (5.5f * heightScale).coerceIn(3.5f, 7.5f).dp
  val keyShape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
  val fontSize =
    remember(displayText, spec.role, heightScale, fontScale) {
      KeyboardTypography.resolveFontSize(
        displayText = displayText,
        isSpacebar = spec.role == KeyRole.SPACEBAR,
        heightScale = heightScale,
        fontScale = fontScale,
      )
    }

  Box(
    modifier =
      modifier
        .fillMaxHeight()
        .padding(
          horizontal = horizontalPaddingDp.dp,
          vertical = (2.2f * heightScale).coerceIn(1.2f, 4.5f).dp,
        )
        .clip(keyShape)
        .background(bgColor)
        .semantics {
          role = Role.Button
          contentDescription = displayText
          onClick {
            currentOnKeyAction(spec.action)
            true
          }
          if (currentCutCopyPasteEnabled && spec.longPressEditAction != null) {
            onLongClick {
              currentOnKeyAction(spec.longPressEditAction)
              true
            }
          }
        }
        .pointerInput(spec.id) {
          val horizontalSwipeThresholdPx = 38.dp.toPx()
          val maxVerticalDriftPx = 28.dp.toPx()

          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            isPressed = true

            if (spec.role == KeyRole.SPACEBAR) {
              var swipeTriggered = false
              var cancelled = false
              val startX = down.position.x
              val startY = down.position.y

              while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val pointerChange =
                  event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()

                if (pointerChange == null) {
                  cancelled = true
                  break
                }

                if (!pointerChange.pressed) {
                  break
                }

                val dx = pointerChange.position.x - startX
                val dy = pointerChange.position.y - startY
                val absDx = abs(dx)
                val absDy = abs(dy)

                if (
                  !swipeTriggered &&
                    absDx >= horizontalSwipeThresholdPx &&
                    absDx > absDy * 1.6f &&
                    absDy <= maxVerticalDriftPx
                ) {
                  swipeTriggered = true
                  pointerChange.consume()
                  if (dx < 0f) {
                    currentOnKeyAction(KeyAction.SwipeSpaceLeft)
                  } else {
                    currentOnKeyAction(KeyAction.SwipeSpaceRight)
                  }
                }
              }

              isPressed = false
              if (!swipeTriggered && !cancelled) {
                currentOnKeyAction(KeyAction.Space)
              }
            } else {
              var longPressTriggered = false
              var repeatJob: Job? = null

              if (spec.repeatable) {
                currentOnKeyAction(spec.action)
                repeatJob =
                  scope.launch {
                    delay(380L)
                    while (true) {
                      currentOnKeyAction(spec.action)
                      delay(55L)
                    }
                  }
              } else if (currentCutCopyPasteEnabled && spec.longPressEditAction != null) {
                repeatJob =
                  scope.launch {
                    delay(420L)
                    longPressTriggered = true
                    currentOnKeyAction(spec.longPressEditAction)
                  }
              } else if (spec.secondaryHint != null && currentOnLongPress != null) {
                repeatJob =
                  scope.launch {
                    delay(420L)
                    longPressTriggered = true
                    currentOnLongPress?.invoke(spec.secondaryHint)
                  }
              }

              val up = waitForUpOrCancellation()
              repeatJob?.cancel()
              isPressed = false

              if (up != null && !spec.repeatable && !longPressTriggered) {
                currentOnKeyAction(spec.action)
              }
            }
          }
        }
        .testTag("key_${spec.id}"),
    contentAlignment = Alignment.Center,
  ) {
    // Primary centered content
    val isCombiningPrimary = remember(displayText) { isBengaliCombiningLabel(displayText) }
    val primaryFontFamily =
      remember(displayText) {
        if (KeyboardTypography.isBengali(displayText) || KeyboardTypography.isDevanagari(displayText)) {
          BengaliFontFamily
        } else {
          FontFamily.Default
        }
      }

    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      if (spec.role == KeyRole.SPACEBAR) {
        val indicatorColor =
          remember(palette.keyTextSecondary) { palette.keyTextSecondary.copy(alpha = 0.5f) }
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
          Text(
            text = displayText,
            color = palette.keyTextSecondary,
            fontSize = fontSize,
            fontFamily = primaryFontFamily,
            style = BaseCenteredTextStyle,
          )
          Spacer(modifier = Modifier.height((2f * heightScale).dp))
          Box(
            modifier =
              Modifier.width((28f * heightScale).dp)
                .height((1.5f * heightScale).coerceAtLeast(1f).dp)
                .background(
                  indicatorColor,
                  SpacebarIndicatorShape,
                ),
          )
        }
      } else if (isCombiningPrimary) {
        BengaliCombiningMark(
          markString = displayText,
          color = textColor,
          fontSize = fontSize,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Text(
          text = displayText,
          color = textColor,
          fontSize = fontSize,
          fontFamily = primaryFontFamily,
          style = BaseCenteredTextStyle,
          lineHeight = (fontSize.value * 1.15f).sp,
          maxLines = 1,
          softWrap = false,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
          fontWeight =
            if (spec.role == KeyRole.FUNCTIONAL || spec.role == KeyRole.ACCENT) {
              FontWeight.SemiBold
            } else {
              FontWeight.Normal
            },
        )
      }
    }

    // Secondary hint at top-right
    if (topRightHint != null && spec.role == KeyRole.CHARACTER) {
      val isIndicHint =
        remember(topRightHint) {
          KeyboardTypography.isBengali(topRightHint) || KeyboardTypography.isDevanagari(topRightHint)
        }
      val isCombiningHint = remember(topRightHint) { isBengaliCombiningLabel(topRightHint) }
      val hintFontSize =
        remember(topRightHint, heightScale, fontScale) {
          KeyboardTypography.resolveHintFontSize(topRightHint, heightScale, fontScale)
        }
      val hintFontFamily = if (isIndicHint) BengaliFontFamily else FontFamily.Default
      val hintScale =
        if (isIndicHint) {
          KeyboardTypography.getBengaliControlledScale(heightScale)
        } else {
          heightScale
        }

      Box(
        modifier =
          Modifier.align(Alignment.TopEnd)
            .padding(
              top = (2f * hintScale).dp,
              end = (3f * hintScale).coerceAtLeast(2.2f).dp,
            ),
      ) {
        if (isCombiningHint) {
          BengaliCombiningMark(
            markString = topRightHint,
            color = palette.keyTextSecondary,
            fontSize = hintFontSize,
            modifier = Modifier.size((11f * hintScale).dp),
          )
        } else {
          Text(
            text = topRightHint,
            color = palette.keyTextSecondary,
            fontSize = hintFontSize,
            fontFamily = hintFontFamily,
            style = BaseCenteredTextStyle,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}
