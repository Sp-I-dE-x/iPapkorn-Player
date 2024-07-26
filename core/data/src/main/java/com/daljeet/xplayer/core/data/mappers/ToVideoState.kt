package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.data.models.VideoState
import con.daljeet.xplayer.core.database.converter.UriListConverter
import com.daljeet.xplayer.core.database.entities.MediumEntity

fun MediumEntity.toVideoState(): VideoState {
    return VideoState(
        path = path,
        position = playbackPosition,
        audioTrackIndex = audioTrackIndex,
        subtitleTrackIndex = subtitleTrackIndex,
        playbackSpeed = playbackSpeed,
        externalSubs = UriListConverter.fromStringToList(externalSubs),
        videoScale = videoScale,
    )
}
