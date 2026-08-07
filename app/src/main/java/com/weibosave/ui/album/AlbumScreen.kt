package com.weibosave.ui.album

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.weibosave.R
import com.weibosave.model.PicItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    postId: String,
    preIndices: List<Int> = emptyList(),
    onBack: () -> Unit,
    onStartDownload: (pids: List<String>, thumbUrls: List<String>, indices: List<Int>) -> Unit,
    vm: AlbumViewModel = viewModel(),
) {
    LaunchedEffect(postId) { vm.load(postId, preIndices) }

    val uiState by vm.uiState.collectAsState()
    val pics = (uiState.loadState as? AlbumLoadState.Success)?.pics ?: emptyList()
    val selectedCount = uiState.selected.size
    val selectionMode = uiState.selectionMode

    uiState.previewIndex?.let { index ->
        if (pics.isNotEmpty()) {
            ImagePreviewDialog(pics = pics, initialIndex = index, onDismiss = vm::closePreview)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            selectionMode && selectedCount > 0 ->
                                stringResource(R.string.album_selected_count, selectedCount)
                            selectionMode -> stringResource(R.string.album_select_photos)
                            pics.isEmpty() -> stringResource(R.string.album_loading)
                            else -> stringResource(R.string.album_photo_count, pics.size)
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) vm.clearSelection() else onBack() }) {
                        Icon(
                            if (selectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = stringResource(
                                if (selectionMode) R.string.album_cancel_selection else R.string.album_back
                            ),
                        )
                    }
                },
                actions = {
                    if (selectionMode && pics.isNotEmpty()) {
                        val allSelected = selectedCount == pics.size
                        IconButton(onClick = {
                            if (allSelected) vm.clearSelection() else vm.selectAll()
                        }) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .clip(CircleShape)
                                    .background(
                                        if (allSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = stringResource(
                                        if (allSelected) R.string.album_deselect_all
                                        else R.string.album_select_all
                                    ),
                                    tint = if (allSelected) MaterialTheme.colorScheme.onPrimary
                                           else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (pics.isNotEmpty()) {
                AlbumActionBar(
                    pics = pics,
                    selectionMode = selectionMode,
                    selectedCount = selectedCount,
                    onDownloadSelected = {
                        val indices = vm.selectedIndices()
                        onStartDownload(pics.map { it.pid }, pics.map { it.thumbUrl }, indices)
                    },
                    onDownloadAll = {
                        onStartDownload(
                            pics.map { it.pid }, pics.map { it.thumbUrl }, vm.allIndices()
                        )
                    },
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val state = uiState.loadState) {
                is AlbumLoadState.Loading -> CircularProgressIndicator()
                is AlbumLoadState.Error -> Text(
                    text = state.arg?.let { stringResource(state.resId, it) }
                        ?: stringResource(state.resId),
                    color = MaterialTheme.colorScheme.error,
                )
                is AlbumLoadState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(state.pics) { index, pic ->
                            ThumbCell(
                                pic = pic,
                                index = index,
                                selectionMode = selectionMode,
                                isSelected = index in uiState.selected,
                                sizeBytes = uiState.picSizes[index],
                                onClick = { vm.onImageClick(index) },
                                onLongClick = { vm.onImageLongPress(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThumbCell(
    pic: PicItem,
    index: Int,
    selectionMode: Boolean,
    isSelected: Boolean,
    sizeBytes: Long?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = pic.thumbUrl,
            contentDescription = stringResource(R.string.album_photo_label, index + 1),
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // Subtle dark overlay when selected (matches gallery app dimming)
        if (selectionMode && isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0x33000000)))
        }
        // Gallery-style circle indicator: ring when unselected, filled+check when selected
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Black.copy(alpha = 0.25f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        // Index badge — bottom left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x88000000))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        // Size badge — bottom right (appears as probe completes)
        if (sizeBytes != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x88000000))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(formatBytes(sizeBytes), style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    pics: List<PicItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            val pagerState = rememberPagerState(initialPage = initialIndex) { pics.size }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = "https://wx2.sinaimg.cn/large/${pics[page].pid}.jpg",
                    contentDescription = stringResource(R.string.album_photo_label, page + 1),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.album_close),
                        tint = Color.White,
                    )
                }
                Text(
                    stringResource(R.string.album_photo_counter, pagerState.currentPage + 1, pics.size),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun AlbumActionBar(
    pics: List<PicItem>,
    selectionMode: Boolean,
    selectedCount: Int,
    onDownloadSelected: () -> Unit,
    onDownloadAll: () -> Unit,
) {
    Surface(tonalElevation = 4.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectionMode && selectedCount > 0) {
                Button(onClick = onDownloadSelected, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.album_download_selected, selectedCount))
                }
            }
            OutlinedButton(onClick = onDownloadAll, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.album_download_all, pics.size))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String =
    if (bytes >= 1_048_576) "%.1f MB".format(bytes / 1_048_576.0)
    else "${bytes / 1024} KB"
