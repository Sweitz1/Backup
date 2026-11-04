package dev.sweitz.veniceglasses.ui

import android.os.Bundle
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
import dev.sweitz.veniceglasses.data.ModelInfo
import dev.sweitz.veniceglasses.data.Settings
import dev.sweitz.veniceglasses.databinding.ActivityModelPickerBinding
import kotlinx.coroutines.launch

/**
 * Menu: two settings toggles, then every Venice text model available to the key.
 * Swipe to move, tap to choose, double tap to go back.
 */
class ModelPickerActivity : BaseMirrorActivity<ActivityModelPickerBinding>() {
    private sealed interface Item {
        data object ReadAloud : Item
        data object WebSearch : Item
        data class Model(val info: ModelInfo) : Item
    }

    private val settings by lazy { app.settings }
    private var items: List<Item> = listOf(Item.ReadAloud, Item.WebSearch)
    private var focus = 0
    private var loading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()

        lifecycleScope.launch {
            val models = runCatching { app.venice.listTextModels() }
            loading = false
            models.onSuccess { list ->
                val current = settings.model
                val withCurrent = if (list.none { it.id == current }) listOf(ModelInfo(current, current)) + list else list
                items = listOf(Item.ReadAloud, Item.WebSearch) + withCurrent.map { Item.Model(it) }
                focus = items.indexOfFirst { it is Item.Model && it.info.id == current }.coerceAtLeast(0)
            }.onFailure { FToast.show("Couldn't load models: ${it.message}") }
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
        focus = (focus + delta).coerceIn(0, items.lastIndex)
        render()
    }

    private fun select() {
        when (val item = items.getOrNull(focus) ?: return) {
            Item.ReadAloud -> {
                settings.readAloud = !settings.readAloud
                FToast.show(getString(if (settings.readAloud) R.string.toast_speech_on else R.string.toast_speech_off))
                render()
            }
            Item.WebSearch -> {
                val modes = Settings.WEB_SEARCH_MODES
                settings.webSearch = modes[(modes.indexOf(settings.webSearch) + 1) % modes.size]
                render()
            }
            is Item.Model -> {
                settings.model = item.info.id
                FToast.show(item.info.name)
                finish()
            }
        }
    }

    private fun label(item: Item): String = when (item) {
        Item.ReadAloud -> getString(if (settings.readAloud) R.string.picker_read_aloud_on else R.string.picker_read_aloud_off)
        Item.WebSearch -> getString(R.string.picker_web_search, settings.webSearch.uppercase())
        is Item.Model -> (if (item.info.id == settings.model) "● " else "   ") + item.info.name
    }

    /** Shows a window of rows around the focused item, identically in both eyes. */
    private fun render() {
        val window = VISIBLE_ROWS
        val start = (focus - window / 2).coerceIn(0, (items.size - window).coerceAtLeast(0))
        val visible = items.drop(start).take(window)
        val textColor = ContextCompat.getColor(this, R.color.text)
        val accent = ContextCompat.getColor(this, R.color.accent)
        val focusBg = ContextCompat.getColor(this, R.color.focus_bg)
        val pad = (10 * resources.displayMetrics.density).toInt()

        mBindingPair.updateView {
            tvTitle.text = if (loading) getString(R.string.picker_loading) else getString(R.string.picker_title)
            rows.removeAllViews()
            visible.forEachIndexed { i, item ->
                val focused = start + i == focus
                rows.addView(TextView(this@ModelPickerActivity).apply {
                    this.text = label(item)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setTextColor(if (focused) accent else textColor)
                    if (focused) setBackgroundColor(focusBg)
                    setPadding(pad, pad / 2, pad, pad / 2)
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private companion object {
        const val VISIBLE_ROWS = 6
    }
}
