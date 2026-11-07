package com.example.update

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GitHubUpdateManager(
  private val context: Context,
) {
  private val appContext = context.applicationContext
  private val prefs: SharedPreferences =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  // Configurable repository target with safe default
  companion object {
    const val DEFAULT_GITHUB_REPO = "b2kzoon-coder/Powerfull-keyboard"
    private const val GITHUB_API_URL = "https://api.github.com/repos"
    private const val GITHUB_WEB_URL = "https://github.com"
    private const val TIMEOUT_MS = 15000
    private const val UPDATES_DIR = "updates"

    private const val PREFS_NAME = "github_update_manager_prefs"
    private const val KEY_LAST_AUTO_CHECK_MS = "last_auto_check_ms"
    private const val KEY_BACKOFF_UNTIL_MS = "backoff_until_ms"
    private const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"
    private const val KEY_CACHED_ETAG = "cached_etag"
    private const val KEY_CACHED_LAST_MODIFIED = "cached_last_modified"
    private const val KEY_CACHED_RELEASE_JSON = "cached_release_json"

    // 15 minutes minimum interval between automatic background checks
    const val MIN_AUTO_CHECK_INTERVAL_MS = 15 * 60 * 1000L

    // Exponential backoff bounds (30s -> 60s -> 120s -> 240s ... up to 30m)
    const val BASE_BACKOFF_MS = 30_000L
    const val MAX_BACKOFF_MS = 30 * 60 * 1000L
  }

  var repositorySlug: String = DEFAULT_GITHUB_REPO
  var enableWebFallbackOnRateLimit: Boolean = true

  fun shouldPerformAutomaticCheck(nowMs: Long = System.currentTimeMillis()): Boolean {
    val backoffUntil = prefs.getLong(KEY_BACKOFF_UNTIL_MS, 0L)
    if (nowMs < backoffUntil) {
      return false
    }
    val lastCheck = prefs.getLong(KEY_LAST_AUTO_CHECK_MS, 0L)
    if (lastCheck > 0L && (nowMs - lastCheck) < MIN_AUTO_CHECK_INTERVAL_MS) {
      return false
    }
    return true
  }

  fun getRemainingBackoffMs(nowMs: Long = System.currentTimeMillis()): Long {
    val backoffUntil = prefs.getLong(KEY_BACKOFF_UNTIL_MS, 0L)
    return (backoffUntil - nowMs).coerceAtLeast(0L)
  }

  fun computeExponentialBackoffMs(failureCount: Int): Long {
    val shift = failureCount.coerceIn(0, 6)
    val rawBackoff = BASE_BACKOFF_MS * (1L shl shift)
    return min(rawBackoff, MAX_BACKOFF_MS)
  }

  fun isRateLimitResponse(
    responseCode: Int,
    rateLimitRemaining: String?,
    retryAfterHeader: String?,
    errorBody: String,
  ): Boolean {
    if (responseCode == 429) return true
    if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
      if (rateLimitRemaining == "0") return true
      if (!retryAfterHeader.isNullOrBlank()) return true
      if (errorBody.contains("rate limit", ignoreCase = true) ||
        errorBody.contains("secondary rate limit", ignoreCase = true) ||
        errorBody.contains("abuse detection", ignoreCase = true)
      ) {
        return true
      }
    }
    return false
  }

  fun parseRateLimitDelayMs(
    retryAfterHeader: String?,
    rateLimitResetHeader: String?,
    failureCount: Int,
    nowMs: Long = System.currentTimeMillis(),
  ): Long {
    // 1. Check standard Retry-After header (seconds)
    retryAfterHeader?.trim()?.toLongOrNull()?.let { seconds ->
      if (seconds > 0) {
        return (seconds * 1000L).coerceIn(BASE_BACKOFF_MS, MAX_BACKOFF_MS)
      }
    }

    // 2. Check GitHub X-RateLimit-Reset header (UTC epoch seconds)
    rateLimitResetHeader?.trim()?.toLongOrNull()?.let { resetEpochSeconds ->
      val delayMs = (resetEpochSeconds * 1000L) - nowMs
      if (delayMs > 0L) {
        return delayMs.coerceIn(BASE_BACKOFF_MS, MAX_BACKOFF_MS)
      }
    }

    // 3. Fallback to exponential backoff
    return computeExponentialBackoffMs(failureCount)
  }

  fun recordRateLimitOrFailure(
    retryAfterHeader: String? = null,
    rateLimitResetHeader: String? = null,
    nowMs: Long = System.currentTimeMillis(),
  ): Long {
    val currentFailures = prefs.getInt(KEY_CONSECUTIVE_FAILURES, 0)
    val delayMs =
      parseRateLimitDelayMs(
        retryAfterHeader = retryAfterHeader,
        rateLimitResetHeader = rateLimitResetHeader,
        failureCount = currentFailures,
        nowMs = nowMs,
      )
    val nextFailures = (currentFailures + 1).coerceAtMost(10)
    prefs
      .edit()
      .putInt(KEY_CONSECUTIVE_FAILURES, nextFailures)
      .putLong(KEY_BACKOFF_UNTIL_MS, nowMs + delayMs)
      .apply()
    return delayMs
  }

  fun recordSuccess(
    etag: String? = null,
    lastModified: String? = null,
    releaseInfo: GitHubReleaseInfo? = null,
    nowMs: Long = System.currentTimeMillis(),
  ) {
    val editor =
      prefs
        .edit()
        .putLong(KEY_LAST_AUTO_CHECK_MS, nowMs)
        .putLong(KEY_BACKOFF_UNTIL_MS, 0L)
        .putInt(KEY_CONSECUTIVE_FAILURES, 0)

    if (!etag.isNullOrBlank()) {
      editor.putString(KEY_CACHED_ETAG, etag)
    }
    if (!lastModified.isNullOrBlank()) {
      editor.putString(KEY_CACHED_LAST_MODIFIED, lastModified)
    }
    if (releaseInfo != null) {
      editor.putString(KEY_CACHED_RELEASE_JSON, serializeReleaseInfo(releaseInfo).toString())
    } else {
      editor.remove(KEY_CACHED_RELEASE_JSON)
    }
    editor.apply()
  }

  fun clearRateLimitAndCache() {
    prefs.edit().clear().apply()
  }

  fun getCachedReleaseInfo(): GitHubReleaseInfo? {
    val cachedStr = prefs.getString(KEY_CACHED_RELEASE_JSON, null) ?: return null
    return runCatching {
      val obj = JSONObject(cachedStr)
      deserializeCachedReleaseInfo(obj)
    }.getOrNull()
  }

  suspend fun checkForUpdates(forceRefresh: Boolean = false): Result<GitHubReleaseInfo?> =
    withContext(Dispatchers.IO) {
      runCatching {
        val nowMs = System.currentTimeMillis()
        val remainingBackoffMs = getRemainingBackoffMs(nowMs)

        // If automatic check is within the 15-minute cooldown or backoff window, return cached state
        if (!forceRefresh && !shouldPerformAutomaticCheck(nowMs)) {
          return@runCatching getCachedReleaseInfo()
        }

        // If manual refresh is requested while api.github.com is in a rate-limit window,
        // use the zero-quota public GitHub web release endpoint directly
        if (remainingBackoffMs > 0L) {
          if (enableWebFallbackOnRateLimit) {
            val webResult = runCatching { fetchReleaseViaWebFallback() }
            if (webResult.isSuccess) {
              val release = webResult.getOrNull()
              prefs.edit().putLong(KEY_LAST_AUTO_CHECK_MS, nowMs).apply()
              return@runCatching release
            }
          }
          getCachedReleaseInfo()?.let { return@runCatching it }
          throw RateLimitException(remainingBackoffMs)
        }

        val endpoint = "$GITHUB_API_URL/$repositorySlug/releases/latest"
        val url = URL(endpoint)
        val connection =
          (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github.v3+json")
            setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")

            // Conditional request headers: 304 Not Modified responses do NOT count against GitHub's rate limit
            val cachedEtag = prefs.getString(KEY_CACHED_ETAG, null)
            if (!cachedEtag.isNullOrBlank()) {
              setRequestProperty("If-None-Match", cachedEtag)
            }
            val cachedLastModified = prefs.getString(KEY_CACHED_LAST_MODIFIED, null)
            if (!cachedLastModified.isNullOrBlank()) {
              setRequestProperty("If-Modified-Since", cachedLastModified)
            }
          }

        val responseCode = connection.responseCode

        // 304 Not Modified -> Release unchanged, 0 rate limit consumed
        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
          prefs
            .edit()
            .putLong(KEY_LAST_AUTO_CHECK_MS, nowMs)
            .putLong(KEY_BACKOFF_UNTIL_MS, 0L)
            .putInt(KEY_CONSECUTIVE_FAILURES, 0)
            .apply()
          return@runCatching getCachedReleaseInfo()
        }

        // 404 Not Found -> Repository has no published latest release yet
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
          recordSuccess(releaseInfo = null, nowMs = nowMs)
          return@runCatching null
        }

        // Check for HTTP 403 / 429 GitHub API rate limiting
        if (responseCode !in 200..299) {
          val errorText = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
          val rateLimitRemaining = connection.getHeaderField("X-RateLimit-Remaining")
          val rateLimitReset = connection.getHeaderField("X-RateLimit-Reset")
          val retryAfter = connection.getHeaderField("Retry-After")

          if (isRateLimitResponse(responseCode, rateLimitRemaining, retryAfter, errorText)) {
            val backoffDelayMs =
              recordRateLimitOrFailure(
                retryAfterHeader = retryAfter,
                rateLimitResetHeader = rateLimitReset,
                nowMs = nowMs,
              )

            // Fallback to zero-API-quota public web release discovery
            if (enableWebFallbackOnRateLimit) {
              val webFallback = runCatching { fetchReleaseViaWebFallback() }
              if (webFallback.isSuccess) {
                val release = webFallback.getOrNull()
                if (release != null) {
                  prefs
                    .edit()
                    .putString(KEY_CACHED_RELEASE_JSON, serializeReleaseInfo(release).toString())
                    .apply()
                }
                return@runCatching release
              }
            }

            // Return cached release info if available
            getCachedReleaseInfo()?.let { return@runCatching it }

            throw RateLimitException(backoffDelayMs)
          }

          recordRateLimitOrFailure(nowMs = nowMs)
          throw IllegalStateException("Unable to check for updates (HTTP $responseCode)")
        }

        val etag = connection.getHeaderField("ETag")
        val lastModified = connection.getHeaderField("Last-Modified")
        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        val releaseObj = JSONObject(jsonString)

        val parsed = parseReleaseInfo(releaseObj)
        recordSuccess(etag = etag, lastModified = lastModified, releaseInfo = parsed, nowMs = nowMs)
        parsed
      }
    }

  /**
   * Zero-API-quota fallback that uses GitHub's public web release redirect
   * (`https://github.com/<owner>/<repo>/releases/latest`) and expanded_assets fragment
   * (`https://github.com/<owner>/<repo>/releases/expanded_assets/<tag>`).
   * Does not require an API token and does not count against api.github.com's 60 req/hr limit.
   */
  internal fun fetchReleaseViaWebFallback(): GitHubReleaseInfo? {
    val latestWebUrl = "$GITHUB_WEB_URL/$repositorySlug/releases/latest"
    val conn =
      (URL(latestWebUrl).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = TIMEOUT_MS
        readTimeout = TIMEOUT_MS
        instanceFollowRedirects = false
        setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")
      }

    val status = conn.responseCode
    val location = conn.getHeaderField("Location").orEmpty()
    conn.disconnect()

    if (status == HttpURLConnection.HTTP_NOT_FOUND) {
      return null
    }

    if (status in 300..399) {
      val tagMarker = "/releases/tag/"
      val markerIdx = location.indexOf(tagMarker)
      if (markerIdx == -1) {
        // Redirected to /releases (no releases published yet)
        return null
      }
      val rawTag = location.substring(markerIdx + tagMarker.length).substringBefore("?").trim()
      val tagName = runCatching { URLDecoder.decode(rawTag, "UTF-8") }.getOrDefault(rawTag)
      if (tagName.isBlank()) return null

      // Fetch expanded_assets HTML fragment for this release tag
      val assetsUrl = "$GITHUB_WEB_URL/$repositorySlug/releases/expanded_assets/$tagName"
      val assetsConn =
        (URL(assetsUrl).openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = TIMEOUT_MS
          readTimeout = TIMEOUT_MS
          setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")
        }

      if (assetsConn.responseCode !in 200..299) {
        assetsConn.disconnect()
        return null
      }
      val assetsHtml = assetsConn.inputStream.bufferedReader().use { it.readText() }
      assetsConn.disconnect()

      return parseWebExpandedAssetsHtml(tagName, assetsHtml)
    }

    return null
  }

  fun parseWebExpandedAssetsHtml(tagName: String, html: String): GitHubReleaseInfo? {
    // Match href="/<owner>/<repo>/releases/download/<tag>/<file>.apk"
    val hrefRegex =
      Regex(
        """href=["'](/[^"']+/releases/download/[^"']+\.apk)["']""",
        RegexOption.IGNORE_CASE,
      )
    val match = hrefRegex.find(html) ?: return null
    val relativePath = match.groupValues[1]
    val fullDownloadUrl =
      if (relativePath.startsWith("http")) relativePath else "$GITHUB_WEB_URL$relativePath"
    val fileName =
      runCatching { URLDecoder.decode(relativePath.substringAfterLast('/'), "UTF-8") }
        .getOrDefault("app-release.apk")

    val cleanedTag = tagName.removePrefix("v").removePrefix("V")
    val versionName = if (cleanedTag.isNotEmpty()) cleanedTag else tagName
    val versionCode = extractVersionCode(tagName, "")

    return GitHubReleaseInfo(
      tagName = tagName,
      versionName = versionName,
      versionCode = versionCode,
      releaseNotes = "",
      apkDownloadUrl = fullDownloadUrl,
      apkFileName = fileName,
      apkSizeBytes = 0L,
      publishedAt = "",
    )
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

      if (assetName.endsWith(".apk", ignoreCase = true) ||
        contentType == "application/vnd.android.package-archive"
      ) {
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

    // If version codes equal or zero, compare semantic versions (e.g. "1.1" > "1.0.1")
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
          connection =
            (url.openConnection() as HttpURLConnection).apply {
              connectTimeout = TIMEOUT_MS
              readTimeout = TIMEOUT_MS
              instanceFollowRedirects = true
              setRequestProperty("User-Agent", "PersonalKeyboard-Updater/${BuildConfig.VERSION_NAME}")
            }

          val status = connection.responseCode
          if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
            status == HttpURLConnection.HTTP_MOVED_PERM ||
            status == 307 ||
            status == 308
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

    // 2. Compare semantic version tag against current BuildConfig.VERSION_NAME
    val cleanedTag = tag.removePrefix("v").removePrefix("V").trim()
    if (cleanedTag.contains(".")) {
      val cmp = compareSemanticVersions(cleanedTag, BuildConfig.VERSION_NAME)
      return when {
        cmp > 0 -> BuildConfig.VERSION_CODE + 1
        cmp == 0 -> BuildConfig.VERSION_CODE
        else -> (BuildConfig.VERSION_CODE - 1).coerceAtLeast(0)
      }
    }

    val digitsOnly = cleanedTag.filter { it.isDigit() }
    return digitsOnly.toIntOrNull() ?: BuildConfig.VERSION_CODE
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

  private fun serializeReleaseInfo(info: GitHubReleaseInfo): JSONObject {
    return JSONObject().apply {
      put("tagName", info.tagName)
      put("versionName", info.versionName)
      put("versionCode", info.versionCode)
      put("releaseNotes", info.releaseNotes)
      put("apkDownloadUrl", info.apkDownloadUrl)
      put("apkFileName", info.apkFileName)
      put("apkSizeBytes", info.apkSizeBytes)
      put("publishedAt", info.publishedAt)
    }
  }

  private fun deserializeCachedReleaseInfo(obj: JSONObject): GitHubReleaseInfo {
    return GitHubReleaseInfo(
      tagName = obj.optString("tagName", ""),
      versionName = obj.optString("versionName", ""),
      versionCode = obj.optInt("versionCode", 0),
      releaseNotes = obj.optString("releaseNotes", ""),
      apkDownloadUrl = obj.optString("apkDownloadUrl", ""),
      apkFileName = obj.optString("apkFileName", "app-release.apk"),
      apkSizeBytes = obj.optLong("apkSizeBytes", 0L),
      publishedAt = obj.optString("publishedAt", ""),
    )
  }
}
