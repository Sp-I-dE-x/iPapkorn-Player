package com.daljeet.xplayer.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.daljeet.xplayer.feature.player.PlayerActivity
import com.daljeet.xplayer.feature.videopicker.navigation.mediaPickerFolderScreen
import com.daljeet.xplayer.feature.videopicker.navigation.mediaPickerNavigationRoute
import com.daljeet.xplayer.feature.videopicker.navigation.mediaPickerScreen
import com.daljeet.xplayer.feature.videopicker.navigation.navigateToMediaPickerFolderScreen
import com.daljeet.xplayer.settings.navigation.navigateToSettings

const val MEDIA_ROUTE = "media_nav_route"

fun NavGraphBuilder.mediaNavGraph(
    context: Context,
    mainNavController: NavHostController,
    mediaNavController: NavHostController
) {
    navigation(
        startDestination = mediaPickerNavigationRoute,
        route = MEDIA_ROUTE
    ) {
        mediaPickerScreen(
            onPlayVideo = context::startPlayerActivity,
            onFolderClick = mediaNavController::navigateToMediaPickerFolderScreen,
            onSettingsClick = mainNavController::navigateToSettings
        )
        mediaPickerFolderScreen(
            onNavigateUp = mediaNavController::navigateUp,
            onVideoClick = context::startPlayerActivity
        )
    }
}

fun Context.startPlayerActivity(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri, this, PlayerActivity::class.java)
    startActivity(intent)
}
