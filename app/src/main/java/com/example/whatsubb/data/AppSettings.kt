package com.example.whatsubb.data

import android.content.Context
import com.example.whatsubb.BuildConfig
import java.security.MessageDigest

/**
 * Lokale instellingen van de app. Er worden hier GEEN persoonsgegevens opgeslagen,
 * alleen technische instellingen, (gehashte) toegangscodes en eventueel de voornaam van de begeleider.
 */
data class CallTarget(val name: String, val phone: String, val confirmFirst: Boolean) {
    val label get() = "Bel $name"
    val chatLabel get() = "Chat met $name"
}

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("whatsubb_settings", Context.MODE_PRIVATE)

    /** API-sleutel uit de instellingen, of anders uit local.properties (GEMINI_API_KEY). */
    var apiKey: String
        get() = prefs.getString(KEY_API, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY
        set(value) = prefs.edit().putString(KEY_API, value.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString(KEY_MODEL, value.trim()).apply()

    /** 06-nummer van de begeleiding voor de bel-knop bij bewoners (leeg = geen knop). */
    var begeleidingPhone: String
        get() = prefs.getString(KEY_PHONE, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_PHONE, value.trim()).apply()

    /** Nummer van de bereikbare dienst (23:00-08:00); leeg = 's nachts geen knop. */
    var bereikbareDienstPhone: String
        get() = prefs.getString(KEY_NIGHT_PHONE, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_NIGHT_PHONE, value.trim()).apply()

    /** Voornaam van de begeleider op deze telefoon (alleen voornaam; leeg = niet ingesteld). Gaat nooit mee bij bewoners. */
    var begeleiderName: String
        get() = prefs.getString(KEY_BEGELEIDER_NAME, null).orEmpty()
        set(value) = prefs.edit().putString(KEY_BEGELEIDER_NAME, firstName(value)).apply()

    /** Welk nummer de bel-knop nu belt: overdag de begeleiding, 's nachts de bereikbare dienst. */
    fun currentCallTarget(now: java.util.Calendar = java.util.Calendar.getInstance()): CallTarget? {
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        val night = hour >= 23 || hour < 8
        return when {
            night && bereikbareDienstPhone.isNotBlank() ->
                CallTarget("de bereikbare dienst", bereikbareDienstPhone, confirmFirst = true)
            !night && begeleidingPhone.isNotBlank() ->
                CallTarget("de begeleiding", begeleidingPhone, confirmFirst = false)
            else -> null
        }
    }

    val hasBegeleiderPin: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    fun setBegeleiderPin(pin: String) =
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()

    fun checkBegeleiderPin(pin: String): Boolean =
        prefs.getString(KEY_PIN_HASH, null) == hash(pin)

    /** Aparte PIN voor beheer (kennisbank, documenten, sleutel, codes). */
    val hasAdminPin: Boolean
        get() = prefs.contains(KEY_ADMIN_HASH)

    fun setAdminPin(pin: String) =
        prefs.edit().putString(KEY_ADMIN_HASH, hash("admin:$pin")).apply()

    fun checkAdminPin(pin: String): Boolean =
        prefs.getString(KEY_ADMIN_HASH, null) == hash("admin:$pin")

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
        /** Alleen het eerste woord, met hoofdletter: " jeroen van sonsbeek" -> "Jeroen". */
        fun firstName(value: String): String =
            value.trim().substringBefore(' ').filter { it.isLetter() || it == '-' }.take(30)
                .replaceFirstChar { it.uppercase() }

        const val DEFAULT_MODEL = "gemini-flash-latest"
        private const val KEY_BEGELEIDER_NAME = "begeleider_name"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_PIN_HASH = "begeleider_pin_hash"
        private const val KEY_BEWONER_HASH = "bewoner_code_hash"
        private const val KEY_ADMIN_HASH = "admin_pin_hash"
        private const val KEY_PHONE = "begeleiding_phone"
        private const val KEY_NIGHT_PHONE = "bereikbare_dienst_phone"
    }
}
