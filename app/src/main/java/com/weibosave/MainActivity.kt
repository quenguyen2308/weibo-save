package com.weibosave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.weibosave.model.DownloadState
import com.weibosave.service.DownloadService
import com.weibosave.service.DownloadStateHolder
import com.weibosave.ui.album.AlbumScreen
import com.weibosave.ui.download.DownloadScreen
import com.weibosave.ui.home.HomeScreen
import com.weibosave.ui.stats.StatsScreen
import com.weibosave.ui.theme.WeiboSaveTheme
import com.weibosave.util.UrlExtractor

class MainActivity : ComponentActivity() {

    private var navController: NavController? = null

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, proceed — notification is nice-to-have */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val sharedPostId = resolveShareIntent(intent)

        setContent {
            WeiboSaveTheme {
                WeiboSaveApp(
                    startPostId = sharedPostId,
                    onNavReady = { navController = it },
                    onStartDownload = ::startDownload,
                    onFinish = ::finish,
                )
            }
        }
    }

    // Called when app is already running in singleTask mode and receives a new intent.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        resolveShareIntent(intent)?.let { postId ->
            navController?.navigate("album/$postId")
        }
    }

    private fun resolveShareIntent(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return null
        return UrlExtractor.extractPostId(text)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startDownload(
        postId: String,
        pids: List<String>,
        thumbUrls: List<String>,
        indices: List<Int>,
    ) {
        val serviceIntent = DownloadService.buildIntent(this, postId, pids, thumbUrls, indices)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}

@Composable
private fun WeiboSaveApp(
    startPostId: String?,
    onNavReady: (NavController) -> Unit,
    onStartDownload: (postId: String, pids: List<String>, thumbUrls: List<String>, indices: List<Int>) -> Unit,
    onFinish: () -> Unit,
) {
    val navController = rememberNavController()
    val start = remember { if (startPostId != null) "album/$startPostId" else "home" }

    LaunchedEffect(navController) { onNavReady(navController) }

    val isRunning by DownloadStateHolder.isRunning.collectAsState()
    var showDoneDialog by remember { mutableStateOf(false) }
    var doneCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        var wasRunning = false
        DownloadStateHolder.isRunning.collect { running ->
            if (wasRunning && !running) {
                doneCount = DownloadStateHolder.items.value.count { it.state is DownloadState.Done }
                showDoneDialog = true
            }
            wasRunning = running
        }
    }

    if (showDoneDialog) {
        Dialog(onDismissRequest = { showDoneDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.done_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.download_saved_summary, doneCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDoneDialog = false }) {
                            Text(stringResource(R.string.done_dialog_ok), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = start) {
        composable("home") {
            HomeScreen(
                onNavigateToAlbum = { postId, indices ->
                    val indicesArg = indices.joinToString(",")
                    navController.navigate("album/$postId?indices=$indicesArg")
                },
                onNavigateToStats = { navController.navigate("stats") },
                onStartDownload = { postId, pids, thumbUrls, indices ->
                    onStartDownload(postId, pids, thumbUrls, indices)
                },
            )
        }

        composable("stats") {
            StatsScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "album/{postId}?indices={indices}",
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType },
                navArgument("indices") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStack ->
            val postId = backStack.arguments?.getString("postId") ?: return@composable
            val indicesStr = backStack.arguments?.getString("indices") ?: ""
            val preIndices = if (indicesStr.isBlank()) emptyList()
                             else indicesStr.split(",").mapNotNull { it.toIntOrNull() }
            AlbumScreen(
                postId = postId,
                preIndices = preIndices,
                onBack = {
                    if (!navController.popBackStack()) onFinish()
                },
                onStartDownload = { pids, thumbUrls, indices ->
                    onStartDownload(postId, pids, thumbUrls, indices)
                },
            )
        }

        composable("download") {
            DownloadScreen(onBack = { navController.popBackStack() })
        }
        }

        AnimatedVisibility(
            visible = isRunning,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            stringResource(R.string.downloading_wait),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
