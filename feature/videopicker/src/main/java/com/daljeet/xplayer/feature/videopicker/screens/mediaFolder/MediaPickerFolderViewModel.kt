package com.daljeet.xplayer.feature.videopicker.screens.mediaFolder

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.domain.GetSortedVideosUseCase
import com.daljeet.xplayer.core.media.sync.MediaInfoSynchronizer
import com.daljeet.xplayer.core.model.ApplicationPreferences
import com.daljeet.xplayer.feature.videopicker.navigation.FolderArgs
import com.daljeet.xplayer.feature.videopicker.screens.VideosState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MediaPickerFolderViewModel @Inject constructor(
    getSortedVideosUseCase: GetSortedVideosUseCase,
    savedStateHandle: SavedStateHandle,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer
) : ViewModel() {

    private val folderArgs = FolderArgs(savedStateHandle)

    val folderPath = folderArgs.folderId

    val videos = getSortedVideosUseCase.invoke(folderPath)
        .map { VideosState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VideosState.Loading
        )

    val preferences = preferencesRepository.applicationPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApplicationPreferences()
        )

    fun deleteVideos(uris: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            mediaRepository.deleteVideos(uris, intentSenderLauncher)
        }
    }

    fun addToMediaInfoSynchronizer(uri: Uri) {
        viewModelScope.launch {
            mediaInfoSynchronizer.addMedia(uri)
        }
    }
}
