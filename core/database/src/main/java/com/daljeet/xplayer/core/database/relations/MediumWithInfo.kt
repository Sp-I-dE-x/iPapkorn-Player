package com.daljeet.xplayer.core.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.daljeet.xplayer.core.database.entities.AudioStreamInfoEntity
import com.daljeet.xplayer.core.database.entities.MediumEntity
import com.daljeet.xplayer.core.database.entities.SubtitleStreamInfoEntity
import com.daljeet.xplayer.core.database.entities.VideoStreamInfoEntity

data class MediumWithInfo(
    @Embedded val mediumEntity: MediumEntity,
    @Relation(
        parentColumn = "path",
        entityColumn = "medium_path"
    )
    val videoStreamInfo: VideoStreamInfoEntity?,
    @Relation(
        parentColumn = "path",
        entityColumn = "medium_path"
    )
    val audioStreamsInfo: List<AudioStreamInfoEntity>,
    @Relation(
        parentColumn = "path",
        entityColumn = "medium_path"
    )
    val subtitleStreamsInfo: List<SubtitleStreamInfoEntity>
)
