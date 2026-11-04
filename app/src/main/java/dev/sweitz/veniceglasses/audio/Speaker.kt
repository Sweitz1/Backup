package dev.sweitz.veniceglasses.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Reads answers aloud with the on-device text-to-speech engine (nothing leaves the glasses). */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    private var queued: String? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.getDefault()
            queued?.let { speak(it) }
        }
        queued = null
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            queued = text
            return
        }
        // Long answers are split so each piece stays under the engine's input limit.
        val max = TextToSpeech.getMaxSpeechInputLength() - 1
        text.chunked(max).forEachIndexed { i, part ->
            tts.speak(part, if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "answer-$i")
        }
    }

    fun stop() {
        queued = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
