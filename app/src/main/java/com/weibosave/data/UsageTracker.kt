package com.weibosave.data

import com.weibosave.model.UsageSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object UsageTracker {

    private data class SessionData(
        val id: String = UUID.randomUUID().toString(),
        val postId: String,
        val startTime: Long = System.currentTimeMillis(),
        val apiBytes: AtomicLong = AtomicLong(0),
        val viewBytes: AtomicLong = AtomicLong(0),
        val downloadBytes: AtomicLong = AtomicLong(0),
        @Volatile var imageCount: Int = 0,
        @Volatile var downloadCount: Int = 0,
    )

    private val sessions = ConcurrentHashMap<String, SessionData>()

    // postId → sessionId for the currently active load (used by DownloadService)
    private val activeByPostId = ConcurrentHashMap<String, String>()

    // Session whose view bytes are currently accumulating (most recently started)
    @Volatile private var currentViewSessionId: String? = null

    fun startSession(postId: String): String {
        val data = SessionData(postId = postId)
        sessions[data.id] = data
        activeByPostId[postId] = data.id
        currentViewSessionId = data.id
        return data.id
    }

    fun setImageCount(sessionId: String, count: Int) {
        sessions[sessionId]?.imageCount = count
    }

    fun addApiBytes(sessionId: String, bytes: Long) {
        sessions[sessionId]?.apiBytes?.addAndGet(bytes)
        persistSession(sessionId)
    }

    // Called by TrafficInterceptor for sinaimg.cn GET responses (thumbnails / previews).
    fun addViewBytes(bytes: Long) {
        currentViewSessionId?.let { sessions[it]?.viewBytes?.addAndGet(bytes) }
    }

    // Called by DownloadService after each image file is downloaded.
    fun addDownloadBytes(postId: String, bytes: Long) {
        val sessionId = activeByPostId[postId] ?: return
        sessions[sessionId]?.let {
            it.downloadBytes.addAndGet(bytes)
            it.downloadCount++
        }
        persistSession(sessionId)
    }

    // Called when DownloadService finishes all downloads for a post.
    fun endDownloadSession(postId: String) {
        val sessionId = activeByPostId.remove(postId) ?: return
        persistSession(sessionId)
        if (currentViewSessionId == sessionId) currentViewSessionId = null
        sessions.remove(sessionId)
    }

    private fun persistSession(sessionId: String) {
        val data = sessions[sessionId] ?: return
        UsageRepository.addOrUpdateSession(data.toUsageSession())
    }

    private fun SessionData.toUsageSession() = UsageSession(
        id = id,
        postId = postId,
        timestamp = startTime,
        apiBytes = apiBytes.get(),
        viewBytes = viewBytes.get(),
        downloadBytes = downloadBytes.get(),
        imageCount = imageCount,
        downloadCount = downloadCount,
    )
}
