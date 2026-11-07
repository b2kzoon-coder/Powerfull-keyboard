package com.example.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class KeyboardThemePreset(val id: String, val displayName: String) {
  SLATE_DARK("slate_dark", "Slate Dark"),
  CRISP_LIGHT("crisp_light", "Crisp Light"),
  OLED_BLACK("oled_black", "OLED Pitch Black"),
  NORD_FROST("nord_frost", "Nord Frost");

  companion object {
    fun fromId(id: String): KeyboardThemePreset {
      return entries.firstOrNull { it.id == id } ?: SLATE_DARK
    }
  }
}

@Immutable
data class KeyboardPalette(
  val keyboardBackground: Color,
  val toolbarBackground: Color,
  val keyBackground: Color,
  val keyPressedBackground: Color,
  val functionalKeyBackground: Color,
  val functionalKeyPressedBackground: Color,
  val accentKeyBackground: Color,
  val keyTextPrimary: Color,
  val keyTextSecondary: Color,
  val accentKeyText: Color,
  val dividerColor: Color,
)

object KeyboardThemeRepository {
  private val SlateDarkPalette =
    KeyboardPalette(
      keyboardBackground = Color(0xFF0F172A),
      toolbarBackground = Color(0xFF1E293B),
      keyBackground = Color(0xFF1E293B),
      keyPressedBackground = Color(0xFF334155),
      functionalKeyBackground = Color(0xFF172033),
      functionalKeyPressedBackground = Color(0xFF283548),
      accentKeyBackground = Color(0xFF0284C7),
      keyTextPrimary = Color(0xFFF8FAFC),
      keyTextSecondary = Color(0xFF94A3B8),
      accentKeyText = Color(0xFFFFFFFF),
      dividerColor = Color(0xFF334155),
    )

  private val CrispLightPalette =
    KeyboardPalette(
      keyboardBackground = Color(0xFFE2E8F0),
      toolbarBackground = Color(0xFFCBD5E1),
      keyBackground = Color(0xFFFFFFFF),
      keyPressedBackground = Color(0xFFCBD5E1),
      functionalKeyBackground = Color(0xFFCBD5E1),
      functionalKeyPressedBackground = Color(0xFF94A3B8),
      accentKeyBackground = Color(0xFF0284C7),
      keyTextPrimary = Color(0xFF0F172A),
      keyTextSecondary = Color(0xFF475569),
      accentKeyText = Color(0xFFFFFFFF),
      dividerColor = Color(0xFFCBD5E1),
    )

  private val OledBlackPalette =
    KeyboardPalette(
      keyboardBackground = Color(0xFF000000),
      toolbarBackground = Color(0xFF0A0A0A),
      keyBackground = Color(0xFF141414),
      keyPressedBackground = Color(0xFF292929),
      functionalKeyBackground = Color(0xFF0C0C0C),
      functionalKeyPressedBackground = Color(0xFF222222),
      accentKeyBackground = Color(0xFF38BDF8),
      keyTextPrimary = Color(0xFFF1F5F9),
      keyTextSecondary = Color(0xFF71717A),
      accentKeyText = Color(0xFF020617),
      dividerColor = Color(0xFF27272A),
    )

  private val NordFrostPalette =
    KeyboardPalette(
      keyboardBackground = Color(0xFF2E3440),
      toolbarBackground = Color(0xFF3B4252),
      keyBackground = Color(0xFF434C5E),
      keyPressedBackground = Color(0xFF4C566A),
      functionalKeyBackground = Color(0xFF3B4252),
      functionalKeyPressedBackground = Color(0xFF4C566A),
      accentKeyBackground = Color(0xFF88C0D0),
      keyTextPrimary = Color(0xFFECEFF4),
      keyTextSecondary = Color(0xFFD8DEE9),
      accentKeyText = Color(0xFF2E3440),
      dividerColor = Color(0xFF4C566A),
    )

  fun paletteFor(preset: KeyboardThemePreset): KeyboardPalette {
    return when (preset) {
      KeyboardThemePreset.SLATE_DARK -> SlateDarkPalette
      KeyboardThemePreset.CRISP_LIGHT -> CrispLightPalette
      KeyboardThemePreset.OLED_BLACK -> OledBlackPalette
      KeyboardThemePreset.NORD_FROST -> NordFrostPalette
    }
  }
}
