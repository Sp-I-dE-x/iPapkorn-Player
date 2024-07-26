package com.daljeet.xplayer.core.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.daljeet.xplayer.core.data.repository.LocalMediaRepository
import com.daljeet.xplayer.core.data.repository.LocalPreferencesRepository
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.data.repository.PreferencesRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsMediaRepository(
        videoRepository: LocalMediaRepository
    ): MediaRepository

    @Binds
    fun bindsPreferencesRepository(
        preferencesRepository: LocalPreferencesRepository,
    ): PreferencesRepository
}
