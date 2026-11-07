package com.example.update

import androidx.compose.runtime.Immutable

@Immutable
data class GitHubReleaseInfo(
  val tagName: String,
  val versionName: String,
  val versionCode: Int,
  val releaseNotes: String,
  val apkDownloadUrl: String,
  val apkFileName: String,
  val apkSizeBytes: Long,
  val publishedAt: String,
)

sealed interface UpdateUiState {
  data object Idle : UpdateUiState

  data object Checking : UpdateUiState

  data class UpToDate(
    val currentVersionName: String,
    val currentVersionCode: Int,
    val lastCheckedTimeMs: Long = System.currentTimeMillis(),
  ) : UpdateUiState

  data class UpdateAvailable(
    val releaseInfo: GitHubReleaseInfo,
    val currentVersionName: String,
    val currentVersionCode: Int,
  ) : UpdateUiState

  data class Downloading(
    val releaseInfo: GitHubReleaseInfo,
    val progressPercent: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
  ) : UpdateUiState

  data class ReadyToInstall(
    val releaseInfo: GitHubReleaseInfo,
    val apkFilePath: String,
  ) : UpdateUiState

  data class Error(
    val message: String,
    val isNetworkError: Boolean = false,
    val canRetry: Boolean = true,
  ) : UpdateUiState
}
