package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.database.entities.DirectoryEntity
import com.daljeet.xplayer.core.model.Folder

fun DirectoryWithMedia.toFolder() = Folder(
    name = directory.name,
    path = directory.path,
    mediaSize = media.sumOf { it.size },
    mediaCount = media.size,
    dateModified = directory.modified,
    formattedMediaSize = Utils.formatFileSize(media.sumOf { it.size }),
    mediaList = media.map { it.toVideo() },
)
