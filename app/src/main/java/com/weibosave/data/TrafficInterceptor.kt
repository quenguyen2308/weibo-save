package com.weibosave.data

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

// Marker tag attached to download requests so the interceptor skips counting them
// (download bytes are tracked explicitly in DownloadService).
class DownloadTag

class TrafficInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val url = request.url.toString()
        // request.tag() retrieves the tag stored at Any::class.java key (set via .tag(DownloadTag())).
        // request.tag(DownloadTag::class.java) would look up the wrong key and always return null.
        val isViewImageGet = request.method == "GET"
            && url.contains("sinaimg.cn")
            && request.tag() !is DownloadTag

        if (!isViewImageGet) return response

        val originalBody = response.body ?: return response
        val countingBody = CountingResponseBody(originalBody) { bytes ->
            UsageTracker.addViewBytes(bytes)
        }
        return response.newBuilder().body(countingBody).build()
    }
}

private class CountingResponseBody(
    private val delegate: ResponseBody,
    private val onBytesRead: (Long) -> Unit,
) : ResponseBody() {
    private val countingSource = CountingSource(delegate.source(), onBytesRead).buffer()
    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()
    override fun source(): BufferedSource = countingSource
}

private class CountingSource(
    source: Source,
    private val onBytesRead: (Long) -> Unit,
) : ForwardingSource(source) {
    override fun read(sink: Buffer, byteCount: Long): Long {
        val bytesRead = super.read(sink, byteCount)
        if (bytesRead != -1L) onBytesRead(bytesRead)
        return bytesRead
    }
}
