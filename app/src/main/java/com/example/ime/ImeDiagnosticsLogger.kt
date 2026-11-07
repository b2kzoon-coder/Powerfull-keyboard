package com.example.ime

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

data class ImeDiagnosticEvent(
  val sequenceNumber: Long,
  val timestampMs: Long,
  val category: String,
  val event: String,
  val details: String,
) {
  fun formatted(): String =
    "[seq=$sequenceNumber t=${timestampMs}ms][$category] $event | $details"
}

/**
 * Structured runtime diagnostics logger for PersonalKeyboardService and EmojiPanel lifecycle,
 * mode transitions, search focus changes, InputConnection validity, and window visibility.
 */
object ImeDiagnosticsLogger {
  private const val TAG = "PersonalKeyboardIME"
  private const val MAX_BUFFER_SIZE = 250

  private val sequenceGenerator = AtomicLong(0L)
  private val eventsBuffer = ArrayDeque<ImeDiagnosticEvent>(MAX_BUFFER_SIZE)

  fun log(category: String, event: String, details: String = "") {
    val seq = sequenceGenerator.incrementAndGet()
    val now = try {
      SystemClock.uptimeMillis()
    } catch (_: Throwable) {
      System.currentTimeMillis()
    }
    val entry = ImeDiagnosticEvent(
      sequenceNumber = seq,
      timestampMs = now,
      category = category,
      event = event,
      details = details,
    )
    synchronized(eventsBuffer) {
      if (eventsBuffer.size >= MAX_BUFFER_SIZE) {
        eventsBuffer.removeFirst()
      }
      eventsBuffer.addLast(entry)
    }
    try {
      if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, entry.formatted())
      }
    } catch (_: Throwable) {
      // Ignore if android.util.Log is unmocked in pure JVM context
    }
  }

  fun getEvents(): List<ImeDiagnosticEvent> = synchronized(eventsBuffer) { eventsBuffer.toList() }

  fun clear() {
    synchronized(eventsBuffer) {
      eventsBuffer.clear()
    }
    sequenceGenerator.set(0L)
  }
}
