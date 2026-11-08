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

  // Bengali question interrogatives & particles (including conversational Bangladesh Bengali)
  private val BENGALI_INTERROGATIVES = setOf(
    "কি", "কী", "কেন", "ক্যান", "কোথায়", "কোথায়", "কই", "কখন", "কবে",
    "কিভাবে", "কীভাবে", "কেমনে", "কেমন", "কার", "কাকে", "কাদের", "কোন",
    "কোনটা", "কোনটি", "কোনখানে", "কিসে", "কিসের", "নাকি", "কিনা", "কত",
    "কয়", "কয়", "কয়টা", "কয়টা", "কয়টায়", "কয়টায়", "কে", "কারা",
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
  private val RE_DUPLICATE_BENGALI_KAR = Regex("([\\u09BE-\\u09CC\\u09D7])\\1+")
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

    // 1. Conservative Bengali Unicode normalization (canonical nukta consonants, split kars, stray artifacts)
    var text = if (containsBengali(trimmed)) normalizeBengaliUnicode(trimmed) else trimmed

    // 2. Normalize code-switching boundaries between Bengali script and Latin text
    text = normalizeCodeSwitching(text)

    // 3. Format punctuation spacing and deduplicate marks
    text = normalizePunctuationSpacing(text)

    // 4. For Banglish mode, transliterate Bengali segments into clean Romanized text
    if (language.isBanglishOutput) {
      text = BanglishTransliterator.transliterate(text)
    }

    // 5. Intelligent punctuation on final results (when requested / enabled)
    if (isFinal && autoPunctuate) {
      text = applyIntelligentPunctuation(text, language)
    }

    // 6. Auto-capitalization for Latin text (English and Banglish)
    if (language.isBanglishOutput || language.tag.startsWith("en", ignoreCase = true)) {
      text = applyAutoCapitalization(text)
    }

    return text.trim()
  }

  /**
   * Applies conservative, non-destructive Bengali Unicode normalization:
   * - Composes decomposed nukta pairs (ড+় -> ড়, ঢ+় -> ঢ়, য+় -> য়)
   * - Composes split two-part vowel signs (ে+া -> ো, ে+ৗ -> ৌ)
   * - Removes stray dotted circles (\u25CC) and zero-width spaces (\u200B, \uFEFF) while preserving ZWNJ/ZWJ
   * - Collapses accidental duplicated consecutive dependent vowel signs (কার)
   * Never alters lexical choice or replaces words.
   */
  fun normalizeBengaliUnicode(text: String): String {
    if (text.isEmpty()) return text
    val sb = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
      val c = text[i]
      if (c == '\u25CC' || c == '\u200B' || c == '\uFEFF') {
        i++
        continue
      }
      if (i + 1 < text.length) {
        val next = text[i + 1]
        if (next == '\u09BC') {
          when (c) {
            '\u09A1' -> {
              sb.append('\u09DC') // ড়
              i += 2
              continue
            }
            '\u09A2' -> {
              sb.append('\u09DD') // ঢ়
              i += 2
              continue
            }
            '\u09AF' -> {
              sb.append('\u09DF') // য়
              i += 2
              continue
            }
          }
        } else if (c == '\u09C7') {
          if (next == '\u09BE') {
            sb.append('\u09CB') // ো
            i += 2
            continue
          } else if (next == '\u09D7') {
            sb.append('\u09CC') // ৌ
            i += 2
            continue
          }
        }
      }
      sb.append(c)
      i++
    }
    return RE_DUPLICATE_BENGALI_KAR.replace(sb.toString(), "$1")
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
      // Common Banglish question words (including conversational Bangladesh Bengali)
      val banglishInterrogatives = setOf(
        "ki", "kemon", "kemne", "kothay", "koi", "keno", "kokhon", "kobe",
        "kivabe", "kar", "konta", "kise", "kiser", "naki",
      )
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

  /**
   * Computes Word Error Rate (WER) between a reference transcript and a recognized hypothesis.
   * Strips terminal punctuation for fair lexical comparison and normalizes Bengali Unicode.
   */
  fun calculateWordErrorRate(reference: String, hypothesis: String): Double {
    val refWords = tokenizeForEvaluation(reference)
    val hypWords = tokenizeForEvaluation(hypothesis)
    if (refWords.isEmpty()) return if (hypWords.isEmpty()) 0.0 else 1.0
    val distance = levenshteinDistance(refWords, hypWords)
    return distance.toDouble() / refWords.size.toDouble()
  }

  /**
   * Computes Character Error Rate (CER) between a reference transcript and a recognized hypothesis.
   */
  fun calculateCharacterErrorRate(reference: String, hypothesis: String): Double {
    val normRef = normalizeBengaliUnicode(reference.trim()).replace(RE_MULTIPLE_SPACES, " ")
    val normHyp = normalizeBengaliUnicode(hypothesis.trim()).replace(RE_MULTIPLE_SPACES, " ")
    if (normRef.isEmpty()) return if (normHyp.isEmpty()) 0.0 else 1.0
    val refChars = normRef.map { it.toString() }
    val hypChars = normHyp.map { it.toString() }
    val distance = levenshteinDistance(refChars, hypChars)
    return distance.toDouble() / refChars.size.toDouble()
  }

  private fun tokenizeForEvaluation(text: String): List<String> {
    val normalized = normalizeBengaliUnicode(text.trim())
    return normalized
      .split(Regex("[\\s.,!?:;|।॥]+"))
      .map { it.trim() }
      .filter { it.isNotEmpty() }
  }

  private fun <T> levenshteinDistance(a: List<T>, b: List<T>): Int {
    val m = a.size
    val n = b.size
    val dp = IntArray(n + 1) { it }
    for (i in 1..m) {
      var prev = dp[0]
      dp[0] = i
      for (j in 1..n) {
        val temp = dp[j]
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
        prev = temp
      }
    }
    return dp[n]
  }

  internal fun containsBengali(text: String): Boolean {
    for (ch in text) {
      if (ch.code in 0x0980..0x09FF) return true
    }
    return false
  }
}
