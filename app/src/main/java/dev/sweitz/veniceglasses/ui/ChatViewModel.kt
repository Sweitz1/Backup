package dev.sweitz.veniceglasses.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.sweitz.veniceglasses.app
import dev.sweitz.veniceglasses.audio.Speaker
import dev.sweitz.veniceglasses.audio.VoiceRecorder
import dev.sweitz.veniceglasses.data.Conversation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { Idle, Listening, Transcribing, Thinking, Answering, Error }

data class ChatUi(
    val phase: Phase = Phase.Idle,
    val question: String = "",
    val reply: String = "",
    val error: String? = null,
    val model: String = "",
    val hasKey: Boolean = true,
) {
    val busy: Boolean get() = phase == Phase.Transcribing || phase == Phase.Thinking || phase == Phase.Answering
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = application.app.settings
    private val venice = application.app.venice
    private val recorder = VoiceRecorder(application)
    private val speaker = Speaker(application)
    private val conversation = Conversation()
    private var job: Job? = null

    private val _ui = MutableStateFlow(ChatUi(model = settings.model, hasKey = settings.hasApiKey))
    val ui: StateFlow<ChatUi> = _ui.asStateFlow()

    /** One-off messages for a glasses toast. */
    private val _toasts = MutableStateFlow<String?>(null)
    val toasts: StateFlow<String?> = _toasts.asStateFlow()
    fun toastShown() { _toasts.value = null }

    /** Call when returning to the screen: settings may have changed over adb or in the menu. */
    fun refreshSettings() = _ui.update { it.copy(model = settings.model, hasKey = settings.hasApiKey) }

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
            _ui.update { it.copy(phase = Phase.Listening, error = null) }
        } catch (e: Exception) {
            fail("Microphone unavailable: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun stopListeningAndSend() {
        if (_ui.value.phase != Phase.Listening) return
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

    /** Sends [text] as the next user message and streams the answer. Also used for typed/adb input. */
    fun ask(text: String) {
        val previous = job // e.g. the transcription job that called us; it's finishing anyway
        job = viewModelScope.launch {
            previous?.cancel()
            conversation.addUser(text)
            _ui.update { it.copy(phase = Phase.Thinking, question = text, reply = "", error = null, model = settings.model) }

            val reply = StringBuilder()
            try {
                venice.streamChat(conversation.forRequest(Conversation.GLASSES_SYSTEM_PROMPT)).collect { chunk ->
                    reply.append(chunk)
                    _ui.update { it.copy(phase = Phase.Answering, reply = reply.toString().trimStart()) }
                }
                val answer = reply.toString().trim()
                conversation.addAssistant(answer)
                _ui.update { it.copy(phase = Phase.Idle, reply = answer) }
                if (settings.readAloud) speaker.speak(answer)
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

    /** Double tap while busy: stop listening / generating. */
    fun cancel(): Boolean {
        val state = _ui.value
        if (state.phase == Phase.Listening) {
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

    fun newChat() {
        cancel()
        conversation.clear()
        _ui.update { ChatUi(model = settings.model, hasKey = settings.hasApiKey) }
    }

    private fun fail(message: String) {
        _ui.update { it.copy(phase = Phase.Error, error = message) }
    }

    override fun onCleared() {
        recorder.stop(discard = true)
        speaker.shutdown()
    }
}
