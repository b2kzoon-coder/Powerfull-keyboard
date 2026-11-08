package com.example.voice

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.ime.ImeDiagnosticsLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VoiceRecognitionManager(
  private val context: Context,
  private val onSpeechRecognized: (String) -> Unit = {},
  private val onSpeechPartial: ((String) -> Unit)? = null,
  private val onErrorOccurred: ((String) -> Unit)? = null,
  private val onPermissionRequired: (() -> Unit)? = null,
  private val onListeningStateChanged: ((Boolean) -> Unit)? = null,
  private val onRmsChanged: ((Float) -> Unit)? = null,
) {
  private val appContext: Context = context.applicationContext ?: context
  private val mainHandler = Handler(Looper.getMainLooper())
  internal val audioPreProcessor = AudioPreProcessor(appContext)
  private var speechRecognizer: SpeechRecognizer? = null
  internal var activeRecognitionListener: RecognitionListener? = null
  internal val hasActiveRecognizer: Boolean
    get() = speechRecognizer != null

  internal val hasPendingTimers: Boolean
    get() =
      readyWatchdogRunnable != null ||
        partialSilenceRunnable != null ||
        stopFinalizeRunnable != null

  private var candidateComponents: List<ComponentName?> = emptyList()
  private var currentCandidateIndex: Int = 0
  private var readyReceivedInAttempt: Boolean = false
  private var finalResultDeliveredInSession: Boolean = false
  private var rmsLogCounter: Int = 0

  // Zero-Failure Offline Fallback Hack state & retry guard
  var offlineFallbackRetryCount: Int = 0
    internal set
  var isPreferOfflineFallbackActive: Boolean = false
    internal set
  internal var activeRecognitionIntent: Intent? = null

  private var readyWatchdogRunnable: Runnable? = null
  private var partialSilenceRunnable: Runnable? = null
  private var stopFinalizeRunnable: Runnable? = null

  private val _uiState = MutableStateFlow(VoiceUiState())
  val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

  var recognitionMode: VoiceRecognitionMode = VoiceRecognitionMode.AUTOMATIC
    private set
  var isNoiseCancellationEnabled: Boolean = true
    private set
  var isVoiceEnhancementEnabled: Boolean = true
    private set
  var isAutoPunctuationEnabled: Boolean = false
    internal set
  var isWaveformAndHapticsEnabled: Boolean = true
    internal set
  var isVoiceInputEnabled: Boolean = true
    internal set

  fun updatePreferences(
    mode: VoiceRecognitionMode,
    noiseCancellation: Boolean,
    voiceEnhancement: Boolean,
    autoPunctuation: Boolean = false,
    waveformAndHaptics: Boolean = true,
    voiceInputEnabled: Boolean = true,
  ) {
    recognitionMode = mode
    isNoiseCancellationEnabled = noiseCancellation
    isVoiceEnhancementEnabled = voiceEnhancement
    isAutoPunctuationEnabled = autoPunctuation
    isWaveformAndHapticsEnabled = waveformAndHaptics
    this.isVoiceInputEnabled = voiceInputEnabled
    val isOnline = isNetworkConnected()
    val audioInfo = getAudioProcessingInfo()
    _uiState.update {
      it.copy(
        recognitionMode = mode,
        noiseCancellationEnabled = noiseCancellation,
        voiceEnhancementEnabled = voiceEnhancement,
        autoPunctuationEnabled = autoPunctuation,
        waveformAndHapticsEnabled = waveformAndHaptics,
        voiceInputEnabled = voiceInputEnabled,
        isOnline = isOnline,
        audioProcessingInfo = audioInfo,
      )
    }
  }

  internal var isNetworkConnectedOverride: Boolean? = null

  fun getAudioProcessingInfo(): AudioProcessingInfo = audioPreProcessor.getProcessingInfo()

  @Suppress("DEPRECATION")
  fun isNetworkConnected(): Boolean {
    isNetworkConnectedOverride?.let { return it }
    return try {
      val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
      val info = cm.activeNetworkInfo
      if (info != null) {
        return info.isConnected
      }
      val activeNet = cm.activeNetwork
      if (activeNet != null) {
        val caps = cm.getNetworkCapabilities(activeNet)
        if (caps != null) {
          return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
      }
      false
    } catch (e: Exception) {
      Log.w(TAG, "isNetworkConnected check error", e)
      true
    }
  }

  fun hasRecordAudioPermission(): Boolean {
    return ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
      PackageManager.PERMISSION_GRANTED
  }

  fun isRecognitionAvailable(): Boolean {
    return SpeechRecognizer.isRecognitionAvailable(appContext) ||
      buildCandidateComponents().any { it != null }
  }

  private var startupRetryCount: Int = 0

  fun preloadCandidateComponents(): List<ComponentName?> {
    return preloadCandidateComponents(appContext)
  }

  fun invalidateCandidateCache() {
    cachedCandidateComponents = null
  }

  fun prewarm() {
    runOnMainThread {
      preloadCandidateComponents(appContext)
    }
  }

  fun resolveBestRecognitionComponent(): ComponentName? {
    return buildCandidateComponents().firstOrNull { it != null }
  }

  /**
   * Builds an ordered list of RecognitionService candidates.
   * On Xiaomi MIUI/HyperOS devices, Settings.Secure.VOICE_RECOGNITION_SERVICE is often empty or
   * points to a Chinese-only OEM service (com.xiaomi.mibrain.speech / com.miui.voiceassist) that
   * silently hangs without calling onReadyForSpeech() or onResults(). Prioritizing explicit Google
   * Speech Services components first ensures reliable multilingual (bn-BD, en-US, hi-IN) recognition
   * while preserving automatic fallback to the system default and any other installed recognizer.
   */
  internal fun buildCandidateComponents(): List<ComponentName?> {
    return preloadCandidateComponents(appContext)
  }

  fun setLanguage(language: VoiceLanguage) {
    _uiState.update { current ->
      if (current.selectedLanguage == language) current
      else current.copy(selectedLanguage = language)
    }
  }

  fun startListening(
    language: VoiceLanguage = _uiState.value.selectedLanguage,
    forceExplicitComponent: Boolean = false,
  ) {
    runOnMainThread {
      if (_uiState.value.state == VoiceState.STARTING && speechRecognizer != null && !readyReceivedInAttempt) {
        Log.d(TAG, "startListening invoked while already STARTING; skipping duplicate call")
        return@runOnMainThread
      }
      clearAllTimers()
      readyReceivedInAttempt = false
      rmsLogCounter = 0
      finalResultDeliveredInSession = false
      startupRetryCount = 0
      safelyDestroyRecognizer()

      if (!isVoiceInputEnabled) {
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "startListening_disabled",
          details = "voiceInputEnabled=false",
        )
        Log.w(TAG, "startListening aborted: voiceInputEnabled is false")
        _uiState.update {
          it.copy(
            state = VoiceState.IDLE,
            errorMessage = "Voice input is disabled in settings",
          )
        }
        onListeningStateChanged?.invoke(false)
        return@runOnMainThread
      }

      val hasPermission = hasRecordAudioPermission()
      if (!hasPermission) {
        val msg = "Microphone permission required for voice typing"
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "startListening_permissionDenied",
          details = "hasRecordAudioPermission=false",
        )
        Log.w(TAG, "startListening aborted: $msg")
        _uiState.update {
          it.copy(
            state = VoiceState.ERROR,
            hasPermission = false,
            errorMessage = msg,
            selectedLanguage = language,
          )
        }
        onListeningStateChanged?.invoke(false)
        onPermissionRequired?.invoke()
        return@runOnMainThread
      }

      val isOnline = isNetworkConnected()
      if (isOnline) {
        // Network connectivity is healthy: automatically switch back to online/hybrid mode
        isPreferOfflineFallbackActive = false
        offlineFallbackRetryCount = 0
      } else {
        val msg = "No internet connection. Please reconnect to use voice typing."
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "startListening_noInternet",
          details = msg,
        )
        Log.w(TAG, msg)
        _uiState.update {
          it.copy(
            state = VoiceState.ERROR,
            errorMessage = msg,
            isOnline = false,
            selectedLanguage = language,
            partialTranscription = "",
            finalTranscription = "",
            rmsNormalized = 0f,
          )
        }
        onListeningStateChanged?.invoke(false)
        onErrorOccurred?.invoke(msg)
        return@runOnMainThread
      }

      val candidates = buildCandidateComponents()
      val nonNullCandidates = candidates.filterNotNull()
      if (candidates.isEmpty() || (nonNullCandidates.isEmpty() && !SpeechRecognizer.isRecognitionAvailable(appContext))) {
        val msg = "Speech recognition is not available on this device"
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "startListening_serviceUnavailable",
          details = msg,
        )
        Log.e(TAG, "startListening aborted: $msg")
        _uiState.update {
          it.copy(
            state = VoiceState.ERROR,
            isRecognitionAvailable = false,
            errorMessage = msg,
          )
        }
        onListeningStateChanged?.invoke(false)
        onErrorOccurred?.invoke(msg)
        return@runOnMainThread
      }

      candidateComponents =
        if (forceExplicitComponent && nonNullCandidates.isNotEmpty()) {
          nonNullCandidates + listOf(null)
        } else {
          candidates
        }
      currentCandidateIndex = 0
      finalResultDeliveredInSession = false
      offlineFallbackRetryCount = 0

      val audioInfo = if (isNoiseCancellationEnabled) {
        audioPreProcessor.start()
      } else {
        audioPreProcessor.stop()
        audioPreProcessor.getProcessingInfo()
      }

      _uiState.update {
        it.copy(
          state = VoiceState.STARTING,
          selectedLanguage = language,
          partialTranscription = "",
          finalTranscription = "",
          errorMessage = null,
          rmsNormalized = 0f,
          hasPermission = true,
          isRecognitionAvailable = true,
          isOnline = true,
          isOnDeviceActive = isPreferOfflineFallbackActive,
          audioProcessingInfo = audioInfo,
        )
      }

      startCandidateAt(index = 0, language = language)
    }
  }

  /**
   * Checks current network connectivity and automatically restores online/hybrid intent mode
   * when connectivity is healthy again.
   */
  fun refreshConnectivityMode(): Boolean {
    val online = isNetworkConnected()
    if (online) {
      isPreferOfflineFallbackActive = false
      offlineFallbackRetryCount = 0
      activeRecognitionIntent?.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
      _uiState.update {
        it.copy(
          isOnline = true,
          isOnDeviceActive = false,
        )
      }
    }
    return online
  }

  internal fun formatRecognizedOutput(
    rawText: String,
    language: VoiceLanguage = _uiState.value.selectedLanguage,
    autoPunctuate: Boolean = false,
  ): String {
    if (rawText.isBlank()) return rawText
    return VoicePostProcessor.process(
      rawText = rawText,
      language = language,
      isFinal = true,
      autoPunctuate = autoPunctuate,
    )
  }

  private fun startCandidateAt(index: Int, language: VoiceLanguage) {
    clearReadyWatchdog()
    readyReceivedInAttempt = false

    if (index >= candidateComponents.size) {
      safelyDestroyRecognizer()
      val msg = "Speech recognition could not be started with any installed service"
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "startCandidateAt_exhausted",
        details = "totalCandidates=${candidateComponents.size}",
      )
      Log.e(TAG, "All SpeechRecognizer candidates exhausted for lang=${language.tag}")
      _uiState.update {
        it.copy(
          state = VoiceState.ERROR,
          errorMessage = msg,
          rmsNormalized = 0f,
        )
      }
      onListeningStateChanged?.invoke(false)
      onErrorOccurred?.invoke(msg)
      return
    }

    currentCandidateIndex = index
    val candidate = candidateComponents[index]

    try {
      safelyDestroyRecognizer()

      val recognizer =
        if (candidate != null) {
          Log.d(
            TAG,
            "Creating SpeechRecognizer candidate[$index] with component: ${candidate.flattenToShortString()}",
          )
          SpeechRecognizer.createSpeechRecognizer(appContext, candidate)
        } else {
          Log.d(TAG, "Creating SpeechRecognizer candidate[$index] with system default")
          SpeechRecognizer.createSpeechRecognizer(appContext)
        }
      speechRecognizer = recognizer

      val candidateName = candidate?.flattenToShortString() ?: "SYSTEM_DEFAULT"
      val onlineHealthy = isNetworkConnected() && !isPreferOfflineFallbackActive

      _uiState.update {
        it.copy(
          activeEngineDescription = candidateName,
          isOnDeviceActive = isPreferOfflineFallbackActive,
          isOnline = onlineHealthy,
        )
      }

      val listener = createListener(language = language, candidate = candidate, candidateIndex = index)
      activeRecognitionListener = listener
      recognizer.setRecognitionListener(listener)

      val intent = buildRecognitionIntent(language, forcePreferOffline = isPreferOfflineFallbackActive)
      activeRecognitionIntent = intent
      recognizer.startListening(intent)

      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "startListening",
        details = "lang=${language.tag} candidateIndex=$index component=$candidateName mode=${recognitionMode.id} preferOffline=$isPreferOfflineFallbackActive",
      )
      Log.d(
        TAG,
        "SpeechRecognizer.startListening invoked: lang=${language.tag} candidateIndex=$index component=$candidateName mode=${recognitionMode.id} preferOffline=$isPreferOfflineFallbackActive",
      )

      val watchdog = Runnable {
        readyWatchdogRunnable = null
        if (!readyReceivedInAttempt && !finalResultDeliveredInSession) {
          val nextIndex = index + 1
          ImeDiagnosticsLogger.log(
            category = "VOICE_TYPING",
            event = "readyWatchdogTimeout",
            details = "candidateIndex=$index component=$candidateName nextIndex=$nextIndex",
          )
          Log.w(
            TAG,
            "RecognitionService candidate[$index] ($candidateName) did not call onReadyForSpeech within ${READY_WATCHDOG_TIMEOUT_MS}ms",
          )
          startCandidateAt(nextIndex, language)
        }
      }
      readyWatchdogRunnable = watchdog
      mainHandler.postDelayed(watchdog, READY_WATCHDOG_TIMEOUT_MS)
    } catch (e: Exception) {
      val candidateName = candidate?.flattenToShortString() ?: "SYSTEM_DEFAULT"
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "startListeningFailed",
        details = "candidateIndex=$index component=$candidateName error=${e.message ?: "unknown"}",
      )
      Log.e(TAG, "startCandidateAt[$index] ($candidateName) exception", e)
      val nextIndex = index + 1
      if (nextIndex < candidateComponents.size) {
        startCandidateAt(nextIndex, language)
      } else {
        safelyDestroyRecognizer()
        _uiState.update {
          it.copy(
            state = VoiceState.ERROR,
            errorMessage = e.message ?: "Could not start voice recognition",
          )
        }
        onListeningStateChanged?.invoke(false)
        onErrorOccurred?.invoke(e.message ?: "Could not start voice recognition")
      }
    }
  }

  internal fun buildRecognitionIntent(
    language: VoiceLanguage,
    forcePreferOffline: Boolean = isPreferOfflineFallbackActive,
  ): Intent {
    val isBengaliLocale = language.recognitionTag.startsWith("bn", ignoreCase = true)
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
      )
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.recognitionTag)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.recognitionTag)
      putExtra(
        RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
        if (isBengaliLocale && !language.isBanglishOutput) language.recognitionTag else false,
      )
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RECOGNITION_ALTERNATIVES)
      putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
      putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)

      // Avoid passing EXTRA_ADDITIONAL_LANGUAGES = ["en-US"] when dictating native Bengali (bn-BD),
      // as active dual-language LID switching causes Google Speech Services to misclassify short or
      // colloquial Bangladesh Bengali syllables (e.g., "পুন", "কই", "তো") as English lookalikes.
      if (!isBengaliLocale) {
        val additionalLangs = when {
          language.tag.startsWith("en", ignoreCase = true) -> arrayOf("bn-BD")
          language.tag.startsWith("hi", ignoreCase = true) -> arrayOf("en-US")
          else -> arrayOf("en-US")
        }
        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additionalLangs)
      }
      putExtra("android.speech.extra.ENABLE_FORMATTING", isAutoPunctuationEnabled)
      putExtra("android.speech.extra.ENABLE_BIASING_DEVICE_CONTEXT", true)

      val shouldPreferOffline = forcePreferOffline || !isNetworkConnected()
      putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, shouldPreferOffline)

      if (isVoiceEnhancementEnabled) {
        // Bengali-first & pause-tuned Voice Enhancement:
        // Preserves subtle Bengali word endings, monosyllabic words and natural conversational pauses
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
          600L,
        )
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
          1800L,
        )
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
          1400L,
        )
      } else {
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
          400L,
        )
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
          1500L,
        )
        putExtra(
          RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
          1200L,
        )
      }
    }
  }

  fun stopListening() {
    runOnMainThread {
      clearReadyWatchdog()
      clearPartialSilenceTimer()
      try {
        val currentPartial = _uiState.value.partialTranscription.trim()
        val activeRecognizer = speechRecognizer
        if (activeRecognizer == null) {
          clearStopFinalizeTimer()
          if (currentPartial.isNotEmpty() && !finalResultDeliveredInSession) {
            finalResultDeliveredInSession = true
            val formatted = formatRecognizedOutput(
              rawText = currentPartial,
              language = _uiState.value.selectedLanguage,
              autoPunctuate = isAutoPunctuationEnabled,
            )
            _uiState.update {
              it.copy(
                state = VoiceState.IDLE,
                finalTranscription = formatted,
                partialTranscription = formatted,
                rmsNormalized = 0f,
              )
            }
            onListeningStateChanged?.invoke(false)
            onSpeechRecognized(formatted)
          } else {
            _uiState.update { it.copy(state = VoiceState.IDLE, rmsNormalized = 0f) }
            onListeningStateChanged?.invoke(false)
          }
          return@runOnMainThread
        }

        _uiState.update { it.copy(state = VoiceState.PROCESSING) }
        activeRecognizer.stopListening()
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "stopListening",
          details = "partial='$currentPartial'",
        )
        Log.d(TAG, "SpeechRecognizer.stopListening invoked (partial='$currentPartial')")

        // If onResults doesn't fire within 800ms after stopListening(), commit any captured partial
        clearStopFinalizeTimer()
        val finalizeTask = Runnable {
          stopFinalizeRunnable = null
          if (!finalResultDeliveredInSession) {
            val latestPartial = _uiState.value.partialTranscription.trim()
            if (latestPartial.isNotEmpty()) {
              finalResultDeliveredInSession = true
              val formatted = formatRecognizedOutput(
                rawText = latestPartial,
                language = _uiState.value.selectedLanguage,
                autoPunctuate = isAutoPunctuationEnabled,
              )
              ImeDiagnosticsLogger.log(
                category = "VOICE_TYPING",
                event = "stopListening_autoCommitPartial",
                details = "raw='$latestPartial' formatted='$formatted'",
              )
              Log.d(TAG, "stopListening_autoCommitPartial: committing '$formatted'")
              _uiState.update {
                it.copy(
                  state = VoiceState.IDLE,
                  finalTranscription = formatted,
                  partialTranscription = formatted,
                  rmsNormalized = 0f,
                )
              }
              onListeningStateChanged?.invoke(false)
              onSpeechRecognized(formatted)
            } else {
              _uiState.update { it.copy(state = VoiceState.IDLE, rmsNormalized = 0f) }
              onListeningStateChanged?.invoke(false)
            }
            safelyDestroyRecognizer()
          }
        }
        stopFinalizeRunnable = finalizeTask
        mainHandler.postDelayed(finalizeTask, STOP_FINALIZE_TIMEOUT_MS)
      } catch (e: Exception) {
        Log.e(TAG, "stopListening exception", e)
        safelyDestroyRecognizer()
        _uiState.update { it.copy(state = VoiceState.IDLE, rmsNormalized = 0f) }
        onListeningStateChanged?.invoke(false)
      }
    }
  }

  fun cancel() {
    runOnMainThread {
      clearAllTimers()
      safelyDestroyRecognizer()
      _uiState.update {
        it.copy(
          state = VoiceState.IDLE,
          rmsNormalized = 0f,
          partialTranscription = "",
        )
      }
      onListeningStateChanged?.invoke(false)
    }
  }

  fun commitAndFinish() {
    runOnMainThread {
      clearAllTimers()
      val textToCommit =
        _uiState.value.partialTranscription.ifBlank { _uiState.value.finalTranscription }
      safelyDestroyRecognizer()
      _uiState.update {
        it.copy(
          state = VoiceState.IDLE,
          rmsNormalized = 0f,
        )
      }
      onListeningStateChanged?.invoke(false)
      if (textToCommit.isNotBlank() && !finalResultDeliveredInSession) {
        finalResultDeliveredInSession = true
        val formatted =
          formatRecognizedOutput(
            textToCommit,
            _uiState.value.selectedLanguage,
            autoPunctuate = isAutoPunctuationEnabled,
          )
        onSpeechRecognized(formatted)
      }
    }
  }

  fun destroy() {
    runOnMainThread {
      clearAllTimers()
      safelyDestroyRecognizer()
      audioPreProcessor.release()
      _uiState.update { it.copy(state = VoiceState.IDLE, rmsNormalized = 0f) }
    }
  }

  @androidx.annotation.VisibleForTesting
  internal fun simulateTranscription(rawText: String, isFinal: Boolean) {
    val language = _uiState.value.selectedLanguage
    val formatted =
      formatRecognizedOutput(
        rawText,
        language,
        autoPunctuate = isAutoPunctuationEnabled && isFinal,
      )
    if (isFinal) {
      clearAllTimers()
      safelyDestroyRecognizer()
      _uiState.update {
        it.copy(
          state = VoiceState.IDLE,
          finalTranscription = formatted,
          partialTranscription = formatted,
          rmsNormalized = 0f,
        )
      }
      onListeningStateChanged?.invoke(false)
      onSpeechRecognized(formatted)
    } else {
      markRecognitionActuallyStarted("simulateTranscription", language)
      _uiState.update {
        it.copy(
          state = VoiceState.LISTENING,
          partialTranscription = formatted,
        )
      }
      onSpeechPartial?.invoke(formatted)
    }
  }

  @androidx.annotation.VisibleForTesting
  internal fun simulateError(message: String) {
    clearAllTimers()
    safelyDestroyRecognizer()
    _uiState.update { it.copy(state = VoiceState.ERROR, errorMessage = message) }
    onListeningStateChanged?.invoke(false)
    onErrorOccurred?.invoke(message)
  }

  @androidx.annotation.VisibleForTesting
  internal fun simulateError(error: Int) {
    activeRecognitionListener?.onError(error)
  }

  private fun clearReadyWatchdog() {
    readyWatchdogRunnable?.let { mainHandler.removeCallbacks(it) }
    readyWatchdogRunnable = null
  }

  private fun clearPartialSilenceTimer() {
    partialSilenceRunnable?.let { mainHandler.removeCallbacks(it) }
    partialSilenceRunnable = null
  }

  private fun clearStopFinalizeTimer() {
    stopFinalizeRunnable?.let { mainHandler.removeCallbacks(it) }
    stopFinalizeRunnable = null
  }

  private fun clearAllTimers() {
    clearReadyWatchdog()
    clearPartialSilenceTimer()
    clearStopFinalizeTimer()
  }

  private fun schedulePartialSilenceAutoFinish() {
    clearPartialSilenceTimer()
    val task = Runnable {
      partialSilenceRunnable = null
      if (!finalResultDeliveredInSession && _uiState.value.state == VoiceState.LISTENING) {
        val captured = _uiState.value.partialTranscription.trim()
        if (captured.isNotEmpty()) {
          ImeDiagnosticsLogger.log(
            category = "VOICE_TYPING",
            event = "partialSilenceAutoStop",
            details = "text='$captured'",
          )
          Log.d(TAG, "partialSilenceAutoStop triggered for text: '$captured', requesting graceful stopListening()")
          stopListening()
        }
      }
    }
    partialSilenceRunnable = task
    mainHandler.postDelayed(task, PARTIAL_SILENCE_AUTO_STOP_MS)
  }

  private fun safelyDestroyRecognizer() {
    onRmsChanged?.invoke(0f)
    audioPreProcessor.stop()
    val recognizer = speechRecognizer
    speechRecognizer = null
    activeRecognitionListener = null
    if (recognizer != null) {
      try {
        recognizer.setRecognitionListener(null)
      } catch (_: Exception) {}
      try {
        recognizer.cancel()
      } catch (_: Exception) {}
      try {
        recognizer.destroy()
      } catch (_: Exception) {}
    }
  }

  private fun markRecognitionActuallyStarted(source: String, language: VoiceLanguage) {
    clearReadyWatchdog()
    if (!readyReceivedInAttempt) {
      readyReceivedInAttempt = true
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "recognitionStarted",
        details = "source=$source lang=${language.tag}",
      )
    }
    _uiState.update { current ->
      if (current.state == VoiceState.LISTENING && current.errorMessage == null) current
      else current.copy(state = VoiceState.LISTENING, errorMessage = null)
    }
    onListeningStateChanged?.invoke(true)
  }

  internal fun extractStringsFromBundle(bundle: Bundle?, key: String): List<String> {
    if (bundle == null || !bundle.containsKey(key)) return emptyList()
    bundle.getStringArrayList(key)?.let { list ->
      if (list.isNotEmpty()) return list.map { it.toString() }
    }
    bundle.getStringArray(key)?.let { arr ->
      if (arr.isNotEmpty()) return arr.map { it.toString() }
    }
    bundle.getCharSequenceArrayList(key)?.let { list ->
      if (list.isNotEmpty()) return list.map { it.toString() }
    }
    bundle.getCharSequenceArray(key)?.let { arr ->
      if (arr.isNotEmpty()) return arr.map { it.toString() }
    }
    bundle.getString(key)?.let { single ->
      if (single.isNotBlank()) return listOf(single)
    }
    return emptyList()
  }

  private fun createListener(
    language: VoiceLanguage,
    candidate: ComponentName?,
    candidateIndex: Int,
  ): RecognitionListener {
    val candidateName = candidate?.flattenToShortString() ?: "SYSTEM_DEFAULT"

    return object : RecognitionListener {
      override fun onReadyForSpeech(params: Bundle?) {
        if (activeRecognitionListener !== this) return
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "onReadyForSpeech",
          details = "lang=${language.tag} candidateIndex=$candidateIndex component=$candidateName",
        )
        markRecognitionActuallyStarted("onReadyForSpeech", language)
      }

      override fun onBeginningOfSpeech() {
        if (activeRecognitionListener !== this) return
        clearReadyWatchdog()
        clearPartialSilenceTimer()
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "onBeginningOfSpeech",
          details = "lang=${language.tag} candidateIndex=$candidateIndex component=$candidateName",
        )
        markRecognitionActuallyStarted("onBeginningOfSpeech", language)
      }

      override fun onRmsChanged(rmsdB: Float) {
        if (activeRecognitionListener !== this) return
        val safeRms = if (rmsdB < 0f) 0f else rmsdB
        val rawNormalized = (safeRms / 10f).coerceIn(0f, 1f)
        val normalized = if (isWaveformAndHapticsEnabled) rawNormalized else 0f
        val previous = _uiState.value.rmsNormalized
        if (
          kotlin.math.abs(normalized - previous) >= 0.05f ||
            (normalized == 0f && previous != 0f) ||
            (previous == 0f && normalized > 0f)
        ) {
          _uiState.update { it.copy(rmsNormalized = normalized) }
        }
        onRmsChanged?.invoke(normalized)
        rmsLogCounter++
        if (rmsLogCounter == 1 || rmsLogCounter % 20 == 0) {
          ImeDiagnosticsLogger.log(
            category = "VOICE_TYPING",
            event = "onRmsChanged",
            details = "rmsdB=$rmsdB normalized=$normalized waveformEnabled=$isWaveformAndHapticsEnabled state=${_uiState.value.state}",
          )
        }
      }

      override fun onBufferReceived(buffer: ByteArray?) {}

      override fun onEndOfSpeech() {
        if (activeRecognitionListener !== this) return
        clearReadyWatchdog()
        _uiState.update { it.copy(state = VoiceState.PROCESSING, rmsNormalized = 0f) }
        onRmsChanged?.invoke(0f)
      }

      override fun onError(error: Int) {
        if (activeRecognitionListener !== this) return
        clearReadyWatchdog()
        clearPartialSilenceTimer()
        clearStopFinalizeTimer()

        // Zero-Failure Offline Fallback Hack:
        // When ERROR_NETWORK or ERROR_NETWORK_TIMEOUT occurs, prevent failure/cancellation.
        // Dynamically set EXTRA_PREFER_OFFLINE = true and instantly restart speechRecognizer.startListening(intent)
        // in silent offline fallback mode with a retry guard (maximum MAX_OFFLINE_FALLBACK_RETRIES attempts).
        if (
          recognitionMode != VoiceRecognitionMode.ONLINE_ONLY &&
          (error == SpeechRecognizer.ERROR_NETWORK ||
            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) &&
            offlineFallbackRetryCount < MAX_OFFLINE_FALLBACK_RETRIES
        ) {
          offlineFallbackRetryCount++
          isPreferOfflineFallbackActive = true
          val intent =
            (activeRecognitionIntent ?: buildRecognitionIntent(language, forcePreferOffline = true)).apply {
              putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
          activeRecognitionIntent = intent
          readyReceivedInAttempt = false

          _uiState.update {
            it.copy(
              state = VoiceState.LISTENING,
              isOnDeviceActive = true,
              isOnline = false,
              errorMessage = null,
            )
          }
          onListeningStateChanged?.invoke(true)

          ImeDiagnosticsLogger.log(
            category = "VOICE_TYPING",
            event = "onError_offlineFallbackRetry",
            details = "error=$error attempt=$offlineFallbackRetryCount/$MAX_OFFLINE_FALLBACK_RETRIES lang=${language.tag} candidateIndex=$candidateIndex component=$candidateName",
          )
          Log.w(
            TAG,
            "Zero-Failure Offline Fallback: code=$error -> restarting with EXTRA_PREFER_OFFLINE=true (attempt $offlineFallbackRetryCount/$MAX_OFFLINE_FALLBACK_RETRIES)",
          )

          try {
            val activeRec = speechRecognizer
            if (activeRec != null) {
              clearReadyWatchdog()
              val retryIndex = candidateIndex
              val retryWatchdog = Runnable {
                readyWatchdogRunnable = null
                if (!readyReceivedInAttempt && !finalResultDeliveredInSession) {
                  ImeDiagnosticsLogger.log(
                    category = "VOICE_TYPING",
                    event = "readyWatchdogTimeout_offlineRetry",
                    details = "candidateIndex=$retryIndex attempt=$offlineFallbackRetryCount",
                  )
                  Log.w(
                    TAG,
                    "Offline fallback retry did not call onReadyForSpeech within ${READY_WATCHDOG_TIMEOUT_MS}ms",
                  )
                  val nextIndex = retryIndex + 1
                  if (nextIndex < candidateComponents.size) {
                    startCandidateAt(nextIndex, language)
                  } else {
                    safelyDestroyRecognizer()
                    _uiState.update {
                      it.copy(
                        state = VoiceState.ERROR,
                        errorMessage = "Speech recognition timed out",
                        rmsNormalized = 0f,
                      )
                    }
                    onListeningStateChanged?.invoke(false)
                    onErrorOccurred?.invoke("Speech recognition timed out")
                  }
                }
              }
              readyWatchdogRunnable = retryWatchdog
              mainHandler.postDelayed(retryWatchdog, READY_WATCHDOG_TIMEOUT_MS)

              activeRec.startListening(intent)
              return
            }
          } catch (e: Exception) {
            clearReadyWatchdog()
            Log.e(
              TAG,
              "Offline fallback startListening exception on attempt $offlineFallbackRetryCount",
              e,
            )
          }
        }

        val isNetworkIssue =
          error == SpeechRecognizer.ERROR_NETWORK ||
            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
            error == 12 /* ERROR_LANGUAGE_NOT_SUPPORTED */ ||
            error == 13 /* ERROR_LANGUAGE_UNAVAILABLE */

        val message =
          if (isNetworkIssue) {
            "No internet connection. Please reconnect to use voice typing."
          } else {
            when (error) {
              SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
              SpeechRecognizer.ERROR_CLIENT -> "Client error"
              SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
              SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
              SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
              SpeechRecognizer.ERROR_SERVER -> "Server error"
              SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
              14 -> "Speech recognition server disconnected"
              else -> "Speech recognition error ($error)"
            }
          }

        val currentPartial = _uiState.value.partialTranscription.trim()
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "onError",
          details = "code=$error msg='$message' partial='$currentPartial' candidateIndex=$candidateIndex component=$candidateName",
        )
        Log.e(
          TAG,
          "onError: code=$error ($message) partial='$currentPartial' candidateIndex=$candidateIndex component=$candidateName",
        )

        // If speech was already partially transcribed before timeout/no-match/client error, commit the partial text
        if (
          currentPartial.isNotEmpty() &&
            !finalResultDeliveredInSession &&
            (error == SpeechRecognizer.ERROR_NO_MATCH ||
              error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
              error == SpeechRecognizer.ERROR_CLIENT)
        ) {
          finalResultDeliveredInSession = true
          val formatted = formatRecognizedOutput(
            rawText = currentPartial,
            language = language,
            autoPunctuate = isAutoPunctuationEnabled,
          )
          ImeDiagnosticsLogger.log(
            category = "VOICE_TYPING",
            event = "onError_rescuedPartial",
            details = "raw='$currentPartial' formatted='$formatted'",
          )
          Log.d(TAG, "onError: rescuing non-empty partial transcription '$formatted'")
          safelyDestroyRecognizer()
          _uiState.update {
            it.copy(
              state = VoiceState.IDLE,
              finalTranscription = formatted,
              partialTranscription = formatted,
              rmsNormalized = 0f,
            )
          }
          onListeningStateChanged?.invoke(false)
          onSpeechRecognized(formatted)
          return
        }

        // If the current candidate failed before capturing speech and more candidates exist, try the next candidate
        val nextIndex = candidateIndex + 1
        val isServiceLevelFailure =
          error == SpeechRecognizer.ERROR_CLIENT ||
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
            error == SpeechRecognizer.ERROR_SERVER ||
            (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && hasRecordAudioPermission())

        if (isServiceLevelFailure && nextIndex < candidateComponents.size) {
          Log.w(
            TAG,
            "Candidate[$candidateIndex] ($candidateName) failed with error=$error; trying candidate[$nextIndex]",
          )
          startCandidateAt(nextIndex, language)
          return
        }

        // If all candidates exhausted with transient service-level error (ERROR_CLIENT / ERROR_RECOGNIZER_BUSY)
        // during cold start before speech began, retry primary candidate once after 120ms backoff
        if (
          isServiceLevelFailure &&
            !readyReceivedInAttempt &&
            startupRetryCount < 1 &&
            candidateComponents.isNotEmpty()
        ) {
          startupRetryCount++
          Log.w(
            TAG,
            "All candidates failed with transient service error=$error; retrying primary candidate after 120ms backoff",
          )
          mainHandler.postDelayed({
            if (_uiState.value.state == VoiceState.STARTING || _uiState.value.state == VoiceState.PROCESSING) {
              startCandidateAt(0, language)
            }
          }, 120L)
          return
        }

        safelyDestroyRecognizer()
        _uiState.update {
          it.copy(
            state = VoiceState.ERROR,
            errorMessage = message,
            rmsNormalized = 0f,
          )
        }
        onListeningStateChanged?.invoke(false)
        if (
          error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && !hasRecordAudioPermission()
        ) {
          onPermissionRequired?.invoke()
        } else {
          onErrorOccurred?.invoke(message)
        }
      }

      override fun onResults(results: Bundle?) {
        if (activeRecognitionListener !== this) return
        clearAllTimers()
        if (finalResultDeliveredInSession) {
          Log.d(TAG, "onResults: session already finalized, ignoring late callback")
          return
        }
        val matches =
          extractStringsFromBundle(results, SpeechRecognizer.RESULTS_RECOGNITION).ifEmpty {
            extractStringsFromBundle(results, RecognizerIntent.EXTRA_RESULTS)
          }
        val unstable = extractStringsFromBundle(results, EXTRA_UNSTABLE_TEXT)
        val confidenceScores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        val fallbackPartial = _uiState.value.partialTranscription.trim()

        val bestResult = selectBestHypothesis(
          matches = matches,
          unstable = unstable,
          fallbackPartial = fallbackPartial,
          language = language,
          confidenceScores = confidenceScores,
        )

        val formattedResult =
          formatRecognizedOutput(bestResult, language, autoPunctuate = isAutoPunctuationEnabled)

        // Reset retry guard after successful recognition; if network connectivity is healthy again,
        // automatically switch back to online/hybrid mode for subsequent sessions.
        offlineFallbackRetryCount = 0
        if (isNetworkConnected()) {
          isPreferOfflineFallbackActive = false
          activeRecognitionIntent?.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "onResults",
          details = "raw='$bestResult' formatted='$formattedResult' candidates=${matches.size} lang=${language.tag} isBanglish=${language.isBanglishOutput}",
        )
        Log.d(TAG, "onResults: raw='$bestResult' -> formatted='$formattedResult' (candidates=${matches.size}) for lang=${language.tag}")

        safelyDestroyRecognizer()
        _uiState.update {
          it.copy(
            state = VoiceState.IDLE,
            finalTranscription = formattedResult,
            partialTranscription = formattedResult,
            rmsNormalized = 0f,
            isOnline = isNetworkConnected(),
            isOnDeviceActive = isPreferOfflineFallbackActive,
          )
        }
        onListeningStateChanged?.invoke(false)

        if (formattedResult.isNotBlank() && !finalResultDeliveredInSession) {
          finalResultDeliveredInSession = true
          onSpeechRecognized(formattedResult)
        }
      }

      override fun onPartialResults(partialResults: Bundle?) {
        if (activeRecognitionListener !== this) return
        if (finalResultDeliveredInSession) {
          Log.d(TAG, "onPartialResults: session already finalized, ignoring late partial callback")
          return
        }
        val matches =
          extractStringsFromBundle(partialResults, SpeechRecognizer.RESULTS_RECOGNITION).ifEmpty {
            extractStringsFromBundle(partialResults, RecognizerIntent.EXTRA_RESULTS)
          }
        val unstable = extractStringsFromBundle(partialResults, EXTRA_UNSTABLE_TEXT)
        val stableText = selectBestPartialCandidate(matches, language)
        val unstableText = selectBestPartialCandidate(unstable, language)

        val bestPartial = mergeStreamingText(stableText, unstableText)
        if (bestPartial.isBlank()) return

        val formattedPartial = VoicePostProcessor.process(
          rawText = bestPartial,
          language = language,
          isFinal = false,
          autoPunctuate = false,
        )

        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "onPartialResults",
          details = "raw='$bestPartial' formatted='$formattedPartial' lang=${language.tag}",
        )

        markRecognitionActuallyStarted("onPartialResults", language)
        val prevPartial = _uiState.value.partialTranscription
        if (prevPartial != formattedPartial || _uiState.value.state != VoiceState.LISTENING) {
          _uiState.update {
            it.copy(
              partialTranscription = formattedPartial,
              state = VoiceState.LISTENING,
            )
          }
          onSpeechPartial?.invoke(formattedPartial)
        }

        schedulePartialSilenceAutoFinish()
      }

      override fun onEvent(eventType: Int, params: Bundle?) {}
    }
  }

  internal fun selectBestPartialCandidate(
    candidates: List<String>,
    language: VoiceLanguage,
  ): String {
    val clean = candidates.map { it.trim() }.filter { it.isNotBlank() }
    if (clean.isEmpty()) return ""
    if (clean.size == 1) return clean.first()
    if (language.recognitionTag.startsWith("bn", ignoreCase = true)) {
      val topWords = splitIntoWords(clean.first())
      val pureBengali = clean.firstOrNull { cand ->
        val words = splitIntoWords(cand)
        words.size >= topWords.size && words.isNotEmpty() && words.all { containsBengaliScript(it) }
      }
      if (pureBengali != null) return pureBengali
      val anyBengali = clean.firstOrNull { containsBengaliScript(it) }
      if (anyBengali != null) return anyBengali
    }
    return clean.first()
  }

  internal fun selectBestHypothesis(
    matches: List<String>,
    unstable: List<String>,
    fallbackPartial: String,
    language: VoiceLanguage,
    confidenceScores: FloatArray? = null,
  ): String {
    val isBengaliRecognition = language.recognitionTag.startsWith("bn", ignoreCase = true)
    val cleanMatches = matches
      .map { raw -> if (isBengaliRecognition) VoicePostProcessor.normalizeBengaliUnicode(raw.trim()) else raw.trim() }
      .filter { it.isNotBlank() }
    val cleanUnstable = unstable
      .map { raw -> if (isBengaliRecognition) VoicePostProcessor.normalizeBengaliUnicode(raw.trim()) else raw.trim() }
      .filter { it.isNotBlank() }
    val normalizedFallback =
      if (isBengaliRecognition && !language.isBanglishOutput) {
        VoicePostProcessor.normalizeBengaliUnicode(fallbackPartial.trim())
      } else {
        fallbackPartial.trim()
      }

    if (cleanMatches.isEmpty() && cleanUnstable.isEmpty()) {
      return normalizedFallback
    }
    if (cleanMatches.isEmpty()) {
      return cleanUnstable.first()
    }

    var selected = cleanMatches.first()
    val topConfidence = confidenceScores?.getOrNull(0) ?: -1f

    // 1. Bengali (bn-BD) intelligent N-best hypothesis evaluation
    if (isBengaliRecognition) {
      val topWords = splitIntoWords(selected)
      val topBengaliWords = topWords.count { containsBengaliScript(it) }

      // 1a. Check if an alternative hypothesis in N-best matches recovers a short Bengali word
      // that was dropped in matches[0] (e.g., "তুই কি মারতেছিলি" -> "তুই কি পুন মারতেছিলি")
      val recoveredDroppedWordMatch = cleanMatches.indices.drop(1).firstNotNullOfOrNull { idx ->
        val candidate = cleanMatches[idx]
        val candConf = confidenceScores?.getOrNull(idx) ?: -1f
        val confAcceptable = topConfidence <= 0f || candConf <= 0f || candConf >= (topConfidence - CONFIDENCE_MARGIN)
        if (confAcceptable && isWordSubsequenceWithBengaliInsertion(topWords, splitIntoWords(candidate))) {
          candidate
        } else {
          null
        }
      }

      if (recoveredDroppedWordMatch != null) {
        selected = recoveredDroppedWordMatch
      } else if (topBengaliWords < topWords.size || topBengaliWords == 0) {
        // 1b. If matches[0] has stray Latin misrecognitions of short Bengali words (e.g., "তুই কি phone মারতেছিলি"
        // or "Ki koro"), prefer an alternative hypothesis with higher Bengali word coverage and no word loss.
        val betterBengaliCandidate = cleanMatches.indices.drop(1).firstNotNullOfOrNull { idx ->
          val candidate = cleanMatches[idx]
          val candConf = confidenceScores?.getOrNull(idx) ?: -1f
          val confAcceptable = topConfidence <= 0f || candConf <= 0f || candConf >= (topConfidence - CONFIDENCE_MARGIN)
          val candWords = splitIntoWords(candidate)
          val candBengaliWords = candWords.count { containsBengaliScript(it) }
          val preservesLength = candWords.size >= topWords.size || topBengaliWords == 0
          val improvesBengaliCoverage =
            candBengaliWords > topBengaliWords &&
              (candBengaliWords == candWords.size || topBengaliWords == 0)
          if (confAcceptable && preservesLength && improvesBengaliCoverage) {
            candidate
          } else {
            null
          }
        }
        if (betterBengaliCandidate != null) {
          selected = betterBengaliCandidate
        }
      }

      // 1c. If streaming fallbackPartial captured a short Bengali word that all final hypotheses dropped,
      // recover the complete utterance from fallbackPartial.
      if (!language.isBanglishOutput && normalizedFallback.isNotBlank()) {
        val selectedWords = splitIntoWords(selected)
        val fallbackWords = splitIntoWords(normalizedFallback)
        if (isWordSubsequenceWithBengaliInsertion(selectedWords, fallbackWords)) {
          selected = normalizedFallback
        }
      }
    } else if (language.tag.startsWith("hi", ignoreCase = true)) {
      val devanagariMatch = cleanMatches.firstOrNull { candidate ->
        candidate.any { char -> char in '\u0900'..'\u097F' }
      }
      if (devanagariMatch != null) {
        selected = devanagariMatch
      }
    }

    // 2. Completeness check: if selected match is 1 word but fallbackPartial is a full sentence (>= 3 words),
    // prefer a candidate from matches that contains the full utterance.
    if (normalizedFallback.isNotBlank()) {
      val fallbackWordCount = splitIntoWords(normalizedFallback).size
      val selectedWordCount = splitIntoWords(selected).size
      if (selectedWordCount <= 1 && fallbackWordCount >= 3) {
        val completeMatch = cleanMatches.firstOrNull { candidate ->
          splitIntoWords(candidate).size >= fallbackWordCount - 1
        }
        if (completeMatch != null) {
          selected = completeMatch
        } else if (!language.isBanglishOutput || !containsBengaliScript(selected)) {
          selected = normalizedFallback
        }
      }
    }

    // 3. Merge any trailing unstable tokens not yet folded into the final result
    if (cleanUnstable.isNotEmpty()) {
      selected = mergeStreamingText(selected, cleanUnstable.first())
    }

    return selected
  }

  /**
   * Returns true if [shorterWords] is an ordered word-subsequence of [longerWords] with 1..2 extra
   * Bengali-script words inserted (recovering short conversational words dropped by beam rescoring).
   */
  internal fun isWordSubsequenceWithBengaliInsertion(
    shorterWords: List<String>,
    longerWords: List<String>,
  ): Boolean {
    if (shorterWords.isEmpty()) return false
    val diff = longerWords.size - shorterWords.size
    if (diff !in 1..2) return false

    var shortIdx = 0
    val insertedWords = mutableListOf<String>()
    for (longIdx in longerWords.indices) {
      val longWord = longerWords[longIdx]
      val normLong = longWord.trim('?', '!', '.', '।', ',')
      val normShort =
        if (shortIdx < shorterWords.size) shorterWords[shortIdx].trim('?', '!', '.', '।', ',') else ""

      if (shortIdx < shorterWords.size && normLong.equals(normShort, ignoreCase = true)) {
        shortIdx++
      } else {
        // Must not be an accidental stutter/duplicate of adjacent word
        val prevWord = if (longIdx > 0) longerWords[longIdx - 1].trim('?', '!', '.', '।', ',') else ""
        val nextWord =
          if (longIdx + 1 < longerWords.size) longerWords[longIdx + 1].trim('?', '!', '.', '।', ',') else ""
        if (normLong.equals(prevWord, ignoreCase = true) || normLong.equals(nextWord, ignoreCase = true)) {
          return false
        }
        insertedWords.add(normLong)
      }
    }

    if (shortIdx != shorterWords.size) return false
    return insertedWords.isNotEmpty() && insertedWords.all { containsBengaliScript(it) }
  }

  private fun splitIntoWords(text: String): List<String> {
    if (text.isBlank()) return emptyList()
    return text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
  }

  private fun containsBengaliScript(text: String): Boolean {
    for (ch in text) {
      if (ch in '\u0980'..'\u09FF') return true
    }
    return false
  }

  internal fun mergeStreamingText(stable: String, unstable: String): String {
    if (stable.isEmpty()) return unstable
    if (unstable.isEmpty()) return stable
    if (stable.equals(unstable, ignoreCase = true) || stable.endsWith(unstable, ignoreCase = true)) {
      return stable
    }
    if (unstable.startsWith(stable, ignoreCase = true)) {
      return unstable
    }
    val stableWords = stable.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val unstableWords = unstable.split("\\s+".toRegex()).filter { it.isNotBlank() }
    val maxOverlap = minOf(stableWords.size, unstableWords.size)
    for (overlapLen in maxOverlap downTo 1) {
      val stableSuffix = stableWords.takeLast(overlapLen).joinToString(" ")
      val unstablePrefix = unstableWords.take(overlapLen).joinToString(" ")
      if (stableSuffix.equals(unstablePrefix, ignoreCase = true)) {
        val remainingUnstable = unstableWords.drop(overlapLen).joinToString(" ")
        return if (remainingUnstable.isNotEmpty()) "$stable $remainingUnstable" else stable
      }
    }
    return "$stable $unstable"
  }

  private fun runOnMainThread(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      action()
    } else {
      mainHandler.post(action)
    }
  }

  companion object {
    private const val TAG = "VoiceRecognitionMgr"
    const val MAX_OFFLINE_FALLBACK_RETRIES = 2
    const val MAX_RECOGNITION_ALTERNATIVES = 10
    private const val CONFIDENCE_MARGIN = 0.25f
    private const val READY_WATCHDOG_TIMEOUT_MS = 2500L
    private const val PARTIAL_SILENCE_AUTO_STOP_MS = 2200L
    private const val STOP_FINALIZE_TIMEOUT_MS = 800L
    private const val EXTRA_UNSTABLE_TEXT = "android.speech.extra.UNSTABLE_TEXT"

    @Volatile
    private var cachedCandidateComponents: List<ComponentName?>? = null
    @Volatile
    private var cachedAppIdentityHash: Int = 0

    fun preloadCandidateComponents(context: Context): List<ComponentName?> {
      val appCtx = context.applicationContext ?: context
      val appHash = System.identityHashCode(appCtx)
      val cached = cachedCandidateComponents
      if (cached != null && cachedAppIdentityHash == appHash && cached.any { it != null }) {
        return cached
      }
      val candidates = queryCandidateComponents(appCtx)
      cachedCandidateComponents = candidates
      cachedAppIdentityHash = appHash
      return candidates
    }

    private fun queryCandidateComponents(appContext: Context): List<ComponentName?> {
      val result = mutableListOf<ComponentName?>()
      val defaultServiceSetting =
        runCatching {
          Settings.Secure.getString(appContext.contentResolver, "voice_recognition_service")
        }.getOrNull().orEmpty()

      try {
        val services =
          appContext.packageManager.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE),
            0,
          )
        val discoveredComponents =
          services.orEmpty().mapNotNull { resolveInfo ->
            val si = resolveInfo.serviceInfo ?: return@mapNotNull null
            if (si.packageName.isNullOrBlank() || si.name.isNullOrBlank()) null
            else ComponentName(si.packageName, si.name)
          }

        val summary = discoveredComponents.joinToString(",") { it.flattenToShortString() }
        ImeDiagnosticsLogger.log(
          category = "VOICE_TYPING",
          event = "resolveRecognitionComponent",
          details = "found=${discoveredComponents.size} defaultSetting='$defaultServiceSetting' services=[$summary]",
        )
        Log.d(
          TAG,
          "resolveRecognitionComponent: found=${discoveredComponents.size} defaultSetting='$defaultServiceSetting' services=[$summary]",
        )

        val googlePackageOrder =
          listOf(
            "com.google.android.tts",
            "com.google.android.googlequicksearchbox",
            "com.google.android.as",
            "com.google.android.voicesearch",
          )

        for (pkg in googlePackageOrder) {
          discoveredComponents
            .filter { it.packageName == pkg }
            .forEach { comp ->
              if (!result.contains(comp)) {
                result.add(comp)
              }
            }
        }

        if (defaultServiceSetting.isNotBlank()) {
          ComponentName.unflattenFromString(defaultServiceSetting)?.let { configuredComp ->
            val isXiaomiOem =
              configuredComp.packageName.contains("xiaomi", ignoreCase = true) ||
                configuredComp.packageName.contains("miui", ignoreCase = true) ||
                configuredComp.packageName.contains("mibrain", ignoreCase = true)
            if (!isXiaomiOem && !result.contains(configuredComp)) {
              result.add(configuredComp)
            }
          }
        }

        // Include null (system default SpeechRecognizer.createSpeechRecognizer(context)) as fallback
        result.add(null)

        // Include any remaining discovered non-Google / OEM components as last-resort fallbacks
        for (comp in discoveredComponents) {
          if (!result.contains(comp)) {
            result.add(comp)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "buildCandidateComponents query error", e)
        result.clear()
        result.add(null)
      }

      return result
    }
  }
}
