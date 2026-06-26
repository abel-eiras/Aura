package com.aura.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.app.R
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.ui.components.AuraOrb
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    onSettingsClick: () -> Unit,
    onRecordingsClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val recordingStatus by viewModel.recordingStatus.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                IconButton(onClick = onRecordingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.List,
                        contentDescription = stringResource(R.string.content_description_recordings)
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.content_description_settings)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val isActivelyRecording = recordingStatus is RecordingStatus.Recording

                AuraOrb(
                    isRecording = isActivelyRecording,
                    modifier = Modifier.clickable { viewModel.toggleRecording() }
                )

                StatusText(recordingStatus, accountState.isSignedIn)

                if (recordingStatus !is RecordingStatus.Idle) {
                    val isPaused = recordingStatus is RecordingStatus.Paused
                    IconButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                            contentDescription = stringResource(
                                if (isPaused) R.string.content_description_resume else R.string.content_description_pause
                            ),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusText(status: RecordingStatus, isSignedIn: Boolean) {
    val text = when {
        status is RecordingStatus.Recording -> {
            var elapsedSeconds by remember(status.startedAtMillis, status.accumulatedPausedMillis) {
                val elapsedMillis = System.currentTimeMillis() - status.startedAtMillis - status.accumulatedPausedMillis
                mutableLongStateOf(elapsedMillis / 1000)
            }
            LaunchedEffect(status.startedAtMillis, status.accumulatedPausedMillis) {
                while (true) {
                    elapsedSeconds = (System.currentTimeMillis() - status.startedAtMillis - status.accumulatedPausedMillis) / 1000
                    delay(1000)
                }
            }
            stringResource(R.string.status_recording, formatElapsed(elapsedSeconds))
        }
        status is RecordingStatus.Paused -> {
            val elapsedSeconds = (status.pausedAtMillis - status.startedAtMillis - status.accumulatedPausedMillis) / 1000
            stringResource(R.string.status_paused, formatElapsed(elapsedSeconds))
        }
        isSignedIn -> stringResource(R.string.status_idle_ready)
        else -> stringResource(R.string.status_not_authenticated)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 24.dp, start = 32.dp, end = 32.dp)
    )
}

private fun formatElapsed(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
