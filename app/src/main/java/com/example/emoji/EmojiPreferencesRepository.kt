package com.example.emoji

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Persists recent and frequently used emojis locally using SharedPreferences.
 * Provides StateFlows for real-time reactivity without restarting the keyboard.
 */
class EmojiPreferencesRepository private constructor(context: Context) {

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _recentEmojis = MutableStateFlow<List<String>>(emptyList())
  val recentEmojis: StateFlow<List<String>> = _recentEmojis.asStateFlow()

  private val _frequentEmojis = MutableStateFlow<List<String>>(emptyList())
  val frequentEmojis: StateFlow<List<String>> = _frequentEmojis.asStateFlow()

  private val _selectedSkinTone = MutableStateFlow(EmojiSkinTone.DEFAULT)
  val selectedSkinTone: StateFlow<EmojiSkinTone> = _selectedSkinTone.asStateFlow()

  // In-memory frequency map
  private val frequencyMap = mutableMapOf<String, Int>()

  init {
    loadPreferences()
  }

  private fun loadPreferences() {
    // 1. Load recent emojis (ordered CSV)
    val recentRaw = prefs.getString(KEY_RECENT_EMOJIS, null)
    if (!recentRaw.isNullOrEmpty()) {
      _recentEmojis.value = recentRaw.split(DELIMITER).filter { it.isNotEmpty() }.take(MAX_RECENT)
    } else {
      // Default initial recent emojis
      _recentEmojis.value = listOf("❤️", "😊", "👍", "😂", "🔥", "✨", "🇧🇩", "🙏")
    }

    // 2. Load frequency counts
    val freqRaw = prefs.getString(KEY_FREQUENT_EMOJIS, null)
    if (!freqRaw.isNullOrEmpty()) {
      freqRaw.split(";").forEach { entry ->
        val parts = entry.split("=")
        if (parts.size == 2) {
          val count = parts[1].toIntOrNull() ?: 0
          if (parts[0].isNotEmpty() && count > 0) {
            frequencyMap[parts[0]] = count
          }
        }
      }
    }
    updateFrequentList()

    // 3. Load default skin tone
    val toneName = prefs.getString(KEY_SKIN_TONE, EmojiSkinTone.DEFAULT.name)
    _selectedSkinTone.value =
      EmojiSkinTone.entries.find { it.name == toneName } ?: EmojiSkinTone.DEFAULT
  }

  /**
   * Records an emoji usage:
   * 1. Prepends to recent emojis list (deduplicating).
   * 2. Increments frequency count for the emoji.
   * 3. Persists to SharedPreferences.
   */
  @Synchronized
  fun recordEmojiUsage(emoji: String) {
    if (emoji.isEmpty()) return

    // Update recent only if the emoji is not already at index 0
    val currentRecent = _recentEmojis.value
    if (currentRecent.firstOrNull() != emoji) {
      val updated = ArrayList<String>((currentRecent.size + 1).coerceAtMost(MAX_RECENT))
      updated.add(emoji)
      for (item in currentRecent) {
        if (item != emoji && updated.size < MAX_RECENT) {
          updated.add(item)
        }
      }
      _recentEmojis.value = updated
      persistRecent(updated)
    }

    // Update frequency
    val currentCount = frequencyMap[emoji] ?: 0
    frequencyMap[emoji] = currentCount + 1
    persistFrequency()
    updateFrequentList()
  }

  fun setSelectedSkinTone(tone: EmojiSkinTone) {
    if (_selectedSkinTone.value == tone) return
    _selectedSkinTone.value = tone
    prefs.edit().putString(KEY_SKIN_TONE, tone.name).apply()
  }

  @Synchronized
  fun clearRecent() {
    _recentEmojis.value = emptyList()
    frequencyMap.clear()
    _frequentEmojis.value = emptyList()
    prefs.edit()
      .remove(KEY_RECENT_EMOJIS)
      .remove(KEY_FREQUENT_EMOJIS)
      .apply()
  }

  private fun updateFrequentList() {
    val top = frequencyMap.entries
      .sortedByDescending { it.value }
      .map { it.key }
      .take(MAX_FREQUENT)
    if (_frequentEmojis.value != top) {
      _frequentEmojis.value = top
    }
  }

  private fun persistRecent(list: List<String>) {
    val raw = list.joinToString(DELIMITER)
    prefs.edit().putString(KEY_RECENT_EMOJIS, raw).apply()
  }

  private fun persistFrequency() {
    val raw = frequencyMap.entries.joinToString(";") { "${it.key}=${it.value}" }
    prefs.edit().putString(KEY_FREQUENT_EMOJIS, raw).apply()
  }

  companion object {
    private const val PREFS_NAME = "personal_keyboard_emojis"
    private const val KEY_RECENT_EMOJIS = "key_recent_emojis"
    private const val KEY_FREQUENT_EMOJIS = "key_frequent_emojis"
    private const val KEY_SKIN_TONE = "key_default_skin_tone"
    private const val DELIMITER = "\u001F"
    private const val MAX_RECENT = 40
    private const val MAX_FREQUENT = 32

    @Volatile
    private var instance: EmojiPreferencesRepository? = null

    fun getInstance(context: Context): EmojiPreferencesRepository {
      return instance ?: synchronized(this) {
        instance ?: EmojiPreferencesRepository(context).also { instance = it }
      }
    }
  }
}
