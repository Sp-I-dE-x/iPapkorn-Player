package com.daljeet.xplayer.core.domain

import com.daljeet.xplayer.core.common.Dispatcher
import com.daljeet.xplayer.core.common.NextDispatchers
import com.daljeet.xplayer.core.data.repository.MediaRepository
import com.daljeet.xplayer.core.data.repository.PreferencesRepository
import com.daljeet.xplayer.core.model.Folder
import com.daljeet.xplayer.core.model.SortBy
import com.daljeet.xplayer.core.model.SortOrder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Use case to get sorted folders from the repository.
 *
 * @property mediaRepository Repository to fetch media data.
 * @property preferencesRepository Repository to fetch user preferences.
 * @property defaultDispatcher Coroutine dispatcher for running the use case.
 */
class GetSortedFoldersUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val preferencesRepository: PreferencesRepository,
    @Dispatcher(NextDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) {

    /**
     * Fetches folders and sorts them based on user preferences.
     *
     * @return A flow emitting a sorted list of folders.
     */
    operator fun invoke(): Flow<List<Folder>> {
        return combine(
            mediaRepository.getFoldersFlow(),
            preferencesRepository.applicationPreferences
        ) { folders, preferences ->

            val nonExcludedDirectories = folders.filterNot {
                it.path in preferences.excludeFolders
            }

            val sortedFolders = when (preferences.sortOrder) {
                SortOrder.ASCENDING -> sortFolders(nonExcludedDirectories, preferences.sortBy, ascending = true)
                SortOrder.DESCENDING -> sortFolders(nonExcludedDirectories, preferences.sortBy, ascending = false)
            }
            sortedFolders
        }.flowOn(defaultDispatcher)
    }

    /**
     * Sorts the list of folders based on the given criteria.
     *
     * @param folders List of folders to be sorted.
     * @param sortBy Criteria to sort the folders.
     * @param ascending Whether to sort in ascending order.
     * @return A sorted list of folders.
     */
    private fun sortFolders(folders: List<Folder>, sortBy: SortBy, ascending: Boolean): List<Folder> {
        return when (sortBy) {
            SortBy.TITLE -> folders.sortedWith(compareBy { it.name.lowercase() })
            SortBy.LENGTH -> folders.sortedWith(compareBy { it.mediaCount })
            SortBy.PATH -> folders.sortedWith(compareBy { it.path.lowercase() })
            SortBy.SIZE -> folders.sortedWith(compareBy { it.mediaSize })
            SortBy.DATE -> folders.sortedWith(compareBy { it.dateModified })
        }.let { if (ascending) it else it.reversed() }
    }
}
