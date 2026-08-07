package com.weibosave.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.weibosave.R
import com.weibosave.util.UrlExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val urlInput: String = "",
    val indexInput: String = "",
    val clipboardUrl: String? = null,
    @StringRes val errorRes: Int? = null,
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onUrlChange(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, errorRes = null)
    }

    fun onIndexInputChange(value: String) {
        _uiState.value = _uiState.value.copy(indexInput = value)
    }

    // Returns 0-based indices; empty list means "all photos"
    fun parsedIndices(): List<Int> {
        val input = _uiState.value.indexInput.trim()
        if (input.isBlank()) return emptyList()
        return input.split(",").flatMap { part ->
            val trimmed = part.trim()
            val dash = trimmed.indexOf('-').takeIf { it > 0 }
            if (dash != null) {
                val start = trimmed.substring(0, dash).trim().toIntOrNull() ?: return@flatMap emptyList()
                val end = trimmed.substring(dash + 1).trim().toIntOrNull() ?: return@flatMap emptyList()
                (minOf(start, end)..maxOf(start, end)).toList()
            } else {
                listOf(trimmed.toIntOrNull() ?: return@flatMap emptyList())
            }
        }.filter { it >= 1 }.map { it - 1 }.distinct().sorted()
    }

    fun onClipboardChecked(text: String?) {
        val weiboUrl = text?.takeIf { UrlExtractor.isWeiboUrl(it) }
        _uiState.value = _uiState.value.copy(clipboardUrl = weiboUrl)
    }

    fun useClipboardUrl() {
        val url = _uiState.value.clipboardUrl ?: return
        _uiState.value = _uiState.value.copy(urlInput = url, clipboardUrl = null)
    }

    fun dismissClipboardBanner() {
        _uiState.value = _uiState.value.copy(clipboardUrl = null)
    }

    fun validateAndGetPostId(): String? {
        val url = _uiState.value.urlInput.trim()
        val postId = UrlExtractor.extractPostId(url)
        if (postId == null) {
            _uiState.value = _uiState.value.copy(errorRes = R.string.error_invalid_url)
        }
        return postId
    }
}
