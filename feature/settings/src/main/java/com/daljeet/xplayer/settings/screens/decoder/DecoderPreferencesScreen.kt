package com.daljeet.xplayer.settings.screens.decoder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daljeet.xplayer.core.model.DecoderPriority
import com.daljeet.xplayer.core.ui.R
import com.daljeet.xplayer.core.ui.components.ClickablePreferenceItem
import com.daljeet.xplayer.core.ui.components.NextTopAppBar
import com.daljeet.xplayer.core.ui.components.RadioTextButton
import com.daljeet.xplayer.core.ui.designsystem.NextIcons
import com.daljeet.xplayer.settings.composables.OptionsDialog
import com.daljeet.xplayer.settings.composables.PreferenceSubtitle
import com.daljeet.xplayer.settings.extensions.name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecoderPreferencesScreen(
    onNavigateUp: () -> Unit,
    viewModel: DecoderPreferencesViewModel = hiltViewModel()
) {
    val preferences by viewModel.preferencesFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehaviour.nestedScrollConnection),
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.decoder),
                scrollBehavior = scrollBehaviour,
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(state = rememberScrollState())
        ) {
            PreferenceSubtitle(text = stringResource(id = R.string.playback))
            DecoderPrioritySetting(
                currentValue = preferences.decoderPriority,
                onClick = { viewModel.showDialog(DecoderPreferenceDialog.DecoderPriorityDialog) }
            )
        }

        uiState.showDialog?.let { showDialog ->
            when (showDialog) {
                DecoderPreferenceDialog.DecoderPriorityDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.decoder_priority),
                        onDismissClick = viewModel::hideDialog
                    ) {
                        items(DecoderPriority.entries.toTypedArray()) {
                            RadioTextButton(
                                text = it.name(),
                                selected = it == preferences.decoderPriority,
                                onClick = {
                                    viewModel.updateDecoderPriority(it)
                                    viewModel.hideDialog()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DecoderPrioritySetting(
    currentValue: DecoderPriority,
    onClick: () -> Unit
) {
    ClickablePreferenceItem(
        title = stringResource(R.string.decoder_priority),
        description = currentValue.name(),
        icon = NextIcons.Priority,
        onClick = onClick
    )
}
