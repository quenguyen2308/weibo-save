package com.weibosave.model

sealed class DownloadState {
    data object Pending : DownloadState()
    data object Probing : DownloadState()
    data class Downloading(val progress: Float, val totalBytes: Long) : DownloadState()
    data class Done(val url: String, val bytes: Long) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

data class DownloadItem(
    val index: Int,
    val pid: String,
    val thumbUrl: String,
    val state: DownloadState = DownloadState.Pending,
)

