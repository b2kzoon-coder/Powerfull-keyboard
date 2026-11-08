package com.example.keyboard

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.language.LanguageInputMethodKind
import com.example.layout.EnterActionType
import com.example.layout.KeyAction
import com.example.layout.KeyRole
import com.example.layout.KeySpec
import com.example.layout.KeyboardHeightBounds
import com.example.layout.KeyboardMode
import kotlin.math.max
import kotlin.math.min

/**
 * Universal, device-independent responsive layout engine for Personal Keyboard.
 *
 * Calculates all horizontal and vertical keyboard measurements directly from parent
 * layout constraints (`Constraints.maxWidth` / `BoxWithConstraints`) and `Density`,
 * avoiding device-model-specific hardcoding or reliance on stale global screen metrics.
 *
 * Guarantees across OPPO, Redmi/Xiaomi, Samsung, Realme, Tecno, Infinix, Pixel, and tablets:
 * 1. Available width is derived strictly from actual parent constraints after system insets.
 * 2. Horizontal container padding, key spacing, and 9-key row indentation compress FIRST
 *    on narrow viewports before key cap widths are reduced.
 * 3. Dense rows (12-key Bengali Probhat Row 1, 11-key Probhat Row 3) automatically tighten
 *    inter-key gaps to maximize interior glyph space.
 * 4. Bottom action row keys (`?123`, `😊`, `,`, `Spacebar`, `.`/`।`, `Enter`) receive
 *    adaptive weight allocation so no button is ever clipped, hidden, or pushed off-screen.
 * 5. Key typography is bounded by BOTH vertical row height and horizontal allocated key width.
 */
object ResponsiveLayoutEngine {

  const val MAX_HANDHELD_WIDTH_DP = 600f
  const val MAX_TABLET_KEYBOARD_WIDTH_DP = 580f
  const val TABLET_WIDTH_FRACTION = 0.85f

  const val MIN_SAFE_KEYBOARD_WIDTH_DP = 240f
  const val ULTRA_NARROW_WIDTH_DP = 325f
  const val VERY_NARROW_WIDTH_DP = 330f
  const val NARROW_WIDTH_DP = 360f
  const val COMPACT_WIDTH_DP = 385f

  // Minimum usable horizontal touch-target ratios for bottom-row functional keys
  private const val MIN_BOTTOM_SYMBOL_KEY_DP = 40f
  private const val MIN_BOTTOM_EMOJI_KEY_DP = 34f
  private const val MIN_BOTTOM_PUNCT_KEY_DP = 32f
  private const val MIN_BOTTOM_ENTER_KEY_DP = 46f
  private const val MIN_BOTTOM_ENTER_TEXT_LABEL_DP = 54f
  private const val MIN_SPACEBAR_WIDTH_FRACTION = 0.30f

  @Immutable
  data class ResponsiveRowLayout(
    val rowIndex: Int,
    val sideIndentationDp: Float,
    val keyHorizontalPaddingDp: Float,
    val keyVerticalPaddingDp: Float,
    val effectiveWeights: List<Float>,
    val allocatedKeyWidthsDp: List<Float>,
  )

  @Immutable
  data class ResponsiveKeyboardMetrics(
    val availableParentWidthDp: Float,
    val maxKeyboardWidthDp: Float,
    val containerHorizontalPaddingDp: Float,
    val containerVerticalPaddingDp: Float,
    val rowHeightDp: Float,
    val toolbarHeightDp: Float,
    val heightScale: Float,
    val baseKeyHorizontalPaddingDp: Float,
    val isLandscape: Boolean,
    val isUltraNarrow: Boolean,
    val isVeryNarrow: Boolean,
    val isNarrow: Boolean,
    val rows: List<ResponsiveRowLayout>,
  )

  @Immutable
  data class ResponsiveToolbarMetrics(
    val availableWidthDp: Float,
    val horizontalContainerPaddingDp: Dp,
    val buttonPaddingH: Dp,
    val itemSpacing: Dp,
    val rightClusterSpacing: Dp,
    val badgeMinWidth: Dp,
    val badgeMaxWidth: Dp,
    val leftIconSize: Dp,
    val rightArrowIconSize: Dp,
    val rightActionIconSize: Dp,
    val showCursorArrows: Boolean,
    val micTouchTargetWidth: Dp,
    val micIconSize: Dp,
  )

  /**
   * Resolves the available parent width in dp directly from layout [constraints] and [density].
   */
  fun resolveAvailableWidthDp(
    constraints: Constraints,
    density: Density,
    fallbackWidthDp: Float = 360f,
  ): Float {
    if (constraints.hasBoundedWidth && constraints.maxWidth > 0) {
      val safeDensity = if (density.density > 0f) density.density else 1f
      return (constraints.maxWidth / safeDensity).coerceAtLeast(MIN_SAFE_KEYBOARD_WIDTH_DP)
    }
    return fallbackWidthDp.coerceAtLeast(MIN_SAFE_KEYBOARD_WIDTH_DP)
  }

  /**
   * Computes the effective maximum keyboard width in dp from the parent's available width.
   * Full bleed on handheld screens (<= 600dp); comfortably centered cap on tablets/foldables (> 600dp).
   */
  fun computeMaxKeyboardWidthDp(availableWidthDp: Float): Float {
    val safeWidth = availableWidthDp.coerceAtLeast(MIN_SAFE_KEYBOARD_WIDTH_DP)
    return if (safeWidth > MAX_HANDHELD_WIDTH_DP) {
      MAX_TABLET_KEYBOARD_WIDTH_DP.coerceAtMost(safeWidth * TABLET_WIDTH_FRACTION)
    } else {
      safeWidth
    }
  }

  /**
   * Computes baseline per-key horizontal padding (half-gap on each side of a key) for standard 10-key rows.
   */
  fun computeKeyHorizontalPaddingDp(keyboardWidthDp: Float): Float {
    return when {
      keyboardWidthDp < VERY_NARROW_WIDTH_DP -> 1.0f
      keyboardWidthDp < NARROW_WIDTH_DP -> 1.4f
      else -> 1.8f
    }
  }

  /**
   * Computes per-row per-key horizontal padding, tightening gaps first on dense rows
   * (such as 12-key Probhat Row 1 and 11-key Probhat Row 3) before reducing key cap widths.
   */
  fun computeRowKeyHorizontalPaddingDp(
    keyboardWidthDp: Float,
    rowKeyCount: Int,
  ): Float {
    val basePadding = computeKeyHorizontalPaddingDp(keyboardWidthDp)
    return when {
      rowKeyCount >= 12 -> {
        when {
          keyboardWidthDp < VERY_NARROW_WIDTH_DP -> 0.70f
          keyboardWidthDp < NARROW_WIDTH_DP -> 0.95f
          keyboardWidthDp < 400f -> 1.15f
          else -> 1.35f
        }
      }
      rowKeyCount == 11 -> {
        when {
          keyboardWidthDp < VERY_NARROW_WIDTH_DP -> 0.80f
          keyboardWidthDp < NARROW_WIDTH_DP -> 1.10f
          keyboardWidthDp < 400f -> 1.30f
          else -> 1.50f
        }
      }
      else -> basePadding
    }
  }

  /**
   * Computes container horizontal outer margin. Compresses on narrow screens before shrinking keys.
   */
  fun computeContainerHorizontalPaddingDp(keyboardWidthDp: Float): Float {
    return when {
      keyboardWidthDp < ULTRA_NARROW_WIDTH_DP -> 1.0f
      keyboardWidthDp < NARROW_WIDTH_DP -> 1.5f
      else -> 2.0f
    }
  }

  /**
   * Computes proportional horizontal side indentation for 9-key alphabet middle rows (ASDFGHJKL / Probhat Row 2).
   */
  fun computeNineKeyRowPaddingDp(
    keyboardWidthDp: Float,
    isProbhat: Boolean,
  ): Float {
    val factor = if (isProbhat) 0.042f else 0.05f
    val minIndent = if (keyboardWidthDp < 300f) 4f else 8f
    return (keyboardWidthDp * factor).coerceIn(minIndent, 28f)
  }

  /**
   * Computes adaptive effective weights for a row of keys.
   *
   * For standard character rows, preserves the exact specification weights.
   * For the bottom action row (`?123`/`ABC`, `😊`, `,`, `Spacebar`, `.`/`।`, `Enter`),
   * guarantees that functional keys (`?123`, `😊`, `,`, `.`/`।`, `Enter`) meet minimum
   * legible width requirements while `Spacebar` absorbs the remaining width cleanly.
   */
  fun computeEffectiveRowWeights(
    rowKeys: List<KeySpec>,
    usableRowWidthDp: Float,
    enterActionType: EnterActionType = EnterActionType.ENTER,
  ): List<Float> {
    if (rowKeys.isEmpty()) return emptyList()
    val hasSpacebar = rowKeys.any { it.role == KeyRole.SPACEBAR }
    if (!hasSpacebar || usableRowWidthDp <= 0f) {
      return rowKeys.map { it.weight.coerceAtLeast(0.1f) }
    }

    val rawWeights = rowKeys.map { it.weight.coerceAtLeast(0.1f) }
    val totalRawWeight = rawWeights.sum()
    if (totalRawWeight <= 0f) return rawWeights

    // Calculate minimum required weight share for non-spacebar keys on narrow viewports
    val adjustedWeights = rawWeights.toMutableList()
    var spacebarIndex = -1
    var nonSpaceRequestedFraction = 0f

    rowKeys.forEachIndexed { idx, keySpec ->
      if (keySpec.role == KeyRole.SPACEBAR) {
        spacebarIndex = idx
      } else {
        val baseFraction = rawWeights[idx] / totalRawWeight
        val baseWidthDp = baseFraction * usableRowWidthDp
        val minRequiredDp =
          when {
            keySpec.action is KeyAction.ModeSymbolsPrimary ||
              keySpec.action is KeyAction.ModeSymbolsSecondary ||
              keySpec.action is KeyAction.ModeAlphabet -> MIN_BOTTOM_SYMBOL_KEY_DP
            keySpec.action is KeyAction.ModeEmoji -> MIN_BOTTOM_EMOJI_KEY_DP
            keySpec.action is KeyAction.Enter -> {
              if (enterActionType.label.length > 2) {
                MIN_BOTTOM_ENTER_TEXT_LABEL_DP
              } else {
                MIN_BOTTOM_ENTER_KEY_DP
              }
            }
            else -> MIN_BOTTOM_PUNCT_KEY_DP
          }
        val targetWidthDp = max(baseWidthDp, min(minRequiredDp, usableRowWidthDp * 0.20f))
        val targetFraction = (targetWidthDp / usableRowWidthDp).coerceIn(0.08f, 0.22f)
        adjustedWeights[idx] = targetFraction * totalRawWeight
        nonSpaceRequestedFraction += targetFraction
      }
    }

    if (spacebarIndex >= 0) {
      val maxAllowedNonSpaceFraction = 1f - MIN_SPACEBAR_WIDTH_FRACTION
      if (nonSpaceRequestedFraction > maxAllowedNonSpaceFraction && nonSpaceRequestedFraction > 0f) {
        // Scale non-spacebar adjustments proportionally so Spacebar always retains >= 30% width
        val scaleDown = maxAllowedNonSpaceFraction / nonSpaceRequestedFraction
        rowKeys.forEachIndexed { idx, keySpec ->
          if (keySpec.role != KeyRole.SPACEBAR) {
            adjustedWeights[idx] = adjustedWeights[idx] * scaleDown
          }
        }
        nonSpaceRequestedFraction = maxAllowedNonSpaceFraction
      }
      val spaceFraction = (1f - nonSpaceRequestedFraction).coerceAtLeast(MIN_SPACEBAR_WIDTH_FRACTION)
      adjustedWeights[spacebarIndex] = spaceFraction * totalRawWeight
    }

    return adjustedWeights
  }

  /**
   * Computes complete responsive metrics for the keyboard surface and all its rows.
   */
  fun computeKeyboardMetrics(
    availableParentWidthDp: Float,
    isLandscape: Boolean,
    portraitHeightPercent: Int,
    landscapeHeightPercent: Int,
    isCompactSearchMode: Boolean,
    mode: KeyboardMode,
    languageKind: LanguageInputMethodKind,
    enterActionType: EnterActionType,
    rows: List<List<KeySpec>>,
  ): ResponsiveKeyboardMetrics {
    val safeParentWidthDp = availableParentWidthDp.coerceAtLeast(MIN_SAFE_KEYBOARD_WIDTH_DP)
    val maxKeyboardWidthDp = computeMaxKeyboardWidthDp(safeParentWidthDp)
    val containerHorizontalPaddingDp = computeContainerHorizontalPaddingDp(maxKeyboardWidthDp)

    val activePercent = if (isLandscape) landscapeHeightPercent else portraitHeightPercent
    val baseRowHeightDp = KeyboardHeightBounds.rowHeightDpForPercent(activePercent, isLandscape)
    val toolbarHeightDp = KeyboardHeightBounds.toolbarHeightDpForPercent(activePercent, isLandscape)
    val heightScale =
      (baseRowHeightDp / (if (isLandscape) 38f else 50f)).coerceIn(0.60f, 1.80f)

    val searchRowHeightDp =
      (baseRowHeightDp * 0.72f).coerceIn(
        if (isLandscape) 28f else 36f,
        if (isLandscape) 38f else 46f,
      )
    val activeRowHeightDp = if (isCompactSearchMode) searchRowHeightDp else baseRowHeightDp
    val activeRowHeightScale = (activeRowHeightDp / 50f).coerceIn(0.60f, 1.80f)
    val keyVerticalPaddingDp = (2.2f * activeRowHeightScale).coerceIn(1.2f, 4.5f)
    val containerVerticalPaddingDp =
      if (isCompactSearchMode) 2f else (2.5f * heightScale)

    val baseKeyHorizontalPaddingDp = computeKeyHorizontalPaddingDp(maxKeyboardWidthDp)
    val isProbhat = languageKind == LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT

    val innerContainerWidthDp =
      (maxKeyboardWidthDp - containerHorizontalPaddingDp * 2f).coerceAtLeast(200f)

    val rowLayouts =
      rows.mapIndexed { idx, rowKeys ->
        val isNineKeyRow =
          (mode == KeyboardMode.ALPHABET || mode == KeyboardMode.EMOJI) && rowKeys.size == 9
        val sideIndentDp =
          if (isNineKeyRow) {
            computeNineKeyRowPaddingDp(maxKeyboardWidthDp, isProbhat)
          } else {
            0f
          }
        val usableRowWidthDp =
          (innerContainerWidthDp - sideIndentDp * 2f).coerceAtLeast(160f)
        val keyPadH = computeRowKeyHorizontalPaddingDp(maxKeyboardWidthDp, rowKeys.size)
        val effectiveWeights =
          computeEffectiveRowWeights(
            rowKeys = rowKeys,
            usableRowWidthDp = usableRowWidthDp,
            enterActionType = enterActionType,
          )
        val totalWeight = effectiveWeights.sum().coerceAtLeast(0.1f)
        val allocatedWidths =
          effectiveWeights.map { w ->
            (usableRowWidthDp * (w / totalWeight)).coerceAtLeast(16f)
          }

        ResponsiveRowLayout(
          rowIndex = idx,
          sideIndentationDp = sideIndentDp,
          keyHorizontalPaddingDp = keyPadH,
          keyVerticalPaddingDp = keyVerticalPaddingDp,
          effectiveWeights = effectiveWeights,
          allocatedKeyWidthsDp = allocatedWidths,
        )
      }

    return ResponsiveKeyboardMetrics(
      availableParentWidthDp = safeParentWidthDp,
      maxKeyboardWidthDp = maxKeyboardWidthDp,
      containerHorizontalPaddingDp = containerHorizontalPaddingDp,
      containerVerticalPaddingDp = containerVerticalPaddingDp,
      rowHeightDp = activeRowHeightDp,
      toolbarHeightDp = toolbarHeightDp,
      heightScale = heightScale,
      baseKeyHorizontalPaddingDp = baseKeyHorizontalPaddingDp,
      isLandscape = isLandscape,
      isUltraNarrow = maxKeyboardWidthDp < ULTRA_NARROW_WIDTH_DP,
      isVeryNarrow = maxKeyboardWidthDp < VERY_NARROW_WIDTH_DP,
      isNarrow = maxKeyboardWidthDp < NARROW_WIDTH_DP,
      rows = rowLayouts,
    )
  }

  /**
   * Computes responsive metrics for the top [KeyboardToolbar] so all left and right action
   * buttons fit without clipping across 280dp–1200dp widths while keeping the far-right
   * microphone button at its exact 48dp touch target and 24dp icon size.
   */
  fun computeToolbarMetrics(
    availableWidthDp: Float,
  ): ResponsiveToolbarMetrics {
    val safeWidth = availableWidthDp.coerceAtLeast(MIN_SAFE_KEYBOARD_WIDTH_DP)
    val isUltraNarrow = safeWidth < ULTRA_NARROW_WIDTH_DP
    val isCompact = safeWidth < 350f
    val isNarrow = safeWidth < 395f

    val containerPadH =
      when {
        isUltraNarrow -> 2.dp
        isCompact -> 3.dp
        else -> 4.dp
      }
    val buttonPaddingH =
      when {
        isUltraNarrow -> 3.0.dp
        isCompact -> 3.8.dp
        isNarrow -> 4.8.dp
        else -> 6.5.dp
      }
    val itemSpacing =
      when {
        isUltraNarrow -> 1.0.dp
        isCompact -> 1.2.dp
        isNarrow -> 2.0.dp
        else -> 3.0.dp
      }
    val rightClusterSpacing =
      when {
        isUltraNarrow -> 1.0.dp
        isCompact -> 1.0.dp
        else -> 1.5.dp
      }
    val badgeMinWidth = if (isUltraNarrow) 38.dp else 42.dp
    val badgeMaxWidth =
      when {
        isUltraNarrow -> 60.dp
        isCompact -> 66.dp
        isNarrow -> 72.dp
        else -> 78.dp
      }
    val leftIconSize = if (isCompact) 14.dp else 15.dp
    val rightArrowIconSize = if (isCompact) 15.dp else 17.dp
    val rightActionIconSize = if (isCompact) 15.dp else 16.dp

    return ResponsiveToolbarMetrics(
      availableWidthDp = safeWidth,
      horizontalContainerPaddingDp = containerPadH,
      buttonPaddingH = buttonPaddingH,
      itemSpacing = itemSpacing,
      rightClusterSpacing = rightClusterSpacing,
      badgeMinWidth = badgeMinWidth,
      badgeMaxWidth = badgeMaxWidth,
      leftIconSize = leftIconSize,
      rightArrowIconSize = rightArrowIconSize,
      rightActionIconSize = rightActionIconSize,
      showCursorArrows = !isUltraNarrow,
      micTouchTargetWidth = 48.dp,
      micIconSize = 24.dp,
    )
  }

  /**
   * Computes adaptive column count for the Emoji grid based on available width.
   */
  fun computeEmojiGridColumns(availableWidthDp: Float): Int {
    return when {
      availableWidthDp < 305f -> 7
      availableWidthDp > 520f -> 10
      else -> 8
    }
  }
}
