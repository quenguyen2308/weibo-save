package com.weibosave.model

data class PicItem(
    val pid: String,
    val thumbUrl: String,
)

data class PostData(
    val postId: String,
    val pics: List<PicItem>,
)
