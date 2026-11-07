package com.example.voice

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ime.ImeDiagnosticsLogger

class VoicePermissionActivity : Activity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val granted =
      ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED

    ImeDiagnosticsLogger.log(
      category = "VOICE_TYPING",
      event = "VoicePermissionActivity.onCreate",
      details = "alreadyGranted=$granted",
    )
    Log.d(TAG, "VoicePermissionActivity.onCreate: alreadyGranted=$granted")

    if (granted) {
      deliverPermissionResult(true)
      finish()
      return
    }

    ActivityCompat.requestPermissions(
      this,
      arrayOf(Manifest.permission.RECORD_AUDIO),
      REQUEST_RECORD_AUDIO,
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == REQUEST_RECORD_AUDIO) {
      val granted =
        grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
      ImeDiagnosticsLogger.log(
        category = "VOICE_TYPING",
        event = "VoicePermissionActivity.onRequestPermissionsResult",
        details = "granted=$granted",
      )
      Log.d(TAG, "onRequestPermissionsResult: granted=$granted")

      if (granted) {
        Toast.makeText(
            applicationContext,
            "Microphone permission granted — tap mic to speak",
            Toast.LENGTH_SHORT,
          )
          .show()
        deliverPermissionResult(true)
      } else {
        Toast.makeText(
            applicationContext,
            "Microphone permission is required for voice typing",
            Toast.LENGTH_SHORT,
          )
          .show()
        deliverPermissionResult(false)
      }
      finish()
    }
  }

  override fun onDestroy() {
    if (isFinishing) {
      clearPermissionCallback()
    }
    super.onDestroy()
  }

  companion object {
    private const val TAG = "PersonalKeyboardVoice"
    private const val REQUEST_RECORD_AUDIO = 1001
    @Volatile var onPermissionResultCallback: ((Boolean) -> Unit)? = null

    fun deliverPermissionResult(granted: Boolean) {
      val callback = onPermissionResultCallback
      onPermissionResultCallback = null
      callback?.invoke(granted)
    }

    fun clearPermissionCallback() {
      onPermissionResultCallback = null
    }
  }
}
