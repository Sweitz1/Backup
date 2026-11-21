package dev.sweitz.veniceglasses.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Inflater

/**
 * Reads SillyTavern / Tavern character cards:
 *  - JSON, spec V2/V3 (`{"spec":"chara_card_v2","data":{…}}`), V1 (flat fields) and the older
 *    Pygmalion field names (`char_name`, `char_persona`, …);
 *  - PNG with the card as base64 JSON in a `ccv3` or `chara` tEXt / zTXt / iTXt chunk.
 * Pure JVM code so it's unit-tested without a device.
 */
object CharacterCard {
    class InvalidCard(message: String) : Exception(message)

    private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)

    fun isPng(bytes: ByteArray): Boolean =
        bytes.size >= 8 && PNG_SIGNATURE.indices.all { bytes[it] == PNG_SIGNATURE[it] }

    /** Parses a card file's bytes (PNG or JSON). [id] identifies it in the store. */
    fun parse(bytes: ByteArray, id: String, avatarPath: String? = null, videoPath: String? = null): Character {
        val json = if (isPng(bytes)) {
            val chunks = pngTextChunks(bytes)
            val raw = chunks["ccv3"] ?: chunks["chara"] ?: throw InvalidCard("PNG has no character card in it")
            decodeBase64Json(raw)
        } else {
            bytes.toString(Charsets.UTF_8).trimStart('﻿')
        }
        return parseJson(json, id, avatarPath, videoPath)
    }

    fun parseJson(json: String, id: String, avatarPath: String? = null, videoPath: String? = null): Character {
        val root = runCatching { JSONObject(json) }.getOrElse { throw InvalidCard("Card isn't valid JSON") }
        val d = root.optJSONObject("data")?.takeIf { root.has("spec") || it.has("name") } ?: root

        val name = d.str("name").ifBlank { d.str("char_name") }.ifBlank { throw InvalidCard("Card has no name") }
        val tags = (d.optJSONArray("tags") ?: root.optJSONArray("tags")).strings()
        val ext = d.optJSONObject("extensions")
        val nsfw = tags.any { it.equals("nsfw", true) || it.equals("explicit", true) } ||
            ext?.optBoolean("nsfw", false) == true || root.optBoolean("nsfw", false)

        return Character(
            id = id,
            name = name.trim(),
            description = d.str("description").ifBlank { d.str("char_persona") },
            personality = d.str("personality"),
            scenario = d.str("scenario").ifBlank { d.str("world_scenario") },
            firstMessage = d.str("first_mes").ifBlank { d.str("char_greeting") },
            exampleDialogue = d.str("mes_example").ifBlank { d.str("example_dialogue") },
            systemPrompt = d.str("system_prompt"),
            postHistoryInstructions = d.str("post_history_instructions"),
            creatorNotes = d.str("creator_notes").ifBlank { d.str("creatorcomment") },
            tags = tags,
            nsfw = nsfw,
            avatarPath = avatarPath,
            videoPath = videoPath,
        )
    }

    /** All text chunks in a PNG, keyword → text (zTXt and compressed iTXt are inflated). */
    fun pngTextChunks(bytes: ByteArray): Map<String, String> {
        if (!isPng(bytes)) throw InvalidCard("Not a PNG")
        val out = LinkedHashMap<String, String>()
        var pos = 8
        while (pos + 8 <= bytes.size) {
            val len = readInt(bytes, pos)
            val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)
            val start = pos + 8
            val end = start + len
            if (len < 0 || end + 4 > bytes.size) break
            val data = bytes.copyOfRange(start, end)
            when (type) {
                "tEXt" -> {
                    val z = data.indexOf(0)
                    if (z > 0) out[String(data, 0, z, Charsets.ISO_8859_1)] = String(data, z + 1, data.size - z - 1, Charsets.ISO_8859_1)
                }
                "zTXt" -> {
                    val z = data.indexOf(0)
                    if (z > 0 && z + 2 <= data.size) {
                        out[String(data, 0, z, Charsets.ISO_8859_1)] = inflate(data.copyOfRange(z + 2, data.size)).toString(Charsets.ISO_8859_1)
                    }
                }
                "iTXt" -> parseITxt(data)?.let { (k, v) -> out[k] = v }
                "IEND" -> break
            }
            pos = end + 4 // skip CRC
        }
        return out
    }

    private fun parseITxt(data: ByteArray): Pair<String, String>? {
        val k = data.indexOf(0).takeIf { it > 0 } ?: return null
        if (k + 3 > data.size) return null
        val compressed = data[k + 1].toInt() == 1
        var p = k + 3
        val langEnd = indexOf(data, 0, p).takeIf { it >= 0 } ?: return null
        p = langEnd + 1
        val transEnd = indexOf(data, 0, p).takeIf { it >= 0 } ?: return null
        p = transEnd + 1
        val text = data.copyOfRange(p, data.size).let { if (compressed) inflate(it) else it }
        return String(data, 0, k, Charsets.ISO_8859_1) to text.toString(Charsets.UTF_8)
    }

    private fun decodeBase64Json(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) return trimmed // some tools store plain JSON
        return runCatching { Base64.getMimeDecoder().decode(trimmed).toString(Charsets.UTF_8) }
            .getOrElse { throw InvalidCard("Card data in PNG isn't valid base64") }
    }

    private fun inflate(data: ByteArray): ByteArray {
        val inflater = Inflater()
        inflater.setInput(data)
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (!inflater.finished()) {
            val n = inflater.inflate(buf)
            if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
            out.write(buf, 0, n)
        }
        inflater.end()
        return out.toByteArray()
    }

    private fun readInt(b: ByteArray, i: Int): Int =
        ((b[i].toInt() and 0xFF) shl 24) or ((b[i + 1].toInt() and 0xFF) shl 16) or
            ((b[i + 2].toInt() and 0xFF) shl 8) or (b[i + 3].toInt() and 0xFF)

    private fun ByteArray.indexOf(value: Byte): Int = indexOf(this, value, 0)

    private fun indexOf(b: ByteArray, value: Byte, from: Int): Int {
        for (i in from until b.size) if (b[i] == value) return i
        return -1
    }

    private fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it, "").trim().takeIf(String::isNotEmpty) }

    /** Minimal V2 card for a character, e.g. to export the built-in one as a template. */
    fun toJson(c: Character): String = JSONObject().apply {
        put("spec", "chara_card_v2")
        put("spec_version", "2.0")
        put("data", JSONObject().apply {
            put("name", c.name)
            put("description", c.description)
            put("personality", c.personality)
            put("scenario", c.scenario)
            put("first_mes", c.firstMessage)
            put("mes_example", c.exampleDialogue)
            put("system_prompt", c.systemPrompt)
            put("post_history_instructions", c.postHistoryInstructions)
            put("creator_notes", c.creatorNotes)
            put("tags", JSONArray(c.tags))
        })
    }.toString(2)
}
