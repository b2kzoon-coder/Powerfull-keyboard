package com.example.voice

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.util.Log

/**
 * Audio Pre-Processor for Voice Typing V2.
 *
 * Provides real-time audio pre-processing using Android hardware/DSP audiofx:
 * - NoiseSuppressor (NS): cleans background noise and ambient acoustic clutter
 * - AcousticEchoCanceler (AEC): cancels acoustic feedback from speakers/feedback
 * - AutomaticGainControl (AGC): normalizes microphone input levels for quiet or loud speech
 *
 * Safe across all OEM devices (handles missing audiofx hardware drivers gracefully)
 * with zero memory leaks and guaranteed resource release on session completion.
 */
class AudioPreProcessor(private val context: Context) {

  private var activeSessionId: Int? = null
  private var noiseSuppressor: NoiseSuppressor? = null
  private var echoCanceler: AcousticEchoCanceler? = null
  private var gainControl: AutomaticGainControl? = null

  val isNoiseSuppressorAvailable: Boolean
    get() = runCatching { NoiseSuppressor.isAvailable() }.getOrDefault(false)

  val isAcousticEchoCancelerAvailable: Boolean
    get() = runCatching { AcousticEchoCanceler.isAvailable() }.getOrDefault(false)

  val isAutomaticGainControlAvailable: Boolean
    get() = runCatching { AutomaticGainControl.isAvailable() }.getOrDefault(false)

  val isNoiseSuppressorActive: Boolean
    get() = noiseSuppressor?.enabled == true

  val isAcousticEchoCancelerActive: Boolean
    get() = echoCanceler?.enabled == true

  val isAutomaticGainControlActive: Boolean
    get() = gainControl?.enabled == true

  fun getProcessingInfo(): AudioProcessingInfo {
    return AudioProcessingInfo(
      isNoiseSuppressorAvailable = isNoiseSuppressorAvailable,
      isAcousticEchoCancelerAvailable = isAcousticEchoCancelerAvailable,
      isAutomaticGainControlAvailable = isAutomaticGainControlAvailable,
      isNoiseSuppressorActive = isNoiseSuppressorActive,
      isAcousticEchoCancelerActive = isAcousticEchoCancelerActive,
      isAutomaticGainControlActive = isAutomaticGainControlActive,
    )
  }

  /**
   * Initializes and activates audio pre-processing filters on the given audioSessionId
   * or a newly generated audio session from AudioManager.
   */
  fun start(requestedSessionId: Int? = null): AudioProcessingInfo {
    stop() // Ensure previous effects are cleanly released

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val sessionId = requestedSessionId
      ?: runCatching { audioManager?.generateAudioSessionId() }.getOrNull()
      ?: 0

    if (sessionId <= 0) {
      Log.d(TAG, "Audio session ID not available for hardware audiofx pre-processing")
      return getProcessingInfo()
    }

    activeSessionId = sessionId

    // 1. Noise Suppressor
    if (isNoiseSuppressorAvailable) {
      runCatching {
        noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply {
          enabled = true
        }
      }.onFailure { e ->
        Log.w(TAG, "NoiseSuppressor.create($sessionId) failed or unsupported on this device: ${e.message}")
        noiseSuppressor = null
      }
    }

    // 2. Acoustic Echo Canceler
    if (isAcousticEchoCancelerAvailable) {
      runCatching {
        echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply {
          enabled = true
        }
      }.onFailure { e ->
        Log.w(TAG, "AcousticEchoCanceler.create($sessionId) failed or unsupported on this device: ${e.message}")
        echoCanceler = null
      }
    }

    // 3. Automatic Gain Control
    if (isAutomaticGainControlAvailable) {
      runCatching {
        gainControl = AutomaticGainControl.create(sessionId)?.apply {
          enabled = true
        }
      }.onFailure { e ->
        Log.w(TAG, "AutomaticGainControl.create($sessionId) failed or unsupported on this device: ${e.message}")
        gainControl = null
      }
    }

    Log.d(
      TAG,
      "AudioPreProcessor started on session=$sessionId [NS=$isNoiseSuppressorActive, AEC=$isAcousticEchoCancelerActive, AGC=$isAutomaticGainControlActive]",
    )

    return getProcessingInfo()
  }

  /**
   * Safely disables and releases all active audio effects.
   */
  fun stop() {
    noiseSuppressor?.let { ns ->
      runCatching {
        if (ns.enabled) ns.enabled = false
        ns.release()
      }
      noiseSuppressor = null
    }

    echoCanceler?.let { aec ->
      runCatching {
        if (aec.enabled) aec.enabled = false
        aec.release()
      }
      echoCanceler = null
    }

    gainControl?.let { agc ->
      runCatching {
        if (agc.enabled) agc.enabled = false
        agc.release()
      }
      gainControl = null
    }

    activeSessionId = null
  }

  fun release() {
    stop()
  }

  companion object {
    private const val TAG = "AudioPreProcessor"
  }
}
