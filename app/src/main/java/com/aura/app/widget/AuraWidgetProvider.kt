package com.aura.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.aura.app.R
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.recording.RecordingService
import com.aura.app.recording.RecordingStateHolder
import com.aura.app.util.IconRotation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Home screen widget: just the Aura logo, deliberately bare - static while idle/paused, spinning
 * while actively recording, no text. Tapping it toggles recording, same as the Quick Settings tile.
 * Spin frames are pushed by [RecordingService] while it runs; this provider only renders the
 * current/static state on placement, reboot, or other system-triggered updates.
 */
@AndroidEntryPoint
class AuraWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var recordingStateHolder: RecordingStateHolder

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE) {
            when (recordingStateHolder.status.value) {
                is RecordingStatus.Idle -> RecordingService.start(context)
                else -> RecordingService.stop(context)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isSpinning = recordingStateHolder.status.value is RecordingStatus.Recording
        if (isSpinning) renderSpinning(context, angleDegrees = 0f) else renderStatic(context)
    }

    companion object {
        private const val ACTION_TOGGLE = "com.aura.app.action.WIDGET_TOGGLE"
        private const val ICON_SIZE_PX = 96

        fun renderStatic(context: Context) {
            val views = baseViews(context)
            views.setImageViewResource(R.id.widget_image, R.drawable.ic_aura_mono)
            pushUpdate(context, views)
        }

        fun renderSpinning(context: Context, angleDegrees: Float) {
            val views = baseViews(context)
            val bitmap = IconRotation.rotatedBitmap(
                context,
                R.drawable.ic_aura_mono,
                angleDegrees,
                ICON_SIZE_PX,
                Color.parseColor("#6E5BFF")
            )
            views.setImageViewBitmap(R.id.widget_image, bitmap)
            pushUpdate(context, views)
        }

        private fun baseViews(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_aura)
            val toggleIntent = Intent(context, AuraWidgetProvider::class.java).setAction(ACTION_TOGGLE)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)
            return views
        }

        private fun pushUpdate(context: Context, views: RemoteViews) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AuraWidgetProvider::class.java))
            if (ids.isEmpty()) return
            manager.updateAppWidget(ids, views)
        }
    }
}
