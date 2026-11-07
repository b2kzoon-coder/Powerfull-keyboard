package com.example.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.BuildConfig
import com.example.update.ApkInstallerHelper
import com.example.update.GitHubReleaseInfo
import com.example.update.UpdateUiState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.keyboard.KeyFeedbackType
import com.example.keyboard.KeyboardFeedbackManager
import com.example.language.LanguageManager
import com.example.layout.KeyboardHeightBounds
import com.example.theme.KeyboardThemePreset
import com.example.theme.KeyboardThemeRepository
import com.example.voice.VoiceRecognitionMode
import kotlin.math.roundToInt

private val EnabledCheckGreen = Color(0xFF16A34A)

private enum class HeightDialogTarget {
  PORTRAIT,
  LANDSCAPE,
}

@Composable
fun SettingsHomeScreen(
  viewModel: SettingsViewModel,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val config by viewModel.userConfig.collectAsStateWithLifecycle()
  val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
  val updateState by viewModel.updateState.collectAsStateWithLifecycle()

  val feedbackManager = remember(context) {
    KeyboardFeedbackManager(context, KeyboardPreferencesRepository.getInstance(context))
  }
  DisposableEffect(feedbackManager) {
    onDispose { feedbackManager.release() }
  }

  var activeHeightDialog by remember { mutableStateOf<HeightDialogTarget?>(null) }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refreshImeStatus(context)
        viewModel.checkForUpdates(isAutomatic = true)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    viewModel.refreshImeStatus(context)
    viewModel.checkForUpdates(isAutomatic = true)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  activeHeightDialog?.let { target ->
    val isPortrait = target == HeightDialogTarget.PORTRAIT
    val title =
      if (isPortrait) {
        stringResource(R.string.label_keyboard_height_portrait)
      } else {
        stringResource(R.string.label_keyboard_height_landscape)
      }
    val initialPercent =
      if (isPortrait) config.portraitHeightPercent else config.landscapeHeightPercent

    KeyboardHeightDialog(
      title = title,
      initialPercent = initialPercent,
      onDismissCancel = { activeHeightDialog = null },
      onConfirmOk = { newPercent ->
        if (isPortrait) {
          viewModel.setPortraitHeightPercent(newPercent)
        } else {
          viewModel.setLandscapeHeightPercent(newPercent)
        }
        activeHeightDialog = null
      },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier =
          Modifier.widthIn(max = 600.dp)
            .fillMaxWidth()
            .testTag("settings_home_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // 1. Clean Header
        item {
          Row(
            modifier = Modifier.fillMaxWidth().testTag("settings_header"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Outlined.Keyboard,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(24.dp),
                )
              }
              Column {
                Text(
                  text = stringResource(R.string.settings_title),
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                  text = stringResource(R.string.settings_subtitle),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            IconButton(
              onClick = { viewModel.refreshImeStatus(context) },
              modifier = Modifier.testTag("refresh_ime_status_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.btn_refresh_status),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }

        // 2. Activation Card (Top)
        item {
          ImeActivationCard(
            imeStatus = imeStatus,
            onEnableClick = { viewModel.openSystemInputMethodSettings(context) },
            onSelectClick = { viewModel.showInputMethodPicker(context) },
          )
        }

        // 3. Keyboard Theme Selector (4 Themes at the top: Slate Dark, Crisp Light, OLED Pitch, Nord Frost)
        item {
          SettingsSectionGroup(
            title = stringResource(R.string.label_keyboard_theme),
            tag = "section_keyboard_theme",
          ) {
            Text(
              text = "Choose your preferred keyboard appearance style",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              KeyboardThemePreset.entries.forEach { preset ->
                val palette = KeyboardThemeRepository.paletteFor(preset)
                val isSelected = config.themePreset == preset
                val label =
                  if (preset == KeyboardThemePreset.OLED_BLACK) "OLED Pitch" else preset.displayName

                Column(
                  modifier =
                    Modifier.weight(1f)
                      .clip(RoundedCornerShape(10.dp))
                      .background(palette.keyboardBackground)
                      .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color =
                          if (isSelected) {
                            MaterialTheme.colorScheme.primary
                          } else {
                            palette.dividerColor
                          },
                        shape = RoundedCornerShape(10.dp),
                      )
                      .clickable { viewModel.setThemePreset(preset) }
                      .padding(vertical = 12.dp, horizontal = 4.dp)
                      .testTag("theme_option_${preset.id}"),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Box(
                    modifier =
                      Modifier.size(20.dp)
                        .clip(CircleShape)
                        .background(palette.accentKeyBackground)
                  )
                  Text(
                    text = label,
                    color = palette.keyTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                  )
                }
              }
            }

            SettingsRowDivider(modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))

            HeightPreferenceRow(
              title = stringResource(R.string.label_keyboard_height_portrait),
              percentValue = config.portraitHeightPercent,
              onClick = { activeHeightDialog = HeightDialogTarget.PORTRAIT },
              tag = "row_keyboard_height_portrait",
            )

            SettingsRowDivider(modifier = Modifier.padding(vertical = 4.dp))

            HeightPreferenceRow(
              title = stringResource(R.string.label_keyboard_height_landscape),
              percentValue = config.landscapeHeightPercent,
              onClick = { activeHeightDialog = HeightDialogTarget.LANDSCAPE },
              tag = "row_keyboard_height_landscape",
            )
          }
        }

        // 4. Typing & Feedback Section
        item {
          SettingsSectionGroup(
            title = stringResource(R.string.section_typing),
            tag = "section_typing",
          ) {
            // Keypress Sound
            SettingsSwitchRow(
              label = stringResource(R.string.label_sound_feedback),
              subtitle = "Premium iPhone-style keyboard click sound",
              checked = config.soundOnKeypress,
              onCheckedChange = { isChecked ->
                viewModel.setSoundOnKeypress(isChecked)
                if (isChecked) {
                  feedbackManager.trigger(KeyFeedbackType.CHARACTER)
                }
              },
              tag = "switch_sound_feedback",
            )

            SettingsRowDivider()

            // Keypress Vibration
            SettingsSwitchRow(
              label = stringResource(R.string.label_haptic_feedback),
              subtitle = "Crisp, low-latency tactile vibration on key press",
              checked = config.hapticFeedback,
              onCheckedChange = { isChecked ->
                viewModel.setHapticFeedback(isChecked)
                if (isChecked) {
                  feedbackManager.trigger(KeyFeedbackType.CHARACTER)
                }
              },
              tag = "switch_haptic_feedback",
            )

            SettingsRowDivider()

            // Show Dedicated Number Row
            SettingsSwitchRow(
              label = stringResource(R.string.label_number_row),
              subtitle = "Display dedicated 1–0 row above QWERTY keys",
              checked = config.showNumberRow,
              onCheckedChange = viewModel::setShowNumberRow,
              tag = "switch_number_row",
            )

            SettingsRowDivider()

            // Show Quick Toolbar
            SettingsSwitchRow(
              label = stringResource(R.string.label_show_toolbar),
              subtitle = "Show quick bar for Clipboard, cursor movement, and theme switch",
              checked = config.showToolbar,
              onCheckedChange = viewModel::setShowToolbar,
              tag = "switch_show_toolbar",
            )

            SettingsRowDivider()

            // Cut Copy Paste Feature
            SettingsSwitchRow(
              label = stringResource(R.string.label_cut_copy_paste),
              subtitle = stringResource(R.string.subtitle_cut_copy_paste),
              checked = config.cutCopyPasteEnabled,
              onCheckedChange = viewModel::setCutCopyPasteEnabled,
              tag = "switch_cut_copy_paste",
            )

            SettingsRowDivider()

            // Auto-Capitalize Sentences
            SettingsSwitchRow(
              label = stringResource(R.string.label_auto_caps),
              subtitle = "Automatically shift at the start of sentences",
              checked = config.autoCapitalize,
              onCheckedChange = viewModel::setAutoCapitalize,
              tag = "switch_auto_caps",
            )

            SettingsRowDivider()

            // Double-Space Period
            SettingsSwitchRow(
              label = stringResource(R.string.label_double_space_period),
              subtitle = "Double-tap spacebar to insert a period followed by space",
              checked = config.doubleSpacePeriod,
              onCheckedChange = viewModel::setDoubleSpacePeriod,
              tag = "switch_double_space_period",
            )
          }
        }

        // 5. Voice Typing (V2 Engine Controls) Section
        item {
          SettingsSectionGroup(
            title = "Voice Typing (V2 Engine Controls)",
            tag = "section_voice_typing_v2",
          ) {
            // Master Voice Input
            SettingsSwitchRow(
              label = stringResource(R.string.label_voice_input),
              subtitle = stringResource(R.string.subtitle_voice_input),
              checked = config.voiceInputEnabled,
              onCheckedChange = viewModel::setVoiceInputEnabled,
              tag = "switch_voice_input",
            )

            SettingsRowDivider()

            // Voice Recognition Engine Mode (Auto / Online toggle)
            Text(
              text = stringResource(R.string.label_voice_recognition_mode),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              VoiceRecognitionMode.entries.forEach { mode ->
                val isSelected = config.voiceRecognitionMode == mode
                OutlinedButton(
                  onClick = { viewModel.setVoiceRecognitionMode(mode) },
                  modifier = Modifier.weight(1f).testTag("voice_mode_${mode.id}"),
                  shape = RoundedCornerShape(8.dp),
                  colors =
                    if (isSelected) {
                      ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      )
                    } else {
                      ButtonDefaults.outlinedButtonColors()
                    },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                  Text(
                    text = mode.displayName,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                  )
                }
              }
            }
            Text(
              text =
                when (config.voiceRecognitionMode) {
                  VoiceRecognitionMode.AUTOMATIC ->
                    "Automatic: Uses Google Online Speech Recognition with intelligent offline fallback."
                  VoiceRecognitionMode.ONLINE_ONLY ->
                    "Online Only: Forces online recognition with Google speech services."
                },
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 10.dp),
            )

            SettingsRowDivider()

            // Smart Noise Cancellation
            SettingsSwitchRow(
              label = stringResource(R.string.label_voice_noise_cancellation),
              subtitle = stringResource(R.string.subtitle_voice_noise_cancellation),
              checked = config.voiceNoiseCancellation,
              onCheckedChange = viewModel::setVoiceNoiseCancellation,
              tag = "switch_voice_noise_cancellation",
            )

            SettingsRowDivider()

            // Bengali-First Voice Enhancement
            SettingsSwitchRow(
              label = stringResource(R.string.label_voice_enhancement),
              subtitle = stringResource(R.string.subtitle_voice_enhancement),
              checked = config.voiceEnhancement,
              onCheckedChange = viewModel::setVoiceEnhancement,
              tag = "switch_voice_enhancement",
            )

            SettingsRowDivider()

            // Banglish Mode
            val isBanglishActive =
              config.voiceBanglishMode || config.activeLanguageId == LanguageManager.ID_BANGLISH
            SettingsSwitchRow(
              label = stringResource(R.string.label_banglish_mode),
              subtitle = stringResource(R.string.subtitle_banglish_mode),
              checked = isBanglishActive,
              onCheckedChange = viewModel::setVoiceBanglishMode,
              tag = "switch_banglish_mode",
            )

            SettingsRowDivider()

            // Auto-Punctuation
            SettingsSwitchRow(
              label = stringResource(R.string.label_auto_punctuation),
              subtitle = stringResource(R.string.subtitle_auto_punctuation),
              checked = config.voiceAutoPunctuation,
              onCheckedChange = viewModel::setVoiceAutoPunctuation,
              tag = "switch_auto_punctuation",
            )

            SettingsRowDivider()

            // Audio Waveform & Haptics
            SettingsSwitchRow(
              label = stringResource(R.string.label_audio_waveform_haptics),
              subtitle = stringResource(R.string.subtitle_audio_waveform_haptics),
              checked = config.voiceWaveformAndHaptics,
              onCheckedChange = viewModel::setVoiceWaveformAndHaptics,
              tag = "switch_audio_waveform_haptics",
            )
          }
        }

        // 6. App Updates Section (GitHub Releases Direct In-App Updater)
        item {
          AppUpdateSection(
            updateState = updateState,
            onCheckForUpdates = { viewModel.checkForUpdates(isAutomatic = false) },
            onStartDownload = { release -> viewModel.startDownload(context, release) },
            onInstall = { apkPath ->
              if (ApkInstallerHelper.canRequestPackageInstalls(context)) {
                viewModel.installDownloadedApk(context, apkPath)
              } else {
                val intent = ApkInstallerHelper.createInstallUnknownAppsIntent(context)
                runCatching { context.startActivity(intent) }
              }
            },
          )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
      }
    }
  }
}

@Composable
private fun AppUpdateSection(
  updateState: UpdateUiState,
  onCheckForUpdates: () -> Unit,
  onStartDownload: (GitHubReleaseInfo) -> Unit,
  onInstall: (String) -> Unit,
) {
  val context = LocalContext.current
  val currentVersionName = BuildConfig.VERSION_NAME
  val currentVersionCode = BuildConfig.VERSION_CODE

  SettingsSectionGroup(
    title = stringResource(R.string.section_app_updates),
    tag = "section_app_updates",
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.update_current_version, currentVersionName, currentVersionCode),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Direct APK updates from GitHub Releases",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      if (updateState is UpdateUiState.Checking) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          strokeWidth = 2.dp,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    }

    when (updateState) {
      is UpdateUiState.Idle -> {
        Button(
          onClick = onCheckForUpdates,
          modifier = Modifier.fillMaxWidth().testTag("check_updates_button"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.update_btn_check))
        }
      }

      is UpdateUiState.Checking -> {
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.update_checking),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      is UpdateUiState.UpToDate -> {
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_up_to_date"),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
          border = BorderStroke(1.dp, EnabledCheckGreen.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.CheckCircleOutline,
              contentDescription = null,
              tint = EnabledCheckGreen,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = stringResource(R.string.update_up_to_date),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }
        }

        OutlinedButton(
          onClick = onCheckForUpdates,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp).testTag("check_updates_again_button"),
          shape = RoundedCornerShape(10.dp),
        ) {
          Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = stringResource(R.string.update_btn_check))
        }
      }

      is UpdateUiState.UpdateAvailable -> {
        val release = updateState.releaseInfo
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_update_available"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
          border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
              )
              Text(
                text = stringResource(R.string.update_available),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }

            Text(
              text = stringResource(R.string.update_latest_version, release.versionName, release.versionCode),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
            )

            if (release.releaseNotes.isNotBlank()) {
              Text(
                text = release.releaseNotes.take(180) + if (release.releaseNotes.length > 180) "…" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            Button(
              onClick = { onStartDownload(release) },
              modifier = Modifier.fillMaxWidth().testTag("update_now_button"),
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.update_btn_update_now),
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      is UpdateUiState.Downloading -> {
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_downloading"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = if (updateState.progressPercent >= 0) {
                  stringResource(R.string.update_downloading, updateState.progressPercent)
                } else {
                  "Downloading update…"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "${updateState.progressPercent.coerceAtLeast(0)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
            }

            if (updateState.progressPercent >= 0) {
              LinearProgressIndicator(
                progress = { updateState.progressPercent / 100f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
              )
            } else {
              LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      is UpdateUiState.ReadyToInstall -> {
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_ready_to_install"),
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
          border = BorderStroke(1.5.dp, EnabledCheckGreen),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = EnabledCheckGreen,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Update verified and ready to install",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }

            Text(
              text = "Version: ${updateState.releaseInfo.versionName} (${updateState.releaseInfo.versionCode})",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
              onClick = { onInstall(updateState.apkFilePath) },
              modifier = Modifier.fillMaxWidth().testTag("install_update_button"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = EnabledCheckGreen),
            ) {
              Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.update_btn_install),
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }

      is UpdateUiState.Error -> {
        Card(
          modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("card_update_error"),
          shape = RoundedCornerShape(10.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.ErrorOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = updateState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
          }
        }

        if (updateState.canRetry) {
          OutlinedButton(
            onClick = onCheckForUpdates,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("retry_update_button"),
            shape = RoundedCornerShape(10.dp),
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.update_btn_retry))
          }
        }
      }
    }
  }
}

@Composable
private fun ImeActivationCard(
  imeStatus: ImeSystemStatus,
  onEnableClick: () -> Unit,
  onSelectClick: () -> Unit,
) {
  val isEnabled = imeStatus.isEnabled
  val isSelected = imeStatus.isSelected
  val isFullyReady = isEnabled && isSelected

  Card(
    modifier = Modifier.fillMaxWidth().testTag("ime_activation_card"),
    shape = RoundedCornerShape(14.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (isFullyReady) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
          } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
          }
      ),
    border =
      BorderStroke(
        width = 1.dp,
        color =
          if (isFullyReady) {
            EnabledCheckGreen.copy(alpha = 0.35f)
          } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          },
      ),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = if (isEnabled) Icons.Filled.CheckCircle else Icons.Outlined.Keyboard,
          contentDescription = null,
          tint =
            if (isEnabled) {
              EnabledCheckGreen
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          modifier = Modifier.size(20.dp),
        )
        Text(
          text =
            when {
              isFullyReady -> stringResource(R.string.status_ime_ready)
              !isEnabled -> stringResource(R.string.status_ime_not_enabled)
              else -> stringResource(R.string.status_ime_not_selected)
            },
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onEnableClick,
          modifier = Modifier.weight(1f).testTag("btn_enable_ime"),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
          if (isEnabled) {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = EnabledCheckGreen,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = stringResource(R.string.btn_enable_ime),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
          )
        }

        Button(
          onClick = onSelectClick,
          modifier = Modifier.weight(1f).testTag("btn_select_ime"),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
          colors =
            if (isSelected) {
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            } else {
              ButtonDefaults.buttonColors()
            },
        ) {
          Icon(
            imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.SwapHoriz,
            contentDescription = null,
            tint =
              if (isSelected) {
                EnabledCheckGreen
              } else {
                MaterialTheme.colorScheme.onPrimary
              },
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = stringResource(R.string.btn_select_ime),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionGroup(
  title: String,
  tag: String,
  content: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth().testTag(tag),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier.padding(horizontal = 4.dp),
    )
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp),
      colors =
        CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ),
      border =
        BorderStroke(
          width = 1.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      ) {
        content()
      }
    }
  }
}

@Composable
private fun SettingsRowDivider(modifier: Modifier = Modifier) {
  HorizontalDivider(
    modifier = modifier,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
  )
}

@Composable
private fun SettingsSwitchRow(
  label: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  tag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable { onCheckedChange(!checked) }
        .padding(vertical = 10.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(
      modifier = Modifier.weight(1f).padding(end = 16.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
private fun HeightPreferenceRow(
  title: String,
  percentValue: Int,
  onClick: () -> Unit,
  tag: String,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(vertical = 10.dp)
        .testTag(tag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = "$percentValue%",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(
      text = "$percentValue%",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
private fun KeyboardHeightDialog(
  title: String,
  initialPercent: Int,
  onDismissCancel: () -> Unit,
  onConfirmOk: (Int) -> Unit,
) {
  var sliderValue by
    remember(initialPercent) { mutableFloatStateOf(initialPercent.toFloat()) }
  val displayedPercent = sliderValue.roundToInt()

  AlertDialog(
    onDismissRequest = onDismissCancel,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth().testTag("height_slider_dialog"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "$displayedPercent%",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.testTag("height_dialog_percent_text"),
        )
        Slider(
          value = sliderValue,
          onValueChange = { sliderValue = it },
          valueRange =
            KeyboardHeightBounds.MIN_PERCENT.toFloat()..KeyboardHeightBounds.MAX_PERCENT.toFloat(),
          modifier = Modifier.fillMaxWidth().testTag("height_dialog_slider"),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "${KeyboardHeightBounds.MIN_PERCENT}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${KeyboardHeightBounds.MAX_PERCENT}%",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismissCancel,
        modifier = Modifier.testTag("height_dialog_cancel_button"),
      ) {
        Text(text = stringResource(R.string.dialog_cancel))
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirmOk(displayedPercent) },
        modifier = Modifier.testTag("height_dialog_ok_button"),
      ) {
        Text(text = stringResource(R.string.dialog_ok))
      }
    },
  )
}
