package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.database.entities.AudioStreamInfoEntity
import com.daljeet.xplayer.core.database.entities.SubtitleStreamInfoEntity
import com.daljeet.xplayer.core.database.relations.MediumWithInfo
import com.daljeet.xplayer.core.model.Video

fun MediumWithInfo.toVideo() = Video(
    id = mediumEntity.mediaStoreId,
    path = mediumEntity.path,
    parentPath = mediumEntity.parentPath,
    duration = mediumEntity.duration,
    uriString = mediumEntity.uriString,
    displayName = mediumEntity.name.substringBeforeLast("."),
    nameWithExtension = mediumEntity.name,
    width = mediumEntity.width,
    height = mediumEntity.height,
    size = mediumEntity.size,
    dateModified = mediumEntity.modified,
    format = mediumEntity.format,
    thumbnailPath = mediumEntity.thumbnailPath,
    formattedDuration = Utils.formatDurationMillis(mediumEntity.duration),
    formattedFileSize = Utils.formatFileSize(mediumEntity.size),
    videoStream = videoStreamInfo?.toVideoStreamInfo(),
    audioStreams = audioStreamsInfo.map(AudioStreamInfoEntity::toAudioStreamInfo),
    subtitleStreams = subtitleStreamsInfo.map(SubtitleStreamInfoEntity::toSubtitleStreamInfo)
)
