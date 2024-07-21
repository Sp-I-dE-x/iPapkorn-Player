package com.daljeet.xplayer.core.data.repository.fake

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.daljeet.xplayer.core.data.models.VideoState
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.model.Folder
import com.daljeet.xplayer.core.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMediaRepository : MediaRepository {

    val videos = mutableListOf<Video>()
    val directories = mutableListOf<Folder>()

    override fun getVideosFlow(): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return flowOf(videos)
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return flowOf(directories)
    }

    override suspend fun saveVideoState(
        path: String,
        position: Long,
        audioTrackIndex: Int?,
        subtitleTrackIndex: Int?,
        playbackSpeed: Float?,
        externalSubs: List<Uri>
    ) {
    }

    override suspend fun getVideoState(path: String): VideoState? {
        return null
    }

    override suspend fun deleteVideos(videoUris: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFolders(folderPaths: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        TODO("Not yet implemented")
    }
}
