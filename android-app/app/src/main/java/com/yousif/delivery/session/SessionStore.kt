package com.yousif.delivery.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yousif.delivery.BuildConfig

/**
 * Encrypted session storage: the verified password, the backend URL and the
 * "stay logged in" flag survive app restarts (but not a reinstall, which is
 * exactly what we want — a reinstall must ask for the password again).
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (error: Throwable) {
        // Some OEM ROMs break the keystore; fall back so the app stays usable.
        context.getSharedPreferences("${FILE_NAME}_fallback", Context.MODE_PRIVATE)
    }

    init {
        // v1.2.0 shipped with "follow the system", which turned the phone dark-mode
        // into the night palette by surprise. Start everybody on the daylight theme
        // once; the choice in Settings is respected from then on.
        if (!prefs.getBoolean(KEY_THEME_SEEDED, false)) {
            prefs.edit()
                .putInt(KEY_THEME_MODE, THEME_LIGHT)
                .putBoolean(KEY_THEME_SEEDED, true)
                .apply()
        }
    }

    var baseUrl: String
        get() {
            val stored = prefs.getString(KEY_BASE_URL, null)
            return if (stored.isNullOrBlank()) BuildConfig.API_BASE_URL else stored
        }
        set(value) {
            prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()
        }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_PASSWORD, value).apply()
        }

    /** 0 = follow the system, 1 = always light (default), 2 = always dark. */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_LIGHT)
        set(value) {
            prefs.edit().putInt(KEY_THEME_MODE, value).apply()
        }

    var loggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false) && password.isNotEmpty()
        set(value) {
            prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
        }

    fun lock() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    fun defaultBaseUrl(): String = BuildConfig.API_BASE_URL

    private companion object {
        const val FILE_NAME = "yousif_session"
        const val KEY_BASE_URL = "base_url"
        const val KEY_PASSWORD = "password"
        const val KEY_LOGGED_IN = "logged_in"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_SEEDED = "theme_seeded"
        const val THEME_LIGHT = 1
    }
}
