package com.example.layout

import androidx.compose.runtime.Immutable

enum class ShiftState {
  LOWERCASE,
  SHIFTED,
  CAPS_LOCK,
}

enum class KeyboardMode {
  ALPHABET,
  SYMBOLS_PRIMARY,
  SYMBOLS_SECONDARY,
  NUMBER_PAD,
  EMOJI,
}

enum class EnterActionType(val label: String) {
  ENTER("↵"),
  DONE("Done"),
  GO("Go"),
  SEARCH("Search"),
  SEND("Send"),
  NEXT("Next"),
}

object KeyboardHeightBounds {
  const val MIN_PERCENT = 60
  const val MAX_PERCENT = 180
  const val DEFAULT_PORTRAIT_PERCENT = 130
  const val DEFAULT_LANDSCAPE_PERCENT = 100

  // 100% standard baseline: 50dp in portrait (90dp at 180%), 38dp in landscape (68.4dp at 180%)
  const val BASE_PORTRAIT_ROW_HEIGHT_DP = 50f
  const val BASE_LANDSCAPE_ROW_HEIGHT_DP = 38f

  fun clampPercent(percent: Int): Int {
    return percent.coerceIn(MIN_PERCENT, MAX_PERCENT)
  }

  fun rowHeightDpForPercent(percent: Int, isLandscape: Boolean = false): Float {
    val clamped = clampPercent(percent)
    val base = if (isLandscape) BASE_LANDSCAPE_ROW_HEIGHT_DP else BASE_PORTRAIT_ROW_HEIGHT_DP
    return base * (clamped / 100f)
  }

  fun rowHeightDpForPercent(percent: Int): Float {
    return rowHeightDpForPercent(percent, isLandscape = false)
  }

  fun toolbarHeightDpForPercent(percent: Int, isLandscape: Boolean = false): Float {
    val clamped = clampPercent(percent)
    val base = if (isLandscape) 32f else 36f
    return base * (clamped / 100f).coerceIn(0.85f, 1.35f)
  }
}

@Immutable
sealed interface KeyAction {
  @Immutable
  data class CommitChar(
    val lowercase: String,
    val uppercase: String = lowercase.uppercase(),
  ) : KeyAction

  data object Shift : KeyAction

  data object Backspace : KeyAction

  data object ModeSymbolsPrimary : KeyAction

  data object ModeSymbolsSecondary : KeyAction

  data object ModeAlphabet : KeyAction

  data object ModeEmoji : KeyAction

  data object LanguageSwitch : KeyAction

  data object SwipeSpaceLeft : KeyAction

  data object SwipeSpaceRight : KeyAction

  data object Space : KeyAction

  data object Enter : KeyAction

  data object Cut : KeyAction

  data object Copy : KeyAction

  data object Paste : KeyAction
}

enum class KeyRole {
  CHARACTER,
  FUNCTIONAL,
  ACCENT,
  SPACEBAR,
}

@Immutable
data class KeySpec(
  val id: String,
  val primaryLabel: String,
  val shiftedLabel: String = primaryLabel,
  val secondaryHint: String? = null,
  val longPressEditAction: KeyAction? = null,
  val weight: Float = 1f,
  val role: KeyRole = KeyRole.CHARACTER,
  val action: KeyAction,
  val repeatable: Boolean = false,
)

@Immutable
data class KeyboardLayoutSpec(
  val mode: KeyboardMode,
  val rows: List<List<KeySpec>>,
)
