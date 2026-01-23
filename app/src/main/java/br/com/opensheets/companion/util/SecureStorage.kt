package br.com.opensheets.companion.util

import android.content.Context
import android.content.SharedPreferences
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

    var refreshToken: String?
        get() = prefs.getString(KEY_REFRESH_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_REFRESH_TOKEN, value).apply()

    var tokenId: String?
        get() = prefs.getString(KEY_TOKEN_ID, null)
        set(value) = prefs.edit().putString(KEY_TOKEN_ID, value).apply()

    var tokenName: String?
        get() = prefs.getString(KEY_TOKEN_NAME, null)
        set(value) = prefs.edit().putString(KEY_TOKEN_NAME, value).apply()

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    fun isConfigured(): Boolean {
        return !serverUrl.isNullOrBlank() && !accessToken.isNullOrBlank()
    }

    fun hasServerUrl(): Boolean {
        return !serverUrl.isNullOrBlank()
    }

    fun saveCredentials(
        serverUrl: String,
        accessToken: String,
        refreshToken: String?,
        tokenId: String?,
        tokenName: String?
    ) {
        prefs.edit().apply {
            putString(KEY_SERVER_URL, serverUrl)
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_TOKEN_ID, tokenId)
            putString(KEY_TOKEN_NAME, tokenName)
            apply()
        }
    }

    fun updateTokens(accessToken: String, refreshToken: String? = null) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (refreshToken != null) {
                putString(KEY_REFRESH_TOKEN, refreshToken)
            }
            apply()
        }
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "opensheets_secure_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_ID = "token_id"
        private const val KEY_TOKEN_NAME = "token_name"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
