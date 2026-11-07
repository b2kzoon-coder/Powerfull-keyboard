package com.example.emoji

import androidx.compose.runtime.Immutable

/**
 * 10 standard categories for the Emoji keyboard, matching Unicode standards
 * and Ridmik Classic interaction layout.
 */
enum class EmojiCategory(
  val displayName: String,
  val iconSymbol: String,
) {
  RECENT("Recent", "🕒"),
  SMILEYS("Smileys", "😀"),
  PEOPLE("People", "👋"),
  ANIMALS("Animals", "🐶"),
  FOOD("Food", "🍔"),
  ACTIVITIES("Activities", "⚽"),
  TRAVEL("Travel", "✈️"),
  OBJECTS("Objects", "💡"),
  SYMBOLS("Symbols", "🔣"),
  FLAGS("Flags", "🏁"),
}

/**
 * Represents a single emoji entry with search keywords and optional skin-tone support.
 */
@Immutable
data class EmojiItem(
  val unicode: String,
  val name: String,
  val category: EmojiCategory,
  val keywords: List<String> = emptyList(),
  val supportsSkinTone: Boolean = false,
)

/**
 * Unicode Fitzpatrick skin tone modifiers (Types 1-6).
 */
enum class EmojiSkinTone(
  val modifier: String,
  val samplePreview: String,
  val label: String,
) {
  DEFAULT("", "🟡", "Default"),
  LIGHT("\uD83C\uDFFB", "🏻", "Light"),
  MEDIUM_LIGHT("\uD83C\uDFFC", "🏼", "Medium-Light"),
  MEDIUM("\uD83C\uDFFD", "🏽", "Medium"),
  MEDIUM_DARK("\uD83C\uDFFE", "🏾", "Medium-Dark"),
  DARK("\uD83C\uDFFF", "🏿", "Dark"),
}
