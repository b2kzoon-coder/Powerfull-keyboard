package com.example.voice

import androidx.compose.runtime.Immutable

enum class VoiceState {
  IDLE,
  STARTING,
  LISTENING,
  PROCESSING,
  ERROR,
}

enum class VoiceRecognitionMode(val id: String, val displayName: String, val shortLabel: String) {
  AUTOMATIC("auto", "Automatic (Online)", "Auto"),
  ONLINE_ONLY("online", "Online Only (Google Speech)", "Online");

  companion object {
    fun fromId(id: String): VoiceRecognitionMode {
      return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AUTOMATIC
    }
  }
}

@Immutable
data class AudioProcessingInfo(
  val isNoiseSuppressorAvailable: Boolean = false,
  val isAcousticEchoCancelerAvailable: Boolean = false,
  val isAutomaticGainControlAvailable: Boolean = false,
  val isNoiseSuppressorActive: Boolean = false,
  val isAcousticEchoCancelerActive: Boolean = false,
  val isAutomaticGainControlActive: Boolean = false,
) {
  val hasHardwareEffects: Boolean
    get() = isNoiseSuppressorAvailable || isAcousticEchoCancelerAvailable || isAutomaticGainControlAvailable
  val hasActiveEffects: Boolean
    get() = isNoiseSuppressorActive || isAcousticEchoCancelerActive || isAutomaticGainControlActive
}

@Immutable
data class VoiceLanguage(
  val tag: String,
  val displayName: String,
  val shortLabel: String,
  val recognitionTag: String = tag,
  val isBanglishOutput: Boolean = false,
) {
  companion object {
    val Bangla =
      VoiceLanguage(
        tag = "bn-BD",
        displayName = "বাংলা",
        shortLabel = "BN",
        recognitionTag = "bn-BD",
        isBanglishOutput = false,
      )
    val Banglish =
      VoiceLanguage(
        tag = "bn-Latn",
        displayName = "Banglish",
        shortLabel = "Banglish",
        recognitionTag = "bn-BD",
        isBanglishOutput = true,
      )
    val English =
      VoiceLanguage(
        tag = "en-US",
        displayName = "English",
        shortLabel = "EN",
        recognitionTag = "en-US",
        isBanglishOutput = false,
      )
    val Hindi =
      VoiceLanguage(
        tag = "hi-IN",
        displayName = "हिन्दी",
        shortLabel = "HI",
        recognitionTag = "hi-IN",
        isBanglishOutput = false,
      )

    val SupportedLanguages = listOf(Bangla, English, Hindi)

    fun forLanguageId(languageId: String): VoiceLanguage {
      return when {
        languageId.equals("bn_BANGLISH", ignoreCase = true) ||
          languageId.equals("bn-Latn", ignoreCase = true) ||
          languageId.equals("banglish", ignoreCase = true) -> Banglish
        languageId.startsWith("bn", ignoreCase = true) -> Bangla
        languageId.startsWith("hi", ignoreCase = true) -> Hindi
        else -> English
      }
    }

    fun fromTag(tag: String): VoiceLanguage {
      return when {
        tag.equals("bn-Latn", ignoreCase = true) ||
          tag.equals("bn_BANGLISH", ignoreCase = true) ||
          tag.equals("banglish", ignoreCase = true) -> Banglish
        else -> SupportedLanguages.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: Bangla
      }
    }
  }
}

@Immutable
data class VoiceUiState(
  val state: VoiceState = VoiceState.IDLE,
  val selectedLanguage: VoiceLanguage = VoiceLanguage.Bangla,
  val partialTranscription: String = "",
  val finalTranscription: String = "",
  val errorMessage: String? = null,
  val rmsNormalized: Float = 0f,
  val hasPermission: Boolean = true,
  val isRecognitionAvailable: Boolean = true,
  val recognitionMode: VoiceRecognitionMode = VoiceRecognitionMode.AUTOMATIC,
  val isOnline: Boolean = true,
  val isOnDeviceActive: Boolean = false,
  val noiseCancellationEnabled: Boolean = true,
  val voiceEnhancementEnabled: Boolean = true,
  val audioProcessingInfo: AudioProcessingInfo = AudioProcessingInfo(),
  val activeEngineDescription: String = "Google Speech Services",
)
