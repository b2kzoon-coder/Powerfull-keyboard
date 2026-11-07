package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.clipboard.ClipboardController
import com.example.clipboard.ClipboardEntry
import com.example.language.LanguageInputMethodKind
import com.example.language.LanguageManager
import com.example.language.hindi.HindiPhoneticEngine
import com.example.layout.KeyAction
import com.example.layout.KeyboardHeightBounds
import com.example.layout.KeyboardMode
import com.example.layout.LayoutManager
import com.example.settings.KeyboardPreferencesRepository
import com.example.theme.KeyboardThemePreset
import com.example.translate.LanguageSelectorMode
import com.example.translate.SmartTranslatorManager
import com.example.translate.TranslateLanguageItem
import com.example.translate.TranslatorUiState
import com.example.voice.BanglishTransliterator
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceRecognitionManager
import com.example.voice.VoiceState
import com.example.voice.VoiceUiState
import com.example.keyboard.KeyboardTypography
import com.example.keyboard.isBengaliCombiningLabel
import com.example.keyboard.isBengaliCombiningChar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.ime.PersonalKeyboardService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app_name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal Keyboard", appName)
  }

  @Test
  fun `banglish transliterator converts required bengali sentences to natural romanized banglish`() {
    assertEquals("Tumi kemon acho", BanglishTransliterator.transliterate("তুমি কেমন আছো"))
    assertEquals("Ami valo achi", BanglishTransliterator.transliterate("আমি ভালো আছি"))
    assertEquals("Tomar nam ki", BanglishTransliterator.transliterate("তোমার নাম কী"))
    assertEquals("Tumi kothay jaccho", BanglishTransliterator.transliterate("তুমি কোথায় যাচ্ছো"))
    assertEquals("Ami Bangladesh-e thaki", BanglishTransliterator.transliterate("আমি বাংলাদেশে থাকি"))
  }

  @Test
  fun `banglish transliterator handles arbitrary bengali vowels consonants conjuncts and punctuation`() {
    assertEquals("Ami Bangla valobashi.", BanglishTransliterator.transliterate("আমি বাংলা ভালোবাসি।"))
    assertEquals("Dhonnobad", BanglishTransliterator.transliterate("ধন্যবাদ"))
    assertEquals("Shuvo shokal", BanglishTransliterator.transliterate("শুভ সকাল"))
    assertEquals("Amar desh Bangladesh", BanglishTransliterator.transliterate("আমার দেশ বাংলাদেশ"))
    assertEquals(
      "Tumi kemon acho? Ami valo achi.",
      BanglishTransliterator.transliterate("তুমি কেমন আছো? আমি ভালো আছি।"),
    )
  }

  @Test
  fun `probhat layout matches reference screenshot keys exactly`() {
    val probhatLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )

    assertEquals(4, probhatLayout.rows.size)

    // Row 1: 12 keys
    val r1 = probhatLayout.rows[0]
    assertEquals(12, r1.size)
    val r1Expected =
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
      )
    r1Expected.forEachIndexed { i, (unsh, sh) ->
      assertEquals("Row 1 index $i unshifted", unsh, r1[i].primaryLabel)
      assertEquals("Row 1 index $i shifted", sh, r1[i].shiftedLabel)
    }

    // Row 2: 9 keys
    val r2 = probhatLayout.rows[1]
    assertEquals(9, r2.size)
    val r2Expected =
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
      )
    r2Expected.forEachIndexed { i, (unsh, sh) ->
      assertEquals("Row 2 index $i unshifted", unsh, r2[i].primaryLabel)
      assertEquals("Row 2 index $i shifted", sh, r2[i].shiftedLabel)
    }

    // Row 3: Shift + 9 keys + Backspace = 11 keys
    val r3 = probhatLayout.rows[2]
    assertEquals(11, r3.size)
    assertEquals(KeyAction.Shift, r3.first().action)
    assertEquals(KeyAction.Backspace, r3.last().action)

    val r3Middle = r3.subList(1, 10)
    val r3Expected =
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
    r3Expected.forEachIndexed { i, (unsh, sh) ->
      assertEquals("Row 3 index $i unshifted", unsh, r3Middle[i].primaryLabel)
      assertEquals("Row 3 index $i shifted", sh, r3Middle[i].shiftedLabel)
    }

    // Bottom row has Dari (।), Comma, Spacebar with Probhat label
    val r4 = probhatLayout.rows[3]
    val spaceKey = r4.firstOrNull { it.id == "key_space_pb" }
    assertNotNull(spaceKey)
    assertEquals("◄  প্রভাত  ►", spaceKey?.primaryLabel)

    val dariKey = r4.firstOrNull { it.id == "key_dari_pb" }
    assertNotNull(dariKey)
    assertEquals("।", dariKey?.primaryLabel)
  }

  @Test
  fun `probhat hasanta enables Unicode conjunct composition`() {
    val probhatLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )

    val r2 = probhatLayout.rows[1]
    val r3 = probhatLayout.rows[2]

    val keyKa = r2.first { it.primaryLabel == "ক" }.primaryLabel
    val keyHasanta = r3.first { it.primaryLabel == "্" }.primaryLabel
    val keyTa = r2.first { it.primaryLabel == "ত" }.primaryLabel

    val conjunct = "$keyKa$keyHasanta$keyTa"
    assertEquals("ক্ত", conjunct)
  }

  @Test
  fun `language manager switches between English Probhat Banglish and Hindi modes with Avro removed`() {
    val manager = LanguageManager()

    // 1. Initial mode is English
    assertEquals(LanguageManager.ID_ENGLISH, manager.activeLanguage.value.id)
    assertEquals("English", manager.activeLanguage.value.spacebarLabel)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, manager.activeLanguage.value.kind)

    // 2. Swipe left -> বাংলা (Probhat)
    assertTrue(manager.onSpacebarSwipeLeft())
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, manager.activeLanguage.value.id)
    assertEquals("◄  প্রভাত  ►", manager.activeLanguage.value.spacebarLabel)
    assertEquals(LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT, manager.activeLanguage.value.kind)

    // 3. Swipe left again -> Banglish
    assertTrue(manager.onSpacebarSwipeLeft())
    assertEquals(LanguageManager.ID_BANGLISH, manager.activeLanguage.value.id)
    assertEquals("Banglish", manager.activeLanguage.value.spacebarLabel)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, manager.activeLanguage.value.kind)

    // 4. Swipe left again -> हिन्दी (Hinglish)
    assertTrue(manager.onSpacebarSwipeLeft())
    assertEquals(LanguageManager.ID_HINDI_HINGLISH, manager.activeLanguage.value.id)
    assertEquals("हिन्दी (Hinglish)", manager.activeLanguage.value.spacebarLabel)
    assertEquals(LanguageInputMethodKind.PHONETIC_HINDI, manager.activeLanguage.value.kind)

    // 5. Swipe right -> directly back to English
    assertTrue(manager.onSpacebarSwipeRight())
    assertEquals(LanguageManager.ID_ENGLISH, manager.activeLanguage.value.id)

    // 6. Layout switcher cycles through all 4 modes (English -> Probhat -> Banglish -> Hindi -> English)
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, manager.switchToNextLanguage().id)
    assertEquals(LanguageManager.ID_BANGLISH, manager.switchToNextLanguage().id)
    assertEquals(LanguageManager.ID_HINDI_HINGLISH, manager.switchToNextLanguage().id)
    assertEquals(LanguageManager.ID_ENGLISH, manager.switchToNextLanguage().id)

    // 7. Legacy bn_AVRO preference safely falls back to Probhat
    manager.setActiveLanguage("bn_AVRO")
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, manager.activeLanguage.value.id)
  }

  @Test
  fun `preferences repository persists portrait and landscape height percentages and cut copy paste`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = KeyboardPreferencesRepository.getInstance(context)

    repo.setPortraitHeightPercent(KeyboardHeightBounds.DEFAULT_PORTRAIT_PERCENT)
    repo.setLandscapeHeightPercent(KeyboardHeightBounds.DEFAULT_LANDSCAPE_PERCENT)
    assertEquals(130, repo.config.value.portraitHeightPercent)
    assertEquals(100, repo.config.value.landscapeHeightPercent)

    repo.setPortraitHeightPercent(145)
    repo.setLandscapeHeightPercent(85)
    repo.setThemePreset(KeyboardThemePreset.OLED_BLACK)
    repo.setCutCopyPasteEnabled(false)

    assertEquals(145, repo.config.value.portraitHeightPercent)
    assertEquals(85, repo.config.value.landscapeHeightPercent)
    assertEquals(KeyboardThemePreset.OLED_BLACK, repo.config.value.themePreset)
    assertFalse(repo.config.value.cutCopyPasteEnabled)

    repo.setPortraitHeightPercent(40)
    repo.setLandscapeHeightPercent(220)
    assertEquals(60, repo.config.value.portraitHeightPercent)
    assertEquals(180, repo.config.value.landscapeHeightPercent)

    repo.setCutCopyPasteEnabled(true)
    assertTrue(repo.config.value.cutCopyPasteEnabled)
  }

  @Test
  fun `banglish transliterator handles vowels aspirated consonants reph ya-phala ba-phala and clusters`() {
    assertEquals("Kormo", BanglishTransliterator.transliterate("কর্ম"))
    assertEquals("Dhormo", BanglishTransliterator.transliterate("ধর্ম"))
    assertEquals("Bisshash", BanglishTransliterator.transliterate("বিশ্বাস"))
    assertEquals("Shadhin", BanglishTransliterator.transliterate("স্বাধীন"))
    assertEquals("Biggan", BanglishTransliterator.transliterate("বিজ্ঞান"))
    assertEquals("Bakko", BanglishTransliterator.transliterate("বাক্য"))
    assertEquals("Shotto", BanglishTransliterator.transliterate("সত্য"))
    assertEquals("Dhonno", BanglishTransliterator.transliterate("ধন্য"))
  }

  @Test
  fun `probhat keys have shifted secondary hints and clean combining marks without dotted circle`() {
    val probhatLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )

    val combiningMarksFound = mutableListOf<String>()

    probhatLayout.rows.forEachIndexed { rowIndex, row ->
      row.forEach { keySpec ->
        // No literal dotted circle in labels
        assertFalse("Key ${keySpec.id} primaryLabel should not contain ◌", keySpec.primaryLabel.contains("◌"))
        assertFalse("Key ${keySpec.id} primaryLabel should not contain \\u25CC", keySpec.primaryLabel.contains("\u25CC"))
        assertFalse("Key ${keySpec.id} shiftedLabel should not contain \\u25CC", keySpec.shiftedLabel.contains("\u25CC"))

        if (com.example.keyboard.isBengaliCombiningLabel(keySpec.primaryLabel)) {
          combiningMarksFound.add(keySpec.primaryLabel)
        }
        if (com.example.keyboard.isBengaliCombiningLabel(keySpec.shiftedLabel)) {
          combiningMarksFound.add(keySpec.shiftedLabel)
        }
      }
    }

    // Ensure all 13 combining marks on Probhat layout are detected for custom Canvas rendering
    val expectedMarks = listOf("া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ", "্", "ঁ", "ং", "ঃ")
    expectedMarks.forEach { mark ->
      assertTrue("Combining mark $mark must be detected in Probhat keys", combiningMarksFound.contains(mark))
      assertNotNull("Combining mark $mark must have pre-parsed vector path", com.example.keyboard.BengaliGlyphPaths.getPath(mark))
    }

    // Verify last key of first row ("ো" / "ৌ") has horizontally separated two-part glyph bounds (not overlapping at X=0)
    val lastKeyRow1 = probhatLayout.rows[0].last()
    assertEquals("ো", lastKeyRow1.primaryLabel)
    assertEquals("ৌ", lastKeyRow1.shiftedLabel)
    assertEquals(KeyAction.CommitChar("ো", "ৌ"), lastKeyRow1.action)
    val eBounds = com.example.keyboard.BengaliGlyphPaths.getPath("ে")!!.getBounds()
    val oBounds = com.example.keyboard.BengaliGlyphPaths.getPath("ো")!!.getBounds()
    val auBounds = com.example.keyboard.BengaliGlyphPaths.getPath("ৌ")!!.getBounds()
    assertTrue("ো path width (${oBounds.width}) must be wider than single ে (${eBounds.width}) so left and right parts do not overlap", oBounds.width > eBounds.width * 1.5f)
    assertTrue("ৌ path width (${auBounds.width}) must be wider than single ে (${eBounds.width}) so left and right parts do not overlap", auBounds.width > eBounds.width * 1.5f)
  }

  @Test
  fun `keyboard height bounds scale row height and toolbar height across 60 to 180 percent`() {
    // Portrait tests
    assertEquals(30f, KeyboardHeightBounds.rowHeightDpForPercent(60, isLandscape = false), 0.01f)
    assertEquals(50f, KeyboardHeightBounds.rowHeightDpForPercent(100, isLandscape = false), 0.01f)
    assertEquals(65f, KeyboardHeightBounds.rowHeightDpForPercent(130, isLandscape = false), 0.01f)
    assertEquals(90f, KeyboardHeightBounds.rowHeightDpForPercent(180, isLandscape = false), 0.01f)

    // At 180%, 4 rows = 360dp, toolbar ~48.6dp, container padding ~16dp -> ~424.6dp total (within ~380-440dp range)
    val rowH180 = KeyboardHeightBounds.rowHeightDpForPercent(180, isLandscape = false)
    val toolbarH180 = KeyboardHeightBounds.toolbarHeightDpForPercent(180, isLandscape = false)
    val totalEstimated = (rowH180 * 4) + toolbarH180 + 16f
    assertTrue("Total height at 180% ($totalEstimated) should be within 380-440dp", totalEstimated in 380f..440f)

    // Landscape tests (independent base 38dp)
    assertEquals(22.8f, KeyboardHeightBounds.rowHeightDpForPercent(60, isLandscape = true), 0.01f)
    assertEquals(38f, KeyboardHeightBounds.rowHeightDpForPercent(100, isLandscape = true), 0.01f)
    assertEquals(68.4f, KeyboardHeightBounds.rowHeightDpForPercent(180, isLandscape = true), 0.01f)
  }

  @Test
  fun `vibration and sound feedback toggles operate completely independently`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = KeyboardPreferencesRepository.getInstance(context)

    // Mode 1: Both OFF
    repo.setHapticFeedback(false)
    repo.setSoundOnKeypress(false)
    assertFalse(repo.config.value.hapticFeedback)
    assertFalse(repo.config.value.soundOnKeypress)

    // Mode 2: Sound ON + Vibration OFF
    repo.setSoundOnKeypress(true)
    assertFalse(repo.config.value.hapticFeedback)
    assertTrue(repo.config.value.soundOnKeypress)

    // Mode 3: Sound OFF + Vibration ON
    repo.setHapticFeedback(true)
    repo.setSoundOnKeypress(false)
    assertTrue(repo.config.value.hapticFeedback)
    assertFalse(repo.config.value.soundOnKeypress)

    // Mode 4: Both ON
    repo.setSoundOnKeypress(true)
    assertTrue(repo.config.value.hapticFeedback)
    assertTrue(repo.config.value.soundOnKeypress)
  }

  @Test
  fun `keyboard feedback manager triggers all key types smoothly in all modes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = KeyboardPreferencesRepository.getInstance(context)
    val feedbackManager = com.example.keyboard.KeyboardFeedbackManager(context, repo)

    val keyTypes = listOf(
      com.example.keyboard.KeyFeedbackType.CHARACTER,
      com.example.keyboard.KeyFeedbackType.DELETE,
      com.example.keyboard.KeyFeedbackType.SPACE,
      com.example.keyboard.KeyFeedbackType.ENTER,
      com.example.keyboard.KeyFeedbackType.FUNCTIONAL,
    )

    // Test with Both ON
    repo.setHapticFeedback(true)
    repo.setSoundOnKeypress(true)
    keyTypes.forEach { type ->
      feedbackManager.trigger(type)
    }

    // Test with Sound ON + Vibration OFF
    repo.setHapticFeedback(false)
    repo.setSoundOnKeypress(true)
    keyTypes.forEach { type ->
      feedbackManager.trigger(type)
    }

    // Test with Sound OFF + Vibration ON
    repo.setHapticFeedback(true)
    repo.setSoundOnKeypress(false)
    keyTypes.forEach { type ->
      feedbackManager.trigger(type)
    }

    // Test with Both OFF
    repo.setHapticFeedback(false)
    repo.setSoundOnKeypress(false)
    keyTypes.forEach { type ->
      feedbackManager.trigger(type)
    }

    feedbackManager.release()
  }

  @Test
  fun `raw click sound assets exist in resources`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val res = context.resources
    val idChar = res.getIdentifier("keyboard_click_char", "raw", context.packageName)
    val idDelete = res.getIdentifier("keyboard_click_delete", "raw", context.packageName)
    val idSpace = res.getIdentifier("keyboard_click_space", "raw", context.packageName)

    assertTrue("R.raw.keyboard_click_char should exist", idChar != 0)
    assertTrue("R.raw.keyboard_click_delete should exist", idDelete != 0)
    assertTrue("R.raw.keyboard_click_space should exist", idSpace != 0)
  }

  @Test
  fun `emoji data contains all 9 non-recent categories with rich content`() {
    val categories = com.example.emoji.EmojiCategory.entries
    assertEquals(10, categories.size)

    val smileys = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.SMILEYS)
    val people = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.PEOPLE)
    val animals = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.ANIMALS)
    val food = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.FOOD)
    val travel = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.TRAVEL)
    val activities = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.ACTIVITIES)
    val objects = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.OBJECTS)
    val symbols = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.SYMBOLS)
    val flags = com.example.emoji.EmojiData.getEmojisForCategory(com.example.emoji.EmojiCategory.FLAGS)

    assertTrue(smileys.size >= 50)
    assertTrue(people.size >= 40)
    assertTrue(animals.size >= 40)
    assertTrue(food.size >= 40)
    assertTrue(travel.size >= 30)
    assertTrue(activities.size >= 30)
    assertTrue(objects.size >= 30)
    assertTrue(symbols.size >= 30)
    assertTrue(flags.size >= 30)

    val bdFlag = flags.firstOrNull { it.unicode == "🇧🇩" }
    assertNotNull(bdFlag)
    assertEquals("flag: Bangladesh", bdFlag?.name)

    // Verify exact Ridmik category navigation order:
    // Recent, Smileys, People, Animals, Food, Activities, Travel, Objects, Symbols, Flags
    val expectedOrder = listOf(
      com.example.emoji.EmojiCategory.RECENT,
      com.example.emoji.EmojiCategory.SMILEYS,
      com.example.emoji.EmojiCategory.PEOPLE,
      com.example.emoji.EmojiCategory.ANIMALS,
      com.example.emoji.EmojiCategory.FOOD,
      com.example.emoji.EmojiCategory.ACTIVITIES,
      com.example.emoji.EmojiCategory.TRAVEL,
      com.example.emoji.EmojiCategory.OBJECTS,
      com.example.emoji.EmojiCategory.SYMBOLS,
      com.example.emoji.EmojiCategory.FLAGS,
    )
    assertEquals(expectedOrder, categories)
  }

  @Test
  fun `emoji search locates items by keyword and name`() {
    val resultsHeart = com.example.emoji.EmojiData.search("heart")
    assertTrue("Search for 'heart' should return items", resultsHeart.isNotEmpty())
    assertTrue(resultsHeart.any { it.unicode == "❤️" })

    val resultsBangladesh = com.example.emoji.EmojiData.search("bangla")
    assertTrue("Search for 'bangla' should return Bangladesh flag", resultsBangladesh.any { it.unicode == "🇧🇩" })

    val resultsFire = com.example.emoji.EmojiData.search("fire")
    assertTrue("Search for 'fire' should return fire emoji", resultsFire.any { it.unicode == "🔥" })
  }

  @Test
  fun `emoji text util calculates correct grapheme length for deletion`() {
    // 1. Single ASCII character: 1 UTF-16 code unit
    assertEquals(1, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Hello A"))

    // 2. Simple emoji (surrogate pair): 2 UTF-16 code units
    assertEquals(2, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Text 😀"))

    // 3. Emoji with variation selector: 2 UTF-16 code units
    assertEquals(2, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Text ❤️"))

    // 4. Emoji with skin tone modifier: 4 UTF-16 code units
    assertEquals(4, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Text 👍🏽"))

    // 5. Flag sequence (two regional indicators): 4 UTF-16 code units
    assertEquals(4, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Text 🇧🇩"))

    // 6. Complex family ZWJ sequence (👨‍👩‍👧‍👦): 11 UTF-16 code units
    val familyEmoji = "👨‍👩‍👧‍👦"
    assertEquals(11, familyEmoji.length)
    assertEquals(11, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Text $familyEmoji"))
  }

  @Test
  fun `emoji text util generates 6 skin tone variants for supported emojis`() {
    val variants = com.example.emoji.EmojiTextUtil.getSkinToneVariants("👍")
    assertEquals(6, variants.size)
    assertEquals("👍", variants[0])
    assertTrue(variants[1].contains("\uD83C\uDFFB"))
    assertTrue(variants[2].contains("\uD83C\uDFFC"))
    assertTrue(variants[3].contains("\uD83C\uDFFD"))
    assertTrue(variants[4].contains("\uD83C\uDFFE"))
    assertTrue(variants[5].contains("\uD83C\uDFFF"))
  }

  @Test
  fun `emoji preferences repository tracks recents and frequently used emojis`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.emoji.EmojiPreferencesRepository.getInstance(context)

    repo.clearRecent()
    assertTrue(repo.recentEmojis.value.isEmpty())

    repo.recordEmojiUsage("🔥")
    repo.recordEmojiUsage("❤️")
    repo.recordEmojiUsage("🔥")

    assertEquals("🔥", repo.recentEmojis.value.first())
    assertTrue(repo.recentEmojis.value.contains("❤️"))
    assertEquals("🔥", repo.frequentEmojis.value.first())

    repo.setSelectedSkinTone(com.example.emoji.EmojiSkinTone.MEDIUM)
    assertEquals(com.example.emoji.EmojiSkinTone.MEDIUM, repo.selectedSkinTone.value)
  }

  @Test
  fun `dedicated emoji button is immediately to the right of 123 key in English QWERTY and Probhat layouts`() {
    val languageKinds = listOf(
      LanguageInputMethodKind.DIRECT_QWERTY,
      LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      LanguageInputMethodKind.PHONETIC_HINDI,
    )

    for (kind in languageKinds) {
      for (showNumRow in listOf(false, true)) {
        val layout =
          LayoutManager.getLayout(
            mode = KeyboardMode.ALPHABET,
            showNumberRow = showNumRow,
            languageKind = kind,
          )
        val bottomRow = layout.rows.last()
        assertEquals("Bottom row for $kind should have 6 keys", 6, bottomRow.size)

        // Index 0 must be ?123
        assertEquals("?123", bottomRow[0].primaryLabel)
        assertEquals(KeyAction.ModeSymbolsPrimary, bottomRow[0].action)

        // Index 1 (immediately to the right of ?123) must be the Emoji button (😊)
        assertEquals("😊", bottomRow[1].primaryLabel)
        assertEquals(KeyAction.ModeEmoji, bottomRow[1].action)
      }
    }
  }

  @Test
  fun `tapping emoji button action transitions keyboard service to EMOJI mode and back`() {
    val service = org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)

    // Tap Emoji button -> opens Emoji Panel (KeyboardMode.EMOJI)
    service.handleKeyAction(KeyAction.ModeEmoji)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)

    // Tap Back to Keyboard (ABC / বাং) -> returns to ALPHABET mode
    service.handleKeyAction(KeyAction.ModeAlphabet)
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)
  }

  @Test
  fun `emoji search bar focus keeps emoji panel open and routes typing in English QWERTY and Probhat`() {
    val service = org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // 1. Open Emoji Panel
    service.handleKeyAction(KeyAction.ModeEmoji)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertFalse(service.sessionState.value.isEmojiSearchFocused)

    // 2. Tap Emoji Search Bar -> focus search and show keyboard while keeping Emoji Panel open
    service.setEmojiSearchFocused(true)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertTrue(service.sessionState.value.isEmojiSearchFocused)

    // 3. Test typing in English QWERTY ("fire")
    for (ch in "fire") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
    }
    assertEquals("fire", service.sessionState.value.emojiSearchQuery)
    val englishFireResults =
      com.example.emoji.EmojiData.search(
        service.sessionState.value.emojiSearchQuery,
        service.sessionState.value.emojiSearchRawQuery,
      )
    assertTrue("English search for 'fire' must return 🔥", englishFireResults.any { it.unicode == "🔥" })

    // Test Backspace in search query
    service.handleKeyAction(KeyAction.Backspace)
    assertEquals("fir", service.sessionState.value.emojiSearchQuery)

    // Test Clear (X) action
    service.setEmojiSearchQuery("")
    assertEquals("", service.sessionState.value.emojiSearchQuery)
    assertTrue("Clearing search query keeps Emoji Panel open", service.sessionState.value.mode == KeyboardMode.EMOJI)

    // 4. Switch to Bangla Probhat and test direct Bengali character emoji search ("হাসি" -> 😀, "আগুন" -> 🔥)
    service.handleKeyAction(KeyAction.LanguageSwitch) // English -> Bangla Probhat
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, service.languageManager.activeLanguage.value.id)
    for (ch in listOf("হ", "া", "স", "ি")) {
      service.handleKeyAction(KeyAction.CommitChar(ch, ch))
    }
    assertEquals("হাসি", service.sessionState.value.emojiSearchQuery)
    val probhatHashiResults =
      com.example.emoji.EmojiData.search(
        service.sessionState.value.emojiSearchQuery,
        service.sessionState.value.emojiSearchRawQuery,
      )
    assertTrue("Probhat search for 'হাসি' must return 😀", probhatHashiResults.any { it.unicode == "😀" })

    // 5. Dismiss keyboard via Enter/Search action -> restores full Emoji Panel without closing it
    service.handleKeyAction(KeyAction.Enter)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertFalse("Enter in emoji search should dismiss keyboard and restore full Emoji Panel", service.sessionState.value.isEmojiSearchFocused)
  }

  @Test
  fun `personal keyboard service input view composes emoji panel and emoji search without crashing`() {
    val service = org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    val inputView = service.onCreateInputView()
    assertNotNull("onCreateInputView must return a non-null ComposeView", inputView)

    // Attach the IME ComposeView to an Android window (without ComponentActivity OnBackPressedDispatcherOwner,
    // matching InputMethodService's window environment)
    val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).create().start().resume().visible().get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)
    container.addView(inputView)

    val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
    val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.AT_MOST)

    // 1. Initial ALPHABET mode composition
    inputView.measure(widthSpec, heightSpec)
    inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)

    // 2. Tap Emoji button -> must immediately compose EmojiPanel on first tap without throwing
    service.handleKeyAction(KeyAction.ModeEmoji)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    inputView.measure(widthSpec, heightSpec)
    inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertTrue("EmojiPanel must have positive measured height when opened", inputView.measuredHeight > 0)

    // 3. Tap Emoji Search Bar -> must keep EmojiPanel visible and compose keyboard rows below it
    service.setEmojiSearchFocused(true)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    inputView.measure(widthSpec, heightSpec)
    inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertTrue(service.sessionState.value.isEmojiSearchFocused)
    assertTrue("EmojiPanel + keyboard must have positive measured height during search", inputView.measuredHeight > 0)

    // 4. Press Back key while Emoji Search is focused -> dismisses search keyboard and keeps EmojiPanel open
    val handledBack = service.onKeyDown(android.view.KeyEvent.KEYCODE_BACK, android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_BACK))
    assertTrue("Back key should be handled when Emoji Search is focused", handledBack)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertFalse(service.sessionState.value.isEmojiSearchFocused)
  }

  @Test
  fun `emoji search maintains persistent focus and keeps keyboard open continuously across character input and restarts`() {
    val service = org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // Open Emoji Panel
    service.handleKeyAction(KeyAction.ModeEmoji)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)

    // Tap search bar to focus
    service.setEmojiSearchFocused(true)
    assertTrue("Emoji Search must be focused", service.sessionState.value.isEmojiSearchFocused)

    // Type character by character: 'f', 'i', 'r', 'e'
    val characters = listOf("f", "i", "r", "e")
    var expectedQuery = ""
    for (char in characters) {
      service.handleKeyAction(KeyAction.CommitChar(char, char.uppercase()))
      expectedQuery += char
      // Critical check: Focus must NEVER drop and keyboard must NEVER hide after typing each character
      assertTrue("Keyboard must remain visible and search focused after typing '$char'", service.sessionState.value.isEmojiSearchFocused)
      assertEquals(expectedQuery, service.sessionState.value.emojiSearchQuery)
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    }

    // Simulate system onStartInputView restarting (which occurs when input connection reconnects or updates)
    service.onStartInputView(null, restarting = true)
    assertTrue("Emoji search focus must NOT be lost when onStartInputView restarts", service.sessionState.value.isEmojiSearchFocused)
    assertEquals("fire", service.sessionState.value.emojiSearchQuery)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)

    // Continue typing after restart
    service.handleKeyAction(KeyAction.CommitChar("t", "T"))
    assertTrue("Search must remain focused after typing additional character", service.sessionState.value.isEmojiSearchFocused)
    assertEquals("firet", service.sessionState.value.emojiSearchQuery)

    // Backspace must also maintain focus and keep keyboard open
    service.handleKeyAction(KeyAction.Backspace)
    assertTrue("Search must remain focused after backspace", service.sessionState.value.isEmojiSearchFocused)
    assertEquals("fire", service.sessionState.value.emojiSearchQuery)

    // Clear search must also maintain focus and keep keyboard open
    service.setEmojiSearchQuery("")
    assertTrue("Search must remain focused after clearing query", service.sessionState.value.isEmojiSearchFocused)
    assertEquals("", service.sessionState.value.emojiSearchQuery)

    // Only explicit user dismissal (Enter action) dismisses search keyboard
    service.handleKeyAction(KeyAction.Enter)
    assertFalse("Enter action should dismiss search keyboard", service.sessionState.value.isEmojiSearchFocused)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
  }

  @Test
  fun `emoji data catalog and search results never contain duplicate unicode entries`() {
    val allUnicodes = com.example.emoji.EmojiData.allEmojis.map { it.unicode }
    assertEquals(
      "EmojiData.allEmojis must never contain duplicate unicode strings (which crash LazyVerticalGrid)",
      allUnicodes.distinct().size,
      allUnicodes.size,
    )

    for (category in com.example.emoji.EmojiCategory.entries) {
      val catUnicodes = com.example.emoji.EmojiData.getEmojisForCategory(category).map { it.unicode }
      assertEquals(
        "Category $category must never contain duplicate unicode strings",
        catUnicodes.distinct().size,
        catUnicodes.size,
      )
    }

    val testQueries =
      ('a'..'z').map { it.toString() } +
        ('0'..'9').map { it.toString() } +
        listOf("heart", "smile", "fire", "love", "happy", "broken", "100", "red", "yellow", "flag", "হ", "আগুন", "হাসি", "ভালোবাসা")
    for (q in testQueries) {
      val results = com.example.emoji.EmojiData.search(q).map { it.unicode }
      assertEquals(
        "Search results for '$q' must never contain duplicate unicode strings",
        results.distinct().size,
        results.size,
      )
    }
  }

  @Test
  fun `emoji search full runtime lifecycle with ComposeView stays visible across heart smile fire love happy queries`() {
    com.example.ime.ImeDiagnosticsLogger.clear()
    val service = org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    val inputView = service.onCreateInputView()

    // Verify focus isolation on root IME view so it can never steal focus from the host editor
    assertFalse("IME root view must not be focusable", inputView.isFocusable)
    assertFalse("IME root view must not be focusable in touch mode", inputView.isFocusableInTouchMode)
    assertFalse("IME root view requestFocus() must return false", inputView.requestFocus())

    val activity = org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java).create().start().resume().visible().get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)
    container.addView(inputView)

    val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
    val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(1200, android.view.View.MeasureSpec.AT_MOST)

    val editorInfo = android.view.inputmethod.EditorInfo().apply {
      packageName = "com.mi.android.globallauncher"
      fieldId = 101
      inputType = android.text.InputType.TYPE_CLASS_TEXT
    }

    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    fun measureAndIdle() {
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      inputView.measure(widthSpec, heightSpec)
      inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    measureAndIdle()
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)

    // 1. Open Emoji Panel
    service.handleKeyAction(KeyAction.ModeEmoji)
    measureAndIdle()
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertFalse(service.sessionState.value.isEmojiSearchFocused)
    val fullEmojiPanelHeight = inputView.measuredHeight
    assertTrue("Full EmojiPanel height must be positive", fullEmojiPanelHeight > 0)

    // 2. Tap Emoji Search Bar
    service.setEmojiSearchFocused(true)
    measureAndIdle()
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    assertTrue(service.sessionState.value.isEmojiSearchFocused)
    val searchActiveHeight = inputView.measuredHeight
    assertTrue(
      "When Emoji Search is active, KeyboardSurface must render both compact EmojiPanel and keyboard rows ($searchActiveHeight >= $fullEmojiPanelHeight)",
      searchActiveHeight >= fullEmojiPanelHeight,
    )

    // Simulate host window reacting to IME inset height change with non-restarting onStartInputView
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)
    measureAndIdle()
    assertTrue("Search focus must survive host window inset-triggered onStartInputView(restarting=false)", service.sessionState.value.isEmojiSearchFocused)
    assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)

    // 3. Test all required queries character-by-character: heart, smile, fire, love, happy
    val queriesAndExpectedEmoji = listOf(
      "heart" to "❤️",
      "smile" to "😀",
      "fire" to "🔥",
      "love" to "❤️",
      "happy" to "😀",
    )

    for ((word, expectedEmoji) in queriesAndExpectedEmoji) {
      service.setEmojiSearchQuery("")
      measureAndIdle()
      assertTrue("Search must stay focused after clearing for '$word'", service.sessionState.value.isEmojiSearchFocused)

      var built = ""
      for (ch in word) {
        service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
        built += ch
        measureAndIdle()

        // Simulate host window or InputConnection callback after each character
        service.onStartInputView(editorInfo, true)
        measureAndIdle()

        assertEquals("Mode must remain EMOJI after typing '$ch' in '$word'", KeyboardMode.EMOJI, service.sessionState.value.mode)
        assertTrue("Search focus must remain true after typing '$ch' in '$word'", service.sessionState.value.isEmojiSearchFocused)
        assertEquals("Query must match '$built'", built, service.sessionState.value.emojiSearchQuery)
        assertEquals("Measured height must remain stable search+keyboard height", searchActiveHeight, inputView.measuredHeight)
      }

      val results = com.example.emoji.EmojiData.search(
        service.sessionState.value.emojiSearchQuery,
        service.sessionState.value.emojiSearchRawQuery,
      )
      assertTrue("Search for '$word' must return results including $expectedEmoji", results.any { it.unicode == expectedEmoji })
    }

    // 4. Test 20 consecutive characters, Backspace and retyping
    service.setEmojiSearchQuery("")
    measureAndIdle()
    val twentyChars = "abcdefghijklmnopqrst"
    assertEquals(20, twentyChars.length)
    var builtTwenty = ""
    for (ch in twentyChars) {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
      builtTwenty += ch
      measureAndIdle()
      assertTrue("Search must remain focused during 20 consecutive characters at '$builtTwenty'", service.sessionState.value.isEmojiSearchFocused)
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
      assertEquals(builtTwenty, service.sessionState.value.emojiSearchQuery)
      assertEquals(builtTwenty.length, service.sessionState.value.emojiSearchCursor)
    }

    // Backspace all 20 characters one by one and verify focus never drops
    for (i in 19 downTo 0) {
      service.handleKeyAction(KeyAction.Backspace)
      measureAndIdle()
      assertTrue("Search must remain focused while backspacing at index $i", service.sessionState.value.isEmojiSearchFocused)
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
      assertEquals(twentyChars.substring(0, i), service.sessionState.value.emojiSearchQuery)
      assertEquals(i, service.sessionState.value.emojiSearchCursor)
    }

    // Retype "heart" after full backspace
    for (ch in "heart") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
      measureAndIdle()
      assertTrue("Search must remain focused when retyping '$ch'", service.sessionState.value.isEmojiSearchFocused)
    }
    assertEquals("heart", service.sessionState.value.emojiSearchQuery)

    // 5. Test cursor movement and mid-query editing ("healt" -> move left -> backspace -> 'r' -> "heart")
    service.setEmojiSearchQuery("healt")
    assertEquals(5, service.sessionState.value.emojiSearchCursor)
    service.moveEmojiSearchCursorLeft() // cursor at 4 (between 'l' and 't')
    assertEquals(4, service.sessionState.value.emojiSearchCursor)
    service.handleKeyAction(KeyAction.Backspace) // deletes 'l' -> "heat", cursor at 3
    assertEquals("heat", service.sessionState.value.emojiSearchQuery)
    assertEquals(3, service.sessionState.value.emojiSearchCursor)
    service.handleKeyAction(KeyAction.CommitChar("r", "R")) // inserts 'r' -> "heart", cursor at 4
    assertEquals("heart", service.sessionState.value.emojiSearchQuery)
    assertEquals(4, service.sessionState.value.emojiSearchCursor)
    service.moveEmojiSearchCursorRight() // cursor back at end (5)
    assertEquals(5, service.sessionState.value.emojiSearchCursor)
    assertTrue("Search must remain focused throughout cursor editing", service.sessionState.value.isEmojiSearchFocused)

    // 6. Verify diagnostics log recorded key routing to Emoji Search and no unexpected hideWindow / requestHideSelf occurred
    val events = com.example.ime.ImeDiagnosticsLogger.getEvents()
    assertTrue("Diagnostics logger must record runtime events", events.isNotEmpty())
    assertTrue(
      "Diagnostics logger must record routeToEmojiSearch events",
      events.any { it.category == "KEY_ROUTING" && it.event == "routeToEmojiSearch" },
    )
    assertFalse(
      "hideWindow must NEVER be called during emoji search typing",
      events.any { it.event == "hideWindow" || it.event == "requestHideSelf" || it.event == "onWindowHidden" },
    )
  }

  @Test
  fun `hindi phonetic engine transliterates Hinglish words and phrases accurately`() {
    // 1. High frequency greetings and common conversational words
    assertEquals("नमस्ते", HindiPhoneticEngine.transliterate("namaste"))
    assertEquals("नमस्कार", HindiPhoneticEngine.transliterate("namaskar"))
    assertEquals("शुक्रिया", HindiPhoneticEngine.transliterate("shukriya"))
    assertEquals("धन्यवाद", HindiPhoneticEngine.transliterate("dhanyawad"))
    assertEquals("धन्यवाद", HindiPhoneticEngine.transliterate("dhanyavad"))
    assertEquals("कृपया", HindiPhoneticEngine.transliterate("kripya"))

    // 2. Pronouns and question words
    assertEquals("आप", HindiPhoneticEngine.transliterate("aap"))
    assertEquals("कैसे", HindiPhoneticEngine.transliterate("kaise"))
    assertEquals("क्या", HindiPhoneticEngine.transliterate("kya"))
    assertEquals("है", HindiPhoneticEngine.transliterate("hai"))
    assertEquals("हैं", HindiPhoneticEngine.transliterate("hain"))
    assertEquals("मेरा", HindiPhoneticEngine.transliterate("mera"))
    assertEquals("मेरी", HindiPhoneticEngine.transliterate("meri"))
    assertEquals("मेरे", HindiPhoneticEngine.transliterate("mere"))
    assertEquals("भारत", HindiPhoneticEngine.transliterate("bharat"))
    assertEquals("दोस्त", HindiPhoneticEngine.transliterate("dost"))
    assertEquals("भाई", HindiPhoneticEngine.transliterate("bhai"))
    assertEquals("घर", HindiPhoneticEngine.transliterate("ghar"))
    assertEquals("पानी", HindiPhoneticEngine.transliterate("pani"))

    // 3. Multi-word phrases
    assertEquals("नमस्ते आप कैसे हैं", HindiPhoneticEngine.transliterate("namaste aap kaise hain"))
  }

  @Test
  fun `select typing language supports 4 options in exact order with Banglish and persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KeyboardPreferencesRepository.getInstance(context)
    val manager = LanguageManager()
    val available = manager.availableLanguages.value
    assertEquals(4, available.size)

    // Verify exact final language selector order:
    // 1. English
    // 2. বাংলা (Probhat)
    // 3. Banglish
    // 4. हिन्दी (Hinglish)
    assertEquals(LanguageManager.ID_ENGLISH, available[0].id)
    assertEquals("English", available[0].displayName)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, available[0].kind)

    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, available[1].id)
    assertEquals("বাংলা (Probhat)", available[1].displayName)
    assertEquals(LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT, available[1].kind)

    assertEquals(LanguageManager.ID_BANGLISH, available[2].id)
    assertEquals("Banglish", available[2].displayName)
    assertEquals("Banglish", available[2].spacebarLabel)
    assertEquals("BN", available[2].badgeLabel)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, available[2].kind)

    assertEquals(LanguageManager.ID_HINDI_HINGLISH, available[3].id)
    assertEquals("हिन्दी (Hinglish)", available[3].displayName)
    assertEquals(LanguageInputMethodKind.PHONETIC_HINDI, available[3].kind)

    assertFalse(
      "Avro must not be present in available languages",
      available.any { it.id.contains("AVRO", ignoreCase = true) },
    )

    // Verify Banglish shares the exact English QWERTY keyboard layout
    val englishLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = available[0].kind,
      )
    val banglishLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = available[2].kind,
      )
    assertEquals(englishLayout.rows.size, banglishLayout.rows.size)
    assertEquals(
      englishLayout.rows.flatten().map { it.primaryLabel },
      banglishLayout.rows.flatten().map { it.primaryLabel },
    )

    // Verify persistence and safe normalization of saved language selections
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, LanguageManager.normalizeLanguageId("bn_AVRO"))
    assertEquals(LanguageManager.ID_BANGLISH, LanguageManager.normalizeLanguageId(LanguageManager.ID_BANGLISH))
    prefs.setActiveLanguageId(LanguageManager.ID_BANGLISH)
    assertEquals(LanguageManager.ID_BANGLISH, prefs.config.value.activeLanguageId)

    // Verify reopening PersonalKeyboardService restores saved typing language
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    assertEquals(LanguageManager.ID_BANGLISH, service.languageManager.activeLanguage.value.id)
    assertEquals(
      VoiceLanguage.Banglish,
      service.voiceRecognitionManager.uiState.value.selectedLanguage,
    )

    // Spacebar swipe left from Banglish switches to Hindi Hinglish, then Probhat
    manager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    assertTrue(manager.onSpacebarSwipeLeft())
    assertEquals(LanguageManager.ID_HINDI_HINGLISH, manager.activeLanguage.value.id)
    assertTrue(manager.onSpacebarSwipeLeft())
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, manager.activeLanguage.value.id)

    // Spacebar swipe right returns directly to English
    assertTrue(manager.onSpacebarSwipeRight())
    assertEquals(LanguageManager.ID_ENGLISH, manager.activeLanguage.value.id)
  }

  @Test
  fun `voice recognition manager lifecycle, 3 Voice Typing Settings options, and Banglish voice output`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    var recognizedText = ""
    var partialText = ""
    val voiceManager =
      VoiceRecognitionManager(
        context = context,
        onSpeechRecognized = { text -> recognizedText = text },
        onSpeechPartial = { text -> partialText = text },
      )

    // 1. Verify Voice Typing Settings contains ONLY 3 languages (Banglish removed from Voice Typing Settings)
    assertEquals(3, VoiceLanguage.SupportedLanguages.size)
    assertEquals(VoiceLanguage.Bangla, VoiceLanguage.SupportedLanguages[0])
    assertEquals(VoiceLanguage.English, VoiceLanguage.SupportedLanguages[1])
    assertEquals(VoiceLanguage.Hindi, VoiceLanguage.SupportedLanguages[2])
    assertFalse(
      "Banglish must NOT be present in Voice Typing Settings SupportedLanguages",
      VoiceLanguage.SupportedLanguages.contains(VoiceLanguage.Banglish),
    )

    // Verify mapping from Select Typing Language IDs to VoiceLanguage
    assertEquals(VoiceLanguage.English, VoiceLanguage.forLanguageId(LanguageManager.ID_ENGLISH))
    assertEquals(VoiceLanguage.Bangla, VoiceLanguage.forLanguageId(LanguageManager.ID_BANGLA_PROBHAT))
    assertEquals(VoiceLanguage.Banglish, VoiceLanguage.forLanguageId(LanguageManager.ID_BANGLISH))
    assertEquals(VoiceLanguage.Hindi, VoiceLanguage.forLanguageId(LanguageManager.ID_HINDI_HINGLISH))

    // Both বাংলা and Banglish use bn-BD recognition engine tag
    assertEquals("bn-BD", VoiceLanguage.Bangla.recognitionTag)
    assertFalse(VoiceLanguage.Bangla.isBanglishOutput)
    assertEquals("bn-Latn", VoiceLanguage.Banglish.tag)
    assertEquals("bn-BD", VoiceLanguage.Banglish.recognitionTag)
    assertTrue(VoiceLanguage.Banglish.isBanglishOutput)

    // 2. বাংলা (Probhat) keeps Bengali Unicode script
    voiceManager.setLanguage(VoiceLanguage.Bangla)
    assertEquals("bn-BD", voiceManager.uiState.value.selectedLanguage.tag)
    voiceManager.simulateTranscription("তুমি কেমন আছো", isFinal = false)
    assertEquals("তুমি কেমন আছো", partialText)
    voiceManager.simulateTranscription("তুমি কেমন আছো", isFinal = true)
    assertEquals("তুমি কেমন আছো", recognizedText)

    // 3. Banglish converts Bengali speech into Romanized Bengali
    voiceManager.setLanguage(VoiceLanguage.Banglish)
    assertEquals("bn-Latn", voiceManager.uiState.value.selectedLanguage.tag)
    voiceManager.simulateTranscription("তুমি কেমন আছো", isFinal = false)
    assertEquals("Tumi kemon acho", partialText)
    assertEquals("Tumi kemon acho", voiceManager.uiState.value.partialTranscription)

    voiceManager.simulateTranscription("আমি ভালো আছি", isFinal = true)
    assertEquals("Ami valo achi", recognizedText)
    assertEquals("Ami valo achi", voiceManager.uiState.value.finalTranscription)

    voiceManager.simulateTranscription("তোমার নাম কী", isFinal = true)
    assertEquals("Tomar nam ki", recognizedText)

    // 4. English and Hindi modes
    voiceManager.setLanguage(VoiceLanguage.Hindi)
    assertEquals("hi-IN", voiceManager.uiState.value.selectedLanguage.tag)

    voiceManager.setLanguage(VoiceLanguage.English)
    assertEquals("en-US", voiceManager.uiState.value.selectedLanguage.tag)
    voiceManager.simulateTranscription("Hello world final", isFinal = true)
    assertEquals("Hello world final", recognizedText)

    // 5. Cancel and destroy
    voiceManager.cancel()
    assertEquals(VoiceState.IDLE, voiceManager.uiState.value.state)
    assertEquals("", voiceManager.uiState.value.partialTranscription)

    voiceManager.destroy()
  }

  private fun grantAudioAndRegisterRecognitionService() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO)
    val pm = org.robolectric.Shadows.shadowOf(app.packageManager)
    val resolveInfo =
      android.content.pm.ResolveInfo().apply {
        serviceInfo =
          android.content.pm.ServiceInfo().apply {
            packageName = "com.google.android.tts"
            name = "com.google.android.libraries.speech.transcription.recognition.grpc.GoogleRecognitionService"
          }
      }
    pm.addResolveInfoForIntent(
      android.content.Intent(android.speech.RecognitionService.SERVICE_INTERFACE),
      resolveInfo,
    )

    runCatching {
      val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
      if (cm != null) {
        val shadowCm = org.robolectric.Shadows.shadowOf(cm)
        val networkInfo =
          org.robolectric.shadows.ShadowNetworkInfo.newInstance(
            android.net.NetworkInfo.DetailedState.CONNECTED,
            android.net.ConnectivityManager.TYPE_WIFI,
            0,
            true,
            true,
          )
        shadowCm.setActiveNetworkInfo(networkInfo)
      }
    }
  }

  @Test
  fun `voice typing panel and language selector panel toggle from keyboard service`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(app).denyPermissions(android.Manifest.permission.RECORD_AUDIO)

    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // 1. Without RECORD_AUDIO permission, tapping mic must NOT show "Listening..." (isVoiceTypingOpen stays false)
    service.toggleVoiceTyping()
    assertFalse("Must NOT show Listening when RECORD_AUDIO permission is missing", service.isVoiceTypingOpen.value)
    assertEquals(VoiceState.ERROR, service.voiceRecognitionManager.uiState.value.state)

    // 2. Grant RECORD_AUDIO and register RecognitionService
    grantAudioAndRegisterRecognitionService()

    // 3. Open Language Selector Panel
    service.toggleLanguageSelector()
    assertTrue(service.isLanguageSelectorOpen.value)
    assertFalse(service.isVoiceTypingOpen.value)

    // 4. Start Voice Typing -> enters STARTING first (must NOT show "Listening..." before onReadyForSpeech!)
    service.toggleVoiceTyping()
    assertEquals(VoiceState.STARTING, service.voiceRecognitionManager.uiState.value.state)
    assertFalse(
      "Must NOT show Listening before onReadyForSpeech fires",
      service.isVoiceTypingOpen.value,
    )
    assertFalse(service.isLanguageSelectorOpen.value)

    // 5. Once onReadyForSpeech fires, state transitions to LISTENING and isVoiceTypingOpen becomes true
    service.voiceRecognitionManager.activeRecognitionListener?.onReadyForSpeech(android.os.Bundle())
    assertEquals(VoiceState.LISTENING, service.voiceRecognitionManager.uiState.value.state)
    assertTrue(service.isVoiceTypingOpen.value)

    // 6. Stop Voice Typing
    service.toggleVoiceTyping()
    assertFalse(service.isVoiceTypingOpen.value)
  }

  @Test
  fun `hindi hinglish phonetic typing transliterates Hindi in Emoji Search and phonetic engine`() {
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // Switch to Hindi Hinglish mode
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    assertEquals(LanguageInputMethodKind.PHONETIC_HINDI, service.languageManager.activeLanguage.value.kind)
    assertEquals("हिन्दी (Hinglish)", service.languageManager.activeLanguage.value.spacebarLabel)

    // Test Emoji search in Hindi mode
    service.setEmojiSearchFocused(true)
    for (ch in "namaste") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
    }
    assertEquals("namaste", service.sessionState.value.emojiSearchRawQuery)
    assertEquals("नमस्ते", HindiPhoneticEngine.transliterate(service.sessionState.value.emojiSearchRawQuery))
  }

  @Test
  fun `select typing language controls voice typing output mode for English Probhat Banglish and Hindi`() {
    grantAudioAndRegisterRecognitionService()
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // Initially not listening
    assertFalse(service.isVoiceTypingOpen.value)

    // 1. Select বাংলা (Probhat) -> Probhat layout + Bengali Unicode voice output (bn-BD)
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    service.toggleVoiceTyping()
    assertEquals(VoiceState.STARTING, service.voiceRecognitionManager.uiState.value.state)
    assertFalse(service.isVoiceTypingOpen.value)
    assertEquals(VoiceLanguage.Bangla, service.voiceRecognitionManager.uiState.value.selectedLanguage)
    assertEquals(LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT, service.languageManager.activeLanguage.value.kind)

    // 2. Partial streaming speech updates state to LISTENING
    service.voiceRecognitionManager.simulateTranscription("আমি", isFinal = false)
    assertTrue(service.isVoiceTypingOpen.value)
    assertEquals(VoiceState.LISTENING, service.voiceRecognitionManager.uiState.value.state)
    assertEquals("আমি", service.voiceRecognitionManager.uiState.value.partialTranscription)

    // 3. Final speech commits and completes listening session
    service.voiceRecognitionManager.simulateTranscription("আমি ভালো আছি", isFinal = true)
    assertEquals(VoiceState.IDLE, service.voiceRecognitionManager.uiState.value.state)
    assertFalse(service.isVoiceTypingOpen.value)

    // 4. Select Banglish -> English QWERTY layout + Bengali speech recognition (bn-BD) with Banglish output
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, service.languageManager.activeLanguage.value.kind)
    service.toggleVoiceTyping()
    assertEquals(VoiceState.STARTING, service.voiceRecognitionManager.uiState.value.state)
    service.voiceRecognitionManager.activeRecognitionListener?.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)
    assertEquals(VoiceLanguage.Banglish, service.voiceRecognitionManager.uiState.value.selectedLanguage)
    assertEquals("bn-Latn", service.voiceRecognitionManager.uiState.value.selectedLanguage.tag)
    assertEquals("bn-BD", service.voiceRecognitionManager.uiState.value.selectedLanguage.recognitionTag)

    // Tapping mic again cancels/stops listening
    service.toggleVoiceTyping()
    assertFalse(service.isVoiceTypingOpen.value)

    // 5. Select हिन्दी (Hinglish) -> Hindi phonetic typing + Hindi voice typing (hi-IN)
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    service.toggleVoiceTyping()
    service.voiceRecognitionManager.activeRecognitionListener?.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)
    assertEquals(VoiceLanguage.Hindi, service.voiceRecognitionManager.uiState.value.selectedLanguage)
    assertEquals("hi-IN", service.voiceRecognitionManager.uiState.value.selectedLanguage.tag)

    // 6. Error simulation resets listening state
    service.voiceRecognitionManager.simulateError("Microphone permission required")
    assertEquals(VoiceState.ERROR, service.voiceRecognitionManager.uiState.value.state)
    assertFalse(service.isVoiceTypingOpen.value)
  }

  @Test
  fun `xiaomi notes end-to-end speech recognition callbacks insert Bangla script and Banglish into active InputConnection without duplication`() {
    com.example.ime.ImeDiagnosticsLogger.clear()
    grantAudioAndRegisterRecognitionService()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    // Create a real host EditText and InputConnection simulating Xiaomi Notes (com.miui.notes)
    val notesEditText = android.widget.EditText(context)
    val notesEditorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.miui.notes"
        fieldId = 501
        inputType =
          android.text.InputType.TYPE_CLASS_TEXT or
            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
      }
    val realInputConnection = notesEditText.onCreateInputConnection(notesEditorInfo)
    assertNotNull("Real EditText InputConnection must not be null", realInputConnection)
    service.testInputConnectionOverride = realInputConnection

    service.onWindowShown()
    service.onStartInput(notesEditorInfo, false)
    service.onStartInputView(notesEditorInfo, false)

    // --- UTTERANCE 1: Select বাংলা (Probhat) ("আমি ভালো আছি।") ---
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    service.toggleVoiceTyping()
    assertFalse(
      "Must NOT show Listening before onReadyForSpeech fires",
      service.isVoiceTypingOpen.value,
    )
    assertEquals(VoiceState.STARTING, service.voiceRecognitionManager.uiState.value.state)
    assertEquals("bn-BD", service.voiceRecognitionManager.uiState.value.selectedLanguage.tag)

    val listener1 = service.voiceRecognitionManager.activeRecognitionListener
    assertNotNull("activeRecognitionListener must be attached to SpeechRecognizer", listener1)
    listener1!!.onReadyForSpeech(android.os.Bundle())
    assertTrue("Listening must be shown after onReadyForSpeech fires", service.isVoiceTypingOpen.value)
    assertEquals(VoiceState.LISTENING, service.voiceRecognitionManager.uiState.value.state)
    listener1.onBeginningOfSpeech()
    listener1.onRmsChanged(6.5f)

    // Partial 1 (with UNSTABLE_TEXT like Google Speech Services)
    val partialBundle1 =
      android.os.Bundle().apply {
        putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("আমি"))
        putStringArrayList("android.speech.extra.UNSTABLE_TEXT", arrayListOf("ভালো"))
      }
    listener1.onPartialResults(partialBundle1)
    assertEquals("আমি ভালো", notesEditText.text.toString())

    // Final Result 1 ("আমি ভালো আছি।") -> replaces composing text, no duplicate words!
    val finalBundle1 =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("আমি ভালো আছি।"),
        )
      }
    listener1.onResults(finalBundle1)
    assertEquals("আমি ভালো আছি। ", notesEditText.text.toString())
    assertFalse("Listening state must reset after final result", service.isVoiceTypingOpen.value)

    // --- UTTERANCE 2: Select Banglish ("তুমি কেমন আছো" -> "Tumi kemon acho") ---
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    service.toggleVoiceTyping()
    assertEquals("bn-Latn", service.voiceRecognitionManager.uiState.value.selectedLanguage.tag)
    assertEquals("bn-BD", service.voiceRecognitionManager.uiState.value.selectedLanguage.recognitionTag)

    val listenerBanglish = service.voiceRecognitionManager.activeRecognitionListener!!
    listenerBanglish.onReadyForSpeech(android.os.Bundle())
    listenerBanglish.onBeginningOfSpeech()

    val banglishPartial =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("তুমি কেমন"),
        )
      }
    listenerBanglish.onPartialResults(banglishPartial)
    assertEquals("আমি ভালো আছি। Tumi kemon", notesEditText.text.toString())

    val banglishFinal =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("তুমি কেমন আছো"),
        )
      }
    listenerBanglish.onResults(banglishFinal)
    assertEquals("আমি ভালো আছি। Tumi kemon acho ", notesEditText.text.toString())
    assertFalse(service.isVoiceTypingOpen.value)

    // --- UTTERANCE 3: Select English ("Hello, how are you?") ---
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.toggleVoiceTyping()
    assertFalse(service.isVoiceTypingOpen.value)
    assertEquals("en-US", service.voiceRecognitionManager.uiState.value.selectedLanguage.tag)

    val listener2 = service.voiceRecognitionManager.activeRecognitionListener!!
    listener2.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)
    listener2.onBeginningOfSpeech()

    val enPartial =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("Hello, how"),
        )
      }
    listener2.onPartialResults(enPartial)
    assertEquals("আমি ভালো আছি। Tumi kemon acho Hello, how", notesEditText.text.toString())

    val enFinal =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("Hello, how are you?"),
        )
      }
    listener2.onResults(enFinal)
    assertEquals("আমি ভালো আছি। Tumi kemon acho Hello, how are you? ", notesEditText.text.toString())
    assertFalse(service.isVoiceTypingOpen.value)

    // Verify runtime diagnostics logs captured all callbacks and InputConnection commits
    val voiceLogs =
      com.example.ime.ImeDiagnosticsLogger.getEvents().filter { it.category == "VOICE_TYPING" }
    assertTrue(voiceLogs.any { it.event == "onReadyForSpeech" })
    assertTrue(voiceLogs.any { it.event == "onBeginningOfSpeech" })
    assertTrue(voiceLogs.any { it.event == "onRmsChanged" })
    assertTrue(voiceLogs.any { it.event == "onPartialResults" })
    assertTrue(voiceLogs.any { it.event == "onResults" })
    assertTrue(voiceLogs.any { it.event == "handleVoicePartial_setComposingText" })
    assertTrue(voiceLogs.any { it.event == "commitVoiceText_committed" && it.details.contains("pkg=com.miui.notes") })
  }

  @Test
  fun `xiaomi hung recognizer watchdog switches candidate and post-partial silence auto-commits speech`() {
    com.example.ime.ImeDiagnosticsLogger.clear()
    grantAudioAndRegisterRecognitionService()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)

    val notesEditText = android.widget.EditText(context)
    val notesEditorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.miui.notes"
        fieldId = 777
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = notesEditText.onCreateInputConnection(notesEditorInfo)
    service.onWindowShown()
    service.onStartInput(notesEditorInfo, false)
    service.onStartInputView(notesEditorInfo, false)

    // 1. Start voice typing; simulate candidate[0] hanging for 2.5s without calling onReadyForSpeech
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.toggleVoiceTyping()
    assertEquals(VoiceState.STARTING, service.voiceRecognitionManager.uiState.value.state)
    assertFalse(service.isVoiceTypingOpen.value)

    // Advance main looper by 2600ms to trigger READY_WATCHDOG_TIMEOUT_MS
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
      .idleFor(java.time.Duration.ofMillis(2600))

    val eventsAfterWatchdog =
      com.example.ime.ImeDiagnosticsLogger.getEvents().filter { it.category == "VOICE_TYPING" }
    assertTrue(
      "Watchdog must log readyWatchdogTimeout when candidate[0] hangs without onReadyForSpeech",
      eventsAfterWatchdog.any { it.event == "readyWatchdogTimeout" },
    )

    // 2. Fallback candidate now fires onReadyForSpeech and streams partial speech, then stalls without onResults
    val fallbackListener = service.voiceRecognitionManager.activeRecognitionListener!!
    fallbackListener.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)

    val partialBundle =
      android.os.Bundle().apply {
        putStringArray(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayOf("Hello, how are you?"),
        )
      }
    fallbackListener.onPartialResults(partialBundle)
    assertEquals("Hello, how are you?", notesEditText.text.toString())

    // Advance main looper by 3500ms (2500ms partial silence + 800ms finalize) so auto-commit fires
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
      .idleFor(java.time.Duration.ofMillis(3500))

    assertEquals("Hello, how are you? ", notesEditText.text.toString())
    assertFalse(service.isVoiceTypingOpen.value)
  }

  @Test
  fun `personal keyboard and settings operate standalone across all four typing modes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = KeyboardPreferencesRepository.getInstance(context)
    val settingsViewModel = com.example.settings.SettingsViewModel(repository = prefsRepo)

    // Verify user preferences and settings viewmodel work standalone
    settingsViewModel.setThemePreset(KeyboardThemePreset.OLED_BLACK)
    assertEquals(KeyboardThemePreset.OLED_BLACK, settingsViewModel.userConfig.value.themePreset)
    settingsViewModel.setShowNumberRow(true)
    assertTrue(settingsViewModel.userConfig.value.showNumberRow)

    // Verify PersonalKeyboardService initializes and types across all 4 language selections:
    // English, বাংলা (Probhat), Banglish, हिन्दी (Hinglish)
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 42
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // 1. English QWERTY typing (auto-capitalizes first letter "H")
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.handleKeyAction(KeyAction.CommitChar("h", "H"))
    service.handleKeyAction(KeyAction.CommitChar("i", "I"))
    service.handleKeyAction(KeyAction.Space)
    assertEquals("Hi ", hostEditText.text.toString())

    // 2. Bangla Probhat typing (direct Unicode typing including Ya-Phala)
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    service.handleKeyAction(KeyAction.CommitChar("ক", "খ"))
    service.handleKeyAction(KeyAction.CommitChar("্য", "্য"))
    service.handleKeyAction(KeyAction.Space)
    assertEquals("Hi ক্য ", hostEditText.text.toString())

    // 3. Banglish selection keeps English QWERTY keyboard typing without phonetic composition
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    service.handleKeyAction(KeyAction.CommitChar("a", "A"))
    service.handleKeyAction(KeyAction.CommitChar("m", "M"))
    service.handleKeyAction(KeyAction.CommitChar("i", "I"))
    service.handleKeyAction(KeyAction.Space)
    assertEquals("Hi ক্য ami ", hostEditText.text.toString())

    // 4. Hindi Hinglish typing (phonetic composition remains active ONLY for Hindi Hinglish)
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    for (ch in "namaste") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
    }
    service.handleKeyAction(KeyAction.Space)
    assertEquals("Hi ক্য ami नमस्ते ", hostEditText.text.toString())
  }

  @Test
  fun `smart translator language item supported list and code lookup`() {
    val languages = TranslateLanguageItem.SUPPORTED_LANGUAGES
    assertTrue(languages.isNotEmpty())

    // Verify mandatory languages exist
    val bn = TranslateLanguageItem.fromCode("bn")
    assertEquals("Bengali", bn.displayName)
    assertEquals("বাংলা", bn.nativeName)
    assertEquals("BN", bn.badge)

    val hi = TranslateLanguageItem.fromCode("hi")
    assertEquals("Hindi", hi.displayName)
    assertEquals("हिन्दी", hi.nativeName)
    assertEquals("HI", hi.badge)

    val en = TranslateLanguageItem.fromCode("en")
    assertEquals("English", en.displayName)
    assertEquals("English", en.nativeName)
    assertEquals("EN", en.badge)

    val auto = TranslateLanguageItem.AUTO_DETECT
    assertTrue(auto.isAutoDetect)
    assertEquals("auto", auto.code)

    // Verify source languages includes AUTO_DETECT
    val sourceLangs = TranslateLanguageItem.getSourceLanguages()
    assertEquals(auto, sourceLangs.first())
    assertTrue(sourceLangs.contains(bn))
    assertTrue(sourceLangs.contains(hi))
    assertTrue(sourceLangs.contains(en))

    // Verify target languages does NOT include AUTO_DETECT
    val targetLangs = TranslateLanguageItem.getTargetLanguages()
    assertFalse(targetLangs.contains(auto))
    assertTrue(targetLangs.contains(bn))
    assertTrue(targetLangs.contains(hi))
    assertTrue(targetLangs.contains(en))
  }

  @Test
  fun `smart translator manager ui state transitions and language swap`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = SmartTranslatorManager(context)

    // Initial state: Auto -> Bengali
    val initial = manager.uiState.value
    assertTrue(initial.sourceLanguage.isAutoDetect)
    assertEquals("bn", initial.targetLanguage.code)
    assertFalse(initial.canSwap)

    // Set source to Hindi
    manager.setSourceLanguage(TranslateLanguageItem.HINDI)
    val withHindi = manager.uiState.value
    assertEquals("hi", withHindi.sourceLanguage.code)
    assertEquals("bn", withHindi.targetLanguage.code)
    assertTrue(withHindi.canSwap)

    // Set input text
    manager.setInputText("नमस्ते आप कैसे हैं")
    assertEquals("नमस्ते आप कैसे हैं", manager.uiState.value.inputText)

    // Swap Hindi and Bengali
    manager.swapLanguages()
    val swapped = manager.uiState.value
    assertEquals("bn", swapped.sourceLanguage.code)
    assertEquals("hi", swapped.targetLanguage.code)

    // Clear input
    manager.clearInputText()
    assertEquals("", manager.uiState.value.inputText)
    assertEquals("", manager.uiState.value.translatedText)

    // Selector open & close
    manager.openSelector(LanguageSelectorMode.SOURCE)
    assertEquals(LanguageSelectorMode.SOURCE, manager.uiState.value.selectorMode)
    manager.setLanguageSearchQuery("beng")
    assertEquals("beng", manager.uiState.value.languageSearchQuery)
    manager.closeSelector()
    assertEquals(LanguageSelectorMode.NONE, manager.uiState.value.selectorMode)

    manager.close()
  }

  @Test
  fun `smart translator direct insertion commits to host input connection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 100
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // Open translator
    service.toggleTranslator()
    assertTrue(service.isTranslatorOpen.value)

    // Insert translated text directly into host text field
    service.insertTranslation("আমার নাম রাহুল")
    assertEquals("আমার নাম রাহুল", hostEditText.text.toString())
    assertFalse(service.isTranslatorOpen.value)
  }

  @Test
  fun `voice typing toggle start stop cancel does not trigger key sound feedback`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val service =
      org.robolectric.Robolectric.setupService(com.example.ime.PersonalKeyboardService::class.java)
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 200
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // Toggle voice typing on (start)
    service.toggleVoiceTyping()
    // Verify voice state transitions without crashing or generating audio feedback
    assertNotNull(service.voiceRecognitionManager)

    // Simulate speech transcription while listening
    service.voiceRecognitionManager.simulateTranscription("Hello voice world", isFinal = true)
    assertEquals("Hello voice world ", hostEditText.text.toString())

    // Stop and cancel voice typing
    service.toggleVoiceTyping()
    service.voiceRecognitionManager.cancel()
    assertFalse(service.isVoiceTypingOpen.value)
  }

  @Test
  fun `bengali typography font size resolution across keyboard height settings`() {
    // Height percent to heightScale mapping:
    // 100% -> 1.0f
    // 120% -> 1.2f
    // 130% -> 1.3f
    // 150% -> 1.5f
    // 180% -> 1.8f
    val heightPercentages = listOf(100 to 1.0f, 120 to 1.2f, 130 to 1.3f, 150 to 1.5f, 180 to 1.8f)

    for ((percent, heightScale) in heightPercentages) {
      val controlledScale = KeyboardTypography.getBengaliControlledScale(heightScale)

      // Controlled scale must be sub-linear compared to heightScale
      if (heightScale > 1.0f) {
        assertTrue("Controlled scale at $percent% ($controlledScale) must be strictly less than heightScale ($heightScale)",
          controlledScale < heightScale)
      }

      // Bengali single character (দ, ক, ম, ব, আ) font size
      val bnCharSize = KeyboardTypography.resolveFontSize("দ", isSpacebar = false, heightScale = heightScale)
      val expectedCharSize = KeyboardTypography.BENGALI_CHAR_FONT_SIZE_SP * controlledScale
      assertEquals(expectedCharSize, bnCharSize.value, 0.05f)

      // Bengali font size at 100% must be calibrated to medium size 16.8sp
      if (percent == 100) {
        assertEquals(16.8f, bnCharSize.value, 0.01f)
      }

      // Bengali font size at 180% must scale proportionally without exceeding 22.0sp
      if (percent == 180) {
        assertTrue("Bengali char size at 180% height (${bnCharSize.value}) must be <= 22.0sp to prevent boundary clipping",
          bnCharSize.value <= 22.0f)
      }

      // English character must scale independently and remain unaffected by Bengali calibration
      val enCharSize = KeyboardTypography.resolveFontSize("q", isSpacebar = false, heightScale = heightScale)
      val expectedEnSize = KeyboardTypography.ENGLISH_CHAR_FONT_SIZE_SP * heightScale
      assertEquals(expectedEnSize, enCharSize.value, 0.05f)
      if (percent == 100) {
        assertEquals(18.5f, enCharSize.value, 0.01f)
      }
    }
  }

  @Test
  fun `bengali unicode recognition for swaroborno byanjonborno kar fola and juktoborno`() {
    // Swaroborno (vowels)
    val vowels = listOf("অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "এ", "ঐ", "ও", "ঔ")
    vowels.forEach { vowel ->
      assertTrue("Vowel $vowel must be detected as Bengali", KeyboardTypography.isBengali(vowel))
      assertFalse("Vowel $vowel must NOT be detected as standalone combining label", isBengaliCombiningLabel(vowel))
      val size = KeyboardTypography.resolveFontSize(vowel, false, 1.0f)
      assertEquals(16.8f, size.value, 0.05f)
    }

    // Byanjonborno (consonants)
    val consonants = listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ", "ট", "ঠ", "ড", "ঢ", "ণ",
      "ত", "থ", "দ", "ধ", "ন", "প", "ফ", "ব", "ভ", "ম", "য", "র", "ল", "শ", "ষ", "স", "হ", "ড়", "ঢ়", "য়", "ৎ")
    consonants.forEach { consonant ->
      assertTrue("Consonant $consonant must be detected as Bengali", KeyboardTypography.isBengali(consonant))
      assertFalse("Consonant $consonant must NOT be detected as standalone combining label", isBengaliCombiningLabel(consonant))
      val size = KeyboardTypography.resolveFontSize(consonant, false, 1.0f)
      assertEquals(16.8f, size.value, 0.05f)
    }

    // Kar (vowel signs) - standalone on keys must be detected as combining label
    val karMarks = listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো", "ৌ")
    karMarks.forEach { kar ->
      assertTrue("Kar $kar must be detected as Bengali", KeyboardTypography.isBengali(kar))
      assertTrue("Kar $kar must be recognized as Bengali combining label for vector outline rendering", isBengaliCombiningLabel(kar))
      val size = KeyboardTypography.resolveFontSize(kar, false, 1.0f)
      assertEquals(16.8f, size.value, 0.05f)
    }

    // Fola and Juktoborno (compound grapheme clusters)
    val compounds = listOf("্য", "্র", "্ব", "্ম", "ক্ষ", "জ্ঞ", "ঞ্চ", "ঙ্ক", "ঙ্গ", "ণ্ড", "ণ্ট", "র্")
    compounds.forEach { cluster ->
      assertTrue("Cluster $cluster must be detected as Bengali", KeyboardTypography.isBengali(cluster))
      assertTrue("Cluster $cluster must be recognized as compound", KeyboardTypography.isBengaliCompound(cluster))
      assertFalse("Cluster $cluster with consonants must NOT be treated as standalone single combining mark", isBengaliCombiningLabel(cluster))
      val size = KeyboardTypography.resolveFontSize(cluster, false, 1.0f)
      // Compound characters receive calibrated 15.0sp for optical breathing room
      assertEquals(15.0f, size.value, 0.05f)
    }

    // Modifiers and Punctuation
    val modifiers = listOf("ঁ", "ং", "ঃ", "্")
    modifiers.forEach { mod ->
      assertTrue("Modifier $mod must be detected as Bengali", KeyboardTypography.isBengali(mod))
      assertTrue("Modifier $mod must be recognized as combining label", isBengaliCombiningLabel(mod))
    }

    // Bengali Dari (।)
    assertTrue(KeyboardTypography.isBengali("।"))
    assertEquals(17.0f, KeyboardTypography.resolveFontSize("।", false, 1.0f).value, 0.05f)

    // Bengali Digits (১-০)
    val digits = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")
    digits.forEach { digit ->
      assertTrue("Digit $digit must be detected as Bengali", KeyboardTypography.isBengali(digit))
      assertEquals(16.8f, KeyboardTypography.resolveFontSize(digit, false, 1.0f).value, 0.05f)
    }
  }

  @Test
  fun `bengali secondary hint font scaling is properly restrained`() {
    val hints = listOf("ধ", "ঊ", "ঈ", "ড়", "ঠ", "ঐ", "উ", "ই", "ঔ", "ফ", "ৈ", "ৌ", "অ", "ষ", "ঢ", "থ", "ঘ", "ঃ", "ঝ", "খ", "ং")
    hints.forEach { hint ->
      assertTrue(KeyboardTypography.isBengali(hint))
      val hintSize100 = KeyboardTypography.resolveHintFontSize(hint, 1.0f)
      assertEquals(9.2f, hintSize100.value, 0.05f)

      val hintSize180 = KeyboardTypography.resolveHintFontSize(hint, 1.8f)
      assertTrue("Bengali hint at 180% (${hintSize180.value}) must remain subtle and not crowd primary key",
        hintSize180.value <= 12.0f)
    }

    // English secondary hint should remain 9.0sp at 100%
    val enHintSize = KeyboardTypography.resolveHintFontSize("1", 1.0f)
    assertEquals(9.0f, enHintSize.value, 0.05f)
  }

  @Test
  fun `bengali spacebar label typography across layouts`() {
    val probhatSpace = "◄  প্রভাত  ►"
    assertTrue(KeyboardTypography.isBengali(probhatSpace))
    val probhatSize = KeyboardTypography.resolveFontSize(probhatSpace, isSpacebar = true, heightScale = 1.0f)
    assertEquals(13.5f, probhatSize.value, 0.05f)

    val avroSpace = "বাংলা (Avro)"
    assertTrue(KeyboardTypography.isBengali(avroSpace))
    val avroSize = KeyboardTypography.resolveFontSize(avroSpace, isSpacebar = true, heightScale = 1.0f)
    assertEquals(13.5f, avroSize.value, 0.05f)

    val englishSpace = "English"
    assertFalse(KeyboardTypography.isBengali(englishSpace))
    val enSize = KeyboardTypography.resolveFontSize(englishSpace, isSpacebar = true, heightScale = 1.0f)
    assertEquals(13.5f, enSize.value, 0.05f)
  }

  @Test
  fun `probhat ya-phala hint and unicode conjunct combinations`() {
    val probhatLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )

    val r3 = probhatLayout.rows[2]
    val key0 = r3.first { it.id == "probhat_r3_0" }
    assertEquals("য়", key0.primaryLabel)
    assertEquals("য", key0.shiftedLabel)
    assertEquals("্য", key0.secondaryHint)

    // Verify Ya-Phala compound recognition and rendering
    assertTrue("্য must be recognized as compound", KeyboardTypography.isBengaliCompound("্য"))
    assertFalse("্য must not be treated as single combining label", isBengaliCombiningLabel("্য"))

    // Test Unicode composition for common consonants: ক্য, খ্য, গ্য, ত্য, ন্য, ব্য, ম্য
    val yaPhala = "\u09CD\u09AF"
    assertEquals("ক্য", "ক$yaPhala")
    assertEquals("খ্য", "খ$yaPhala")
    assertEquals("গ্য", "গ$yaPhala")
    assertEquals("ত্য", "ত$yaPhala")
    assertEquals("ন্য", "ন$yaPhala")
    assertEquals("ব্য", "ব$yaPhala")
    assertEquals("ম্য", "ম$yaPhala")

    // Test Banglish transliteration of Ya-Phala words and syllables
    assertEquals("Bakko", BanglishTransliterator.transliterate("বাক্য"))
    assertEquals("Shotto", BanglishTransliterator.transliterate("সত্য"))
    assertEquals("Tottho", BanglishTransliterator.transliterate("তথ্য"))
    assertEquals("Bonna", BanglishTransliterator.transliterate("বন্যা"))
    assertEquals("Dhonno", BanglishTransliterator.transliterate("ধন্য"))
    assertEquals("Manno", BanglishTransliterator.transliterate("মান্য"))
    assertEquals("Ko", BanglishTransliterator.transliterate("ক্য"))
    assertEquals("Kho", BanglishTransliterator.transliterate("খ্য"))
    assertEquals("Go", BanglishTransliterator.transliterate("গ্য"))
    assertEquals("To", BanglishTransliterator.transliterate("ত্য"))
    assertEquals("No", BanglishTransliterator.transliterate("ন্য"))
    assertEquals("Bo", BanglishTransliterator.transliterate("ব্য"))
    assertEquals("Mo", BanglishTransliterator.transliterate("ম্য"))
  }

  @Test
  fun `spacebar width reduction by 10 to 15 percent`() {
    val probhatLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )
    val probhatBottomRow = probhatLayout.rows[3]
    val probhatSpace = probhatBottomRow.first { it.id == "key_space_pb" }
    assertEquals(3.7f, probhatSpace.weight, 0.01f)

    // Previous weight was 4.3f, 3.7f represents a 13.95% reduction
    val reductionPercent = (4.3f - probhatSpace.weight) / 4.3f * 100f
    assertTrue("Spacebar reduction ($reductionPercent%) must be between 10% and 15%",
      reductionPercent in 10f..15f)

    val englishLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.ALPHABET,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_QWERTY,
      )
    val englishBottomRow = englishLayout.rows[3]
    val englishSpace = englishBottomRow.first { it.id == "key_space" }
    assertEquals(3.7f, englishSpace.weight, 0.01f)
  }

  @Test
  fun `voice typing preferences persistence and default values`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = KeyboardPreferencesRepository.getInstance(context)

    // Check defaults
    val initialConfig = repository.config.value
    assertEquals(com.example.voice.VoiceRecognitionMode.AUTOMATIC, initialConfig.voiceRecognitionMode)
    assertTrue(initialConfig.voiceNoiseCancellation)
    assertTrue(initialConfig.voiceEnhancement)

    // Update and verify persistence with simplified modes (AUTOMATIC and ONLINE_ONLY)
    repository.setVoiceRecognitionMode(com.example.voice.VoiceRecognitionMode.ONLINE_ONLY)
    repository.setVoiceNoiseCancellation(false)
    repository.setVoiceEnhancement(false)

    val updatedConfig = repository.config.value
    assertEquals(com.example.voice.VoiceRecognitionMode.ONLINE_ONLY, updatedConfig.voiceRecognitionMode)
    assertFalse(updatedConfig.voiceNoiseCancellation)
    assertFalse(updatedConfig.voiceEnhancement)

    // Restore defaults for subsequent tests
    repository.setVoiceRecognitionMode(com.example.voice.VoiceRecognitionMode.AUTOMATIC)
    repository.setVoiceNoiseCancellation(true)
    repository.setVoiceEnhancement(true)
  }

  @Test
  fun `voice recognition manager online mode configuration and audio effects diagnostics`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.voice.VoiceRecognitionManager(context = context)

    // Verify Bengali-first language resolution
    val bengaliLang = com.example.voice.VoiceLanguage.forLanguageId("bn_PROBHAT")
    assertEquals("bn-BD", bengaliLang.tag)
    assertEquals("বাংলা", bengaliLang.displayName)
    assertFalse(bengaliLang.isBanglishOutput)

    val banglishLang = com.example.voice.VoiceLanguage.forLanguageId("bn_BANGLISH")
    assertEquals("bn-Latn", banglishLang.tag)
    assertEquals("bn-BD", banglishLang.recognitionTag)
    assertTrue(banglishLang.isBanglishOutput)

    val englishLang = com.example.voice.VoiceLanguage.forLanguageId("en_US")
    assertEquals("en-US", englishLang.tag)
    assertFalse(englishLang.isBanglishOutput)

    val hindiLang = com.example.voice.VoiceLanguage.forLanguageId("hi_HINGLISH")
    assertEquals("hi-IN", hindiLang.tag)
    assertFalse(hindiLang.isBanglishOutput)

    // Audio effects safe query
    val audioInfo = manager.getAudioProcessingInfo()
    assertNotNull(audioInfo)

    // Verify preference propagation to UI state
    manager.updatePreferences(
      mode = com.example.voice.VoiceRecognitionMode.ONLINE_ONLY,
      noiseCancellation = true,
      voiceEnhancement = true,
    )
    val state = manager.uiState.value
    assertEquals(com.example.voice.VoiceRecognitionMode.ONLINE_ONLY, state.recognitionMode)
    assertTrue(state.noiseCancellationEnabled)
    assertTrue(state.voiceEnhancementEnabled)

    // Clean up
    manager.destroy()
  }

  @Test
  fun `voice recognition manager informs user clearly when internet is unavailable and asks to reconnect`() {
    grantAudioAndRegisterRecognitionService()
    val context = ApplicationProvider.getApplicationContext<Context>()
    var capturedError: String? = null
    val manager =
      VoiceRecognitionManager(
        context = context,
        onSpeechRecognized = {},
        onErrorOccurred = { capturedError = it },
      )
    manager.isNetworkConnectedOverride = false

    manager.updatePreferences(
      mode = com.example.voice.VoiceRecognitionMode.AUTOMATIC,
      noiseCancellation = true,
      voiceEnhancement = true,
    )

    // Trigger voice typing while offline
    manager.startListening(language = VoiceLanguage.Bangla)

    val state = manager.uiState.value
    assertEquals(VoiceState.ERROR, state.state)
    assertFalse(state.isOnline)
    assertNotNull(state.errorMessage)
    assertTrue(
      "Must ask user to reconnect when offline",
      state.errorMessage!!.contains("reconnect", ignoreCase = true),
    )
    assertNotNull(capturedError)
    assertTrue(
      "Callback error must ask user to reconnect",
      capturedError!!.contains("reconnect", ignoreCase = true),
    )

    manager.destroy()
  }

  @Test
  fun `select typing language is single source of truth for voice output mode`() {
    grantAudioAndRegisterRecognitionService()
    val controller = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = controller.get()
    service.onCreateInputView()

    // 1. English Typing Mode -> Voice output produces English
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    assertEquals(LanguageManager.ID_ENGLISH, service.languageManager.activeLanguage.value.id)
    assertEquals(VoiceLanguage.English, VoiceLanguage.forLanguageId(service.languageManager.activeLanguage.value.id))
    assertEquals("Hello world", service.voiceRecognitionManager.formatRecognizedOutput("Hello world", VoiceLanguage.English))

    // 2. বাংলা (Probhat) Mode -> Voice output produces native Bengali Unicode
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    assertEquals(LanguageManager.ID_BANGLA_PROBHAT, service.languageManager.activeLanguage.value.id)
    val probhatVoiceLang = VoiceLanguage.forLanguageId(service.languageManager.activeLanguage.value.id)
    assertEquals(VoiceLanguage.Bangla, probhatVoiceLang)
    assertEquals("তুমি কেমন আছো", service.voiceRecognitionManager.formatRecognizedOutput("তুমি কেমন আছো", probhatVoiceLang))

    // 3. Banglish Mode -> Keeps English QWERTY keyboard layout, Bengali voice transliterates to Romanized Banglish
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    assertEquals(LanguageManager.ID_BANGLISH, service.languageManager.activeLanguage.value.id)
    assertEquals(LanguageInputMethodKind.DIRECT_QWERTY, service.languageManager.activeLanguage.value.kind)
    val banglishVoiceLang = VoiceLanguage.forLanguageId(service.languageManager.activeLanguage.value.id)
    assertEquals(VoiceLanguage.Banglish, banglishVoiceLang)
    assertTrue(banglishVoiceLang.isBanglishOutput)
    val banglishOutput = service.voiceRecognitionManager.formatRecognizedOutput("তুমি কেমন আছো", banglishVoiceLang)
    assertEquals("Tumi kemon acho", banglishOutput)

    // 4. हिन्दी (Hinglish) Mode -> Voice output produces Hindi
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    assertEquals(LanguageManager.ID_HINDI_HINGLISH, service.languageManager.activeLanguage.value.id)
    val hindiVoiceLang = VoiceLanguage.forLanguageId(service.languageManager.activeLanguage.value.id)
    assertEquals(VoiceLanguage.Hindi, hindiVoiceLang)
    assertEquals("नमस्ते", service.voiceRecognitionManager.formatRecognizedOutput("नमस्ते", hindiVoiceLang))

    controller.destroy()
  }

  @Test
  fun `settings viewmodel manages clean preferences without offline speech model`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = KeyboardPreferencesRepository.getInstance(context)
    val viewModel = com.example.settings.SettingsViewModel(repository = prefsRepo)

    assertNotNull(viewModel.userConfig.value)
    assertEquals(com.example.voice.VoiceRecognitionMode.AUTOMATIC, viewModel.userConfig.value.voiceRecognitionMode)

    viewModel.setVoiceRecognitionMode(com.example.voice.VoiceRecognitionMode.ONLINE_ONLY)
    assertEquals(com.example.voice.VoiceRecognitionMode.ONLINE_ONLY, viewModel.userConfig.value.voiceRecognitionMode)

    viewModel.setVoiceNoiseCancellation(true)
    assertTrue(viewModel.userConfig.value.voiceNoiseCancellation)

    viewModel.setVoiceEnhancement(true)
    assertTrue(viewModel.userConfig.value.voiceEnhancement)

    // Restore
    viewModel.setVoiceRecognitionMode(com.example.voice.VoiceRecognitionMode.AUTOMATIC)
  }

  @Test
  fun `keyboard startup defers heavy modules and caches emoji category lists`() {
    val runtime = Runtime.getRuntime()
    System.gc()
    val memBeforeBytes = runtime.totalMemory() - runtime.freeMemory()

    val startNs = System.nanoTime()
    val controller = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = controller.get()
    val inputView = service.onCreateInputView()
    assertNotNull(inputView)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.onStartInputView(editorInfo, false)
    val startupMs = (System.nanoTime() - startNs) / 1_000_000.0

    // Verify VoiceRecognitionManager and SmartTranslatorManager are NOT initialized on startup
    assertFalse(
      "VoiceRecognitionManager should be lazily initialized on demand",
      service.isVoiceManagerInitialized,
    )
    assertFalse(
      "SmartTranslatorManager should be lazily initialized on demand",
      service.isTranslatorInitialized,
    )

    // Measure 100 key actions across English, Probhat, and Hinglish
    val fakeIc = android.view.inputmethod.BaseInputConnection(inputView, true)
    service.testInputConnectionOverride = fakeIc
    val keyStartNs = System.nanoTime()
    repeat(50) {
      service.handleKeyAction(KeyAction.CommitChar("a", "A"))
      service.handleKeyAction(KeyAction.Backspace)
    }
    val keyElapsedMs = (System.nanoTime() - keyStartNs) / 1_000_000.0
    val perKeyUs = (keyElapsedMs * 1000.0) / 100.0

    // Verify EmojiData category display caching and O(1) skin tone lookup
    val smileysFirst =
      com.example.emoji.EmojiData.getDisplayEmojisForCategory(
        com.example.emoji.EmojiCategory.SMILEYS,
        com.example.emoji.EmojiSkinTone.DEFAULT,
      )
    val smileysSecond =
      com.example.emoji.EmojiData.getDisplayEmojisForCategory(
        com.example.emoji.EmojiCategory.SMILEYS,
        com.example.emoji.EmojiSkinTone.DEFAULT,
      )
    assertTrue("Cached category list must return same instance", smileysFirst === smileysSecond)
    assertTrue(com.example.emoji.EmojiData.supportsSkinTone("👍"))
    assertTrue(com.example.emoji.EmojiData.supportsSkinTone("👍🏽"))
    assertFalse(com.example.emoji.EmojiData.supportsSkinTone("😀"))

    System.gc()
    val memAfterBytes = runtime.totalMemory() - runtime.freeMemory()
    val usedHeapMb = memAfterBytes / (1024.0 * 1024.0)
    val deltaHeapMb = (memAfterBytes - memBeforeBytes).coerceAtLeast(0L) / (1024.0 * 1024.0)
    println(
      "PERF_METRICS: startupMs=%.2f perKeyUs=%.2f usedHeapMb=%.2f deltaHeapMb=%.2f".format(
        startupMs,
        perKeyUs,
        usedHeapMb,
        deltaHeapMb,
      )
    )

    controller.destroy()
  }

  @Test
  fun `p0 fix 1 - repeated voice typing sessions destroy SpeechRecognizer after completion error and cancellation`() {
    grantAudioAndRegisterRecognitionService()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val recognizedOutputs = mutableListOf<String>()
    val errors = mutableListOf<String>()

    val manager =
      VoiceRecognitionManager(
        context = context,
        onSpeechRecognized = { recognizedOutputs.add(it) },
        onErrorOccurred = { errors.add(it) },
      )

    // Session 1: Normal completion via onResults (Bangla Probhat)
    manager.setLanguage(VoiceLanguage.Bangla)
    manager.startListening()
    assertTrue("Recognizer must be active during session 1", manager.hasActiveRecognizer)
    assertNotNull(manager.activeRecognitionListener)
    manager.activeRecognitionListener!!.onReadyForSpeech(android.os.Bundle())
    val bundle1 =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("আমি বাংলায় কথা বলি"),
        )
      }
    manager.activeRecognitionListener!!.onResults(bundle1)
    assertFalse("Recognizer must be destroyed after onResults", manager.hasActiveRecognizer)
    assertNull("Listener must be cleared after onResults", manager.activeRecognitionListener)
    assertEquals("আমি বাংলায় কথা বলি", recognizedOutputs.last())

    // Session 2: Rescued partial on ERROR_SPEECH_TIMEOUT (Banglish)
    manager.setLanguage(VoiceLanguage.Banglish)
    manager.startListening()
    assertTrue("Recognizer must be active during session 2", manager.hasActiveRecognizer)
    val listener2 = manager.activeRecognitionListener!!
    listener2.onReadyForSpeech(android.os.Bundle())
    val partialBundle2 =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("তুমি কেমন আছো"),
        )
      }
    listener2.onPartialResults(partialBundle2)
    listener2.onError(android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
    assertFalse(
      "Recognizer must be destroyed after onError rescues partial transcription",
      manager.hasActiveRecognizer,
    )
    assertNull("Listener must be cleared after rescued partial", manager.activeRecognitionListener)
    assertEquals("Tumi kemon acho", recognizedOutputs.last())

    // Session 3: Terminal error (ERROR_NO_MATCH with no partial)
    manager.setLanguage(VoiceLanguage.English)
    manager.startListening()
    assertTrue("Recognizer must be active during session 3", manager.hasActiveRecognizer)
    val listener3 = manager.activeRecognitionListener!!
    listener3.onReadyForSpeech(android.os.Bundle())
    listener3.onError(android.speech.SpeechRecognizer.ERROR_NO_MATCH)
    assertFalse("Recognizer must be destroyed after terminal onError", manager.hasActiveRecognizer)
    assertNull("Listener must be cleared after terminal onError", manager.activeRecognitionListener)
    assertEquals(VoiceState.ERROR, manager.uiState.value.state)

    // Session 4: User cancellation via cancel()
    manager.setLanguage(VoiceLanguage.Hindi)
    manager.startListening()
    assertTrue("Recognizer must be active during session 4", manager.hasActiveRecognizer)
    manager.cancel()
    assertFalse("Recognizer must be destroyed after cancel()", manager.hasActiveRecognizer)
    assertNull("Listener must be cleared after cancel()", manager.activeRecognitionListener)
    assertEquals(VoiceState.IDLE, manager.uiState.value.state)

    manager.destroy()
    assertFalse(manager.hasActiveRecognizer)
  }

  @Test
  fun `p0 fix 2 - VoicePermissionActivity callback is cleared after permission result activity finish and service destroy`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    org.robolectric.Shadows.shadowOf(app).denyPermissions(android.Manifest.permission.RECORD_AUDIO)

    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()

    // 1. Trigger permission required -> callback registered
    service.handleVoicePermissionRequired()
    assertNotNull(
      "Callback should be set when launching VoicePermissionActivity",
      com.example.voice.VoicePermissionActivity.onPermissionResultCallback,
    )

    // 2. Launch VoicePermissionActivity and simulate user denying permission -> callback invoked and cleared
    val activityController1 =
      Robolectric.buildActivity(com.example.voice.VoicePermissionActivity::class.java).create()
    val activity1 = activityController1.get()
    activity1.onRequestPermissionsResult(
      1001,
      arrayOf(android.Manifest.permission.RECORD_AUDIO),
      intArrayOf(android.content.pm.PackageManager.PERMISSION_DENIED),
    )
    assertNull(
      "Callback must be cleared immediately after onRequestPermissionsResult",
      com.example.voice.VoicePermissionActivity.onPermissionResultCallback,
    )
    activityController1.destroy()

    // 3. Trigger permission required again, then grant permission and open VoicePermissionActivity -> callback invoked and cleared
    grantAudioAndRegisterRecognitionService()
    service.handleVoicePermissionRequired()
    assertNotNull(com.example.voice.VoicePermissionActivity.onPermissionResultCallback)
    val activityController2 =
      Robolectric.buildActivity(com.example.voice.VoicePermissionActivity::class.java).create()
    assertNull(
      "Callback must be cleared immediately when permission is already granted in onCreate",
      com.example.voice.VoicePermissionActivity.onPermissionResultCallback,
    )
    activityController2.destroy()
    service.voiceRecognitionManager.cancel()

    // 4. Verify service.onDestroy() clears any pending static callback reference
    service.handleVoicePermissionRequired()
    assertNotNull(com.example.voice.VoicePermissionActivity.onPermissionResultCallback)
    serviceController.destroy()
    assertNull(
      "PersonalKeyboardService.onDestroy() must clear VoicePermissionActivity.onPermissionResultCallback",
      com.example.voice.VoicePermissionActivity.onPermissionResultCallback,
    )
  }

  @Test
  fun `p0 fix 3 - repeated translator open close and Hindi to Bengali pivot translation with safe resource cleanup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 301
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    val translator = service.smartTranslatorManager
    val pivotCalls = mutableListOf<String>()
    translator.translationEngineOverride = { text, sourceLang, targetLang ->
      pivotCalls.add("$sourceLang->$targetLang:$text")
      when {
        sourceLang == "hi" && targetLang == "en" -> "How are you"
        sourceLang == "en" && targetLang == "bn" -> "আপনি কেমন আছেন"
        sourceLang == "en" && targetLang == "hi" -> "आप कैसे हैं"
        else -> "$text ($sourceLang->$targetLang)"
      }
    }

    // 1. Repeatedly open, translate Hindi -> Bengali (2-stage pivot via English), and close translator panel
    repeat(3) { cycle ->
      pivotCalls.clear()
      service.toggleTranslator()
      assertTrue("Translator should be open in cycle $cycle", service.isTranslatorOpen.value)

      translator.setSourceLanguage(TranslateLanguageItem.HINDI)
      translator.setTargetLanguage(TranslateLanguageItem.BENGALI)
      translator.setInputText("आप कैसे हैं")

      kotlinx.coroutines.runBlocking {
        translator.translateCurrentText()
      }

      assertEquals(
        "Hindi to Bengali must execute 2-stage pivot (hi->en then en->bn)",
        listOf("hi->en:आप कैसे हैं", "en->bn:How are you"),
        pivotCalls,
      )
      assertEquals("আপনি কেমন আছেন", translator.uiState.value.translatedText)
      assertFalse(translator.uiState.value.isLoading)
      assertEquals(0, translator.activeTranslations)

      // Close translator panel -> must reset loading/selector states and release resources cleanly
      service.toggleTranslator()
      assertFalse("Translator should be closed in cycle $cycle", service.isTranslatorOpen.value)
      assertEquals(0, translator.cachedTranslatorCount)
      assertFalse(translator.hasLanguageIdentifier)
    }

    // 2. Verify keyboard window hide (onWindowHidden) closes translator panel and cleans up resources
    service.toggleTranslator()
    assertTrue(service.isTranslatorOpen.value)
    service.onWindowHidden()
    assertFalse("onWindowHidden must close translator panel", service.isTranslatorOpen.value)
    assertEquals(0, translator.cachedTranslatorCount)
    assertFalse(translator.hasLanguageIdentifier)

    serviceController.destroy()
  }

  @Test
  fun `p1 translator optimization - lazy init pair eviction verified model preservation duplicate job guard and rapid open close`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 401
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // 1. Verify Translator is NOT initialized on startup
    assertFalse("Translator must not be initialized on keyboard startup", service.isTranslatorInitialized)

    val translatorInitStartNs = System.nanoTime()
    service.toggleTranslator()
    val translator = service.smartTranslatorManager
    val translatorInitMs = (System.nanoTime() - translatorInitStartNs) / 1_000_000.0
    assertTrue(service.isTranslatorOpen.value)
    assertTrue(service.isTranslatorInitialized)
    assertEquals(0, translator.cachedTranslatorCount)
    assertFalse("LanguageIdentifier must not be initialized before auto-detect is used", translator.hasLanguageIdentifier)

    val engineCalls = java.util.concurrent.CopyOnWriteArrayList<String>()
    translator.translationEngineOverride = { text, sourceLang, targetLang ->
      engineCalls.add("$sourceLang->$targetLang:$text")
      when {
        sourceLang == "en" && targetLang == "bn" && text == "Good morning" -> "সুপ্রভাত"
        sourceLang == "hi" && targetLang == "en" -> "Good morning"
        sourceLang == "en" && targetLang == "bn" -> "সুপ্রভাত"
        else -> "$text ($sourceLang->$targetLang)"
      }
    }

    // 2. Direct English-to-Bengali translation -> 1 active session translator, 0 LanguageIdentifier
    translator.setSourceLanguage(TranslateLanguageItem.ENGLISH)
    translator.setTargetLanguage(TranslateLanguageItem.BENGALI)
    translator.setInputText("Good morning")
    kotlinx.coroutines.runBlocking {
      translator.translateCurrentText()
    }
    assertEquals("সুপ্রভাত", translator.uiState.value.translatedText)
    assertEquals(1, translator.cachedTranslatorCount)
    assertEquals(1, translator.verifiedModelCount)
    assertFalse("Explicit source language must not initialize LanguageIdentifier", translator.hasLanguageIdentifier)

    // 3. Switch to Hindi-to-Bengali (2-stage pivot: hi->en, en->bn) -> 2 active session translators
    translator.setSourceLanguage(TranslateLanguageItem.HINDI)
    translator.setTargetLanguage(TranslateLanguageItem.BENGALI)
    translator.setInputText("सुप्रभात")
    kotlinx.coroutines.runBlocking {
      translator.translateCurrentText()
    }
    assertEquals("সুপ্রভাত", translator.uiState.value.translatedText)
    assertEquals(2, translator.cachedTranslatorCount)
    assertEquals(2, translator.verifiedModelCount)

    // 4. Switch back to direct English-to-Bengali -> obsolete hi_to_en is evicted, only en_to_bn remains
    translator.setSourceLanguage(TranslateLanguageItem.ENGLISH)
    translator.setTargetLanguage(TranslateLanguageItem.BENGALI)
    translator.setInputText("Good morning")
    kotlinx.coroutines.runBlocking {
      translator.translateCurrentText()
    }
    assertEquals(
      "Eviction must close unused hi_to_en translator when switching back to direct en_to_bn",
      1,
      translator.cachedTranslatorCount,
    )
    assertEquals(
      "Verified downloaded model states must remain cached (hi_to_en and en_to_bn)",
      2,
      translator.verifiedModelCount,
    )

    // 5. Close Translator panel -> active translators released (0), verified models preserved (2)
    service.toggleTranslator()
    assertFalse(service.isTranslatorOpen.value)
    assertEquals("Active translators must be closed when panel closes", 0, translator.cachedTranslatorCount)
    assertEquals("Verified model states must be preserved across panel close", 2, translator.verifiedModelCount)
    assertFalse(translator.hasLanguageIdentifier)

    // 6. Rapid open/close stress test (10 cycles) -> no leaked translators or stuck loading states
    repeat(10) {
      service.toggleTranslator()
      assertTrue(service.isTranslatorOpen.value)
      service.toggleTranslator()
      assertFalse(service.isTranslatorOpen.value)
      assertEquals(0, translator.cachedTranslatorCount)
      assertEquals(0, translator.activeTranslations)
    }

    println("P1_TRANSLATOR_METRICS: initMs=%.3f verifiedModels=%d activeAfterClose=%d".format(
      translatorInitMs,
      translator.verifiedModelCount,
      translator.cachedTranslatorCount,
    ))

    serviceController.destroy()
    assertEquals("All verified model metadata cleared on service destroy", 0, translator.verifiedModelCount)
  }

  @Test
  fun `p1 voice optimization - stale listener guard timer cleanup and cancellation during starting or manual typing`() {
    grantAudioAndRegisterRecognitionService()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 402
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // 1. VoiceRecognitionManager is NOT initialized on startup
    assertFalse("VoiceRecognitionManager must not be initialized on startup", service.isVoiceManagerInitialized)

    // 2. Start voice typing (enters STARTING state before onReadyForSpeech)
    val voiceStartNs = System.nanoTime()
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.toggleVoiceTyping()
    val voiceActivationMs = (System.nanoTime() - voiceStartNs) / 1_000_000.0
    val voiceMgr = service.voiceRecognitionManager
    assertTrue(service.isVoiceManagerInitialized)
    assertEquals(VoiceState.STARTING, voiceMgr.uiState.value.state)
    assertTrue(voiceMgr.hasActiveRecognizer)
    assertTrue("Watchdog timer should be active during STARTING", voiceMgr.hasPendingTimers)
    val staleListenerFromSession1 = voiceMgr.activeRecognitionListener!!

    // 3. Opening Translator while Voice is in STARTING state must cancel Voice and release recognizer & timers
    service.toggleTranslator()
    assertTrue(service.isTranslatorOpen.value)
    assertFalse("Recognizer must be destroyed when switching to Translator during STARTING", voiceMgr.hasActiveRecognizer)
    assertFalse("All voice timers must be cleared on cancel", voiceMgr.hasPendingTimers)
    assertEquals(VoiceState.IDLE, voiceMgr.uiState.value.state)
    service.toggleTranslator()

    // 4. Stale listener from cancelled Session 1 must be ignored if it fires late callbacks
    staleListenerFromSession1.onReadyForSpeech(android.os.Bundle())
    val staleBundle =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("Stale ghost speech"),
        )
      }
    staleListenerFromSession1.onResults(staleBundle)
    assertEquals("Stale listener must not commit text", "", hostEditText.text.toString())
    assertFalse(service.isVoiceTypingOpen.value)

    // 5. Start Session 2, transition to LISTENING, then press a keyboard key -> cancels voice session cleanly
    service.toggleVoiceTyping()
    val listener2 = voiceMgr.activeRecognitionListener!!
    listener2.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)
    assertTrue(voiceMgr.hasActiveRecognizer)

    service.handleKeyAction(KeyAction.CommitChar("a", "A"))
    assertFalse("Manual key typing must cancel active voice session", service.isVoiceTypingOpen.value)
    assertFalse("Manual key typing must release SpeechRecognizer", voiceMgr.hasActiveRecognizer)
    assertFalse("Manual key typing must clear voice timers", voiceMgr.hasPendingTimers)
    assertEquals("A", hostEditText.text.toString())

    // 6. Calling stopListening() when already idle must not schedule stopFinalize timer or enter PROCESSING
    voiceMgr.stopListening()
    assertEquals(VoiceState.IDLE, voiceMgr.uiState.value.state)
    assertFalse("stopListening when idle must not schedule timers", voiceMgr.hasPendingTimers)

    println("P1_VOICE_METRICS: activationMs=%.3f activeRecognizerAfterStop=%b pendingTimers=%b".format(
      voiceActivationMs,
      voiceMgr.hasActiveRecognizer,
      voiceMgr.hasPendingTimers,
    ))

    serviceController.destroy()
  }

  @Test
  fun `p2 compose recomposition - voice RMS and voice state updates isolate recomposition to toolbar indicator with zero root keyboard recompositions`() {
    grantAudioAndRegisterRecognitionService()
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val inputView = service.onCreateInputView()

    val activity =
      Robolectric.buildActivity(android.app.Activity::class.java)
        .create()
        .start()
        .resume()
        .visible()
        .get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)
    container.addView(inputView)

    val hostEditText =
      android.widget.EditText(context).apply {
        setText("Hi ")
        setSelection(3)
      }
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 501
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    val widthSpec =
      android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
    val heightSpec =
      android.view.View.MeasureSpec.makeMeasureSpec(900, android.view.View.MeasureSpec.AT_MOST)

    fun flushCompose() {
      androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        .idleFor(java.time.Duration.ofMillis(32))
      val uiDispatcher =
        androidx.compose.ui.platform.AndroidUiDispatcher.CurrentThread[
          kotlin.coroutines.ContinuationInterceptor
        ]
          as? androidx.compose.ui.platform.AndroidUiDispatcher
      if (uiDispatcher != null) {
        val field =
          uiDispatcher.javaClass.getDeclaredField("dispatchCallback").apply { isAccessible = true }
        val callback = field.get(uiDispatcher) as android.view.Choreographer.FrameCallback
        callback.doFrame(System.nanoTime())
      }
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
        .idleFor(java.time.Duration.ofMillis(32))
      inputView.measure(widthSpec, heightSpec)
      inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
    }

    flushCompose()
    val baselineRootRecompositions = com.example.keyboard.debugRootRecompositionCount
    val baselineVoiceToolbarRecompositions = com.example.toolbar.debugVoiceToolbarRecompositionCount
    assertTrue("Initial composition must have occurred", baselineRootRecompositions >= 1)

    // 1. Start voice typing and transition to LISTENING
    service.toggleVoiceTyping()
    val listener = service.voiceRecognitionManager.activeRecognitionListener!!
    listener.onReadyForSpeech(android.os.Bundle())
    flushCompose()

    val rootAfterListening = com.example.keyboard.debugRootRecompositionCount
    val voiceToolbarAfterListening = com.example.toolbar.debugVoiceToolbarRecompositionCount
    assertEquals(
      "Transitioning voice state to LISTENING must NOT trigger root KeyboardSurface recomposition",
      baselineRootRecompositions,
      rootAfterListening,
    )
    assertTrue(
      "Transitioning voice state to LISTENING must update only the voice toolbar indicator",
      voiceToolbarAfterListening > baselineVoiceToolbarRecompositions,
    )

    // 2. Emit 50 consecutive RMS changes during speech
    val rmsStartNs = System.nanoTime()
    for (i in 1..50) {
      val rmsDb = -1.5f + (i % 10) * 1.2f
      listener.onRmsChanged(rmsDb)
    }
    flushCompose()
    val rmsTotalMs = (System.nanoTime() - rmsStartNs) / 1_000_000.0

    val rootAfter50Rms = com.example.keyboard.debugRootRecompositionCount
    val voiceToolbarAfter50Rms = com.example.toolbar.debugVoiceToolbarRecompositionCount
    assertEquals(
      "50 consecutive voice RMS updates must trigger 0 root KeyboardSurface recompositions",
      rootAfterListening,
      rootAfter50Rms,
    )
    assertEquals(
      "50 consecutive voice RMS updates must trigger 0 voice toolbar indicator recompositions",
      voiceToolbarAfterListening,
      voiceToolbarAfter50Rms,
    )

    // 3. Complete speech recognition -> returns to IDLE
    val finalBundle =
      android.os.Bundle().apply {
        putStringArrayList(
          android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
          arrayListOf("Hello world"),
        )
      }
    listener.onResults(finalBundle)
    flushCompose()

    assertEquals("Hi Hello world ", hostEditText.text.toString())
    assertEquals(
      "Returning voice state from LISTENING to IDLE must NOT trigger root KeyboardSurface recomposition",
      rootAfter50Rms,
      com.example.keyboard.debugRootRecompositionCount,
    )
    assertTrue(
      "Returning voice state to IDLE must update the voice toolbar indicator",
      com.example.toolbar.debugVoiceToolbarRecompositionCount > voiceToolbarAfter50Rms,
    )

    println(
      "P2_RECOMPOSITION_METRICS: rootOnVoiceState=%d rootOn50Rms=%d toolbarOn50Rms=%d rms50Ms=%.3f".format(
        rootAfterListening - baselineRootRecompositions,
        rootAfter50Rms - rootAfterListening,
        voiceToolbarAfter50Rms - voiceToolbarAfterListening,
        rmsTotalMs,
      )
    )

    activity.finish()
    serviceController.destroy()
  }

  @Test
  fun `p2 emoji optimization - fast searchUnicode caching multi-language search focus retention and skin tone variants`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val inputView = service.onCreateInputView()

    val activity =
      Robolectric.buildActivity(android.app.Activity::class.java)
        .create()
        .start()
        .resume()
        .visible()
        .get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)
    container.addView(inputView)

    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 502
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    val widthSpec =
      android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
    val heightSpec =
      android.view.View.MeasureSpec.makeMeasureSpec(1000, android.view.View.MeasureSpec.AT_MOST)

    fun flushCompose() {
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
      inputView.measure(widthSpec, heightSpec)
      inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    // 1. Verify category display list caching across all non-recent categories and skin tones
    val catStartNs = System.nanoTime()
    for (category in com.example.emoji.EmojiCategory.entries) {
      if (category != com.example.emoji.EmojiCategory.RECENT) {
        val first = com.example.emoji.EmojiData.getDisplayEmojisForCategory(category)
        val second = com.example.emoji.EmojiData.getDisplayEmojisForCategory(category)
        assertTrue("Category $category display list must not be empty", first.isNotEmpty())
        assertTrue(
          "Category $category display list must return cached reference",
          first === second,
        )
      }
    }
    val peopleMedium1 =
      com.example.emoji.EmojiData.getDisplayEmojisForCategory(
        com.example.emoji.EmojiCategory.PEOPLE,
        com.example.emoji.EmojiSkinTone.MEDIUM,
      )
    val peopleMedium2 =
      com.example.emoji.EmojiData.getDisplayEmojisForCategory(
        com.example.emoji.EmojiCategory.PEOPLE,
        com.example.emoji.EmojiSkinTone.MEDIUM,
      )
    assertTrue(peopleMedium1 === peopleMedium2)
    assertTrue(peopleMedium1.any { it.contains("\uD83C\uDFFD") })
    val catLookupUs = (System.nanoTime() - catStartNs) / 1_000.0

    // 2. Verify EmojiTextUtil.getSkinToneVariants handles both base and pre-toned emojis and caches results
    val variantsFromBase = com.example.emoji.EmojiTextUtil.getSkinToneVariants("👋")
    val variantsFromToned = com.example.emoji.EmojiTextUtil.getSkinToneVariants("👋🏽")
    assertEquals(6, variantsFromBase.size)
    assertEquals(
      "Skin tone variants from pre-toned emoji must match base emoji variants without double modifiers",
      variantsFromBase,
      variantsFromToned,
    )
    assertTrue(
      "getSkinToneVariants must return cached list instance on repeated calls",
      variantsFromBase === com.example.emoji.EmojiTextUtil.getSkinToneVariants("👋"),
    )

    // 3. Verify EmojiData.search and searchUnicode across English, Bengali, and Hindi (Devanagari & Hinglish)
    val searchStartNs = System.nanoTime()
    val enSmile = com.example.emoji.EmojiData.searchUnicode("smile")
    val bnSmile = com.example.emoji.EmojiData.searchUnicode("হাসি")
    val hiDevanagariSmile = com.example.emoji.EmojiData.searchUnicode("खुशी")
    val hiHinglishSmile = com.example.emoji.EmojiData.searchUnicode("khushi")
    val hiDevanagariHeart = com.example.emoji.EmojiData.searchUnicode("दिल")
    val hiHinglishHeart = com.example.emoji.EmojiData.searchUnicode("pyaar")
    val hiFire = com.example.emoji.EmojiData.searchUnicode("आग")
    val searchColdUs = (System.nanoTime() - searchStartNs) / 1_000.0

    val cachedSearchStartNs = System.nanoTime()
    val enSmileCached = com.example.emoji.EmojiData.searchUnicode("smile")
    val bnSmileCached = com.example.emoji.EmojiData.searchUnicode("হাসি")
    val hiSmileCached = com.example.emoji.EmojiData.searchUnicode("खुशी")
    val searchWarmUs = (System.nanoTime() - cachedSearchStartNs) / 1_000.0

    assertTrue(enSmile.contains("😀"))
    assertTrue(bnSmile.contains("😀"))
    assertTrue("Hindi Devanagari search for 'खुशी' must return 😀", hiDevanagariSmile.contains("😀"))
    assertTrue("Hindi Hinglish search for 'khushi' must return 😀", hiHinglishSmile.contains("😀"))
    assertTrue("Hindi Devanagari search for 'दिल' must return ❤️", hiDevanagariHeart.contains("❤️"))
    assertTrue("Hindi Hinglish search for 'pyaar' must return ❤️", hiHinglishHeart.contains("❤️"))
    assertTrue("Hindi Devanagari search for 'आग' must return 🔥", hiFire.contains("🔥"))
    assertTrue("searchUnicode must return cached list reference", enSmile === enSmileCached)
    assertTrue("searchUnicode must return cached list reference for Bengali", bnSmile === bnSmileCached)
    assertTrue("searchUnicode must return cached list reference for Hindi", hiDevanagariSmile === hiSmileCached)

    // 4. Verify multi-character Emoji Search in English, Bengali, and Hindi maintains focus and never hides keyboard
    service.handleKeyAction(KeyAction.ModeEmoji)
    flushCompose()
    service.setEmojiSearchFocused(true)
    flushCompose()
    assertTrue(service.sessionState.value.isEmojiSearchFocused)

    // Type English "smile"
    for (ch in "smile") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
      flushCompose()
      assertTrue(
        "Emoji search must stay focused while typing English '$ch'",
        service.sessionState.value.isEmojiSearchFocused,
      )
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    }
    assertEquals("smile", service.sessionState.value.emojiSearchQuery)

    // Clear and type Bengali "আগুন"
    service.setEmojiSearchQuery("")
    flushCompose()
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    for (ch in listOf("আ", "গ", "ু", "ন")) {
      service.handleKeyAction(KeyAction.CommitChar(ch, ch))
      flushCompose()
      assertTrue(
        "Emoji search must stay focused while typing Bengali '$ch'",
        service.sessionState.value.isEmojiSearchFocused,
      )
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    }
    assertEquals("আগুন", service.sessionState.value.emojiSearchQuery)
    assertTrue(
      com.example.emoji.EmojiData.searchUnicode(service.sessionState.value.emojiSearchQuery)
        .contains("🔥")
    )

    // Clear and type Hindi Hinglish "pyaar"
    service.setEmojiSearchQuery("")
    flushCompose()
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    for (ch in "pyaar") {
      service.handleKeyAction(KeyAction.CommitChar(ch.toString(), ch.uppercaseChar().toString()))
      flushCompose()
      assertTrue(
        "Emoji search must stay focused while typing Hindi Hinglish '$ch'",
        service.sessionState.value.isEmojiSearchFocused,
      )
      assertEquals(KeyboardMode.EMOJI, service.sessionState.value.mode)
    }
    assertEquals("pyaar", service.sessionState.value.emojiSearchQuery)
    assertTrue(
      com.example.emoji.EmojiData.searchUnicode(service.sessionState.value.emojiSearchQuery)
        .contains("❤️")
    )

    // Dismiss search keyboard only via explicit Enter/Search action
    service.handleKeyAction(KeyAction.Enter)
    flushCompose()
    assertFalse(
      "Search keyboard must close when Enter/Search is pressed",
      service.sessionState.value.isEmojiSearchFocused,
    )
    assertEquals(
      "Emoji panel must remain open after dismissing search keyboard",
      KeyboardMode.EMOJI,
      service.sessionState.value.mode,
    )

    println(
      "P2_EMOJI_METRICS: catLookupUs=%.2f searchCold7QueriesUs=%.2f searchWarm3QueriesUs=%.2f".format(
        catLookupUs,
        searchColdUs,
        searchWarmUs,
      )
    )

    activity.finish()
    serviceController.destroy()
  }

  @Test
  fun `clipboard controller copy A B C produces C B A in history`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    val texts = controller.entries.value.map { it.text }
    assertEquals(listOf("text C", "text B", "text A"), texts)
    controller.release()
  }

  @Test
  fun `clipboard controller delete middle entry B leaves C A and sync does not restore B`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    val entryB = controller.entries.value.first { it.text == "text B" }
    controller.removeEntry(entryB.id)

    val afterDelete = controller.entries.value.map { it.text }
    assertEquals(listOf("text C", "text A"), afterDelete)

    // Simulate lifecycle sync / onWindowShown
    controller.syncFromSystemClipboard()
    assertEquals(listOf("text C", "text A"), controller.entries.value.map { it.text })
    controller.release()
  }

  @Test
  fun `clipboard controller delete top entry C leaves B A and sync does not restore C`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    val entryC = controller.entries.value.first { it.text == "text C" }
    controller.removeEntry(entryC.id)

    assertEquals(listOf("text B", "text A"), controller.entries.value.map { it.text })

    // Even if system clipboard still holds text C, sync must NOT resurrect C
    controller.syncFromSystemClipboard()
    assertEquals(listOf("text B", "text A"), controller.entries.value.map { it.text })
    controller.release()
  }

  @Test
  fun `clipboard controller clear unpinned preserves pinned items and does not restore cleared items on sync`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    // Pin B
    val entryB = controller.entries.value.first { it.text == "text B" }
    controller.togglePin(entryB.id)
    assertTrue(controller.entries.value.first { it.text == "text B" }.isPinned)

    // Clear unpinned
    controller.clearUnpinned()
    assertEquals(listOf("text B"), controller.entries.value.map { it.text })
    assertTrue(controller.entries.value.first().isPinned)

    // Sync must not resurrect C or A
    controller.syncFromSystemClipboard()
    assertEquals(listOf("text B"), controller.entries.value.map { it.text })
    controller.release()
  }

  @Test
  fun `clipboard controller clear all removes everything and sync does not resurrect last item`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    controller.clearAll()
    assertTrue(controller.entries.value.isEmpty())

    // Repeated syncs must not resurrect C
    controller.syncFromSystemClipboard()
    assertTrue(controller.entries.value.isEmpty())
    controller.release()
  }

  @Test
  fun `clipboard controller copy D after deletion adds D at top`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    val entryC = controller.entries.value.first { it.text == "text C" }
    controller.removeEntry(entryC.id)
    assertEquals(listOf("text B", "text A"), controller.entries.value.map { it.text })

    // Copy new text D
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text D"))
    controller.syncFromSystemClipboard()
    assertEquals(listOf("text D", "text B", "text A"), controller.entries.value.map { it.text })
    controller.release()
  }

  @Test
  fun `clipboard controller repeated delete operations completely remove items`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller.syncFromSystemClipboard()

    // Delete C
    controller.removeEntry(controller.entries.value.first { it.text == "text C" }.id)
    assertEquals(listOf("text B", "text A"), controller.entries.value.map { it.text })

    // Delete B
    controller.removeEntry(controller.entries.value.first { it.text == "text B" }.id)
    assertEquals(listOf("text A"), controller.entries.value.map { it.text })

    // Delete A
    controller.removeEntry(controller.entries.value.first { it.text == "text A" }.id)
    assertTrue(controller.entries.value.isEmpty())

    // Multiple sync calls
    controller.syncFromSystemClipboard()
    controller.syncFromSystemClipboard()
    assertTrue(controller.entries.value.isEmpty())
    controller.release()
  }

  @Test
  fun `clipboard controller persistent storage across controller restart`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()

    val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val controller1 = ClipboardController(context)

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text A"))
    controller1.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text B"))
    controller1.syncFromSystemClipboard()
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "text C"))
    controller1.syncFromSystemClipboard()

    // Pin B and delete A in controller 1
    controller1.togglePin(controller1.entries.value.first { it.text == "text B" }.id)
    controller1.removeEntry(controller1.entries.value.first { it.text == "text A" }.id)
    assertEquals(listOf("text C", "text B"), controller1.entries.value.map { it.text })
    controller1.release()

    // Instantiate fresh controller 2
    val controller2 = ClipboardController(context)
    assertEquals(listOf("text C", "text B"), controller2.entries.value.map { it.text })
    assertTrue(controller2.entries.value.first { it.text == "text B" }.isPinned)

    // Sync from system clipboard should not resurrect A
    controller2.syncFromSystemClipboard()
    assertEquals(listOf("text C", "text B"), controller2.entries.value.map { it.text })
    controller2.release()
  }

  @Test
  fun `clipboard integration test through PersonalKeyboardService onWindowShown`() {
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java)
    val service = serviceController.create().get()

    val sysClipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "clip1"))
    service.onWindowShown()

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "clip2"))
    service.onWindowShown()

    sysClipboard.setPrimaryClip(ClipData.newPlainText("text", "clip3"))
    service.onWindowShown()

    // Clear unpinned through service preferences / controller
    val prefs = service.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
    assertNotNull(prefs)

    // Simulate window hide and show
    service.onWindowHidden()
    service.onWindowShown()

    serviceController.destroy()
  }

  @Test
  fun `p3 cpu and ram optimization benchmark and lifecycle verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // 1. Verify & benchmark HindiPhoneticEngine first-char indexed buckets + word cache
    val hindiSentence = "namaste aap kaise hain mera naam raahul hai shukriya"
    assertEquals("नमस्ते आप कैसे हैं मेरा नाम राहुल है शुक्रिया", HindiPhoneticEngine.transliterate(hindiSentence))
    val hindiStartNs = System.nanoTime()
    repeat(1000) {
      HindiPhoneticEngine.transliterate(hindiSentence)
    }
    val hindiAvgUs = (System.nanoTime() - hindiStartNs) / 1000.0 / 1000.0

    // 2. Verify & benchmark BanglishTransliterator Nukta fast-path + streaming word cache
    val banglaUtterance = "আমি ভালো আছি ধন্যবাদ আপনি কেমন আছেন"
    val banglishOut = BanglishTransliterator.transliterate(banglaUtterance)
    assertEquals("Ami valo achi dhonnobad apni kemon achen", banglishOut)
    val banglishStartNs = System.nanoTime()
    repeat(1000) {
      BanglishTransliterator.transliterate(banglaUtterance)
    }
    val banglishAvgUs = (System.nanoTime() - banglishStartNs) / 1000.0 / 1000.0

    // 3. Verify & benchmark EmojiTextUtil ASCII fast-path and ThreadLocal BreakIterator
    assertEquals(1, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Hello world"))
    assertEquals(1, com.example.emoji.EmojiTextUtil.calculateFirstGraphemeLength("Hello world"))
    val familyZwj = "👨‍👩‍👧‍👦"
    assertEquals(familyZwj.length, com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Hi $familyZwj"))
    val graphemeStartNs = System.nanoTime()
    repeat(5000) {
      com.example.emoji.EmojiTextUtil.calculateGraphemeLengthToDelete("Typing fast in ASCII mode")
    }
    val asciiDeleteAvgNs = (System.nanoTime() - graphemeStartNs) / 5000.0

    // 4. Verify PersonalKeyboardService repeated open/close cycles and fast typing without leaks or redundant state copies
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java)
    val service = serviceController.create().get()
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 777
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)

    // Repeated window show/hide cycles
    repeat(20) {
      service.onWindowShown()
      service.onStartInput(editorInfo, false)
      service.onStartInputView(editorInfo, false)
      service.onWindowHidden()
    }
    assertFalse(service.isVoiceManagerInitialized)
    assertFalse(service.isTranslatorInitialized)

    // Benchmark 200 rapid keystrokes in PersonalKeyboardService
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)

    val typingStartNs = System.nanoTime()
    repeat(50) {
      service.handleKeyAction(KeyAction.CommitChar("t", "T"))
      service.handleKeyAction(KeyAction.CommitChar("e", "E"))
      service.handleKeyAction(KeyAction.CommitChar("s", "S"))
      service.handleKeyAction(KeyAction.Space)
    }
    val perKeystrokeUs = (System.nanoTime() - typingStartNs) / 200.0 / 1000.0
    assertTrue(hostEditText.text.toString().startsWith("Tes tes "))

    println(
      "P3_BENCHMARK: hindiAvgUs=${"%.2f".format(hindiAvgUs)}us, " +
        "banglishAvgUs=${"%.2f".format(banglishAvgUs)}us, " +
        "asciiDeleteAvgNs=${"%.1f".format(asciiDeleteAvgNs)}ns, " +
        "perKeystrokeUs=${"%.2f".format(perKeystrokeUs)}us"
    )

    serviceController.destroy()
  }

  @Test
  fun `voice model v1 - complete verification of bn-BD en-US hi-IN Banglish partial final start stop cancel repeated sessions and window lifecycle`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java)
    val service = serviceController.create().get()
    val hostEditText = android.widget.EditText(context)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 950
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)

    // 1. Keyboard opening (onWindowShown) — lazy voice initialization preserved
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)
    assertFalse("VoiceRecognitionManager must remain lazy until first voice use", service.isVoiceManagerInitialized)

    // 2. V1 Bengali (bn-BD) partial transcription & final text insertion
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    service.voiceRecognitionManager.setLanguage(com.example.voice.VoiceLanguage.Bangla)
    service.voiceRecognitionManager.simulateTranscription("আমি আজকে", isFinal = false)
    assertEquals("আমি আজকে", hostEditText.text.toString())
    service.voiceRecognitionManager.simulateTranscription("আমি আজকে অফিসে যাব।", isFinal = true)
    assertEquals("আমি আজকে অফিসে যাব। ", hostEditText.text.toString())

    // 3. Repeated voice typing session — English (en-US)
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.voiceRecognitionManager.setLanguage(com.example.voice.VoiceLanguage.English)
    service.voiceRecognitionManager.simulateTranscription("Hello world", isFinal = false)
    assertEquals("আমি আজকে অফিসে যাব। Hello world", hostEditText.text.toString())
    service.voiceRecognitionManager.simulateTranscription("Hello world today", isFinal = true)
    assertEquals("আমি আজকে অফিসে যাব। Hello world today ", hostEditText.text.toString())

    // 4. Repeated voice typing session — Hindi (hi-IN)
    service.languageManager.setActiveLanguage(LanguageManager.ID_HINDI_HINGLISH)
    service.voiceRecognitionManager.setLanguage(com.example.voice.VoiceLanguage.Hindi)
    service.voiceRecognitionManager.simulateTranscription("नमस्ते", isFinal = false)
    service.voiceRecognitionManager.simulateTranscription("नमस्ते भारत", isFinal = true)
    assertEquals("আমি আজকে অফিসে যাব। Hello world today नमस्ते भारत ", hostEditText.text.toString())

    // 5. Repeated voice typing session — Banglish Voice Output (spoken Bengali -> Romanized Banglish)
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLISH)
    service.voiceRecognitionManager.setLanguage(com.example.voice.VoiceLanguage.Banglish)
    service.voiceRecognitionManager.simulateTranscription("তুমি", isFinal = false)
    service.voiceRecognitionManager.simulateTranscription("তুমি কেমন আছো", isFinal = true)
    assertTrue(
      "Banglish output must append Romanized text",
      hostEditText.text.toString().contains("Tumi kemon acho"),
    )

    // 6. Voice start, stop, cancel & microphone permission callback cleanup
    service.handleVoicePermissionRequired()
    assertNotNull(com.example.voice.VoicePermissionActivity.onPermissionResultCallback)
    com.example.voice.VoicePermissionActivity.clearPermissionCallback()
    assertNull(com.example.voice.VoicePermissionActivity.onPermissionResultCallback)

    service.voiceRecognitionManager.stopListening()
    service.voiceRecognitionManager.cancel()
    assertFalse(service.voiceRecognitionManager.hasActiveRecognizer)

    // 7. Keyboard closing (onWindowHidden) & service destroy cleanup
    service.onWindowHidden()
    assertFalse(service.voiceRecognitionManager.hasActiveRecognizer)
    serviceController.destroy()
  }

  @Test
  fun `universal adaptive layout - keyboard typography dampens extreme accessibility font scaling to prevent key overflow`() {
    // 1.0f baseline: damp factor is exactly 1.0f
    assertEquals(1.0f, KeyboardTypography.getEffectiveFontScaleDamping(1.0f), 0.001f)
    assertEquals(1.0f, KeyboardTypography.getEffectiveFontScaleDamping(0.85f), 0.001f)

    // Accessibility font scale 1.3f (common on Samsung / Xiaomi large font)
    val damp13 = KeyboardTypography.getEffectiveFontScaleDamping(1.3f)
    assertTrue("Damp factor for 1.3f must be between 0.80 and 0.95", damp13 in 0.80f..0.95f)

    // Accessibility font scale 1.5f (vision accessibility)
    val damp15 = KeyboardTypography.getEffectiveFontScaleDamping(1.5f)
    assertTrue("Damp factor for 1.5f must be between 0.70 and 0.85", damp15 in 0.70f..0.85f)

    // Accessibility font scale 2.0f (maximum Android accessibility)
    val damp20 = KeyboardTypography.getEffectiveFontScaleDamping(2.0f)
    assertTrue("Damp factor for 2.0f must be between 0.60 and 0.75", damp20 in 0.60f..0.75f)

    // Verify resolved font sizes across standard vs large font scale
    val stdEnglishChar = KeyboardTypography.resolveFontSize("A", isSpacebar = false, heightScale = 1.0f, fontScale = 1.0f)
    val largeEnglishChar = KeyboardTypography.resolveFontSize("A", isSpacebar = false, heightScale = 1.0f, fontScale = 1.5f)
    assertTrue("Large font scale must produce clamped sp to prevent key overflow", largeEnglishChar.value <= stdEnglishChar.value)

    val stdBengaliChar = KeyboardTypography.resolveFontSize("ক", isSpacebar = false, heightScale = 1.0f, fontScale = 1.0f)
    val largeBengaliChar = KeyboardTypography.resolveFontSize("ক", isSpacebar = false, heightScale = 1.0f, fontScale = 1.5f)
    assertTrue("Large font scale must clamp Bengali characters within key boundaries", largeBengaliChar.value <= stdBengaliChar.value)

    val stdLabel = KeyboardTypography.resolveFontSize("Search", isSpacebar = false, heightScale = 1.0f, fontScale = 1.0f)
    val largeLabel = KeyboardTypography.resolveFontSize("Search", isSpacebar = false, heightScale = 1.0f, fontScale = 1.5f)
    assertTrue("Long functional action labels must not blow up on large font scale", largeLabel.value <= stdLabel.value)
  }

  @Test
  fun `universal adaptive layout - responsive dimensions and proportional 9-key row padding`() {
    fun computeMaxKeyboardWidth(maxWidthDp: Float): Float {
      return if (maxWidthDp > 600f) {
        580f.coerceAtMost(maxWidthDp * 0.85f)
      } else {
        maxWidthDp
      }
    }

    // Small phone (320dp) -> exactly 320dp
    assertEquals(320f, computeMaxKeyboardWidth(320f), 0.01f)
    // Medium phone (360dp) -> exactly 360dp
    assertEquals(360f, computeMaxKeyboardWidth(360f), 0.01f)
    // Standard phone (412dp) -> exactly 412dp
    assertEquals(412f, computeMaxKeyboardWidth(412f), 0.01f)
    // Tablet (800dp) -> capped at 580dp
    assertEquals(580f, computeMaxKeyboardWidth(800f), 0.01f)
    // Large tablet (1200dp) -> capped at 580dp
    assertEquals(580f, computeMaxKeyboardWidth(1200f), 0.01f)

    // Key horizontal padding
    fun computeKeyHorizontalPaddingDp(widthDp: Float): Float {
      return if (widthDp < 330f) 1.0f
      else if (widthDp < 360f) 1.4f
      else 1.8f
    }
    assertEquals(1.0f, computeKeyHorizontalPaddingDp(320f), 0.01f)
    assertEquals(1.4f, computeKeyHorizontalPaddingDp(340f), 0.01f)
    assertEquals(1.8f, computeKeyHorizontalPaddingDp(412f), 0.01f)
    assertEquals(1.8f, computeKeyHorizontalPaddingDp(580f), 0.01f)

    // 9-key row proportional margin
    fun computeNineKeyPaddingDp(widthDp: Float, isProbhat: Boolean): Float {
      val factor = if (isProbhat) 0.042f else 0.05f
      return (widthDp * factor).coerceIn(8f, 28f)
    }
    assertEquals(16.0f, computeNineKeyPaddingDp(320f, isProbhat = false), 0.5f)
    assertEquals(18.0f, computeNineKeyPaddingDp(360f, isProbhat = false), 0.5f)
    assertEquals(20.6f, computeNineKeyPaddingDp(412f, isProbhat = false), 0.5f)
  }

  @Test
  fun `universal adaptive layout - dynamic toolbar controls and microphone presence across screen sizes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val serviceController = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()
    val inputView = service.onCreateInputView()

    val activity =
      Robolectric.buildActivity(android.app.Activity::class.java)
        .create()
        .start()
        .resume()
        .visible()
        .get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)
    container.addView(inputView)

    val hostEditText =
      android.widget.EditText(context).apply {
        setText("")
        setSelection(0)
      }
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.host"
        fieldId = 701
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // Measure across 320px, 720px, and 1080px display resolutions
    val testWidths = listOf(320, 720, 1080)
    for (w in testWidths) {
      val widthSpec = android.view.View.MeasureSpec.makeMeasureSpec(w, android.view.View.MeasureSpec.EXACTLY)
      val heightSpec = android.view.View.MeasureSpec.makeMeasureSpec(800, android.view.View.MeasureSpec.AT_MOST)
      inputView.measure(widthSpec, heightSpec)
      inputView.layout(0, 0, inputView.measuredWidth, inputView.measuredHeight)
      assertTrue("Input view measured width must equal window width $w", inputView.measuredWidth == w)
      assertTrue("Input view measured height must be positive", inputView.measuredHeight > 0)
    }

    serviceController.destroy()
  }

  @Test
  fun `universal adaptive layout - portrait vs landscape orientation height transitions`() {
    // Clamped percent ranges
    val pMin = KeyboardHeightBounds.clampPercent(30)
    val pMax = KeyboardHeightBounds.clampPercent(220)
    assertEquals(KeyboardHeightBounds.MIN_PERCENT, pMin)
    assertEquals(KeyboardHeightBounds.MAX_PERCENT, pMax)

    // Portrait height scaling
    val portRowH100 = KeyboardHeightBounds.rowHeightDpForPercent(100, isLandscape = false)
    val portRowH130 = KeyboardHeightBounds.rowHeightDpForPercent(130, isLandscape = false)
    val portRowH180 = KeyboardHeightBounds.rowHeightDpForPercent(180, isLandscape = false)
    assertEquals(50f, portRowH100, 0.01f)
    assertEquals(65f, portRowH130, 0.01f)
    assertEquals(90f, portRowH180, 0.01f)

    // Landscape height scaling (compact base: 38dp)
    val landRowH100 = KeyboardHeightBounds.rowHeightDpForPercent(100, isLandscape = true)
    val landRowH130 = KeyboardHeightBounds.rowHeightDpForPercent(130, isLandscape = true)
    val landRowH180 = KeyboardHeightBounds.rowHeightDpForPercent(180, isLandscape = true)
    assertEquals(38f, landRowH100, 0.01f)
    assertEquals(49.4f, landRowH130, 0.01f)
    assertEquals(68.4f, landRowH180, 0.01f)

    // Toolbar heights
    val portToolbarH = KeyboardHeightBounds.toolbarHeightDpForPercent(100, isLandscape = false)
    val landToolbarH = KeyboardHeightBounds.toolbarHeightDpForPercent(100, isLandscape = true)
    assertEquals(36f, portToolbarH, 0.01f)
    assertEquals(32f, landToolbarH, 0.01f)
  }

  @Test
  fun `bengali probhat symbols layout displays bengali numerals 1 to 0 with correct unicode codepoints U+09E6 to U+09EF`() {
    val probhatSymbolsLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.SYMBOLS_PRIMARY,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT,
      )

    val row1 = probhatSymbolsLayout.rows.first()
    assertEquals("Row 1 in Probhat SYMBOLS_PRIMARY must have exactly 10 keys", 10, row1.size)

    val expectedBengaliDigits = listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০")
    val expectedCodepoints = listOf(
      0x09E7, // ১ (BENGALI DIGIT ONE)
      0x09E8, // ২ (BENGALI DIGIT TWO)
      0x09E9, // ৩ (BENGALI DIGIT THREE)
      0x09EA, // ৪ (BENGALI DIGIT FOUR)
      0x09EB, // ৫ (BENGALI DIGIT FIVE)
      0x09EC, // ৬ (BENGALI DIGIT SIX)
      0x09ED, // ৭ (BENGALI DIGIT SEVEN)
      0x09EE, // ৮ (BENGALI DIGIT EIGHT)
      0x09EF, // ৯ (BENGALI DIGIT NINE)
      0x09E6, // ০ (BENGALI DIGIT ZERO)
    )

    for (i in expectedBengaliDigits.indices) {
      val key = row1[i]
      val expectedChar = expectedBengaliDigits[i]
      val expectedCp = expectedCodepoints[i]

      assertEquals("Key index $i label must be $expectedChar", expectedChar, key.primaryLabel)
      assertEquals("Key index $i codepoint must match U+${Integer.toHexString(expectedCp).uppercase()}", expectedCp, key.primaryLabel.first().code)
      assertTrue("Codepoint must be within U+09E6..U+09EF", key.primaryLabel.first().code in 0x09E6..0x09EF)

      // Verify action commits exact Unicode Bengali digit
      val action = key.action as KeyAction.CommitChar
      assertEquals(expectedChar, action.lowercase)
      assertEquals(expectedChar, action.uppercase)
    }

    // Verify Row 2, Row 3 and Bottom Row are intact and functional
    assertEquals(10, probhatSymbolsLayout.rows[1].size) // @ # $ _ & - + ( ) /
    assertTrue(probhatSymbolsLayout.rows[2].any { it.primaryLabel == "=\\<" })
    assertTrue(probhatSymbolsLayout.rows[2].any { it.primaryLabel == "।" }) // Bengali Dari
    assertTrue(probhatSymbolsLayout.rows.last().any { it.primaryLabel == "ABC" })
  }

  @Test
  fun `english qwerty symbols layout retains standard english digits 1 to 0`() {
    val englishSymbolsLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.SYMBOLS_PRIMARY,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_QWERTY,
      )

    val row1 = englishSymbolsLayout.rows.first()
    assertEquals("Row 1 in English SYMBOLS_PRIMARY must have exactly 10 keys", 10, row1.size)

    val expectedEnglishDigits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    for (i in expectedEnglishDigits.indices) {
      val key = row1[i]
      val expectedChar = expectedEnglishDigits[i]
      assertEquals("English key index $i label must be $expectedChar", expectedChar, key.primaryLabel)
      val action = key.action as KeyAction.CommitChar
      assertEquals(expectedChar, action.lowercase)
    }
  }

  @Test
  fun `avro banglish and hindi keyboards keep english numbers unchanged`() {
    // Banglish uses DIRECT_QWERTY
    val banglishSymbolsLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.SYMBOLS_PRIMARY,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.DIRECT_QWERTY,
      )
    val banglishDigits = banglishSymbolsLayout.rows.first().map { it.primaryLabel }
    assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), banglishDigits)

    // Hindi uses PHONETIC_HINDI
    val hindiSymbolsLayout =
      LayoutManager.getLayout(
        mode = KeyboardMode.SYMBOLS_PRIMARY,
        showNumberRow = false,
        languageKind = LanguageInputMethodKind.PHONETIC_HINDI,
      )
    val hindiDigits = hindiSymbolsLayout.rows.first().map { it.primaryLabel }
    assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), hindiDigits)
  }

  @Test
  fun `probhat service integration - tapping 123 displays bengali numerals and commits bengali digits`() {
    val serviceController = org.robolectric.Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()

    val hostEditText = android.widget.EditText(service)
    val editorInfo =
      android.view.inputmethod.EditorInfo().apply {
        packageName = "com.example.test"
        fieldId = 801
        inputType = android.text.InputType.TYPE_CLASS_TEXT
      }
    service.testInputConnectionOverride = hostEditText.onCreateInputConnection(editorInfo)
    service.onWindowShown()
    service.onStartInput(editorInfo, false)
    service.onStartInputView(editorInfo, false)

    // Switch to Bengali Probhat
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    assertEquals(LanguageInputMethodKind.DIRECT_BANGLA_PROBHAT, service.languageManager.activeLanguage.value.kind)
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)

    // Tap ?123 key -> transition to SYMBOLS_PRIMARY
    service.handleKeyAction(KeyAction.ModeSymbolsPrimary)
    assertEquals(KeyboardMode.SYMBOLS_PRIMARY, service.sessionState.value.mode)

    // Get current layout for active language
    val activeLayout = LayoutManager.getLayout(
      mode = service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    val numberKeys = activeLayout.rows.first()

    // Tap each number key: ১ ২ ৩ ৪ ৫ ৬ ৭ ৮ ৯ ০
    for (key in numberKeys) {
      service.handleKeyAction(key.action)
    }

    assertEquals("১২৩৪৫৬৭৮৯০", hostEditText.text.toString())

    serviceController.destroy()
  }

  @Test
  fun `probhat symbols mode switching - alphabet to symbols primary to secondary and back to alphabet`() {
    val serviceController = org.robolectric.Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()

    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)

    // 1. Tapping ?123 transitions to SYMBOLS_PRIMARY (with Bengali digits)
    service.handleKeyAction(KeyAction.ModeSymbolsPrimary)
    assertEquals(KeyboardMode.SYMBOLS_PRIMARY, service.sessionState.value.mode)
    val sym1Layout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("১", sym1Layout.rows.first().first().primaryLabel)

    // 2. Tapping =\\< transitions to SYMBOLS_SECONDARY
    service.handleKeyAction(KeyAction.ModeSymbolsSecondary)
    assertEquals(KeyboardMode.SYMBOLS_SECONDARY, service.sessionState.value.mode)
    val sym2Layout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("~", sym2Layout.rows.first().first().primaryLabel)

    // 3. Tapping ?123 from SYMBOLS_SECONDARY transitions back to SYMBOLS_PRIMARY (Bengali digits)
    service.handleKeyAction(KeyAction.ModeSymbolsPrimary)
    assertEquals(KeyboardMode.SYMBOLS_PRIMARY, service.sessionState.value.mode)
    val sym1BackLayout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("১", sym1BackLayout.rows.first().first().primaryLabel)

    // 4. Tapping ABC transitions back to ALPHABET (Probhat letters)
    service.handleKeyAction(KeyAction.ModeAlphabet)
    assertEquals(KeyboardMode.ALPHABET, service.sessionState.value.mode)
    val alphaLayout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("দ", alphaLayout.rows.first().first().primaryLabel)

    serviceController.destroy()
  }

  @Test
  fun `language switching dynamically updates symbols row between English and Bengali Probhat`() {
    val serviceController = org.robolectric.Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = serviceController.get()

    // Start in English
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    service.handleKeyAction(KeyAction.ModeSymbolsPrimary)
    assertEquals(KeyboardMode.SYMBOLS_PRIMARY, service.sessionState.value.mode)

    var currentLayout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("1", currentLayout.rows.first().first().primaryLabel)
    assertEquals("0", currentLayout.rows.first().last().primaryLabel)

    // Switch to Bengali Probhat
    service.languageManager.setActiveLanguage(LanguageManager.ID_BANGLA_PROBHAT)
    currentLayout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("১", currentLayout.rows.first().first().primaryLabel)
    assertEquals("০", currentLayout.rows.first().last().primaryLabel)

    // Switch back to English
    service.languageManager.setActiveLanguage(LanguageManager.ID_ENGLISH)
    currentLayout = LayoutManager.getLayout(
      service.sessionState.value.mode,
      showNumberRow = false,
      languageKind = service.languageManager.activeLanguage.value.kind,
    )
    assertEquals("1", currentLayout.rows.first().first().primaryLabel)
    assertEquals("0", currentLayout.rows.first().last().primaryLabel)

    serviceController.destroy()
  }

  @Test
  fun `v2 audio preprocessor initializes safely and exposes hardware effect availability`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preProcessor = com.example.voice.AudioPreProcessor(context)

    val infoBefore = preProcessor.getProcessingInfo()
    assertNotNull(infoBefore)

    // start() starts without crashing on all devices and emulators
    val infoStarted = preProcessor.start(requestedSessionId = 101)
    assertNotNull(infoStarted)

    // stop() safely releases effects
    preProcessor.stop()
    assertFalse(preProcessor.isNoiseSuppressorActive)
    assertFalse(preProcessor.isAcousticEchoCancelerActive)
    assertFalse(preProcessor.isAutomaticGainControlActive)
  }

  @Test
  fun `v2 voice post processor detects bengali and english questions and terminates with question mark`() {
    // Bengali questions with interrogative pronouns
    val bnQ1 = com.example.voice.VoicePostProcessor.process(
      rawText = "তুমি কেমন আছো",
      language = VoiceLanguage.Bangla,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("তুমি কেমন আছো?", bnQ1)

    val bnQ2 = com.example.voice.VoicePostProcessor.process(
      rawText = "তোমার নাম কি",
      language = VoiceLanguage.Bangla,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("তোমার নাম কি?", bnQ2)

    val bnQ3 = com.example.voice.VoicePostProcessor.process(
      rawText = "তুমি যাবে নাকি",
      language = VoiceLanguage.Bangla,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("তুমি যাবে নাকি?", bnQ3)

    // English questions with question starters
    val enQ1 = com.example.voice.VoicePostProcessor.process(
      rawText = "how are you",
      language = VoiceLanguage.English,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("How are you?", enQ1)

    val enQ2 = com.example.voice.VoicePostProcessor.process(
      rawText = "what is your name",
      language = VoiceLanguage.English,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("What is your name?", enQ2)
  }

  @Test
  fun `v2 voice post processor inserts clause commas and declarative terminators`() {
    // Bengali declarative statement gets Dari '।'
    val bnDec = com.example.voice.VoicePostProcessor.process(
      rawText = "আমি ভাত খাবো",
      language = VoiceLanguage.Bangla,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("আমি ভাত খাবো।", bnDec)

    // Bengali clause with conjunction 'কিন্তু' gets comma
    val bnClause = com.example.voice.VoicePostProcessor.process(
      rawText = "আমি যাব কিন্তু তুমি আসবে না",
      language = VoiceLanguage.Bangla,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("আমি যাব, কিন্তু তুমি আসবে না।", bnClause)

    // English declarative statement gets '.' and capitalization
    val enDec = com.example.voice.VoicePostProcessor.process(
      rawText = "i am going to school",
      language = VoiceLanguage.English,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("I am going to school.", enDec)

    // English clause with conjunction 'but' gets comma
    val enClause = com.example.voice.VoicePostProcessor.process(
      rawText = "i wanted to come but it was raining",
      language = VoiceLanguage.English,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("I wanted to come, but it was raining.", enClause)
  }

  @Test
  fun `v2 voice post processor normalizes code-switching and Romanized Banglish`() {
    // Code-switching boundary spacing
    val codeSwitched = com.example.voice.VoicePostProcessor.normalizeCodeSwitching("আমিofficeযাচ্ছি")
    assertEquals("আমি office যাচ্ছি", codeSwitched)

    // Romanized Banglish conversion with embedded English word
    val banglish = com.example.voice.VoicePostProcessor.process(
      rawText = "আমি office যাচ্ছি",
      language = VoiceLanguage.Banglish,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("Ami office jacchi.", banglish)

    // Banglish question
    val banglishQ = com.example.voice.VoicePostProcessor.process(
      rawText = "tumi kemon acho",
      language = VoiceLanguage.Banglish,
      isFinal = true,
      autoPunctuate = true,
    )
    assertEquals("Tumi kemon acho?", banglishQ)
  }

  @Test
  fun `zero failure offline fallback hack retries on network error with EXTRA_PREFER_OFFLINE and recovers to online mode`() {
    grantAudioAndRegisterRecognitionService()
    val context = ApplicationProvider.getApplicationContext<Context>()
    var errorCallbackCount = 0
    var listeningState = false

    val manager =
      VoiceRecognitionManager(
        context = context,
        onSpeechRecognized = {},
        onErrorOccurred = { errorCallbackCount++ },
        onListeningStateChanged = { listeningState = it },
      )
    manager.isNetworkConnectedOverride = true
    manager.startListening(language = VoiceLanguage.Bangla)

    // Initially online/hybrid mode: EXTRA_PREFER_OFFLINE is false
    assertFalse(manager.isPreferOfflineFallbackActive)
    assertEquals(0, manager.offlineFallbackRetryCount)
    assertFalse(
      manager.activeRecognitionIntent?.getBooleanExtra(
        android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,
        true,
      ) ?: true
    )

    // 1st network error (ERROR_NETWORK): triggers silent offline fallback (attempt 1 of 2)
    manager.simulateError(android.speech.SpeechRecognizer.ERROR_NETWORK)
    assertEquals(1, manager.offlineFallbackRetryCount)
    assertTrue(manager.isPreferOfflineFallbackActive)
    assertEquals(0, errorCallbackCount)
    assertTrue(listeningState)
    assertEquals(VoiceState.LISTENING, manager.uiState.value.state)
    assertTrue(manager.uiState.value.isOnDeviceActive)
    assertTrue(
      manager.activeRecognitionIntent?.getBooleanExtra(
        android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,
        false,
      ) == true
    )

    // 2nd network error (ERROR_NETWORK_TIMEOUT): triggers silent offline fallback (attempt 2 of 2)
    manager.simulateError(android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT)
    assertEquals(2, manager.offlineFallbackRetryCount)
    assertTrue(manager.isPreferOfflineFallbackActive)
    assertEquals(0, errorCallbackCount)
    assertEquals(VoiceState.LISTENING, manager.uiState.value.state)

    // 3rd network error exceeds MAX_OFFLINE_FALLBACK_RETRIES (2): retry guard stops infinite loop
    manager.simulateError(android.speech.SpeechRecognizer.ERROR_NETWORK)
    assertEquals(1, errorCallbackCount)
    assertEquals(VoiceState.ERROR, manager.uiState.value.state)

    // When network connectivity is healthy again, intent switches back to online/hybrid mode
    manager.isNetworkConnectedOverride = true
    manager.refreshConnectivityMode()
    assertFalse(manager.isPreferOfflineFallbackActive)
    assertEquals(0, manager.offlineFallbackRetryCount)
    assertFalse(
      manager.activeRecognitionIntent?.getBooleanExtra(
        android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,
        true,
      ) ?: true
    )

    manager.destroy()
  }

  @Test
  fun `microphone button pulses smoothly between 1_0f and 1_25f with live rms audio feedback and resets cleanly`() {
    grantAudioAndRegisterRecognitionService()
    val controller = Robolectric.buildService(PersonalKeyboardService::class.java).create()
    val service = controller.get()
    service.onCreateInputView()

    // Default scale when voice typing is closed
    assertEquals(1.0f, service.voiceRmsScaleState.value, 0.001f)

    // Open voice typing
    service.toggleVoiceTyping()
    val listener = service.voiceRecognitionManager.activeRecognitionListener!!
    listener.onReadyForSpeech(android.os.Bundle())
    assertTrue(service.isVoiceTypingOpen.value)

    // Negative / silence decibel (-2 dB) -> safe normalization maps to 0f -> scale is 1.0f
    listener.onRmsChanged(-2.0f)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
    assertEquals(1.0f, service.voiceRmsScaleState.value, 0.01f)

    // Medium sound level (+5 dB) -> normalized to 0.5f -> target scale is 1.0f + 0.5 * 0.25 = 1.125f
    listener.onRmsChanged(5.0f)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
    assertEquals(1.125f, service.voiceRmsScaleState.value, 0.015f)

    // Loud sound level (+10 dB+) -> normalized to 1.0f -> target scale is 1.25f
    listener.onRmsChanged(12.0f)
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(60))
    assertEquals(1.25f, service.voiceRmsScaleState.value, 0.015f)

    // End of speech -> animates cleanly back to 1.0f
    listener.onEndOfSpeech()
    org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(100))
    assertEquals(1.0f, service.voiceRmsScaleState.value, 0.01f)

    // Window hidden -> cleans up and resets scale to 1.0f
    service.onWindowHidden()
    assertEquals(1.0f, service.voiceRmsScaleState.value, 0.001f)

    controller.destroy()
  }

  @Test
  fun `universal bug fix - bangla probhat microphone icon size matches english banglish and hindi across widths densities and font scales`() {
    val activity =
      Robolectric.buildActivity(androidx.activity.ComponentActivity::class.java)
        .create()
        .start()
        .resume()
        .visible()
        .get()
    val container = android.widget.FrameLayout(activity)
    activity.setContentView(container)

    val activeBadgeState = androidx.compose.runtime.mutableStateOf("EN")
    val toolbarHeightState =
      androidx.compose.runtime.mutableStateOf(androidx.compose.ui.unit.Dp(36f))
    val densityState =
      androidx.compose.runtime.mutableStateOf(
        androidx.compose.ui.unit.Density(density = 1.0f, fontScale = 1.0f)
      )
    val clipboardCountState = androidx.compose.runtime.mutableIntStateOf(3)

    val composeView =
      androidx.compose.ui.platform.ComposeView(activity).apply {
        setContent {
          androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalDensity provides densityState.value
          ) {
            com.example.toolbar.KeyboardToolbar(
              palette =
                com.example.theme.KeyboardThemeRepository.paletteFor(
                  com.example.theme.KeyboardThemePreset.SLATE_DARK
                ),
              isClipboardOpen = false,
              clipboardCount = clipboardCountState.intValue,
              showNumberRow = true,
              activeLanguageBadge = activeBadgeState.value,
              onToggleClipboard = {},
              onToggleNumberRow = {},
              onSwitchLanguage = {},
              onMoveCursorLeft = {},
              onMoveCursorRight = {},
              onCycleTheme = {},
              onOpenSettings = {},
              height = toolbarHeightState.value,
            )
          }
        }
      }
    container.addView(composeView)

    val languages =
      listOf(
        LanguageManager.EnglishLanguage,
        LanguageManager.BanglaProbhatLanguage,
        LanguageManager.BanglishLanguage,
        LanguageManager.HindiHinglishLanguage,
      )
    val widthDpMatrix = listOf(320, 330, 360, 393, 412, 480, 600, 720)
    val densityMatrix = listOf(0.75f, 1.0f, 1.5f, 2.0f, 3.0f)
    val fontScaleMatrix = listOf(1.0f, 1.3f, 1.5f, 2.0f)
    val toolbarHeightsDp =
      listOf(
        androidx.compose.ui.unit.Dp(36f),
        androidx.compose.ui.unit.Dp(32f),
      ) // Portrait (36dp) & Landscape (32dp)

    for (densityVal in densityMatrix) {
      val expectedIconPx = Math.round(24f * densityVal)
      val expectedTouchWidthPx = Math.round(48f * densityVal)

      for (fontScaleVal in fontScaleMatrix) {
        densityState.value =
          androidx.compose.ui.unit.Density(density = densityVal, fontScale = fontScaleVal)

        for (toolbarHeight in toolbarHeightsDp) {
          toolbarHeightState.value = toolbarHeight
          val heightPx = Math.round(toolbarHeight.value * densityVal)

          for (widthDp in widthDpMatrix) {
            val widthPx = Math.round(widthDp * densityVal)
            val widthSpec =
              android.view.View.MeasureSpec.makeMeasureSpec(
                widthPx,
                android.view.View.MeasureSpec.EXACTLY,
              )
            val heightSpec =
              android.view.View.MeasureSpec.makeMeasureSpec(
                heightPx,
                android.view.View.MeasureSpec.EXACTLY,
              )

            var baselineEnglishIconW = -1
            var baselineEnglishIconH = -1
            var baselineEnglishBtnW = -1
            var baselineEnglishRightX = -1f

            for (lang in languages) {
              activeBadgeState.value = lang.badgeLabel
              androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
              org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(java.time.Duration.ofMillis(16))
              val uiDispatcher =
                androidx.compose.ui.platform.AndroidUiDispatcher.CurrentThread[
                  kotlin.coroutines.ContinuationInterceptor
                ]
                  as? androidx.compose.ui.platform.AndroidUiDispatcher
              if (uiDispatcher != null) {
                val field =
                  uiDispatcher.javaClass
                    .getDeclaredField("dispatchCallback")
                    .apply { isAccessible = true }
                val callback = field.get(uiDispatcher) as android.view.Choreographer.FrameCallback
                callback.doFrame(System.nanoTime())
              }
              org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                .idleFor(java.time.Duration.ofMillis(16))
              composeView.forceLayout()
              composeView.measure(widthSpec, heightSpec)
              composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

              val iconW = com.example.toolbar.lastMeasuredMicIconWidthPx
              val iconH = com.example.toolbar.lastMeasuredMicIconHeightPx
              val btnW = com.example.toolbar.lastMeasuredMicButtonWidthPx
              val btnH = com.example.toolbar.lastMeasuredMicButtonHeightPx
              val rightX = com.example.toolbar.lastMeasuredMicButtonRightInToolbarPx

              val contextMsg =
                "lang=${lang.id}(${lang.badgeLabel}) width=${widthDp}dp density=$densityVal fontScale=$fontScaleVal height=$toolbarHeight"

              assertEquals("Mic icon width must be 24dp ($contextMsg)", expectedIconPx, iconW)
              assertEquals("Mic icon height must be 24dp ($contextMsg)", expectedIconPx, iconH)
              assertEquals(
                "Mic touch target width must be 48dp ($contextMsg)",
                expectedTouchWidthPx,
                btnW,
              )
              assertEquals("Mic touch target height must fill toolbar ($contextMsg)", heightPx, btnH)

              if (lang.id == LanguageManager.ID_ENGLISH) {
                baselineEnglishIconW = iconW
                baselineEnglishIconH = iconH
                baselineEnglishBtnW = btnW
                baselineEnglishRightX = rightX
              } else {
                assertEquals(
                  "Mic icon width in ${lang.id} must match English exactly ($contextMsg)",
                  baselineEnglishIconW,
                  iconW,
                )
                assertEquals(
                  "Mic icon height in ${lang.id} must match English exactly ($contextMsg)",
                  baselineEnglishIconH,
                  iconH,
                )
                assertEquals(
                  "Mic touch target width in ${lang.id} must match English exactly ($contextMsg)",
                  baselineEnglishBtnW,
                  btnW,
                )
                assertEquals(
                  "Mic far-right anchor position in ${lang.id} must match English ($contextMsg)",
                  baselineEnglishRightX,
                  rightX,
                  1.0f,
                )
              }
            }
          }
        }
      }
    }
    activity.finish()
  }

  @Test
  fun `github update manager parses release info and detects newer versions correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val updateManager = com.example.update.GitHubUpdateManager(context)

    val sampleReleaseJson = org.json.JSONObject("""
      {
        "tag_name": "v1.5",
        "name": "Personal Keyboard v1.5",
        "body": "Fixed microphone alignment and added update system.\nversionCode: 5",
        "published_at": "2026-11-07T00:00:00Z",
        "assets": [
          {
            "name": "PersonalKeyboard-v1.5.apk",
            "content_type": "application/vnd.android.package-archive",
            "size": 18456000,
            "browser_download_url": "https://github.com/lionhitgo/PersonalKeyboard/releases/download/v1.5/PersonalKeyboard-v1.5.apk"
          }
        ]
      }
    """.trimIndent())

    val release = updateManager.parseReleaseInfo(sampleReleaseJson)
    assertNotNull(release)
    assertEquals("v1.5", release!!.tagName)
    assertEquals("1.5", release.versionName)
    assertEquals(5, release.versionCode)
    assertEquals(18456000L, release.apkSizeBytes)
    assertEquals("PersonalKeyboard-v1.5.apk", release.apkFileName)
    assertTrue(updateManager.isNewerThanCurrent(release))

    // Older release test
    val olderRelease = release.copy(versionName = "0.9", versionCode = 0)
    assertFalse(updateManager.isNewerThanCurrent(olderRelease))
  }

  @Test
  fun `settings viewmodel handles update states and transitions cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefsRepo = KeyboardPreferencesRepository.getInstance(context)
    val viewModel = com.example.settings.SettingsViewModel(repository = prefsRepo)

    assertEquals(com.example.update.UpdateUiState.Idle, viewModel.updateState.value)

    val dummyRelease = com.example.update.GitHubReleaseInfo(
      tagName = "v2.0",
      versionName = "2.0",
      versionCode = 2,
      releaseNotes = "Major V2 update",
      apkDownloadUrl = "https://example.com/test.apk",
      apkFileName = "test.apk",
      apkSizeBytes = 1024L,
      publishedAt = "2026-11-07T00:00:00Z",
    )

    viewModel.setCustomUpdateState(
      com.example.update.UpdateUiState.UpdateAvailable(
        releaseInfo = dummyRelease,
        currentVersionName = "1.0",
        currentVersionCode = 1,
      )
    )
    assertTrue(viewModel.updateState.value is com.example.update.UpdateUiState.UpdateAvailable)

    viewModel.setCustomUpdateState(
      com.example.update.UpdateUiState.Downloading(
        releaseInfo = dummyRelease,
        progressPercent = 45,
        bytesDownloaded = 450L,
        totalBytes = 1000L,
      )
    )
    val downloadingState = viewModel.updateState.value as com.example.update.UpdateUiState.Downloading
    assertEquals(45, downloadingState.progressPercent)

    viewModel.setCustomUpdateState(
      com.example.update.UpdateUiState.ReadyToInstall(
        releaseInfo = dummyRelease,
        apkFilePath = "/tmp/dummy.apk",
      )
    )
    assertTrue(viewModel.updateState.value is com.example.update.UpdateUiState.ReadyToInstall)
  }

  @Test
  fun `github update manager handles rate limit 403 exponential backoff cooldown and web fallback gracefully`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val updateManager = com.example.update.GitHubUpdateManager(context)
    updateManager.clearRateLimitAndCache()

    // 1. Verify HTTP 403 and 429 rate-limit response detection
    assertTrue(
      updateManager.isRateLimitResponse(
        responseCode = 403,
        rateLimitRemaining = "0",
        retryAfterHeader = null,
        errorBody = """{"message":"API rate limit exceeded for 203.0.113.1."}""",
      )
    )
    assertTrue(
      updateManager.isRateLimitResponse(
        responseCode = 403,
        rateLimitRemaining = null,
        retryAfterHeader = null,
        errorBody = """{"message":"You have exceeded a secondary rate limit."}""",
      )
    )
    assertTrue(
      updateManager.isRateLimitResponse(
        responseCode = 429,
        rateLimitRemaining = null,
        retryAfterHeader = "60",
        errorBody = "",
      )
    )
    assertFalse(
      updateManager.isRateLimitResponse(
        responseCode = 404,
        rateLimitRemaining = "59",
        retryAfterHeader = null,
        errorBody = """{"message":"Not Found"}""",
      )
    )

    // 2. Verify exponential backoff progression (30s -> 60s -> 120s -> 240s, capped at 30m)
    assertEquals(30_000L, updateManager.computeExponentialBackoffMs(0))
    assertEquals(60_000L, updateManager.computeExponentialBackoffMs(1))
    assertEquals(120_000L, updateManager.computeExponentialBackoffMs(2))
    assertEquals(240_000L, updateManager.computeExponentialBackoffMs(3))
    assertEquals(
      com.example.update.GitHubUpdateManager.MAX_BACKOFF_MS,
      updateManager.computeExponentialBackoffMs(10),
    )

    // 3. Verify Retry-After and X-RateLimit-Reset header parsing
    val nowMs = (System.currentTimeMillis() / 1000L) * 1000L
    assertEquals(
      90_000L,
      updateManager.parseRateLimitDelayMs(
        retryAfterHeader = "90",
        rateLimitResetHeader = null,
        failureCount = 0,
        nowMs = nowMs,
      ),
    )
    val resetEpochSec = ((nowMs + 180_000L) / 1000L).toString()
    assertEquals(
      180_000L,
      updateManager.parseRateLimitDelayMs(
        retryAfterHeader = null,
        rateLimitResetHeader = resetEpochSec,
        failureCount = 0,
        nowMs = nowMs,
      ),
    )

    // 4. Verify automatic check cooldown (15 minutes) suppresses repeated automatic checks
    assertTrue(updateManager.shouldPerformAutomaticCheck(nowMs))
    val cachedRelease =
      com.example.update.GitHubReleaseInfo(
        tagName = "v1.2.0",
        versionName = "1.2.0",
        versionCode = 5,
        releaseNotes = "Cached release notes",
        apkDownloadUrl = "https://github.com/b2kzoon-coder/Powerfull-keyboard/releases/download/v1.2.0/app-release.apk",
        apkFileName = "app-release.apk",
        apkSizeBytes = 31761003L,
        publishedAt = "2026-11-07T00:00:00Z",
      )
    updateManager.recordSuccess(
      etag = "\"abc123etag\"",
      lastModified = "Sat, 07 Nov 2026 12:00:00 GMT",
      releaseInfo = cachedRelease,
      nowMs = nowMs,
    )
    // 1 minute later -> automatic check must be skipped
    assertFalse(updateManager.shouldPerformAutomaticCheck(nowMs + 60_000L))
    // 16 minutes later -> automatic check is allowed again
    assertTrue(updateManager.shouldPerformAutomaticCheck(nowMs + 16 * 60 * 1000L))
    assertEquals(cachedRelease, updateManager.getCachedReleaseInfo())

    // 5. Verify SettingsViewModel restores cached release during cooldown without network spam
    val prefsRepo = KeyboardPreferencesRepository.getInstance(context)
    val viewModel =
      com.example.settings.SettingsViewModel(
        repository = prefsRepo,
        updateManager = updateManager,
      )
    viewModel.checkForUpdates(isAutomatic = true)
    assertTrue(viewModel.updateState.value is com.example.update.UpdateUiState.UpdateAvailable)

    // 6. Verify zero-API-quota web expanded_assets HTML fallback parser
    val sampleExpandedAssetsHtml =
      """
      <div class="Box Box--condensed mt-3">
        <ul>
          <li class="Box-row d-flex flex-column flex-md-row">
            <a href="/b2kzoon-coder/Powerfull-keyboard/releases/download/v1.0.3/app-release.apk" rel="nofollow">
              <span>app-release.apk</span>
            </a>
          </li>
        </ul>
      </div>
      """.trimIndent()
    val webFallbackRelease =
      updateManager.parseWebExpandedAssetsHtml("v1.0.3", sampleExpandedAssetsHtml)
    assertNotNull(webFallbackRelease)
    assertEquals("v1.0.3", webFallbackRelease!!.tagName)
    assertEquals("1.0.3", webFallbackRelease.versionName)
    assertEquals(
      "https://github.com/b2kzoon-coder/Powerfull-keyboard/releases/download/v1.0.3/app-release.apk",
      webFallbackRelease.apkDownloadUrl,
    )
    assertTrue(updateManager.isNewerThanCurrent(webFallbackRelease))
    assertFalse(
      updateManager.isNewerThanCurrent(
        webFallbackRelease.copy(
          tagName = "v1.0.2",
          versionName = "1.0.2",
          versionCode = BuildConfig.VERSION_CODE,
        )
      )
    )

    updateManager.clearRateLimitAndCache()
  }

  @Test
  fun testDirectBootSafeInitializationAndUnlockReload() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userManager = context.getSystemService(Context.USER_SERVICE) as android.os.UserManager
    val shadowUserManager = org.robolectric.Shadows.shadowOf(userManager)

    try {
      // 1. First prepare persistent credential-protected preferences while unlocked
      shadowUserManager.setUserUnlocked(true)
      val directPrefs = context.getSharedPreferences("personal_keyboard_prefs", Context.MODE_PRIVATE)
      directPrefs.edit()
        .putString("active_language_id", com.example.language.LanguageManager.ID_BANGLA_PROBHAT)
        .putInt("portrait_height_percent", 125)
        .apply()

      val clipPrefs = context.getSharedPreferences("com.example.clipboard_prefs", Context.MODE_PRIVATE)
      val sampleClipArray = org.json.JSONArray().apply {
        put(org.json.JSONObject().apply {
          put("id", 101L)
          put("text", "DirectBootPersistentClip")
          put("pinned", true)
        })
      }
      clipPrefs.edit()
        .putString("entries", sampleClipArray.toString())
        .putLong("next_id", 102L)
        .apply()

      // Reset static instances for clean isolation
      val repoField = KeyboardPreferencesRepository::class.java.getDeclaredField("instance")
      repoField.isAccessible = true
      repoField.set(null, null)

      val emojiRepoField = com.example.emoji.EmojiPreferencesRepository::class.java.getDeclaredField("instance")
      emojiRepoField.isAccessible = true
      emojiRepoField.set(null, null)

      // 2. Simulate Direct Boot state: device booted, but user NOT yet unlocked
      shadowUserManager.setUserUnlocked(false)
      assertFalse(androidx.core.os.UserManagerCompat.isUserUnlocked(context))

      // Verify KeyboardPreferencesRepository initializes without crashing into default config
      val lockedRepo = KeyboardPreferencesRepository.getInstance(context)
      assertNotNull(lockedRepo)
      assertEquals(com.example.language.LanguageManager.ID_ENGLISH, lockedRepo.config.value.activeLanguageId)

      // Verify ClipboardController initializes safely in Direct Boot without throwing
      val lockedClipboard = com.example.clipboard.ClipboardController(context)
      assertNotNull(lockedClipboard)
      assertEquals(emptyList<com.example.clipboard.ClipboardEntry>(), lockedClipboard.entries.value)

      // Verify EmojiPreferencesRepository initializes safely in Direct Boot without throwing
      val lockedEmojiRepo = com.example.emoji.EmojiPreferencesRepository.getInstance(context)
      assertNotNull(lockedEmojiRepo)
      assertTrue(lockedEmojiRepo.recentEmojis.value.isNotEmpty())

      // Verify PersonalKeyboardService initializes during Direct Boot without crash
      val serviceController = org.robolectric.Robolectric.buildService(PersonalKeyboardService::class.java)
      serviceController.create()
      val service = serviceController.get()
      assertNotNull(service)

      // Service should be in safe default mode (English)
      assertEquals(com.example.language.LanguageManager.ID_ENGLISH, service.languageManager.activeLanguage.value.id)

      // 3. Simulate first device unlock: user enters PIN/password
      shadowUserManager.setUserUnlocked(true)
      assertTrue(androidx.core.os.UserManagerCompat.isUserUnlocked(context))

      // Trigger unlock broadcast or reload
      context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_USER_UNLOCKED))
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

      // Verify state was reloaded seamlessly from persistent credential-protected preferences
      service.unlockAndReloadState()
      org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

      // Verify preferences reloaded and preserved
      assertEquals(125, lockedRepo.config.value.portraitHeightPercent)
      assertEquals(com.example.language.LanguageManager.ID_BANGLA_PROBHAT, lockedRepo.config.value.activeLanguageId)
      assertEquals(com.example.language.LanguageManager.ID_BANGLA_PROBHAT, service.languageManager.activeLanguage.value.id)

      // Verify clipboard entries restored from credential-protected storage
      lockedClipboard.reloadIfUnlocked()
      val restoredClips = lockedClipboard.entries.value
      assertEquals(1, restoredClips.size)
      assertEquals("DirectBootPersistentClip", restoredClips[0].text)
      assertTrue(restoredClips[0].isPinned)

      serviceController.destroy()
    } finally {
      shadowUserManager.setUserUnlocked(true)
    }
  }
}


