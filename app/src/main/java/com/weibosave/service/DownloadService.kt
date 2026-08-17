package com.weibosave.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.weibosave.R
import com.weibosave.data.ImageDownloader.Companion.CHUNK_SIZE
import com.weibosave.data.UsageTracker
import com.weibosave.data.WeiboRepository
import com.weibosave.model.DownloadItem
import com.weibosave.model.DownloadState
import com.weibosave.util.FolderPreference
import com.weibosave.util.MediaStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var downloadPostId: String = ""
    private var safFolderUri: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pids = intent?.getStringArrayListExtra(EXTRA_PIDS) ?: return START_NOT_STICKY
        val thumbUrls = intent.getStringArrayListExtra(EXTRA_THUMB_URLS) ?: ArrayList()
        val indices = intent.getIntegerArrayListExtra(EXTRA_INDICES)
            ?: ArrayList((pids.indices).toList())
        downloadPostId = intent.getStringExtra(EXTRA_POST_ID) ?: ""
        safFolderUri = FolderPreference.getFolderUri(this)

        val items = indices.map { i ->
            DownloadItem(
                index = i,
                pid = pids.getOrElse(i) { "" },
                thumbUrl = thumbUrls.getOrElse(i) { "" },
            )
        }
        DownloadStateHolder.setItems(items)
        DownloadStateHolder.setRunning(true)

        startForeground(NOTIF_ID, buildNotification(0, items.size))

        downloadJob = scope.launch {
            var doneCount = 0

            items.chunked(CHUNK_SIZE).forEach { chunk ->
                chunk.map { item ->
                    async {
                        processItem(item) { done ->
                            doneCount = done
                            updateNotification(doneCount, items.size)
                        }
                    }
                }.awaitAll()
            }

            updateNotification(doneCount, items.size, finished = true)
            DownloadStateHolder.setRunning(false)
            UsageTracker.endDownloadSession(downloadPostId)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private suspend fun processItem(
        item: DownloadItem,
        onDone: (Int) -> Unit,
    ) {
        DownloadStateHolder.updateItem(item.copy(state = DownloadState.Probing))

        val url = WeiboRepository.downloader.probeBestUrl(item.pid)
        if (url == null) {
            DownloadStateHolder.updateItem(item.copy(state = DownloadState.Error(getString(R.string.error_url_not_found))))
            return
        }

        DownloadStateHolder.updateItem(item.copy(state = DownloadState.Downloading(0f, 0L)))

        val bytes = WeiboRepository.downloader.downloadImage(url)
        if (bytes == null) {
            DownloadStateHolder.updateItem(item.copy(state = DownloadState.Error(getString(R.string.error_download_failed))))
            return
        }

        val filename = WeiboRepository.downloader.getFilenameFromUrl(url)
        MediaStoreHelper.saveImage(this, bytes, filename, safFolderUri)

        val byteCount = bytes.size.toLong()
        val doneState = DownloadState.Done(url, byteCount)
        DownloadStateHolder.updateItem(item.copy(state = doneState))
        UsageTracker.addDownloadBytes(downloadPostId, byteCount)
        onDone(DownloadStateHolder.items.value.count { it.state is DownloadState.Done })
    }

    private fun updateNotification(done: Int, total: Int, finished: Boolean = false) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(done, total, finished))
    }

    private fun buildNotification(done: Int, total: Int, finished: Boolean = false) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                if (finished) getString(R.string.notif_done_title)
                else getString(R.string.notif_downloading, done, total)
            )
            .setContentText(
                if (finished) getString(R.string.notif_saved, done)
                else getString(R.string.notif_processing)
            )
            .setProgress(total, done, false)
            .setOngoing(!finished)
            .build()

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val CHANNEL_ID = "weibosave_download"
        const val NOTIF_ID = 1001
        const val EXTRA_PIDS = "pids"
        const val EXTRA_THUMB_URLS = "thumb_urls"
        const val EXTRA_INDICES = "indices"
        const val EXTRA_POST_ID = "post_id"

        fun buildIntent(
            context: Context,
            postId: String,
            pids: List<String>,
            thumbUrls: List<String>,
            indices: List<Int>,
        ) = Intent(context, DownloadService::class.java).apply {
            putStringArrayListExtra(EXTRA_PIDS, ArrayList(pids))
            putStringArrayListExtra(EXTRA_THUMB_URLS, ArrayList(thumbUrls))
            putIntegerArrayListExtra(EXTRA_INDICES, ArrayList(indices))
            putExtra(EXTRA_POST_ID, postId)
        }
    }
}
