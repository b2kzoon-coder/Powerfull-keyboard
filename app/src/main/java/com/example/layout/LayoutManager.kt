package com.example.layout

import com.example.language.LanguageInputMethodKind

/**
 * Pre-allocated static keyboard layouts to eliminate object allocations during typing and mode
 * switching on low and mid-range devices.
 *
 * Implements both:
 * 1. English / Avro Phonetic QWERTY layout
 * 2. Exact Bengali Probhat layout matching the reference design
 */
object LayoutManager {

  val NumberRow: List<KeySpec> =
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { digit ->
      KeySpec(
        id = "num_row_$digit",
        primaryLabel = digit,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(digit, digit),
      )
    }

  val BengaliNumberRow: List<KeySpec> =
    listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০").map { digit ->
      KeySpec(
        id = "bn_num_row_$digit",
        primaryLabel = digit,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(digit, digit),
      )
    }

  // --- ENGLISH / AVRO QWERTY ROWS ---

  private val QwertyRow1: List<KeySpec> =
    listOf(
        "q" to "1",
        "w" to "2",
        "e" to "3",
        "r" to "4",
        "t" to "5",
        "y" to "6",
        "u" to "7",
        "i" to "8",
        "o" to "9",
        "p" to "0",
      )
      .map { (ch, hint) ->
        KeySpec(
          id = "alpha_$ch",
          primaryLabel = ch,
          shiftedLabel = ch.uppercase(),
          secondaryHint = hint,
          weight = 1f,
          role = KeyRole.CHARACTER,
          action = KeyAction.CommitChar(ch, ch.uppercase()),
        )
      }

  private val QwertyRow2: List<KeySpec> =
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map { ch ->
      KeySpec(
        id = "alpha_$ch",
        primaryLabel = ch,
        shiftedLabel = ch.uppercase(),
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(ch, ch.uppercase()),
      )
    }

  private val QwertyRow3: List<KeySpec> =
    buildList {
      add(
        KeySpec(
          id = "key_shift",
          primaryLabel = "⇧",
          shiftedLabel = "⬆",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Shift,
        )
      )
      listOf("z", "x", "c", "v", "b", "n", "m").forEach { ch ->
        val editAction =
          when (ch) {
            "x" -> KeyAction.Cut
            "c" -> KeyAction.Copy
            "v" -> KeyAction.Paste
            else -> null
          }
        add(
          KeySpec(
            id = "alpha_$ch",
            primaryLabel = ch,
            shiftedLabel = ch.uppercase(),
            longPressEditAction = editAction,
            weight = 1f,
            role = KeyRole.CHARACTER,
            action = KeyAction.CommitChar(ch, ch.uppercase()),
          )
        )
      }
      add(
        KeySpec(
          id = "key_backspace",
          primaryLabel = "⌫",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Backspace,
          repeatable = true,
        )
      )
    }

  private val AlphabetBottomRow: List<KeySpec> =
    listOf(
      KeySpec(
        id = "key_mode_symbols",
        primaryLabel = "?123",
        weight = 1.35f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.ModeSymbolsPrimary,
      ),
      KeySpec(
        id = "key_emoji",
        primaryLabel = "😊",
        weight = 1.0f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.ModeEmoji,
      ),
      KeySpec(
        id = "key_comma",
        primaryLabel = ",",
        weight = 1.0f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar(",", ","),
      ),
      KeySpec(
        id = "key_space",
        primaryLabel = "English",
        weight = 3.7f,
        role = KeyRole.SPACEBAR,
        action = KeyAction.Space,
      ),
      KeySpec(
        id = "key_period",
        primaryLabel = ".",
        weight = 1.35f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar(".", "."),
      ),
      KeySpec(
        id = "key_enter",
        primaryLabel = "↵",
        weight = 1.50f,
        role = KeyRole.ACCENT,
        action = KeyAction.Enter,
      ),
    )

  // --- PROBHAT BENGALI ROWS (EXACT REFERENCE SPECIFICATION) ---

  // Row 1: 12 keys (দ, ূ, ী, র, ট, এ, ু, ি, ও, প, ে, ো)
  private val ProbhatRow1: List<KeySpec> =
    listOf(
      "দ" to "ধ",
      "ূ" to "ঊ",
      "ী" to "ঈ",
      "র" to "ড়",
      "ট" to "ঠ",
      "এ" to "ঐ",
      "ু" to "উ",
      "ি" to "ই",
      "ও" to "ঔ",
      "প" to "ফ",
      "ে" to "ৈ",
      "ো" to "ৌ",
    ).mapIndexed { idx, (unshifted, shifted) ->
      KeySpec(
        id = "probhat_r1_$idx",
        primaryLabel = unshifted,
        shiftedLabel = shifted,
        secondaryHint = shifted,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(unshifted, shifted),
      )
    }

  // Row 2: 9 keys (া, স, ড, ত, গ, হ, জ, ক, ল)
  private val ProbhatRow2: List<KeySpec> =
    listOf(
      "া" to "অ",
      "স" to "ষ",
      "ড" to "ঢ",
      "ত" to "থ",
      "গ" to "ঘ",
      "হ" to "ঃ",
      "জ" to "ঝ",
      "ক" to "খ",
      "ল" to "ং",
    ).mapIndexed { idx, (unshifted, shifted) ->
      KeySpec(
        id = "probhat_r2_$idx",
        primaryLabel = unshifted,
        shiftedLabel = shifted,
        secondaryHint = shifted,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(unshifted, shifted),
      )
    }

  // Row 3: Shift + 9 keys (য়, শ, চ, আ, ব, ন, ম, ,, ্) + Backspace
  private val ProbhatRow3: List<KeySpec> =
    buildList {
      add(
        KeySpec(
          id = "key_shift_probhat",
          primaryLabel = "⇧",
          shiftedLabel = "⬆",
          weight = 1.35f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Shift,
        )
      )
      val keys =
        listOf(
          "য়" to "য",
          "শ" to "ঢ়",
          "চ" to "ছ",
          "আ" to "ঋ",
          "ব" to "ভ",
          "ন" to "ণ",
          "ম" to "ঙ",
          "," to "<",
          "্" to "ঁ", // hasanta and candrabindu
        )
      keys.forEachIndexed { idx, (unshifted, shifted) ->
        val hint = if (idx == 0) "্য" else shifted
        add(
          KeySpec(
            id = "probhat_r3_$idx",
            primaryLabel = unshifted,
            shiftedLabel = shifted,
            secondaryHint = hint,
            weight = 1f,
            role = KeyRole.CHARACTER,
            action = KeyAction.CommitChar(unshifted, shifted),
          )
        )
      }
      add(
        KeySpec(
          id = "key_backspace_probhat",
          primaryLabel = "⌫",
          weight = 1.35f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Backspace,
          repeatable = true,
        )
      )
    }

  // Probhat Bottom Row matching screenshot
  private val ProbhatBottomRow: List<KeySpec> =
    listOf(
      KeySpec(
        id = "key_mode_symbols_pb",
        primaryLabel = "?123",
        weight = 1.35f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.ModeSymbolsPrimary,
      ),
      KeySpec(
        id = "key_smiley_pb",
        primaryLabel = "😊",
        weight = 1.0f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.ModeEmoji,
      ),
      KeySpec(
        id = "key_comma_pb",
        primaryLabel = ",",
        weight = 1.0f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar(",", ","),
      ),
      KeySpec(
        id = "key_space_pb",
        primaryLabel = "◄  প্রভাত  ►",
        weight = 3.7f,
        role = KeyRole.SPACEBAR,
        action = KeyAction.Space,
      ),
      KeySpec(
        id = "key_dari_pb",
        primaryLabel = "।",
        weight = 1.35f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar("।", "।"),
      ),
      KeySpec(
        id = "key_enter_pb",
        primaryLabel = "↵",
        weight = 1.50f,
        role = KeyRole.ACCENT,
        action = KeyAction.Enter,
      ),
    )

  // --- SYMBOLS ROWS ---

  private val SymbolsPrimaryRow1: List<KeySpec> =
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map { sym ->
      KeySpec(
        id = "sym1_$sym",
        primaryLabel = sym,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(sym, sym),
      )
    }

  val BengaliSymbolsPrimaryRow1: List<KeySpec> =
    listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০").map { sym ->
      KeySpec(
        id = "bn_sym1_$sym",
        primaryLabel = sym,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(sym, sym),
      )
    }

  private val SymbolsPrimaryRow2: List<KeySpec> =
    listOf("@", "#", "$", "_", "&", "-", "+", "(", ")", "/").map { sym ->
      KeySpec(
        id = "sym1_$sym",
        primaryLabel = sym,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(sym, sym),
      )
    }

  private val SymbolsPrimaryRow3: List<KeySpec> =
    buildList {
      add(
        KeySpec(
          id = "key_mode_sym2",
          primaryLabel = "=\\<",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.ModeSymbolsSecondary,
        )
      )
      listOf("*", "\"", "'", ":", ";", "!", "?", "।").forEach { sym ->
        add(
          KeySpec(
            id = "sym1_$sym",
            primaryLabel = sym,
            weight = 1f,
            role = KeyRole.CHARACTER,
            action = KeyAction.CommitChar(sym, sym),
          )
        )
      }
      add(
        KeySpec(
          id = "key_backspace_sym1",
          primaryLabel = "⌫",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Backspace,
          repeatable = true,
        )
      )
    }

  private val SymbolsBottomRow: List<KeySpec> =
    listOf(
      KeySpec(
        id = "key_mode_alpha",
        primaryLabel = "ABC",
        weight = 1.4f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.ModeAlphabet,
      ),
      KeySpec(
        id = "key_comma_sym",
        primaryLabel = ",",
        weight = 1f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar(",", ","),
      ),
      KeySpec(
        id = "key_space_sym",
        primaryLabel = "Space",
        weight = 4.2f,
        role = KeyRole.SPACEBAR,
        action = KeyAction.Space,
      ),
      KeySpec(
        id = "key_period_sym",
        primaryLabel = ".",
        weight = 1f,
        role = KeyRole.FUNCTIONAL,
        action = KeyAction.CommitChar(".", "."),
      ),
      KeySpec(
        id = "key_enter_sym",
        primaryLabel = "↵",
        weight = 1.5f,
        role = KeyRole.ACCENT,
        action = KeyAction.Enter,
      ),
    )

  private val SymbolsSecondaryRow1: List<KeySpec> =
    listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "∆").map { sym ->
      KeySpec(
        id = "sym2_$sym",
        primaryLabel = sym,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(sym, sym),
      )
    }

  private val SymbolsSecondaryRow2: List<KeySpec> =
    listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\").map { sym ->
      KeySpec(
        id = "sym2_$sym",
        primaryLabel = sym,
        weight = 1f,
        role = KeyRole.CHARACTER,
        action = KeyAction.CommitChar(sym, sym),
      )
    }

  private val SymbolsSecondaryRow3: List<KeySpec> =
    buildList {
      add(
        KeySpec(
          id = "key_mode_sym1_back",
          primaryLabel = "?123",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.ModeSymbolsPrimary,
        )
      )
      listOf("%", "©", "®", "™", "✓", "[", "]").forEach { sym ->
        add(
          KeySpec(
            id = "sym2_$sym",
            primaryLabel = sym,
            weight = 1f,
            role = KeyRole.CHARACTER,
            action = KeyAction.CommitChar(sym, sym),
          )
        )
      }
      add(
        KeySpec(
          id = "key_backspace_sym2",
          primaryLabel = "⌫",
          weight = 1.45f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.Backspace,
          repeatable = true,
        )
      )
    }

  private val NumberPadRows: List<List<KeySpec>> =
    listOf(
      listOf("1", "2", "3", "-").map { ch ->
        KeySpec(
          id = "np_$ch",
          primaryLabel = ch,
          weight = 1f,
          role = if (ch == "-") KeyRole.FUNCTIONAL else KeyRole.CHARACTER,
          action = KeyAction.CommitChar(ch, ch),
        )
      },
      listOf("4", "5", "6", " ").map { ch ->
        if (ch == " ") {
          KeySpec(
            id = "np_space",
            primaryLabel = "␣",
            weight = 1f,
            role = KeyRole.FUNCTIONAL,
            action = KeyAction.Space,
          )
        } else {
          KeySpec(
            id = "np_$ch",
            primaryLabel = ch,
            weight = 1f,
            role = KeyRole.CHARACTER,
            action = KeyAction.CommitChar(ch, ch),
          )
        }
      },
      buildList {
        listOf("7", "8", "9").forEach { ch ->
          add(
            KeySpec(
              id = "np_$ch",
              primaryLabel = ch,
              weight = 1f,
              role = KeyRole.CHARACTER,
              action = KeyAction.CommitChar(ch, ch),
            )
          )
        }
        add(
          KeySpec(
            id = "np_backspace",
            primaryLabel = "⌫",
            weight = 1f,
            role = KeyRole.FUNCTIONAL,
            action = KeyAction.Backspace,
            repeatable = true,
          )
        )
      },
      listOf(
        KeySpec(
          id = "np_comma",
          primaryLabel = ",",
          weight = 1f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.CommitChar(",", ","),
        ),
        KeySpec(
          id = "np_0",
          primaryLabel = "0",
          weight = 1f,
          role = KeyRole.CHARACTER,
          action = KeyAction.CommitChar("0", "0"),
        ),
        KeySpec(
          id = "np_period",
          primaryLabel = ".",
          weight = 1f,
          role = KeyRole.FUNCTIONAL,
          action = KeyAction.CommitChar(".", "."),
        ),
        KeySpec(
          id = "np_enter",
          primaryLabel = "↵",
          weight = 1f,
          role = KeyRole.ACCENT,
          action = KeyAction.Enter,
        ),
      ),
    )

  // Layout specs
  private val AlphabetLayoutWithNumberRow =
    KeyboardLayoutSpec(
      mode = KeyboardMode.ALPHABET,
      rows = listOf(NumberRow, QwertyRow1, QwertyRow2, QwertyRow3, AlphabetBottomRow),
    )

  private val AlphabetLayoutCompact =
    KeyboardLayoutSpec(
      mode = KeyboardMode.ALPHABET,
      rows = listOf(QwertyRow1, QwertyRow2, QwertyRow3, AlphabetBottomRow),
    )

  private val ProbhatLayoutWithNumberRow =
    KeyboardLayoutSpec(
      mode = KeyboardMode.ALPHABET,
      rows = listOf(BengaliNumberRow, ProbhatRow1, ProbhatRow2, ProbhatRow3, ProbhatBottomRow),
    )

  private val ProbhatLayoutCompact =
    KeyboardLayoutSpec(
      mode = KeyboardMode.ALPHABET,
      rows = listOf(ProbhatRow1, ProbhatRow2, ProbhatRow3, ProbhatBottomRow),
    )

  private val SymbolsPrimaryLayout =
    KeyboardLayoutSpec(
      mode = KeyboardMode.SYMBOLS_PRIMARY,
      rows = listOf(SymbolsPrimaryRow1, SymbolsPrimaryRow2, SymbolsPrimaryRow3, SymbolsBottomRow),
    )

  val ProbhatSymbolsPrimaryLayout: KeyboardLayoutSpec =
    KeyboardLayoutSpec(
      mode = KeyboardMode.SYMBOLS_PRIMARY,
      rows = listOf(BengaliSymbolsPrimaryRow1, SymbolsPrimaryRow2, SymbolsPrimaryRow3, SymbolsBottomRow),
    )

  private val SymbolsSecondaryLayout =
    KeyboardLayoutSpec(
      mode = KeyboardMode.SYMBOLS_SECONDARY,
      rows = listOf(SymbolsSecondaryRow1, SymbolsSecondaryRow2, SymbolsSecondaryRow3, SymbolsBottomRow),
    )

  private val NumberPadLayout =
    KeyboardLayoutSpec(
      mode = KeyboardMode.NUMBER_PAD,
      rows = NumberPadRows,
    )

  fun getLayout(
    mode: KeyboardMode,
    showNumberRow: Boolean,
    languageKind: LanguageInputMethodKind = LanguageInputMethodKind.DIRECT_QWERTY,
  ): KeyboardLayoutSpec {
    return when (mode) {
      KeyboardMode.ALPHABET,
      KeyboardMode.EMOJI -> {
        if (languageKind == LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT) {
          if (showNumberRow) ProbhatLayoutWithNumberRow else ProbhatLayoutCompact
        } else {
          if (showNumberRow) AlphabetLayoutWithNumberRow else AlphabetLayoutCompact
        }
      }
      KeyboardMode.SYMBOLS_PRIMARY -> {
        if (languageKind == LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT) {
          ProbhatSymbolsPrimaryLayout
        } else {
          SymbolsPrimaryLayout
        }
      }
      KeyboardMode.SYMBOLS_SECONDARY -> SymbolsSecondaryLayout
      KeyboardMode.NUMBER_PAD -> NumberPadLayout
    }
  }
}
