package com.weibosave.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weibosave.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAlbum: (postId: String, indices: List<Int>) -> Unit,
    onNavigateToStats: () -> Unit = {},
    onStartDownload: (postId: String, pids: List<String>, thumbUrls: List<String>, indices: List<Int>) -> Unit = { _, _, _, _ -> },
    vm: HomeViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    val accent = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(accent, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("W", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        }
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToStats) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = stringResource(R.string.stats_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                InputCard(
                    value = uiState.urlInput,
                    onValueChange = vm::onUrlChange,
                    label = stringResource(R.string.home_url_label).uppercase(),
                    placeholder = stringResource(R.string.home_url_placeholder),
                    maxLines = 2,
                    onPaste = { vm.onUrlChange(clipboard.getText()?.text ?: "") },
                    errorRes = uiState.errorRes,
                )
            }

            item {
                InputCard(
                    value = uiState.indexInput,
                    onValueChange = vm::onIndexInputChange,
                    label = stringResource(R.string.home_index_label).uppercase(),
                    placeholder = stringResource(R.string.home_index_placeholder),
                )
            }

            item {
                Button(
                    onClick = {
                        vm.validateAndGetPostId()?.let { onNavigateToAlbum(it, vm.parsedIndices()) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState.urlInput.isNotBlank() && !uiState.isDirectDownloading,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        stringResource(R.string.home_action_view),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        vm.downloadDirectly { postId, pids, thumbUrls, indices ->
                            onStartDownload(postId, pids, thumbUrls, indices)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = uiState.urlInput.isNotBlank() && !uiState.isDirectDownloading,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    if (uiState.isDirectDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.home_fetching),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    } else {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            stringResource(R.string.home_action_download),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}


/**
 * Styled dark-surface input card used for URL and photo number inputs.
 * When [onPaste] is non-null, shows a paste icon while empty; always shows
 * a clear icon when [value] is non-empty.
 */
@Composable
private fun InputCard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    onPaste: (() -> Unit)? = null,
    @StringRes errorRes: Int? = null,
) {
    val hasError = errorRes != null
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (hasError) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.outlineVariant

    // Single TextStyle shared by both placeholder and BasicTextField so
    // they stay on the same baseline — the root cause of the cursor jump.
    val inputTextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = onSurface,
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.1.sp,
                color = if (hasError) MaterialTheme.colorScheme.error else onSurfaceVariant,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = maxLines == 1,
                maxLines = maxLines,
                textStyle = inputTextStyle,
                cursorBrush = SolidColor(accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                // Same TextStyle as BasicTextField so baseline aligns perfectly.
                                Text(
                                    placeholder,
                                    style = inputTextStyle.copy(
                                        color = onSurfaceVariant.copy(alpha = 0.5f),
                                    ),
                                )
                            }
                            innerTextField()
                        }
                        val showPaste = onPaste != null && value.isEmpty()
                        val showClear = value.isNotEmpty()
                        if (showPaste || showClear) {
                            IconButton(
                                onClick = if (showPaste) onPaste!! else ({ onValueChange("") }),
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    if (showPaste) Icons.Default.ContentPaste else Icons.Default.Clear,
                                    contentDescription = stringResource(
                                        if (showPaste) R.string.cd_paste else R.string.cd_clear,
                                    ),
                                    modifier = Modifier.size(18.dp),
                                    tint = onSurfaceVariant,
                                )
                            }
                        }
                    }
                },
            )
            if (hasError) {
                Text(
                    stringResource(errorRes!!),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
