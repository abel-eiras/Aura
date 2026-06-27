package com.aura.app.qstile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.graphics.drawable.Icon
import com.aura.app.R
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.recording.RecordingService
import com.aura.app.recording.RecordingStateHolder
import com.aura.app.util.IconRotation
import com.aura.app.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuraTileService : TileService() {

    @Inject lateinit var recordingStateHolder: RecordingStateHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectJob: Job? = null
    private var rotationJob: Job? = null
    private var rotationAngle = 0f

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onStartListening() {
        super.onStartListening()
        collectJob = scope.launch {
            recordingStateHolder.status.collect { status ->
                renderTile(status)
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        collectJob?.cancel()
        rotationJob?.cancel()
    }

    override fun onClick() {
        super.onClick()
        when (recordingStateHolder.status.value) {
            is RecordingStatus.Idle -> RecordingService.start(applicationContext)
            is RecordingStatus.Recording, is RecordingStatus.Paused -> RecordingService.stop(applicationContext)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun renderTile(status: RecordingStatus) {
        val tile = qsTile ?: return
        rotationJob?.cancel()

        when (status) {
            is RecordingStatus.Idle -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_label_idle)
                setSubtitleIfSupported(tile, getString(R.string.tile_subtitle_idle))
                tile.icon = Icon.createWithResource(this, R.drawable.ic_aura_mono)
                tile.updateTile()
            }
            is RecordingStatus.Recording -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_recording)
                setSubtitleIfSupported(tile, getString(R.string.tile_subtitle_recording))
                rotationJob = scope.launch {
                    while (isActive) {
                        tile.icon = Icon.createWithBitmap(rotatedIconBitmap(rotationAngle))
                        tile.updateTile()
                        rotationAngle = (rotationAngle + 18f) % 360f
                        delay(120)
                    }
                }
            }
            is RecordingStatus.Paused -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_label_paused)
                setSubtitleIfSupported(tile, getString(R.string.tile_subtitle_paused))
                tile.icon = Icon.createWithResource(this, R.drawable.ic_aura_mono)
                tile.updateTile()
            }
        }
    }

    private fun setSubtitleIfSupported(tile: Tile, subtitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = subtitle
        }
    }

    private fun rotatedIconBitmap(angleDegrees: Float): Bitmap =
        IconRotation.rotatedBitmap(this, R.drawable.ic_aura_mono, angleDegrees, ICON_SIZE_PX, Color.parseColor("#6E5BFF"))

    private companion object {
        const val ICON_SIZE_PX = 96
    }
}
