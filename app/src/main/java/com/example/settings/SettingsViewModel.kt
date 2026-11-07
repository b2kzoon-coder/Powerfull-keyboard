package com.example.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.theme.KeyboardThemePreset
import com.example.update.ApkInstallerHelper
import com.example.update.ApkSecurityVerifier
import com.example.update.GitHubReleaseInfo
import com.example.update.GitHubUpdateManager
import com.example.update.RateLimitException
import com.example.update.UpdateUiState
import com.example.voice.VoiceRecognitionMode
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
data class ImeSystemStatus(
  val isEnabled: Boolean = false,
  val isSelected: Boolean = false,
)

class SettingsViewModel(
  private val repository: KeyboardPreferencesRepository,
  private val updateManager: GitHubUpdateManager? = null,
) : ViewModel() {

  val userConfig: StateFlow<KeyboardUserConfig> = repository.config

  private val _imeStatus = MutableStateFlow(ImeSystemStatus())
  val imeStatus: StateFlow<ImeSystemStatus> = _imeStatus.asStateFlow()

  private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
  val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

  private var checkUpdateJob: Job? = null
  private var downloadJob: Job? = null

  internal var installedVersionCodeOverride: Int? = null
  internal var installedVersionNameOverride: String? = null

  private val effectiveInstalledVersionCode: Int
    get() = installedVersionCodeOverride ?: BuildConfig.VERSION_CODE

  private val effectiveInstalledVersionName: String
    get() = installedVersionNameOverride ?: BuildConfig.VERSION_NAME

  fun checkForUpdates(isAutomatic: Boolean = false) {
    val manager = updateManager ?: return
    if (_updateState.value is UpdateUiState.Checking ||
      _updateState.value is UpdateUiState.Downloading ||
      _updateState.value is UpdateUiState.ReadyToInstall
    ) {
      return
    }
    if (isAutomatic && _updateState.value is UpdateUiState.UpdateAvailable) return

    val currentCode = effectiveInstalledVersionCode
    val currentName = effectiveInstalledVersionName

    if (isAutomatic && !manager.shouldPerformAutomaticCheck()) {
      val cached = manager.getCachedReleaseInfo()
      if (cached != null && manager.isNewerThanCurrent(cached, currentCode, currentName)) {
        _updateState.value =
          UpdateUiState.UpdateAvailable(
            releaseInfo = cached,
            currentVersionName = currentName,
            currentVersionCode = currentCode,
          )
      } else if (_updateState.value is UpdateUiState.Idle) {
        _updateState.value =
          UpdateUiState.UpToDate(
            currentVersionName = currentName,
            currentVersionCode = currentCode,
          )
      }
      return
    }

    checkUpdateJob?.cancel()
    checkUpdateJob =
      viewModelScope.launch {
        _updateState.value = UpdateUiState.Checking
        val result = manager.checkForUpdates(forceRefresh = !isAutomatic)
        result.fold(
          onSuccess = { release ->
            if (release != null && manager.isNewerThanCurrent(release, currentCode, currentName)) {
              _updateState.value =
                UpdateUiState.UpdateAvailable(
                  releaseInfo = release,
                  currentVersionName = currentName,
                  currentVersionCode = currentCode,
                )
            } else {
              _updateState.value =
                UpdateUiState.UpToDate(
                  currentVersionName = currentName,
                  currentVersionCode = currentCode,
                )
            }
          },
          onFailure = { error ->
            if (error is RateLimitException) {
              val cached = manager.getCachedReleaseInfo()
              if (cached != null && manager.isNewerThanCurrent(cached, currentCode, currentName)) {
                _updateState.value =
                  UpdateUiState.UpdateAvailable(
                    releaseInfo = cached,
                    currentVersionName = currentName,
                    currentVersionCode = currentCode,
                  )
              } else if (isAutomatic) {
                _updateState.value =
                  UpdateUiState.UpToDate(
                    currentVersionName = currentName,
                    currentVersionCode = currentCode,
                  )
              } else {
                _updateState.value =
                  UpdateUiState.Error(
                    message =
                      error.message
                        ?: "GitHub API rate limit reached. Please wait a moment and try again.",
                    isNetworkError = false,
                    isRateLimited = true,
                    retryAfterMs = error.retryAfterMs,
                  )
              }
            } else {
              _updateState.value =
                UpdateUiState.Error(
                  message = error.localizedMessage ?: "Failed to check for updates",
                  isNetworkError = true,
                )
            }
          },
        )
      }
  }

  fun startDownload(context: Context, release: GitHubReleaseInfo) {
    val manager = updateManager ?: return
    if (_updateState.value is UpdateUiState.Downloading) return

    val resolvedUrl =
      manager.resolvePublicApkDownloadUrl(
        rawUrl = release.apkDownloadUrl,
        tagName = release.tagName,
        assetFileName = release.apkFileName,
      )
    val effectiveRelease = release.copy(apkDownloadUrl = resolvedUrl)
    val installedCode = effectiveInstalledVersionCode.toLong()

    downloadJob?.cancel()
    downloadJob =
      viewModelScope.launch {
        _updateState.value =
          UpdateUiState.Downloading(
            releaseInfo = effectiveRelease,
            progressPercent = 0,
            bytesDownloaded = 0L,
            totalBytes = effectiveRelease.apkSizeBytes,
          )

        val downloadResult =
          manager.downloadApk(
            downloadUrl = effectiveRelease.apkDownloadUrl,
            fileName = effectiveRelease.apkFileName,
            onProgress = { percent, downloaded, total ->
              _updateState.value =
                UpdateUiState.Downloading(
                  releaseInfo = effectiveRelease,
                  progressPercent = percent,
                  bytesDownloaded = downloaded,
                  totalBytes = total,
                )
            },
          )

        downloadResult.fold(
          onSuccess = { apkFile ->
            // Verify APK before allowing installation
            when (
              val verifyResult =
                ApkSecurityVerifier.verifyApk(
                  context = context,
                  apkFile = apkFile,
                  installedVersionCode = installedCode,
                )
            ) {
              is ApkSecurityVerifier.VerificationResult.Success -> {
                _updateState.value =
                  UpdateUiState.ReadyToInstall(
                    releaseInfo = effectiveRelease,
                    apkFilePath = apkFile.absolutePath,
                  )
              }
              is ApkSecurityVerifier.VerificationResult.Failure -> {
                runCatching { apkFile.delete() }
                _updateState.value =
                  UpdateUiState.Error(
                    message = "Verification failed: ${verifyResult.reason}",
                    isNetworkError = false,
                    fallbackMirrorUrl = GitHubUpdateManager.FALLBACK_GOFILE_MIRROR_URL,
                  )
              }
            }
          },
          onFailure = { error ->
            _updateState.value =
              UpdateUiState.Error(
                message = "Download failed: ${error.localizedMessage ?: "Unable to download APK"}",
                isNetworkError = true,
                fallbackMirrorUrl = GitHubUpdateManager.FALLBACK_GOFILE_MIRROR_URL,
              )
          },
        )
      }
  }

  fun installDownloadedApk(context: Context, apkFilePath: String): Result<Unit> {
    val file = File(apkFilePath)
    val result = ApkInstallerHelper.launchApkInstaller(context, file)
    result.onFailure { error ->
      _updateState.value =
        UpdateUiState.Error(
          message = "Installation failed: ${error.localizedMessage ?: "Could not launch package installer"}",
          isNetworkError = false,
          fallbackMirrorUrl = GitHubUpdateManager.FALLBACK_GOFILE_MIRROR_URL,
        )
    }
    return result
  }

  fun reportInstallError(message: String) {
    _updateState.value =
      UpdateUiState.Error(
        message = "Installation failed: $message",
        isNetworkError = false,
        fallbackMirrorUrl = GitHubUpdateManager.FALLBACK_GOFILE_MIRROR_URL,
      )
  }

  fun setCustomUpdateState(state: UpdateUiState) {
    _updateState.value = state
  }

  fun refreshImeStatus(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val packageName = context.packageName
    val isEnabled =
      imm?.enabledInputMethodList?.any { it.packageName == packageName } ?: false

    val defaultIme =
      runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
      }
        .getOrNull()
        .orEmpty()

    val isSelected = defaultIme.startsWith("$packageName/")
    _imeStatus.value = ImeSystemStatus(isEnabled = isEnabled, isSelected = isSelected)
  }

  fun openSystemInputMethodSettings(context: Context) {
    val intent =
      Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    runCatching { context.startActivity(intent) }
  }

  fun showInputMethodPicker(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.showInputMethodPicker()
  }

  fun setPortraitHeightPercent(percent: Int) = repository.setPortraitHeightPercent(percent)

  fun setLandscapeHeightPercent(percent: Int) = repository.setLandscapeHeightPercent(percent)

  fun setThemePreset(preset: KeyboardThemePreset) = repository.setThemePreset(preset)

  fun setShowNumberRow(enabled: Boolean) = repository.setShowNumberRow(enabled)

  fun setShowToolbar(enabled: Boolean) = repository.setShowToolbar(enabled)

  fun setCutCopyPasteEnabled(enabled: Boolean) = repository.setCutCopyPasteEnabled(enabled)

  fun setHapticFeedback(enabled: Boolean) = repository.setHapticFeedback(enabled)

  fun setSoundOnKeypress(enabled: Boolean) = repository.setSoundOnKeypress(enabled)

  fun setAutoCapitalize(enabled: Boolean) = repository.setAutoCapitalize(enabled)

  fun setDoubleSpacePeriod(enabled: Boolean) = repository.setDoubleSpacePeriod(enabled)

  fun setVoiceRecognitionMode(mode: VoiceRecognitionMode) =
    repository.setVoiceRecognitionMode(mode)

  fun setVoiceNoiseCancellation(enabled: Boolean) = repository.setVoiceNoiseCancellation(enabled)

  fun setVoiceEnhancement(enabled: Boolean) = repository.setVoiceEnhancement(enabled)

  fun setVoiceInputEnabled(enabled: Boolean) = repository.setVoiceInputEnabled(enabled)

  fun setVoiceBanglishMode(enabled: Boolean) = repository.setVoiceBanglishMode(enabled)

  fun setVoiceAutoPunctuation(enabled: Boolean) = repository.setVoiceAutoPunctuation(enabled)

  fun setVoiceWaveformAndHaptics(enabled: Boolean) = repository.setVoiceWaveformAndHaptics(enabled)

  class Factory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      val appContext = context.applicationContext
      return SettingsViewModel(
        repository = KeyboardPreferencesRepository.getInstance(appContext),
        updateManager = GitHubUpdateManager(appContext),
      ) as T
    }
  }
}
