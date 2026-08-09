package com.weibosave.data

import android.content.Context
import android.content.SharedPreferences
import com.weibosave.model.UsageSession
import org.json.JSONArray
import org.json.JSONObject

object UsageRepository {
    private const val PREFS_NAME = "usage_stats"
    private const val KEY_SESSIONS = "sessions"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getAllSessions(): List<UsageSession> {
        val json = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        return parseSessionsJson(json)
    }

    fun addOrUpdateSession(session: UsageSession) {
        val sessions = getAllSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) sessions[index] = session else sessions.add(session)
        saveAll(sessions)
    }

    // Deletes sessions with timestamp >= cutoffMs (clears recent history).
    fun deleteSessionsNewerThan(cutoffMs: Long) {
        val remaining = getAllSessions().filter { it.timestamp < cutoffMs }
        saveAll(remaining)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_SESSIONS).apply()
    }

    private fun saveAll(sessions: List<UsageSession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    private fun parseSessionsJson(json: String): List<UsageSession> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                runCatching { arr.getJSONObject(i).toSession() }.getOrNull()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun UsageSession.toJson() = JSONObject().apply {
        put("id", id)
        put("postId", postId)
        put("timestamp", timestamp)
        put("apiBytes", apiBytes)
        put("viewBytes", viewBytes)
        put("downloadBytes", downloadBytes)
        put("imageCount", imageCount)
        put("downloadCount", downloadCount)
    }

    private fun JSONObject.toSession() = UsageSession(
        id = getString("id"),
        postId = getString("postId"),
        timestamp = getLong("timestamp"),
        apiBytes = optLong("apiBytes", 0L),
        viewBytes = optLong("viewBytes", 0L),
        downloadBytes = optLong("downloadBytes", 0L),
        imageCount = optInt("imageCount", 0),
        downloadCount = optInt("downloadCount", 0),
    )
}
