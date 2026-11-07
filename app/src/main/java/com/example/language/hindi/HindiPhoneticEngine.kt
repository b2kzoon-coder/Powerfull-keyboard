package com.example.language.hindi

/**
 * Fast, offline Roman-to-Hindi (Hinglish) phonetic transliteration engine.
 * Transliterates Romanized English input (Hinglish) into proper Devanagari Hindi script.
 *
 * Fully supports:
 * - High-frequency dictionary for common conversational Hinglish words
 * - Independent vowels and dependent vowel signs (matras)
 * - Aspirated consonants and consonants (k, kh, g, gh, ch, chh, j, jh, t, th, d, dh, p, ph, b, bh, etc.)
 * - Conjuncts (ksh = क्ष, tr = त्र, gy = ज्ञ, sh = श, shh = ष, etc.)
 * - Anusvara (ं) and Chandrabindu (ँ)
 * - Automatic virama (halant = ्) composition between consonants
 */
object HindiPhoneticEngine {

  private val CommonWords =
    mapOf(
      // Greetings & Common
      "namaste" to "नमस्ते",
      "namaskar" to "नमस्कार",
      "pranam" to "प्रणाम",
      "shukriya" to "शुक्रिया",
      "dhanyawad" to "धन्यवाद",
      "dhanyavad" to "धन्यवाद",
      "alvida" to "अलविदा",
      "swagat" to "स्वागत",
      "kripya" to "कृपया",
      "maaf" to "माफ़",

      // Pronouns
      "main" to "मैं",
      "mera" to "मेरा",
      "mere" to "मेरे",
      "meri" to "मेरी",
      "mujhe" to "मुझे",
      "mujhko" to "मुझको",
      "hum" to "हम",
      "hamara" to "हमारा",
      "hamare" to "हमारे",
      "hamari" to "हमारी",
      "humein" to "हमें",
      "humko" to "हमको",
      "aap" to "आप",
      "aapka" to "आपका",
      "aapke" to "आपके",
      "aapki" to "आपकी",
      "aapko" to "आपको",
      "tum" to "तुम",
      "tumhara" to "तुम्हारा",
      "tumhare" to "तुम्हारे",
      "tumhari" to "तुम्हारी",
      "tumhe" to "तुम्हें",
      "tumko" to "तुमको",
      "yeh" to "यह",
      "ye" to "ये",
      "woh" to "वह",
      "wo" to "वो",
      "is" to "इस",
      "us" to "उस",
      "in" to "इन",
      "un" to "उन",
      "iska" to "इसका",
      "iske" to "इसके",
      "iski" to "इसकी",
      "isko" to "इसको",
      "uska" to "उसका",
      "uske" to "उसके",
      "uski" to "उसकी",
      "usko" to "उसको",
      "kisko" to "किसको",
      "kiska" to "किसका",
      "sab" to "सब",
      "sabka" to "सबका",
      "sabke" to "सबके",
      "sabki" to "सबकी",
      "sabhi" to "सभी",
      "koi" to "कोई",
      "kuch" to "कुछ",

      // Question words
      "kya" to "क्या",
      "kaise" to "कैसे",
      "kaisa" to "कैसा",
      "kaisi" to "कैसी",
      "kyun" to "क्यों",
      "kyon" to "क्यों",
      "kab" to "कब",
      "kahan" to "कहाँ",
      "kaha" to "कहा",
      "kaun" to "कौन",
      "kitna" to "कितना",
      "kitne" to "कितने",
      "kitni" to "कितनी",

      // Auxiliary verbs
      "hai" to "है",
      "hain" to "हैं",
      "ho" to "हो",
      "hoon" to "हूँ",
      "hun" to "हूँ",
      "tha" to "था",
      "the" to "थे",
      "thi" to "थी",
      "theen" to "थीं",
      "hoga" to "होगा",
      "hogi" to "होगी",
      "honge" to "होंगे",

      // Affirmation / Negation
      "haan" to "हाँ",
      "ha" to "हाँ",
      "nahi" to "नहीं",
      "nahin" to "नहीं",
      "na" to "ना",
      "mat" to "मत",

      // Relationships & People
      "bhai" to "भाई",
      "behan" to "बहन",
      "bahen" to "बहन",
      "dost" to "दोस्त",
      "mitra" to "मित्र",
      "yaar" to "यार",
      "papa" to "पापा",
      "mummy" to "मम्मी",
      "maa" to "माँ",
      "dada" to "दादा",
      "dadi" to "दादी",
      "nana" to "नाना",
      "nani" to "नानी",
      "beta" to "बेटा",
      "beti" to "बेटी",
      "baccha" to "बच्चा",
      "bacche" to "बच्चे",
      "log" to "लोग",

      // Common adjectives & Adverbs
      "accha" to "अच्छा",
      "achha" to "अच्छा",
      "acche" to "अच्छे",
      "achhe" to "अच्छे",
      "acchi" to "अच्छी",
      "achhi" to "अच्छी",
      "bura" to "बुरा",
      "bure" to "बुरे",
      "buri" to "बुरी",
      "bahut" to "बहुत",
      "zyada" to "ज़्यादा",
      "jyada" to "ज्यादा",
      "kam" to "कम",
      "thoda" to "थोड़ा",
      "thode" to "थोड़े",
      "thodi" to "थोड़ी",
      "sahi" to "सही",
      "galat" to "गलत",
      "bada" to "बड़ा",
      "bade" to "बड़े",
      "badi" to "बड़ी",
      "chota" to "छोटा",
      "chote" to "छोटे",
      "choti" to "छोटी",
      "naya" to "नया",
      "naye" to "नए",
      "nayi" to "नई",
      "purana" to "पुराना",
      "sundar" to "सुंदर",

      // Places & Things
      "bharat" to "भारत",
      "desh" to "देश",
      "shahar" to "शहर",
      "shehar" to "शहर",
      "gao" to "गाँव",
      "gaanv" to "गाँव",
      "ghar" to "घर",
      "kamra" to "कमरा",
      "pani" to "पानी",
      "jal" to "जल",
      "khana" to "खाना",
      "peena" to "पीना",
      "chai" to "चाय",
      "roti" to "रोटी",
      "kitab" to "किताब",
      "pustak" to "पुस्तक",
      "pyaar" to "प्यार",
      "pyar" to "प्यार",
      "prem" to "प्रेम",
      "khushi" to "ख़ुशी",
      "dukh" to "दुख",
      "kaam" to "काम",
      "baat" to "बात",
      "cheez" to "चीज़",

      // Time
      "aaj" to "आज",
      "kal" to "कल",
      "parson" to "परसों",
      "ab" to "अब",
      "abhi" to "अभी",
      "tab" to "तब",
      "tabhi" to "तभी",
      "subah" to "सुबह",
      "shaam" to "शाम",
      "dophar" to "दोपहर",
      "raat" to "रात",
      "din" to "दिन",
      "samay" to "समय",
      "waqt" to "वक़्त",

      // Common verbs
      "karna" to "करना",
      "karte" to "करते",
      "karta" to "करता",
      "karti" to "करती",
      "karo" to "करो",
      "kiya" to "किया",
      "hona" to "होना",
      "hote" to "होते",
      "hota" to "होता",
      "hoti" to "होती",
      "aana" to "आना",
      "aao" to "आओ",
      "aaye" to "आए",
      "jaana" to "जाना",
      "jao" to "जाओ",
      "gaye" to "गए",
      "gaya" to "गया",
      "gayi" to "गई",
      "bolna" to "बोलना",
      "bolo" to "बोलो",
      "bola" to "बोला",
      "boli" to "बोली",
      "dekhna" to "देखना",
      "dekho" to "देखो",
      "dekha" to "देखा",
      "sunna" to "सुनना",
      "suno" to "सुनो",
      "suna" to "सुना",
      "dena" to "देना",
      "do" to "दो",
      "diya" to "दिया",
      "lena" to "लेना",
      "lo" to "लो",
      "liya" to "लिया",
      "chalo" to "चलो",
      "chal" to "चल",
      "sochna" to "सोचना",
      "socho" to "सोचो",
      "samajh" to "समझ",
      "samajhna" to "समझना",
      "milna" to "मिलना",
      "milo" to "मिलो",
    )

  // Independent Vowels
  private val IndependentVowels =
    listOf(
      "aau" to "औ",
      "aai" to "ऐ",
      "aa" to "आ",
      "ee" to "ई",
      "ii" to "ई",
      "oo" to "ऊ",
      "uu" to "ऊ",
      "ai" to "ऐ",
      "au" to "औ",
      "ou" to "औ",
      "ri" to "ऋ",
      "a" to "अ",
      "i" to "इ",
      "u" to "उ",
      "e" to "ए",
      "o" to "ओ",
    )

  // Dependent Vowel Signs (Matras)
  private val DependentVowels =
    listOf(
      "aau" to "ौ",
      "aai" to "ै",
      "aa" to "ा",
      "ee" to "ी",
      "ii" to "ी",
      "oo" to "ू",
      "uu" to "ू",
      "ai" to "ै",
      "au" to "ौ",
      "ou" to "ौ",
      "ri" to "ृ",
      "a" to "", // Inherent vowel - removes halant
      "i" to "ि",
      "u" to "ु",
      "e" to "े",
      "o" to "ो",
    )

  // Consonants ordered by length descending for longest match
  private val Consonants =
    listOf(
      "kkh" to "क्ख",
      "ggh" to "ग्घ",
      "cch" to "च्छ",
      "jjh" to "ज्झ",
      "tth" to "त्थ",
      "ddh" to "द्ध",
      "pph" to "प्फ",
      "bbh" to "ब्भ",
      "ksh" to "क्ष",
      "gy" to "ज्ञ",
      "tr" to "त्र",
      "shh" to "ष",
      "chh" to "छ",
      "kh" to "ख",
      "gh" to "घ",
      "ch" to "च",
      "jh" to "झ",
      "th" to "थ",
      "dh" to "ध",
      "ph" to "फ",
      "bh" to "भ",
      "sh" to "श",
      "k" to "क",
      "g" to "ग",
      "j" to "ज",
      "t" to "त",
      "d" to "द",
      "n" to "न",
      "p" to "प",
      "b" to "ब",
      "m" to "म",
      "y" to "य",
      "r" to "र",
      "l" to "ल",
      "v" to "व",
      "w" to "व",
      "s" to "स",
      "h" to "ह",
      "f" to "फ़",
      "z" to "ज़",
    )

  private const val MAX_WORD_CACHE_SIZE = 256
  private val wordTransliterationCache = java.util.concurrent.ConcurrentHashMap<String, String>(128)

  // Pre-built consonant map with halant attached for seamless chaining
  private val ConsantsWithHalant: List<Pair<String, String>> =
    Consonants.map { (pat, dev) -> pat to "$dev\u094D" }

  // 26-bucket first-character indices for O(1) prefix dispatch instead of 56-item linear scans
  private val independentVowelsByChar: Array<List<Pair<String, String>>> =
    Array(26) { idx ->
      val ch = ('a'.code + idx).toChar()
      IndependentVowels.filter { it.first[0] == ch }
    }

  private val dependentVowelsByChar: Array<List<Pair<String, String>>> =
    Array(26) { idx ->
      val ch = ('a'.code + idx).toChar()
      DependentVowels.filter { it.first[0] == ch }
    }

  private val consonantsByChar: Array<List<Pair<String, String>>> =
    Array(26) { idx ->
      val ch = ('a'.code + idx).toChar()
      ConsantsWithHalant.filter { it.first[0] == ch }
    }

  private val asciiConsonantLookup =
    BooleanArray(128).apply {
      for (ch in "bcdfghjklmnpqrstvwxyz") {
        this[ch.code] = true
      }
    }

  /**
   * Transliterates Roman Hinglish input into Devanagari Hindi text.
   */
  fun transliterate(rawInput: String): String {
    if (rawInput.isEmpty()) return ""

    // Fast path when rawInput is a single alphabetic word (common case for live composing buffer)
    var isSingleWord = true
    var hasUpperCase = false
    for (idx in 0 until rawInput.length) {
      val c = rawInput[idx]
      if (c in 'A'..'Z') {
        hasUpperCase = true
      } else if (c !in 'a'..'z') {
        isSingleWord = false
        break
      }
    }

    if (isSingleWord) {
      val lower = if (hasUpperCase) rawInput.lowercase() else rawInput
      return transliterateLowerWord(lower)
    }

    // Multi-word or punctuation-containing string: tokenize by non-alphabetic boundaries
    val result = StringBuilder(rawInput.length)
    var i = 0
    while (i < rawInput.length) {
      val c = rawInput[i]
      if (c in 'a'..'z' || c in 'A'..'Z') {
        val wordStart = i
        var wordHasUpper = false
        while (i < rawInput.length) {
          val wc = rawInput[i]
          if (wc in 'A'..'Z') {
            wordHasUpper = true
            i++
          } else if (wc in 'a'..'z') {
            i++
          } else {
            break
          }
        }
        val word = rawInput.substring(wordStart, i)
        val lowerWord = if (wordHasUpper) word.lowercase() else word
        result.append(transliterateLowerWord(lowerWord))
      } else {
        result.append(c)
        i++
      }
    }
    return result.toString()
  }

  private fun transliterateLowerWord(lower: String): String {
    CommonWords[lower]?.let { return it }
    wordTransliterationCache[lower]?.let { return it }

    val out = StringBuilder(lower.length)
    var idx = 0
    var lastWasConsonant = false

    while (idx < lower.length) {
      val ch = lower[idx]

      // Check for Anusvara / Chandrabindu patterns at end or before consonant
      if (ch == 'a' && lower.startsWith("aan", idx) && (idx + 3 == lower.length || isConsonantAt(lower, idx + 3))) {
        if (lastWasConsonant) {
          if (out.isNotEmpty() && out[out.length - 1] == '\u094D') out.setLength(out.length - 1)
          out.append("ाँ")
        } else {
          out.append("आँ")
        }
        idx += 3
        lastWasConsonant = false
        continue
      }

      if (ch == 'o' && lower.startsWith("on", idx) && idx + 2 == lower.length) {
        if (lastWasConsonant) {
          if (out.isNotEmpty() && out[out.length - 1] == '\u094D') out.setLength(out.length - 1)
          out.append("ों")
        } else {
          out.append("ओं")
        }
        idx += 2
        lastWasConsonant = false
        continue
      }

      if (ch == 'e' && lower.startsWith("en", idx) && idx + 2 == lower.length) {
        if (lastWasConsonant) {
          if (out.isNotEmpty() && out[out.length - 1] == '\u094D') out.setLength(out.length - 1)
          out.append("ें")
        } else {
          out.append("एं")
        }
        idx += 2
        lastWasConsonant = false
        continue
      }

      val bucketIdx = ch.code - 'a'.code
      if (bucketIdx in 0..25) {
        // Check vowels starting with ch
        var matchedVowel = false
        val vowelBucket =
          if (lastWasConsonant) dependentVowelsByChar[bucketIdx] else independentVowelsByChar[bucketIdx]
        for (vIdx in 0 until vowelBucket.size) {
          val pair = vowelBucket[vIdx]
          val pattern = pair.first
          if (lower.startsWith(pattern, idx)) {
            if (lastWasConsonant && out.isNotEmpty() && out[out.length - 1] == '\u094D') {
              out.setLength(out.length - 1)
            }
            out.append(pair.second)
            idx += pattern.length
            matchedVowel = true
            lastWasConsonant = false
            break
          }
        }
        if (matchedVowel) continue

        // Check consonants starting with ch
        var matchedConsonant = false
        val consonantBucket = consonantsByChar[bucketIdx]
        for (cIdx in 0 until consonantBucket.size) {
          val pair = consonantBucket[cIdx]
          val pattern = pair.first
          if (lower.startsWith(pattern, idx)) {
            out.append(pair.second)
            idx += pattern.length
            matchedConsonant = true
            lastWasConsonant = true
            break
          }
        }
        if (matchedConsonant) continue
      }

      // Fallback: character as-is
      out.append(ch)
      idx++
      lastWasConsonant = false
    }

    // Clean up trailing virama (halant '्') at word boundary for natural Hindi pronunciation
    if (out.isNotEmpty() && out[out.length - 1] == '्') {
      out.setLength(out.length - 1)
    }
    val cleaned = out.toString()

    if (lower.length <= 24) {
      if (wordTransliterationCache.size >= MAX_WORD_CACHE_SIZE) {
        wordTransliterationCache.clear()
      }
      wordTransliterationCache[lower] = cleaned
    }
    return cleaned
  }

  private fun isConsonantAt(str: String, index: Int): Boolean {
    if (index >= str.length) return false
    val code = str[index].code
    return code < 128 && asciiConsonantLookup[code]
  }
}
