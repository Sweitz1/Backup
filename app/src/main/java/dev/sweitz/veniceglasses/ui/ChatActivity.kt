package dev.sweitz.veniceglasses.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
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
 *   double tap   stop listening or answering; when idle, exit
 *   long press   new chat
 *   triple tap   menu (model, read aloud, web search)
 *   swipe        scroll the answer
 */
class ChatActivity : BaseMirrorActivity<ActivityChatBinding>() {
    private val vm: ChatViewModel by viewModels()
    private var followTail = true // keep the newest text in view while it streams

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
            is TempleAction.DoubleClick -> if (!vm.cancel()) finish()
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
        val status = when {
            !ui.hasKey -> getString(R.string.status_no_key)
            ui.phase == Phase.Listening -> getString(R.string.status_listening)
            ui.phase == Phase.Transcribing -> getString(R.string.status_transcribing)
            ui.phase == Phase.Thinking -> getString(R.string.status_thinking)
            ui.phase == Phase.Answering -> getString(R.string.status_answering)
            ui.phase == Phase.Error -> ui.error ?: "Error"
            else -> getString(R.string.status_ready)
        }
        val colorRes = when {
            !ui.hasKey || ui.phase == Phase.Error || ui.phase == Phase.Listening -> R.color.listening
            ui.busy -> R.color.accent
            else -> R.color.muted
        }
        val color = ContextCompat.getColor(this, colorRes)
        val reply = ui.reply.ifEmpty { if (ui.phase == Phase.Idle && ui.question.isEmpty()) getString(R.string.hint_idle) else "" }

        mBindingPair.updateView {
            tvStatus.text = status
            tvStatus.setTextColor(color)
            statusDot.backgroundTintList = ColorStateList.valueOf(color)
            tvModel.text = ui.model
            tvQuestion.text = ui.question
            tvQuestion.visibility = if (ui.question.isEmpty()) View.GONE else View.VISIBLE
            tvReply.text = reply
            tvHint.visibility = if (ui.reply.isEmpty() && ui.question.isEmpty()) View.GONE else View.VISIBLE
            if (followTail && ui.phase == Phase.Answering) scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
            if (ui.phase == Phase.Thinking) scroll.scrollTo(0, 0)
        }
    }

    companion object {
        const val EXTRA_ASK = "ask"
    }
}
