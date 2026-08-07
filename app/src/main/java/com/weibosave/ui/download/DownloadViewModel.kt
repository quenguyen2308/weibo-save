package com.weibosave.ui.download

import androidx.lifecycle.ViewModel
import com.weibosave.model.DownloadItem
import com.weibosave.model.DownloadState
import com.weibosave.service.DownloadStateHolder
import kotlinx.coroutines.flow.StateFlow

class DownloadViewModel : ViewModel() {

    val items: StateFlow<List<DownloadItem>> = DownloadStateHolder.items
    val isRunning: StateFlow<Boolean> = DownloadStateHolder.isRunning

    fun doneCount() = items.value.count { it.state is DownloadState.Done }
    fun totalCount() = items.value.size
    fun totalBytes() = items.value.sumOf {
        (it.state as? DownloadState.Done)?.bytes ?: 0L
    }
}
