package com.daljeet.xplayer.feature.videopicker.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.daljeet.xplayer.core.ui.designsystem.animatedComposable
import com.daljeet.xplayer.feature.videopicker.screens.media.MediaPickerRoute

const val mediaPickerNavigationRoute = "media_picker_screen"

fun NavController.navigateToMediaPickerScreen(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(mediaPickerNavigationRoute, navOptions)
}

fun NavGraphBuilder.mediaPickerScreen(
    onSettingsClick: () -> Unit,
    onPlayVideo: (uri: Uri) -> Unit,
    onFolderClick: (path: String) -> Unit
) {
    animatedComposable(route = mediaPickerNavigationRoute) {
        MediaPickerRoute(
            onSettingsClick = onSettingsClick,
            onPlayVideo = onPlayVideo,
            onFolderClick = onFolderClick
        )
    }
}
