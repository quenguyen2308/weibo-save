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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.weibosave.service.DownloadService
import com.weibosave.ui.album.AlbumScreen
import com.weibosave.ui.download.DownloadScreen
import com.weibosave.ui.home.HomeScreen
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

    androidx.compose.runtime.LaunchedEffect(navController) { onNavReady(navController) }

    NavHost(navController = navController, startDestination = start) {
        composable("home") {
            HomeScreen(
                onNavigateToAlbum = { postId, indices ->
                    val indicesArg = indices.joinToString(",")
                    navController.navigate("album/$postId?indices=$indicesArg")
                }
            )
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
                    navController.navigate("download") { launchSingleTop = true }
                },
            )
        }

        composable("download") {
            DownloadScreen(onBack = { navController.popBackStack() })
        }
    }
}
