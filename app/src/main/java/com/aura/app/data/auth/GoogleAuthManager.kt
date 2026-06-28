package com.aura.app.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.aura.app.data.prefs.SecurePrefs
import com.aura.app.util.Constants
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Wraps Google Sign-In for the drive.file scope. Access tokens are fetched
 * fresh from Play Services on demand (it maintains its own refresh cache);
 * we additionally cache the last-known token in SecurePrefs purely so the
 * upload worker has something to try before falling back to a blocking
 * token fetch.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePrefs: SecurePrefs
) {
    private val signInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(Constants.DRIVE_SCOPE))
        .build()

    val client: GoogleSignInClient by lazy { GoogleSignIn.getClient(context, signInOptions) }

    /** Set by [handleSignInResult] when sign-in fails, so the caller can surface a reason. */
    var lastSignInErrorMessage: String? = null
        private set

    private val _needsReauth = MutableStateFlow(false)
    val needsReauth: StateFlow<Boolean> = _needsReauth

    @Volatile
    private var pendingRecoveryIntent: Intent? = null

    fun signInIntent(): Intent = client.signInIntent

    fun lastSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        return try {
            task.getResult(ApiException::class.java).also { account ->
                lastSignInErrorMessage = null
                securePrefs.accountEmail = account.email
                _needsReauth.value = false
                pendingRecoveryIntent = null
            }
        } catch (e: ApiException) {
            val reason = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
            Log.w(TAG, "Google sign-in failed: code=${e.statusCode} ($reason)", e)
            lastSignInErrorMessage = "$reason (${e.statusCode})"
            null
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching { client.revokeAccess() }
        runCatching { client.signOut() }
        securePrefs.clear()
        _needsReauth.value = false
        pendingRecoveryIntent = null
    }

    /**
     * Returns the recovery [Intent] saved from the last [UserRecoverableAuthException], if any,
     * and clears it. Launching this intent lets the user re-grant consent without a full sign-out.
     */
    fun consumeRecoveryIntent(): Intent? = pendingRecoveryIntent.also { pendingRecoveryIntent = null }

    /** Blocking Play Services call; must run off the main thread. */
    suspend fun fetchFreshAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = lastSignedInAccount() ?: return@withContext null
        try {
            val token = GoogleAuthUtil.getToken(context, account.account!!, "oauth2:${Constants.DRIVE_SCOPE}")
            securePrefs.cachedAccessToken = token
            _needsReauth.value = false
            token
        } catch (e: UserRecoverableAuthException) {
            pendingRecoveryIntent = e.intent
            _needsReauth.value = true
            null
        } catch (e: Exception) {
            null
        }
    }

    fun invalidateToken(token: String) {
        runCatching { GoogleAuthUtil.clearToken(context, token) }
    }

    private companion object {
        const val TAG = "GoogleAuthManager"
    }
}
