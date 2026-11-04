package dev.sweitz.veniceglasses

import dev.sweitz.veniceglasses.data.ChatMessage
import dev.sweitz.veniceglasses.data.Conversation
import dev.sweitz.veniceglasses.data.RequestLog
import dev.sweitz.veniceglasses.data.SseParser
import dev.sweitz.veniceglasses.data.ThinkFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant

class SseParserTest {
    private fun parse(vararg lines: String): List<SseParser.Event> {
        val p = SseParser()
        val out = lines.mapNotNull { p.feed(it) }.toMutableList()
        p.finish()?.let { out += it }
        return out
    }

    @Test fun parsesDataEventsAndDone() {
        val events = parse("data: {\"a\":1}", "", ": keep-alive", "", "data: [DONE]", "")
        assertEquals(listOf(SseParser.Event.Data("{\"a\":1}"), SseParser.Event.Done), events)
    }

    @Test fun joinsMultiLineDataAndHandlesCrlf() {
        assertEquals(listOf(SseParser.Event.Data("x\ny")), parse("data: x\r", "data:y\r", "\r"))
    }

    @Test fun flushesFinalEventWithoutBlankLine() {
        assertEquals(listOf(SseParser.Event.Data("last")), parse("data: last"))
    }

    @Test fun ignoresOtherFieldsAndEmptyBlocks() {
        assertEquals(emptyList<SseParser.Event>(), parse("event: ping", "id: 3", "", ""))
    }
}

class ThinkFilterTest {
    private fun run(vararg chunks: String): String {
        val f = ThinkFilter()
        return chunks.joinToString("") { f.push(it) } + f.flush()
    }

    @Test fun passesPlainText() = assertEquals("Hello there", run("Hel", "lo ", "there"))

    @Test fun removesThinkBlock() = assertEquals("Answer: 4", run("<think>2+2…</think>Answer: 4"))

    @Test fun handlesTagsSplitAcrossChunks() =
        assertEquals("Hi!", run("<th", "ink>secret reas", "oning</thi", "nk>Hi", "!"))

    @Test fun keepsLookalikeText() = assertEquals("a < b and <thin ice", run("a < b and <thi", "n ice"))

    @Test fun dropsUnclosedThinking() = assertEquals("ok", run("ok<think>never closed"))
}

class ConversationTest {
    @Test fun buildsRequestWithSystemPrompt() {
        val c = Conversation()
        c.addUser("hi")
        c.addAssistant("hello")
        assertEquals(
            listOf(ChatMessage("system", "S"), ChatMessage("user", "hi"), ChatMessage("assistant", "hello")),
            c.forRequest("S"),
        )
        assertEquals(2, c.forRequest(null).size)
    }

    @Test fun trimsToMaxTurnsAndNeverStartsWithAssistant() {
        val c = Conversation(maxTurns = 2)
        repeat(5) { c.addUser("q$it"); c.addAssistant("a$it") }
        assertEquals(listOf("q3", "a3", "q4", "a4"), c.messages.map { it.content })
        assertEquals("user", c.messages.first().role)
    }

    @Test fun dropsDanglingUserAfterFailure() {
        val c = Conversation()
        c.addUser("q")
        c.dropDanglingUser()
        assertTrue(c.messages.isEmpty())
    }

    @Test fun systemPromptAsksForPlainShortText() =
        assertTrue("markdown" in Conversation.GLASSES_SYSTEM_PROMPT && "80 words" in Conversation.GLASSES_SYSTEM_PROMPT)
}

class RequestLogTest {
    private fun tempFile(): File = Files.createTempDirectory("log").resolve("requests-24h.csv").toFile()

    @Test fun writesHeaderAndRowsInTheCsvColumns() {
        val now = Instant.parse("2026-11-03T23:00:00Z")
        val log = RequestLog(tempFile(), clock = { now })
        log.record(RequestLog.Entry(now, "venice-uncensored-1-2", "chat", "8f1a-IAD", 200, 1234))
        val lines = log.read().trim().lines()
        assertEquals("date,model,type,request_id,status,latency_ms,cost_usd", lines[0])
        assertEquals("2026-11-03T23:00:00Z,venice-uncensored-1-2,chat,8f1a-IAD,200,1234,", lines[1])
    }

    @Test fun keepsOnlyTheLast24Hours() {
        var now = Instant.parse("2026-11-03T00:00:00Z")
        val log = RequestLog(tempFile(), window = Duration.ofHours(24), clock = { now })
        log.record(RequestLog.Entry(now, "m", "chat", null, 200, 1))
        now = now.plus(Duration.ofHours(25))
        log.record(RequestLog.Entry(now, "m", "transcription", null, 500, 2))
        val rows = log.read().trim().lines().drop(1)
        assertEquals(1, rows.size)
        assertTrue(rows[0].contains(",transcription,"))
    }

    @Test fun quotesCsvFieldsAndFormatsCost() {
        val row = RequestLog.toRow(RequestLog.Entry(Instant.EPOCH, "a,b", "chat", "x\"y", 0, 5, 0.0012))
        assertEquals("1970-01-01T00:00:00Z,\"a,b\",chat,\"x\"\"y\",0,5,0.001200", row)
    }

    @Test fun readWithoutFileReturnsHeader() {
        assertEquals(RequestLog.HEADER, RequestLog(tempFile()).read().trim())
        assertNull(null)
    }
}
