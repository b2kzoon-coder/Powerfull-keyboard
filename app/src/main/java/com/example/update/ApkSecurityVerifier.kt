package com.example.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.BuildConfig
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Arrays

object ApkSecurityVerifier {

  const val EXPECTED_SIGNING_CERT_SHA256 =
    "c749dc26adcecb8988551c4589666451483a6a46ec30e309d769b0692c523a54"

  private const val APK_SIG_BLOCK_MAGIC_HI = 0x3234206b636f6c42L
  private const val APK_SIG_BLOCK_MAGIC_LO = 0x20676953204b5041L
  private const val APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 0x7109871a

  sealed class VerificationResult {
    data class Success(
      val verifiedPackageName: String,
      val verifiedVersionCode: Long,
      val verifiedVersionName: String,
      val certificateSha256: String = EXPECTED_SIGNING_CERT_SHA256,
    ) : VerificationResult()

    data class Failure(val reason: String) : VerificationResult()
  }

  fun verifyApk(
    context: Context,
    apkFile: File,
    installedVersionCode: Long = BuildConfig.VERSION_CODE.toLong(),
  ): VerificationResult {
    if (!apkFile.exists() || apkFile.length() <= 4L) {
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
      runCatching { pm.getPackageArchiveInfo(apkFile.absolutePath, archiveFlags) }
        .getOrElse {
          runCatching { pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_META_DATA) }
            .getOrNull()
        }
        ?: return VerificationResult.Failure("Package installer could not parse APK archive info")

    // 1. Verify Package Name
    val expectedPackageName = BuildConfig.APPLICATION_ID
    if (packageInfo.packageName != expectedPackageName) {
      return VerificationResult.Failure(
        "Package name mismatch: expected $expectedPackageName, got ${packageInfo.packageName}"
      )
    }

    // 2. Verify Version Code is greater than installed version
    val apkVersionCode: Long =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
      }

    if (apkVersionCode <= installedVersionCode) {
      return VerificationResult.Failure(
        "Downloaded APK version ($apkVersionCode) is not newer than currently installed ($installedVersionCode)"
      )
    }

    // 3. Verify Signing Certificate matches current app signature
    val installedSignatures = getInstalledSignatures(context)
    val apkSignatures =
      getApkSignatures(packageInfo).ifEmpty { extractV2SigningCertificates(apkFile) }

    if (apkSignatures.isEmpty()) {
      return VerificationResult.Failure("Downloaded APK is not signed with a valid certificate.")
    }

    val matchedCertSha256: String? =
      if (installedSignatures.isNotEmpty()) {
        apkSignatures.map { computeSha256(it) }.firstOrNull { apkSha ->
          installedSignatures.any { inst -> computeSha256(inst).equals(apkSha, ignoreCase = true) }
        }
      } else {
        apkSignatures.map { computeSha256(it) }.firstOrNull { apkSha ->
          apkSha.equals(EXPECTED_SIGNING_CERT_SHA256, ignoreCase = true)
        }
      }

    if (matchedCertSha256 == null) {
      return VerificationResult.Failure(
        "APK signing certificate mismatch. Cannot install over current application without signature match."
      )
    }

    return VerificationResult.Success(
      verifiedPackageName = packageInfo.packageName,
      verifiedVersionCode = apkVersionCode,
      verifiedVersionName = packageInfo.versionName ?: "",
      certificateSha256 = matchedCertSha256,
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

  /**
   * Extracts X.509 DER signing certificates directly from the APK Signature Scheme v2 block
   * (ID 0x7109871a) when PackageManager does not populate signingInfo (e.g. archive inspection
   * in JVM/Robolectric environments).
   */
  fun extractV2SigningCertificates(apkFile: File): List<ByteArray> {
    return runCatching {
      RandomAccessFile(apkFile, "r").use { raf ->
        val fileLen = raf.length()
        if (fileLen < 22L) return emptyList()

        // Locate ZIP End of Central Directory (EOCD: 0x06054b50)
        val maxSearch = minOf(fileLen, 65557L).toInt()
        val tail = ByteArray(maxSearch)
        raf.seek(fileLen - maxSearch)
        raf.readFully(tail)
        val tailBuf = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)

        var eocdOffsetInTail = -1
        for (i in maxSearch - 22 downTo 0) {
          if (tailBuf.getInt(i) == 0x06054b50) {
            eocdOffsetInTail = i
            break
          }
        }
        if (eocdOffsetInTail < 0) return emptyList()

        val cdOffset = tailBuf.getInt(eocdOffsetInTail + 16).toLong() and 0xFFFFFFFFL
        if (cdOffset < 32L) return emptyList()

        // Read APK Signing Block footer (24 bytes before Central Directory)
        val footerBytes = ByteArray(24)
        raf.seek(cdOffset - 24L)
        raf.readFully(footerBytes)
        val footer = ByteBuffer.wrap(footerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val blockSizeFooter = footer.getLong(0)
        val magicLo = footer.getLong(8)
        val magicHi = footer.getLong(16)
        if (magicLo != APK_SIG_BLOCK_MAGIC_LO || magicHi != APK_SIG_BLOCK_MAGIC_HI) {
          return emptyList()
        }

        val totalBlockSize = (blockSizeFooter + 8L).toInt()
        val blockStart = cdOffset - totalBlockSize
        if (blockStart < 0L || totalBlockSize <= 32) return emptyList()

        val blockBytes = ByteArray(totalBlockSize)
        raf.seek(blockStart)
        raf.readFully(blockBytes)
        val blockBuf = ByteBuffer.wrap(blockBytes).order(ByteOrder.LITTLE_ENDIAN)
        val blockSizeHeader = blockBuf.getLong(0)
        if (blockSizeHeader != blockSizeFooter) return emptyList()

        // Iterate ID-value pairs in [8 .. totalBlockSize - 24)
        var pos = 8
        val endPos = totalBlockSize - 24
        val certs = mutableListOf<ByteArray>()
        while (pos + 12 <= endPos) {
          val pairLen = blockBuf.getLong(pos).toInt()
          if (pairLen < 4 || pos + 8 + pairLen > endPos) break
          val id = blockBuf.getInt(pos + 8)
          if (id == APK_SIGNATURE_SCHEME_V2_BLOCK_ID) {
            val v2Slice = ByteBuffer.wrap(blockBytes, pos + 12, pairLen - 4).order(ByteOrder.LITTLE_ENDIAN)
            val signersLen = v2Slice.int
            val signersEnd = v2Slice.position() + signersLen
            while (v2Slice.position() + 4 <= signersEnd) {
              val signerLen = v2Slice.int
              val signerEnd = v2Slice.position() + signerLen
              val signedDataLen = v2Slice.int
              val signedDataStart = v2Slice.position()
              val digestsLen = v2Slice.int
              v2Slice.position(v2Slice.position() + digestsLen)
              val certsLen = v2Slice.int
              val certsEnd = v2Slice.position() + certsLen
              while (v2Slice.position() + 4 <= certsEnd) {
                val certLen = v2Slice.int
                if (certLen > 0 && v2Slice.position() + certLen <= certsEnd) {
                  val certBytes = ByteArray(certLen)
                  v2Slice.get(certBytes)
                  certs.add(certBytes)
                } else {
                  break
                }
              }
              v2Slice.position(signedDataStart + signedDataLen)
              v2Slice.position(signerEnd)
            }
          }
          pos += 8 + pairLen
        }
        certs
      }
    }.getOrDefault(emptyList())
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
