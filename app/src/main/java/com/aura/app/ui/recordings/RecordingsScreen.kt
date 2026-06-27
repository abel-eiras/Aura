package com.aura.app.ui.recordings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.app.R
import com.aura.app.domain.model.LocalRecording
import com.aura.app.domain.model.UploadState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

private val RECORDINGS_REFRESH_INTERVAL_MILLIS = 3000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    onBackClick: () -> Unit,
    viewModel: RecordingsViewModel = hiltViewModel()
) {
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val playingFilePath by viewModel.playingFilePath.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<LocalRecording?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(RECORDINGS_REFRESH_INTERVAL_MILLIS)
            viewModel.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recordings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (recordings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.recordings_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(recordings, key = { it.fileName }) { recording ->
                    RecordingRow(
                        recording = recording,
                        isPlaying = playingFilePath == recording.filePath,
                        onPlayToggle = { recording.filePath?.let { viewModel.togglePlayback(it) } },
                        onRetry = { recording.filePath?.let { viewModel.retry(it) } },
                        onDeleteClick = { pendingDelete = recording }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { recording ->
        val isUploaded = recording.uploadState == UploadState.UPLOADED
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.recordings_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (isUploaded) {
                            R.string.recordings_delete_confirm_body_uploaded
                        } else {
                            R.string.recordings_delete_confirm_body
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(recording)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.recordings_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.recordings_delete_cancel))
                }
            }
        )
    }
}

@Composable
private fun RecordingRow(
    recording: LocalRecording,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onRetry: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (recording.filePath != null) {
            IconButton(onClick = onPlayToggle) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.content_description_stop_playback else R.string.content_description_play_recording
                    )
                )
            }
        } else {
            IconButton(onClick = {}, enabled = false) {
                Icon(
                    imageVector = Icons.Outlined.CloudDone,
                    contentDescription = stringResource(R.string.content_description_uploaded)
                )
            }
        }

        Column(modifier = Modifier.weight(1f).padding(start = 8.dp, top = 12.dp)) {
            Text(text = formatTimestamp(recording.createdAtMillis), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${formatBytes(recording.sizeBytes)} · ${statusLabel(recording.uploadState)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        if (recording.uploadState == UploadState.FAILED) {
            TextButton(onClick = onRetry, modifier = Modifier.padding(top = 4.dp)) {
                Text(stringResource(R.string.recordings_retry))
            }
        }

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.content_description_delete_recording)
            )
        }
    }
}

@Composable
private fun statusLabel(state: UploadState): String = when (state) {
    UploadState.QUEUED -> stringResource(R.string.recordings_status_pending)
    UploadState.UPLOADED -> stringResource(R.string.recordings_status_uploaded)
    UploadState.FAILED -> stringResource(R.string.recordings_status_failed)
}

private fun formatTimestamp(millis: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
