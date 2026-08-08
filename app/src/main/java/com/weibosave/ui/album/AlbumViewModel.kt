package com.weibosave.ui.album

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weibosave.R
import com.weibosave.data.ImageDownloader
import com.weibosave.data.WeiboRepository
import com.weibosave.model.PicItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AlbumLoadState {
    data object Loading : AlbumLoadState()
    data class Success(val pics: List<PicItem>) : AlbumLoadState()
    data class Error(@StringRes val resId: Int, val arg: String? = null) : AlbumLoadState()
}

data class AlbumUiState(
    val postId: String = "",
    val loadState: AlbumLoadState = AlbumLoadState.Loading,
    val selected: Set<Int> = emptySet(),
    val selectionMode: Boolean = false,
    val previewIndex: Int? = null,
    val picSizes: Map<Int, Long> = emptyMap(),
)

class AlbumViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    fun load(postId: String, preIndices: List<Int> = emptyList()) {
        // Skip network call only when same postId AND no filter requested
        if (_uiState.value.postId == postId && preIndices.isEmpty()) return
        _uiState.value = AlbumUiState(postId = postId, loadState = AlbumLoadState.Loading)

        viewModelScope.launch {
            try {
                val post = WeiboRepository.fetchPost(postId)
                if (post == null || post.pics.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        loadState = AlbumLoadState.Error(R.string.error_no_images)
                    )
                    return@launch
                }
                val allPics = post.pics
                val pics = if (preIndices.isEmpty()) allPics
                           else allPics.filterIndexed { i, _ -> i in preIndices.toSet() }
                            .takeIf { it.isNotEmpty() } ?: allPics
                _uiState.value = _uiState.value.copy(loadState = AlbumLoadState.Success(pics))
                probeSizes(pics)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loadState = AlbumLoadState.Error(R.string.error_network, e.message ?: "")
                )
            }
        }
    }

    private fun probeSizes(pics: List<PicItem>) {
        viewModelScope.launch {
            pics.indices.chunked(ImageDownloader.CHUNK_SIZE).forEach { chunk ->
                chunk.map { index ->
                    async {
                        val result = WeiboRepository.downloader.probeBestUrlWithSize(pics[index].pid)
                        if (result != null && result.second > 0) {
                            _uiState.value = _uiState.value.copy(
                                picSizes = _uiState.value.picSizes + (index to result.second)
                            )
                        }
                    }
                }.awaitAll()
            }
        }
    }

    fun onImageClick(index: Int) {
        val state = _uiState.value
        if (state.selectionMode) toggleSelection(index)
        else _uiState.value = state.copy(previewIndex = index)
    }

    fun onImageLongPress(index: Int) {
        _uiState.value = _uiState.value.copy(
            selectionMode = true,
            selected = _uiState.value.selected + index,
        )
    }

    fun closePreview() {
        _uiState.value = _uiState.value.copy(previewIndex = null)
    }

    fun toggleSelection(index: Int) {
        val current = _uiState.value.selected.toMutableSet()
        if (current.contains(index)) current.remove(index) else current.add(index)
        _uiState.value = _uiState.value.copy(selected = current)
    }

    fun selectAll() {
        val pics = (_uiState.value.loadState as? AlbumLoadState.Success)?.pics ?: return
        _uiState.value = _uiState.value.copy(selectionMode = true, selected = pics.indices.toSet())
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selected = emptySet(), selectionMode = false)
    }

    fun selectedIndices(): List<Int> = _uiState.value.selected.sorted()

    fun allIndices(): List<Int> {
        val pics = (_uiState.value.loadState as? AlbumLoadState.Success)?.pics ?: return emptyList()
        return pics.indices.toList()
    }
}
