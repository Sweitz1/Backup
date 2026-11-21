package dev.sweitz.veniceglasses.data

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Characters available on the glasses: the built-in assistant and companion, plus every
 * SillyTavern card in the app's characters folder, which adb can write to without root:
 *
 *     /sdcard/Android/data/dev.sweitz.veniceglasses/files/characters/
 *
 * Files are grouped by name: `Name.png` (a card PNG, also used as the avatar) or `Name.json`
 * (card) with an optional `Name.png|jpg|webp` avatar, and an optional `Name.mp4|webm` looping
 * video shown in place of the avatar. `companion.png` / `companion.mp4` give the built-in
 * companion an avatar or video.
 */
class CharacterStore(context: Context) {
    val folder: File = (context.getExternalFilesDir("characters") ?: File(context.filesDir, "characters")).apply { mkdirs() }

    @Volatile private var cache: List<Character>? = null

    /** Errors from the last scan, e.g. a PNG without a card in it. */
    @Volatile var lastErrors: List<String> = emptyList()
        private set

    fun all(refresh: Boolean = false): List<Character> {
        if (!refresh) cache?.let { return it }
        val loaded = listOf(Character.ASSISTANT, withMedia(Character.COMPANION, "companion")) + scan()
        cache = loaded
        return loaded
    }

    fun find(id: String): Character = all().firstOrNull { it.id == id } ?: Character.ASSISTANT

    /** Characters the menu may show under the current mature settings. */
    fun visible(access: MatureAccess): List<Character> = all().filter { ContentPolicy.characterVisible(it, access) }

    /** Copies a card file (and nothing else) into the characters folder. Returns the parsed card. */
    fun import(source: File): Character {
        val bytes = source.readBytes()
        CharacterCard.parse(bytes, "check") // throws if it isn't a card
        val dest = File(folder, source.name.replace(Regex("[^A-Za-z0-9._ -]"), "_"))
        if (source.canonicalPath != dest.canonicalPath) dest.writeBytes(bytes)
        cache = null
        return all(refresh = true).first { it.id == idFor(dest) }
    }

    private fun scan(): List<Character> {
        val errors = mutableListOf<String>()
        val files = folder.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()
        val byBase = files.groupBy { it.nameWithoutExtension }
        val out = byBase.mapNotNull { (base, group) ->
            if (base.equals("companion", ignoreCase = true)) return@mapNotNull null
            val json = group.firstOrNull { it.extension.equals("json", true) }
            val image = group.firstOrNull { it.extension.lowercase() in IMAGE_EXT }
            val video = group.firstOrNull { it.extension.lowercase() in VIDEO_EXT }
            val cardFile = json ?: image?.takeIf { it.extension.equals("png", true) } ?: return@mapNotNull null
            runCatching {
                if (cardFile.length() > MAX_CARD_BYTES) throw CharacterCard.InvalidCard("file is too large")
                CharacterCard.parse(cardFile.readBytes(), idFor(cardFile), image?.absolutePath, video?.absolutePath)
            }.onFailure { errors += "${cardFile.name}: ${it.message}" }.getOrNull()
        }.sortedBy { it.name.lowercase() }
        lastErrors = errors
        errors.forEach { Log.w("VeniceX3", "Character card skipped: $it") }
        return out
    }

    private fun withMedia(c: Character, base: String): Character {
        val files = folder.listFiles()?.filter { it.nameWithoutExtension.equals(base, true) } ?: return c
        return c.copy(
            avatarPath = files.firstOrNull { it.extension.lowercase() in IMAGE_EXT }?.absolutePath,
            videoPath = files.firstOrNull { it.extension.lowercase() in VIDEO_EXT }?.absolutePath,
        )
    }

    private fun idFor(f: File) = "file:" + f.nameWithoutExtension

    companion object {
        val IMAGE_EXT = setOf("png", "jpg", "jpeg", "webp", "gif")
        val VIDEO_EXT = setOf("mp4", "webm", "3gp", "mkv")
        private const val MAX_CARD_BYTES = 20L * 1024 * 1024
    }
}
