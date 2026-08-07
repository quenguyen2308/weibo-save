package com.weibosave.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

// Port of _probe_size() + get_best_url() + download_image() from bot.py.
// Uses the same priority order and CDN fallback strategy.
class ImageDownloader(private val client: OkHttpClient) {

    // Mirrors bot.py IMAGE_SIZES + PRIMARY_SIZE constant.
    // 'large' is tried first with a single HEAD — only falls back if missing/errored.
    private val primarySize = "large"
    private val fallbackSizes = listOf("orj1080", "mw2000", "orj480", "orj360")
    private val cdnHosts = listOf("wx1", "wx2", "wx3", "wx4")

    // Returns the best available URL for a given PID, or null if all probes fail.
    suspend fun probeBestUrl(pid: String): String? =
        probeBestUrlWithSize(pid)?.first

    // Like probeBestUrl but also returns Content-Length in bytes (-1 if unknown).
    suspend fun probeBestUrlWithSize(pid: String): Pair<String, Long>? = withContext(Dispatchers.IO) {
        probeSize(pid, primarySize) ?: fallbackSizes.firstNotNullOfOrNull { probeSize(pid, it) }
    }

    // HEAD probe one size, with CDN fallback wx1→wx4 on 403 (mirrors _probe_size()).
    // Returns (url, contentLength) where contentLength is -1 if the header is absent.
    private fun probeSize(pid: String, size: String): Pair<String, Long>? {
        for (host in cdnHosts) {
            val url = "https://$host.sinaimg.cn/$size/$pid.jpg"
            try {
                val resp = client.newCall(
                    Request.Builder().url(url).head().applyImgHeaders().build()
                ).execute()
                val bytes = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                resp.close()
                when (resp.code) {
                    200 -> return url to bytes
                    403 -> continue  // try next CDN host
                    else -> break    // non-recoverable for this size, try next size
                }
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    // GET full image bytes, retry up to 3 times with exponential backoff.
    // On 403 tries rotating CDN hosts before giving up.
    suspend fun downloadImage(url: String, onProgress: ((Float) -> Unit)? = null): ByteArray? =
        withContext(Dispatchers.IO) {
            repeat(3) { attempt ->
                try {
                    val resp = client.newCall(
                        Request.Builder().url(url).applyImgHeaders().build()
                    ).execute()

                    if (resp.isSuccessful) {
                        return@withContext resp.body?.bytes()
                    }

                    if (resp.code == 403) {
                        val altUrl = rotateCdn(url) ?: return@withContext null
                        val resp2 = client.newCall(
                            Request.Builder().url(altUrl).applyImgHeaders().build()
                        ).execute()
                        if (resp2.isSuccessful) return@withContext resp2.body?.bytes()
                    }
                } catch (_: Exception) {
                    if (attempt < 2) Thread.sleep((2000L * (attempt + 1)))
                }
            }
            null
        }

    // Try next CDN host in rotation for a sinaimg URL (mirrors 403 fallback in bot.py).
    private fun rotateCdn(url: String): String? {
        val currentHost = cdnHosts.firstOrNull { url.contains("$it.sinaimg.cn") } ?: return null
        val nextIndex = (cdnHosts.indexOf(currentHost) + 1) % cdnHosts.size
        return url.replace("$currentHost.sinaimg.cn", "${cdnHosts[nextIndex]}.sinaimg.cn")
    }

    fun getFilenameFromUrl(url: String): String {
        val match = Regex("""([^/]+\.(?:jpg|jpeg|png|gif|webp))$""", RegexOption.IGNORE_CASE)
            .find(url)
        return match?.groupValues?.get(1) ?: "weibo_image.jpg"
    }

    private fun Request.Builder.applyImgHeaders() = this
        .header("User-Agent", UA_IMG)
        .header("Referer", "https://weibo.com/")
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("Accept-Language", "zh-CN,zh;q=0.9")
        .header("Cache-Control", "no-cache")

    companion object {
        const val CHUNK_SIZE = 8
        private const val UA_IMG =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
    }
}
