package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.database.entities.AudioStreamInfoEntity
import com.daljeet.xplayer.core.model.AudioStreamInfo

fun AudioStreamInfoEntity.toAudioStreamInfo() = AudioStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition,
    bitRate = bitRate,
    sampleFormat = sampleFormat,
    sampleRate = sampleRate,
    channels = channels,
    channelLayout = channelLayout
)
