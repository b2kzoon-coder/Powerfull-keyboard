package com.example.translate

import androidx.compose.runtime.Immutable

enum class LanguageSelectorMode {
  NONE,
  SOURCE,
  TARGET,
}

@Immutable
data class TranslatorUiState(
  val sourceLanguage: TranslateLanguageItem = TranslateLanguageItem.AUTO_DETECT,
  val targetLanguage: TranslateLanguageItem = TranslateLanguageItem.BENGALI,
  val inputText: String = "",
  val translatedText: String = "",
  val detectedLanguage: TranslateLanguageItem? = null,
  val isLoading: Boolean = false,
  val isDownloadingModel: Boolean = false,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
  val selectorMode: LanguageSelectorMode = LanguageSelectorMode.NONE,
  val languageSearchQuery: String = "",
) {
  val canSwap: Boolean
    get() = !sourceLanguage.isAutoDetect && sourceLanguage.code != targetLanguage.code

  val hasResult: Boolean
    get() = translatedText.isNotBlank()
}
