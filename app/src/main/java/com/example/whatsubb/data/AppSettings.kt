package com.example.whatsubb.data

import android.content.Context
import com.example.whatsubb.BuildConfig
import java.security.MessageDigest

/**
 * Lokale instellingen van de app. Er worden hier GEEN persoonsgegevens opgeslagen,
 * alleen technische instellingen en (gehashte) toegangscodes.
 */
class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("whatsubb_settings", Context.MODE_PRIVATE)

    /** API-sleutel uit de instellingen, of anders uit local.properties (GEMINI_API_KEY). */
    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    val hasBegeleiderPin: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    fun setBegeleiderPin(pin: String) =
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()

    fun checkBegeleiderPin(pin: String): Boolean =
        prefs.getString(KEY_PIN_HASH, null) == hash(pin)

    /** Leeg = bewoners hebben geen code nodig. */
    val hasBewonerCode: Boolean
        get() = prefs.contains(KEY_BEWONER_HASH)

    fun setBewonerCode(code: String) {
        if (code.isBlank()) prefs.edit().remove(KEY_BEWONER_HASH).apply()
        else prefs.edit().putString(KEY_BEWONER_HASH, hash(code)).apply()
    }

    fun checkBewonerCode(code: String): Boolean =
        !hasBewonerCode || prefs.getString(KEY_BEWONER_HASH, null) == hash(code)

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(("whatsubb:" + value.trim()).toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val DEFAULT_MODEL = "gemini-flash-latest"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_PIN_HASH = "begeleider_pin_hash"
        private const val KEY_BEWONER_HASH = "bewoner_code_hash"
    }
}
