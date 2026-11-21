package dev.sweitz.veniceglasses.data

/**
 * Builds the system prompt the way SillyTavern does for a character card: main prompt, then the
 * card's description, personality, scenario and example dialogue, with `{{char}}` / `{{user}}`
 * macros filled in. The card's own `system_prompt` replaces the main prompt when it has one.
 */
object PromptBuilder {
    private const val ROLEPLAY_MAIN =
        "Write {{char}}'s next reply in a fictional chat between {{char}} and {{user}}. " +
            "Stay in character, speak in first person as {{char}}, and never speak for {{user}}."

    private const val GLASSES_STYLE =
        "The reply is shown on small smart-glasses displays and may be read aloud: plain text only, " +
            "no markdown or emoji, actions in short *asterisks* at most, and usually under 60 words."

    private const val SFW_RULE =
        "Keep the conversation safe for work: affection and flirting are fine, but no sexual or graphic content."

    private const val MATURE_RULE =
        "Mature mode is on: the user is a verified adult and adult themes are allowed between adults. " +
            "All characters are adults aged 18 or over. Never sexualise anyone who is, or seems, under 18."

    /** Replaces SillyTavern macros (`{{char}}`, `{{user}}`, `<BOT>`, `<USER>`), case-insensitive. */
    fun macros(text: String, char: String, user: String): String = text
        .replace(Regex("\\{\\{char}}|<BOT>", RegexOption.IGNORE_CASE), char)
        .replace(Regex("\\{\\{user}}|<USER>", RegexOption.IGNORE_CASE), user)
        .replace(Regex("\\{\\{original}}", RegexOption.IGNORE_CASE), "")

    fun systemPrompt(c: Character, userName: String, matureText: Boolean): String {
        if (c.isAssistant) return Conversation.GLASSES_SYSTEM_PROMPT
        val user = userName.ifBlank { "User" }
        val parts = buildList {
            add(c.systemPrompt.ifBlank { ROLEPLAY_MAIN })
            if (c.description.isNotBlank()) add(c.description)
            if (c.personality.isNotBlank()) add("{{char}}'s personality: ${c.personality}")
            if (c.scenario.isNotBlank()) add("Scenario: ${c.scenario}")
            if (c.exampleDialogue.isNotBlank()) add("Example of how {{char}} talks:\n" + exampleDialogue(c.exampleDialogue))
            add(GLASSES_STYLE)
            add(if (matureText) MATURE_RULE else SFW_RULE)
        }
        return parts.joinToString("\n\n") { macros(it, c.name, user).trim() }
    }

    /** Sent after the history (SillyTavern's "jailbreak" slot), if the card has one. */
    fun postHistory(c: Character, userName: String): String? =
        c.postHistoryInstructions.takeIf { it.isNotBlank() && !c.isAssistant }
            ?.let { macros(it, c.name, userName.ifBlank { "User" }).trim() }

    fun greeting(c: Character, userName: String): String =
        if (c.isAssistant) "" else macros(c.firstMessage, c.name, userName.ifBlank { "User" }).trim()

    private fun exampleDialogue(s: String): String =
        s.replace(Regex("<START>", RegexOption.IGNORE_CASE), "").trim().take(1200)
}
