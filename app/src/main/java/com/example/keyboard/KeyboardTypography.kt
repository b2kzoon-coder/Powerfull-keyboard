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
}
