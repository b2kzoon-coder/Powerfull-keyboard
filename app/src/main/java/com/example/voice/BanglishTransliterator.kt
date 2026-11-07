package com.example.voice

/**
 * Lightweight, deterministic Bengali-to-Roman (Banglish) transliteration engine.
 *
 * Converts Bengali Unicode text (স্বরবর্ণ, ব্যঞ্জনবর্ণ, কার, ফলা, রেফ, যুক্তবর্ণ, সংখ্যা)
 * into clean, natural Romanized Bengali (Banglish) while preserving word boundaries,
 * capitalization at the start of sentences/utterances, and punctuation.
 *
 * IMPORTANT: This performs phonetic transliteration (Bengali -> Romanized Bengali),
 * NOT semantic translation into English.
 */
object BanglishTransliterator {

  // Dictionary of high-frequency Bengali words & inflected forms for natural, consistent Banglish spelling
  private val wordLexicon: Map<String, String> =
    mapOf(
      "আমি" to "ami",
      "আমার" to "amar",
      "আমাকে" to "amake",
      "আমাদের" to "amader",
      "তুমি" to "tumi",
      "তোমার" to "tomar",
      "তোমাকে" to "tomake",
      "তোমাদের" to "tomader",
      "আপনি" to "apni",
      "আপনার" to "apnar",
      "আপনাকে" to "apnake",
      "আপনাদের" to "apnader",
      "সে" to "she",
      "তার" to "tar",
      "তাকে" to "take",
      "তারা" to "tara",
      "তাদের" to "tader",
      "কেমন" to "kemon",
      "আছো" to "acho",
      "আছেন" to "achen",
      "আছি" to "achi",
      "আছে" to "ache",
      "ভালো" to "valo",
      "ভাল" to "valo",
      "খারাপ" to "kharap",
      "নাম" to "nam",
      "কী" to "ki",
      "কি" to "ki",
      "কোথায়" to "kothay",
      "কোথায়" to "kothay",
      "যাচ্ছো" to "jaccho",
      "যাচ্ছেন" to "jacchen",
      "যাচ্ছি" to "jacchi",
      "যাচ্ছে" to "jacche",
      "যাবো" to "jabo",
      "যাবে" to "jabe",
      "যান" to "jan",
      "বাংলাদেশ" to "Bangladesh",
      "বাংলাদেশে" to "Bangladesh-e",
      "বাংলাদেশের" to "Bangladesh-er",
      "বাংলা" to "Bangla",
      "বাংলায়" to "Banglay",
      "বাংলায়" to "Banglay",
      "থাকি" to "thaki",
      "থাকো" to "thako",
      "থাকেন" to "thaken",
      "থাকে" to "thake",
      "করি" to "kori",
      "করো" to "koro",
      "করেন" to "koren",
      "করে" to "kore",
      "করছি" to "korchi",
      "করছো" to "korcho",
      "করছেন" to "korchen",
      "করছে" to "korche",
      "হচ্ছে" to "hocche",
      "হয়েছে" to "hoyeche",
      "হয়েছে" to "hoyeche",
      "হবে" to "hobe",
      "হয়" to "hoy",
      "হয়" to "hoy",
      "নয়" to "noy",
      "নয়" to "noy",
      "না" to "na",
      "হ্যাঁ" to "hya",
      "জি" to "ji",
      "ধন্যবাদ" to "dhonnobad",
      "স্বাগতম" to "shagotom",
      "ভালোবাসি" to "valobashi",
      "ভালোবাসা" to "valobasha",
      "বন্ধু" to "bondhu",
      "বাড়ি" to "bari",
      "বাড়ি" to "bari",
      "বাসা" to "basha",
      "বাসায়" to "bashay",
      "বাসায়" to "bashay",
      "খাবার" to "khabar",
      "খেয়েছো" to "kheyecho",
      "খেয়েছো" to "kheyecho",
      "খেয়েছি" to "kheyechi",
      "খেয়েছি" to "kheyechi",
      "খাবো" to "khabo",
      "পানি" to "pani",
      "ভাত" to "bhat",
      "কাজ" to "kaj",
      "কথা" to "kotha",
      "বলি" to "boli",
      "বলো" to "bolo",
      "বলেন" to "bolen",
      "বলে" to "bole",
      "বলছি" to "bolchi",
      "শুনো" to "shuno",
      "শোনো" to "shono",
      "দেখো" to "dekho",
      "দেখি" to "dekhi",
      "আসো" to "asho",
      "এসো" to "esho",
      "আসছি" to "ashchi",
      "এখন" to "ekhon",
      "আজ" to "aj",
      "আজকে" to "ajke",
      "কাল" to "kal",
      "কালকে" to "kalke",
      "আগামীকাল" to "agamikal",
      "গতকাল" to "gotokal",
      "সকাল" to "shokal",
      "সকালে" to "shokale",
      "শুভ" to "shuvo",
      "দুপুর" to "dupur",
      "বিকাল" to "bikal",
      "সন্ধ্যা" to "shondha",
      "রাত" to "rat",
      "রাতে" to "rate",
      "সময়" to "shomoy",
      "সময়" to "shomoy",
      "টাকা" to "taka",
      "মানুষ" to "manush",
      "দেশ" to "desh",
      "শহর" to "shohor",
      "ঢাকা" to "Dhaka",
      "ঢাকায়" to "Dhakay",
      "ঢাকায়" to "Dhakay",
      "কলকাতা" to "Kolkata",
      "অনেক" to "onek",
      "খুব" to "khub",
      "বেশি" to "beshi",
      "কম" to "kom",
      "সব" to "shob",
      "সবাই" to "shobai",
      "কিছু" to "kichu",
      "কেন" to "keno",
      "কখন" to "kokhon",
      "কিভাবে" to "kivabe",
      "কীভাবে" to "kivabe",
      "কে" to "ke",
      "কার" to "kar",
      "কাকে" to "kake",
      "কোন" to "kon",
      "কোনটা" to "konta",
      "এই" to "ei",
      "ওই" to "oi",
      "সেই" to "shei",
      "এটা" to "eta",
      "ওটা" to "ota",
      "সেটা" to "sheta",
      "এখানে" to "ekhane",
      "সেখানে" to "shekhane",
      "ওখানে" to "okhane",
      "সাথে" to "shathe",
      "জন্য" to "jonno",
      "থেকে" to "theke",
      "পরে" to "pore",
      "আগে" to "age",
      "ভিতরে" to "vitore",
      "বাইরে" to "baire",
      "উপরে" to "upore",
      "নিচে" to "niche",
      "কাছে" to "kache",
      "দূরে" to "dure",
      "মন" to "mon",
      "মনে" to "mone",
      "জীবন" to "jibon",
      "দিন" to "din",
      "বছর" to "bochor",
      "মাস" to "mash",
      "সপ্তাহ" to "shoptaho",
      "স্কুল" to "school",
      "কলেজ" to "college",
      "অফিস" to "office",
      "ফোন" to "phone",
      "কল" to "call",
      "মেসেজ" to "message",
      "ছবি" to "chobi",
      "গান" to "gan",
      "বই" to "boi",
      "পড়া" to "pora",
      "পড়া" to "pora",
      "লেখা" to "lekha",
      "সত্য" to "shotto",
      "মিথ্যা" to "mittha",
      "সুন্দর" to "shundor",
      "নতুন" to "notun",
      "পুরাতন" to "puraton",
      "বড়" to "boro",
      "বড়" to "boro",
      "ছোট" to "choto",
      "এক" to "ek",
      "দুই" to "dui",
      "তিন" to "tin",
      "চার" to "char",
      "পাঁচ" to "pach",
      "ছয়" to "choy",
      "ছয়" to "choy",
      "সাত" to "shat",
      "আট" to "at",
      "নয়" to "noy",
      "দশ" to "dosh",
      "বাবা" to "baba",
      "মা" to "ma",
      "ভাই" to "bhai",
      "বোন" to "bon",
      "দাদা" to "dada",
      "দিদি" to "didi",
    )

  private const val MAX_WORD_CACHE_SIZE = 256
  private val wordCache = java.util.concurrent.ConcurrentHashMap<String, String>(128)
  @Volatile private var lastRawInput: String = ""
  @Volatile private var lastRomanOutput: String = ""

  /**
   * Transliterates a Bengali string (single word, phrase, or multiline text) into Romanized Bengali (Banglish).
   * If the input contains no Bengali characters, returns the input unchanged.
   */
  fun transliterate(input: String): String {
    if (input.isEmpty()) return input
    if (input == lastRawInput) return lastRomanOutput
    if (!containsBengali(input)) return input

    val normalized = normalizePrecomposedConsonants(input)
    val sb = StringBuilder(normalized.length + 8)
    var i = 0
    val len = normalized.length
    var capitalizeNextWord = true

    while (i < len) {
      val ch = normalized[i]
      if (isBengaliWordChar(ch)) {
        val start = i
        while (i < len && isBengaliWordChar(normalized[i])) {
          i++
        }
        val bengaliWord = normalized.substring(start, i)
        val romanWord = transliterateWord(bengaliWord)
        if (capitalizeNextWord && romanWord.isNotEmpty() && romanWord[0].isLowerCase()) {
          sb.append(romanWord[0].uppercaseChar())
          sb.append(romanWord, 1, romanWord.length)
        } else {
          sb.append(romanWord)
        }
        capitalizeNextWord = false
      } else if (ch in '০'..'৯') {
        sb.append(('0'.code + (ch.code - '০'.code)).toChar())
        capitalizeNextWord = false
        i++
      } else if (ch == '।' || ch == '॥') {
        sb.append('.')
        capitalizeNextWord = true
        i++
      } else {
        sb.append(ch)
        if (ch == '.' || ch == '!' || ch == '?' || ch == '\n') {
          capitalizeNextWord = true
        }
        i++
      }
    }

    val output = sb.toString()
    if (input.length <= 256) {
      lastRawInput = input
      lastRomanOutput = output
    }
    return output
  }

  fun containsBengali(text: String): Boolean {
    for (ch in text) {
      if (ch.code in 0x0980..0x09FF) return true
    }
    return false
  }

  private fun isBengaliWordChar(ch: Char): Boolean {
    val code = ch.code
    return code in 0x0980..0x09FF && ch !in '০'..'৯' && ch != '।' && ch != '॥'
  }

  private fun normalizePrecomposedConsonants(text: String): String {
    if ('\u09BC' !in text) return text
    val sb = StringBuilder(text.length)
    var i = 0
    while (i < text.length) {
      val c = text[i]
      if (i + 1 < text.length && text[i + 1] == '\u09BC') {
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
      }
      sb.append(c)
      i++
    }
    return sb.toString()
  }

  private fun transliterateWord(word: String): String {
    if (word.isEmpty()) return ""

    // 1. Direct lexicon or bounded cache lookup for exact match
    wordLexicon[word]?.let { return it }
    wordCache[word]?.let { return it }

    // 2. Rule-based phonetic transliteration for arbitrary Bengali words
    val result = StringBuilder(word.length + 4)
    val n = word.length
    var i = 0

    while (i < n) {
      val c = word[i]

      // Independent vowels (স্বরবর্ণ)
      val indVowel = independentVowelToRoman(c, isWordStart = (i == 0))
      if (indVowel != null) {
        result.append(indVowel)
        i++
        continue
      }

      // Modifiers (অনুস্বার, বিসর্গ, চন্দ্রবিন্দু, খণ্ড-ত)
      when (c) {
        'ং' -> {
          result.append("ng")
          i++
          continue
        }
        'ঃ' -> {
          result.append("h")
          i++
          continue
        }
        'ঁ' -> {
          // Nasalization mark — in common Banglish typing it's silent or 'n' before stops
          i++
          continue
        }
        'ৎ' -> {
          result.append("t")
          i++
          continue
        }
        '\u09BC', '\u09CD' -> {
          i++
          continue
        }
      }

      // Consonants & Conjuncts (ব্যঞ্জনবর্ণ ও যুক্তবর্ণ)
      if (isConsonant(c)) {
        // Check for 3-consonant or 2-consonant conjuncts using Hasanta (\u09CD)
        if (i + 2 < n && word[i + 1] == '\u09CD' && isConsonant(word[i + 2])) {
          val c1 = c
          val c2 = word[i + 2]

          // Check 3-consonant conjunct: c1 + ্ + c2 + ্ + c3
          if (i + 4 < n && word[i + 3] == '\u09CD' && isConsonant(word[i + 4])) {
            val c3 = word[i + 4]
            val clusterRoman = mapThreeConsonantCluster(c1, c2, c3, isWordStart = (i == 0))
            result.append(clusterRoman)
            i += 5
            i = appendVowelAfterConsonant(word, i, n, result, isCluster = true)
            continue
          }

          // 2-consonant conjunct: c1 + ্ + c2
          val conjunctRoman = mapTwoConsonantCluster(c1, c2, isWordStart = (i == 0))
          result.append(conjunctRoman)
          i += 3
          // Special case: ক + ্ + ষ (ক্ষ) or জ + ্ + ঞ (জ্ঞ)
          i = appendVowelAfterConsonant(word, i, n, result, isCluster = true)
          continue
        }

        // Single consonant
        val consRoman = consonantToRoman(c, isWordStart = (i == 0), isWordEnd = (i == n - 1))
        result.append(consRoman)
        i++
        i = appendVowelAfterConsonant(word, i, n, result, isCluster = false, prevConsonant = c)
        continue
      }

      // Standalone vowel sign (কার) if any
      val kar = karToRoman(c)
      if (kar != null) {
        result.append(kar)
        i++
        continue
      }

      // Fallback
      result.append(c)
      i++
    }

    val roman = result.toString()
    if (word.length <= 24) {
      if (wordCache.size >= MAX_WORD_CACHE_SIZE) {
        wordCache.clear()
      }
      wordCache[word] = roman
    }
    return roman
  }

  private fun appendVowelAfterConsonant(
    word: String,
    index: Int,
    length: Int,
    out: StringBuilder,
    isCluster: Boolean,
    prevConsonant: Char = ' ',
  ): Int {
    if (index < length) {
      val nextChar = word[index]
      val kar = karToRoman(nextChar)
      if (kar != null) {
        out.append(kar)
        return index + 1
      }
      if (nextChar == '\u09CD') {
        // Explicit hasanta at end of word or syllable: suppress inherent vowel
        return index + 1
      }
    }

    // No explicit kar and no hasanta -> decide inherent vowel ('o' or 'a' or silent at word end)
    val isAtWordEnd =
      index >= length ||
        (index == length - 1 && (word[index] == 'ং' || word[index] == 'ঃ' || word[index] == 'ঁ'))

    if (isAtWordEnd) {
      // In Bengali, word-final single consonants usually drop the inherent vowel (e.g., নাম -> nam, কাজ -> kaj),
      // except after conjunct clusters (e.g., কর্ম -> kormo, সত্য -> shotto) or 'হ' / 'য়'.
      if (isCluster || prevConsonant == 'হ') {
        out.append('o')
      }
    } else {
      // Internal syllable inherent vowel
      val inherent = chooseInherentVowel(word, index, prevConsonant)
      out.append(inherent)
    }
    return index
  }

  private fun chooseInherentVowel(word: String, nextIndex: Int, prevConsonant: Char): Char {
    if (prevConsonant == 'য়') return 'e'
    // If followed by য় + া (e.g., যাওয়া -> jaoya / হওয়া -> hoya), use 'o'
    // In common Bengali words like ভালো (valo), নাম (nam), কাজ (kaj), explicit আ-কার is used when 'a' is needed.
    // For un-marked inherent vowel in initial/medial syllables, 'o' is the natural Bengali pronunciation (অ = o),
    // except before certain 'ন'/'ম'/'ল' patterns or when followed by 'ই'/'উ' harmonic vowels.
    if (nextIndex < word.length && (word[nextIndex] == 'ং')) {
      return 'a' // যেমন: বাংলা -> bangla, সংগ্রাম -> shongram
    }
    return 'o'
  }

  private fun independentVowelToRoman(c: Char, isWordStart: Boolean): String? {
    return when (c) {
      'অ' -> "o"
      'আ' -> "a"
      'ই', 'ঈ' -> "i"
      'উ', 'ঊ' -> "u"
      'ঋ' -> "ri"
      'এ' -> "e"
      'ঐ' -> "oi"
      'ও' -> "o"
      'ঔ' -> "ou"
      else -> null
    }
  }

  private fun karToRoman(c: Char): String? {
    return when (c) {
      'া' -> "a"
      'ি', 'ী' -> "i"
      'ু', 'ূ' -> "u"
      'ৃ' -> "ri"
      'ে' -> "e"
      'ৈ' -> "oi"
      'ো' -> "o"
      'ৌ' -> "ou"
      else -> null
    }
  }

  private fun isConsonant(c: Char): Boolean {
    return when (c) {
      in 'ক'..'ন',
      in 'প'..'র',
      'ল',
      in 'শ'..'হ',
      'ড়',
      'ঢ়',
      'য়' -> true
      else -> false
    }
  }

  private fun consonantToRoman(c: Char, isWordStart: Boolean, isWordEnd: Boolean): String {
    return when (c) {
      'ক' -> "k"
      'খ' -> "kh"
      'গ' -> "g"
      'ঘ' -> "gh"
      'ঙ' -> "ng"
      'চ' -> "ch"
      'ছ' -> "ch"
      'জ' -> "j"
      'ঝ' -> "jh"
      'ঞ' -> "n"
      'ট' -> "t"
      'ঠ' -> "th"
      'ড' -> "d"
      'ঢ' -> "dh"
      'ণ' -> "n"
      'ত' -> "t"
      'থ' -> "th"
      'দ' -> "d"
      'ধ' -> "dh"
      'ন' -> "n"
      'প' -> "p"
      'ফ' -> "f"
      'ব' -> "b"
      'ভ' -> if (isWordStart) "v" else "bh"
      'ম' -> "m"
      'য' -> "j"
      'র' -> "r"
      'ল' -> "l"
      'শ', 'ষ', 'স' -> "sh"
      'হ' -> "h"
      'ড়', 'ঢ়' -> "r"
      'য়' -> if (isWordEnd) "y" else "y"
      else -> ""
    }
  }

  private fun mapTwoConsonantCluster(c1: Char, c2: Char, isWordStart: Boolean): String {
    // 1. Reph (র + ্ + C): র ফলা রেফ
    if (c1 == 'র') {
      val second = consonantToRoman(c2, isWordStart = false, isWordEnd = false)
      return "r$second"
    }

    // 2. Special ligatures (যুক্তবর্ণ)
    if (c1 == 'ক' && c2 == 'ষ') return if (isWordStart) "kh" else "kkh" // ক্ষ
    if (c1 == 'জ' && c2 == 'ঞ') return if (isWordStart) "g" else "gg"   // জ্ঞ
    if (c1 == 'ঙ' && c2 == 'গ') return "ng"                             // ঙ্গ
    if (c1 == 'ঙ' && c2 == 'ক') return "nk"                             // ঙ্ক
    if (c1 == 'ঞ' && c2 == 'চ') return "nch"                            // ঞ্চ
    if (c1 == 'ঞ' && c2 == 'ছ') return "nch"                            // ঞ্ছ
    if (c1 == 'ঞ' && c2 == 'জ') return "nj"                             // ঞ্জ

    // 3. Ya-Phala (্য): C + ্ + য
    if (c2 == 'য') {
      if (c1 == 'হ') return "hy"
      if (isWordStart) {
        val first = consonantToRoman(c1, isWordStart = true, isWordEnd = false)
        return first // e.g., ব্য -> b (ব্যবহার -> byabohar / babohar)
      }
      // Medial/final Ya-Phala doubles the consonant in Bengali pronunciation (e.g., সত্য -> shotto, ধন্য -> dhonno, যাচ্ছো -> jaccho)
      return doubleConsonantRoman(c1)
    }

    // 4. Ba-Phala (্ব): C + ্ + ব
    if (c2 == 'ব') {
      if (c1 == 'স' || c1 == 'শ') return if (isWordStart) "sh" else "ssh" // স্বাধীন -> shadhin, বিশ্বাস -> bisshash
      if (c1 == 'ধ') return "dh" // ধ্বনি -> dhoni
      if (c1 == 'দ') return "d"
      return if (isWordStart) consonantToRoman(c1, true, false) else doubleConsonantRoman(c1)
    }

    // 5. Ma-Phala (্ম): C + ্ + ম
    if (c2 == 'ম') {
      if (c1 == 'ক' || c1 == 'গ' || c1 == 'জ' || c1 == 'ত' || c1 == 'দ' || c1 == 'ন' || c1 == 'ম' || c1 == 'ল' || c1 == 'শ' || c1 == 'স') {
        if (c1 == 'জ' || c1 == 'ন' || c1 == 'ম' || c1 == 'ল') {
          return "${consonantToRoman(c1, isWordStart, false)}m" // জন্ম -> jonmo, গুল্ম -> gulmo
        }
        return if (isWordStart) consonantToRoman(c1, true, false) else doubleConsonantRoman(c1) // পদ্ম -> poddo, গ্রীষ্ম -> grissho
      }
    }

    // 6. Ra-Phala (্র): C + ্ + র
    if (c2 == 'র') {
      val first = consonantToRoman(c1, isWordStart = isWordStart, isWordEnd = false)
      return "${first}r"
    }

    // 7. La-Phala (্ল): C + ্ + ল
    if (c2 == 'ল') {
      val first = consonantToRoman(c1, isWordStart = isWordStart, isWordEnd = false)
      return "${first}l"
    }

    // 8. Sibilant clusters (স্ট, স্ত, স্থ, স্প, স্ফ, স্ক, স্কুল)
    if ((c1 == 'স' || c1 == 'শ' || c1 == 'ষ') && (c2 == 'ট' || c2 == 'ত' || c2 == 'থ' || c2 == 'প' || c2 == 'ফ' || c2 == 'ক' || c2 == 'খ' || c2 == 'ন')) {
      val second = consonantToRoman(c2, isWordStart = false, isWordEnd = false)
      return "s$second"
    }

    // Default two-consonant combination
    val r1 = consonantToRoman(c1, isWordStart = isWordStart, isWordEnd = false)
    val r2 = consonantToRoman(c2, isWordStart = false, isWordEnd = false)
    return if (r1 == r2) doubleConsonantRoman(c1) else "$r1$r2"
  }

  private fun mapThreeConsonantCluster(c1: Char, c2: Char, c3: Char, isWordStart: Boolean): String {
    if (c1 == 'র') {
      // Reph + 2-consonant cluster
      return "r" + mapTwoConsonantCluster(c2, c3, isWordStart = false)
    }
    if (c3 == 'র') {
      // 2-consonant cluster + Ra-phala (e.g., ন্ত্র -> ntr, ক্ত্র -> ktr, স্ট্র -> str)
      return mapTwoConsonantCluster(c1, c2, isWordStart) + "r"
    }
    if (c3 == 'য') {
      // 2-consonant cluster + Ya-phala (e.g., ন্ত্য -> nto, চ্ছ্য -> ccho)
      return mapTwoConsonantCluster(c1, c2, isWordStart)
    }
    val r1 = consonantToRoman(c1, isWordStart, false)
    val r23 = mapTwoConsonantCluster(c2, c3, false)
    return "$r1$r23"
  }

  private fun doubleConsonantRoman(c: Char): String {
    return when (c) {
      'ক' -> "kk"
      'খ' -> "kkh"
      'গ' -> "gg"
      'ঘ' -> "ggh"
      'চ' -> "cch"
      'ছ' -> "cch"
      'জ' -> "jj"
      'ঝ' -> "jjh"
      'ট' -> "tt"
      'ঠ' -> "tth"
      'ড' -> "dd"
      'ঢ' -> "ddh"
      'ণ', 'ন' -> "nn"
      'ত' -> "tt"
      'থ' -> "tth"
      'দ' -> "dd"
      'ধ' -> "ddh"
      'প' -> "pp"
      'ফ' -> "ff"
      'ব' -> "bb"
      'ভ' -> "v"
      'ম' -> "mm"
      'য' -> "jj"
      'র' -> "rr"
      'ল' -> "ll"
      'শ', 'ষ', 'স' -> "ssh"
      else -> consonantToRoman(c, false, false)
    }
  }
}
