package dev.sweitz.veniceglasses.data

data class ChatMessage(val role: String, val content: String)

/** In-memory chat history, trimmed so requests stay small. Never written to disk. */
class Conversation(private val maxTurns: Int = 8) {
    private val turns = ArrayList<ChatMessage>()

    /** A character's first message (SillyTavern `first_mes`), sent ahead of the history. */
    var greeting: String = ""

    val messages: List<ChatMessage> get() = turns.toList()

    fun addUser(text: String) = add(ChatMessage("user", text))

    fun addAssistant(text: String) {
        if (text.isNotBlank()) add(ChatMessage("assistant", text))
    }

    /** Drops a trailing user message whose answer failed or was cancelled. */
    fun dropDanglingUser() {
        if (turns.lastOrNull()?.role == "user") turns.removeAt(turns.lastIndex)
    }

    fun clear() {
        turns.clear()
        greeting = ""
    }

    /**
     * System prompt, greeting, history, then optional post-history instructions (the order
     * SillyTavern uses), ready to send.
     */
    fun forRequest(systemPrompt: String?, postHistory: String? = null): List<ChatMessage> = buildList {
        if (!systemPrompt.isNullOrBlank()) add(ChatMessage("system", systemPrompt))
        if (greeting.isNotBlank()) add(ChatMessage("assistant", greeting))
        addAll(turns)
        if (!postHistory.isNullOrBlank()) add(ChatMessage("system", postHistory))
    }

    private fun add(m: ChatMessage) {
        turns += m
        // Keep the last [maxTurns] exchanges (user + assistant pairs), oldest first out.
        while (turns.size > maxTurns * 2) turns.removeAt(0)
        // Never start the history with an assistant message.
        while (turns.firstOrNull()?.role == "assistant") turns.removeAt(0)
    }

    companion object {
        /** Tuned for a 640×480 display: short, plain text, no markdown. */
        const val GLASSES_SYSTEM_PROMPT =
            "You are being shown on smart glasses with a small display, and may be read aloud. " +
                "Answer in plain text only: no markdown, tables, headings, bullet symbols or emoji. " +
                "Keep answers under 80 words unless the user asks for more detail. Lead with the answer."
    }
}
