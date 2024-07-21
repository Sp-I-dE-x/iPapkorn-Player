package com.daljeet.xplayer.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.navigation
import com.daljeet.xplayer.settings.Setting
import com.daljeet.xplayer.settings.navigation.aboutPreferencesScreen
import com.daljeet.xplayer.settings.navigation.appearancePreferencesScreen
import com.daljeet.xplayer.settings.navigation.audioPreferencesScreen
import com.daljeet.xplayer.settings.navigation.decoderPreferencesScreen
import com.daljeet.xplayer.settings.navigation.folderPreferencesScreen
import com.daljeet.xplayer.settings.navigation.mediaLibraryPreferencesScreen
import com.daljeet.xplayer.settings.navigation.navigateToAboutPreferences
import com.daljeet.xplayer.settings.navigation.navigateToAppearancePreferences
import com.daljeet.xplayer.settings.navigation.navigateToAudioPreferences
import com.daljeet.xplayer.settings.navigation.navigateToDecoderPreferences
import com.daljeet.xplayer.settings.navigation.navigateToFolderPreferencesScreen
import com.daljeet.xplayer.settings.navigation.navigateToMediaLibraryPreferencesScreen
import com.daljeet.xplayer.settings.navigation.navigateToPlayerPreferences
import com.daljeet.xplayer.settings.navigation.navigateToSubtitlePreferences
import com.daljeet.xplayer.settings.navigation.playerPreferencesScreen
import com.daljeet.xplayer.settings.navigation.settingsNavigationRoute
import com.daljeet.xplayer.settings.navigation.settingsScreen
import com.daljeet.xplayer.settings.navigation.subtitlePreferencesScreen

const val SETTINGS_ROUTE = "settings_nav_route"

fun NavGraphBuilder.settingsNavGraph(
    navController: NavHostController
) {
    navigation(
        startDestination = settingsNavigationRoute,
        route = SETTINGS_ROUTE
    ) {
        settingsScreen(
            onNavigateUp = navController::navigateUp,
            onItemClick = { setting ->
               when (setting) {
                    Setting.APPEARANCE -> navController.navigateToAppearancePreferences()
                    Setting.MEDIA_LIBRARY -> navController.navigateToMediaLibraryPreferencesScreen()
                    Setting.PLAYER -> navController.navigateToPlayerPreferences()
                    Setting.DECODER -> navController.navigateToDecoderPreferences()
                    Setting.AUDIO -> navController.navigateToAudioPreferences()
                    Setting.SUBTITLE -> navController.navigateToSubtitlePreferences()
                   Setting.ABOUT -> navController.navigateToAboutPreferences()
                }
            }
        )
        appearancePreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        mediaLibraryPreferencesScreen(
            onNavigateUp = navController::navigateUp,
            onFolderSettingClick = navController::navigateToFolderPreferencesScreen
        )
        folderPreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        playerPreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        decoderPreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        audioPreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        subtitlePreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
        aboutPreferencesScreen(
            onNavigateUp = navController::navigateUp
        )
    }
}
