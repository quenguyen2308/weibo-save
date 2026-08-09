package com.weibosave.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weibosave.R
import com.weibosave.data.UsageTracker
import com.weibosave.data.WeiboRepository
import com.weibosave.util.UrlExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val urlInput: String = "",
    val indexInput: String = "",
    val clipboardUrl: String? = null,
    @StringRes val errorRes: Int? = null,
    val isDirectDownloading: Boolean = false,
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

    fun downloadDirectly(
        onReady: (postId: String, pids: List<String>, thumbUrls: List<String>, indices: List<Int>) -> Unit,
    ) {
        val url = _uiState.value.urlInput.trim()
        val postId = UrlExtractor.extractPostId(url) ?: run {
            _uiState.value = _uiState.value.copy(errorRes = R.string.error_invalid_url)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDirectDownloading = true, errorRes = null)
            try {
                val sessionId = UsageTracker.startSession(postId)
                val post = WeiboRepository.fetchPost(postId, sessionId)

                if (post == null || post.pics.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isDirectDownloading = false,
                        errorRes = R.string.error_no_images,
                    )
                    return@launch
                }

                val preIndices = parsedIndices()
                val allPics = post.pics
                val pics = if (preIndices.isEmpty()) allPics
                           else allPics.filterIndexed { i, _ -> i in preIndices.toSet() }
                                .takeIf { it.isNotEmpty() } ?: allPics

                UsageTracker.setImageCount(sessionId, pics.size)
                _uiState.value = _uiState.value.copy(isDirectDownloading = false)
                onReady(postId, pics.map { it.pid }, pics.map { it.thumbUrl }, pics.indices.toList())
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDirectDownloading = false,
                    errorRes = R.string.home_direct_download_failed,
                )
            }
        }
    }
}
