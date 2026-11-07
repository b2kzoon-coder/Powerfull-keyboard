package com.example.ime

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import androidx.core.os.UserManagerCompat
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.clipboard.ClipboardController
import com.example.emoji.EmojiTextUtil
import com.example.keyboard.KeyFeedbackType
import com.example.keyboard.KeyboardFeedbackManager
import com.example.keyboard.KeyboardSurface
import com.example.language.LanguageInputMethodKind
import com.example.language.LanguageManager
import com.example.language.hindi.HindiPhoneticEngine
import com.example.layout.EnterActionType
import com.example.layout.KeyAction
import com.example.layout.KeyboardMode
import com.example.layout.ShiftState
import android.util.Log
import com.example.settings.KeyboardPreferencesRepository
import com.example.theme.KeyboardThemeRepository
import com.example.translate.LanguageSelectorMode
import com.example.translate.SmartTranslatorManager
import com.example.translate.TranslateLanguageItem
import com.example.translate.TranslatorUiState
import com.example.voice.AudioPreProcessor
import com.example.voice.VoiceLanguage
import com.example.voice.VoicePermissionActivity
import com.example.voice.VoicePostProcessor
import com.example.voice.VoiceRecognitionManager
import com.example.voice.VoiceState
import com.example.voice.VoiceUiState
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ImeSessionState(
  val mode: KeyboardMode = KeyboardMode.ALPHABET,
  val shiftState: ShiftState = ShiftState.LOWERCASE,
  val enterActionType: EnterActionType = EnterActionType.ENTER,
  val isEmojiSearchFocused: Boolean = false,
  val emojiSearchQuery: String = "",
  val emojiSearchRawQuery: String = "",
  val emojiSearchCursor: Int = 0,
)

class PersonalKeyboardService :
  InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

  private val lifecycleRegistry = LifecycleRegistry(this)
  private val store = ViewModelStore()
  private val savedStateRegistryController = SavedStateRegistryController.create(this)

  override val lifecycle: Lifecycle
    get() = lifecycleRegistry

  override val viewModelStore: ViewModelStore
    get() = store

  override val savedStateRegistry: SavedStateRegistry
    get() = savedStateRegistryController.savedStateRegistry

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

  private lateinit var preferencesRepository: KeyboardPreferencesRepository
  private lateinit var clipboardController: ClipboardController
  private lateinit var feedbackManager: KeyboardFeedbackManager

  private var _voiceRecognitionManager: VoiceRecognitionManager? = null

  val isVoiceManagerInitialized: Boolean
    get() = _voiceRecognitionManager != null

  var voiceRecognitionManager: VoiceRecognitionManager
    get() =
      _voiceRecognitionManager
        ?: createVoiceRecognitionManager().also { _voiceRecognitionManager = it }
    set(value) {
      _voiceRecognitionManager = value
    }

  val audioPreProcessor: AudioPreProcessor
    get() = voiceRecognitionManager.audioPreProcessor

  val isOfflineFallbackActive: Boolean
    get() = _voiceRecognitionManager?.isPreferOfflineFallbackActive == true

  val offlineFallbackRetryCount: Int
    get() = _voiceRecognitionManager?.offlineFallbackRetryCount ?: 0

  private var _smartTranslatorManager: SmartTranslatorManager? = null
  val isTranslatorInitialized: Boolean
    get() = _smartTranslatorManager != null

  var smartTranslatorManager: SmartTranslatorManager
    get() =
      _smartTranslatorManager
        ?: SmartTranslatorManager(applicationContext ?: this).also { _smartTranslatorManager = it }
    set(value) {
      _smartTranslatorManager = value
    }

  val languageManager = LanguageManager()
  private var systemClipboard: ClipboardManager? = null

  private val _sessionState = MutableStateFlow(ImeSessionState())
  val sessionState: StateFlow<ImeSessionState> = _sessionState.asStateFlow()

  private val _isVoiceTypingOpen = MutableStateFlow(false)
  val isVoiceTypingOpen: StateFlow<Boolean> = _isVoiceTypingOpen.asStateFlow()
  private val _voiceListeningComposeState = androidx.compose.runtime.mutableStateOf(false)

  // Live audio waveform / pulse scale for microphone button (1.0f to 1.25f)
  private val mainHandler = Handler(Looper.getMainLooper())
  private val _voiceRmsScaleComposeState = androidx.compose.runtime.mutableFloatStateOf(1.0f)
  val voiceRmsScaleState: androidx.compose.runtime.State<Float> = _voiceRmsScaleComposeState
  private var rmsScaleAnimator: ValueAnimator? = null

  private fun triggerVoiceHaptic() {
    if (::preferencesRepository.isInitialized && preferencesRepository.config.value.voiceWaveformAndHaptics) {
      triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    }
  }

  fun handleVoiceRmsChanged(normalizedRms: Float) {
    val waveformEnabled =
      if (::preferencesRepository.isInitialized) {
        preferencesRepository.config.value.voiceWaveformAndHaptics
      } else {
        true
      }
    if (!_isVoiceTypingOpen.value || !waveformEnabled) {
      if (_voiceRmsScaleComposeState.floatValue != 1.0f) {
        resetVoiceRmsScale()
      }
      return
    }
    // Smoothly scale between 1.0f (silence) and 1.25f (active loud speech)
    val targetScale = 1.0f + (normalizedRms * 0.25f).coerceIn(0f, 0.25f)
    animateVoiceRmsScale(targetScale, durationMs = 50L)
  }

  fun resetVoiceRmsScale() {
    animateVoiceRmsScale(1.0f, durationMs = 80L)
  }

  fun animateVoiceRmsScale(targetScale: Float, durationMs: Long = 50L) {
    if (Looper.myLooper() != Looper.getMainLooper()) {
      mainHandler.post { animateVoiceRmsScale(targetScale, durationMs) }
      return
    }
    val waveformEnabled =
      if (::preferencesRepository.isInitialized) {
        preferencesRepository.config.value.voiceWaveformAndHaptics
      } else {
        true
      }
    if (!waveformEnabled && targetScale != 1.0f) {
      _voiceRmsScaleComposeState.floatValue = 1.0f
      return
    }
    val current = _voiceRmsScaleComposeState.floatValue
    if (kotlin.math.abs(current - targetScale) < 0.005f) {
      _voiceRmsScaleComposeState.floatValue = targetScale
      return
    }
    rmsScaleAnimator?.cancel()
    val anim = ValueAnimator.ofFloat(current, targetScale).apply {
      duration = durationMs
      interpolator = DecelerateInterpolator()
      addUpdateListener { animator ->
        _voiceRmsScaleComposeState.floatValue = (animator.animatedValue as? Float) ?: targetScale
      }
    }
    rmsScaleAnimator = anim
    anim.start()
  }

  private fun setVoiceTypingOpen(open: Boolean) {
    if (open && voiceFinalCommittedInUtterance) {
      resetVoiceUtteranceState()
    }
    if (!open) {
      resetVoiceRmsScale()
    }
    if (_isVoiceTypingOpen.value != open) {
      _isVoiceTypingOpen.value = open
    }
    if (_voiceListeningComposeState.value != open) {
      _voiceListeningComposeState.value = open
    }
  }

  private val _isLanguageSelectorOpen = MutableStateFlow(false)
  val isLanguageSelectorOpen: StateFlow<Boolean> = _isLanguageSelectorOpen.asStateFlow()

  private val _isTranslatorOpen = MutableStateFlow(false)
  val isTranslatorOpen: StateFlow<Boolean> = _isTranslatorOpen.asStateFlow()
  private var translationJob: kotlinx.coroutines.Job? = null

  private var lastShiftTapTimeMs: Long = 0L
  private var lastSpaceTapTimeMs: Long = 0L

  private var isWindowVisible: Boolean = false
  private var activeEditorPackageName: String? = null
  private var activeEditorFieldId: Int = 0
  private var consumedBackInEmojiSearch: Boolean = false

  // Active phonetic composing buffer for Hindi Hinglish mode
  private val hinglishComposingBuffer = StringBuilder()

  // Active prefix for Emoji Search Bar typing across English and Probhat
  private var emojiSearchCommittedPrefix: String = ""

  // Voice typing utterance state to coordinate setComposingText and commitText without duplication
  private var voiceLeadingSpaceResolved: Boolean = false
  private var voiceNeedsLeadingSpace: Boolean = false
  private var voiceHasComposingText: Boolean = false
  private var voiceFinalCommittedInUtterance: Boolean = false
  private var lastVoicePartialText: String = ""

  internal var testInputConnectionOverride: android.view.inputmethod.InputConnection? = null

  override fun getCurrentInputConnection(): android.view.inputmethod.InputConnection? {
    return testInputConnectionOverride ?: super.getCurrentInputConnection()
  }

  override fun onCreate() {
    super.onCreate()
    ImeDiagnosticsLogger.log("IME_LIFECYCLE", "onCreate", "service=${hashCode()}")
    savedStateRegistryController.performRestore(null)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

    preferencesRepository = KeyboardPreferencesRepository.getInstance(applicationContext)
    clipboardController = ClipboardController(applicationContext)
    feedbackManager = KeyboardFeedbackManager(applicationContext, preferencesRepository)

    val initialConfig = preferencesRepository.config.value
    languageManager.setActiveLanguage(initialConfig.activeLanguageId)

    serviceScope.launch {
      languageManager.activeLanguage.collect { activeLang ->
        if (preferencesRepository.config.value.activeLanguageId != activeLang.id) {
          preferencesRepository.setActiveLanguageId(activeLang.id)
        }
        val targetVoiceLang = VoiceLanguage.forLanguageId(activeLang.id)
        _voiceRecognitionManager?.let { mgr ->
          if (mgr.uiState.value.selectedLanguage != targetVoiceLang) {
            mgr.setLanguage(targetVoiceLang)
          }
        }
      }
    }
    serviceScope.launch {
      preferencesRepository.config.collect { cfg ->
        _voiceRecognitionManager?.updatePreferences(
          mode = cfg.voiceRecognitionMode,
          noiseCancellation = cfg.voiceNoiseCancellation,
          voiceEnhancement = cfg.voiceEnhancement,
          autoPunctuation = cfg.voiceAutoPunctuation,
          waveformAndHaptics = cfg.voiceWaveformAndHaptics,
          voiceInputEnabled = cfg.voiceInputEnabled,
        )
        if (!cfg.voiceInputEnabled && _isVoiceTypingOpen.value) {
          cancelActiveVoiceSessionIfNeeded()
        }
        if (!cfg.voiceWaveformAndHaptics && _voiceRmsScaleComposeState.floatValue != 1.0f) {
          resetVoiceRmsScale()
        }
        if (languageManager.activeLanguage.value.id != cfg.activeLanguageId) {
          languageManager.setActiveLanguage(cfg.activeLanguageId)
        }
      }
    }
    systemClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    if (!UserManagerCompat.isUserUnlocked(this)) {
      registerUserUnlockReceiverIfNeeded()
    }
  }

  private var userUnlockReceiver: BroadcastReceiver? = null

  private fun registerUserUnlockReceiverIfNeeded() {
    if (UserManagerCompat.isUserUnlocked(this)) return
    if (userUnlockReceiver != null) return
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        if (Intent.ACTION_USER_UNLOCKED == intent?.action) {
          unregisterUserUnlockReceiver()
          unlockAndReloadState()
        }
      }
    }
    userUnlockReceiver = receiver
    val filter = IntentFilter(Intent.ACTION_USER_UNLOCKED)
    try {
      registerReceiver(receiver, filter)
    } catch (e: Throwable) {
      Log.w("PersonalKeyboardService", "Failed to register ACTION_USER_UNLOCKED receiver", e)
    }
  }

  private fun unregisterUserUnlockReceiver() {
    userUnlockReceiver?.let {
      try {
        unregisterReceiver(it)
      } catch (_: Throwable) {}
      userUnlockReceiver = null
    }
  }

  fun unlockAndReloadState() {
    if (!UserManagerCompat.isUserUnlocked(this)) return
    if (::preferencesRepository.isInitialized) {
      val prefsReloaded = preferencesRepository.reloadIfUnlocked()
      if (prefsReloaded) {
        val savedLangId = preferencesRepository.config.value.activeLanguageId
        if (languageManager.activeLanguage.value.id != savedLangId) {
          languageManager.setActiveLanguage(savedLangId)
        }
      }
    }
    if (::clipboardController.isInitialized) {
      clipboardController.reloadIfUnlocked()
    }
  }

  private fun createVoiceRecognitionManager(): VoiceRecognitionManager {
    val cfg =
      if (::preferencesRepository.isInitialized) preferencesRepository.config.value else null
    val mgr =
      VoiceRecognitionManager(
        context = this,
        onSpeechRecognized = ::commitVoiceText,
        onSpeechPartial = ::handleVoicePartial,
        onErrorOccurred = ::handleVoiceError,
        onPermissionRequired = ::handleVoicePermissionRequired,
        onListeningStateChanged = { listening ->
          setVoiceTypingOpen(listening)
          if (!listening) {
            resetVoiceRmsScale()
          }
        },
        onRmsChanged = ::handleVoiceRmsChanged,
      )
    if (cfg != null) {
      mgr.updatePreferences(
        mode = cfg.voiceRecognitionMode,
        noiseCancellation = cfg.voiceNoiseCancellation,
        voiceEnhancement = cfg.voiceEnhancement,
        autoPunctuation = cfg.voiceAutoPunctuation,
        waveformAndHaptics = cfg.voiceWaveformAndHaptics,
        voiceInputEnabled = cfg.voiceInputEnabled,
      )
    }
    mgr.setLanguage(VoiceLanguage.forLanguageId(languageManager.activeLanguage.value.id))
    return mgr
  }

  override fun onCreateInputView(): View {
    ImeDiagnosticsLogger.log(
      category = "IME_LIFECYCLE",
      event = "onCreateInputView",
      details = "mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    window?.window?.decorView?.let { decorView ->
      decorView.setViewTreeLifecycleOwner(this)
      decorView.setViewTreeViewModelStoreOwner(this)
      decorView.setViewTreeSavedStateRegistryOwner(this)
    }

    val composeView =
      ComposeView(this).apply {
        isFocusable = false
        isFocusableInTouchMode = false
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        setViewTreeLifecycleOwner(this@PersonalKeyboardService)
        setViewTreeViewModelStoreOwner(this@PersonalKeyboardService)
        setViewTreeSavedStateRegistryOwner(this@PersonalKeyboardService)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

        setContent {
          val userConfig by preferencesRepository.config.collectAsState()
          val session by sessionState.collectAsState()
          val activeLanguage by languageManager.activeLanguage.collectAsState()
          val availableLanguages by languageManager.availableLanguages.collectAsState()
          val clipboardEntries by clipboardController.entries.collectAsState()
          val isLanguageSelectorOpen by isLanguageSelectorOpen.collectAsState()
          val isTranslatorOpen by isTranslatorOpen.collectAsState()
          val translatorUiState =
            if (isTranslatorOpen) {
              smartTranslatorManager.uiState.collectAsState().value
            } else {
              DefaultTranslatorUiState
            }

          val palette =
            remember(userConfig.themePreset) {
              KeyboardThemeRepository.paletteFor(userConfig.themePreset)
            }

          val onKeyActionCallback: (KeyAction) -> Unit = remember { ::handleKeyAction }
          val onCommitDirectTextCallback: (String) -> Unit = remember { ::commitDirectText }
          val onToggleNumberRowCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              preferencesRepository.toggleNumberRow()
            }
          }
          val onSwitchLanguageCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              commitAllPhoneticBuffers()
              languageManager.switchToNextLanguage()
            }
          }
          val onMoveCursorLeftCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              val current = _sessionState.value
              if (current.mode == KeyboardMode.EMOJI && current.isEmojiSearchFocused) {
                moveEmojiSearchCursorLeft()
              } else {
                commitAllPhoneticBuffers()
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
              }
            }
          }
          val onMoveCursorRightCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              val current = _sessionState.value
              if (current.mode == KeyboardMode.EMOJI && current.isEmojiSearchFocused) {
                moveEmojiSearchCursorRight()
              } else {
                commitAllPhoneticBuffers()
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
              }
            }
          }
          val onCycleThemeCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              preferencesRepository.cycleThemePreset()
            }
          }
          val onTogglePinClipCallback: (Long) -> Unit = remember { clipboardController::togglePin }
          val onDeleteClipCallback: (Long) -> Unit = remember { clipboardController::removeEntry }
          val onClearUnpinnedClipsCallback: () -> Unit = remember {
            clipboardController::clearUnpinned
          }
          val onOpenSettingsCallback: () -> Unit = remember { ::launchSettingsActivity }
          val onEmojiSearchFocusChangeCallback: (Boolean) -> Unit = remember {
            ::setEmojiSearchFocused
          }
          val onEmojiSearchQueryChangeCallback: (String) -> Unit = remember {
            ::setEmojiSearchQuery
          }
          val onEmojiSearchCursorChangeCallback: (Int) -> Unit = remember {
            ::setEmojiSearchCursor
          }
          val onSelectLanguageCallback: (String) -> Unit = remember {
            { id ->
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              commitAllPhoneticBuffers()
              languageManager.setActiveLanguage(id)
            }
          }
          val onToggleLanguageSelectorCallback: () -> Unit = remember { ::toggleLanguageSelector }
          val onToggleVoiceTypingCallback: () -> Unit = remember { ::toggleVoiceTyping }
          val onSelectVoiceLanguageCallback: (VoiceLanguage) -> Unit = remember {
            { selectedLang ->
              val targetTypingLangId = when {
                selectedLang.isBanglishOutput -> LanguageManager.ID_BANGLISH
                selectedLang.tag == "bn-BD" -> LanguageManager.ID_BANGLA_PROBHAT
                selectedLang.tag == "hi-IN" -> LanguageManager.ID_HINDI_HINGLISH
                else -> LanguageManager.ID_ENGLISH
              }
              languageManager.setActiveLanguage(targetTypingLangId)
            }
          }
          val onStartVoiceListeningCallback: () -> Unit = remember {
            {
              if (preferencesRepository.config.value.voiceInputEnabled) {
                triggerVoiceHaptic()
                voiceRecognitionManager.startListening()
              }
            }
          }
          val onStopVoiceListeningCallback: () -> Unit = remember {
            {
              triggerVoiceHaptic()
              voiceRecognitionManager.stopListening()
            }
          }
          val onCancelVoiceCallback: () -> Unit = remember {
            {
              triggerVoiceHaptic()
              voiceRecognitionManager.cancel()
              setVoiceTypingOpen(false)
            }
          }
          val onInsertVoiceCallback: () -> Unit = remember {
            {
              triggerVoiceHaptic()
              voiceRecognitionManager.commitAndFinish()
              setVoiceTypingOpen(false)
            }
          }
          val onToggleTranslatorCallback: () -> Unit = remember { ::toggleTranslator }
          val onTranslatorInputTextChangeCallback: (String) -> Unit = remember {
            { smartTranslatorManager.setInputText(it) }
          }
          val onTranslatorClearInputCallback: () -> Unit = remember {
            { smartTranslatorManager.clearInputText() }
          }
          val onTranslatorPasteFromClipboardCallback: () -> Unit = remember {
            ::pasteIntoTranslator
          }
          val onTranslatorTranslateCallback: () -> Unit = remember { ::executeTranslation }
          val onTranslatorSwapLanguagesCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              smartTranslatorManager.swapLanguages()
            }
          }
          val onTranslatorOpenSourceSelectorCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              smartTranslatorManager.openSelector(LanguageSelectorMode.SOURCE)
            }
          }
          val onTranslatorOpenTargetSelectorCallback: () -> Unit = remember {
            {
              triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
              smartTranslatorManager.openSelector(LanguageSelectorMode.TARGET)
            }
          }
          val onTranslatorSelectLanguageCallback: (TranslateLanguageItem) -> Unit = remember {
            ::selectTranslatorLanguage
          }
          val onTranslatorCloseSelectorCallback: () -> Unit = remember {
            { smartTranslatorManager.closeSelector() }
          }
          val onTranslatorSearchQueryChangeCallback: (String) -> Unit = remember {
            { smartTranslatorManager.setLanguageSearchQuery(it) }
          }
          val onTranslatorCopyTranslationCallback: (String) -> Unit = remember {
            ::copyTranslationToClipboard
          }
          val onTranslatorInsertTranslationCallback: (String) -> Unit = remember {
            ::insertTranslation
          }

          KeyboardSurface(
            mode = session.mode,
            shiftState = session.shiftState,
            enterActionType = session.enterActionType,
            portraitHeightPercent = userConfig.portraitHeightPercent,
            landscapeHeightPercent = userConfig.landscapeHeightPercent,
            showNumberRow = userConfig.showNumberRow,
            showToolbar = userConfig.showToolbar,
            cutCopyPasteEnabled = userConfig.cutCopyPasteEnabled,
            activeLanguageLabel = activeLanguage.spacebarLabel,
            activeLanguageBadge = activeLanguage.badgeLabel,
            activeLanguageKind = activeLanguage.kind,
            palette = palette,
            clipboardEntries = clipboardEntries,
            onKeyAction = onKeyActionCallback,
            onCommitDirectText = onCommitDirectTextCallback,
            onToggleNumberRow = onToggleNumberRowCallback,
            onSwitchLanguage = onSwitchLanguageCallback,
            onMoveCursorLeft = onMoveCursorLeftCallback,
            onMoveCursorRight = onMoveCursorRightCallback,
            onCycleTheme = onCycleThemeCallback,
            onTogglePinClip = onTogglePinClipCallback,
            onDeleteClip = onDeleteClipCallback,
            onClearUnpinnedClips = onClearUnpinnedClipsCallback,
            onOpenSettings = onOpenSettingsCallback,
            isEmojiSearchFocused = session.isEmojiSearchFocused,
            onEmojiSearchFocusChange = onEmojiSearchFocusChangeCallback,
            emojiSearchQuery = session.emojiSearchQuery,
            emojiSearchRawQuery = session.emojiSearchRawQuery,
            onEmojiSearchQueryChange = onEmojiSearchQueryChangeCallback,
            emojiSearchCursor = session.emojiSearchCursor,
            onEmojiSearchCursorChange = onEmojiSearchCursorChangeCallback,
            activeLanguageId = activeLanguage.id,
            availableLanguages = availableLanguages,
            onSelectLanguage = onSelectLanguageCallback,
            isLanguageSelectorOpen = isLanguageSelectorOpen,
            onToggleLanguageSelector = onToggleLanguageSelectorCallback,
            voiceListeningState = _voiceListeningComposeState,
            voiceRmsScaleState = _voiceRmsScaleComposeState,
            onToggleVoiceTyping = onToggleVoiceTypingCallback,
            onSelectVoiceLanguage = onSelectVoiceLanguageCallback,
            onStartVoiceListening = onStartVoiceListeningCallback,
            onStopVoiceListening = onStopVoiceListeningCallback,
            onCancelVoice = onCancelVoiceCallback,
            onInsertVoice = onInsertVoiceCallback,
            isTranslatorOpen = isTranslatorOpen,
            onToggleTranslator = onToggleTranslatorCallback,
            translatorUiState = translatorUiState,
            onTranslatorInputTextChange = onTranslatorInputTextChangeCallback,
            onTranslatorClearInput = onTranslatorClearInputCallback,
            onTranslatorPasteFromClipboard = onTranslatorPasteFromClipboardCallback,
            onTranslatorTranslate = onTranslatorTranslateCallback,
            onTranslatorSwapLanguages = onTranslatorSwapLanguagesCallback,
            onTranslatorOpenSourceSelector = onTranslatorOpenSourceSelectorCallback,
            onTranslatorOpenTargetSelector = onTranslatorOpenTargetSelectorCallback,
            onTranslatorSelectLanguage = onTranslatorSelectLanguageCallback,
            onTranslatorCloseSelector = onTranslatorCloseSelectorCallback,
            onTranslatorSearchQueryChange = onTranslatorSearchQueryChangeCallback,
            onTranslatorCopyTranslation = onTranslatorCopyTranslationCallback,
            onTranslatorInsertTranslation = onTranslatorInsertTranslationCallback,
          )
        }
      }

    return object : android.widget.FrameLayout(this) {
      override fun requestFocus(direction: Int, previouslyFocusedRect: Rect?): Boolean {
        // Never allow child views in the IME window to steal focus from the host editor
        return false
      }

      override fun clearFocus() {
        // Ignore clearFocus calls originating from AndroidComposeView inside the IME window
      }

      override fun onAttachedToWindow() {
        var current: View? = this
        while (current != null) {
          current.setViewTreeLifecycleOwner(this@PersonalKeyboardService)
          current.setViewTreeViewModelStoreOwner(this@PersonalKeyboardService)
          current.setViewTreeSavedStateRegistryOwner(this@PersonalKeyboardService)
          current = current.parent as? View
        }
        rootView?.let { root ->
          root.setViewTreeLifecycleOwner(this@PersonalKeyboardService)
          root.setViewTreeViewModelStoreOwner(this@PersonalKeyboardService)
          root.setViewTreeSavedStateRegistryOwner(this@PersonalKeyboardService)
        }
        super.onAttachedToWindow()
      }
    }.apply {
      isFocusable = false
      isFocusableInTouchMode = false
      descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
      setViewTreeLifecycleOwner(this@PersonalKeyboardService)
      setViewTreeViewModelStoreOwner(this@PersonalKeyboardService)
      setViewTreeSavedStateRegistryOwner(this@PersonalKeyboardService)
      addView(composeView)
    }
  }

  override fun onComputeInsets(outInsets: Insets?) {
    super.onComputeInsets(outInsets)
    if (outInsets != null) {
      outInsets.contentTopInsets = outInsets.visibleTopInsets
      outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_VISIBLE
    }
  }

  override fun onWindowShown() {
    super.onWindowShown()
    isWindowVisible = true
    if (UserManagerCompat.isUserUnlocked(this)) {
      unregisterUserUnlockReceiver()
      unlockAndReloadState()
    }
    val persistedLangId = preferencesRepository.config.value.activeLanguageId
    if (languageManager.activeLanguage.value.id != persistedLangId) {
      languageManager.setActiveLanguage(persistedLangId)
    }
    ImeDiagnosticsLogger.log(
      category = "WINDOW_VISIBILITY",
      event = "onWindowShown",
      details = "mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    clipboardController.syncFromSystemClipboard()
  }

  override fun onWindowHidden() {
    isWindowVisible = false
    consumedBackInEmojiSearch = false
    ImeDiagnosticsLogger.log(
      category = "WINDOW_VISIBILITY",
      event = "onWindowHidden",
      details = "mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    commitAllPhoneticBuffers()
    resetEmojiSearchState()
    _voiceRecognitionManager?.cancel()
    closeTranslatorPanel()
    setVoiceTypingOpen(false)
    if (_isLanguageSelectorOpen.value) {
      _isLanguageSelectorOpen.value = false
    }
    _sessionState.update { current ->
      if (
        !current.isEmojiSearchFocused &&
          current.emojiSearchQuery.isEmpty() &&
          current.emojiSearchRawQuery.isEmpty() &&
          current.emojiSearchCursor == 0
      ) {
        current
      } else {
        current.copy(
          isEmojiSearchFocused = false,
          emojiSearchQuery = "",
          emojiSearchRawQuery = "",
          emojiSearchCursor = 0,
        )
      }
    }
    super.onWindowHidden()
  }

  override fun hideWindow() {
    ImeDiagnosticsLogger.log(
      category = "WINDOW_VISIBILITY",
      event = "hideWindow",
      details = "mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    super.hideWindow()
  }

  override fun requestHideSelf(flags: Int) {
    ImeDiagnosticsLogger.log(
      category = "WINDOW_VISIBILITY",
      event = "requestHideSelf",
      details = "flags=$flags mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    super.requestHideSelf(flags)
  }

  override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
    super.onStartInput(attribute, restarting)
    val ic = currentInputConnection
    ImeDiagnosticsLogger.log(
      category = "INPUT_CONNECTION",
      event = "onStartInput",
      details = "restarting=$restarting pkg=${attribute?.packageName} fieldId=${attribute?.fieldId} icValid=${ic != null}",
    )
  }

  override fun onFinishInputView(finishingInput: Boolean) {
    ImeDiagnosticsLogger.log(
      category = "IME_LIFECYCLE",
      event = "onFinishInputView",
      details = "finishingInput=$finishingInput mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    super.onFinishInputView(finishingInput)
  }

  override fun onFinishInput() {
    ImeDiagnosticsLogger.log(
      category = "INPUT_CONNECTION",
      event = "onFinishInput",
      details = "mode=${_sessionState.value.mode} searchFocused=${_sessionState.value.isEmojiSearchFocused}",
    )
    super.onFinishInput()
  }

  override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
    super.onStartInputView(editorInfo, restarting)
    isWindowVisible = true
    if (UserManagerCompat.isUserUnlocked(this)) {
      unregisterUserUnlockReceiver()
      unlockAndReloadState()
    }
    if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.CREATED)) {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    val current = _sessionState.value
    val isSameEditor =
      editorInfo == null ||
        (editorInfo.packageName == activeEditorPackageName &&
          editorInfo.fieldId == activeEditorFieldId)

    ImeDiagnosticsLogger.log(
      category = "IME_LIFECYCLE",
      event = "onStartInputView",
      details = "restarting=$restarting isSameEditor=$isSameEditor isWindowVisible=$isWindowVisible mode=${current.mode} searchFocused=${current.isEmojiSearchFocused} pkg=${editorInfo?.packageName}",
    )

    if (editorInfo != null) {
      activeEditorPackageName = editorInfo.packageName
      activeEditorFieldId = editorInfo.fieldId
    }

    if (current.mode == KeyboardMode.EMOJI && current.isEmojiSearchFocused) {
      ImeDiagnosticsLogger.log(
        category = "EMOJI_SEARCH",
        event = "preserveSearchOnStartInputView",
        details = "restarting=$restarting query='${current.emojiSearchQuery}'",
      )
      return
    }
    if (current.mode == KeyboardMode.EMOJI && (restarting || (isWindowVisible && isSameEditor))) {
      ImeDiagnosticsLogger.log(
        category = "MODE_TRANSITION",
        event = "preserveEmojiModeOnStartInputView",
        details = "restarting=$restarting isSameEditor=$isSameEditor",
      )
      return
    }

    commitAllPhoneticBuffers()
    resetEmojiSearchState()

    val inputType = editorInfo?.inputType ?: InputType.TYPE_CLASS_TEXT
    val inputClass = inputType and InputType.TYPE_MASK_CLASS

    val targetMode =
      when (inputClass) {
        InputType.TYPE_CLASS_NUMBER,
        InputType.TYPE_CLASS_PHONE,
        InputType.TYPE_CLASS_DATETIME -> KeyboardMode.NUMBER_PAD
        else -> KeyboardMode.ALPHABET
      }

    val enterAction = resolveEnterAction(editorInfo)
    val initialShift = computeAutoCapShiftState(editorInfo)

    if (current.mode != targetMode) {
      ImeDiagnosticsLogger.log(
        category = "MODE_TRANSITION",
        event = "onStartInputViewModeReset",
        details = "${current.mode} -> $targetMode",
      )
    }

    _sessionState.update { state ->
      if (
        state.mode == targetMode &&
          state.shiftState == initialShift &&
          state.enterActionType == enterAction &&
          !state.isEmojiSearchFocused &&
          state.emojiSearchQuery.isEmpty() &&
          state.emojiSearchRawQuery.isEmpty() &&
          state.emojiSearchCursor == 0
      ) {
        state
      } else {
        state.copy(
          mode = targetMode,
          shiftState = initialShift,
          enterActionType = enterAction,
          isEmojiSearchFocused = false,
          emojiSearchQuery = "",
          emojiSearchRawQuery = "",
          emojiSearchCursor = 0,
        )
      }
    }
  }

  override fun onEvaluateFullscreenMode(): Boolean {
    return false
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    val current = _sessionState.value
    if (current.mode == KeyboardMode.EMOJI && current.isEmojiSearchFocused) {
      when (keyCode) {
        KeyEvent.KEYCODE_BACK -> {
          consumedBackInEmojiSearch = true
          ImeDiagnosticsLogger.log(
            category = "EMOJI_SEARCH",
            event = "onKeyDown_BACK",
            details = "dismissing emoji search keyboard",
          )
          setEmojiSearchFocused(false)
          return true
        }
        KeyEvent.KEYCODE_DEL -> {
          handleKeyAction(KeyAction.Backspace)
          return true
        }
        KeyEvent.KEYCODE_SPACE -> {
          handleKeyAction(KeyAction.Space)
          return true
        }
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
          handleKeyAction(KeyAction.Enter)
          return true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> {
          moveEmojiSearchCursorLeft()
          return true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT -> {
          moveEmojiSearchCursorRight()
          return true
        }
        else -> {
          if (event != null && event.isPrintingKey) {
            val unicodeChar = event.getUnicodeChar(event.metaState)
            if (unicodeChar != 0) {
              val ch = String(Character.toChars(unicodeChar))
              handleKeyAction(KeyAction.CommitChar(ch.lowercase(), ch.uppercase()))
              return true
            }
          }
        }
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
    if (keyCode == KeyEvent.KEYCODE_BACK && consumedBackInEmojiSearch) {
      consumedBackInEmojiSearch = false
      ImeDiagnosticsLogger.log(
        category = "EMOJI_SEARCH",
        event = "onKeyUp_BACK",
        details = "consumed back key release after dismissing emoji search keyboard",
      )
      return true
    }
    return super.onKeyUp(keyCode, event)
  }

  fun setEmojiSearchFocused(focused: Boolean) {
    val previous = _sessionState.value
    if (focused && !previous.isEmojiSearchFocused) {
      emojiSearchCommittedPrefix = previous.emojiSearchQuery
    }
    ImeDiagnosticsLogger.log(
      category = "EMOJI_SEARCH",
      event = "setEmojiSearchFocused",
      details = "${previous.isEmojiSearchFocused} -> $focused query='${previous.emojiSearchQuery}'",
    )
    _sessionState.update { current ->
      val nextMode = if (focused) KeyboardMode.EMOJI else current.mode
      val nextShift =
        if (focused && !previous.isEmojiSearchFocused) ShiftState.LOWERCASE
        else current.shiftState
      val nextCursor = current.emojiSearchQuery.length
      if (
        current.mode == nextMode &&
          current.shiftState == nextShift &&
          current.isEmojiSearchFocused == focused &&
          current.emojiSearchCursor == nextCursor
      ) {
        current
      } else {
        current.copy(
          mode = nextMode,
          shiftState = nextShift,
          isEmojiSearchFocused = focused,
          emojiSearchCursor = nextCursor,
        )
      }
    }
  }

  fun setEmojiSearchQuery(query: String) {
    emojiSearchCommittedPrefix = query
    ImeDiagnosticsLogger.log(
      category = "EMOJI_SEARCH",
      event = "setEmojiSearchQuery",
      details = "query='$query' cursor=${query.length}",
    )
    _sessionState.update { current ->
      if (
        current.emojiSearchQuery == query &&
          current.emojiSearchRawQuery == query &&
          current.emojiSearchCursor == query.length
      ) {
        current
      } else {
        current.copy(
          emojiSearchQuery = query,
          emojiSearchRawQuery = query,
          emojiSearchCursor = query.length,
        )
      }
    }
  }

  fun setEmojiSearchCursor(cursor: Int) {
    val currentQuery = _sessionState.value.emojiSearchQuery
    val clamped = cursor.coerceIn(0, currentQuery.length)
    ImeDiagnosticsLogger.log(
      category = "EMOJI_SEARCH",
      event = "setEmojiSearchCursor",
      details = "cursor=$clamped queryLength=${currentQuery.length}",
    )
    _sessionState.update { current ->
      if (current.emojiSearchCursor == clamped) current
      else current.copy(emojiSearchCursor = clamped)
    }
  }

  fun moveEmojiSearchCursorLeft() {
    val current = _sessionState.value
    val query = current.emojiSearchQuery
    val cursor = current.emojiSearchCursor.coerceIn(0, query.length)
    if (cursor > 0) {
      val step = EmojiTextUtil.calculateGraphemeLengthToDelete(query.substring(0, cursor))
      val nextCursor = (cursor - step).coerceAtLeast(0)
      ImeDiagnosticsLogger.log(
        category = "EMOJI_SEARCH",
        event = "moveCursorLeft",
        details = "$cursor -> $nextCursor",
      )
      _sessionState.update { it.copy(emojiSearchCursor = nextCursor) }
    }
  }

  fun moveEmojiSearchCursorRight() {
    val current = _sessionState.value
    val query = current.emojiSearchQuery
    val cursor = current.emojiSearchCursor.coerceIn(0, query.length)
    if (cursor < query.length) {
      val remaining = query.substring(cursor)
      val step = EmojiTextUtil.calculateFirstGraphemeLength(remaining)
      val nextCursor = (cursor + step).coerceAtMost(query.length)
      ImeDiagnosticsLogger.log(
        category = "EMOJI_SEARCH",
        event = "moveCursorRight",
        details = "$cursor -> $nextCursor",
      )
      _sessionState.update { it.copy(emojiSearchCursor = nextCursor) }
    }
  }

  private fun resetEmojiSearchState() {
    emojiSearchCommittedPrefix = ""
  }

  fun handleKeyAction(action: KeyAction) {
    cancelActiveVoiceSessionIfNeeded()
    val preSession = _sessionState.value
    val preLang = languageManager.activeLanguage.value
    if (preSession.mode == KeyboardMode.EMOJI) {
      ImeDiagnosticsLogger.log(
        category = "KEY_ROUTING",
        event = "onKeyReceived",
        details = "action=$action mode=${preSession.mode} searchFocused=${preSession.isEmojiSearchFocused} lang=${preLang.id}",
      )
    }

    when (action) {
      KeyAction.SwipeSpaceLeft -> {
        commitAllPhoneticBuffers()
        if (languageManager.onSpacebarSwipeLeft()) {
          triggerKeyFeedback(KeyFeedbackType.SPACE)
        }
        return
      }

      KeyAction.SwipeSpaceRight -> {
        commitAllPhoneticBuffers()
        if (languageManager.onSpacebarSwipeRight()) {
          triggerKeyFeedback(KeyFeedbackType.SPACE)
        }
        return
      }

      KeyAction.LanguageSwitch -> {
        triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
        commitAllPhoneticBuffers()
        languageManager.switchToNextLanguage()
        return
      }

      KeyAction.ModeSymbolsPrimary -> {
        triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
        commitAllPhoneticBuffers()
        resetEmojiSearchState()
        ImeDiagnosticsLogger.log(
          category = "MODE_TRANSITION",
          event = "ModeSymbolsPrimary",
          details = "${_sessionState.value.mode} -> SYMBOLS_PRIMARY",
        )
        _sessionState.update {
          it.copy(
            mode = KeyboardMode.SYMBOLS_PRIMARY,
            isEmojiSearchFocused = false,
          )
        }
        return
      }

      KeyAction.ModeSymbolsSecondary -> {
        triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
        commitAllPhoneticBuffers()
        resetEmojiSearchState()
        ImeDiagnosticsLogger.log(
          category = "MODE_TRANSITION",
          event = "ModeSymbolsSecondary",
          details = "${_sessionState.value.mode} -> SYMBOLS_SECONDARY",
        )
        _sessionState.update {
          it.copy(
            mode = KeyboardMode.SYMBOLS_SECONDARY,
            isEmojiSearchFocused = false,
          )
        }
        return
      }

      KeyAction.ModeAlphabet -> {
        triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
        commitAllPhoneticBuffers()
        resetEmojiSearchState()
        ImeDiagnosticsLogger.log(
          category = "MODE_TRANSITION",
          event = "ModeAlphabet",
          details = "${_sessionState.value.mode} -> ALPHABET",
        )
        _sessionState.update {
          it.copy(
            mode = KeyboardMode.ALPHABET,
            isEmojiSearchFocused = false,
            emojiSearchQuery = "",
            emojiSearchRawQuery = "",
            emojiSearchCursor = 0,
          )
        }
        return
      }

      KeyAction.ModeEmoji -> {
        triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
        commitAllPhoneticBuffers()
        _sessionState.update { current ->
          if (current.mode == KeyboardMode.EMOJI && current.isEmojiSearchFocused) {
            ImeDiagnosticsLogger.log(
              category = "EMOJI_SEARCH",
              event = "ModeEmoji_DismissSearch",
              details = "restoring full Emoji Panel",
            )
            current.copy(isEmojiSearchFocused = false)
          } else {
            ImeDiagnosticsLogger.log(
              category = "MODE_TRANSITION",
              event = "ModeEmoji",
              details = "${current.mode} -> EMOJI",
            )
            current.copy(
              mode = KeyboardMode.EMOJI,
              isEmojiSearchFocused = false,
            )
          }
        }
        return
      }

      else -> Unit
    }

    val feedbackType =
      when (action) {
        is KeyAction.CommitChar -> KeyFeedbackType.CHARACTER
        KeyAction.Backspace -> KeyFeedbackType.DELETE
        KeyAction.Space -> KeyFeedbackType.SPACE
        KeyAction.Enter -> KeyFeedbackType.ENTER
        else -> KeyFeedbackType.FUNCTIONAL
      }
    triggerKeyFeedback(feedbackType)

    val currentSession = _sessionState.value
    val activeLang = languageManager.activeLanguage.value

    // Route typing directly into Emoji Search Bar when Emoji Search is focused
    if (currentSession.mode == KeyboardMode.EMOJI && currentSession.isEmojiSearchFocused) {
      ImeDiagnosticsLogger.log(
        category = "KEY_ROUTING",
        event = "routeToEmojiSearch",
        details = "action=$action hostInputConnectionSkipped=true",
      )
      handleEmojiSearchKeyAction(action, currentSession, activeLang.kind)
      return
    }

    val ic = currentInputConnection
    if (ic == null) {
      ImeDiagnosticsLogger.log(
        category = "INPUT_CONNECTION",
        event = "nullInputConnection",
        details = "action=$action mode=${currentSession.mode}",
      )
      return
    }

    when (action) {
      is KeyAction.CommitChar -> {
        val isShifted = currentSession.shiftState != ShiftState.LOWERCASE
        val charToCommit = if (isShifted) action.uppercase else action.lowercase

        if (charToCommit == "্য" || charToCommit == "\u09CD\u09AF") {
          commitComposingHinglishBuffer()
          handleYaPhalaCommit(ic, charToCommit)
          if (currentSession.shiftState == ShiftState.SHIFTED) {
            _sessionState.update { it.copy(shiftState = ShiftState.LOWERCASE) }
          }
          return
        }

        if (activeLang.kind == LanguageInputMethodKind.PHONETIC_HINDI && currentSession.mode == KeyboardMode.ALPHABET) {
          // In Hindi Hinglish mode, ASCII letters are composed phonetically
          val isAsciiLetter = charToCommit.length == 1 && charToCommit[0].let { it in 'a'..'z' || it in 'A'..'Z' }
          if (isAsciiLetter) {
            hinglishComposingBuffer.append(charToCommit)
            val converted = HindiPhoneticEngine.transliterate(hinglishComposingBuffer.toString())
            ic.setComposingText(converted, 1)

            if (currentSession.shiftState == ShiftState.SHIFTED) {
              _sessionState.update { it.copy(shiftState = ShiftState.LOWERCASE) }
            }
            return
          } else {
            commitComposingHinglishBuffer()
            ic.commitText(charToCommit, 1)
          }
        } else {
          // Direct QWERTY or Direct Bengali Probhat
          ic.commitText(charToCommit, 1)
        }

        if (currentSession.shiftState == ShiftState.SHIFTED) {
          _sessionState.update { it.copy(shiftState = ShiftState.LOWERCASE) }
        }
      }

      KeyAction.Shift -> {
        val now = SystemClock.uptimeMillis()
        val nextShift =
          when (currentSession.shiftState) {
            ShiftState.LOWERCASE -> ShiftState.SHIFTED
            ShiftState.SHIFTED ->
              if (now - lastShiftTapTimeMs < 350L) {
                ShiftState.CAPS_LOCK
              } else {
                ShiftState.LOWERCASE
              }
            ShiftState.CAPS_LOCK -> ShiftState.LOWERCASE
          }
        lastShiftTapTimeMs = now
        _sessionState.update { it.copy(shiftState = nextShift) }
      }

      KeyAction.Backspace -> {
        if (activeLang.kind == LanguageInputMethodKind.PHONETIC_HINDI && hinglishComposingBuffer.isNotEmpty()) {
          hinglishComposingBuffer.deleteCharAt(hinglishComposingBuffer.length - 1)
          if (hinglishComposingBuffer.isNotEmpty()) {
            val converted = HindiPhoneticEngine.transliterate(hinglishComposingBuffer.toString())
            ic.setComposingText(converted, 1)
          } else {
            ic.setComposingText("", 0)
            ic.finishComposingText()
          }
          return
        }

        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
          ic.commitText("", 1)
        } else {
          val beforeText = ic.getTextBeforeCursor(32, 0)?.toString().orEmpty()
          val charsToDelete = EmojiTextUtil.calculateGraphemeLengthToDelete(beforeText)
          ic.deleteSurroundingText(charsToDelete, 0)
        }
        refreshShiftIfAutoCap()
      }

      KeyAction.Space -> {
        commitAllPhoneticBuffers()
        val now = SystemClock.uptimeMillis()
        val config = preferencesRepository.config.value
        val beforeText = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()

        if (
          config.doubleSpacePeriod &&
            now - lastSpaceTapTimeMs < 300L &&
            beforeText.length == 2 &&
            beforeText[1] == ' ' &&
            beforeText[0].isLetterOrDigit()
        ) {
          ic.deleteSurroundingText(1, 0)
          ic.commitText(". ", 1)
          lastSpaceTapTimeMs = 0L
        } else {
          ic.commitText(" ", 1)
          lastSpaceTapTimeMs = now
        }

        if (
          currentSession.mode != KeyboardMode.ALPHABET &&
            currentSession.mode != KeyboardMode.NUMBER_PAD &&
            currentSession.mode != KeyboardMode.EMOJI
        ) {
          _sessionState.update { it.copy(mode = KeyboardMode.ALPHABET) }
        }
        refreshShiftIfAutoCap()
      }

      KeyAction.Enter -> {
        commitAllPhoneticBuffers()
        val info = currentInputEditorInfo
        val imeOptions = info?.imeOptions ?: EditorInfo.IME_NULL
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0

        if (!noEnterAction && isPerformableImeAction(actionId)) {
          ic.performEditorAction(actionId)
        } else {
          ic.commitText("\n", 1)
        }
        refreshShiftIfAutoCap()
      }

      KeyAction.Cut -> {
        commitAllPhoneticBuffers()
        if (!preferencesRepository.config.value.cutCopyPasteEnabled) return
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
          systemClipboard?.setPrimaryClip(ClipData.newPlainText("Cut text", selectedText))
          ic.commitText("", 1)
        } else {
          ic.performContextMenuAction(android.R.id.cut)
        }
        clipboardController.syncFromSystemClipboard()
        refreshShiftIfAutoCap()
      }

      KeyAction.Copy -> {
        commitAllPhoneticBuffers()
        if (!preferencesRepository.config.value.cutCopyPasteEnabled) return
        val selectedText = ic.getSelectedText(0)
        if (!selectedText.isNullOrEmpty()) {
          systemClipboard?.setPrimaryClip(ClipData.newPlainText("Copied text", selectedText))
        } else {
          ic.performContextMenuAction(android.R.id.copy)
        }
        clipboardController.syncFromSystemClipboard()
      }

      KeyAction.Paste -> {
        commitAllPhoneticBuffers()
        if (!preferencesRepository.config.value.cutCopyPasteEnabled) return
        val primaryClip = systemClipboard?.primaryClip
        val clipText =
          if (primaryClip != null && primaryClip.itemCount > 0) {
            primaryClip.getItemAt(0)?.coerceToText(this)?.toString()
          } else {
            null
          }

        if (!clipText.isNullOrEmpty()) {
          ic.commitText(clipText, 1)
        } else {
          ic.performContextMenuAction(android.R.id.paste)
        }
        refreshShiftIfAutoCap()
      }

      else -> Unit
    }
  }

  private fun handleEmojiSearchKeyAction(
    action: KeyAction,
    currentSession: ImeSessionState,
    @Suppress("UNUSED_PARAMETER") languageKind: LanguageInputMethodKind,
  ) {
    when (action) {
      is KeyAction.CommitChar -> {
        val isShifted = currentSession.shiftState != ShiftState.LOWERCASE
        val charToCommit = if (isShifted) action.uppercase else action.lowercase
        val base = currentSession.emojiSearchQuery
        val cursor = currentSession.emojiSearchCursor.coerceIn(0, base.length)
        val textToInsert = if (charToCommit == "্য" || charToCommit == "\u09CD\u09AF") {
          if (cursor > 0 && base.substring(0, cursor).endsWith('\u09CD')) "\u09AF" else "\u09CD\u09AF"
        } else {
          charToCommit
        }
        val nextQuery = base.substring(0, cursor) + textToInsert + base.substring(cursor)
        val nextCursor = cursor + textToInsert.length
        emojiSearchCommittedPrefix = nextQuery
        ImeDiagnosticsLogger.log(
          category = "EMOJI_SEARCH",
          event = "CommitChar",
          details = "char='$textToInsert' query='$nextQuery' cursor=$nextCursor focused=true",
        )
        _sessionState.update {
          it.copy(
            emojiSearchQuery = nextQuery,
            emojiSearchRawQuery = nextQuery,
            emojiSearchCursor = nextCursor,
            shiftState = if (it.shiftState == ShiftState.SHIFTED) ShiftState.LOWERCASE else it.shiftState,
          )
        }
      }

      KeyAction.Backspace -> {
        val currentQuery = currentSession.emojiSearchQuery
        val cursor = currentSession.emojiSearchCursor.coerceIn(0, currentQuery.length)
        if (cursor > 0) {
          val prefixBeforeCursor = currentQuery.substring(0, cursor)
          val deleteLen = EmojiTextUtil.calculateGraphemeLengthToDelete(prefixBeforeCursor)
          val nextCursor = (cursor - deleteLen).coerceAtLeast(0)
          val nextQuery =
            currentQuery.substring(0, nextCursor) + currentQuery.substring(cursor)
          emojiSearchCommittedPrefix = nextQuery
          ImeDiagnosticsLogger.log(
            category = "EMOJI_SEARCH",
            event = "Backspace",
            details = "query='$nextQuery' cursor=$nextCursor focused=true",
          )
          _sessionState.update {
            it.copy(
              emojiSearchQuery = nextQuery,
              emojiSearchRawQuery = nextQuery,
              emojiSearchCursor = nextCursor,
            )
          }
        }
      }

      KeyAction.Space -> {
        val base = _sessionState.value.emojiSearchQuery
        val cursor = _sessionState.value.emojiSearchCursor.coerceIn(0, base.length)
        val nextQuery = base.substring(0, cursor) + " " + base.substring(cursor)
        val nextCursor = cursor + 1
        emojiSearchCommittedPrefix = nextQuery
        ImeDiagnosticsLogger.log(
          category = "EMOJI_SEARCH",
          event = "Space",
          details = "query='$nextQuery' cursor=$nextCursor focused=true",
        )
        _sessionState.update {
          it.copy(
            emojiSearchQuery = nextQuery,
            emojiSearchRawQuery = nextQuery,
            emojiSearchCursor = nextCursor,
          )
        }
      }

      KeyAction.Enter -> {
        ImeDiagnosticsLogger.log(
          category = "EMOJI_SEARCH",
          event = "Enter_DismissSearch",
          details = "query='${_sessionState.value.emojiSearchQuery}' focused -> false",
        )
        _sessionState.update {
          it.copy(isEmojiSearchFocused = false)
        }
      }

      KeyAction.Shift -> {
        val now = SystemClock.uptimeMillis()
        val nextShift =
          when (currentSession.shiftState) {
            ShiftState.LOWERCASE -> ShiftState.SHIFTED
            ShiftState.SHIFTED ->
              if (now - lastShiftTapTimeMs < 350L) {
                ShiftState.CAPS_LOCK
              } else {
                ShiftState.LOWERCASE
              }
            ShiftState.CAPS_LOCK -> ShiftState.LOWERCASE
          }
        lastShiftTapTimeMs = now
        _sessionState.update { it.copy(shiftState = nextShift) }
      }

      else -> Unit
    }
  }

  private fun commitComposingHinglishBuffer() {
    if (hinglishComposingBuffer.isNotEmpty()) {
      val converted = HindiPhoneticEngine.transliterate(hinglishComposingBuffer.toString())
      val ic = currentInputConnection
      if (ic != null) {
        ic.setComposingText(converted, 1)
        ic.finishComposingText()
      }
      hinglishComposingBuffer.clear()
    }
  }

  private fun commitAllPhoneticBuffers() {
    commitComposingHinglishBuffer()
  }

  private fun resetVoiceUtteranceState() {
    voiceLeadingSpaceResolved = false
    voiceNeedsLeadingSpace = false
    voiceHasComposingText = false
    voiceFinalCommittedInUtterance = false
    lastVoicePartialText = ""
  }

  private fun cancelActiveVoiceSessionIfNeeded() {
    val v1Mgr = _voiceRecognitionManager
    val v1State = v1Mgr?.uiState?.value?.state ?: VoiceState.IDLE
    val hasActive = v1Mgr?.hasActiveRecognizer == true
    if (
      _isVoiceTypingOpen.value ||
        hasActive ||
        v1State == VoiceState.STARTING ||
        v1State == VoiceState.LISTENING ||
        v1State == VoiceState.PROCESSING
    ) {
      v1Mgr?.cancel()
      if (voiceHasComposingText) {
        currentInputConnection?.finishComposingText()
      }
      resetVoiceUtteranceState()
      setVoiceTypingOpen(false)
    }
  }

  fun toggleVoiceTyping() {
    val voiceEnabled =
      if (::preferencesRepository.isInitialized) {
        preferencesRepository.config.value.voiceInputEnabled
      } else {
        true
      }
    if (!voiceEnabled) {
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "toggleVoiceTyping_disabled",
        details = "voiceInputEnabled=false",
      )
      Log.d("PersonalKeyboardVoice", "toggleVoiceTyping: voiceInputEnabled is false, aborting")
      return
    }

    val currentVoiceState = _voiceRecognitionManager?.uiState?.value?.state ?: VoiceState.IDLE
    val hasActiveRecognizer = _voiceRecognitionManager?.hasActiveRecognizer == true
    val isCurrentlyActive =
      _isVoiceTypingOpen.value ||
        hasActiveRecognizer ||
        currentVoiceState == VoiceState.LISTENING ||
        currentVoiceState == VoiceState.STARTING ||
        currentVoiceState == VoiceState.PROCESSING
    if (!isCurrentlyActive) {
      triggerVoiceHaptic()
      commitAllPhoneticBuffers()
      resetVoiceUtteranceState()
      _isLanguageSelectorOpen.value = false
      closeTranslatorPanel()
      _sessionState.update { it.copy(isEmojiSearchFocused = false) }
      val voiceLang = VoiceLanguage.forLanguageId(languageManager.activeLanguage.value.id)
      val ic = currentInputConnection
      val editorInfo = currentInputEditorInfo
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "toggleVoiceTyping_start",
        details = "engine=V1 lang=${voiceLang.tag} icValid=${ic != null} pkg=${editorInfo?.packageName ?: activeEditorPackageName} fieldId=${editorInfo?.fieldId ?: activeEditorFieldId}",
      )
      Log.d(
        "PersonalKeyboardVoice",
        "toggleVoiceTyping_start: engine=V1 lang=${voiceLang.tag} icValid=${ic != null} pkg=${editorInfo?.packageName ?: activeEditorPackageName}",
      )
      voiceRecognitionManager.refreshConnectivityMode()
      voiceRecognitionManager.setLanguage(voiceLang)
      voiceRecognitionManager.startListening(voiceLang)
    } else {
      triggerVoiceHaptic()
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "toggleVoiceTyping_stop",
        details = "engine=V1 state=$currentVoiceState hadComposing=$voiceHasComposingText partial='$lastVoicePartialText'",
      )
      Log.d(
        "PersonalKeyboardVoice",
        "toggleVoiceTyping_stop: engine=V1 state=$currentVoiceState hadComposing=$voiceHasComposingText partial='$lastVoicePartialText'",
      )
      if (voiceHasComposingText && lastVoicePartialText.isNotBlank() && !voiceFinalCommittedInUtterance) {
        commitVoiceText(lastVoicePartialText)
        voiceRecognitionManager.cancel()
      } else if (currentVoiceState == VoiceState.STARTING) {
        voiceRecognitionManager.cancel()
        resetVoiceUtteranceState()
        setVoiceTypingOpen(false)
      } else {
        voiceRecognitionManager.stopListening()
        currentInputConnection?.finishComposingText()
        resetVoiceUtteranceState()
        setVoiceTypingOpen(false)
      }
    }
  }

  fun handleVoicePartial(partialText: String) {
    val cleanPartial = partialText.trim()
    if (cleanPartial.isEmpty() || voiceFinalCommittedInUtterance) return
    if (voiceHasComposingText && cleanPartial == lastVoicePartialText) return

    val ic = currentInputConnection
    val editorInfo = currentInputEditorInfo
    val targetPkg = editorInfo?.packageName ?: activeEditorPackageName
    val targetFieldId = editorInfo?.fieldId ?: activeEditorFieldId

    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "handleVoicePartial_beforeInsert",
      details = "icValid=${ic != null} pkg=$targetPkg fieldId=$targetFieldId partial='$cleanPartial'",
    )
    Log.d(
      "PersonalKeyboardVoice",
      "handleVoicePartial: icValid=${ic != null} pkg=$targetPkg fieldId=$targetFieldId partial='$cleanPartial'",
    )

    if (ic == null) {
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "handleVoicePartial_nullInputConnection",
        details = "partial='$cleanPartial'",
      )
      Log.e("PersonalKeyboardVoice", "handleVoicePartial failed: currentInputConnection is null")
      return
    }

    if (!voiceLeadingSpaceResolved) {
      val beforeCursor = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
      voiceNeedsLeadingSpace =
        beforeCursor.isNotEmpty() && !beforeCursor.endsWith(" ") && !beforeCursor.endsWith("\n")
      voiceLeadingSpaceResolved = true
    }

    val composingToSet = if (voiceNeedsLeadingSpace) " $cleanPartial" else cleanPartial
    val applied = ic.setComposingText(composingToSet, 1)
    voiceHasComposingText = true
    lastVoicePartialText = cleanPartial

    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "handleVoicePartial_setComposingText",
      details = "applied=$applied composing='$composingToSet'",
    )
    Log.d(
      "PersonalKeyboardVoice",
      "handleVoicePartial_setComposingText: applied=$applied composing='$composingToSet'",
    )
  }

  fun handleVoiceError(errorMessage: String) {
    setVoiceTypingOpen(false)
    if (voiceHasComposingText && lastVoicePartialText.isNotBlank() && !voiceFinalCommittedInUtterance) {
      commitVoiceText(lastVoicePartialText)
      return
    }
    currentInputConnection?.finishComposingText()
    resetVoiceUtteranceState()
    runCatching {
      android.widget.Toast.makeText(this, errorMessage, android.widget.Toast.LENGTH_SHORT).show()
    }
  }

  fun handleVoicePermissionRequired() {
    setVoiceTypingOpen(false)
    resetVoiceUtteranceState()
    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "launchVoicePermissionActivity",
      details = "requesting RECORD_AUDIO",
    )
    Log.w("PersonalKeyboardVoice", "RECORD_AUDIO permission missing; launching VoicePermissionActivity")
    VoicePermissionActivity.onPermissionResultCallback = { granted ->
      VoicePermissionActivity.clearPermissionCallback()
      if (granted) {
        val voiceLang = VoiceLanguage.forLanguageId(languageManager.activeLanguage.value.id)
        voiceRecognitionManager.setLanguage(voiceLang)
        voiceRecognitionManager.startListening(voiceLang)
      }
    }
    runCatching {
      val intent =
        Intent(this, VoicePermissionActivity::class.java).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
      startActivity(intent)
    }.onFailure {
      VoicePermissionActivity.clearPermissionCallback()
      runCatching {
        android.widget.Toast.makeText(
            this,
            "Microphone permission required for voice typing",
            android.widget.Toast.LENGTH_SHORT,
          )
          .show()
      }
    }
  }

  private fun closeTranslatorPanel() {
    translationJob?.cancel()
    translationJob = null
    if (_isTranslatorOpen.value) {
      _isTranslatorOpen.value = false
    }
    _smartTranslatorManager?.onPanelClosed()
  }

  fun toggleLanguageSelector() {
    val willOpen = !_isLanguageSelectorOpen.value
    if (willOpen) {
      commitAllPhoneticBuffers()
      cancelActiveVoiceSessionIfNeeded()
      closeTranslatorPanel()
      _sessionState.update { it.copy(isEmojiSearchFocused = false) }
    }
    _isLanguageSelectorOpen.value = willOpen
  }

  fun toggleTranslator() {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    commitAllPhoneticBuffers()
    val willOpen = !_isTranslatorOpen.value
    if (willOpen) {
      cancelActiveVoiceSessionIfNeeded()
      _isLanguageSelectorOpen.value = false
      _sessionState.update { it.copy(isEmojiSearchFocused = false) }
      _isTranslatorOpen.value = true
    } else {
      closeTranslatorPanel()
    }
  }

  fun pasteIntoTranslator() {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    val clipText = systemClipboard?.primaryClip?.let { clip ->
      if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(this)?.toString() else null
    } ?: clipboardController.entries.value.firstOrNull()?.text
    if (!clipText.isNullOrBlank()) {
      smartTranslatorManager.setInputText(clipText)
    }
  }

  fun executeTranslation() {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    if (
      translationJob?.isActive == true &&
        _smartTranslatorManager?.isCurrentRequestAlreadyInFlight() == true
    ) {
      return
    }
    translationJob?.cancel()
    translationJob = serviceScope.launch {
      smartTranslatorManager.translateCurrentText()
    }
  }

  fun selectTranslatorLanguage(lang: TranslateLanguageItem) {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    val mode = smartTranslatorManager.uiState.value.selectorMode
    if (mode == LanguageSelectorMode.SOURCE) {
      smartTranslatorManager.setSourceLanguage(lang)
    } else {
      smartTranslatorManager.setTargetLanguage(lang)
    }
  }

  fun copyTranslationToClipboard(text: String) {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    val clip = ClipData.newPlainText("Translated Text", text)
    systemClipboard?.setPrimaryClip(clip)
    android.widget.Toast.makeText(
      this,
      getString(com.example.R.string.translator_copied_toast),
      android.widget.Toast.LENGTH_SHORT,
    ).show()
  }

  fun insertTranslation(text: String) {
    triggerKeyFeedback(KeyFeedbackType.FUNCTIONAL)
    commitDirectText(text)
    closeTranslatorPanel()
  }

  fun commitVoiceText(text: String) {
    setVoiceTypingOpen(false)
    if (voiceFinalCommittedInUtterance) {
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "commitVoiceText_duplicateSkipped",
        details = "text='$text'",
      )
      return
    }

    val cleanText = text.trim().ifEmpty { lastVoicePartialText.trim() }
    val ic = currentInputConnection
    val editorInfo = currentInputEditorInfo
    val targetPkg = editorInfo?.packageName ?: activeEditorPackageName
    val targetFieldId = editorInfo?.fieldId ?: activeEditorFieldId

    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "commitVoiceText_beforeCommit",
      details = "icValid=${ic != null} pkg=$targetPkg fieldId=$targetFieldId text='$cleanText' hadComposing=$voiceHasComposingText",
    )
    Log.d(
      "PersonalKeyboardVoice",
      "commitVoiceText_beforeCommit: icValid=${ic != null} pkg=$targetPkg fieldId=$targetFieldId text='$cleanText' hadComposing=$voiceHasComposingText",
    )

    if (cleanText.isEmpty()) {
      ic?.finishComposingText()
      resetVoiceUtteranceState()
      return
    }

    if (ic == null) {
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "commitVoiceText_nullInputConnection",
        details = "text='$cleanText'",
      )
      Log.e("PersonalKeyboardVoice", "commitVoiceText failed: currentInputConnection is null")
      resetVoiceUtteranceState()
      return
    }

    if (!voiceHasComposingText) {
      commitAllPhoneticBuffers()
    }

    if (!voiceLeadingSpaceResolved) {
      val beforeCursor = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
      voiceNeedsLeadingSpace =
        beforeCursor.isNotEmpty() && !beforeCursor.endsWith(" ") && !beforeCursor.endsWith("\n")
      voiceLeadingSpaceResolved = true
    }

    val prefix = if (voiceNeedsLeadingSpace) " " else ""
    val withTrailingSpace = if (cleanText.endsWith(" ")) "$prefix$cleanText" else "$prefix$cleanText "

    ic.beginBatchEdit()
    val committed = ic.commitText(withTrailingSpace, 1)
    ic.endBatchEdit()

    voiceFinalCommittedInUtterance = true
    voiceHasComposingText = false
    lastVoicePartialText = ""

    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "commitVoiceText_committed",
      details = "committed=$committed text='$withTrailingSpace' pkg=$targetPkg",
    )
    Log.d(
      "PersonalKeyboardVoice",
      "commitVoiceText_committed: committed=$committed text='$withTrailingSpace' pkg=$targetPkg",
    )
    refreshShiftIfAutoCap()
  }

  private fun handleYaPhalaCommit(ic: InputConnection?, yaPhalaText: String = "\u09CD\u09AF") {
    if (ic == null) return
    val before = ic.getTextBeforeCursor(2, 0)?.toString().orEmpty()
    if (before.endsWith('\u09CD')) {
      // Consonant already has hasanta attached (e.g. ক্), so just append য to form ক্য
      ic.commitText("\u09AF", 1)
    } else {
      ic.commitText(yaPhalaText, 1)
    }
  }

  private fun commitDirectText(text: String) {
    cancelActiveVoiceSessionIfNeeded()
    commitAllPhoneticBuffers()
    val feedbackType =
      if (text == " " || text == "\n") KeyFeedbackType.SPACE else KeyFeedbackType.CHARACTER
    triggerKeyFeedback(feedbackType)
    val ic = currentInputConnection
    if (ic != null) {
      if (text == "্য" || text == "\u09CD\u09AF") {
        handleYaPhalaCommit(ic, text)
      } else {
        ic.commitText(text, 1)
      }
    }
    if (_sessionState.value.shiftState == ShiftState.SHIFTED) {
      _sessionState.update { it.copy(shiftState = ShiftState.LOWERCASE) }
    }
  }

  private fun refreshShiftIfAutoCap() {
    val currentShift = _sessionState.value.shiftState
    if (currentShift == ShiftState.CAPS_LOCK) return
    val nextShift = computeAutoCapShiftState(currentInputEditorInfo)
    if (nextShift != currentShift) {
      _sessionState.update { it.copy(shiftState = nextShift) }
    }
  }

  private fun computeAutoCapShiftState(editorInfo: EditorInfo?): ShiftState {
    if (!preferencesRepository.config.value.autoCapitalize) return ShiftState.LOWERCASE
    // Auto-capitalize only in English mode
    if (languageManager.activeLanguage.value.kind != LanguageInputMethodKind.DIRECT_QWERTY) {
      return ShiftState.LOWERCASE
    }

    val info = editorInfo ?: return ShiftState.LOWERCASE
    val inputType = info.inputType
    if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
      return ShiftState.LOWERCASE
    }

    val variation = inputType and InputType.TYPE_MASK_VARIATION
    if (
      variation == InputType.TYPE_TEXT_VARIATION_URI ||
        variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
        variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    ) {
      return ShiftState.LOWERCASE
    }

    val ic = currentInputConnection ?: return ShiftState.SHIFTED
    val before = ic.getTextBeforeCursor(3, 0) ?: return ShiftState.SHIFTED
    val len = before.length
    if (len == 0 || before[len - 1] == '\n') return ShiftState.SHIFTED
    if (before[len - 1] == ' ') {
      var i = len - 2
      while (i >= 0 && before[i].isWhitespace()) {
        i--
      }
      if (i >= 0) {
        val punct = before[i]
        if (punct == '.' || punct == '!' || punct == '?') {
          return ShiftState.SHIFTED
        }
      }
    }
    return ShiftState.LOWERCASE
  }

  private fun isPerformableImeAction(actionId: Int): Boolean {
    return when (actionId) {
      EditorInfo.IME_ACTION_DONE,
      EditorInfo.IME_ACTION_GO,
      EditorInfo.IME_ACTION_NEXT,
      EditorInfo.IME_ACTION_SEARCH,
      EditorInfo.IME_ACTION_SEND -> true
      else -> false
    }
  }

  private fun resolveEnterAction(editorInfo: EditorInfo?): EnterActionType {
    val imeOptions = editorInfo?.imeOptions ?: return EnterActionType.ENTER
    if ((imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
      return EnterActionType.ENTER
    }
    return when (imeOptions and EditorInfo.IME_MASK_ACTION) {
      EditorInfo.IME_ACTION_DONE -> EnterActionType.DONE
      EditorInfo.IME_ACTION_GO -> EnterActionType.GO
      EditorInfo.IME_ACTION_SEARCH -> EnterActionType.SEARCH
      EditorInfo.IME_ACTION_SEND -> EnterActionType.SEND
      EditorInfo.IME_ACTION_NEXT -> EnterActionType.NEXT
      else -> EnterActionType.ENTER
    }
  }

  private fun triggerKeyFeedback(type: KeyFeedbackType = KeyFeedbackType.CHARACTER) {
    if (::feedbackManager.isInitialized) {
      feedbackManager.trigger(type, window?.window?.decorView)
    }
  }

  private fun launchSettingsActivity() {
    val intent =
      Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
      }
    runCatching { startActivity(intent) }
  }

  override fun onDestroy() {
    ImeDiagnosticsLogger.log("IME_LIFECYCLE", "onDestroy", "service=${hashCode()}")
    rmsScaleAnimator?.cancel()
    rmsScaleAnimator = null
    _voiceRmsScaleComposeState.floatValue = 1.0f
    commitAllPhoneticBuffers()
    VoicePermissionActivity.clearPermissionCallback()
    translationJob?.cancel()
    translationJob = null
    _smartTranslatorManager?.close()
    serviceScope.cancel()
    _voiceRecognitionManager?.destroy()
    unregisterUserUnlockReceiver()
    if (::clipboardController.isInitialized) {
      clipboardController.release()
    }
    if (::feedbackManager.isInitialized) {
      feedbackManager.release()
    }
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    store.clear()
    super.onDestroy()
  }

  companion object {
    private val DefaultTranslatorUiState = TranslatorUiState()
  }
}
