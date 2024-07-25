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

/**
 * Starts the PlayerActivity to play a video from the given URI.
 *
 * @param uri The URI of the video to be played.
 */
fun Context.startPlayerActivity(uri: Uri?) {
    uri?.let {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    } ?: run {
        // Handle the case where the URI is null
        // This could be a log message or a user-facing error message
    }
}
