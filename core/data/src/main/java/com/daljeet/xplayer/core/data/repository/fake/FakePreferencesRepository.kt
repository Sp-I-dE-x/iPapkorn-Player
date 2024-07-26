
package com.daljeet.xplayer.core.data.repository.fake

import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.model.ApplicationPreferences
import com.daljeet.xplayer.core.model.PlayerPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakePreferencesRepository : PreferencesRepository {

    private val applicationPreferencesStateFlow = MutableStateFlow(ApplicationPreferences())
    private val playerPreferencesStateFlow = MutableStateFlow(PlayerPreferences())

    override val applicationPreferences: Flow<ApplicationPreferences>
        get() = applicationPreferencesStateFlow
    override val playerPreferences: Flow<PlayerPreferences>
        get() = playerPreferencesStateFlow

    override suspend fun updateApplicationPreferences(
        transform: suspend (ApplicationPreferences) -> ApplicationPreferences,
    ) {
        applicationPreferencesStateFlow.update { transform.invoke(it) }
    }

    override suspend fun updatePlayerPreferences(
        transform: suspend (PlayerPreferences) -> PlayerPreferences,
    ) {
        playerPreferencesStateFlow.update { transform.invoke(it) }
    }
}
