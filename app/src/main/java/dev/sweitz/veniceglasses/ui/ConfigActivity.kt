package dev.sweitz.veniceglasses.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import dev.sweitz.veniceglasses.app
import dev.sweitz.veniceglasses.data.Settings

/**
 * Headless settings screen for adb, since there's no keyboard on the glasses. Protected in
 * the manifest by the DUMP permission, which adb's shell has and ordinary apps can't get.
 *
 *   adb shell am start -n dev.sweitz.veniceglasses/.ui.ConfigActivity \
 *       --es api_key "YOUR_KEY" --es model "venice-uncensored-1-2"
 *
 * Extras (all optional): api_key, model, stt_model, web_search (off|auto|on),
 * read_aloud (true|false), venice_system_prompt (true|false), clear_key (true),
 * print_log (true: prints the 24 h request log to logcat under tag VeniceX3).
 */
class ConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val s = app.settings
        val x = intent.extras
        val changed = mutableListOf<String>()

        if (x != null) {
            x.getString("api_key")?.takeIf { it.isNotBlank() }?.let { s.apiKey = it; changed += "api_key" }
            if (x.getString("clear_key") == "true" || x.getBoolean("clear_key", false)) { s.apiKey = ""; changed += "api_key cleared" }
            x.getString("model")?.takeIf { it.isNotBlank() }?.let { s.model = it; changed += "model=$it" }
            x.getString("stt_model")?.takeIf { it.isNotBlank() }?.let { s.sttModel = it; changed += "stt_model=$it" }
            x.getString("web_search")?.let {
                if (it in Settings.WEB_SEARCH_MODES) { s.webSearch = it; changed += "web_search=$it" }
                else Log.w(TAG, "web_search must be one of ${Settings.WEB_SEARCH_MODES}")
            }
            bool(x, "read_aloud")?.let { s.readAloud = it; changed += "read_aloud=$it" }
            bool(x, "venice_system_prompt")?.let { s.includeVeniceSystemPrompt = it; changed += "venice_system_prompt=$it" }
            if (bool(x, "print_log") == true) app.requestLog.read().lines().forEach { Log.i(TAG, it) }
        }

        // Never log the key itself.
        Log.i(TAG, if (changed.isEmpty()) "No settings changed" else "Updated: ${changed.joinToString()}")
        Log.i(TAG, "model=${s.model} stt_model=${s.sttModel} web_search=${s.webSearch} read_aloud=${s.readAloud} key_set=${s.hasApiKey}")
        finish()
    }

    /** Accepts --ez name true and --es name "true". */
    private fun bool(b: Bundle, key: String): Boolean? = when (val v = b.get(key)) {
        is Boolean -> v
        is String -> v.equals("true", ignoreCase = true).takeIf { v.equals("true", true) || v.equals("false", true) }
        else -> null
    }

    private companion object {
        const val TAG = "VeniceX3"
    }
}
