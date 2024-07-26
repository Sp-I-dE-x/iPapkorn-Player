package com.daljeet.xplayer.core.data.repository

import android.net.Uri
import com.daljeet.xplayer.core.data.models.VideoState
import com.daljeet.xplayer.core.model.Folder
import com.daljeet.xplayer.core.model.Video
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getVideosFlow(): Flow<List<Video>>
    fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>>
    fun getFoldersFlow(): Flow<List<Folder>>
    suspend fun saveVideoState(
        uri: String,
        position: Long,
        audioTrackIndex: Int?,
        subtitleTrackIndex: Int?,
        playbackSpeed: Float?,
        externalSubs: List<Uri>,
        videoScale: Float,
    )
    suspend fun getVideoState(uri: String): VideoState?
}
