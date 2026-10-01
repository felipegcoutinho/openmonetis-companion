package br.com.openmonetis.companion.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage using EncryptedSharedPreferences backed by Android Keystore.
 * Stores sensitive data like API tokens and server URL.
 */
@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var serverUrl: String?
        get() = prefs.getString(KEY_SERVER_URL, null)
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var accessToken: String?
        get() = prefs.getString(KEY_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var tokenId: String?
        get() = prefs.getString(KEY_TOKEN_ID, null)
        set(value) = prefs.edit().putString(KEY_TOKEN_ID, value).apply()

    var tokenName: String?
        get() = prefs.getString(KEY_TOKEN_NAME, null)
        set(value) = prefs.edit().putString(KEY_TOKEN_NAME, value).apply()

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var lastSyncTime: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var lastVerifiedTime: Long
        get() = prefs.getLong("last_verified_time", 0L)
        set(value) = prefs.edit().putLong("last_verified_time", value).apply()

    data class ConnectionState(val configured: Boolean, val lastSyncTime: Long, val lastVerifiedTime: Long)
    fun observeConnection(): kotlinx.coroutines.flow.Flow<ConnectionState> = kotlinx.coroutines.flow.callbackFlow {
        fun emit() { trySend(ConnectionState(isConfigured(), lastSyncTime, lastVerifiedTime)) }
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> emit() }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        emit()
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    var notifySyncSuccess: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_SYNC_SUCCESS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_SYNC_SUCCESS, value).apply()

    var notifySyncError: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_SYNC_ERROR, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_SYNC_ERROR, value).apply()

    fun isConfigured(): Boolean {
        return !serverUrl.isNullOrBlank() && !accessToken.isNullOrBlank()
    }

    fun hasServerUrl(): Boolean {
        return !serverUrl.isNullOrBlank()
    }

    fun saveCredentials(
        serverUrl: String,
        accessToken: String,
        tokenId: String?,
        tokenName: String?
    ) {
        prefs.edit().apply {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_ACCESS_TOKEN, accessToken)
            remove(KEY_REFRESH_TOKEN)
            putString(KEY_TOKEN_ID, tokenId)
            putString(KEY_TOKEN_NAME, tokenName)
            putLong("last_verified_time", System.currentTimeMillis())
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "openmonetis_secure_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_ID = "token_id"
        private const val KEY_TOKEN_NAME = "token_name"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_NOTIFY_SYNC_SUCCESS = "notify_sync_success"
        private const val KEY_NOTIFY_SYNC_ERROR = "notify_sync_error"
    }
}
