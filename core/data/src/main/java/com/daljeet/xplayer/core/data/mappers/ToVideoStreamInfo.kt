package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.database.entities.VideoStreamInfoEntity
import com.daljeet.xplayer.core.model.VideoStreamInfo

fun VideoStreamInfoEntity.toVideoStreamInfo() = VideoStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    frameRate = frameRate,
    frameWidth = frameWidth,
    frameHeight = frameHeight,
)
