package dev.sweitz.veniceglasses.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import dev.sweitz.veniceglasses.app
import dev.sweitz.veniceglasses.data.CharacterCard
import dev.sweitz.veniceglasses.data.Settings
import java.io.File
import kotlinx.coroutines.launch

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
 *
 * Characters: character (id or name), user_name, layout (compact|portrait), image_model,
 * import_card (path to a .png/.json card the app can read), import_url (a Chub.ai or
 * Character Tavern card page, or a direct .png/.json link: downloaded in the background,
 * result logged), list_characters (true),
 * export_template (true: writes the built-in companion as a V2 card to the characters folder).
 *
 * Mature mode: confirm_adult (true|false — required before any mature switch works; false also
 * turns mature mode off), mature, mature_text, mature_images, mature_video (true|false).
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

            // Characters
            x.getString("user_name")?.takeIf { it.isNotBlank() }?.let { s.userName = it; changed += "user_name=$it" }
            x.getString("layout")?.let {
                if (it in Settings.LAYOUTS) { s.layout = it; changed += "layout=$it" }
                else Log.w(TAG, "layout must be one of ${Settings.LAYOUTS}")
            }
            x.getString("image_model")?.takeIf { it.isNotBlank() }?.let { s.imageModel = it; changed += "image_model=$it" }
            x.getString("import_card")?.takeIf { it.isNotBlank() }?.let { path ->
                runCatching { app.characters.import(File(path)) }
                    .onSuccess { c -> s.characterId = c.id; changed += "imported ${c.name} (${c.id})" }
                    .onFailure { Log.w(TAG, "import_card failed: ${it.message}") }
            }
            x.getString("import_url")?.takeIf { it.isNotBlank() }?.let { link ->
                changed += "import_url started"
                val a = app
                a.appScope.launch {
                    runCatching { a.cardDownloader.import(link) }
                        .onSuccess { c ->
                            a.settings.characterId = c.id
                            Log.i(TAG, "Imported ${c.name} (${c.id})${if (c.nsfw) " [18+: shown only with mature text on]" else ""}")
                        }
                        .onFailure { Log.w(TAG, "import_url failed: ${it.message}") }
                }
            }
            if (bool(x, "export_template") == true) {
                val f = File(app.characters.folder, "template.json.txt")
                f.writeText(CharacterCard.toJson(dev.sweitz.veniceglasses.data.Character.COMPANION))
                Log.i(TAG, "Template card written to ${f.absolutePath} (rename to .json after editing)")
            }
            x.getString("character")?.takeIf { it.isNotBlank() }?.let { want ->
                val all = app.characters.all(refresh = true)
                val c = all.firstOrNull { it.id == want } ?: all.firstOrNull { it.name.equals(want, ignoreCase = true) }
                if (c != null) { s.characterId = c.id; changed += "character=${c.name}" }
                else Log.w(TAG, "No character '$want'. Use --ez list_characters true")
            }

            // Mature mode: the adult confirmation comes first so the switches below can apply.
            bool(x, "confirm_adult")?.let { s.adultConfirmed = it; changed += "adult_confirmed=$it" }
            bool(x, "mature")?.let {
                if (it && !s.adultConfirmed) Log.w(TAG, "mature needs --ez confirm_adult true (you must be 18 or older)")
                else { s.matureEnabled = it; changed += "mature=$it" }
            }
            bool(x, "mature_text")?.let { s.matureText = it; changed += "mature_text=$it" }
            bool(x, "mature_images")?.let { s.matureImages = it; changed += "mature_images=$it" }
            bool(x, "mature_video")?.let { s.matureVideo = it; changed += "mature_video=$it" }

            if (bool(x, "list_characters") == true) {
                Log.i(TAG, "Characters folder: ${app.characters.folder.absolutePath}")
                app.characters.all(refresh = true).forEach { c ->
                    Log.i(TAG, "  ${c.id}  ${c.name}${if (c.nsfw) "  [18+]" else ""}${if (c.videoPath != null) "  [video]" else ""}")
                }
                app.characters.lastErrors.forEach { Log.w(TAG, "  skipped: $it") }
            }
        }

        // Never log the key itself.
        Log.i(TAG, if (changed.isEmpty()) "No settings changed" else "Updated: ${changed.joinToString()}")
        Log.i(TAG, "model=${s.model} stt_model=${s.sttModel} web_search=${s.webSearch} read_aloud=${s.readAloud} key_set=${s.hasApiKey}")
        Log.i(TAG, "character=${s.characterId} user_name=${s.userName} layout=${s.layout} image_model=${s.imageModel}")
        Log.i(TAG, "adult_confirmed=${s.adultConfirmed} mature=${s.matureEnabled} text=${s.matureText} images=${s.matureImages} video=${s.matureVideo}")
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
