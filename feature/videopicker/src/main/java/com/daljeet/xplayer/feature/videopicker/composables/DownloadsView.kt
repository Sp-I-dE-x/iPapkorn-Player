package com.daljeet.xplayer.feature.videopicker.composables

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.daljeet.xplayer.core.model.ApplicationPreferences
import com.daljeet.xplayer.core.model.Video
import com.daljeet.xplayer.core.ui.R
import com.daljeet.xplayer.core.ui.designsystem.NextIcons
import com.daljeet.xplayer.feature.videopicker.screens.DownloadsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DownloadsView(
    videosState: DownloadsState,
    preferences: ApplicationPreferences,
    onVideoClick: ((Uri) -> Unit)?,
    onDeleteVideoClick: (String) -> Unit,
    onVideoLoaded: (Uri) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var showMediaActionsFor: Video? by rememberSaveable { mutableStateOf(null) }
    var deleteAction: Video? by rememberSaveable { mutableStateOf(null) }
    var showInfoAction: Video? by rememberSaveable { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    when (videosState) {
        DownloadsState.Loading -> CenterCircularProgressBar()
        is DownloadsState.Success -> if (videosState.data.isEmpty()) {
            NoVideosFound()
        } else {
            MediaLazyList {
                items(videosState.data, key = { it.path }) { video ->
                    LaunchedEffect(Unit) {
                        onVideoLoaded(Uri.parse(video.uriString))
                    }
                    VideoItem(
                        video = video,
                        preferences = preferences,
                        modifier = Modifier.combinedClickable(
                            onClick = { onVideoClick?.invoke(Uri.parse(video.uriString)) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMediaActionsFor = video
                            }
                        )
                    )
                }
            }
        }
    }

    showMediaActionsFor?.let {
        OptionsBottomSheet(
            title = it.displayName,
            onDismiss = { showMediaActionsFor = null }
        ) {
            BottomSheetItem(
                text = stringResource(R.string.delete),
                icon = NextIcons.Delete,
                onClick = {
                    deleteAction = it
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) showMediaActionsFor = null
                    }
                }
            )
            BottomSheetItem(
                text = stringResource(R.string.share),
                icon = NextIcons.Share,
                onClick = {
                    val mediaStoreUri = Uri.parse(it.uriString)
                    val intent = Intent.createChooser(
                        Intent().apply {
                            type = "video/*"
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, mediaStoreUri)
                        },
                        null
                    )
                    context.startActivity(intent)
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) showMediaActionsFor = null
                    }
                }
            )
            BottomSheetItem(
                text = stringResource(R.string.properties),
                icon = NextIcons.Info,
                onClick = {
                    showInfoAction = it
                    scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                        if (!bottomSheetState.isVisible) showMediaActionsFor = null
                    }
                }
            )
        }
    }

    deleteAction?.let {
        DeleteConfirmationDialog(
            subText = stringResource(id = R.string.delete_file),
            onCancel = { deleteAction = null },
            onConfirm = {
                onDeleteVideoClick(it.uriString)
                deleteAction = null
            },
            fileNames = listOf(it.nameWithExtension)
        )
    }

    showInfoAction?.let {
        ShowVideoInfoDialog(
            video = it,
            onDismiss = { showInfoAction = null }
        )
    }
}


