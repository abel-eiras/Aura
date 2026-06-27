package com.aura.app.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches device call state so recording can auto-pause for incoming/active calls.
 * No-ops entirely if READ_PHONE_STATE hasn't been granted; call detection is best-effort,
 * not required for the app's core recording function.
 */
@Singleton
class CallStateMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private var legacyListener: PhoneStateListener? = null
    private var modernCallback: TelephonyCallback? = null

    fun start(onCallActive: () -> Unit, onCallEnded: () -> Unit) {
        if (telephonyManager == null) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = dispatch(state, onCallActive, onCallEnded)
            }
            modernCallback = callback
            telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) =
                    dispatch(state, onCallActive, onCallEnded)
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun stop() {
        modernCallback?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        modernCallback = null
        legacyListener?.let {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(it, PhoneStateListener.LISTEN_CALL_STATE_NONE)
        }
        legacyListener = null
    }

    private fun dispatch(state: Int, onCallActive: () -> Unit, onCallEnded: () -> Unit) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> onCallActive()
            TelephonyManager.CALL_STATE_IDLE -> onCallEnded()
        }
    }
}
