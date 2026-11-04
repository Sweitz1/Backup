package dev.sweitz.veniceglasses.data

import android.content.Context
import dev.sweitz.veniceglasses.BuildConfig

/** App settings in private SharedPreferences (only this app can read them). */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_API_KEY
        set(v) = prefs.edit().putString(KEY_API_KEY, v.trim()).apply()

    var model: String
        get() = prefs.getString(KEY_MODEL, null) ?: DEFAULT_MODEL
        set(v) = prefs.edit().putString(KEY_MODEL, v.trim()).apply()

    var sttModel: String
        get() = prefs.getString(KEY_STT_MODEL, null) ?: DEFAULT_STT_MODEL
        set(v) = prefs.edit().putString(KEY_STT_MODEL, v.trim()).apply()

    /** Read answers aloud with the glasses' on-device text-to-speech. */
    var readAloud: Boolean
        get() = prefs.getBoolean(KEY_READ_ALOUD, false)
        set(v) = prefs.edit().putBoolean(KEY_READ_ALOUD, v).apply()

    /** Venice's own default system prompt (Venice default: on). */
    var includeVeniceSystemPrompt: Boolean
        get() = prefs.getBoolean(KEY_VENICE_PROMPT, true)
        set(v) = prefs.edit().putBoolean(KEY_VENICE_PROMPT, v).apply()

    /** Venice web search: off | auto | on. */
    var webSearch: String
        get() = prefs.getString(KEY_WEB_SEARCH, null)?.takeIf { it in WEB_SEARCH_MODES } ?: "off"
        set(v) = prefs.edit().putString(KEY_WEB_SEARCH, v.takeIf { it in WEB_SEARCH_MODES } ?: "off").apply()

    val hasApiKey: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val DEFAULT_MODEL = "venice-uncensored-1-2"
        const val DEFAULT_STT_MODEL = "openai/whisper-large-v3"
        val WEB_SEARCH_MODES = listOf("off", "auto", "on")

        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_STT_MODEL = "stt_model"
        const val KEY_READ_ALOUD = "read_aloud"
        const val KEY_VENICE_PROMPT = "venice_system_prompt"
        const val KEY_WEB_SEARCH = "web_search"
    }
}
