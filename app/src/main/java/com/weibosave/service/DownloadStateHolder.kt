package com.weibosave.service

import com.weibosave.model.DownloadItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Singleton shared between DownloadService and DownloadViewModel so the UI
// can observe real-time progress without binding to the service.
object DownloadStateHolder {

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    fun setItems(items: List<DownloadItem>) { _items.value = items }

    fun updateItem(updated: DownloadItem) {
        _items.value = _items.value.map { if (it.index == updated.index) updated else it }
    }

    fun setRunning(running: Boolean) { _isRunning.value = running }
}
