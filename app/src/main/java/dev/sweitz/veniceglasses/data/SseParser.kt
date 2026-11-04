package dev.sweitz.veniceglasses.data

/**
 * Minimal Server-Sent Events parser for OpenAI-style streams (Venice is OpenAI-compatible):
 *
 *     data: {"choices":[{"delta":{"content":"Hel"}}]}
 *
 *     data: [DONE]
 *
 * Feed it one line at a time; it returns an event when a blank line ends one.
 * Pure Kotlin, so it's unit-tested on the JVM.
 */
class SseParser {
    sealed interface Event {
        data class Data(val payload: String) : Event
        data object Done : Event
    }

    private val buffer = StringBuilder()
    private var hasData = false

    /** Returns an event when [line] completes one, otherwise null. */
    fun feed(line: String): Event? {
        val l = line.removeSuffix("\r")
        if (l.isEmpty()) return flush()
        if (l.startsWith(":")) return null // comment / keep-alive

        val field = l.substringBefore(':')
        var value = if (':' in l) l.substringAfter(':') else ""
        if (value.startsWith(" ")) value = value.substring(1)

        if (field == "data") {
            if (hasData) buffer.append('\n')
            buffer.append(value)
            hasData = true
        }
        // "event", "id", "retry" aren't used by chat completions.
        return null
    }

    /** Call at end of stream so a final event without a trailing blank line isn't lost. */
    fun finish(): Event? = flush()

    private fun flush(): Event? {
        if (!hasData) return null
        val payload = buffer.toString()
        buffer.setLength(0)
        hasData = false
        return if (payload.trim() == "[DONE]") Event.Done else Event.Data(payload)
    }
}
