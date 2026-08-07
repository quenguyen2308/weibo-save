package com.weibosave.util

// Port of extract_weibo_id() from bot.py — same 5 patterns, same order.
object UrlExtractor {

    private val PATTERNS = listOf(
        Regex("""weibo\.com/\d+/(\w+)"""),
        Regex("""weibo\.com/detail/(\w+)"""),
        Regex("""m\.weibo\.cn/detail/(\w+)"""),
        Regex("""m\.weibo\.cn/\d+/(\w+)"""),
        Regex("""m\.weibo\.cn/status/(\w+)"""),
    )

    fun extractPostId(url: String): String? =
        PATTERNS.firstNotNullOfOrNull { it.find(url)?.groupValues?.get(1) }

    fun isWeiboUrl(text: String) =
        "weibo.com" in text || "weibo.cn" in text
}
