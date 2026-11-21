package dev.sweitz.veniceglasses

import dev.sweitz.veniceglasses.data.Character
import dev.sweitz.veniceglasses.data.CharacterCard
import dev.sweitz.veniceglasses.data.ContentPolicy
import dev.sweitz.veniceglasses.data.Credits
import dev.sweitz.veniceglasses.data.Conversation
import dev.sweitz.veniceglasses.data.ImageIntent
import dev.sweitz.veniceglasses.data.MatureAccess
import dev.sweitz.veniceglasses.data.PromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32

class CharacterCardTest {
    private val v2 = """{"spec":"chara_card_v2","spec_version":"2.0","data":{"name":"Rin","description":"{{char}} is a 24-year-old barista.",
        "personality":"cheerful","scenario":"a café","first_mes":"Hi {{user}}!","mes_example":"<START>\n{{user}}: hi\n{{char}}: hey",
        "system_prompt":"","post_history_instructions":"Stay as {{char}}.","tags":["romance","NSFW"]}}"""

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val len = data.size
        out.write(byteArrayOf((len ushr 24).toByte(), (len ushr 16).toByte(), (len ushr 8).toByte(), len.toByte()))
        val td = type.toByteArray(Charsets.ISO_8859_1) + data
        out.write(td)
        val crc = CRC32().apply { update(td) }.value
        out.write(byteArrayOf((crc ushr 24).toByte(), (crc ushr 16).toByte(), (crc ushr 8).toByte(), crc.toByte()))
        return out.toByteArray()
    }

    private fun png(keyword: String, text: String): ByteArray {
        val sig = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        val ihdr = chunk("IHDR", ByteArray(13))
        val txt = chunk("tEXt", (keyword + "\u0000" + text).toByteArray(Charsets.ISO_8859_1))
        return sig + ihdr + txt + chunk("IEND", ByteArray(0))
    }

    @Test fun parsesV2Json() {
        val c = CharacterCard.parse(v2.toByteArray(), "file:rin")
        assertEquals("Rin", c.name)
        assertEquals("Hi {{user}}!", c.firstMessage)
        assertEquals("Stay as {{char}}.", c.postHistoryInstructions)
        assertTrue(c.nsfw)
    }

    @Test fun parsesV1AndPygmalionFields() {
        val v1 = CharacterCard.parseJson("""{"name":"Ash","description":"d","first_mes":"yo"}""", "a")
        assertEquals("yo", v1.firstMessage)
        val pyg = CharacterCard.parseJson("""{"char_name":"Bo","char_persona":"p","char_greeting":"g","world_scenario":"w"}""", "b")
        assertEquals(listOf("Bo", "p", "g", "w"), listOf(pyg.name, pyg.description, pyg.firstMessage, pyg.scenario))
        assertFalse(pyg.nsfw)
    }

    @Test fun readsCardFromPngTextChunk() {
        val b64 = Base64.getEncoder().encodeToString(v2.toByteArray())
        val c = CharacterCard.parse(png("chara", b64), "file:rin")
        assertEquals("Rin", c.name)
    }

    @Test fun prefersV3ChunkAndRejectsPlainPng() {
        val v3 = """{"spec":"chara_card_v3","data":{"name":"Three"}}"""
        val sig = png("chara", Base64.getEncoder().encodeToString(v2.toByteArray()))
        val withV3 = sig.copyOfRange(0, sig.size - 12) +
            chunk("tEXt", ("ccv3\u0000" + Base64.getEncoder().encodeToString(v3.toByteArray())).toByteArray(Charsets.ISO_8859_1)) +
            chunk("IEND", ByteArray(0))
        assertEquals("Three", CharacterCard.parse(withV3, "x").name)
        val failed = runCatching { CharacterCard.parse(png("Software", "GIMP"), "x") }
        assertTrue(failed.exceptionOrNull() is CharacterCard.InvalidCard)
    }

    @Test fun roundTripsTemplate() {
        val back = CharacterCard.parseJson(CharacterCard.toJson(Character.COMPANION), "t")
        assertEquals(Character.COMPANION.name, back.name)
        assertEquals(Character.COMPANION.firstMessage, back.firstMessage)
    }
}

class PromptBuilderTest {
    private val rin = CharacterCard.parseJson(
        """{"name":"Rin","description":"{{char}} likes <USER>.","personality":"kind","first_mes":"Hi {{User}}!"}""", "r",
    )

    @Test fun assistantKeepsGlassesPrompt() =
        assertEquals(Conversation.GLASSES_SYSTEM_PROMPT, PromptBuilder.systemPrompt(Character.ASSISTANT, "Sam", false))

    @Test fun fillsMacrosAndAddsRules() {
        val p = PromptBuilder.systemPrompt(rin, "Sam", matureText = false)
        assertTrue(p.contains("Rin likes Sam."))
        assertTrue(p.contains("Rin's personality: kind"))
        assertTrue(p.contains("safe for work"))
        assertFalse(p.contains("{{"))
        assertTrue(PromptBuilder.systemPrompt(rin, "Sam", matureText = true).contains("aged 18 or over"))
        assertEquals("Hi Sam!", PromptBuilder.greeting(rin, "Sam"))
    }

    @Test fun greetingAndPostHistoryWrapHistory() {
        val conv = Conversation().apply { greeting = "Hi Sam!"; addUser("hello") }
        val msgs = conv.forRequest("sys", "post")
        assertEquals(listOf("system", "assistant", "user", "system"), msgs.map { it.role })
        assertEquals("Hi Sam!", msgs[1].content)
    }
}

class ContentPolicyTest {
    private val adultOn = MatureAccess(adultConfirmed = true, enabled = true, text = true, images = true, video = true)
    private val adult = Character.COMPANION
    private val nsfw = Character.COMPANION.copy(id = "n", nsfw = true)

    @Test fun matureNeedsAdultConfirmation() {
        val unconfirmed = adultOn.copy(adultConfirmed = false)
        assertFalse(unconfirmed.textAllowed || unconfirmed.imagesAllowed || unconfirmed.videoAllowed)
        assertFalse(ContentPolicy.characterVisible(nsfw, unconfirmed))
        assertTrue(ContentPolicy.characterVisible(nsfw, adultOn))
        assertFalse(ContentPolicy.characterVisible(nsfw, adultOn.copy(text = false)))
    }

    @Test fun detectsMinors() {
        listOf("a 15-year-old girl", "she is 16 years old", "loli", "aged 12", "a high-school kid", "14yo").forEach {
            assertTrue(it, ContentPolicy.mentionsMinor(it))
        }
        listOf("a 26-year-old illustrator", "she is 30 years old", "kidding around", "page 5").forEach {
            assertFalse(it, ContentPolicy.mentionsMinor(it))
        }
    }

    @Test fun minorCharactersAlwaysStaySafe() {
        val teen = adult.copy(id = "t", description = "{{char}} is 16 years old.", nsfw = true)
        assertTrue(ContentPolicy.characterRestricted(teen))
        assertFalse(ContentPolicy.textAllowed(teen, adultOn))
        assertFalse(ContentPolicy.characterVisible(teen, adultOn))
        assertTrue(ContentPolicy.imageSafeMode(teen, "portrait", adultOn))
        assertFalse(ContentPolicy.textAllowed(adult.copy(description = "a 26-year-old"), MatureAccess()))
    }

    @Test fun imageSafeModeOnlyOffWhenAllowed() {
        assertTrue(ContentPolicy.imageSafeMode(adult, "portrait", MatureAccess()))
        assertTrue(ContentPolicy.imageSafeMode(adult, "portrait", adultOn.copy(images = false)))
        assertFalse(ContentPolicy.imageSafeMode(adult, "portrait", adultOn))
        assertTrue(ContentPolicy.imageSafeMode(adult, "portrait of a child", adultOn))
    }
}

class ImageIntentTest {
    @Test fun recognisesRequests() {
        assertEquals("a red fox", ImageIntent.parse("Draw a red fox.")?.prompt)
        assertEquals("the beach at sunset", ImageIntent.parse("Show me a picture of the beach at sunset")?.prompt)
        assertTrue(ImageIntent.parse("send me a selfie")!!.ofCharacter)
        assertTrue(ImageIntent.parse("What do you look like?")!!.ofCharacter)
        assertNotNull(ImageIntent.parse("picture of a cat"))
    }

    @Test fun ignoresOrdinaryQuestions() {
        assertNull(ImageIntent.parse("Will it rain before my 6 pm dinner?"))
        assertNull(ImageIntent.parse("How do I make pasta"))
        assertNull(ImageIntent.parse("show me"))
    }

    @Test fun addsCharacterLooksToSelfPictures() {
        val p = ImageIntent.prompt(ImageIntent.parse("send me a selfie")!!, Character.COMPANION, "Sam")
        assertTrue(p.contains("Mira, an adult"))
        assertTrue(p.contains("copper-orange hair"))
        assertEquals("a red fox", ImageIntent.prompt(ImageIntent.parse("draw a red fox")!!, Character.COMPANION, "Sam"))
    }
}

class CreditsTest {
    @Test fun classifiesLicences() {
        assertEquals(Credits.LicenseKind.PERMISSIVE, Credits.kindOf("Apache-2.0"))
        assertEquals(Credits.LicenseKind.COPYLEFT, Credits.kindOf("GPL-3.0"))
        assertEquals(Credits.LicenseKind.WEAK_COPYLEFT, Credits.kindOf("EPL-1.0"))
        assertEquals(Credits.LicenseKind.CUSTOM, Credits.kindOf("NOASSERTION"))
        assertEquals(Credits.LicenseKind.NONE, Credits.kindOf("NONE"))
    }

    @Test fun flagsTheUnlicensedSdkAndCreditsCommunity() {
        val c = Credits
        assertTrue(c.unlicensedDependencies.any { it.name.contains("Mercury") })
        assertEquals(c.community.size, c.community.map { it.repo }.toSet().size)
        assertTrue(c.community.any { it.repo == "Dun-John/x3bluetooth" && it.kind == Credits.LicenseKind.PERMISSIVE })
    }
}

class CardStoresTest {
    private val s = dev.sweitz.veniceglasses.data.CardStores

    @Test fun resolvesStorePageLinks() {
        assertEquals(listOf("https://avatars.charhub.io/avatars/someone/my-card/chara_card_v2.png"),
            s.downloadUrls("https://chub.ai/characters/someone/my-card"))
        assertEquals(listOf("https://cards.character-tavern.com/count-six/hybrid_narrator.png?action=download"),
            s.downloadUrls("https://character-tavern.com/character/count-six/hybrid_narrator"))
        assertEquals(listOf("https://example.com/cards/Rin.png"), s.downloadUrls("https://example.com/cards/Rin.png"))
    }

    @Test fun rejectsUnsupportedLinks() {
        listOf("https://janitorai.com/characters/abc", "ftp://x.com/a.png", "https://example.com/page", "https://chub.ai/users/x")
            .forEach { assertTrue(it, runCatching { s.downloadUrls(it) }.exceptionOrNull() is dev.sweitz.veniceglasses.data.CardStores.UnsupportedLink) }
    }

    @Test fun namesSavedFilesSafely() {
        assertEquals("my-card.png", s.fileName("https://chub.ai/characters/someone/my-card", isPng = true))
        assertEquals("Rin.json", s.fileName("https://example.com/c/Rin.json", isPng = false))
    }

    @Test fun refusesMatureCardsInvolvingMinors() {
        val bad = Character.COMPANION.copy(id = "b", nsfw = true, description = "{{char}} is 15 years old")
        assertTrue(ContentPolicy.importRefused(bad))
        assertFalse(ContentPolicy.importRefused(bad.copy(nsfw = false))) // SFW card: kept, stays SFW
        assertFalse(ContentPolicy.importRefused(Character.COMPANION.copy(nsfw = true)))
    }
}
