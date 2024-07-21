package com.daljeet.xplayer.core.data.repository

import android.content.Context
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import com.daljeet.xplayer.core.common.di.ApplicationScope
import com.daljeet.xplayer.core.common.extensions.deleteFiles
import com.daljeet.xplayer.core.data.mappers.toFolder
import com.daljeet.xplayer.core.data.mappers.toVideo
import com.daljeet.xplayer.core.data.mappers.toVideoState
import com.daljeet.xplayer.core.data.models.VideoState
import com.daljeet.xplayer.core.database.converter.UriListConverter
import com.daljeet.xplayer.core.database.dao.DirectoryDao
import com.daljeet.xplayer.core.database.dao.MediumDao
import com.daljeet.xplayer.core.database.entities.DirectoryEntity
import com.daljeet.xplayer.core.database.entities.MediumEntity
import com.daljeet.xplayer.core.database.relations.MediumWithInfo
import com.daljeet.xplayer.core.model.Folder
import com.daljeet.xplayer.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

class LocalMediaRepository @Inject constructor(
    private val mediumDao: MediumDao,
    private val directoryDao: DirectoryDao,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) : MediaRepository {

    override fun getVideosFlow(): Flow<List<Video>> {
        return mediumDao.getAllWithInfo().map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getVideosFlowFromFolderPath(folderPath: String): Flow<List<Video>> {
        return mediumDao.getAllWithInfoFromDirectory(folderPath).map { it.map(MediumWithInfo::toVideo) }
    }

    override fun getFoldersFlow(): Flow<List<Folder>> {
        return directoryDao.getAll().map { it.map(DirectoryEntity::toFolder) }
    }

    override suspend fun getVideoState(path: String): VideoState? {
        return mediumDao.get(path)?.toVideoState()
    }

    override suspend fun deleteVideos(videoUris: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val mediaUrisToDelete = videoUris.map { Uri.parse(it) }
        context.deleteFiles(mediaUrisToDelete, intentSenderLauncher)
    }

    override suspend fun deleteFolders(folderPaths: List<String>, intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>) {
        val mediumEntitiesToDelete = mutableListOf<MediumEntity>()
        for (path in folderPaths) {
            mediumEntitiesToDelete += mediumDao.getAllFromDirectory(path).first()
        }
        val mediaUrisToDelete = mediumEntitiesToDelete.map { Uri.parse(it.uriString) }
        context.deleteFiles(mediaUrisToDelete, intentSenderLauncher)
    }

    override suspend fun saveVideoState(
        path: String,
        position: Long,
        audioTrackIndex: Int?,
        subtitleTrackIndex: Int?,
        playbackSpeed: Float?,
        externalSubs: List<Uri>
    ) {
        Timber.d(
            "save state for [$path]: [$position, $audioTrackIndex, $subtitleTrackIndex, $playbackSpeed]"
        )

        applicationScope.launch {
            mediumDao.updateMediumState(
                path = path,
                position = position,
                audioTrackIndex = audioTrackIndex,
                subtitleTrackIndex = subtitleTrackIndex,
                playbackSpeed = playbackSpeed,
                externalSubs = UriListConverter.fromListToString(externalSubs)
            )
        }
    }
}
