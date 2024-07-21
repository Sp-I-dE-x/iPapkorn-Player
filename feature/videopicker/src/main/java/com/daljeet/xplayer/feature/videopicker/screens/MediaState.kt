package com.daljeet.xplayer.feature.videopicker.screens

import com.daljeet.xplayer.core.model.Folder
import com.daljeet.xplayer.core.model.Video

sealed interface VideosState {
    object Loading : VideosState
    data class Success(val data: List<Video>) : VideosState
}

sealed interface FoldersState {
    object Loading : FoldersState
    data class Success(val data: List<Folder>) : FoldersState
}

sealed interface DownloadsState {
    object Loading : DownloadsState
    data class Success(val data: List<Video>) : DownloadsState
}
