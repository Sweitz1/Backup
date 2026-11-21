package dev.sweitz.veniceglasses.data

/**
 * Mature-content gating. Mature mode is off by default and has three separate switches (text,
 * images, video). They only take effect after the owner confirms over adb that they're 18+.
 *
 * Mature content is never allowed for a character, or an image request, that reads as
 * involving a minor: those always stay in safe mode, whatever the switches say.
 */
data class MatureAccess(
    val adultConfirmed: Boolean = false,
    val enabled: Boolean = false,
    val text: Boolean = false,
    val images: Boolean = false,
    val video: Boolean = false,
) {
    private val on: Boolean get() = adultConfirmed && enabled
    val textAllowed: Boolean get() = on && text
    val imagesAllowed: Boolean get() = on && images
    val videoAllowed: Boolean get() = on && video
}

object ContentPolicy {
    private val MINOR_WORDS = Regex(
        "\\b(loli|lolis|lolicon|shota|shotas|shotacon|underage|under-age|preteen|pre-teen|child|children|" +
            "kid|kids|toddler|infant|minor|minors|elementary school|middle school(er)?|junior high)\\b",
        RegexOption.IGNORE_CASE,
    )

    // "15 years old", "15-year-old", "15yo", "age 15", "aged 15" — any age under 18.
    private val MINOR_AGE = Regex(
        "\\b(([1-9]|1[0-7])\\s*(-|\\s)?\\s*(years?|yrs?)(\\s*-?\\s*old)?|([1-9]|1[0-7])\\s*y/?o|ages?d?\\s*:?\\s*([1-9]|1[0-7]))\\b",
        RegexOption.IGNORE_CASE,
    )

    /** True if [text] mentions minors or an age under 18. Conservative on purpose. */
    fun mentionsMinor(text: String): Boolean = MINOR_WORDS.containsMatchIn(text) || MINOR_AGE.containsMatchIn(text)

    /** True if this character must stay safe-for-work regardless of the mature switches. */
    fun characterRestricted(c: Character): Boolean =
        mentionsMinor(listOf(c.name, c.description, c.personality, c.scenario, c.tags.joinToString(" ")).joinToString("\n"))

    /** Whether the character should appear in the character list at all. */
    fun characterVisible(c: Character, access: MatureAccess): Boolean =
        !c.nsfw || (access.textAllowed && !characterRestricted(c))

    fun textAllowed(c: Character, access: MatureAccess): Boolean = access.textAllowed && !characterRestricted(c)

    /** Venice image `safe_mode`: true blurs adult images. Off only when everything allows it. */
    fun imageSafeMode(c: Character, prompt: String, access: MatureAccess): Boolean =
        !(access.imagesAllowed && !characterRestricted(c) && !mentionsMinor(prompt))

    fun videoAllowed(c: Character, access: MatureAccess): Boolean = access.videoAllowed && !characterRestricted(c)

    /** A card's own video plays when it isn't marked mature, or mature video is allowed. */
    fun showCharacterVideo(c: Character, access: MatureAccess): Boolean = !c.nsfw || videoAllowed(c, access)

    /** Avatar image shows when the card isn't mature, or mature images are allowed. */
    fun showCharacterImage(c: Character, access: MatureAccess): Boolean =
        !c.nsfw || (access.imagesAllowed && !characterRestricted(c))

    /** Always sent with mature image requests; Venice applies it as a negative prompt. */
    const val ADULT_NEGATIVE_PROMPT = "child, children, minor, underage, teen, loli, shota, young-looking, childlike body"
}
