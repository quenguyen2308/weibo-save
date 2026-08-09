package com.weibosave.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weibosave.R
import com.weibosave.model.UsageSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    vm: StatsViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()

    uiState.showDeleteConfirm?.let { scope ->
        val label = when (scope) {
            DeleteScope.WEEK -> stringResource(R.string.stats_delete_week)
            DeleteScope.MONTH -> stringResource(R.string.stats_delete_month)
            DeleteScope.ALL -> stringResource(R.string.stats_delete_all)
        }
        AlertDialog(
            onDismissRequest = vm::dismissDelete,
            title = { Text(stringResource(R.string.stats_delete_confirm_title)) },
            text = { Text(stringResource(R.string.stats_delete_confirm_body, label)) },
            confirmButton = {
                TextButton(onClick = vm::confirmDelete) {
                    Text(stringResource(R.string.stats_delete_confirm_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDelete) {
                    Text(stringResource(R.string.stats_delete_confirm_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.stats_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Summary card
            SummaryCard(
                sessions = uiState.filteredSessions,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.filter == filter,
                        onClick = { vm.setFilter(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    DateFilter.TODAY -> stringResource(R.string.stats_filter_today)
                                    DateFilter.THIS_WEEK -> stringResource(R.string.stats_filter_week)
                                    DateFilter.THIS_MONTH -> stringResource(R.string.stats_filter_month)
                                    DateFilter.ALL -> stringResource(R.string.stats_filter_all)
                                },
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Session list
            if (uiState.filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.stats_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 4.dp
                    ),
                ) {
                    items(uiState.filteredSessions, key = { it.id }) { session ->
                        SessionCard(session)
                    }
                }
            }

            // Delete actions bottom bar
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { vm.requestDelete(DeleteScope.WEEK) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.stats_delete_week), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { vm.requestDelete(DeleteScope.MONTH) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.stats_delete_month), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = { vm.requestDelete(DeleteScope.ALL) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.stats_delete_all), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(sessions: List<UsageSession>, modifier: Modifier = Modifier) {
    val totalApi = sessions.sumOf { it.apiBytes }
    val totalView = sessions.sumOf { it.viewBytes }
    val totalDownload = sessions.sumOf { it.downloadBytes }
    val grandTotal = totalApi + totalView + totalDownload

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.stats_total_data, formatBytes(grandTotal)),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.stats_total_sessions, sessions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ByteChip(label = stringResource(R.string.stats_api_label), bytes = totalApi, modifier = Modifier.weight(1f))
                ByteChip(label = stringResource(R.string.stats_view_label), bytes = totalView, modifier = Modifier.weight(1f))
                ByteChip(label = stringResource(R.string.stats_download_label), bytes = totalDownload, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ByteChip(label: String, bytes: Long, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
        Text(formatBytes(bytes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SessionCard(session: UsageSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    session.postId,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    formatDate(session.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (session.imageCount > 0) {
                    Text(
                        stringResource(R.string.stats_images, session.imageCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (session.downloadCount > 0) {
                    Text(
                        stringResource(R.string.stats_downloaded, session.downloadCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ByteRow(stringResource(R.string.stats_api_label), session.apiBytes)
                    ByteRow(stringResource(R.string.stats_view_label), session.viewBytes)
                    ByteRow(stringResource(R.string.stats_download_label), session.downloadBytes)
                }
                Text(
                    stringResource(R.string.stats_total_data, formatBytes(session.totalBytes)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ByteRow(label: String, bytes: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        )
        Text(formatBytes(bytes), style = MaterialTheme.typography.labelSmall)
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "${bytes} B"
}

private val dateFormatter = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
private fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))
