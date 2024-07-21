package com.daljeet.xplayer.settings.screens.audio

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.common.Utils.menuclick
import dagger.hilt.android.lifecycle.HiltViewModel
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.model.PlayerPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AudioPreferencesViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferencesFlow = preferencesRepository.playerPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerPreferences()
    )

    private val _uiState = MutableStateFlow(AudioPreferencesUIState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AudioPreferencesEvent) {
        if (event is AudioPreferencesEvent.ShowDialog) {
            _uiState.update {
                it.copy(showDialog = event.value)
            }
        }
    }

    fun updateAudioLanguage(value: String) {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(preferredAudioLanguage = value)
            }
        }
    }

    fun togglePauseOnHeadsetDisconnect() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(pauseOnHeadsetDisconnect = !it.pauseOnHeadsetDisconnect)
            }
        }
    }

    fun toggleShowSystemVolumePanel() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(showSystemVolumePanel = !it.showSystemVolumePanel)
            }
        }
    }

    fun toggleRequireAudioFocus() {
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(requireAudioFocus = !it.requireAudioFocus)
            }
        }
    }

    fun toggleShouldUseVolumeBoost() {
        menuclick.value = "booster";
        viewModelScope.launch {
            preferencesRepository.updatePlayerPreferences {
                it.copy(shouldUseVolumeBoost = !it.shouldUseVolumeBoost)
            }
        }
    }
}

data class AudioPreferencesUIState(
    val showDialog: AudioPreferenceDialog? = null
)

sealed interface AudioPreferenceDialog {
    object AudioLanguageDialog : AudioPreferenceDialog
}

sealed interface AudioPreferencesEvent {
    data class ShowDialog(val value: AudioPreferenceDialog?) : AudioPreferencesEvent
}

fun AudioPreferencesViewModel.showDialog(dialog: AudioPreferenceDialog) {
    onEvent(AudioPreferencesEvent.ShowDialog(dialog))
}

fun AudioPreferencesViewModel.hideDialog() {
    onEvent(AudioPreferencesEvent.ShowDialog(null))
}
