package com.daljeet.xplayer.core.data.mappers

import com.daljeet.xplayer.core.common.Utils
import com.daljeet.xplayer.core.database.entities.DirectoryEntity
import com.daljeet.xplayer.core.model.Folder

fun DirectoryEntity.toFolder() = Folder(
    name = name,
    path = path,
    mediaSize = size,
    mediaCount = mediaCount,
    dateModified = modified,
    formattedMediaSize = Utils.formatFileSize(size)
)
