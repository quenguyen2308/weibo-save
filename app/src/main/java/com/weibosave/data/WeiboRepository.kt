package com.weibosave.data

import com.weibosave.model.PostData
import okhttp3.OkHttpClient

// Single shared OkHttpClient for both API and image calls — reuses connection pool,
// avoids per-request TLS handshake (mirrors httpx.AsyncClient shared in bot.py).
object WeiboRepository {

    val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val api = WeiboApi(client)
    val downloader = ImageDownloader(client)

    suspend fun fetchPost(postId: String): PostData? = api.fetchPost(postId)
}
