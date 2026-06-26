package com.aura.app.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aura.app.R
import com.aura.app.data.upload.UploadQueueRepository
import com.aura.app.domain.model.RecordingStatus
import com.aura.app.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var audioRecorderEngine: AudioRecorderEngine
    @Inject lateinit var recordingStateHolder: RecordingStateHolder
    @Inject lateinit var uploadQueueRepository: UploadQueueRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        startForegroundWithNotification(isPaused = false)
        audioRecorderEngine.start(uploadQueueRepository.recordingsDir())
        recordingStateHolder.setRecording(System.currentTimeMillis())
    }

    private fun pauseRecording() {
        if (recordingStateHolder.status.value !is RecordingStatus.Recording) return
        audioRecorderEngine.pause()
        recordingStateHolder.setPaused()
        updateNotification(isPaused = true)
    }

    private fun resumeRecording() {
        if (recordingStateHolder.status.value !is RecordingStatus.Paused) return
        audioRecorderEngine.resume()
        recordingStateHolder.setResumed()
        updateNotification(isPaused = false)
    }

    private fun stopRecording() {
        val file = audioRecorderEngine.stop()
        recordingStateHolder.setIdle()
        file?.let { uploadQueueRepository.enqueueUpload(it) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundWithNotification(isPaused: Boolean) {
        val notification = buildNotification(isPaused)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Constants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(Constants.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(isPaused: Boolean) {
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
        manager?.notify(Constants.NOTIFICATION_ID, buildNotification(isPaused))
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val pauseResumeAction = if (isPaused) {
            NotificationCompat.Action(
                R.drawable.ic_aura_mono,
                getString(R.string.notification_action_resume),
                pendingIntentFor(ACTION_RESUME)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_aura_mono,
                getString(R.string.notification_action_pause),
                pendingIntentFor(ACTION_PAUSE)
            )
        }
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_aura_mono,
            getString(R.string.notification_action_stop),
            pendingIntentFor(ACTION_STOP)
        )
        val contentText = if (isPaused) {
            getString(R.string.notification_text_paused)
        } else {
            getString(R.string.notification_text_recording)
        }

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_aura_mono)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun pendingIntentFor(action: String): PendingIntent {
        val intent = Intent(this, RecordingService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.aura.app.action.START_RECORDING"
        const val ACTION_PAUSE = "com.aura.app.action.PAUSE_RECORDING"
        const val ACTION_RESUME = "com.aura.app.action.RESUME_RECORDING"
        const val ACTION_STOP = "com.aura.app.action.STOP_RECORDING"

        fun start(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_START)
            ContextCompat.startForegroundService(context, intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_PAUSE)
            ContextCompat.startForegroundService(context, intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_RESUME)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordingService::class.java).setAction(ACTION_STOP)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
