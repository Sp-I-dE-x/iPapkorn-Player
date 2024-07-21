package com.daljeet.xplayer.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.daljeet.xplayer.core.ui.R
import com.daljeet.xplayer.core.ui.components.NextCenterAlignedTopAppBar
import com.daljeet.xplayer.core.ui.designsystem.NextIcons
import com.daljeet.xplayer.feature.player.PlayerActivity
import com.daljeet.xplayer.feature.videopicker.composables.DownloadsView
import com.daljeet.xplayer.feature.videopicker.composables.VideosView
import com.daljeet.xplayer.feature.videopicker.screens.media.MediaPickerViewModel
import com.daljeet.xplayer.navigation.MEDIA_ROUTE
import com.daljeet.xplayer.navigation.mediaNavGraph
import com.daljeet.xplayer.navigation.settingsNavGraph
import com.daljeet.xplayer.navigation.startPlayerActivity
import com.daljeet.xplayer.settings.navigation.navigateToSettings
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
    var contollerMain = rememberNavController()

    Scaffold(
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .consumeWindowInsets(it)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        )
        {
            NextCenterAlignedTopAppBar(
                title = "Downloads",
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DownloadsView(
                    videosState = downloadState,
                    onVideoClick = context::startPlayerActivity,
                    preferences = preferences,
                    onDeleteVideoClick = {},
                    onVideoLoaded = { }
                )
            }
        }
    }
}


