package com.weibosave.model

data class UsageSession(
    val id: String,
    val postId: String,
    val timestamp: Long,
    val apiBytes: Long = 0L,
    val viewBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val imageCount: Int = 0,
    val downloadCount: Int = 0,
) {
    val totalBytes: Long get() = apiBytes + viewBytes + downloadBytes
}
