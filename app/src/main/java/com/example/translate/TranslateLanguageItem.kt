package com.example.translate

import androidx.compose.runtime.Immutable
import com.google.mlkit.nl.translate.TranslateLanguage

@Immutable
data class TranslateLanguageItem(
  val code: String,
  val displayName: String,
  val nativeName: String,
  val badge: String,
  val isAutoDetect: Boolean = false,
) {
  val searchKeywords: String by lazy {
    "$code $displayName $nativeName $badge".lowercase()
  }

  companion object {
    val AUTO_DETECT = TranslateLanguageItem(
      code = "auto",
      displayName = "Auto Detect",
      nativeName = "স্বয়ংক্রিয় সনাক্তকরণ / स्वतः पहचान",
      badge = "AUTO",
      isAutoDetect = true,
    )

    val BENGALI = TranslateLanguageItem(
      code = TranslateLanguage.BENGALI,
      displayName = "Bengali",
      nativeName = "বাংলা",
      badge = "BN",
    )

    val ENGLISH = TranslateLanguageItem(
      code = TranslateLanguage.ENGLISH,
      displayName = "English",
      nativeName = "English",
      badge = "EN",
    )

    val HINDI = TranslateLanguageItem(
      code = TranslateLanguage.HINDI,
      displayName = "Hindi",
      nativeName = "हिन्दी",
      badge = "HI",
    )

    /**
     * Primary supported languages: Bengali, English, Hindi are featured first,
     * followed by commonly translated international and regional languages.
     */
    val SUPPORTED_LANGUAGES: List<TranslateLanguageItem> = listOf(
      BENGALI,
      ENGLISH,
      HINDI,
      TranslateLanguageItem(TranslateLanguage.ARABIC, "Arabic", "العربية", "AR"),
      TranslateLanguageItem(TranslateLanguage.SPANISH, "Spanish", "Español", "ES"),
      TranslateLanguageItem(TranslateLanguage.FRENCH, "French", "Français", "FR"),
      TranslateLanguageItem(TranslateLanguage.GERMAN, "German", "Deutsch", "DE"),
      TranslateLanguageItem(TranslateLanguage.CHINESE, "Chinese", "中文", "ZH"),
      TranslateLanguageItem(TranslateLanguage.JAPANESE, "Japanese", "日本語", "JA"),
      TranslateLanguageItem(TranslateLanguage.KOREAN, "Korean", "한국어", "KO"),
      TranslateLanguageItem(TranslateLanguage.RUSSIAN, "Russian", "Русский", "RU"),
      TranslateLanguageItem(TranslateLanguage.PORTUGUESE, "Portuguese", "Português", "PT"),
      TranslateLanguageItem(TranslateLanguage.ITALIAN, "Italian", "Italiano", "IT"),
      TranslateLanguageItem(TranslateLanguage.TURKISH, "Turkish", "Türkçe", "TR"),
      TranslateLanguageItem(TranslateLanguage.URDU, "Urdu", "اردو", "UR"),
      TranslateLanguageItem(TranslateLanguage.TAMIL, "Tamil", "தமிழ்", "TA"),
      TranslateLanguageItem(TranslateLanguage.TELUGU, "Telugu", "తెలుగు", "TE"),
      TranslateLanguageItem(TranslateLanguage.MARATHI, "Marathi", "मराठी", "MR"),
      TranslateLanguageItem(TranslateLanguage.GUJARATI, "Gujarati", "ગુજરાતી", "GU"),
      TranslateLanguageItem(TranslateLanguage.INDONESIAN, "Indonesian", "Bahasa Indonesia", "ID"),
      TranslateLanguageItem(TranslateLanguage.VIETNAMESE, "Vietnamese", "Tiếng Việt", "VI"),
      TranslateLanguageItem(TranslateLanguage.DUTCH, "Dutch", "Nederlands", "NL"),
      TranslateLanguageItem(TranslateLanguage.POLISH, "Polish", "Polski", "PL"),
      TranslateLanguageItem(TranslateLanguage.SWEDISH, "Swedish", "Svenska", "SV"),
      TranslateLanguageItem(TranslateLanguage.THAI, "Thai", "ไทย", "TH"),
    )

    private val SOURCE_LANGUAGES: List<TranslateLanguageItem> =
      listOf(AUTO_DETECT) + SUPPORTED_LANGUAGES

    private val LANGUAGE_BY_CODE: Map<String, TranslateLanguageItem> =
      buildMap {
        put(AUTO_DETECT.code, AUTO_DETECT)
        for (item in SUPPORTED_LANGUAGES) {
          put(item.code.lowercase(), item)
        }
      }

    fun fromCode(code: String): TranslateLanguageItem {
      if (code == "auto") return AUTO_DETECT
      return LANGUAGE_BY_CODE[code.lowercase()]
        ?: TranslateLanguageItem(
          code = code,
          displayName = code.uppercase(),
          nativeName = code.uppercase(),
          badge = code.take(2).uppercase(),
        )
    }

    fun getSourceLanguages(): List<TranslateLanguageItem> {
      return SOURCE_LANGUAGES
    }

    fun getTargetLanguages(): List<TranslateLanguageItem> {
      return SUPPORTED_LANGUAGES
    }
  }
}
