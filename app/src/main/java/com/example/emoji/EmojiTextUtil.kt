package com.example.emoji

import java.text.BreakIterator
import java.util.concurrent.ConcurrentHashMap

object EmojiTextUtil {

  private val skinToneModifiers =
    listOf("\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")

  private val skinToneVariantsCache = ConcurrentHashMap<String, List<String>>()

  private val characterBreakIterator =
    object : ThreadLocal<BreakIterator>() {
      override fun initialValue(): BreakIterator = BreakIterator.getCharacterInstance()
    }

  /**
   * Strips any existing Fitzpatrick skin-tone modifier from [emoji] to recover the base emoji.
   */
  fun stripSkinToneModifier(emoji: String): String {
    if (emoji.length <= 2 || !emoji.contains('\uD83C')) return emoji
    var result = emoji
    for (mod in skinToneModifiers) {
      if (result.contains(mod)) {
        result = result.replace(mod, "")
      }
    }
    return result
  }

  /**
   * Generates skin tone variants for an emoji if supported.
   * If [baseEmoji] supports skin tones, returns 6 variants: Default (yellow) and 5 Fitzpatrick tones.
   */
  fun getSkinToneVariants(baseEmoji: String): List<String> {
    return skinToneVariantsCache.getOrPut(baseEmoji) {
      val normalizedBase = stripSkinToneModifier(baseEmoji)
      val cleanBase = normalizedBase.replace("\uFE0F", "")
      EmojiSkinTone.entries.map { tone ->
        if (tone.modifier.isEmpty()) {
          normalizedBase
        } else {
          // Fitzpatrick modifier directly follows the base character
          "$cleanBase${tone.modifier}"
        }
      }
    }
  }

  /**
   * Calculates the exact number of UTF-16 code units to delete for the last grapheme cluster
   * before the cursor, correctly handling:
   * - Single-code unit ASCII and standard characters (1 code unit)
   * - Surrogate pair emojis (e.g. 😀 = 2 code units)
   * - Emojis with variation selectors (e.g. ❤️ = 2 code units)
   * - Emojis with Fitzpatrick skin tone modifiers (e.g. 👍🏽 = 4 code units)
   * - Flag sequences (e.g. 🇧🇩 = 4 code units)
   * - Complex ZWJ sequences (e.g. 👨‍👩‍👧‍👦 family = 11 code units, 🧑‍💻 = 5 code units)
   */
  fun calculateGraphemeLengthToDelete(beforeText: String): Int {
    val len = beforeText.length
    if (len <= 1) return 1

    // Fast path for standard printable ASCII characters (0x20..0x7E) and newline/tab
    val lastChar = beforeText[len - 1]
    if (lastChar.code in 0x20..0x7E || lastChar == '\n' || lastChar == '\t') {
      return 1
    }

    val bi = characterBreakIterator.get() ?: BreakIterator.getCharacterInstance()
    bi.setText(beforeText)

    val end = bi.last()
    var start = bi.previous()

    if (start == BreakIterator.DONE || end <= start) {
      return 1
    }

    // Check if the sequence is connected across Zero-Width Joiners (\u200D) or Variation Selectors (\uFE0F)
    while (start > 0) {
      val prevChar = beforeText[start - 1]
      if (prevChar == '\u200D' || prevChar == '\uFE0F') {
        start--
        val prevBoundary = bi.preceding(start + 1)
        if (prevBoundary != BreakIterator.DONE && prevBoundary < start) {
          start = prevBoundary
        } else {
          start = (start - 1).coerceAtLeast(0)
        }
      } else {
        break
      }
    }

    val length = end - start
    return length.coerceAtLeast(1)
  }

  /**
   * Calculates the UTF-16 code unit length of the first grapheme cluster in [afterText]
   * for forward cursor movement.
   */
  fun calculateFirstGraphemeLength(afterText: String): Int {
    val len = afterText.length
    if (len <= 1) return 1

    // Fast path for ASCII character not followed by a combining mark or variation selector
    val firstChar = afterText[0]
    val secondChar = afterText[1]
    if (firstChar.code in 0x20..0x7E && secondChar.code in 0x20..0x7E) {
      return 1
    }

    val bi = characterBreakIterator.get() ?: BreakIterator.getCharacterInstance()
    bi.setText(afterText)

    val start = bi.first()
    val end = bi.next()
    if (end == BreakIterator.DONE || end <= start) {
      return 1
    }
    return (end - start).coerceAtLeast(1)
  }
}
