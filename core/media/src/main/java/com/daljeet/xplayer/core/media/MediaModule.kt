package com.daljeet.xplayer.core.media

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.daljeet.xplayer.core.media.sync.LocalMediaInfoSynchronizer
import com.daljeet.xplayer.core.media.sync.LocalMediaSynchronizer
import com.daljeet.xplayer.core.media.sync.MediaInfoSynchronizer
import com.daljeet.xplayer.core.media.sync.MediaSynchronizer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface MediaModule {

    @Binds
    @Singleton
    fun bindsMediaSynchronizer(
        mediaSynchronizer: LocalMediaSynchronizer,
    ): MediaSynchronizer

    @Binds
    @Singleton
    fun bindsMediaInfoSynchronizer(
        mediaInfoSynchronizer: LocalMediaInfoSynchronizer,
    ): MediaInfoSynchronizer

    @Binds
    @Singleton
    fun bindMediaService(
