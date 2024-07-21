package com.daljeet.xplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.daljeet.xplayer.core.ui.designsystem.animatedComposable
import com.daljeet.xplayer.settings.screens.audio.AudioPreferencesScreen

const val audioPreferencesNavigationRoute = "audio_preferences_route"

fun NavController.navigateToAudioPreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(audioPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.audioPreferencesScreen(onNavigateUp: () -> Unit) {
    animatedComposable(route = audioPreferencesNavigationRoute) {
        AudioPreferencesScreen(onNavigateUp = onNavigateUp)
    }
}
