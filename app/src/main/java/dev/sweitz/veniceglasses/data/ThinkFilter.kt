package dev.sweitz.veniceglasses.data

/**
 * Removes `<think>…</think>` reasoning from streamed text, even when a tag is split across
 * chunks. Venice's `strip_thinking_response` usually handles this server-side; this is the
 * fallback so reasoning never fills the small glasses display.
 */
class ThinkFilter {
    private var inThink = false
    private var pending = "" // possible partial tag held back from the last chunk

    fun push(chunk: String): String {
        val out = StringBuilder()
        var s = pending + chunk
        pending = ""

        while (s.isNotEmpty()) {
            val tag = if (inThink) CLOSE else OPEN
            val idx = s.indexOf(tag)
            if (idx >= 0) {
                if (!inThink) out.append(s, 0, idx)
                s = s.substring(idx + tag.length)
                inThink = !inThink
                continue
            }
            // Hold back a trailing fragment that might be the start of the tag.
            val keep = partialSuffix(s, tag)
            if (!inThink) out.append(s, 0, s.length - keep)
            pending = s.substring(s.length - keep)
            break
        }
        return out.toString()
    }

    /** Text held back at the very end (an unfinished "<thi…" that never became a tag). */
    fun flush(): String = if (inThink) "" else pending.also { pending = "" }

    private fun partialSuffix(s: String, tag: String): Int {
        for (len in minOf(tag.length - 1, s.length) downTo 1) {
            if (tag.startsWith(s.substring(s.length - len))) return len
        }
        return 0
    }

    private companion object {
        const val OPEN = "<think>"
        const val CLOSE = "</think>"
    }
}
