package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.os.UserManagerCompat
import com.example.language.LanguageManager
import com.example.layout.KeyboardHeightBounds
import com.example.theme.KeyboardThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.voice.VoiceRecognitionMode

@Immutable
data class KeyboardUserConfig(
  val portraitHeightPercent: Int = KeyboardHeightBounds.DEFAULT_PORTRAIT_PERCENT,
  val landscapeHeightPercent: Int = KeyboardHeightBounds.DEFAULT_LANDSCAPE_PERCENT,
  val themePreset: KeyboardThemePreset = KeyboardThemePreset.SLATE_DARK,
  val showNumberRow: Boolean = false,
  val showToolbar: Boolean = true,
  val cutCopyPasteEnabled: Boolean = true,
  val hapticFeedback: Boolean = true,
  val soundOnKeypress: Boolean = false,
  val autoCapitalize: Boolean = true,
  val doubleSpacePeriod: Boolean = true,
  val voiceRecognitionMode: VoiceRecognitionMode = VoiceRecognitionMode.AUTOMATIC,
  val voiceNoiseCancellation: Boolean = true,
  val voiceEnhancement: Boolean = true,
  val activeLanguageId: String = LanguageManager.ID_ENGLISH,
  val voiceInputEnabled: Boolean = true,
  val voiceBanglishMode: Boolean = false,
  val voiceAutoPunctuation: Boolean = true,
  val voiceWaveformAndHaptics: Boolean = true,
)

class KeyboardPreferencesRepository private constructor(context: Context) {
  private val appContext: Context = context.applicationContext
  private var prefs: SharedPreferences? = null
  @Volatile private var isInitializedFromPrefs = false

  private val _config = MutableStateFlow(KeyboardUserConfig())
  val config: StateFlow<KeyboardUserConfig> = _config.asStateFlow()

  init {
    if (UserManagerCompat.isUserUnlocked(appContext)) {
      initializePrefsLocked()
    }
  }

  private fun initializePrefsLocked(): Boolean {
    if (isInitializedFromPrefs) return true
    return try {
      val p = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs = p
      isInitializedFromPrefs = true
      _config.value = readFromPrefs(p)
      true
    } catch (_: Throwable) {
      false
    }
  }

  fun reloadIfUnlocked(): Boolean {
    if (isInitializedFromPrefs) return true
    if (!UserManagerCompat.isUserUnlocked(appContext)) return false
    return synchronized(this) {
      if (isInitializedFromPrefs) return true
      initializePrefsLocked()
    }
  }

  private fun getPrefs(): SharedPreferences? {
    if (!isInitializedFromPrefs && UserManagerCompat.isUserUnlocked(appContext)) {
      synchronized(this) {
        if (!isInitializedFromPrefs) {
          initializePrefsLocked()
        }
      }
    }
    return prefs
  }

  private fun readFromPrefs(p: SharedPreferences): KeyboardUserConfig {
    val rawLangId =
      p.getString(KEY_ACTIVE_LANGUAGE_ID, LanguageManager.ID_ENGLISH)
        ?: LanguageManager.ID_ENGLISH
    val normalizedLangId = LanguageManager.normalizeLanguageId(rawLangId)
    return KeyboardUserConfig(
      portraitHeightPercent =
        KeyboardHeightBounds.clampPercent(
          p.getInt(KEY_PORTRAIT_HEIGHT_PERCENT, KeyboardHeightBounds.DEFAULT_PORTRAIT_PERCENT)
        ),
      landscapeHeightPercent =
        KeyboardHeightBounds.clampPercent(
          p.getInt(
            KEY_LANDSCAPE_HEIGHT_PERCENT,
            KeyboardHeightBounds.DEFAULT_LANDSCAPE_PERCENT,
          )
        ),
      themePreset =
        KeyboardThemePreset.fromId(
          p.getString(KEY_THEME_PRESET, KeyboardThemePreset.SLATE_DARK.id)
            ?: KeyboardThemePreset.SLATE_DARK.id
        ),
      showNumberRow = p.getBoolean(KEY_SHOW_NUMBER_ROW, false),
      showToolbar = p.getBoolean(KEY_SHOW_TOOLBAR, true),
      cutCopyPasteEnabled = p.getBoolean(KEY_CUT_COPY_PASTE_ENABLED, true),
      hapticFeedback = p.getBoolean(KEY_HAPTIC_FEEDBACK, true),
      soundOnKeypress = p.getBoolean(KEY_SOUND_ON_KEYPRESS, false),
      autoCapitalize = p.getBoolean(KEY_AUTO_CAPITALIZE, true),
      doubleSpacePeriod = p.getBoolean(KEY_DOUBLE_SPACE_PERIOD, true),
      voiceRecognitionMode =
        VoiceRecognitionMode.fromId(
          p.getString(KEY_VOICE_RECOGNITION_MODE, VoiceRecognitionMode.AUTOMATIC.id)
            ?: VoiceRecognitionMode.AUTOMATIC.id
        ),
      voiceNoiseCancellation = p.getBoolean(KEY_VOICE_NOISE_CANCELLATION, true),
      voiceEnhancement = p.getBoolean(KEY_VOICE_ENHANCEMENT, true),
      activeLanguageId = normalizedLangId,
      voiceInputEnabled = p.getBoolean(KEY_VOICE_INPUT_ENABLED, true),
      voiceBanglishMode =
        p.getBoolean(
          KEY_VOICE_BANGLISH_MODE,
          normalizedLangId == LanguageManager.ID_BANGLISH,
        ),
      voiceAutoPunctuation = p.getBoolean(KEY_VOICE_AUTO_PUNCTUATION, true),
      voiceWaveformAndHaptics = p.getBoolean(KEY_VOICE_WAVEFORM_AND_HAPTICS, true),
    )
  }

  fun setPortraitHeightPercent(percent: Int) {
    val clamped = KeyboardHeightBounds.clampPercent(percent)
    if (_config.value.portraitHeightPercent == clamped) return
    getPrefs()?.edit()?.putInt(KEY_PORTRAIT_HEIGHT_PERCENT, clamped)?.apply()
    _config.update { it.copy(portraitHeightPercent = clamped) }
  }

  fun setLandscapeHeightPercent(percent: Int) {
    val clamped = KeyboardHeightBounds.clampPercent(percent)
    if (_config.value.landscapeHeightPercent == clamped) return
    getPrefs()?.edit()?.putInt(KEY_LANDSCAPE_HEIGHT_PERCENT, clamped)?.apply()
    _config.update { it.copy(landscapeHeightPercent = clamped) }
  }

  fun setThemePreset(preset: KeyboardThemePreset) {
    if (_config.value.themePreset == preset) return
    getPrefs()?.edit()?.putString(KEY_THEME_PRESET, preset.id)?.apply()
    _config.update { it.copy(themePreset = preset) }
  }

  fun cycleThemePreset() {
    val entries = KeyboardThemePreset.entries
    val current = _config.value.themePreset
    val next = entries[(entries.indexOf(current) + 1) % entries.size]
    setThemePreset(next)
  }

  fun setShowNumberRow(enabled: Boolean) {
    if (_config.value.showNumberRow == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_SHOW_NUMBER_ROW, enabled)?.apply()
    _config.update { it.copy(showNumberRow = enabled) }
  }

  fun toggleNumberRow() {
    setShowNumberRow(!_config.value.showNumberRow)
  }

  fun setShowToolbar(enabled: Boolean) {
    if (_config.value.showToolbar == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_SHOW_TOOLBAR, enabled)?.apply()
    _config.update { it.copy(showToolbar = enabled) }
  }

  fun setCutCopyPasteEnabled(enabled: Boolean) {
    if (_config.value.cutCopyPasteEnabled == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_CUT_COPY_PASTE_ENABLED, enabled)?.apply()
    _config.update { it.copy(cutCopyPasteEnabled = enabled) }
  }

  fun setHapticFeedback(enabled: Boolean) {
    if (_config.value.hapticFeedback == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_HAPTIC_FEEDBACK, enabled)?.apply()
    _config.update { it.copy(hapticFeedback = enabled) }
  }

  fun setSoundOnKeypress(enabled: Boolean) {
    if (_config.value.soundOnKeypress == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_SOUND_ON_KEYPRESS, enabled)?.apply()
    _config.update { it.copy(soundOnKeypress = enabled) }
  }

  fun setAutoCapitalize(enabled: Boolean) {
    if (_config.value.autoCapitalize == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_AUTO_CAPITALIZE, enabled)?.apply()
    _config.update { it.copy(autoCapitalize = enabled) }
  }

  fun setDoubleSpacePeriod(enabled: Boolean) {
    if (_config.value.doubleSpacePeriod == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_DOUBLE_SPACE_PERIOD, enabled)?.apply()
    _config.update { it.copy(doubleSpacePeriod = enabled) }
  }

  fun setVoiceRecognitionMode(mode: VoiceRecognitionMode) {
    if (_config.value.voiceRecognitionMode == mode) return
    getPrefs()?.edit()?.putString(KEY_VOICE_RECOGNITION_MODE, mode.id)?.apply()
    _config.update { it.copy(voiceRecognitionMode = mode) }
  }

  fun setVoiceNoiseCancellation(enabled: Boolean) {
    if (_config.value.voiceNoiseCancellation == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_VOICE_NOISE_CANCELLATION, enabled)?.apply()
    _config.update { it.copy(voiceNoiseCancellation = enabled) }
  }

  fun setVoiceEnhancement(enabled: Boolean) {
    if (_config.value.voiceEnhancement == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_VOICE_ENHANCEMENT, enabled)?.apply()
    _config.update { it.copy(voiceEnhancement = enabled) }
  }

  fun setVoiceInputEnabled(enabled: Boolean) {
    if (_config.value.voiceInputEnabled == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_VOICE_INPUT_ENABLED, enabled)?.apply()
    _config.update { it.copy(voiceInputEnabled = enabled) }
  }

  fun setVoiceBanglishMode(enabled: Boolean) {
    val targetLangId =
      if (enabled) {
        LanguageManager.ID_BANGLISH
      } else if (_config.value.activeLanguageId == LanguageManager.ID_BANGLISH) {
        LanguageManager.ID_ENGLISH
      } else {
        _config.value.activeLanguageId
      }
    getPrefs()
      ?.edit()
      ?.putBoolean(KEY_VOICE_BANGLISH_MODE, enabled)
      ?.putString(KEY_ACTIVE_LANGUAGE_ID, targetLangId)
      ?.apply()
    _config.update {
      it.copy(
        voiceBanglishMode = enabled,
        activeLanguageId = targetLangId,
      )
    }
  }

  fun setVoiceAutoPunctuation(enabled: Boolean) {
    if (_config.value.voiceAutoPunctuation == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_VOICE_AUTO_PUNCTUATION, enabled)?.apply()
    _config.update { it.copy(voiceAutoPunctuation = enabled) }
  }

  fun setVoiceWaveformAndHaptics(enabled: Boolean) {
    if (_config.value.voiceWaveformAndHaptics == enabled) return
    getPrefs()?.edit()?.putBoolean(KEY_VOICE_WAVEFORM_AND_HAPTICS, enabled)?.apply()
    _config.update { it.copy(voiceWaveformAndHaptics = enabled) }
  }

  fun setActiveLanguageId(languageId: String) {
    val normalized = LanguageManager.normalizeLanguageId(languageId)
    val isBanglish = normalized == LanguageManager.ID_BANGLISH
    if (_config.value.activeLanguageId == normalized && _config.value.voiceBanglishMode == isBanglish) return
    getPrefs()
      ?.edit()
      ?.putString(KEY_ACTIVE_LANGUAGE_ID, normalized)
      ?.putBoolean(KEY_VOICE_BANGLISH_MODE, isBanglish)
      ?.apply()
    _config.update {
      it.copy(
        activeLanguageId = normalized,
        voiceBanglishMode = isBanglish,
      )
    }
  }

  companion object {
    private const val PREFS_NAME = "personal_keyboard_prefs"
    private const val KEY_PORTRAIT_HEIGHT_PERCENT = "portrait_height_percent"
    private const val KEY_LANDSCAPE_HEIGHT_PERCENT = "landscape_height_percent"
    private const val KEY_THEME_PRESET = "theme_preset"
    private const val KEY_SHOW_NUMBER_ROW = "show_number_row"
    private const val KEY_SHOW_TOOLBAR = "show_toolbar"
    private const val KEY_CUT_COPY_PASTE_ENABLED = "cut_copy_paste_enabled"
    private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
    private const val KEY_SOUND_ON_KEYPRESS = "sound_on_keypress"
    private const val KEY_AUTO_CAPITALIZE = "auto_capitalize"
    private const val KEY_DOUBLE_SPACE_PERIOD = "double_space_period"
    private const val KEY_VOICE_RECOGNITION_MODE = "voice_recognition_mode"
    private const val KEY_VOICE_NOISE_CANCELLATION = "voice_noise_cancellation"
    private const val KEY_VOICE_ENHANCEMENT = "voice_enhancement"
    private const val KEY_ACTIVE_LANGUAGE_ID = "active_language_id"
    private const val KEY_VOICE_INPUT_ENABLED = "voice_input_enabled"
    private const val KEY_VOICE_BANGLISH_MODE = "voice_banglish_mode"
    private const val KEY_VOICE_AUTO_PUNCTUATION = "voice_auto_punctuation"
    private const val KEY_VOICE_WAVEFORM_AND_HAPTICS = "voice_waveform_and_haptics"

    @Volatile private var instance: KeyboardPreferencesRepository? = null

    fun getInstance(context: Context): KeyboardPreferencesRepository {
      val appCtx = context.applicationContext
      val current = instance
      if (current != null && current.appContext === appCtx) {
        current.reloadIfUnlocked()
        return current
      }
      return synchronized(this) {
        val existing = instance
        if (existing != null && existing.appContext === appCtx) {
          existing.reloadIfUnlocked()
          existing
        } else {
          KeyboardPreferencesRepository(appCtx).also { instance = it }
        }
      }
    }
  }
}

