package com.weibosave

import com.weibosave.util.UrlExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlExtractorTest {

    // Mirrors the 5 URL patterns from bot.py extract_weibo_id()
    @Test
    fun `extracts id from weibo com user url`() {
        val url = "https://weibo.com/1234567890/Nkq8AbcXyz"
        assertEquals("Nkq8AbcXyz", UrlExtractor.extractPostId(url))
    }

    @Test
    fun `extracts id from weibo com detail url`() {
        val url = "https://weibo.com/detail/Nkq8AbcXyz"
        assertEquals("Nkq8AbcXyz", UrlExtractor.extractPostId(url))
    }

    @Test
    fun `extracts id from m weibo cn detail url`() {
        val url = "https://m.weibo.cn/detail/Nkq8AbcXyz"
        assertEquals("Nkq8AbcXyz", UrlExtractor.extractPostId(url))
    }

    @Test
    fun `extracts id from m weibo cn user url`() {
        val url = "https://m.weibo.cn/1234567890/Nkq8AbcXyz"
        assertEquals("Nkq8AbcXyz", UrlExtractor.extractPostId(url))
    }

    @Test
    fun `extracts id from m weibo cn status url`() {
        val url = "https://m.weibo.cn/status/Nkq8AbcXyz"
        assertEquals("Nkq8AbcXyz", UrlExtractor.extractPostId(url))
    }

    @Test
    fun `returns null for non weibo url`() {
        assertNull(UrlExtractor.extractPostId("https://twitter.com/user/123"))
    }

    @Test
    fun `detects weibo url in clipboard text`() {
        assert(UrlExtractor.isWeiboUrl("check this https://weibo.com/1234/abc out"))
        assert(!UrlExtractor.isWeiboUrl("no weibo here"))
    }
}
