package com.aura.app.ui.settings

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IconButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.app.R
import com.aura.app.domain.model.AudioQuality
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val queueStatus by viewModel.queueStatus.collectAsStateWithLifecycle()
    val wifiOnlyUpload by viewModel.wifiOnlyUpload.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val signInError by viewModel.signInError.collectAsStateWithLifecycle()

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.onSignInResult(result.data)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(24.dp)) {

            SettingsSection(stringResource(R.string.settings_account_section))
            if (accountState.isSignedIn) {
                Text(
                    text = stringResource(R.string.settings_connected_as, accountState.email.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_disconnect))
                }
            } else {
                Button(
                    onClick = { signInLauncher.launch(viewModel.signInIntent()) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_connect_google))
                }
                signInError?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_sign_in_error, error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(stringResource(R.string.settings_upload_queue_section))
            Text(
                text = if (queueStatus.pendingCount > 0) {
                    stringResource(
                        R.string.settings_upload_queue_status,
                        queueStatus.pendingCount,
                        formatBytes(queueStatus.totalBytes)
                    )
                } else {
                    stringResource(R.string.settings_upload_queue_empty)
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(stringResource(R.string.settings_uploads_section))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_wifi_only_upload), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(R.string.settings_wifi_only_upload_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Switch(checked = wifiOnlyUpload, onCheckedChange = { viewModel.setWifiOnlyUpload(it) })
            }

            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(stringResource(R.string.settings_audio_quality_section))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                AudioQuality.entries.forEachIndexed { index, quality ->
                    SegmentedButton(
                        selected = audioQuality == quality,
                        onClick = { viewModel.setAudioQuality(quality) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = AudioQuality.entries.size)
                    ) {
                        Text(stringResource(quality.labelRes()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            SettingsSection(stringResource(R.string.settings_about_section))
            Text(
                text = stringResource(R.string.settings_version, viewModel.versionName),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

private fun AudioQuality.labelRes(): Int = when (this) {
    AudioQuality.LOW -> R.string.settings_audio_quality_low
    AudioQuality.NORMAL -> R.string.settings_audio_quality_normal
    AudioQuality.HIGH -> R.string.settings_audio_quality_high
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}
