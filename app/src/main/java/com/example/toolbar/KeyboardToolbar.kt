package com.example.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.keyboard.KeyboardTypography
import com.example.theme.KeyboardPalette

private val ToolbarButtonCornerShape = RoundedCornerShape(4.dp)
private val VoiceIndicatorPillShape = RoundedCornerShape(12.dp)
private val VoiceRecordingRed = Color(0xFFE53935)
private val VoiceRecordingPillBg = VoiceRecordingRed.copy(alpha = 0.15f)

internal val ToolbarMicIconSize: Dp = 24.dp
internal val ToolbarMicTouchTargetWidth: Dp = 48.dp

internal var debugVoiceToolbarRecompositionCount: Int = 0
internal var lastMeasuredMicButtonWidthPx: Int = 0
internal var lastMeasuredMicButtonHeightPx: Int = 0
internal var lastMeasuredMicIconWidthPx: Int = 0
internal var lastMeasuredMicIconHeightPx: Int = 0
internal var lastMeasuredMicButtonRightInToolbarPx: Float = 0f

@Composable
fun KeyboardToolbar(
  palette: KeyboardPalette,
  isClipboardOpen: Boolean,
  clipboardCount: Int,
  showNumberRow: Boolean,
  activeLanguageBadge: String,
  onToggleClipboard: () -> Unit,
  onToggleNumberRow: () -> Unit,
  onSwitchLanguage: () -> Unit,
  onMoveCursorLeft: () -> Unit,
  onMoveCursorRight: () -> Unit,
  onCycleTheme: () -> Unit,
  onOpenSettings: () -> Unit,
  height: Dp = 36.dp,
  modifier: Modifier = Modifier,
  isLanguageSelectorOpen: Boolean = false,
  onOpenLanguageSelector: () -> Unit = onSwitchLanguage,
  isVoiceTypingOpen: Boolean = false,
  voiceListeningState: State<Boolean>? = null,
  voiceRmsScaleState: State<Float>? = null,
  onToggleVoiceTyping: () -> Unit = {},
  isTranslatorOpen: Boolean = false,
  onToggleTranslator: () -> Unit = {},
  isEmojiOpen: Boolean = false,
  onToggleEmoji: () -> Unit = {},
) {
  val density = LocalDensity.current
  val fontDamping = KeyboardTypography.getEffectiveFontScaleDamping(density.fontScale)
  val badgeFontSize = (11f * fontDamping).sp

  androidx.compose.runtime.key(density) {
    BoxWithConstraints(
      modifier =
        modifier
          .fillMaxWidth()
          .height(height)
          .focusProperties { canFocus = false }
          .background(palette.toolbarBackground)
          .padding(horizontal = 4.dp)
          .testTag("keyboard_toolbar"),
    ) {
    val availableWidth = with(density) { constraints.maxWidth.toDp() }
    val isUltraNarrow = availableWidth < 325.dp
    val isCompact = availableWidth < 345.dp
    val isNarrow = availableWidth < 395.dp
    val buttonPaddingH =
      when {
        isUltraNarrow -> 3.5.dp
        isCompact -> 4.dp
        isNarrow -> 5.dp
        else -> 6.5.dp
      }
    val itemSpacing =
      when {
        isUltraNarrow -> 1.5.dp
        isCompact -> 1.5.dp
        isNarrow -> 2.dp
        else -> 3.dp
      }
    val showCursorArrows = !isUltraNarrow

    androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides density) {
      Row(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
      // Weighted left action row: measured AFTER the unweighted right action cluster so that
      // wider language badges (e.g., Bangla Probhat "প্রভাত") never starve or shrink the
      // far-right microphone button across any screen width, density, or font scale.
      Row(
        modifier =
          Modifier.weight(1f, fill = false)
            .fillMaxHeight()
            .focusProperties { canFocus = false },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
      ) {
        // Language selector badge pill - opens language selection panel
        Row(
          modifier =
            Modifier.widthIn(min = 42.dp, max = if (isCompact) 68.dp else 78.dp)
              .focusProperties { canFocus = false }
              .clip(ToolbarButtonCornerShape)
              .background(
                if (isLanguageSelectorOpen) palette.accentKeyBackground else palette.keyBackground
              )
              .clickable(onClick = onOpenLanguageSelector)
              .padding(horizontal = buttonPaddingH, vertical = 4.dp)
              .testTag("toolbar_language_badge"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Language,
            contentDescription = stringResource(R.string.cd_language_selector),
            tint = if (isLanguageSelectorOpen) palette.accentKeyText else palette.accentKeyBackground,
            modifier = Modifier.requiredSize(13.dp),
          )
          Text(
            text = activeLanguageBadge,
            color = if (isLanguageSelectorOpen) palette.accentKeyText else palette.accentKeyBackground,
            fontSize = badgeFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
          )
          Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = if (isLanguageSelectorOpen) palette.accentKeyText else palette.accentKeyBackground,
            modifier = Modifier.requiredSize(13.dp),
          )
        }

        // Number Row Quick Toggle
        Box(
          modifier =
            Modifier.focusProperties { canFocus = false }
              .clip(ToolbarButtonCornerShape)
              .background(
                if (showNumberRow) palette.accentKeyBackground else palette.keyBackground
              )
              .clickable(onClick = onToggleNumberRow)
              .padding(horizontal = (buttonPaddingH + 1.dp), vertical = 4.dp)
              .testTag("toolbar_number_row_toggle"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "123",
            color = if (showNumberRow) palette.accentKeyText else palette.keyTextSecondary,
            fontSize = badgeFontSize,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
          )
        }

        // Emoji quick toggle button
        Box(
          modifier =
            Modifier.focusProperties { canFocus = false }
              .clip(ToolbarButtonCornerShape)
              .background(
                if (isEmojiOpen) palette.accentKeyBackground else palette.keyBackground
              )
              .clickable(onClick = onToggleEmoji)
              .padding(horizontal = buttonPaddingH, vertical = 4.dp)
              .testTag("toolbar_emoji_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Outlined.Mood,
            contentDescription = stringResource(R.string.cd_open_emoji),
            tint = if (isEmojiOpen) palette.accentKeyText else palette.keyTextPrimary,
            modifier = Modifier.requiredSize(15.dp),
          )
        }

        // Clipboard button
        Row(
          modifier =
            Modifier.focusProperties { canFocus = false }
              .clip(ToolbarButtonCornerShape)
              .background(
                if (isClipboardOpen) palette.accentKeyBackground else palette.keyBackground
              )
              .clickable(onClick = onToggleClipboard)
              .padding(horizontal = (buttonPaddingH + 1.dp), vertical = 4.dp)
              .testTag("toolbar_clipboard_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.ContentPaste,
            contentDescription = stringResource(R.string.cd_open_clipboard),
            tint = if (isClipboardOpen) palette.accentKeyText else palette.keyTextPrimary,
            modifier = Modifier.requiredSize(15.dp),
          )
          if (clipboardCount > 0) {
            Text(
              text = clipboardCount.toString(),
              color = if (isClipboardOpen) palette.accentKeyText else palette.keyTextSecondary,
              fontSize = badgeFontSize,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        // Translator button
        Box(
          modifier =
            Modifier.focusProperties { canFocus = false }
              .clip(ToolbarButtonCornerShape)
              .background(
                if (isTranslatorOpen) palette.accentKeyBackground else palette.keyBackground
              )
              .clickable(onClick = onToggleTranslator)
              .padding(horizontal = buttonPaddingH, vertical = 4.dp)
              .testTag("toolbar_translate_button"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = stringResource(R.string.cd_translate),
            tint = if (isTranslatorOpen) palette.accentKeyText else palette.keyTextPrimary,
            modifier = Modifier.requiredSize(15.dp),
          )
        }
      }

      ToolbarVoiceAndRightActions(
        palette = palette,
        isVoiceTypingOpen = isVoiceTypingOpen,
        voiceListeningState = voiceListeningState,
        voiceRmsScaleState = voiceRmsScaleState,
        onToggleVoiceTyping = onToggleVoiceTyping,
        onMoveCursorLeft = onMoveCursorLeft,
        onMoveCursorRight = onMoveCursorRight,
        onCycleTheme = onCycleTheme,
        onOpenSettings = onOpenSettings,
        showCursorArrows = showCursorArrows,
        buttonPaddingH = buttonPaddingH,
        badgeFontSize = badgeFontSize,
      )
      }
      }
    }
  }
}

@Composable
private fun ToolbarVoiceAndRightActions(
  palette: KeyboardPalette,
  isVoiceTypingOpen: Boolean,
  voiceListeningState: State<Boolean>?,
  voiceRmsScaleState: State<Float>? = null,
  onToggleVoiceTyping: () -> Unit,
  onMoveCursorLeft: () -> Unit,
  onMoveCursorRight: () -> Unit,
  onCycleTheme: () -> Unit,
  onOpenSettings: () -> Unit,
  showCursorArrows: Boolean = true,
  buttonPaddingH: Dp = 6.5.dp,
  badgeFontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
) {
  val density = LocalDensity.current
  val activeVoiceTypingOpen = voiceListeningState?.value ?: isVoiceTypingOpen

  SideEffect {
    debugVoiceToolbarRecompositionCount++
  }

  Row(
    modifier = Modifier.fillMaxHeight().focusProperties { canFocus = false },
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
  ) {
    if (activeVoiceTypingOpen) {
      // Active recording indicator pill - tap stops voice typing silently without click sound
      Row(
        modifier =
          Modifier.focusProperties { canFocus = false }
            .clip(VoiceIndicatorPillShape)
            .background(VoiceRecordingPillBg)
            .semantics {
              role = Role.Button
              onClick {
                onToggleVoiceTyping()
                true
              }
            }
            .pointerInput(onToggleVoiceTyping) {
              detectTapGestures(onTap = { onToggleVoiceTyping() })
            }
            .padding(horizontal = buttonPaddingH, vertical = 3.dp)
            .testTag("toolbar_voice_recording_indicator"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Box(
          modifier =
            Modifier.requiredSize(6.dp)
              .clip(CircleShape)
              .background(VoiceRecordingRed)
        )
        Text(
          text = "Listening…",
          color = VoiceRecordingRed,
          fontSize = badgeFontSize,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          softWrap = false,
        )
      }
    } else {
      if (showCursorArrows) {
        // Cursor left
        ToolbarIconButton(
          onClick = onMoveCursorLeft,
          tag = "toolbar_cursor_left",
          paddingH = buttonPaddingH,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.cd_cursor_left),
            tint = palette.keyTextPrimary,
            modifier = Modifier.requiredSize(17.dp),
          )
        }

        // Cursor right
        ToolbarIconButton(
          onClick = onMoveCursorRight,
          tag = "toolbar_cursor_right",
          paddingH = buttonPaddingH,
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.cd_cursor_right),
            tint = palette.keyTextPrimary,
            modifier = Modifier.requiredSize(17.dp),
          )
        }
      }

      // Quick theme cycle
      ToolbarIconButton(
        onClick = onCycleTheme,
        tag = "toolbar_theme_button",
        paddingH = buttonPaddingH,
      ) {
        Icon(
          imageVector = Icons.Outlined.Palette,
          contentDescription = "Switch keyboard theme",
          tint = palette.keyTextSecondary,
          modifier = Modifier.requiredSize(16.dp),
        )
      }

      // Open Keyboard Settings App
      ToolbarIconButton(
        onClick = onOpenSettings,
        tag = "toolbar_settings_button",
        paddingH = buttonPaddingH,
      ) {
        Icon(
          imageVector = Icons.Outlined.Settings,
          contentDescription = stringResource(R.string.cd_open_settings),
          tint = palette.keyTextSecondary,
          modifier = Modifier.requiredSize(16.dp),
        )
      }
    }

    // Shared, reusable Microphone button anchored at the far right across all 4 layouts
    ToolbarMicrophoneButton(
      palette = palette,
      activeVoiceTypingOpen = activeVoiceTypingOpen,
      voiceRmsScaleState = voiceRmsScaleState,
      onToggleVoiceTyping = onToggleVoiceTyping,
    )
  }
}

/**
 * Shared, reusable Microphone Icon button component used consistently across all four keyboard
 * layouts (English QWERTY, Bangla Probhat, Banglish, and Hindi).
 *
 * Guarantees:
 * - Fixed 48dp horizontal touch target width (`ToolbarMicTouchTargetWidth`).
 * - Fixed 24dp visual microphone icon dimensions (`ToolbarMicIconSize`) via `requiredSize` so parent
 *   row constraints can never shrink the icon.
 * - Centered vertical and horizontal alignment at the far right of the toolbar.
 */
@Composable
internal fun ToolbarMicrophoneButton(
  palette: KeyboardPalette,
  activeVoiceTypingOpen: Boolean,
  voiceRmsScaleState: State<Float>?,
  onToggleVoiceTyping: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentDensity = LocalDensity.current.density
  Box(
    modifier =
      modifier
        .width(if (currentDensity > 0f) ToolbarMicTouchTargetWidth else ToolbarMicTouchTargetWidth)
        .fillMaxHeight()
        .focusProperties { canFocus = false }
        .clip(ToolbarButtonCornerShape)
        .let {
          if (activeVoiceTypingOpen) it.background(VoiceRecordingRed) else it
        }
        .semantics {
          role = Role.Button
          onClick {
            onToggleVoiceTyping()
            true
          }
        }
        .pointerInput(onToggleVoiceTyping) {
          detectTapGestures(onTap = { onToggleVoiceTyping() })
        }
        .onGloballyPositioned { coords ->
          lastMeasuredMicButtonWidthPx = coords.size.width
          lastMeasuredMicButtonHeightPx = coords.size.height
          lastMeasuredMicButtonRightInToolbarPx =
            (coords.parentLayoutCoordinates?.positionInParent()?.x ?: 0f) +
              coords.positionInParent().x +
              coords.size.width
        }
        .testTag("toolbar_voice_button"),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.requiredSize(ToolbarMicIconSize).graphicsLayer {
          val scale = if (activeVoiceTypingOpen) (voiceRmsScaleState?.value ?: 1.0f) else 1.0f
          scaleX = scale
          scaleY = scale
        },
    ) {
      if (activeVoiceTypingOpen) {
        // Live pulsing glow ring when speech/sound is active
        Box(
          modifier =
            Modifier.requiredSize(ToolbarMicIconSize)
              .graphicsLayer {
                val scale = voiceRmsScaleState?.value ?: 1.0f
                val ringScale = 1.0f + ((scale - 1.0f) * 1.6f).coerceAtLeast(0f)
                scaleX = ringScale
                scaleY = ringScale
                alpha = (((scale - 1.0f) / 0.25f) * 0.45f).coerceIn(0f, 0.45f)
              }
              .background(Color.White, CircleShape),
        )
      }
      Icon(
        imageVector = if (activeVoiceTypingOpen) Icons.Filled.Mic else Icons.Outlined.Mic,
        contentDescription = stringResource(R.string.cd_voice_typing),
        tint = if (activeVoiceTypingOpen) Color.White else palette.accentKeyBackground,
        modifier =
          Modifier.size(ToolbarMicIconSize)
            .requiredSize(ToolbarMicIconSize)
            .onGloballyPositioned { coords ->
              lastMeasuredMicIconWidthPx = coords.size.width
              lastMeasuredMicIconHeightPx = coords.size.height
            }
            .testTag("toolbar_voice_icon"),
      )
    }
  }
}

@Composable
private fun ToolbarIconButton(
  onClick: () -> Unit,
  tag: String,
  activeBackground: Color? = null,
  disableClickSound: Boolean = false,
  paddingH: Dp = 6.5.dp,
  content: @Composable () -> Unit,
) {
  val clickModifier =
    if (disableClickSound) {
      Modifier
        .semantics {
          role = Role.Button
          onClick {
            onClick()
            true
          }
        }
        .pointerInput(onClick) {
          detectTapGestures(onTap = { onClick() })
        }
    } else {
      Modifier.clickable(onClick = onClick)
    }

  Box(
    modifier =
      Modifier.fillMaxHeight()
        .focusProperties { canFocus = false }
        .clip(ToolbarButtonCornerShape)
        .let { if (activeBackground != null) it.background(activeBackground) else it }
        .then(clickModifier)
        .padding(horizontal = paddingH)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    content()
  }
}
