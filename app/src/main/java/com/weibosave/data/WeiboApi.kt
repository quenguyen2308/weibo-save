package com.weibosave.data

import android.util.Log
import com.weibosave.model.PicItem
import com.weibosave.model.PostData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "WeiboApi"

// Port of get_pic_list() + _build_full_pic_list() from bot.py.
class WeiboApi(private val client: OkHttpClient) {

    suspend fun fetchPost(postId: String, sessionId: String? = null): PostData? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://m.weibo.cn/statuses/show?id=$postId")
                .header("User-Agent", UA_API)
                .header("Referer", "https://m.weibo.cn/")
                .header("Accept", "application/json, text/plain, */*")
                .header("MWeibo-Pwa", "1")
                .header("X-Requested-With", "XMLHttpRequest")
                .build()

            val body = client.newCall(request).execute().use { resp ->
                Log.d(TAG, "API response: ${resp.code} for postId=$postId")
                if (!resp.isSuccessful) {
                    Log.e(TAG, "API failed: ${resp.code} ${resp.message}")
                    return@withContext null
                }
                resp.body?.string() ?: run {
                    Log.e(TAG, "Empty body for postId=$postId")
                    return@withContext null
                }
            }

            if (sessionId != null) {
                UsageTracker.addApiBytes(sessionId, body.toByteArray(Charsets.UTF_8).size.toLong())
            }

            val root = JSONObject(body)
            Log.d(TAG, "API ok=${root.optInt("ok")} for postId=$postId")

            val postData = root.optJSONObject("data") ?: run {
                Log.e(TAG, "No 'data' field in response")
                return@withContext null
            }

            val pics = buildFullPicList(postData)
            Log.d(TAG, "Found ${pics.size} images for postId=$postId")
            PostData(postId = postId, pics = pics)

        } catch (e: Exception) {
            Log.e(TAG, "fetchPost failed for postId=$postId", e)
            null
        }
    }

    // Port of _build_full_pic_list(): uses pic_ids as source of truth (not truncated
    // at 9 like pics[]), supplements per-pid detail from pics/pics_more where available.
    private fun buildFullPicList(postData: JSONObject): List<PicItem> {
        val byPid = mutableMapOf<String, PicItem>()

        fun collectPics(arr: JSONArray?) {
            arr ?: return
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pid = obj.optString("pid")
                if (pid.isBlank()) continue
                val url = obj.optString("url").ifBlank { "https://wx2.sinaimg.cn/orj360/$pid.jpg" }
                byPid[pid] = PicItem(pid = pid, thumbUrl = url)
            }
        }

        collectPics(postData.optJSONArray("pics"))
        collectPics(postData.optJSONArray("pics_more"))

        val picIds: List<String> = postData.optJSONArray("pic_ids")
            ?.let { arr -> (0 until arr.length()).map { arr.getString(it) } }
            ?: byPid.keys.toList()

        return picIds.map { pid ->
            byPid[pid] ?: PicItem(pid = pid, thumbUrl = "https://wx2.sinaimg.cn/orj360/$pid.jpg")
        }
    }

    companion object {
        private const val UA_API =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"
    }
}
