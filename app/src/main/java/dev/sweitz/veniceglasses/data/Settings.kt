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

    // --- Characters ---

    /** Selected character (see CharacterStore); the built-in assistant by default. */
    var characterId: String
        get() = prefs.getString(KEY_CHARACTER, null) ?: Character.ASSISTANT_ID
        set(v) = prefs.edit().putString(KEY_CHARACTER, v).apply()

    /** Your name in character chats ({{user}}). */
    var userName: String
        get() = prefs.getString(KEY_USER_NAME, null)?.takeIf { it.isNotBlank() } ?: "User"
        set(v) = prefs.edit().putString(KEY_USER_NAME, v.trim()).apply()

    /** compact: small avatar above the chat panel · portrait: large figure beside it. */
    var layout: String
        get() = prefs.getString(KEY_LAYOUT, null)?.takeIf { it in LAYOUTS } ?: LAYOUTS[0]
        set(v) = prefs.edit().putString(KEY_LAYOUT, v.takeIf { it in LAYOUTS } ?: LAYOUTS[0]).apply()

    var imageModel: String
        get() = prefs.getString(KEY_IMAGE_MODEL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_MODEL
        set(v) = prefs.edit().putString(KEY_IMAGE_MODEL, v.trim()).apply()

    // --- Mature content (off by default; needs the adb-only adult confirmation first) ---

    /** Set only over adb (ConfigActivity): the owner confirms they're 18 or older. */
    var adultConfirmed: Boolean
        get() = prefs.getBoolean(KEY_ADULT_CONFIRMED, false)
        set(v) {
            prefs.edit().putBoolean(KEY_ADULT_CONFIRMED, v).apply()
            if (!v) matureEnabled = false
        }

    var matureEnabled: Boolean
        get() = adultConfirmed && prefs.getBoolean(KEY_MATURE, false)
        set(v) = prefs.edit().putBoolean(KEY_MATURE, v && adultConfirmed).apply()

    var matureText: Boolean
        get() = prefs.getBoolean(KEY_MATURE_TEXT, true)
        set(v) = prefs.edit().putBoolean(KEY_MATURE_TEXT, v).apply()

    var matureImages: Boolean
        get() = prefs.getBoolean(KEY_MATURE_IMAGES, false)
        set(v) = prefs.edit().putBoolean(KEY_MATURE_IMAGES, v).apply()

    var matureVideo: Boolean
        get() = prefs.getBoolean(KEY_MATURE_VIDEO, false)
        set(v) = prefs.edit().putBoolean(KEY_MATURE_VIDEO, v).apply()

    val matureAccess: MatureAccess
        get() = MatureAccess(adultConfirmed, matureEnabled, matureText, matureImages, matureVideo)

    companion object {
        const val DEFAULT_MODEL = "venice-uncensored-1-2"
        const val DEFAULT_STT_MODEL = "openai/whisper-large-v3"
        val WEB_SEARCH_MODES = listOf("off", "auto", "on")
        val LAYOUTS = listOf("compact", "portrait")
        const val DEFAULT_IMAGE_MODEL = "venice-sd35"

        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_STT_MODEL = "stt_model"
        const val KEY_READ_ALOUD = "read_aloud"
        const val KEY_VENICE_PROMPT = "venice_system_prompt"
        const val KEY_WEB_SEARCH = "web_search"
        const val KEY_CHARACTER = "character"
        const val KEY_USER_NAME = "user_name"
        const val KEY_LAYOUT = "layout"
        const val KEY_IMAGE_MODEL = "image_model"
        const val KEY_ADULT_CONFIRMED = "adult_confirmed"
        const val KEY_MATURE = "mature"
        const val KEY_MATURE_TEXT = "mature_text"
        const val KEY_MATURE_IMAGES = "mature_images"
        const val KEY_MATURE_VIDEO = "mature_video"
    }
}
