package com.example.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import com.example.R
import com.example.settings.KeyboardPreferencesRepository
import java.util.concurrent.atomic.AtomicBoolean

enum class KeyFeedbackType {
  CHARACTER,
  DELETE,
  SPACE,
  ENTER,
  FUNCTIONAL,
}

/**
 * Controller for tactile haptic vibration and low-latency iPhone-style keyboard click audio.
 * Both features operate independently, immediately respect preference changes without restart,
 * and honor Android audio and vibration system constraints.
 */
class KeyboardFeedbackManager(
  private val context: Context,
  private val preferencesRepository: KeyboardPreferencesRepository,
) {
  private val audioManager: AudioManager? =
    context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

  private val vibrator: Vibrator? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
      @Suppress("DEPRECATION")
      vibratorManager?.defaultVibrator
        ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
  }

  private val hasHardwareVibrator: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    runCatching { vibrator?.hasVibrator() == true }.getOrDefault(false)
  }

  private val sonificationAudioAttributes: AudioAttributes by lazy(LazyThreadSafetyMode.PUBLICATION) {
    AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
  }

  private val touchVibrationAttributes: VibrationAttributes? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      runCatching {
        VibrationAttributes.Builder()
          .setUsage(VibrationAttributes.USAGE_TOUCH)
          .build()
      }.getOrNull()
    } else {
      null
    }
  }

  private val predefinedEffectTick: VibrationEffect? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      runCatching { VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK) }.getOrNull()
    } else {
      null
    }
  }

  private val predefinedEffectClick: VibrationEffect? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      runCatching { VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK) }.getOrNull()
    } else {
      null
    }
  }

  private val oneShotEffectDelete: VibrationEffect? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching {
        VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE)
      }.getOrNull()
    } else {
      null
    }
  }

  private val oneShotEffectDefault: VibrationEffect? by lazy(LazyThreadSafetyMode.PUBLICATION) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      runCatching {
        VibrationEffect.createOneShot(14L, VibrationEffect.DEFAULT_AMPLITUDE)
      }.getOrNull()
    } else {
      null
    }
  }

  private val maxSystemVolume: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
    runCatching { audioManager?.getStreamMaxVolume(AudioManager.STREAM_SYSTEM) ?: 1 }
      .getOrDefault(1)
      .coerceAtLeast(1)
  }

  @Volatile private var cachedRingerMode: Int = AudioManager.RINGER_MODE_NORMAL
  @Volatile private var cachedVolumeRatio: Float = 0.7f
  @Volatile private var lastAudioStateQueryMs: Long = 0L

  // Low-latency SoundPool for authentic iOS-style click sound synthesis
  private var soundPool: SoundPool? = null
  private var soundIdChar: Int = 0
  private var soundIdDelete: Int = 0
  private var soundIdSpace: Int = 0
  private val soundsLoaded = AtomicBoolean(false)
  private val soundPoolInitializing = AtomicBoolean(false)

  init {
    if (preferencesRepository.config.value.soundOnKeypress) {
      ensureSoundPoolInitialized()
    }
  }

  @Synchronized
  private fun ensureSoundPoolInitialized() {
    if (soundPool != null || !soundPoolInitializing.compareAndSet(false, true)) return
    try {
      val pool =
        SoundPool.Builder()
          .setMaxStreams(6)
          .setAudioAttributes(sonificationAudioAttributes)
          .build()

      soundIdChar = pool.load(context, R.raw.keyboard_click_char, 1)
      soundIdDelete = pool.load(context, R.raw.keyboard_click_delete, 1)
      soundIdSpace = pool.load(context, R.raw.keyboard_click_space, 1)

      pool.setOnLoadCompleteListener { _, _, status ->
        if (status == 0) {
          soundsLoaded.set(true)
        }
      }
      soundPool = pool
    } catch (_: Throwable) {
      soundPool = null
      soundPoolInitializing.set(false)
    }
  }

  /**
   * Triggers haptic vibration and/or click sound based on current user preferences.
   */
  fun trigger(type: KeyFeedbackType = KeyFeedbackType.CHARACTER, targetView: View? = null) {
    val config = preferencesRepository.config.value
    if (config.hapticFeedback) {
      performVibration(type, targetView)
    }
    if (config.soundOnKeypress) {
      performSound(type)
    }
  }

  private fun performVibration(type: KeyFeedbackType, targetView: View?) {
    val vib = vibrator
    if (vib != null && hasHardwareVibrator) {
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          val effect =
            if (type == KeyFeedbackType.DELETE) predefinedEffectTick else predefinedEffectClick
          if (effect != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
              val attrs = touchVibrationAttributes
              if (attrs != null) {
                vib.vibrate(effect, attrs)
              } else {
                vib.vibrate(effect)
              }
            } else {
              vib.vibrate(effect)
            }
            return
          }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          val effect =
            if (type == KeyFeedbackType.DELETE) oneShotEffectDelete else oneShotEffectDefault
          if (effect != null) {
            @Suppress("DEPRECATION")
            vib.vibrate(effect, sonificationAudioAttributes)
            return
          }
        } else {
          @Suppress("DEPRECATION")
          vib.vibrate(12L)
          return
        }
      } catch (_: Throwable) {
        // Fallback to View haptic feedback if vibrator throws
      }
    }

    // View-based haptic fallback
    try {
      targetView?.performHapticFeedback(
        HapticFeedbackConstants.KEYBOARD_TAP,
        HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING,
      )
    } catch (_: Throwable) {}
  }

  private fun refreshAudioStateIfNeeded(am: AudioManager?) {
    if (am == null) return
    val now =
      try {
        SystemClock.uptimeMillis()
      } catch (_: Throwable) {
        System.currentTimeMillis()
      }
    if (now - lastAudioStateQueryMs < AUDIO_STATE_CACHE_TTL_MS && lastAudioStateQueryMs != 0L) {
      return
    }
    lastAudioStateQueryMs = now
    val ringer = am.ringerMode
    cachedRingerMode = ringer
    if (ringer != AudioManager.RINGER_MODE_SILENT && ringer != AudioManager.RINGER_MODE_VIBRATE) {
      val currentVol = am.getStreamVolume(AudioManager.STREAM_SYSTEM)
      cachedVolumeRatio =
        (currentVol.toFloat() / maxSystemVolume.toFloat()).coerceIn(0.1f, 1.0f)
    }
  }

  private fun performSound(type: KeyFeedbackType) {
    if (soundPool == null) {
      ensureSoundPoolInitialized()
    }
    val am = audioManager
    refreshAudioStateIfNeeded(am)
    val ringerMode = cachedRingerMode
    // Respect silent / do-not-disturb ringer modes
    if (ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
      return
    }

    val volumeRatio = cachedVolumeRatio
    val pool = soundPool
    if (pool != null && soundsLoaded.get()) {
      val soundId =
        when (type) {
          KeyFeedbackType.DELETE -> soundIdDelete
          KeyFeedbackType.SPACE,
          KeyFeedbackType.ENTER -> soundIdSpace
          KeyFeedbackType.CHARACTER,
          KeyFeedbackType.FUNCTIONAL -> soundIdChar
        }
      val baseVol =
        when (type) {
          KeyFeedbackType.DELETE -> 0.42f
          KeyFeedbackType.SPACE,
          KeyFeedbackType.ENTER -> 0.48f
          KeyFeedbackType.CHARACTER,
          KeyFeedbackType.FUNCTIONAL -> 0.45f
        }
      val finalVol = (baseVol * volumeRatio).coerceIn(0.05f, 1.0f)
      pool.play(soundId, finalVol, finalVol, 1, 0, 1.0f)
    } else {
      // Graceful fallback to Android AudioManager click effect if SoundPool is still initializing
      val effect =
        if (type == KeyFeedbackType.DELETE) {
          AudioManager.FX_KEYPRESS_DELETE
        } else {
          AudioManager.FX_KEY_CLICK
        }
      am?.playSoundEffect(effect)
    }
  }

  fun release() {
    try {
      soundPool?.release()
      soundPool = null
      soundsLoaded.set(false)
      soundPoolInitializing.set(false)
    } catch (_: Throwable) {}
  }

  companion object {
    private const val AUDIO_STATE_CACHE_TTL_MS = 1200L
  }
}
