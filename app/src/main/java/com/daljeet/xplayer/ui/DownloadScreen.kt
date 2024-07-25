package com.daljeet.xplayer.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.daljeet.xplayer.R
import com.daljeet.xplayer.core.ui.components.NextCenterAlignedTopAppBar
import com.daljeet.xplayer.feature.videopicker.composables.DownloadsView
import com.daljeet.xplayer.feature.videopicker.screens.media.MediaPickerViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun DownloadScreen(
    permissionState: PermissionState,
) {
    val context = LocalContext.current
    val viewModel: MediaPickerViewModel = hiltViewModel()
    val downloadState by viewModel.downloadsState.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            NextCenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.downloads))
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .consumeWindowInsets(paddingValues)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    DownloadsView(
                        videosState = downloadState,
                        onVideoClick = { video ->
                            context.startPlayerActivity(video)
                        },
                        preferences = preferences,
                        onDeleteVideoClick = { video ->
                            viewModel.deleteVideo(video)
                        },
                        onVideoLoaded = { video ->
                            viewModel.markVideoAsLoaded(video)
                        }
                    )
                }
            }
        }
    )
}
