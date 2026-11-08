package com.example.keyboard

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Centralized, independent typography, scaling, and alignment system for Personal Keyboard.
 *
 * Implements decoupled font metrics and rendering rules for:
 * 1. English QWERTY characters, labels, and spacebar.
 * 2. Bengali vowels (স্বরবর্ণ), consonants (ব্যঞ্জনবর্ণ), vowel signs/kar (কারচিহ্ন),
 *    fola (ফলা), juktoborno (যুক্তবর্ণ), ref (রেফ), and combining marks.
 * 3. Controlled dynamic keyboard height scaling:
 *    When the keyboard height setting increases from 100% to 180%, key row height grows,
 *    but key width remains fixed (especially critical for Row 1 in Probhat with 12 keys).
 *    A controlled sub-linear damping factor prevents Bengali glyphs from expanding horizontally
 *    beyond key boundaries while maintaining optimal vertical presence and legibility.
 */
object KeyboardTypography {

  // --- ENGLISH QWERTY TYPOGRAPHY (PRESERVED UNCHANGED) ---
  const val ENGLISH_CHAR_FONT_SIZE_SP = 18.5f
  const val ENGLISH_LABEL_FONT_SIZE_SP = 13.0f // Functional key labels ("?123", "ABC", "↵", etc.)
  const val ENGLISH_SPACEBAR_FONT_SIZE_SP = 13.5f
  const val ENGLISH_HINT_FONT_SIZE_SP = 9.0f

  // --- BENGALI TYPOGRAPHY (MEDIUM SIZE CALIBRATION - 45-55% KEY HEIGHT) ---
  // Calibrated to a comfortable medium font size (+12% increase over compact baseline).
  // Occupies ~48-52% of the key height, offering optimal clarity and visual weight
  // matching Redmi Keyboard, while leaving clean horizontal padding in 12-key rows.
  const val BENGALI_CHAR_FONT_SIZE_SP = 16.8f
  const val BENGALI_COMPOUND_FONT_SIZE_SP = 15.0f // Juktoborno / compound characters with hasanta/fola
  const val BENGALI_SPACEBAR_FONT_SIZE_SP = 13.5f // "◄  প্রভাত  ►", "বাংলা (Avro)"
  const val BENGALI_HINT_FONT_SIZE_SP = 9.2f     // Shifted secondary hints (ধ, ঊ, ঈ, etc.)
  const val BENGALI_DIGIT_FONT_SIZE_SP = 16.8f   // ১, ২, ৩, ৪, ৫, ৬, ৭, ৮, ৯, ০
  const val BENGALI_DARI_FONT_SIZE_SP = 17.0f    // । (Dari)

  // --- HINDI / DEVANAGARI TYPOGRAPHY ---
  const val DEVANAGARI_CHAR_FONT_SIZE_SP = 15.5f
  const val DEVANAGARI_SPACEBAR_FONT_SIZE_SP = 13.0f

  /**
   * Calculates a controlled scale factor for Bengali font size based on row height.
   *
   * When keyboard height scales from 100% (rowHeight ~50dp) to 180% (rowHeight ~90dp),
   * row height expands by up to 1.8x, but screen width and key width remain constant.
   * A sub-linear damping factor (0.32f) allows Bengali glyphs to grow comfortably with key height
   * without distorting character proportions or overflowing key edges.
   */
  fun getBengaliControlledScale(heightScale: Float): Float {
    return (1.0f + (heightScale - 1.0f) * 0.32f).coerceIn(0.70f, 1.30f)
  }

  /**
   * Linear scale factor for English keys.
   */
  fun getEnglishScale(heightScale: Float): Float {
    return heightScale.coerceIn(0.60f, 1.80f)
  }

  /**
   * Checks if a string contains any Bengali Unicode characters.
   * Unicode range for Bengali: U+0980 to U+09FF, plus Bengali dari (U+0964, U+0965).
   */
  fun isBengali(text: String): Boolean {
    for (i in 0 until text.length) {
      val ch = text[i]
      if ((ch in '\u0980'..'\u09FF') || ch == '।' || ch == '॥') return true
    }
    return false
  }

  /**
   * Checks if a character is a Bengali Unicode character.
   */
  fun isBengaliChar(ch: Char): Boolean {
    return (ch in '\u0980'..'\u09FF') || ch == '।' || ch == '॥'
  }

  /**
   * Checks if a string contains any Devanagari (Hindi) characters.
   */
  fun isDevanagari(text: String): Boolean {
    for (i in 0 until text.length) {
      if (text[i] in '\u0900'..'\u097F') return true
    }
    return false
  }

  /**
   * Checks if a Bengali character string is a compound character (Juktoborno or with Fola/Ref).
   */
  fun isBengaliCompound(text: String): Boolean {
    if (text.isEmpty()) return false
    if (text.length > 2) return true
    for (i in 0 until text.length) {
      if (text[i] == '্') return true
    }
    return false
  }

  /**
   * Calculates a controlled damping factor for system font scaling (LocalDensity.current.fontScale).
   *
   * On standard devices, fontScale is 1.0f.
   * On accessibility-enhanced devices (e.g., Samsung large font, Android vision accessibility),
   * fontScale can range from 1.15f up to 2.0f.
   *
   * A sub-linear damping curve allows key text to grow for enhanced readability while
   * capping extreme scaling so that functional labels ("Search", "Done", "Paste"),
   * 12-key Bengali Probhat rows, and secondary hints do not wrap or overflow key boundaries.
   */
  fun getEffectiveFontScaleDamping(fontScale: Float): Float {
    if (fontScale <= 1.0f) return 1.0f
    val targetScale = 1.0f + (fontScale - 1.0f) * 0.35f
    return (targetScale / fontScale).coerceIn(0.60f, 1.0f)
  }

  /**
   * Determines the exact font size for a key based on its content, role, heightScale, and optional system fontScale.
   */
  fun resolveFontSize(
    displayText: String,
    isSpacebar: Boolean,
    heightScale: Float,
    fontScale: Float = 1.0f,
  ): TextUnit {
    val fontDamping = getEffectiveFontScaleDamping(fontScale)
    val isBengali = isBengali(displayText)
    val isHindi = !isBengali && isDevanagari(displayText)

    return when {
      isSpacebar -> {
        if (isBengali) {
          val scale = getBengaliControlledScale(heightScale) * fontDamping
          (BENGALI_SPACEBAR_FONT_SIZE_SP * scale).sp
        } else if (isHindi) {
          val scale = getBengaliControlledScale(heightScale) * fontDamping
          (DEVANAGARI_SPACEBAR_FONT_SIZE_SP * scale).sp
        } else {
          val scale = getEnglishScale(heightScale) * fontDamping
          (ENGLISH_SPACEBAR_FONT_SIZE_SP * scale).sp
        }
      }

      isBengali -> {
        val scale = getBengaliControlledScale(heightScale) * fontDamping
        when {
          displayText == "।" -> (BENGALI_DARI_FONT_SIZE_SP * scale).sp
          displayText.length == 1 && displayText[0] in '০'..'৯' -> (BENGALI_DIGIT_FONT_SIZE_SP * scale).sp
          isBengaliCompound(displayText) -> (BENGALI_COMPOUND_FONT_SIZE_SP * scale).sp
          displayText.length > 2 -> (13.5f * scale).sp
          else -> (BENGALI_CHAR_FONT_SIZE_SP * scale).sp
        }
      }

      isHindi -> {
        val scale = getBengaliControlledScale(heightScale) * fontDamping
        (DEVANAGARI_CHAR_FONT_SIZE_SP * scale).sp
      }

      else -> {
        val scale = getEnglishScale(heightScale) * fontDamping
        if (displayText.length > 2) {
          (ENGLISH_LABEL_FONT_SIZE_SP * scale).sp
        } else {
          (ENGLISH_CHAR_FONT_SIZE_SP * scale).sp
        }
      }
    }
  }

  /**
   * Determines the font size for a secondary hint (top-right of key).
   */
  fun resolveHintFontSize(
    hintText: String,
    heightScale: Float,
    fontScale: Float = 1.0f,
  ): TextUnit {
    val fontDamping = getEffectiveFontScaleDamping(fontScale)
    return if (isBengali(hintText)) {
      val scale = getBengaliControlledScale(heightScale) * fontDamping
      (BENGALI_HINT_FONT_SIZE_SP * scale).sp
    } else {
      val scale = getEnglishScale(heightScale) * fontDamping
      (ENGLISH_HINT_FONT_SIZE_SP * scale).sp
    }
  }

  /**
   * Sub-linear height scale for multi-character functional labels ("?123", "ABC", "Search", "Done")
   * and emoji icons so increasing vertical row height (e.g. default 130% up to 180%) does not
   * expand label width beyond horizontal key boundaries.
   */
  fun getFunctionalLabelControlledScale(heightScale: Float): Float {
    return (1.0f + (heightScale - 1.0f) * 0.28f).coerceIn(0.72f, 1.22f)
  }

  /**
   * Width- and height-aware key font size resolver used by [KeyButton].
   * Ensures labels (`?123`, `😊`, `Search`, `Done`, 12-key Probhat characters, spacebar labels)
   * never overflow or ellipsis horizontally on narrow screens, high display zoom, or tall row heights.
   */
  fun resolveResponsiveKeyFontSize(
    displayText: String,
    isSpacebar: Boolean,
    heightScale: Float,
    fontScale: Float = 1.0f,
    allocatedKeyWidthDp: Float = 0f,
    keyHorizontalPaddingDp: Float = 1.8f,
  ): TextUnit {
    val baseFontSize = resolveFontSize(displayText, isSpacebar, heightScale, fontScale)
    if (allocatedKeyWidthDp <= 0f || displayText.isEmpty()) {
      return baseFontSize
    }

    val fontDamping = getEffectiveFontScaleDamping(fontScale)
    val innerWidthDp = (allocatedKeyWidthDp - (keyHorizontalPaddingDp * 2f) - 3.0f).coerceAtLeast(10f)
    val codePointCount = displayText.codePointCount(0, displayText.length).coerceAtLeast(1)
    val isBengaliText = isBengali(displayText)
    val isHindiText = !isBengaliText && isDevanagari(displayText)

    val heightAdjustedSp =
      when {
        isSpacebar -> baseFontSize.value
        !isBengaliText && !isHindiText && displayText.length > 2 -> {
          // Multi-char functional label ("?123", "ABC", "Search", "Done", "=\\<")
          ENGLISH_LABEL_FONT_SIZE_SP * getFunctionalLabelControlledScale(heightScale) * fontDamping
        }
        codePointCount == 1 && Character.codePointAt(displayText, 0) > 0x1F000 -> {
          // Emoji key ("😊")
          16.5f * getFunctionalLabelControlledScale(heightScale) * fontDamping
        }
        else -> baseFontSize.value
      }

    // Calculate horizontal width-constrained ceiling (in sp) so text never clips horizontally
    val maxWidthFittedSp =
      when {
        isSpacebar -> {
          val avgGlyphWidthFactor = if (isBengaliText || isHindiText) 0.56f else 0.52f
          (innerWidthDp / (codePointCount * avgGlyphWidthFactor)).coerceIn(9.5f, 15.5f) * fontDamping
        }
        displayText.length > 2 && !isBengaliText && !isHindiText -> {
          // Functional labels ("?123", "ABC", "Search", "Done", "Send", "Next")
          (innerWidthDp / (codePointCount * 0.60f)).coerceIn(9.0f, 15.0f) * fontDamping
        }
        isBengaliText -> {
          // Single or compound Bengali character in 12-key or 9-key row
          (innerWidthDp * 0.72f).coerceIn(11.0f, 21.0f) * fontDamping
        }
        else -> {
          (innerWidthDp * 0.74f).coerceIn(11.0f, 23.0f) * fontDamping
        }
      }

    val finalSp = minOf(heightAdjustedSp, maxWidthFittedSp).coerceAtLeast(8.5f)
    return finalSp.sp
  }

  /**
   * Width-aware secondary hint font size resolver for top-right key hints.
   */
  fun resolveResponsiveHintFontSize(
    hintText: String,
    heightScale: Float,
    fontScale: Float = 1.0f,
    allocatedKeyWidthDp: Float = 0f,
  ): TextUnit {
    val baseHint = resolveHintFontSize(hintText, heightScale, fontScale)
    if (allocatedKeyWidthDp <= 0f) return baseHint
    val fontDamping = getEffectiveFontScaleDamping(fontScale)
    val widthCapSp = (allocatedKeyWidthDp * 0.34f).coerceIn(7.0f, 11.5f) * fontDamping
    return minOf(baseHint.value, widthCapSp).coerceAtLeast(6.8f).sp
  }
}
