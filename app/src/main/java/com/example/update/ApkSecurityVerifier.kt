package com.example.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import java.io.File
import java.security.MessageDigest
import java.util.Arrays

object ApkSecurityVerifier {

  sealed class VerificationResult {
    data class Success(
      val verifiedPackageName: String,
      val verifiedVersionCode: Long,
      val verifiedVersionName: String,
    ) : VerificationResult()

    data class Failure(val reason: String) : VerificationResult()
  }

  fun verifyApk(context: Context, apkFile: File): VerificationResult {
    if (!apkFile.exists() || apkFile.length() <= 0) {
      return VerificationResult.Failure("APK file does not exist or is empty")
    }

    val pm = context.packageManager
    val archiveFlags =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA
      } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES or PackageManager.GET_META_DATA
      }

    val packageInfo: PackageInfo =
      pm.getPackageArchiveInfo(apkFile.absolutePath, archiveFlags)
        ?: return VerificationResult.Failure("Package installer could not parse APK archive info")

    // 1. Verify Package Name
    val expectedPackageName = BuildConfig.APPLICATION_ID
    if (packageInfo.packageName != expectedPackageName) {
      return VerificationResult.Failure(
        "Package name mismatch: expected $expectedPackageName, got ${packageInfo.packageName}"
      )
    }

    // 2. Verify Version Code is greater than current
    val apkVersionCode: Long =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
      }

    val currentVersionCode = BuildConfig.VERSION_CODE.toLong()
    if (apkVersionCode <= currentVersionCode) {
      return VerificationResult.Failure(
        "Downloaded APK version ($apkVersionCode) is not newer than currently installed ($currentVersionCode)"
      )
    }

    // 3. Verify Signing Certificate matches current app signature
    val installedSignatures = getInstalledSignatures(context)
    val apkSignatures = getApkSignatures(packageInfo)

    if (installedSignatures.isNotEmpty() && apkSignatures.isNotEmpty()) {
      val signatureMatches = matchesAnySignature(installedSignatures, apkSignatures)
      if (!signatureMatches) {
        return VerificationResult.Failure(
          "APK signing certificate mismatch. Cannot install over current application without signature match."
        )
      }
    }

    return VerificationResult.Success(
      verifiedPackageName = packageInfo.packageName,
      verifiedVersionCode = apkVersionCode,
      verifiedVersionName = packageInfo.versionName ?: "",
    )
  }

  private fun getInstalledSignatures(context: Context): List<ByteArray> {
    return runCatching {
      val pm = context.packageManager
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = pi.signingInfo ?: return emptyList()
        if (signingInfo.hasMultipleSigners()) {
          signingInfo.apkContentsSigners.map { it.toByteArray() }
        } else {
          signingInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
        }
      } else {
        @Suppress("DEPRECATION")
        val pi = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        @Suppress("DEPRECATION")
        pi.signatures?.map { it.toByteArray() } ?: emptyList()
      }
    }.getOrDefault(emptyList())
  }

  private fun getApkSignatures(packageInfo: PackageInfo): List<ByteArray> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val signingInfo = packageInfo.signingInfo ?: return emptyList()
      if (signingInfo.hasMultipleSigners()) {
        signingInfo.apkContentsSigners.map { it.toByteArray() }
      } else {
        signingInfo.signingCertificateHistory?.map { it.toByteArray() } ?: emptyList()
      }
    } else {
      @Suppress("DEPRECATION")
      packageInfo.signatures?.map { it.toByteArray() } ?: emptyList()
    }
  }

  private fun matchesAnySignature(
    installed: List<ByteArray>,
    incoming: List<ByteArray>,
  ): Boolean {
    for (inst in installed) {
      for (inc in incoming) {
        if (Arrays.equals(inst, inc)) return true
        // Also compare SHA-256 fingerprint
        if (computeSha256(inst) == computeSha256(inc)) return true
      }
    }
    return false
  }

  fun computeSha256(data: ByteArray): String {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(data)
    return digest.joinToString("") { "%02x".format(it) }
  }

  fun computeFileSha256(file: File): String {
    val md = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { stream ->
      val buffer = ByteArray(16384)
      var bytesRead: Int
      while (stream.read(buffer).also { bytesRead = it } != -1) {
        md.update(buffer, 0, bytesRead)
      }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
  }
}
