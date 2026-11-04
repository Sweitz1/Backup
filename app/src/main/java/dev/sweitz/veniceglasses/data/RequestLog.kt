package dev.sweitz.veniceglasses.data

import java.io.File
import java.time.Duration
import java.time.Instant

/**
 * Rolling 24-hour log of API requests, one CSV row per request, using the columns
 * `date,model,type,request_id,status,latency_ms,cost_usd`.
 *
 * Only metadata is logged — never prompts, transcripts or replies.
 * `request_id` is Venice's CF-RAY header (what their support asks for); `cost_usd` is left
 * empty because the API doesn't return a per-request price.
 */
class RequestLog(
    private val file: File,
    private val window: Duration = Duration.ofHours(24),
    private val clock: () -> Instant = Instant::now,
) {
    data class Entry(
        val date: Instant,
        val model: String,
        val type: String, // chat | transcription | models
        val requestId: String?,
        val status: Int, // HTTP status, or 0 if the request never got a response
        val latencyMs: Long,
        val costUsd: Double? = null,
    )

    @Synchronized
    fun record(entry: Entry) {
        val cutoff = clock().minus(window)
        val kept = readRows().filter { row -> parseDate(row)?.isAfter(cutoff) == true }
        file.parentFile?.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeText(buildString {
            appendLine(HEADER)
            kept.forEach { appendLine(it) }
            appendLine(toRow(entry))
        })
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    /** Current log contents (header + rows), e.g. for export. */
    @Synchronized
    fun read(): String = if (file.exists()) file.readText() else HEADER + "\n"

    private fun readRows(): List<String> =
        if (!file.exists()) emptyList()
        else file.readLines().drop(1).filter { it.isNotBlank() }

    private fun parseDate(row: String): Instant? =
        runCatching { Instant.parse(row.substringBefore(',')) }.getOrNull()

    companion object {
        const val HEADER = "date,model,type,request_id,status,latency_ms,cost_usd"

        fun toRow(e: Entry): String = listOf(
            e.date.toString(),
            csv(e.model),
            csv(e.type),
            csv(e.requestId ?: ""),
            e.status.toString(),
            e.latencyMs.toString(),
            e.costUsd?.let { "%.6f".format(java.util.Locale.ROOT, it) } ?: "",
        ).joinToString(",")

        /** RFC 4180 quoting. */
        fun csv(v: String): String =
            if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\"" else v
    }
}
