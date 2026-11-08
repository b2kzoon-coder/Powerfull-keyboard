package com.example.language

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LanguageInputMethodKind {
  DIRECT_QWERTY,
  DIRECT_BANGLA_PROBHAT,
  PHONETIC_HINDI,
  FIXED_LAYOUT,
}

@Immutable
data class KeyboardLanguage(
  val id: String,
  val displayName: String,
  val spacebarLabel: String,
  val badgeLabel: String,
  val kind: LanguageInputMethodKind = LanguageInputMethodKind.DIRECT_QWERTY,
  val isRtl: Boolean = false,
)

/**
 * Modular interface for registering additional language packs and layout engines in future
 * development stages without modifying core IME or UI code.
 */
interface LanguageTransliterationEngine {
  val languageId: String

  fun processInput(currentBuffer: String, char: Char): String
}

class LanguageManager {
  private val registeredLanguages =
    mutableListOf(
      EnglishLanguage,
      BanglaProbhatLanguage,
      BanglishLanguage,
      HindiHinglishLanguage,
    )

  private val _availableLanguages =
    MutableStateFlow<List<KeyboardLanguage>>(registeredLanguages.toList())
  val availableLanguages: StateFlow<List<KeyboardLanguage>> = _availableLanguages.asStateFlow()

  private val _activeLanguage = MutableStateFlow(EnglishLanguage)
  val activeLanguage: StateFlow<KeyboardLanguage> = _activeLanguage.asStateFlow()

  fun registerLanguage(language: KeyboardLanguage) {
    if (registeredLanguages.none { it.id == language.id }) {
      registeredLanguages.add(language)
      _availableLanguages.value = registeredLanguages.toList()
    }
  }

  /**
   * Spacebar swipe left (Forward Cycle):
   * English -> বাংলা (Probhat) -> Banglish -> हिन्दी (Hinglish) -> English.
   * Cycles indefinitely.
   *
   * @return true if the active language changed.
   */
  fun onSpacebarSwipeLeft(): Boolean {
    val current = _activeLanguage.value
    val target =
      when (current.id) {
        ID_ENGLISH -> BanglaProbhatLanguage
        ID_BANGLA_PROBHAT -> BanglishLanguage
        ID_BANGLISH -> HindiHinglishLanguage
        ID_HINDI_HINGLISH -> EnglishLanguage
        else -> EnglishLanguage
      }

    if (target.id == current.id) return false
    _activeLanguage.value = target
    return true
  }

  /**
   * Spacebar swipe right (Reverse Cycle):
   * English -> हिन्दी (Hinglish) -> Banglish -> বাংলা (Probhat) -> English.
   * Cycles indefinitely.
   *
   * @return true if the active language changed.
   */
  fun onSpacebarSwipeRight(): Boolean {
    val current = _activeLanguage.value
    val target =
      when (current.id) {
        ID_ENGLISH -> HindiHinglishLanguage
        ID_HINDI_HINGLISH -> BanglishLanguage
        ID_BANGLISH -> BanglaProbhatLanguage
        ID_BANGLA_PROBHAT -> EnglishLanguage
        else -> EnglishLanguage
      }

    if (target.id == current.id) return false
    _activeLanguage.value = target
    return true
  }

  /**
   * Accessible layout switcher:
   * Cycles English -> বাংলা (Probhat) -> Banglish -> हिन्दी (Hinglish) -> English.
   */
  fun switchToNextLanguage(): KeyboardLanguage {
    val list = _availableLanguages.value
    if (list.size <= 1) return _activeLanguage.value
    val currentIndex = list.indexOfFirst { it.id == _activeLanguage.value.id }
    val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % list.size
    val next = list[nextIndex]
    _activeLanguage.value = next
    return next
  }

  fun setActiveLanguage(languageId: String) {
    val normalizedId =
      when {
        languageId.equals("bn_AVRO", ignoreCase = true) -> ID_BANGLA_PROBHAT
        languageId.equals("bn-Latn", ignoreCase = true) ||
          languageId.equals("banglish", ignoreCase = true) -> ID_BANGLISH
        else -> languageId
      }
    val match = _availableLanguages.value.firstOrNull { it.id.equals(normalizedId, ignoreCase = true) }
    if (match != null) {
      _activeLanguage.value = match
    }
  }

  companion object {
    const val ID_ENGLISH = "en_US"
    const val ID_BANGLA_PROBHAT = "bn_PROBHAT"
    const val ID_BANGLISH = "bn_BANGLISH"
    const val ID_HINDI_HINGLISH = "hi_HINGLISH"

    fun normalizeLanguageId(languageId: String): String {
      return when {
        languageId.equals(ID_ENGLISH, ignoreCase = true) -> ID_ENGLISH
        languageId.equals(ID_BANGLA_PROBHAT, ignoreCase = true) ||
          languageId.equals("bn_AVRO", ignoreCase = true) -> ID_BANGLA_PROBHAT
        languageId.equals(ID_BANGLISH, ignoreCase = true) ||
          languageId.equals("bn-Latn", ignoreCase = true) ||
          languageId.equals("banglish", ignoreCase = true) -> ID_BANGLISH
        languageId.equals(ID_HINDI_HINGLISH, ignoreCase = true) -> ID_HINDI_HINGLISH
        else -> ID_ENGLISH
      }
    }

    val EnglishLanguage =
      KeyboardLanguage(
        id = ID_ENGLISH,
        displayName = "English",
        spacebarLabel = "English",
        badgeLabel = "EN",
        kind = LanguageInputMethodKind.DIRECT_QWERTY,
        isRtl = false,
      )

    val BanglaProbhatLanguage =
      KeyboardLanguage(
        id = ID_BANGLA_PROBHAT,
        displayName = "বাংলা (Probhat)",
        spacebarLabel = "◄  প্রভাত  ►",
        badgeLabel = "প্রভাত",
        kind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
        isRtl = false,
      )

    val BanglishLanguage =
      KeyboardLanguage(
        id = ID_BANGLISH,
        displayName = "Banglish",
        spacebarLabel = "Banglish",
        badgeLabel = "BN",
        kind = LanguageInputMethodKind.DIRECT_QWERTY,
        isRtl = false,
      )

    val HindiHinglishLanguage =
      KeyboardLanguage(
        id = ID_HINDI_HINGLISH,
        displayName = "हिन्दी (Hinglish)",
        spacebarLabel = "हिन्दी (Hinglish)",
        badgeLabel = "हिन्दी",
        kind = LanguageInputMethodKind.PHONETIC_HINDI,
        isRtl = false,
      )
  }
}
