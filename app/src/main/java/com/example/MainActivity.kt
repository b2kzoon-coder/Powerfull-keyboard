package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.settings.SettingsHomeScreen
import com.example.settings.SettingsViewModel
import com.example.ui.theme.PersonalKeyboardAppTheme

class MainActivity : ComponentActivity() {

  private val settingsViewModel: SettingsViewModel by viewModels {
    SettingsViewModel.Factory(applicationContext)
  }

  private val audioPermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    if (
      ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
        PackageManager.PERMISSION_GRANTED
    ) {
      runCatching { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
    }

    setContent {
      PersonalKeyboardAppTheme {
        SettingsHomeScreen(viewModel = settingsViewModel)
      }
    }
  }
}
