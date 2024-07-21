package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.database.entities.SubtitleStreamInfoEntity
import com.daljeet.xplayer.core.model.SubtitleStreamInfo

fun SubtitleStreamInfoEntity.toSubtitleStreamInfo() = SubtitleStreamInfo(
    index = index,
    title = title,
    codecName = codecName,
    language = language,
    disposition = disposition
)
