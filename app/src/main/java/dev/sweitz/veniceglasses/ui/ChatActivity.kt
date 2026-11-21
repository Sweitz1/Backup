package dev.sweitz.veniceglasses.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ffalcon.mercury.android.sdk.touch.TempleAction
import com.ffalcon.mercury.android.sdk.ui.activity.BaseMirrorActivity
import com.ffalcon.mercury.android.sdk.ui.toast.FToast
import dev.sweitz.veniceglasses.R
import dev.sweitz.veniceglasses.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

/**
 * Main screen on the glasses. BaseMirrorActivity inflates [ActivityChatBinding] once per eye;
 * every UI change goes through mBindingPair.updateView so both eyes stay identical.
 *
 * Temple touchpad:
 *   tap          start speaking / stop and send
 *   double tap   stop listening or answering; close the answer; when idle, exit
 *   long press   new chat (the character greets you again)
 *   triple tap   menu (character, mature mode, layout, model, read aloud, web search)
 *   swipe        scroll the answer
 */
class ChatActivity : BaseMirrorActivity<ActivityChatBinding>() {
    private val vm: ChatViewModel by viewModels()
    private var followTail = true // keep the newest text in view while it streams

    // What's currently loaded in the avatar slot, so it's only reloaded when it changes.
    private var shownLayout: String? = null
    private var shownAvatar: String? = null
    private var shownVideo: String? = null
    private var shownImage: Bitmap? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onTap(hasMicPermission = true) else FToast.show(getString(R.string.status_no_mic))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { vm.ui.collect(::render) }
                launch {
                    vm.toasts.collect { msg ->
                        if (msg != null) {
                            FToast.show(msg)
                            vm.toastShown()
                        }
                    }
                }
                launch { templeActionViewModel.state.collect(::onTemple) }
            }
        }
        handleAskIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAskIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.refreshSettings()
        if (shownVideo != null) mBindingPair.updateView { vvAvatar.start() }
    }

    override fun onPause() {
        super.onPause()
        if (shownVideo != null) mBindingPair.updateView { vvAvatar.pause() }
    }

    /** `adb shell am start -n …/.ui.ChatActivity --es ask "question"` — handy for testing without the mic. */
    private fun handleAskIntent(intent: Intent?) {
        val q = intent?.getStringExtra(EXTRA_ASK)?.trim()
        if (!q.isNullOrEmpty()) {
            intent.removeExtra(EXTRA_ASK)
            vm.ask(q)
        }
    }

    private fun onTemple(action: TempleAction) {
        when (action) {
            is TempleAction.Click -> onTap()
            is TempleAction.DoubleClick -> if (!vm.cancel() && !vm.closeAnswer()) finish()
            is TempleAction.LongClick -> {
                vm.newChat()
                followTail = true
                FToast.show(getString(R.string.toast_new_chat))
            }
            is TempleAction.TripleClick -> startActivity(Intent(this, ModelPickerActivity::class.java))
            is TempleAction.SlideForward, is TempleAction.SlideDownwards -> scrollBy(+1)
            is TempleAction.SlideBackward, is TempleAction.SlideUpwards -> scrollBy(-1)
            else -> Unit
        }
    }

    private fun onTap() {
        val ui = vm.ui.value
        if (!ui.hasKey) {
            vm.refreshSettings()
            FToast.show(getString(R.string.status_no_key))
            return
        }
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!vm.onTap(granted) && !granted && ui.phase != Phase.Listening && !ui.busy) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        followTail = true
    }

    /** Scrolls the answer by about two-thirds of a screen in both eyes. */
    private fun scrollBy(direction: Int) {
        mBindingPair.updateView {
            val page = (scroll.height * 0.66f).toInt().coerceAtLeast(1)
            scroll.smoothScrollBy(0, direction * page)
        }
        if (direction < 0) followTail = false
        mBindingPair.left.scroll.post {
            val s = mBindingPair.left.scroll
            if (!s.canScrollVertically(1)) followTail = true
        }
    }

    private fun render(ui: ChatUi) {
        applyLayout(ui.layout)
        applyAvatar(ui.avatarPath, ui.videoPath)

        val status = when {
            !ui.hasKey -> getString(R.string.status_no_key)
            ui.phase == Phase.Listening -> getString(R.string.status_listening)
            ui.phase == Phase.Transcribing -> getString(R.string.status_transcribing)
            ui.phase == Phase.Thinking -> getString(R.string.status_thinking)
            ui.phase == Phase.Answering -> getString(R.string.status_answering)
            ui.phase == Phase.Drawing -> getString(R.string.status_drawing)
            ui.phase == Phase.Error -> ui.error ?: "Error"
            else -> getString(R.string.status_ready)
        }
        val colorRes = when {
            !ui.hasKey || ui.phase == Phase.Error -> R.color.listening
            ui.phase == Phase.Listening || ui.busy -> R.color.accent
            else -> R.color.muted
        }
        val color = ContextCompat.getColor(this, colorRes)
        val showingAnswer = ui.question.isNotEmpty() || ui.image != null
        val reply = ui.reply.ifEmpty {
            if (ui.phase == Phase.Idle && !showingAnswer && ui.isAssistant) getString(R.string.hint_idle) else ""
        }
        val hint = getString(
            when {
                ui.phase == Phase.Listening -> R.string.hint_listening
                ui.busy -> R.string.hint_busy
                showingAnswer -> R.string.hint_answer
                ui.reply.isNotEmpty() -> R.string.hint_greeting // a character's greeting
                else -> R.string.hint_idle
            },
        )
        val question = if (ui.question.isEmpty()) "" else "“${ui.question}”"
        val imageChanged = ui.image !== shownImage
        shownImage = ui.image

        mBindingPair.updateView {
            tvStatus.text = status
            tvStatus.setTextColor(color)
            statusDot.backgroundTintList = ColorStateList.valueOf(color)
            tvName.text = ui.characterName
            tvMature.visibility = if (ui.mature) View.VISIBLE else View.GONE
            tvModel.text = ui.model
            tvQuestion.text = question
            tvQuestion.visibility = if (question.isEmpty() || ui.phase == Phase.Listening) View.GONE else View.VISIBLE
            wave.visibility = if (ui.phase == Phase.Listening) View.VISIBLE else View.GONE
            if (ui.phase == Phase.Listening) wave.setLevels(ui.levels)
            tvReply.text = if (ui.phase == Phase.Listening) "" else reply
            tvReply.visibility = if (tvReply.text.isEmpty()) View.GONE else View.VISIBLE
            if (imageChanged) {
                ivImage.setImageBitmap(ui.image)
                ivImage.visibility = if (ui.image == null) View.GONE else View.VISIBLE
            }
            tvHint.text = hint
            tvHint.visibility = if (!ui.isAssistant || showingAnswer || ui.phase != Phase.Idle) View.VISIBLE else View.GONE
            if (followTail && ui.phase == Phase.Answering) scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            if (ui.phase == Phase.Thinking || ui.phase == Phase.Drawing) scroll.scrollTo(0, 0)
        }
        if (imageChanged && ui.image != null && ui.imageSafeMode && !ui.isAssistant && ui.mature) {
            FToast.show(getString(R.string.image_safe_note))
        }
    }

    /** compact: avatar above the panel · portrait: tall figure on the left, panel on the right. */
    private fun applyLayout(layout: String) {
        if (layout == shownLayout) return
        shownLayout = layout
        val d = resources.displayMetrics.density
        val portrait = layout == "portrait"
        mBindingPair.updateView {
            root.orientation = if (portrait) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            avatarBox.layoutParams = if (portrait) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { marginEnd = (12 * d).toInt() }
            } else {
                LinearLayout.LayoutParams((96 * d).toInt(), (96 * d).toInt()).apply {
                    marginStart = (12 * d).toInt()
                    bottomMargin = (-6 * d).toInt() // tuck the avatar onto the panel's top edge
                }
            }
            panel.layoutParams = if (portrait) {
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.15f)
            } else {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }
        }
    }

    private fun applyAvatar(avatar: String?, video: String?) {
        if (avatar == shownAvatar && video == shownVideo) return
        shownAvatar = avatar
        shownVideo = video
        val bitmap = avatar?.let { decodeAvatar(it) }
        mBindingPair.updateView {
            avatarBox.visibility = if (bitmap != null || video != null) View.VISIBLE else View.GONE
            ivAvatar.setImageBitmap(bitmap)
            ivAvatar.visibility = if (video == null) View.VISIBLE else View.GONE
            if (video != null) {
                vvAvatar.visibility = View.VISIBLE
                vvAvatar.setOnPreparedListener { mp ->
                    mp.isLooping = true
                    mp.setVolume(0f, 0f) // muted: the glasses' speaker is for answers
                    vvAvatar.start()
                }
                vvAvatar.setOnErrorListener { _, _, _ ->
                    vvAvatar.visibility = View.GONE
                    ivAvatar.visibility = View.VISIBLE
                    true
                }
                vvAvatar.setVideoPath(video)
            } else {
                vvAvatar.stopPlayback()
                vvAvatar.visibility = View.GONE
            }
        }
    }

    /** Decodes an avatar at roughly display size, so large card art doesn't use much memory. */
    private fun decodeAvatar(path: String): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (bounds.outHeight / (sample * 2) >= AVATAR_MAX_PX && bounds.outWidth / (sample * 2) >= AVATAR_MAX_PX / 2) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()

    companion object {
        const val EXTRA_ASK = "ask"
        private const val AVATAR_MAX_PX = 900
    }
}
