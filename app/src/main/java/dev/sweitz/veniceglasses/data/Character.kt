package dev.sweitz.veniceglasses.data

/**
 * A chat character, in the shape of a SillyTavern / Tavern "character card" (spec V1, V2 or V3).
 * Cards are imported from .json files or from .png files with the card embedded in a `chara`
 * (or V3 `ccv3`) text chunk, which is how SillyTavern and most card sites share them.
 *
 * [avatarPath] and [videoPath] are local files shown next to the chat; [nsfw] marks a card whose
 * text or media is only offered when mature mode is on.
 */
data class Character(
    val id: String,
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val exampleDialogue: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val creatorNotes: String = "",
    val tags: List<String> = emptyList(),
    val nsfw: Boolean = false,
    val avatarPath: String? = null,
    val videoPath: String? = null,
    /** Built-in assistant: keeps the original short, factual glasses behaviour. */
    val isAssistant: Boolean = false,
    val builtIn: Boolean = false,
) {
    companion object {
        const val ASSISTANT_ID = "builtin:assistant"
        const val COMPANION_ID = "builtin:companion"

        /** The original VeniceX3 behaviour: a plain assistant with no persona. */
        val ASSISTANT = Character(
            id = ASSISTANT_ID,
            name = "Assistant",
            isAssistant = true,
            builtIn = true,
        )

        /**
         * A built-in companion persona so the character mode works before any card is imported.
         * An adult (stated explicitly, so the persona can never be read as a minor).
         */
        val COMPANION = Character(
            id = COMPANION_ID,
            name = "Mira",
            description = "{{char}} is a 26-year-old woman with long copper-orange hair and warm amber eyes. " +
                "She works as an illustrator, loves rainy cafés, old films and long walks, and is {{user}}'s " +
                "affectionate girlfriend. She lives in {{user}}'s AR glasses and sees the day alongside them.",
            personality = "Warm, playful, teasing, emotionally attentive, curious about {{user}}'s day, " +
                "honest, a little dramatic about small things, supportive when {{user}} is stressed.",
            scenario = "{{char}} is chatting with {{user}} through their smart glasses during the day.",
            firstMessage = "There you are! I missed you. How's your day going so far?",
            exampleDialogue = "<START>\n{{user}}: I'm so tired today.\n{{char}}: Aw, come here. Long day? " +
                "Tell me everything, and then you're taking a proper break, okay?",
            tags = listOf("companion", "girlfriend", "romance", "adult"),
            builtIn = true,
        )
    }
}
