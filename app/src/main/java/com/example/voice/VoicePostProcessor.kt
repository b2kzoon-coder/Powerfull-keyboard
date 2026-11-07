package com.example.voice

import java.util.Locale

/**
 * Intelligent local rule-based and regex post-processor for Voice Typing V2.
 *
 * Runs entirely on-device with zero latency, zero cloud LLM API dependencies,
 * and zero network usage.
 *
 * Responsibilities:
 * 1. Intelligent Punctuation:
 *    - Question detection for spoken Bengali and English (appends '?')
 *    - Clause boundary comma insertion before subordinate conjunctions
 *    - Declarative termination ('।' for Bengali, '.' for English/Banglish)
 * 2. Code-Switching Normalization:
 *    - Clean word boundaries and spacing between Bengali script and Latin words
 *    - Preserves English words during Banglish transliteration
 * 3. Punctuation Spacing & Deduplication:
 *    - Eliminates spaces before punctuation and ensures clean trailing spacing
 *    - Deduplicates repeated punctuation marks
 * 4. Auto-Capitalization:
 *    - Capitalizes start of sentences and post-terminator clauses in Latin scripts
 */
object VoicePostProcessor {

  // Bengali question interrogatives & particles
  private val BENGALI_INTERROGATIVES = setOf(
    "কি", "কী", "কেন", "কোথায়", "কোথায়", "কখন", "কিভাবে", "কীভাবে",
    "কেমন", "কার", "কাকে", "কাদের", "কোন", "কোনটা", "কোনটি", "নাকি",
    "কিনা", "কত", "কয়টা", "কয়টা", "কে", "কারা",
  )

  // English question starters & auxiliary verbs
  private val ENGLISH_QUESTION_STARTERS = setOf(
    "what", "why", "when", "where", "who", "whom", "whose", "which", "how",
    "is", "are", "am", "was", "were", "do", "does", "did", "can", "could",
    "will", "would", "should", "shall", "may", "might", "have", "has", "had",
    "aren't", "isn't", "wasn't", "weren't", "don't", "doesn't", "didn't",
    "can't", "couldn't", "won't", "wouldn't", "shouldn't",
  )

  // Bengali subordinate conjunctions that introduce clauses
  private val BENGALI_CONJUNCTIONS = listOf(
    "কিন্তু", "কারণ", "তবে", "সুতরাং", "যদিও", "তাহলে", "যাতে", "নইলে",
  )

  // English conjunctions that introduce clauses
  private val ENGLISH_CONJUNCTIONS = listOf(
    "but", "however", "although", "whereas", "because",
  )

  private val RE_MULTIPLE_SPACES = Regex("[ \\t]+")
  private val RE_SPACE_BEFORE_PUNCT = Regex("\\s+([.,!?:;|।])")
  private val RE_MISSING_SPACE_AFTER_PUNCT = Regex("([.,!?:;|।])(?=[^\\s.,!?:;|।\\d])")
  private val RE_DUPLICATE_PUNCT = Regex("([.,!?:;|।])\\1+")
  private val RE_BN_EN_BOUNDARY_1 = Regex("([\\u0980-\\u09FF])([a-zA-Z])")
  private val RE_BN_EN_BOUNDARY_2 = Regex("([a-zA-Z])([\\u0980-\\u09FF])")

  /**
   * Main entry point to post-process recognized text.
   *
   * @param rawText Raw text string from speech recognizer.
   * @param language Active VoiceLanguage.
   * @param isFinal True if this is the final speech result, false for streaming partials.
   * @param autoPunctuate If true, appends terminal punctuation and clause commas.
   */
  fun process(
    rawText: String,
    language: VoiceLanguage,
    isFinal: Boolean = false,
    autoPunctuate: Boolean = false,
  ): String {
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return ""

    // 1. Normalize code-switching boundaries between Bengali script and Latin text
    var text = normalizeCodeSwitching(trimmed)

    // 2. Format punctuation spacing and deduplicate marks
    text = normalizePunctuationSpacing(text)

    // 3. For Banglish mode, transliterate Bengali segments into clean Romanized text
    if (language.isBanglishOutput) {
      text = BanglishTransliterator.transliterate(text)
    }

    // 4. Intelligent punctuation on final results (when requested / enabled)
    if (isFinal && autoPunctuate) {
      text = applyIntelligentPunctuation(text, language)
    }

    // 5. Auto-capitalization for Latin text (English and Banglish)
    if (language.isBanglishOutput || language.tag.startsWith("en", ignoreCase = true)) {
      text = applyAutoCapitalization(text)
    }

    return text.trim()
  }

  /**
   * Ensures clean word boundaries when the speaker code-switches between Bengali and English.
   * e.g., "আমিofficeযাচ্ছি" -> "আমি office যাচ্ছি"
   */
  fun normalizeCodeSwitching(text: String): String {
    if (text.isEmpty()) return text
    var res = RE_BN_EN_BOUNDARY_1.replace(text, "$1 $2")
    res = RE_BN_EN_BOUNDARY_2.replace(res, "$1 $2")
    return RE_MULTIPLE_SPACES.replace(res, " ")
  }

  /**
   * Fixes spacing around punctuation and removes duplicate punctuation marks.
   */
  fun normalizePunctuationSpacing(text: String): String {
    if (text.isEmpty()) return text
    var res = RE_SPACE_BEFORE_PUNCT.replace(text, "$1")
    res = RE_DUPLICATE_PUNCT.replace(res, "$1")
    res = RE_MISSING_SPACE_AFTER_PUNCT.replace(res, "$1 ")
    return res
  }

  /**
   * Applies rule-based intelligent punctuation based on speech intent:
   * - Clause commas before conjunctions (কিন্তু, কারণ, but, however)
   * - Interrogative question marks ('?')
   * - Declarative statement terminators ('।' for Bengali, '.' for Latin)
   */
  fun applyIntelligentPunctuation(text: String, language: VoiceLanguage): String {
    if (text.isEmpty()) return text
    var res = text

    // 1. Mid-sentence clause commas
    res = insertClauseCommas(res)

    // 2. Terminal punctuation
    val lastChar = res.last()
    if (lastChar == '?' || lastChar == '!' || lastChar == '.' || lastChar == '।' || lastChar == '॥') {
      return res
    }

    val isQuestion = detectQuestionIntent(res, language)
    return if (isQuestion) {
      "$res?"
    } else {
      if (containsBengali(res)) {
        "$res।"
      } else {
        "$res."
      }
    }
  }

  /**
   * Inserts commas before conjunctions when separating clauses.
   */
  private fun insertClauseCommas(text: String): String {
    var res = text

    // Bengali conjunctions
    for (conj in BENGALI_CONJUNCTIONS) {
      val pattern = Regex("(?<=[^,?!|।\\s])\\s+($conj)\\b")
      res = pattern.replace(res, ", $1")
    }

    // English conjunctions
    for (conj in ENGLISH_CONJUNCTIONS) {
      val pattern = Regex("(?<=[^,?!.\\s])\\s+($conj)\\b", RegexOption.IGNORE_CASE)
      res = pattern.replace(res, ", $1")
    }

    return res
  }

  /**
   * Detects whether an utterance has question/interrogative intent.
   */
  fun detectQuestionIntent(text: String, language: VoiceLanguage): Boolean {
    val clean = text.trim()
    if (clean.isEmpty()) return false

    // Check Bengali question markers
    if (containsBengali(clean)) {
      val words = clean.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
      for (w in words) {
        val normalizedWord = w.trim('?', '!', '.', '।', ',')
        if (BENGALI_INTERROGATIVES.contains(normalizedWord)) {
          return true
        }
      }
      if (words.size >= 2) {
        val lastWord = words.last().trim('?', '!', '.', '।', ',')
        if (lastWord == "নাকি") {
          return true
        }
      }
    }

    // Check English / Banglish question markers
    val lower = clean.lowercase(Locale.ROOT)
    val latinWords = lower.split(Regex("[\\s,]+")).filter { it.isNotBlank() }
    if (latinWords.isNotEmpty()) {
      val firstWord = latinWords.first().trim('?', '!', '.', ',')
      if (ENGLISH_QUESTION_STARTERS.contains(firstWord)) {
        return true
      }
      // Common Banglish question words
      val banglishInterrogatives = setOf("ki", "kemon", "kothay", "keno", "kokhon", "kivabe", "kar", "konta", "naki")
      if (latinWords.any { banglishInterrogatives.contains(it.trim('?', '!', '.', ',')) }) {
        return true
      }
    }

    return false
  }

  /**
   * Capitalizes first character of the text and following sentence terminators (. ? !).
   */
  fun applyAutoCapitalization(text: String): String {
    if (text.isEmpty()) return text
    val chars = text.toCharArray()
    var capitalizeNext = true

    for (i in chars.indices) {
      val c = chars[i]
      if (c.isLetter() && capitalizeNext) {
        chars[i] = c.uppercaseChar()
        capitalizeNext = false
      } else if (c == '.' || c == '?' || c == '!' || c == '\n') {
        capitalizeNext = true
      } else if (!c.isWhitespace() && c != '"' && c != '\'') {
        capitalizeNext = false
      }
    }

    return String(chars)
  }

  private fun containsBengali(text: String): Boolean {
    for (ch in text) {
      if (ch.code in 0x0980..0x09FF) return true
    }
    return false
  }
}
