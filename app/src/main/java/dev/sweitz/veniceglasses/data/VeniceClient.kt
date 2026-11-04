package dev.sweitz.veniceglasses.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

class VeniceException(message: String, val status: Int = 0) : IOException(message)

data class ModelInfo(val id: String, val name: String)

/**
 * Venice API client (OpenAI-compatible): https://api.venice.ai/api/v1
 * Streaming chat, speech-to-text and the model list. Every request is recorded (metadata only)
 * in [log].
 */
class VeniceClient(
    private val settings: Settings,
    private val log: RequestLog?,
    private val http: OkHttpClient = defaultHttp(),
) {
    /**
     * Streams the assistant's reply as text chunks (reasoning stripped).
     * Cancelling the collector cancels the HTTP call.
     */
    fun streamChat(messages: List<ChatMessage>): Flow<String> = callbackFlow {
        val model = settings.model
        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("messages", JSONArray().apply {
                messages.forEach { put(JSONObject().put("role", it.role).put("content", it.content)) }
            })
            put("venice_parameters", JSONObject().apply {
                put("include_venice_system_prompt", settings.includeVeniceSystemPrompt)
                put("strip_thinking_response", true)
                put("enable_web_search", settings.webSearch)
            })
        }
        val request = newRequest("chat/completions")
            .header("Accept", "text/event-stream")
            .post(body.toString().toRequestBody(JSON))
            .build()

        val call = http.newCall(request)
        val producer = this

        // The read blocks, so it runs in a child coroutine; awaitClose below can then cancel
        // the HTTP call the moment the collector stops (user pressed Stop / new chat).
        launch(Dispatchers.IO) {
            val started = System.nanoTime()
            var status = 0
            var requestId: String? = null
            try {
                call.execute().use { response ->
                    status = response.code
                    requestId = response.header("CF-RAY")
                    if (!response.isSuccessful) throw errorFrom(response)

                    val source = response.body?.source() ?: throw VeniceException("Empty response", status)
                    val sse = SseParser()
                    val think = ThinkFilter()
                    var done = false
                    while (!done) {
                        val line = source.readUtf8Line() ?: break
                        when (val ev = sse.feed(line)) {
                            is SseParser.Event.Data -> {
                                val text = think.push(deltaText(ev.payload))
                                if (text.isNotEmpty()) producer.send(text)
                            }
                            SseParser.Event.Done -> done = true
                            null -> Unit
                        }
                    }
                    (sse.finish() as? SseParser.Event.Data)?.let {
                        think.push(deltaText(it.payload)).takeIf(String::isNotEmpty)?.let { t -> producer.send(t) }
                    }
                    think.flush().takeIf { it.isNotEmpty() }?.let { producer.send(it) }
                }
                producer.close()
            } catch (e: Throwable) {
                producer.close(if (call.isCanceled() || e is CancellationException) null else e)
            } finally {
                logRequest(model, "chat", requestId, status, started)
            }
        }
        awaitClose { call.cancel() }
    }

    /** Transcribes a recorded clip with Venice speech-to-text. */
    suspend fun transcribe(audio: File, mimeType: String): String = withContext(Dispatchers.IO) {
        val model = settings.sttModel
        val form = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart("response_format", "json")
            .addFormDataPart("file", audio.name, audio.asRequestBody(mimeType.toMediaType()))
            .build()
        val json = execute(newRequest("audio/transcriptions").post(form).build(), model, "transcription")
        JSONObject(json).optString("text").trim()
    }

    /** Text models available to this key, for the menu. */
    suspend fun listTextModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        val json = execute(newRequest("models?type=text").get().build(), "-", "models")
        val data = JSONObject(json).optJSONArray("data") ?: JSONArray()
        (0 until data.length()).mapNotNull { i ->
            val m = data.optJSONObject(i) ?: return@mapNotNull null
            val type = m.optString("type", "text")
            if (type != "text") return@mapNotNull null
            val id = m.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = m.optJSONObject("model_spec")?.optString("name")?.takeIf { it.isNotBlank() } ?: id
            ModelInfo(id, name)
        }.sortedBy { it.name.lowercase() }
    }

    private fun execute(request: Request, model: String, type: String): String {
        val started = System.nanoTime()
        var status = 0
        var requestId: String? = null
        try {
            http.newCall(request).execute().use { response ->
                status = response.code
                requestId = response.header("CF-RAY")
                if (!response.isSuccessful) throw errorFrom(response)
                return response.body?.string() ?: throw VeniceException("Empty response", status)
            }
        } finally {
            logRequest(model, type, requestId, status, started)
        }
    }

    private fun newRequest(path: String): Request.Builder {
        val key = settings.apiKey
        if (key.isBlank()) throw VeniceException("No Venice API key set")
        return Request.Builder()
            .url(BASE_URL + path)
            .header("Authorization", "Bearer $key")
            .header("User-Agent", "VeniceX3/0.1 (RayNeo X3 Pro)")
    }

    private fun logRequest(model: String, type: String, requestId: String?, status: Int, startedNanos: Long) {
        val ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos)
        runCatching { log?.record(RequestLog.Entry(Instant.now(), model, type, requestId, status, ms)) }
    }

    companion object {
        const val BASE_URL = "https://api.venice.ai/api/v1/"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // long gaps while a model thinks
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        /** Text from one streamed chunk: choices[0].delta.content (empty if none). */
        fun deltaText(payload: String): String = runCatching {
            val choice = JSONObject(payload).optJSONArray("choices")?.optJSONObject(0) ?: return ""
            val delta = choice.optJSONObject("delta") ?: choice.optJSONObject("message") ?: return ""
            if (delta.isNull("content")) "" else delta.optString("content")
        }.getOrDefault("")

        /** Builds a readable error from Venice's error body: {"error": "..."} or {"error": {"message": "..."}}. */
        fun errorFrom(response: Response): VeniceException {
            val raw = runCatching { response.body?.string() }.getOrNull().orEmpty()
            val msg = runCatching {
                val err = JSONObject(raw).opt("error")
                when (err) {
                    is JSONObject -> err.optString("message")
                    is String -> err
                    else -> JSONObject(raw).optString("message")
                }
            }.getOrNull()?.takeIf { it.isNotBlank() }
            val friendly = when (response.code) {
                401 -> "API key rejected"
                402 -> "Out of Venice credits"
                429 -> "Rate limited, try again shortly"
                else -> null
            }
            return VeniceException(listOfNotNull(friendly, msg).joinToString(": ").ifBlank { "HTTP ${response.code}" }, response.code)
        }
    }
}
