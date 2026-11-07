package com.example.translate

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SmartTranslatorManager(
  context: Context,
) {
  companion object {
    private const val TAG = "SmartTranslatorManager"
  }

  private val appContext: Context = context.applicationContext
  private val resourceLock = Any()

  private val _uiState = MutableStateFlow(TranslatorUiState())
  val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

  @Volatile
  private var languageIdentifierInstance: LanguageIdentifier? = null

  private var activeTranslationsCount: Int = 0
  private var pendingCloseWhenIdle: Boolean = false
  private var activeTranslationJob: Job? = null
  private var inFlightRequestSignature: String? = null

  internal var translationEngineOverride: (suspend (text: String, sourceLang: String, targetLang: String) -> String)? = null

  internal val cachedTranslatorCount: Int
    get() = synchronized(resourceLock) { maxOf(translatorCache.size, activeSessionPairs.size) }

  internal val verifiedModelCount: Int
    get() = verifiedModelPairs.size

  internal val hasLanguageIdentifier: Boolean
    get() = synchronized(resourceLock) { languageIdentifierInstance != null }

  internal val activeTranslations: Int
    get() = synchronized(resourceLock) { activeTranslationsCount }

  private fun getOrCreateLanguageIdentifier(): LanguageIdentifier {
    return languageIdentifierInstance ?: synchronized(resourceLock) {
      languageIdentifierInstance ?: LanguageIdentification.getClient().also {
        languageIdentifierInstance = it
      }
    }
  }

  private fun releaseLanguageIdentifierIfIdle() {
    val identifierToClose: LanguageIdentifier? = synchronized(resourceLock) {
      if (activeTranslationsCount == 0) {
        val current = languageIdentifierInstance
        languageIdentifierInstance = null
        current
      } else {
        null
      }
    }
    identifierToClose?.let { identifier ->
      try {
        identifier.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing language identifier", e)
      }
    }
  }

  // Cache translators during active session and preserve verified downloaded model keys across panel reopens
  private val translatorCache = ConcurrentHashMap<String, Translator>()
  private val activeSessionPairs = ConcurrentHashMap.newKeySet<String>()
  private val verifiedModelPairs = ConcurrentHashMap.newKeySet<String>()
  private val defaultDownloadConditions: DownloadConditions by lazy {
    DownloadConditions.Builder().build()
  }

  internal fun isCurrentRequestAlreadyInFlight(): Boolean {
    val current = _uiState.value
    val textToTranslate = current.inputText.trim()
    if (textToTranslate.isEmpty()) return false
    val signature = "${current.sourceLanguage.code}|${current.targetLanguage.code}|$textToTranslate"
    return synchronized(resourceLock) {
      activeTranslationsCount > 0 &&
        inFlightRequestSignature == signature &&
        activeTranslationJob?.isActive == true
    }
  }

  fun setSourceLanguage(language: TranslateLanguageItem) {
    _uiState.update { current ->
      if (current.sourceLanguage == language) {
        current.copy(selectorMode = LanguageSelectorMode.NONE, languageSearchQuery = "")
      } else {
        // If selecting same as target, adjust target if not auto
        var newTarget = current.targetLanguage
        if (!language.isAutoDetect && language.code == current.targetLanguage.code) {
          newTarget = if (language.code == TranslateLanguage.ENGLISH) {
            TranslateLanguageItem.BENGALI
          } else {
            TranslateLanguageItem.ENGLISH
          }
        }
        current.copy(
          sourceLanguage = language,
          targetLanguage = newTarget,
          selectorMode = LanguageSelectorMode.NONE,
          languageSearchQuery = "",
          errorMessage = null,
        )
      }
    }
    if (!language.isAutoDetect) {
      releaseLanguageIdentifierIfIdle()
    }
  }

  fun setTargetLanguage(language: TranslateLanguageItem) {
    if (language.isAutoDetect) return // Target cannot be auto
    _uiState.update { current ->
      var newSource = current.sourceLanguage
      if (!newSource.isAutoDetect && newSource.code == language.code) {
        newSource = if (language.code == TranslateLanguage.ENGLISH) {
          TranslateLanguageItem.BENGALI
        } else {
          TranslateLanguageItem.ENGLISH
        }
      }
      current.copy(
        targetLanguage = language,
        sourceLanguage = newSource,
        selectorMode = LanguageSelectorMode.NONE,
        languageSearchQuery = "",
        errorMessage = null,
      )
    }
  }

  fun swapLanguages() {
    val current = _uiState.value
    if (current.sourceLanguage.isAutoDetect) return
    if (current.sourceLanguage.code == current.targetLanguage.code) return

    val oldSource = current.sourceLanguage
    val oldTarget = current.targetLanguage
    val oldInput = current.inputText
    val oldResult = current.translatedText

    _uiState.update {
      it.copy(
        sourceLanguage = oldTarget,
        targetLanguage = oldSource,
        inputText = oldResult,
        translatedText = oldInput,
        errorMessage = null,
      )
    }
  }

  fun setInputText(text: String) {
    _uiState.update { current ->
      if (current.inputText == text && current.errorMessage == null) current
      else current.copy(inputText = text, errorMessage = null)
    }
  }

  fun clearInputText() {
    _uiState.update {
      it.copy(
        inputText = "",
        translatedText = "",
        detectedLanguage = null,
        errorMessage = null,
        statusMessage = null,
      )
    }
  }

  fun openSelector(mode: LanguageSelectorMode) {
    _uiState.update { it.copy(selectorMode = mode, languageSearchQuery = "") }
  }

  fun closeSelector() {
    _uiState.update { it.copy(selectorMode = LanguageSelectorMode.NONE, languageSearchQuery = "") }
  }

  fun setLanguageSearchQuery(query: String) {
    _uiState.update { current ->
      if (current.languageSearchQuery == query) current
      else current.copy(languageSearchQuery = query)
    }
  }

  suspend fun translateCurrentText() {
    val current = _uiState.value
    val textToTranslate = current.inputText.trim()

    if (textToTranslate.isEmpty()) {
      _uiState.update {
        it.copy(
          errorMessage = "Please enter or paste text to translate",
          isLoading = false,
        )
      }
      return
    }

    val requestSignature = "${current.sourceLanguage.code}|${current.targetLanguage.code}|$textToTranslate"
    val callerJob = coroutineContext[Job]
    synchronized(resourceLock) {
      if (
        activeTranslationsCount > 0 &&
          inFlightRequestSignature == requestSignature &&
          activeTranslationJob?.isActive == true &&
          activeTranslationJob !== callerJob
      ) {
        return
      }
      activeTranslationJob?.cancel()
      activeTranslationJob = callerJob
      inFlightRequestSignature = requestSignature
      activeTranslationsCount++
      pendingCloseWhenIdle = false
    }

    _uiState.update {
      it.copy(
        isLoading = true,
        errorMessage = null,
        statusMessage = "Analyzing text…",
      )
    }

    try {
      withContext(Dispatchers.IO) {
        var effectiveSource = current.sourceLanguage

        // Step 1: Auto Language Detection if needed
        if (effectiveSource.isAutoDetect) {
          val detectedCode = detectLanguageCode(textToTranslate)
          if (detectedCode != null && detectedCode != "und") {
            val detectedItem = TranslateLanguageItem.fromCode(detectedCode)
            effectiveSource = detectedItem
            _uiState.update { it.copy(detectedLanguage = detectedItem) }
          } else {
            // Default to English if language cannot be determined
            effectiveSource = TranslateLanguageItem.ENGLISH
          }
        }

        var effectiveTarget = current.targetLanguage

        // If detected source matches target, smartly adjust target
        if (effectiveSource.code.equals(effectiveTarget.code, ignoreCase = true)) {
          effectiveTarget = if (effectiveSource.code.equals(TranslateLanguage.BENGALI, ignoreCase = true)) {
            TranslateLanguageItem.ENGLISH
          } else if (effectiveSource.code.equals(TranslateLanguage.HINDI, ignoreCase = true)) {
            TranslateLanguageItem.BENGALI
          } else {
            TranslateLanguageItem.BENGALI
          }
          _uiState.update { it.copy(targetLanguage = effectiveTarget) }
        }

        // Step 2: Perform Translation
        val translated = translateText(
          text = textToTranslate,
          sourceLangCode = effectiveSource.code,
          targetLangCode = effectiveTarget.code,
        )

        val isStillCurrent = synchronized(resourceLock) {
          activeTranslationJob === callerJob && !pendingCloseWhenIdle
        }
        if (!isStillCurrent || callerJob?.isCancelled == true) {
          throw CancellationException("Translation cancelled or superseded")
        }

        _uiState.update {
          it.copy(
            translatedText = translated,
            isLoading = false,
            isDownloadingModel = false,
            statusMessage = null,
            errorMessage = null,
          )
        }
      }
    } catch (ce: CancellationException) {
      val shouldResetLoading = synchronized(resourceLock) {
        activeTranslationJob === callerJob || activeTranslationJob == null
      }
      if (shouldResetLoading) {
        _uiState.update {
          it.copy(
            isLoading = false,
            isDownloadingModel = false,
            statusMessage = null,
          )
        }
      }
      throw ce
    } catch (e: Exception) {
      Log.e(TAG, "Translation error", e)
      val shouldReportError = synchronized(resourceLock) {
        activeTranslationJob === callerJob || activeTranslationJob == null
      }
      if (shouldReportError) {
        val userFriendlyMessage = when {
          e.message?.contains("network", ignoreCase = true) == true ->
            "Internet required to download offline language model once"
          e.message?.contains("download", ignoreCase = true) == true ->
            "Failed to download offline model. Check internet connection."
          else -> "Translation error: ${e.localizedMessage ?: "Please try again"}"
        }
        _uiState.update {
          it.copy(
            isLoading = false,
            isDownloadingModel = false,
            errorMessage = userFriendlyMessage,
            statusMessage = null,
          )
        }
      }
    } finally {
      val shouldCloseNow = synchronized(resourceLock) {
        if (activeTranslationJob === callerJob) {
          activeTranslationJob = null
          inFlightRequestSignature = null
        }
        activeTranslationsCount = (activeTranslationsCount - 1).coerceAtLeast(0)
        if (activeTranslationsCount == 0 && pendingCloseWhenIdle) {
          pendingCloseWhenIdle = false
          true
        } else {
          false
        }
      }
      if (shouldCloseNow) {
        closeResourcesInternal(clearVerifiedModels = false)
      }
    }
  }

  suspend fun detectLanguageCode(text: String): String? {
    return try {
      val result = getOrCreateLanguageIdentifier().identifyLanguage(text).awaitResult()
      if (result == "und") null else result
    } catch (e: Exception) {
      Log.w(TAG, "Language identification failed", e)
      null
    }
  }

  /**
   * Translates text from sourceLangCode to targetLangCode on-device using ML Kit.
   *
   * IMPORTANT ARCHITECTURE FOR HINDI -> BENGALI & NON-ENGLISH PAIRS:
   * ML Kit on-device translation supports translation between English and other languages.
   * When both source and target are non-English (such as Hindi -> Bengali, Bengali -> Hindi,
   * or Spanish -> Bengali), this method performs a two-stage local pivot:
   * 1. Source (e.g. Hindi) -> English
   * 2. English -> Target (e.g. Bengali)
   * Both stages run completely on-device without any external server or API!
   */
  private suspend fun translateText(
    text: String,
    sourceLangCode: String,
    targetLangCode: String,
  ): String {
    val englishCode = TranslateLanguage.ENGLISH
    val isDirectEnglish =
      sourceLangCode.equals(englishCode, ignoreCase = true) ||
        targetLangCode.equals(englishCode, ignoreCase = true)

    val requiredKeys =
      if (isDirectEnglish) {
        setOf("${sourceLangCode}_to_$targetLangCode")
      } else {
        setOf("${sourceLangCode}_to_$englishCode", "${englishCode}_to_$targetLangCode")
      }
    evictUnusedTranslators(requiredKeys)

    // Case 1: Direct translation (one of the languages is English)
    if (isDirectEnglish) {
      return executeSingleTranslation(text, sourceLangCode, targetLangCode)
    }

    // Case 2: Pivot translation via English for non-English pairs (e.g. Hindi -> English -> Bengali)
    Log.d(TAG, "Pivoting translation $sourceLangCode -> $englishCode -> $targetLangCode")
    val intermediateEnglish = executeSingleTranslation(
      text = text,
      sourceLang = sourceLangCode,
      targetLang = englishCode,
    )

    if (intermediateEnglish.isBlank()) {
      return ""
    }

    return executeSingleTranslation(
      text = intermediateEnglish,
      sourceLang = englishCode,
      targetLang = targetLangCode,
    )
  }

  private fun evictUnusedTranslators(requiredKeys: Set<String>) {
    val translatorsToClose = mutableListOf<Translator>()
    synchronized(resourceLock) {
      if (activeTranslationsCount <= 1) {
        val obsoleteSessionKeys = activeSessionPairs.filter { it !in requiredKeys }
        obsoleteSessionKeys.forEach { activeSessionPairs.remove(it) }

        val obsoleteCacheKeys = translatorCache.keys.filter { it !in requiredKeys }
        for (key in obsoleteCacheKeys) {
          translatorCache.remove(key)?.let { translatorsToClose.add(it) }
        }
      }
    }
    for (translator in translatorsToClose) {
      try {
        translator.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing evicted translator", e)
      }
    }
  }

  private suspend fun executeSingleTranslation(
    text: String,
    sourceLang: String,
    targetLang: String,
  ): String {
    val cacheKey = "${sourceLang}_to_$targetLang"
    synchronized(resourceLock) {
      activeSessionPairs.add(cacheKey)
    }

    translationEngineOverride?.let { overrideEngine ->
      verifiedModelPairs.add(cacheKey)
      return overrideEngine(text, sourceLang, targetLang)
    }

    val translator = synchronized(resourceLock) {
      translatorCache.getOrPut(cacheKey) {
        val options = TranslatorOptions.Builder()
          .setSourceLanguage(sourceLang)
          .setTargetLanguage(targetLang)
          .build()
        Translation.getClient(options)
      }
    }

    if (!verifiedModelPairs.contains(cacheKey)) {
      _uiState.update {
        it.copy(
          isDownloadingModel = true,
          statusMessage = "Downloading offline model if needed…",
        )
      }
      translator.downloadModelIfNeeded(defaultDownloadConditions).awaitResult()
      verifiedModelPairs.add(cacheKey)
    }

    _uiState.update {
      it.copy(
        isDownloadingModel = false,
        statusMessage = "Translating…",
      )
    }

    return try {
      translator.translate(text).awaitResult()
    } catch (e: Exception) {
      verifiedModelPairs.remove(cacheKey)
      throw e
    }
  }

  fun cancelActiveTranslation() {
    val jobToCancel = synchronized(resourceLock) {
      val job = activeTranslationJob
      activeTranslationJob = null
      inFlightRequestSignature = null
      job
    }
    jobToCancel?.cancel()
    _uiState.update { current ->
      if (current.isLoading || current.isDownloadingModel || current.statusMessage != null) {
        current.copy(
          isLoading = false,
          isDownloadingModel = false,
          statusMessage = null,
        )
      } else {
        current
      }
    }
  }

  fun onPanelClosed() {
    _uiState.update { current ->
      if (
        current.selectorMode != LanguageSelectorMode.NONE ||
          current.languageSearchQuery.isNotEmpty() ||
          current.isLoading ||
          current.isDownloadingModel ||
          current.statusMessage != null
      ) {
        current.copy(
          selectorMode = LanguageSelectorMode.NONE,
          languageSearchQuery = "",
          isLoading = false,
          isDownloadingModel = false,
          statusMessage = null,
        )
      } else {
        current
      }
    }
    cancelAndReleaseResourcesSafely(clearVerifiedModels = false)
  }

  fun close() {
    cancelAndReleaseResourcesSafely(clearVerifiedModels = true)
  }

  private fun cancelAndReleaseResourcesSafely(clearVerifiedModels: Boolean) {
    val jobToCancel: Job?
    val closeImmediately: Boolean
    synchronized(resourceLock) {
      jobToCancel = activeTranslationJob
      activeTranslationJob = null
      inFlightRequestSignature = null
      if (clearVerifiedModels) {
        verifiedModelPairs.clear()
      }
      if (activeTranslationsCount > 0) {
        pendingCloseWhenIdle = true
        closeImmediately = false
      } else {
        pendingCloseWhenIdle = false
        closeImmediately = true
      }
    }
    jobToCancel?.cancel()
    if (closeImmediately) {
      closeResourcesInternal(clearVerifiedModels = clearVerifiedModels)
    }
  }

  private fun closeResourcesInternal(clearVerifiedModels: Boolean) {
    val translatorsToClose: List<Translator>
    val identifierToClose: LanguageIdentifier?
    synchronized(resourceLock) {
      if (activeTranslationsCount > 0) {
        pendingCloseWhenIdle = true
        return
      }
      translatorsToClose = translatorCache.values.toList()
      translatorCache.clear()
      activeSessionPairs.clear()
      if (clearVerifiedModels) {
        verifiedModelPairs.clear()
      }
      identifierToClose = languageIdentifierInstance
      languageIdentifierInstance = null
    }

    for (translator in translatorsToClose) {
      try {
        translator.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing translator", e)
      }
    }
    identifierToClose?.let { identifier ->
      try {
        identifier.close()
      } catch (e: Exception) {
        Log.w(TAG, "Error closing language identifier", e)
      }
    }
  }
}

/**
 * Lightweight coroutine await extension for Google Play Services Task<T>
 */
suspend fun <T> Task<T>.awaitResult(): T {
  if (isComplete) {
    val e = exception
    return if (e == null) {
      if (isCanceled) {
        throw CancellationException("Task $this was cancelled.")
      } else {
        result
      }
    } else {
      throw e
    }
  }
  return suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
      if (continuation.isActive) {
        continuation.resume(result)
      }
    }
    addOnFailureListener { exception ->
      if (continuation.isActive) {
        continuation.resumeWithException(exception)
      }
    }
    addOnCanceledListener {
      if (continuation.isActive) {
        continuation.cancel()
      }
    }
  }
}
