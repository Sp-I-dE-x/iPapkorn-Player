package com.daljeet.xplayer.core.media.sync

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.daljeet.xplayer.core.common.Dispatcher
import com.daljeet.xplayer.core.common.NextDispatchers
import com.daljeet.xplayer.core.common.di.ApplicationScope
import com.daljeet.xplayer.core.common.extensions.getPath
import com.daljeet.xplayer.core.common.extensions.thumbnailCacheDir
import com.daljeet.xplayer.core.database.dao.MediumDao
import com.daljeet.xplayer.core.database.entities.AudioStreamInfoEntity
import com.daljeet.xplayer.core.database.entities.SubtitleStreamInfoEntity
import com.daljeet.xplayer.core.database.entities.VideoStreamInfoEntity
import io.github.anilbeesetti.nextlib.mediainfo.AudioStream
import io.github.anilbeesetti.nextlib.mediainfo.MediaInfoBuilder
import io.github.anilbeesetti.nextlib.mediainfo.SubtitleStream
import io.github.anilbeesetti.nextlib.mediainfo.VideoStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalMediaInfoSynchronizer @Inject constructor(
    private val mediumDao: MediumDao,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val dispatcher: CoroutineDispatcher,
) : MediaInfoSynchronizer {

    private val media = MutableSharedFlow<Uri>()

    override suspend fun addMedia(uri: Uri) = media.emit(uri)

    private suspend fun sync(): Unit = withContext(dispatcher) {
        media.collect { mediumUri ->
            val medium = mediumDao.getWithInfo(mediumUri.toString()) ?: return@collect
            if (medium.mediumEntity.thumbnailPath?.let { File(it) }?.exists() == true) {
                return@collect
            }

            val mediaInfo = runCatching {
                MediaInfoBuilder().from(context = context, uri = mediumUri).build() ?: throw NullPointerException()
            }.onFailure { e ->
                e.printStackTrace()
                Log.d(TAG, "sync: MediaInfoBuilder exception", e)
            }.getOrNull() ?: return@collect

            val thumbnail = runCatching { mediaInfo.getFrame() }.getOrNull()
            mediaInfo.release()

            val videoStreamInfo =
                mediaInfo.videoStream?.toVideoStreamInfoEntity(medium.mediumEntity.uriString)
            val audioStreamsInfo =
                mediaInfo.audioStreams.map { it.toAudioStreamInfoEntity(medium.mediumEntity.uriString) }
            val subtitleStreamsInfo =
                mediaInfo.subtitleStreams.map { it.toSubtitleStreamInfoEntity(medium.mediumEntity.uriString) }
            val thumbnailPath =
                thumbnail?.saveTo(storageDir = context.thumbnailCacheDir, quality = 30)

            mediumDao.upsert(
                medium.mediumEntity.copy(
                    format = mediaInfo.format,
                    thumbnailPath = thumbnailPath,
                ),
            )
            videoStreamInfo?.let { mediumDao.upsertVideoStreamInfo(it) }
            audioStreamsInfo.onEach { mediumDao.upsertAudioStreamInfo(it) }
            subtitleStreamsInfo.onEach { mediumDao.upsertSubtitleStreamInfo(it) }
        }
    }

    init {
        applicationScope.launch { sync() }
    }

    companion object {
        private const val TAG = "MediaInfoSynchronizer"
    }
}

fun VideoStream.toVideoStreamInfoEntity(mediumUri: String) = VideoStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
    mediumUri = mediumUri,
)

fun AudioStream.toAudioStreamInfoEntity(mediumUri: String) = AudioStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout,
    mediumUri = mediumUri,
)

fun SubtitleStream.toSubtitleStreamInfoEntity(mediumUri: String) = SubtitleStreamInfoEntity(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    mediumUri = mediumUri,
)

suspend fun Bitmap.saveTo(storageDir: File, quality: Int = 100): String? =
    withContext(Dispatchers.IO) {
        val thumbnailFileName = "thumbnail-${System.currentTimeMillis()}"
        val thumbFile = File(storageDir, thumbnailFileName)
        try {
            FileOutputStream(thumbFile).use { fos ->
                compress(Bitmap.CompressFormat.JPEG, quality, fos)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext if (thumbFile.exists()) thumbFile.path else null
    }
