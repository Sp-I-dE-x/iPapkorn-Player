package com.daljeet.xplayer.core.datastore.datasource

import kotlinx.coroutines.flow.Flow

interface PreferencesDataSource<T> {

    val preferences: Flow<T>

    suspend fun update(transform: suspend (T) -> T)
}
