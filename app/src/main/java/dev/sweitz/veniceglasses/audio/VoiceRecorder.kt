package dev.sweitz.veniceglasses.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records one spoken question to an AAC .m4a file (a format Venice speech-to-text accepts).
 * The clip lives in the app's private cache and is deleted after it's transcribed.
 */
class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var startedAt = 0L

    val file: File get() = File(context.cacheDir, "utterance.m4a")
    val isRecording: Boolean get() = recorder != null

    /** Starts recording. Stops automatically after [maxMs] and calls [onAutoStop]. */
    fun start(maxMs: Int = 60_000, onAutoStop: () -> Unit = {}) {
        stop(discard = true)
        file.delete()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        try {
            r.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioChannels(1)
            r.setAudioSamplingRate(16_000)
            r.setAudioEncodingBitRate(48_000)
            r.setMaxDuration(maxMs)
            r.setOutputFile(file.absolutePath)
            r.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onAutoStop()
            }
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            throw e
        }
        recorder = r
        startedAt = System.currentTimeMillis()
    }

    /**
     * Stops recording. Returns the clip, or null if it was discarded, too short to contain
     * speech, or the recorder failed.
     */
    fun stop(discard: Boolean = false): File? {
        val r = recorder ?: return null
        recorder = null
        val longEnough = System.currentTimeMillis() - startedAt >= MIN_MS
        val ok = runCatching { r.stop() }.isSuccess // throws if stopped before any audio arrived
        r.release()
        return if (!discard && ok && longEnough && file.length() > 0) file else {
            file.delete()
            null
        }
    }

    companion object {
        const val MIME_TYPE = "audio/mp4"
        private const val MIN_MS = 400L
    }
}
