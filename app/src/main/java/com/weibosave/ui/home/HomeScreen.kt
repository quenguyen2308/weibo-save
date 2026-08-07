package com.weibosave.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.weibosave.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAlbum: (postId: String, indices: List<Int>) -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val uiState by vm.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        vm.onClipboardChecked(clipboard.getText()?.text)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            uiState.clipboardUrl?.let { clipUrl ->
                item {
                    ClipboardBanner(
                        url = clipUrl,
                        onUse = { vm.useClipboardUrl() },
                        onDismiss = { vm.dismissClipboardBanner() },
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.urlInput,
                    onValueChange = vm::onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_url_label)) },
                    placeholder = { Text(stringResource(R.string.home_url_placeholder)) },
                    isError = uiState.errorRes != null,
                    supportingText = uiState.errorRes?.let { res -> { Text(stringResource(res)) } },
                    trailingIcon = {
                        if (uiState.urlInput.isNotEmpty()) {
                            IconButton(onClick = { vm.onUrlChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
                            }
                        } else {
                            IconButton(onClick = {
                                vm.onUrlChange(clipboard.getText()?.text ?: "")
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = stringResource(R.string.cd_paste))
                            }
                        }
                    },
                    singleLine = true,
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.indexInput,
                    onValueChange = vm::onIndexInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_index_label)) },
                    placeholder = { Text(stringResource(R.string.home_index_placeholder)) },
                    trailingIcon = {
                        if (uiState.indexInput.isNotEmpty()) {
                            IconButton(onClick = { vm.onIndexInputChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cd_clear))
                            }
                        }
                    },
                    singleLine = true,
                )
            }

            item {
                Button(
                    onClick = { vm.validateAndGetPostId()?.let { onNavigateToAlbum(it, vm.parsedIndices()) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.urlInput.isNotBlank(),
                ) {
                    Text(stringResource(R.string.home_action_view))
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ClipboardBanner(url: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_clipboard_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(url, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_clipboard_dismiss)) }
            Button(onClick = onUse) { Text(stringResource(R.string.home_clipboard_use)) }
        }
    }
}

