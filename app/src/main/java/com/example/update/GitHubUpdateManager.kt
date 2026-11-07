package com.example.update

import android.content.Context
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GitHubUpdateManager(
  private val context: Context,
) {
  private val appContext = context.applicationContext

  // Configurable repository target with safe default
  companion object {
    const val DEFAULT_GITHUB_REPO = "lionhitgo/PersonalKeyboard"
    private const val GITHUB_API_URL = "https://api.github.com/repos"
    private const val TIMEOUT_MS = 15000
    private const val UPDATES_DIR = "updates"
  }

  var repositorySlug: String = DEFAULT_GITHUB_REPO

  suspend fun checkForUpdates(): Result<GitHubReleaseInfo?> =
    withContext(Dispatchers.IO) {
      runCatching {
        val endpoint = "$GITHUB_API_URL/$repositorySlug/releases/latest"
        val url = URL(endpoint)
        val connection = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = TIMEOUT_MS
          readTimeout = TIMEOUT_MS
          setRequestProperty("Accept", "application/vnd.github.v3+json")
          setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")
        }

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
          // Repository or release not created yet -> up to date
          return@runCatching null
        }

        if (responseCode !in 200..299) {
          val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
          throw IllegalStateException("GitHub API HTTP $responseCode: $errorText")
        }

        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        val releaseObj = JSONObject(jsonString)

        parseReleaseInfo(releaseObj)
      }
    }

  fun parseReleaseInfo(jsonObj: JSONObject): GitHubReleaseInfo? {
    val tagName = jsonObj.optString("tag_name", "").trim()
    val name = jsonObj.optString("name", tagName).trim()
    val body = jsonObj.optString("body", "").trim()
    val publishedAt = jsonObj.optString("published_at", "")

    // Parse version from tag or name (e.g. "v1.1", "1.1", "v2")
    val cleanedTag = tagName.removePrefix("v").removePrefix("V")
    val parsedVersionName = if (cleanedTag.isNotEmpty()) cleanedTag else name

    // Extract versionCode from tag or release notes or fallback numeric calculation
    val parsedVersionCode = extractVersionCode(tagName, body)

    // Locate APK asset from assets list
    val assetsArray = jsonObj.optJSONArray("assets") ?: JSONArray()
    var apkAssetUrl: String? = null
    var apkAssetName = "PersonalKeyboard-update.apk"
    var apkAssetSize: Long = 0L

    for (i in 0 until assetsArray.length()) {
      val asset = assetsArray.getJSONObject(i)
      val assetName = asset.optString("name", "")
      val contentType = asset.optString("content_type", "")
      val browserDownloadUrl = asset.optString("browser_download_url", "")

      if (assetName.endsWith(".apk", ignoreCase = true) || contentType == "application/vnd.android.package-archive") {
        apkAssetUrl = browserDownloadUrl
        apkAssetName = assetName
        apkAssetSize = asset.optLong("size", 0L)
        break
      }
    }

    if (apkAssetUrl.isNullOrBlank()) {
      return null
    }

    return GitHubReleaseInfo(
      tagName = tagName,
      versionName = parsedVersionName,
      versionCode = parsedVersionCode,
      releaseNotes = body,
      apkDownloadUrl = apkAssetUrl,
      apkFileName = apkAssetName,
      apkSizeBytes = apkAssetSize,
      publishedAt = publishedAt,
    )
  }

  fun isNewerThanCurrent(release: GitHubReleaseInfo): Boolean {
    val currentCode = BuildConfig.VERSION_CODE
    if (release.versionCode > currentCode) return true
    if (release.versionCode < currentCode && release.versionCode > 0) return false

    // If version codes equal or zero, compare semantic versions (e.g. "1.1" > "1.0")
    return compareSemanticVersions(release.versionName, BuildConfig.VERSION_NAME) > 0
  }

  suspend fun downloadApk(
    downloadUrl: String,
    fileName: String,
    onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit,
  ): Result<File> =
    withContext(Dispatchers.IO) {
      runCatching {
        val updatesDir = File(appContext.cacheDir, UPDATES_DIR).apply { mkdirs() }
        val targetFile = File(updatesDir, fileName)
        if (targetFile.exists()) {
          targetFile.delete()
        }

        var currentUrl = downloadUrl
        var redirects = 0
        var connection: HttpURLConnection

        while (true) {
          val url = URL(currentUrl)
          connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")
          }

          val status = connection.responseCode
          if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
            status == HttpURLConnection.HTTP_MOVED_PERM ||
            status == 307 || status == 308
          ) {
            val newUrl = connection.getHeaderField("Location")
            if (newUrl.isNullOrBlank() || redirects++ >= 5) {
              throw IllegalStateException("Too many redirects downloading APK")
            }
            connection.disconnect()
            currentUrl = newUrl
            continue
          }

          if (status !in 200..299) {
            throw IllegalStateException("HTTP error $status during APK download")
          }
          break
        }

        val totalLength = connection.contentLengthLong
        var totalBytesRead = 0L

        connection.inputStream.use { input: InputStream ->
          FileOutputStream(targetFile).use { output ->
            val buffer = ByteArray(32768)
            var bytesRead: Int
            var lastReportedPercent = -1

            while (input.read(buffer).also { bytesRead = it } != -1) {
              output.write(buffer, 0, bytesRead)
              totalBytesRead += bytesRead

              val percent =
                if (totalLength > 0) {
                  ((totalBytesRead * 100) / totalLength).toInt().coerceIn(0, 100)
                } else {
                  -1
                }

              if (percent != lastReportedPercent) {
                lastReportedPercent = percent
                onProgress(percent, totalBytesRead, totalLength)
              }
            }
            output.flush()
          }
        }

        targetFile
      }
    }

  private fun extractVersionCode(tag: String, body: String): Int {
    // 1. Look for explicit "versionCode: 2" or "versionCode=2" in release notes
    val vcRegex = Regex("""versionCode[:=\s]+(\d+)""", RegexOption.IGNORE_CASE)
    vcRegex.find(body)?.groupValues?.get(1)?.toIntOrNull()?.let { return it }

    // 2. Parse tag like "v2" or "v1.2.3"
    val digitsOnly = tag.filter { it.isDigit() }
    val semverParts = tag.removePrefix("v").removePrefix("V").split(".")
    if (semverParts.isNotEmpty()) {
      val major = semverParts.getOrNull(0)?.toIntOrNull() ?: 1
      val minor = semverParts.getOrNull(1)?.toIntOrNull() ?: 0
      val patch = semverParts.getOrNull(2)?.toIntOrNull() ?: 0
      return (major * 10000) + (minor * 100) + patch
    }

    return digitsOnly.toIntOrNull() ?: 1
  }

  private fun compareSemanticVersions(v1: String, v2: String): Int {
    val p1 = v1.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
    val p2 = v2.removePrefix("v").removePrefix("V").split(".").map { it.toIntOrNull() ?: 0 }
    val maxLen = maxOf(p1.size, p2.size)
    for (i in 0 until maxLen) {
      val c1 = p1.getOrElse(i) { 0 }
      val c2 = p2.getOrElse(i) { 0 }
      if (c1 != c2) return c1.compareTo(c2)
    }
    return 0
  }
}
