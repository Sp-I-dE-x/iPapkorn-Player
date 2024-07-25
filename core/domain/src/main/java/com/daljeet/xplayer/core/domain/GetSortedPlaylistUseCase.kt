package com.daljeet.xplayer.core.domain

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.daljeet.xplayer.core.common.Dispatcher
import com.daljeet.xplayer.core.common.NextDispatchers
import com.daljeet.xplayer.core.common.extensions.getPath
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class GetSortedPlaylistUseCase @Inject constructor(
    private val getSortedVideosUseCase: GetSortedVideosUseCase,
    @ApplicationContext private val context: Context,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(uri: Uri): List<Uri> = withContext(defaultDispatcher) {
        val path = context.getPath(uri) ?: return@withContext emptyList()
        val parent = File(path).parent

        getSortedVideosUseCase.invoke(parent).first().map { Uri.parse(it.uriString) }
    }
}
