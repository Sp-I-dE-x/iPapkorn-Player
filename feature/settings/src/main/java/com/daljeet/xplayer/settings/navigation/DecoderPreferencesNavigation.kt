package com.daljeet.xplayer.settings.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.daljeet.xplayer.core.ui.designsystem.animatedComposable
import com.daljeet.xplayer.settings.screens.decoder.DecoderPreferencesScreen

const val decoderPreferencesNavigationRoute = "decoder_preferences_route"

fun NavController.navigateToDecoderPreferences(navOptions: NavOptions? = navOptions { launchSingleTop = true }) {
    this.navigate(decoderPreferencesNavigationRoute, navOptions)
}

fun NavGraphBuilder.decoderPreferencesScreen(onNavigateUp: () -> Unit) {
    animatedComposable(route = decoderPreferencesNavigationRoute) {
        DecoderPreferencesScreen(onNavigateUp = onNavigateUp)
    }
}
