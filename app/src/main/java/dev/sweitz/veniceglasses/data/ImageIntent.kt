package dev.sweitz.veniceglasses.data

/**
 * Spots spoken image requests ("show me a picture of…", "send me a selfie", "draw a cat") and
 * turns them into an image prompt. Requests about the character ("selfie", "what do you look
 * like") get the character's description added so the picture matches the card.
 */
object ImageIntent {
    private const val NOUN = "(image|picture|photo|pic|drawing|painting|selfie)"

    private val TRIGGERS = listOf(
        // "draw a red fox", "can you paint me a picture of the sea"
        Regex("^(please\\s+)?((can|could|would) you\\s+)?(draw|paint|sketch)\\s+(me\\s+)?((an?\\s+)?$NOUN\\s+(of\\s+)?)?(?<p>.+)", RegexOption.IGNORE_CASE),
        // "generate an image of…" (needs the noun, so "make me a sandwich" stays a chat)
        Regex("^(please\\s+)?((can|could|would) you\\s+)?(generate|create|make)\\s+(me\\s+)?(an?\\s+)?$NOUN\\s*(of\\s+)?(?<p>.*)", RegexOption.IGNORE_CASE),
        Regex("(show|send|give)\\s+me\\s+(an?\\s+)?$NOUN\\s*(of\\s+)?(?<p>.*)", RegexOption.IGNORE_CASE),
        Regex("(take|send)\\s+(me\\s+)?an?\\s+$NOUN(?<p>.*)", RegexOption.IGNORE_CASE),
        Regex("^(an?\\s+)?$NOUN\\s+of\\s+(?<p>.+)", RegexOption.IGNORE_CASE),
        Regex("what do you look like(?<p>.*)", RegexOption.IGNORE_CASE),
    )
    private val SELF_SUBJECT = Regex("\\b(you|your|yourself)\\b", RegexOption.IGNORE_CASE)
    private val SELF_REQUEST = Regex("\\bselfie\\b|what do you look like", RegexOption.IGNORE_CASE)

    data class Request(val prompt: String, val ofCharacter: Boolean)

    fun parse(text: String): Request? {
        val t = text.trim().trimEnd('.', '!', '?')
        for (r in TRIGGERS) {
            val m = r.find(t) ?: continue
            val subject = m.groups["p"]?.value?.trim().orEmpty()
            val self = SELF_SUBJECT.containsMatchIn(subject) || SELF_REQUEST.containsMatchIn(t)
            if (subject.isBlank() && !self) continue
            return Request(subject.ifBlank { "a portrait" }, self)
        }
        return null
    }

    /** Final prompt: the subject, plus the character's appearance for pictures of them. */
    fun prompt(req: Request, c: Character, userName: String): String {
        if (!req.ofCharacter || c.isAssistant) return req.prompt
        val looks = PromptBuilder.macros(c.description, c.name, userName.ifBlank { "User" })
            .replace(Regex("\\s+"), " ").take(500)
        val subject = req.prompt.replace(Regex("\\b(yourself|you)\\b", RegexOption.IGNORE_CASE), c.name)
        return "$subject. ${c.name}, an adult: $looks"
    }
}
