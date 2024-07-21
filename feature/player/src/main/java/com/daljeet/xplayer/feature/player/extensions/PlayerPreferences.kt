package com.daljeet.xplayer.feature.player.extensions

import com.daljeet.xplayer.core.model.FastSeek
import com.daljeet.xplayer.core.model.PlayerPreferences

fun PlayerPreferences.shouldFastSeek(duration: Long): Boolean {
    return when (fastSeek) {
        FastSeek.ENABLE -> true
        FastSeek.DISABLE -> false
        FastSeek.AUTO -> duration >= minDurationForFastSeek
    }
}
