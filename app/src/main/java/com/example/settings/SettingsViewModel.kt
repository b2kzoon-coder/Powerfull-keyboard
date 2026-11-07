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

  fun checkForUpdates(isAutomatic: Boolean = false) {
    val manager = updateManager ?: return
    if (_updateState.value is UpdateUiState.Downloading) return
    if (isAutomatic && _updateState.value is UpdateUiState.UpdateAvailable) return

    checkUpdateJob?.cancel()
    checkUpdateJob =
      viewModelScope.launch {
        _updateState.value = UpdateUiState.Checking
        val result = manager.checkForUpdates()
        result.fold(
          onSuccess = { release ->
            if (release != null && manager.isNewerThanCurrent(release)) {
              _updateState.value =
                UpdateUiState.UpdateAvailable(
                  releaseInfo = release,
                  currentVersionName = BuildConfig.VERSION_NAME,
                  currentVersionCode = BuildConfig.VERSION_CODE,
                )
            } else {
              _updateState.value =
                UpdateUiState.UpToDate(
                  currentVersionName = BuildConfig.VERSION_NAME,
                  currentVersionCode = BuildConfig.VERSION_CODE,
                )
            }
          },
          onFailure = { error ->
            _updateState.value =
              UpdateUiState.Error(
                message = error.localizedMessage ?: "Failed to check for updates",
                isNetworkError = true,
              )
          },
        )
      }
  }

  fun startDownload(context: Context, release: GitHubReleaseInfo) {
    val manager = updateManager ?: return
    if (_updateState.value is UpdateUiState.Downloading) return

    downloadJob?.cancel()
    downloadJob =
      viewModelScope.launch {
        _updateState.value =
          UpdateUiState.Downloading(
            releaseInfo = release,
            progressPercent = 0,
            bytesDownloaded = 0L,
            totalBytes = release.apkSizeBytes,
          )

        val downloadResult =
          manager.downloadApk(
            downloadUrl = release.apkDownloadUrl,
            fileName = release.apkFileName,
            onProgress = { percent, downloaded, total ->
              _updateState.value =
                UpdateUiState.Downloading(
                  releaseInfo = release,
                  progressPercent = percent,
                  bytesDownloaded = downloaded,
                  totalBytes = total,
                )
            },
          )

        downloadResult.fold(
          onSuccess = { apkFile ->
            // Verify APK before allowing installation
            when (val verifyResult = ApkSecurityVerifier.verifyApk(context, apkFile)) {
              is ApkSecurityVerifier.VerificationResult.Success -> {
                _updateState.value =
                  UpdateUiState.ReadyToInstall(
                    releaseInfo = release,
                    apkFilePath = apkFile.absolutePath,
                  )
              }
              is ApkSecurityVerifier.VerificationResult.Failure -> {
                runCatching { apkFile.delete() }
                _updateState.value =
                  UpdateUiState.Error(
                    message = "Verification failed: ${verifyResult.reason}",
                    isNetworkError = false,
                  )
              }
            }
          },
          onFailure = { error ->
            _updateState.value =
              UpdateUiState.Error(
                message = error.localizedMessage ?: "Download failed",
                isNetworkError = true,
              )
          },
        )
      }
  }

  fun installDownloadedApk(context: Context, apkFilePath: String): Result<Unit> {
    val file = File(apkFilePath)
    return ApkInstallerHelper.launchApkInstaller(context, file)
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
