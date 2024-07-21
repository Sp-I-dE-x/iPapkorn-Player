package com.daljeet.xplayer.feature.videopicker.screens.media

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.domain.GetSortedDownloadVideosUseCase
import com.daljeet.xplayer.core.domain.GetSortedFoldersUseCase
import com.daljeet.xplayer.core.domain.GetSortedVideosUseCase
import com.daljeet.xplayer.core.media.sync.MediaInfoSynchronizer
import com.daljeet.xplayer.core.model.ApplicationPreferences
import com.daljeet.xplayer.feature.videopicker.screens.DownloadsState
import com.daljeet.xplayer.feature.videopicker.screens.FoldersState
import com.daljeet.xplayer.feature.videopicker.screens.VideosState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    getSortedVideosUseCase: GetSortedVideosUseCase,
    getSortedFoldersUseCase: GetSortedFoldersUseCase,
    getSortedDownloadVideosUseCase: GetSortedDownloadVideosUseCase,
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer
) : ViewModel() {

    val videosState = getSortedVideosUseCase.invoke()
        .map { VideosState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VideosState.Loading
        )

    val downloadsState = getSortedDownloadVideosUseCase.invoke("/storage/emulated/0/Download/iPapkorn")
        .map { DownloadsState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DownloadsState.Loading
        )

    val foldersState = getSortedFoldersUseCase.invoke()
        .map { FoldersState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FoldersState.Loading
        )

    val preferences = preferencesRepository.applicationPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApplicationPreferences()
        )

    fun updateMenu(applicationPreferences: ApplicationPreferences) {
        viewModelScope.launch {
            preferencesRepository.updateApplicationPreferences { applicationPreferences }
        }
    }

    fun deleteVideos(uris: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            mediaRepository.deleteVideos(uris, intentSenderLauncher)
        }
    }

    fun deleteFolders(paths: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        viewModelScope.launch {
            mediaRepository.deleteFolders(paths, intentSenderLauncher)
        }
    }

    fun addToMediaInfoSynchronizer(uri: Uri) {
        viewModelScope.launch {
            mediaInfoSynchronizer.addMedia(uri)
        }
    }
}
