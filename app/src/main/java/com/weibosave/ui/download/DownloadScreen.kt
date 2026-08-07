package com.weibosave.ui.download

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.weibosave.R
import com.weibosave.model.DownloadItem
import com.weibosave.model.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    onBack: () -> Unit,
    vm: DownloadViewModel = viewModel(),
) {
    val items by vm.items.collectAsState()
    val isRunning by vm.isRunning.collectAsState()

    val doneCount = items.count { it.state is DownloadState.Done }
    val total = items.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isRunning) stringResource(R.string.download_running, doneCount, total)
                            else stringResource(R.string.download_done_title, doneCount, total),
                            fontWeight = FontWeight.Bold,
                        )
                        if (isRunning) {
                            Text(
                                stringResource(R.string.download_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.download_back))
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (isRunning && total > 0) {
                item {
                    LinearProgressIndicator(
                        progress = { doneCount.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    )
                }
            }

            if (!isRunning && items.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    stringResource(R.string.download_saved_summary, doneCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    stringResource(R.string.download_total, formatBytes(vm.totalBytes())),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            items(items, key = { it.index }) { item -> DownloadItemRow(item) }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DownloadItemRow(item: DownloadItem) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), tonalElevation = 2.dp) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (item.thumbUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                if (item.state is DownloadState.Done) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xAA1B4332)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4DC98A), modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                val filename = (item.state as? DownloadState.Done)?.url
                    ?.substringAfterLast("/") ?: "${item.pid}.jpg"
                Text(
                    "#${item.index + 1} · $filename",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
                Spacer(Modifier.height(4.dp))
                StateRow(item.state)
            }
        }
    }
}

@Composable
private fun StateRow(state: DownloadState) {
    when (state) {
        is DownloadState.Pending -> Text(
            stringResource(R.string.download_state_pending),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is DownloadState.Probing -> Text(
            stringResource(R.string.download_state_probing),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        is DownloadState.Downloading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
            )
            Text("${(state.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        }
        is DownloadState.Done -> Text(
            "✓ ${formatBytes(state.bytes)} · ${state.url.substringAfter("sinaimg.cn/").substringBefore("/")}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4DC98A),
        )
        is DownloadState.Error -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
            Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "?"
    return if (bytes >= 1_048_576) "%.1f MB".format(bytes / 1_048_576.0)
    else "${bytes / 1024} KB"
}
