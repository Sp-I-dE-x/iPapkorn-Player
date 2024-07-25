package com.daljeet.xplayer.core.domain

import com.daljeet.xplayer.core.common.Dispatcher
import com.daljeet.xplayer.core.common.NextDispatchers
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.model.SortBy
import com.daljeet.xplayer.core.model.SortOrder
import com.daljeet.xplayer.core.model.Video
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Use case to get sorted videos from the repository.
 *
 * @property mediaRepository Repository to fetch media data.
 * @property preferencesRepository Repository to fetch user preferences.
 * @property defaultDispatcher Coroutine dispatcher for running the use case.
 */
class GetSortedVideosUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    /**
     * Fetches videos and sorts them based on user preferences.
     *
     * @param folderPath Optional folder path to filter videos.
     * @return A flow emitting a sorted list of videos.
     */
    operator fun invoke(folderPath: String? = null): Flow<List<Video>> {
        val videosFlow = folderPath?.let {
            mediaRepository.getVideosFlowFromFolderPath(it)
        } ?: mediaRepository.getVideosFlow()

        return combine(videosFlow, preferencesRepository.applicationPreferences) { videoItems, preferences ->
            val nonExcludedVideos = videoItems.filterNot {
                it.parentPath in preferences.excludeFolders
            }

            val sortedVideos = when (preferences.sortOrder) {
                SortOrder.ASCENDING -> sortVideos(nonExcludedVideos, preferences.sortBy, ascending = true)
                SortOrder.DESCENDING -> sortVideos(nonExcludedVideos, preferences.sortBy, ascending = false)
            }
            sortedVideos
        }.flowOn(defaultDispatcher)
    }

    /**
     * Sorts the list of videos based on the given criteria.
     *
     * @param videos List of videos to be sorted.
     * @param sortBy Criteria to sort the videos.
     * @param ascending Whether to sort in ascending order.
     * @return A sorted list of videos.
     */
    private fun sortVideos(videos: List<Video>, sortBy: SortBy, ascending: Boolean): List<Video> {
        return when (sortBy) {
            SortBy.TITLE -> videos.sortedWith(compareBy { it.displayName.lowercase() })
            SortBy.LENGTH -> videos.sortedWith(compareBy { it.duration })
            SortBy.PATH -> videos.sortedWith(compareBy { it.path.lowercase() })
            SortBy.SIZE -> videos.sortedWith(compareBy { it.size })
            SortBy.DATE -> videos.sortedWith(compareBy { it.dateModified })
        }.let { if (ascending) it else it.reversed() }
    }
}
