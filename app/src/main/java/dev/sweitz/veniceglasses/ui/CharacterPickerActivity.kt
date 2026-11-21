package dev.sweitz.veniceglasses.ui

import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ffalcon.mercury.android.sdk.touch.TempleAction
import com.ffalcon.mercury.android.sdk.ui.activity.BaseMirrorActivity
import com.ffalcon.mercury.android.sdk.ui.toast.FToast
import dev.sweitz.veniceglasses.R
import dev.sweitz.veniceglasses.app
import dev.sweitz.veniceglasses.data.Character
import dev.sweitz.veniceglasses.databinding.ActivityModelPickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Character list: the assistant, the built-in companion and every imported card that the
 * mature settings allow. Rescans the characters folder each time it opens, so cards pushed
 * with adb show up straight away. Swipe to move, tap to chat, double tap to go back.
 */
class CharacterPickerActivity : BaseMirrorActivity<ActivityModelPickerBinding>() {
    private var characters: List<Character> = emptyList()
    private var focus = 0
    private var loading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()

        lifecycleScope.launch {
            val store = app.characters
            withContext(Dispatchers.IO) { store.all(refresh = true) }
            characters = store.visible(app.settings.matureAccess)
            focus = characters.indexOfFirst { it.id == app.settings.characterId }.coerceAtLeast(0)
            loading = false
            store.lastErrors.firstOrNull()?.let { FToast.show("Skipped $it") }
            render()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                templeActionViewModel.state.collect(::onTemple)
            }
        }
    }

    private fun onTemple(action: TempleAction) {
        when (action) {
            is TempleAction.SlideForward, is TempleAction.SlideDownwards -> move(+1)
            is TempleAction.SlideBackward, is TempleAction.SlideUpwards -> move(-1)
            is TempleAction.Click -> select()
            is TempleAction.DoubleClick -> finish()
            else -> Unit
        }
    }

    private fun move(delta: Int) {
        if (characters.isEmpty()) return
        focus = (focus + delta).coerceIn(0, characters.lastIndex)
        render()
    }

    private fun select() {
        val c = characters.getOrNull(focus) ?: return
        app.settings.characterId = c.id
        FToast.show(c.name)
        finish()
    }

    private fun label(c: Character): String {
        val mark = if (c.id == app.settings.characterId) "● " else "   "
        val badge = if (c.nsfw) "  18+" else ""
        return mark + c.name + badge
    }

    private fun render() {
        val window = VISIBLE_ROWS
        val start = (focus - window / 2).coerceIn(0, (characters.size - window).coerceAtLeast(0))
        val visible = characters.drop(start).take(window)
        val textColor = ContextCompat.getColor(this, R.color.text)
        val accent = ContextCompat.getColor(this, R.color.accent)
        val focusBg = ContextCompat.getColor(this, R.color.focus_bg)
        val pad = (10 * resources.displayMetrics.density).toInt()

        mBindingPair.updateView {
            tvTitle.text = getString(if (loading) R.string.picker_loading else R.string.characters_title)
            tvHint.text = getString(R.string.characters_hint)
            rows.removeAllViews()
            visible.forEachIndexed { i, c ->
                val focused = start + i == focus
                rows.addView(TextView(this@CharacterPickerActivity).apply {
                    text = label(c)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTextColor(if (focused) accent else textColor)
                    if (focused) setBackgroundColor(focusBg)
                    setPadding(pad, pad / 2, pad, pad / 2)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private companion object {
        const val VISIBLE_ROWS = 6
    }
}
