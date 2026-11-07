package com.example.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class ClipboardEntry(
  val id: Long,
  val text: String,
  val isPinned: Boolean = false,
)

class ClipboardController(context: Context) {
  private val appContext: Context = context.applicationContext ?: context
  private val systemClipboard =
    appContext.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

  private val prefs =
    appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val lock = Any()

  private val _entries = MutableStateFlow<List<ClipboardEntry>>(emptyList())
  val entries: StateFlow<List<ClipboardEntry>> = _entries.asStateFlow()

  private var nextId = 1L
  private var isInternalClipboardClearing = false
  private var clipEventCounter = 0L

  private data class Tombstone(val timestamp: Long, val eventCount: Long)
  private val tombstones = mutableMapOf<String, Tombstone>()

  private val clipChangedListener =
    ClipboardManager.OnPrimaryClipChangedListener {
      synchronized(lock) {
        clipEventCounter++
      }
      syncFromSystemClipboard(isListenerTriggered = true)
    }

  init {
    synchronized(lock) {
      val restored = loadFromPrefsLocked()
      _entries.value = restored
      syncFromSystemClipboardLocked(isListenerTriggered = false)
    }
    try {
      systemClipboard?.addPrimaryClipChangedListener(clipChangedListener)
    } catch (e: Throwable) {
      Log.w(TAG, "Failed to register clip listener", e)
    }
  }

  fun syncFromSystemClipboard() {
    syncFromSystemClipboard(isListenerTriggered = false)
  }

  fun syncFromSystemClipboard(isListenerTriggered: Boolean) {
    synchronized(lock) {
      syncFromSystemClipboardLocked(isListenerTriggered)
    }
  }

  private fun syncFromSystemClipboardLocked(isListenerTriggered: Boolean) {
    if (isInternalClipboardClearing) return
    val clip = systemClipboard?.primaryClip ?: return
    if (clip.itemCount <= 0) return
    val rawText = clip.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim() ?: return
    if (rawText.isEmpty() || rawText.length > MAX_CLIP_TEXT_LENGTH) return

    val tombstone = tombstones[rawText]
    if (tombstone != null) {
      val clipTimestamp =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          try {
            clip.description?.timestamp ?: 0L
          } catch (_: Throwable) {
            0L
          }
        } else {
          0L
        }

      val isNewCopy =
        if (clipTimestamp > 0L) {
          clipTimestamp > tombstone.timestamp
        } else {
          isListenerTriggered && clipEventCounter > tombstone.eventCount
        }

      if (isNewCopy) {
        tombstones.remove(rawText)
      } else {
        // Drop deleted text to prevent unexpected restoration
        return
      }
    }

    val current = _entries.value
    if (current.firstOrNull()?.text == rawText) return

    val existingPinned = current.firstOrNull { it.text == rawText }?.isPinned ?: false
    val filtered = current.filterNot { it.text == rawText }
    val updated =
      buildList {
        add(ClipboardEntry(id = nextId++, text = rawText, isPinned = existingPinned))
        addAll(filtered)
      }
        .take(MAX_CLIPS)

    _entries.value = updated
    saveToPrefsLocked(updated)
  }

  fun togglePin(id: Long) = synchronized(lock) {
    val current = _entries.value
    val updated =
      current.map { entry ->
        if (entry.id == id) entry.copy(isPinned = !entry.isPinned) else entry
      }
    _entries.value = updated
    saveToPrefsLocked(updated)
  }

  fun removeEntry(id: Long) = synchronized(lock) {
    val current = _entries.value
    val target = current.firstOrNull { it.id == id } ?: return@synchronized
    val updated = current.filterNot { it.id == id }
    _entries.value = updated
    addTombstoneLocked(target.text)
    saveToPrefsLocked(updated)
    safelyClearSystemClipboardIfMatchingAny(setOf(target.text))
  }

  fun clearUnpinned() = synchronized(lock) {
    val current = _entries.value
    val unpinned = current.filterNot { it.isPinned }
    if (unpinned.isEmpty()) return@synchronized
    val unpinnedTexts = unpinned.map { it.text }.toSet()
    for (entry in unpinned) {
      addTombstoneLocked(entry.text)
    }
    val updated = current.filter { it.isPinned }
    _entries.value = updated
    saveToPrefsLocked(updated)
    safelyClearSystemClipboardIfMatchingAny(unpinnedTexts)
  }

  fun clearAll() = synchronized(lock) {
    val current = _entries.value
    if (current.isEmpty()) return@synchronized
    val allTexts = current.map { it.text }.toSet()
    for (entry in current) {
      addTombstoneLocked(entry.text)
    }
    _entries.value = emptyList()
    saveToPrefsLocked(emptyList())
    safelyClearSystemClipboardIfMatchingAny(allTexts)
  }

  fun release() = synchronized(lock) {
    try {
      systemClipboard?.removePrimaryClipChangedListener(clipChangedListener)
    } catch (e: Throwable) {
      Log.w(TAG, "Failed to remove clip listener", e)
    }
  }

  private fun addTombstoneLocked(text: String) {
    tombstones[text] =
      Tombstone(
        timestamp = System.currentTimeMillis(),
        eventCount = clipEventCounter,
      )
    if (tombstones.size > MAX_TOMBSTONES) {
      val oldestKey = tombstones.entries.minByOrNull { it.value.timestamp }?.key
      if (oldestKey != null) {
        tombstones.remove(oldestKey)
      }
    }
  }

  private fun safelyClearSystemClipboardIfMatchingAny(texts: Set<String>) {
    if (systemClipboard == null) return
    try {
      val clip = systemClipboard.primaryClip
      if (clip != null && clip.itemCount > 0) {
        val raw = clip.getItemAt(0)?.coerceToText(appContext)?.toString()?.trim()
        if (raw != null && texts.contains(raw)) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isInternalClipboardClearing = true
            try {
              systemClipboard.clearPrimaryClip()
            } finally {
              isInternalClipboardClearing = false
            }
          }
        }
      }
    } catch (e: Throwable) {
      Log.w(TAG, "Unable to clear system clipboard: ${e.message}")
    }
  }

  private fun saveToPrefsLocked(entries: List<ClipboardEntry>) {
    try {
      val array = JSONArray()
      for (entry in entries) {
        val obj = JSONObject()
        obj.put("id", entry.id)
        obj.put("text", entry.text)
        obj.put("pinned", entry.isPinned)
        array.put(obj)
      }
      val tombstoneArray = JSONArray()
      for ((text, tombstone) in tombstones) {
        val obj = JSONObject()
        obj.put("text", text)
        obj.put("ts", tombstone.timestamp)
        obj.put("cnt", tombstone.eventCount)
        tombstoneArray.put(obj)
      }
      prefs.edit()
        .putString(KEY_ENTRIES, array.toString())
        .putString(KEY_TOMBSTONES, tombstoneArray.toString())
        .putLong(KEY_NEXT_ID, nextId)
        .apply()
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to persist clipboard state", e)
    }
  }

  private fun loadFromPrefsLocked(): List<ClipboardEntry> {
    try {
      val rawTombstones = prefs.getString(KEY_TOMBSTONES, null)
      if (!rawTombstones.isNullOrEmpty()) {
        val tombstoneArray = JSONArray(rawTombstones)
        for (i in 0 until tombstoneArray.length()) {
          val obj = tombstoneArray.getJSONObject(i)
          val text = obj.getString("text")
          val ts = obj.optLong("ts", 0L)
          val cnt = obj.optLong("cnt", 0L)
          tombstones[text] = Tombstone(ts, cnt)
        }
      }
      nextId = prefs.getLong(KEY_NEXT_ID, 1L)

      val rawEntries = prefs.getString(KEY_ENTRIES, null) ?: return emptyList()
      val array = JSONArray(rawEntries)
      val list = ArrayList<ClipboardEntry>(array.length())
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val id = obj.getLong("id")
        val text = obj.getString("text")
        val isPinned = obj.optBoolean("pinned", false)
        list.add(ClipboardEntry(id = id, text = text, isPinned = isPinned))
      }
      if (list.isNotEmpty()) {
        val maxExistingId = list.maxOf { it.id }
        if (nextId <= maxExistingId) {
          nextId = maxExistingId + 1L
        }
      }
      return list.take(MAX_CLIPS)
    } catch (e: Throwable) {
      Log.e(TAG, "Failed to load clipboard state", e)
      return emptyList()
    }
  }

  companion object {
    private const val TAG = "ClipboardController"
    private const val PREFS_NAME = "com.example.clipboard_prefs"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_TOMBSTONES = "tombstones"
    private const val KEY_NEXT_ID = "next_id"
    private const val MAX_CLIPS = 12
    private const val MAX_TOMBSTONES = 50
    private const val MAX_CLIP_TEXT_LENGTH = 1000
  }
}
