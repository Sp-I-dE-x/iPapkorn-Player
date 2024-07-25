package com.daljeet.xplayer.ui

import android.annotation.SuppressLint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.daljeet.xplayer.R
import com.daljeet.xplayer.core.ui.components.CancelButton
import com.daljeet.xplayer.core.ui.components.DoneButton
import com.daljeet.xplayer.core.ui.components.NextCenterAlignedTopAppBar
import com.daljeet.xplayer.core.ui.components.NextDialog
import com.daljeet.xplayer.core.ui.designsystem.NextIcons
import com.daljeet.xplayer.navigation.MEDIA_ROUTE
import com.daljeet.xplayer.navigation.mediaNavGraph
import com.daljeet.xplayer.navigation.settingsNavGraph
import com.daljeet.xplayer.navigation.startPlayerActivity
import com.daljeet.xplayer.settings.navigation.navigateToSettings
import com.daljeet.xplayer.settings.screens.medialibrary.MediaLibraryPreferencesViewModel

const val MAIN_ROUTE = "main_screen_route"

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun MainScreen(
    permissionState: PermissionState,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showUrlDialog by rememberSaveable { mutableStateOf(false) }
    val selectVideoFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { it?.let(context::startPlayerActivity) }
    )
    val viewModel: MediaLibraryPreferencesViewModel = hiltViewModel()

    Scaffold(
        topBar = {
            NextCenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateToSettings) {
                        Icon(
                            imageVector = NextIcons.Settings,
                            contentDescription = stringResource(id = R.string.settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showUrlDialog = true }) {
                Icon(
                    imageVector = NextIcons.Add,
                    contentDescription = stringResource(id = R.string.add)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .consumeWindowInsets(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        ) {
            if (permissionState.status.isGranted) {
                NavHost(
                    navController = navController,
                    startDestination = MEDIA_ROUTE
                ) {
                    mediaNavGraph(
                        context = context,
                        mainNavController = navController,
                        mediaNavController = navController
                    )
                    settingsNavGraph(navController = navController)
                }
            } else {
                if (permissionState.status.shouldShowRationale) {
                    PermissionRationaleDialog(
                        text = stringResource(
                            id = R.string.permission_info,
                            permissionState.permission
                        ),
                        onConfirmButtonClick = permissionState::launchPermissionRequest
                    )
                } else {
                    PermissionDetailView(
                        text = stringResource(
                            id = R.string.permission_settings,
                            permissionState.permission
                        )
                    )
                }
            }
        }
        if (showUrlDialog) {
            NetworkUrlDialog(
                onDismiss = { showUrlDialog = false },
                onDone = { context.startPlayerActivity(Uri.parse(it)) }
            )
        }
    }
}

@Composable
fun NetworkUrlDialog(
    onDismiss: () -> Unit,
    onDone: (String) -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }
    NextDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.network_stream)) },
        content = {
            Text(text = stringResource(R.string.enter_a_network_url))
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(text = stringResource(R.string.example_url)) }
            )
        },
        confirmButton = { DoneButton(onClick = { if (url.isBlank()) onDismiss() else onDone(url) }) },
        dismissButton = { CancelButton(onClick = onDismiss) }
    )
}
