package dev.sweitz.veniceglasses.data

import java.net.URI

/**
 * Third-party sites that host SillyTavern-compatible character cards, and how a link from each
 * becomes a card download. SillyTavern itself hosts no characters.
 *
 * Imports work like SillyTavern's "Import from external URL": paste (over adb) a card page link
 * from a supported store, or a direct link to any `.png` / `.json` card. Every imported card
 * still goes through [ContentPolicy]: cards tagged NSFW only show with mature text on, and a
 * mature card that involves minors is refused outright.
 */
object CardStores {
    data class Store(
        val name: String,
        val url: String,
        val mature: String,
        val import: String,
        /** True when a card page link from this site can be imported directly. */
        val pageLinks: Boolean,
    )

    val stores = listOf(
        Store(
            "Chub.ai", "https://chub.ai", "SFW/NSFW filters; lightly moderated",
            "Paste the card page link (chub.ai/characters/author/name).", pageLinks = true,
        ),
        Store(
            "Character Tavern", "https://character-tavern.com", "Curated, includes adult scenarios",
            "Paste the card page link (character-tavern.com/character/author/name).", pageLinks = true,
        ),
        Store(
            "CharaVault", "https://charavault.net", "Adult themes; strict rules against minors",
            "Copy the card's direct .png or .json download link.", pageLinks = false,
        ),
        Store(
            "AI Character Cards", "https://aicharactercards.com", "Moderated, vetted cards",
            "Copy the card's direct .png or .json download link.", pageLinks = false,
        ),
        Store(
            "JanitorAI", "https://janitorai.com", "Large, many mature bots",
            "No card downloads: only cards whose creator offers a file can be imported, via that file's link.",
            pageLinks = false,
        ),
        Store(
            "GitHub and community lists", "https://www.reddit.com/r/SillyTavernAI/", "Varies",
            "Use the raw file link (raw.githubusercontent.com/…/card.png).", pageLinks = false,
        ),
    )

    class UnsupportedLink(message: String) : Exception(message)

    /**
     * Candidate download URLs for a link, tried in order. Direct file links return themselves.
     * Throws [UnsupportedLink] for links that can't be turned into a card file.
     */
    fun downloadUrls(link: String): List<String> {
        val uri = runCatching { URI(link.trim()) }.getOrElse { throw UnsupportedLink("Not a valid link") }
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") throw UnsupportedLink("Link must start with https://")
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: throw UnsupportedLink("Not a valid link")
        val parts = uri.path.orEmpty().split('/').filter { it.isNotBlank() }

        return when {
            host == "chub.ai" || host == "characterhub.org" -> {
                // chub.ai/characters/<author>/<slug>
                val i = parts.indexOf("characters")
                val path = if (i >= 0 && parts.size >= i + 3) parts.subList(i + 1, i + 3).joinToString("/")
                else throw UnsupportedLink("Use a chub.ai/characters/author/name link")
                listOf("https://avatars.charhub.io/avatars/$path/chara_card_v2.png")
            }
            host == "character-tavern.com" -> {
                // character-tavern.com/character/<author>/<slug>
                val i = parts.indexOf("character")
                val path = if (i >= 0 && parts.size >= i + 3) parts.subList(i + 1, i + 3).joinToString("/")
                else throw UnsupportedLink("Use a character-tavern.com/character/author/name link")
                listOf("https://cards.character-tavern.com/$path.png?action=download")
            }
            host == "janitorai.com" ->
                throw UnsupportedLink("JanitorAI doesn't offer card downloads; use a file link from the creator")
            parts.lastOrNull()?.substringAfterLast('.', "")?.lowercase() in setOf("png", "json") ->
                listOf(uri.toString())
            else -> throw UnsupportedLink("Use a supported store page or a direct .png/.json card link")
        }
    }

    /** File name to save a downloaded card as, from its link. */
    fun fileName(link: String, isPng: Boolean): String {
        val parts = runCatching { URI(link.trim()).path }.getOrNull().orEmpty().split('/').filter { it.isNotBlank() }
        val base = parts.lastOrNull()?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "card"
        val safe = base.replace(Regex("[^A-Za-z0-9._ -]"), "_").take(60)
        return safe + if (isPng) ".png" else ".json"
    }
}
