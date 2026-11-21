package dev.sweitz.veniceglasses.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.sweitz.veniceglasses.app
import dev.sweitz.veniceglasses.audio.Speaker
import dev.sweitz.veniceglasses.audio.VoiceRecorder
import dev.sweitz.veniceglasses.data.Character
import dev.sweitz.veniceglasses.data.ContentPolicy
import dev.sweitz.veniceglasses.data.Conversation
import dev.sweitz.veniceglasses.data.ImageIntent
import dev.sweitz.veniceglasses.data.PromptBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Phase { Idle, Listening, Transcribing, Thinking, Answering, Drawing, Error }

data class ChatUi(
    val phase: Phase = Phase.Idle,
    val question: String = "",
    val reply: String = "",
    val error: String? = null,
    val model: String = "",
    val hasKey: Boolean = true,
    val characterName: String = "Assistant",
    val isAssistant: Boolean = true,
    /** Avatar and video, already filtered by the mature settings. */
    val avatarPath: String? = null,
    val videoPath: String? = null,
    val image: Bitmap? = null,
    val imageSafeMode: Boolean = true,
    val layout: String = "compact",
    val mature: Boolean = false,
    /** Recent microphone levels, 0..1, newest last. */
    val levels: List<Float> = emptyList(),
) {
    val busy: Boolean
        get() = phase == Phase.Transcribing || phase == Phase.Thinking || phase == Phase.Answering || phase == Phase.Drawing
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = application.app.settings
    private val venice = application.app.venice
    private val store = application.app.characters
    private val recorder = VoiceRecorder(application)
    private val speaker = Speaker(application)
    private val conversation = Conversation()
    private var job: Job? = null
    private var meter: Job? = null
    private var character: Character = Character.ASSISTANT

    private val _ui = MutableStateFlow(ChatUi(model = settings.model, hasKey = settings.hasApiKey))
    val ui: StateFlow<ChatUi> = _ui.asStateFlow()

    /** One-off messages for a glasses toast. */
    private val _toasts = MutableStateFlow<String?>(null)
    val toasts: StateFlow<String?> = _toasts.asStateFlow()
    fun toastShown() { _toasts.value = null }

    init { loadCharacter(resetChat = true) }

    /** Call when returning to the screen: settings may have changed over adb or in the menu. */
    fun refreshSettings() {
        loadCharacter(resetChat = resolveCharacter().id != character.id)
    }

    private fun resolveCharacter(): Character {
        val c = store.find(settings.characterId)
        // A mature-only card falls back to the assistant when mature mode is turned off.
        return if (ContentPolicy.characterVisible(c, settings.matureAccess)) c else Character.ASSISTANT
    }

    private fun loadCharacter(resetChat: Boolean) {
        val access = settings.matureAccess
        val c = resolveCharacter()
        character = c
        if (resetChat) {
            cancel()
            conversation.clear()
            conversation.greeting = PromptBuilder.greeting(c, settings.userName)
        }
        _ui.update {
            val base = if (resetChat) ChatUi(reply = conversation.greeting) else it
            base.copy(
                model = settings.model,
                hasKey = settings.hasApiKey,
                characterName = c.name,
                isAssistant = c.isAssistant,
                avatarPath = c.avatarPath?.takeIf { ContentPolicy.showCharacterImage(c, access) },
                videoPath = c.videoPath?.takeIf { ContentPolicy.showCharacterVideo(c, access) },
                layout = settings.layout,
                mature = access.textAllowed || access.imagesAllowed || access.videoAllowed,
            )
        }
    }

    /** Tap: start listening, or stop listening and send. Ignored while an answer is in progress. */
    fun onTap(hasMicPermission: Boolean): Boolean {
        val state = _ui.value
        return when {
            state.phase == Phase.Listening -> { stopListeningAndSend(); true }
            state.busy -> false
            !settings.hasApiKey -> { refreshSettings(); false }
            !hasMicPermission -> false
            else -> { startListening(); true }
        }
    }

    private fun startListening() {
        speaker.stop()
        try {
            recorder.start(onAutoStop = { viewModelScope.launch { stopListeningAndSend() } })
            _ui.update { it.copy(phase = Phase.Listening, error = null, levels = emptyList()) }
            startMeter()
        } catch (e: Exception) {
            fail("Microphone unavailable: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun startMeter() {
        meter?.cancel()
        meter = viewModelScope.launch {
            while (isActive && _ui.value.phase == Phase.Listening) {
                val level = (recorder.amplitude() / 12_000f).coerceIn(0.06f, 1f)
                _ui.update { it.copy(levels = (it.levels + level).takeLast(WAVE_BARS)) }
                delay(70)
            }
        }
    }

    private fun stopListeningAndSend() {
        if (_ui.value.phase != Phase.Listening) return
        meter?.cancel()
        val clip = recorder.stop()
        if (clip == null) {
            _ui.update { it.copy(phase = Phase.Idle) }
            _toasts.value = "Didn't catch that"
            return
        }
        job = viewModelScope.launch {
            _ui.update { it.copy(phase = Phase.Transcribing, error = null) }
            val text = try {
                venice.transcribe(clip, VoiceRecorder.MIME_TYPE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Transcription failed: ${e.message}")
                return@launch
            } finally {
                clip.delete()
            }
            if (text.isBlank()) {
                _ui.update { it.copy(phase = Phase.Idle) }
                _toasts.value = "Didn't catch that"
                return@launch
            }
            ask(text)
        }
    }

    /** Sends [text] as the next user message: a picture request or a chat message. */
    fun ask(text: String) {
        val image = ImageIntent.parse(text)
        if (image != null) draw(text, image) else chat(text)
    }

    private fun chat(text: String) {
        val previous = job // e.g. the transcription job that called us; it's finishing anyway
        job = viewModelScope.launch {
            previous?.cancel()
            conversation.addUser(text)
            _ui.update {
                it.copy(phase = Phase.Thinking, question = text, reply = "", error = null, image = null, model = settings.model)
            }

            val c = character
            val matureText = ContentPolicy.textAllowed(c, settings.matureAccess)
            val system = PromptBuilder.systemPrompt(c, settings.userName, matureText)
            val messages = conversation.forRequest(system, PromptBuilder.postHistory(c, settings.userName))
            val reply = StringBuilder()
            try {
                // Venice's own system prompt would fight a character card, so only the assistant uses it.
                val veniceDefault = c.isAssistant && settings.includeVeniceSystemPrompt
                venice.streamChat(messages, includeVeniceSystemPrompt = veniceDefault).collect { chunk ->
                    reply.append(chunk)
                    _ui.update { it.copy(phase = Phase.Answering, reply = reply.toString().trimStart()) }
                }
                val answer = reply.toString().trim()
                conversation.addAssistant(answer)
                _ui.update { it.copy(phase = Phase.Idle, reply = answer) }
                if (settings.readAloud) speaker.speak(answer.replace(ACTIONS, " ")) // don't read *actions*
            } catch (e: CancellationException) {
                conversation.addAssistant(reply.toString().trim()) // keep what was shown
                conversation.dropDanglingUser()
                throw e
            } catch (e: Exception) {
                conversation.dropDanglingUser()
                fail(e.message ?: "Request failed")
            }
        }
    }

    private fun draw(text: String, req: ImageIntent.Request) {
        val previous = job
        job = viewModelScope.launch {
            previous?.cancel()
            val c = character
            val prompt = ImageIntent.prompt(req, c, settings.userName)
            val safe = ContentPolicy.imageSafeMode(c, prompt, settings.matureAccess)
            _ui.update { it.copy(phase = Phase.Drawing, question = text, reply = "", error = null, image = null) }
            try {
                val bytes = venice.generateImage(prompt, safeMode = safe, negativePrompt = ContentPolicy.ADULT_NEGATIVE_PROMPT)
                val bmp = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    ?: throw IllegalStateException("couldn't decode the picture")
                _ui.update { it.copy(phase = Phase.Idle, image = bmp, imageSafeMode = safe) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fail("Picture failed: ${e.message}")
            }
        }
    }

    /** Double tap while busy: stop listening / generating. */
    fun cancel(): Boolean {
        val state = _ui.value
        if (state.phase == Phase.Listening) {
            meter?.cancel()
            recorder.stop(discard = true)
            _ui.update { it.copy(phase = Phase.Idle) }
            return true
        }
        if (!state.busy) {
            speaker.stop()
            return false
        }
        job?.cancel()
        speaker.stop()
        _ui.update { it.copy(phase = Phase.Idle) }
        return true
    }

    /** Double tap on an answer: clear it from the screen. Returns false if nothing was showing. */
    fun closeAnswer(): Boolean {
        val s = _ui.value
        if (s.image == null && s.question.isEmpty() && s.phase != Phase.Error) return false
        speaker.stop()
        _ui.update { it.copy(question = "", reply = "", image = null, phase = Phase.Idle, error = null) }
        return true
    }

    fun newChat() = loadCharacter(resetChat = true)

    private fun fail(message: String) {
        _ui.update { it.copy(phase = Phase.Error, error = message) }
    }

    override fun onCleared() {
        recorder.stop(discard = true)
        speaker.shutdown()
    }

    companion object {
        const val WAVE_BARS = 32
        private val ACTIONS = Regex("\\*[^*]*\\*")
    }
}
