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
 * Menu: character, layout, mature-mode switches, read aloud and web search, then every Venice
 * text model available to the key. Swipe to move, tap to choose, double tap to go back.
 *
 * The mature switches only appear once the owner has confirmed they're 18+ over adb.
 */
class ModelPickerActivity : BaseMirrorActivity<ActivityModelPickerBinding>() {
    private sealed interface Item {
        data object Character : Item
        data object Layout : Item
        data object Mature : Item
        data object MatureText : Item
        data object MatureImages : Item
        data object MatureVideo : Item
        data object ReadAloud : Item
        data object WebSearch : Item
        data object Credits : Item
        data object CardStores : Item
        data class Model(val info: ModelInfo) : Item
    }

    private val settings by lazy { app.settings }
    private var models: List<ModelInfo> = emptyList()
    private var items: List<Item> = emptyList() // filled in onCreate, once settings can be read
    private var focus = 0
    private var loading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rebuild()

        lifecycleScope.launch {
            val result = runCatching { app.venice.listTextModels() }
            loading = false
            result.onSuccess { list ->
                val current = settings.model
                models = if (list.none { it.id == current }) listOf(ModelInfo(current, current)) + list else list
                rebuild()
            }.onFailure { FToast.show("Couldn't load models: ${it.message}") }
            render()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                templeActionViewModel.state.collect(::onTemple)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        rebuild() // the character may have changed in the character list
    }

    private fun settingItems(): List<Item> = buildList {
        add(Item.Character)
        add(Item.CardStores)
        add(Item.Layout)
        add(Item.Mature)
        if (settings.matureEnabled) {
            add(Item.MatureText)
            add(Item.MatureImages)
            add(Item.MatureVideo)
        }
        add(Item.ReadAloud)
        add(Item.WebSearch)
        add(Item.Credits)
    }

    private fun rebuild() {
        val current = items.getOrNull(focus)
        items = settingItems() + models.map { Item.Model(it) }
        focus = items.indexOf(current).takeIf { it >= 0 } ?: focus.coerceIn(0, items.lastIndex)
        render()
    }

    private fun onOff(v: Boolean) = if (v) "ON" else "OFF"

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
            Item.Character -> startActivity(android.content.Intent(this, CharacterPickerActivity::class.java))
            Item.Layout -> {
                val l = Settings.LAYOUTS
                settings.layout = l[(l.indexOf(settings.layout) + 1) % l.size]
                render()
            }
            Item.Mature -> {
                if (!settings.adultConfirmed) {
                    FToast.show(getString(R.string.picker_mature_locked))
                    return
                }
                settings.matureEnabled = !settings.matureEnabled
                rebuild()
            }
            Item.MatureText -> { settings.matureText = !settings.matureText; render() }
            Item.MatureImages -> { settings.matureImages = !settings.matureImages; render() }
            Item.MatureVideo -> { settings.matureVideo = !settings.matureVideo; render() }
            Item.CardStores -> startActivity(android.content.Intent(this, CardStoresActivity::class.java))
            Item.Credits -> startActivity(android.content.Intent(this, CreditsActivity::class.java))
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
        Item.Character -> getString(R.string.picker_character, app.characters.find(settings.characterId).name)
        Item.Layout -> getString(R.string.picker_layout, settings.layout)
        Item.Mature ->
            if (!settings.adultConfirmed) getString(R.string.picker_mature_locked)
            else getString(R.string.picker_mature, onOff(settings.matureEnabled))
        Item.MatureText -> getString(R.string.picker_mature_text, onOff(settings.matureText))
        Item.MatureImages -> getString(R.string.picker_mature_images, onOff(settings.matureImages))
        Item.MatureVideo -> getString(R.string.picker_mature_video, onOff(settings.matureVideo))
        Item.Credits -> getString(R.string.picker_credits)
        Item.CardStores -> getString(R.string.picker_stores)
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
