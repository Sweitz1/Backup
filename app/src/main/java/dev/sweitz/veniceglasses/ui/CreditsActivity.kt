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
import dev.sweitz.veniceglasses.R
import dev.sweitz.veniceglasses.data.Credits
import dev.sweitz.veniceglasses.databinding.ActivityModelPickerBinding
import kotlinx.coroutines.launch

/**
 * Credits & licences: what VeniceX3 is built with (unlicensed dependencies flagged), then the
 * RayNeo X3 Pro community projects, credited with the licence GitHub reports for each.
 * Swipe to scroll, double tap to go back.
 */
class CreditsActivity : BaseMirrorActivity<ActivityModelPickerBinding>() {
    private data class Row(val text: String, val header: Boolean = false, val warn: Boolean = false)

    private val lines: List<Row> = buildList {
        add(Row("VeniceX3 is built with", header = true))
        Credits.dependencies.forEach { d ->
            add(Row("${d.name}: ${d.license}", warn = d.kind == Credits.LicenseKind.NONE))
        }
        if (Credits.unlicensedDependencies.isNotEmpty()) {
            add(Row("⚠ No stated licence", header = true, warn = true))
            Credits.unlicensedDependencies.forEach { add(Row(it.note, warn = true)) }
        }
        add(Row("X3 Pro community (credit only, no code used)", header = true))
        Credits.community.forEach { p -> add(Row("${p.repo}: ${p.kind.label} (${p.spdx})")) }
    }
    private var top = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                templeActionViewModel.state.collect(::onTemple)
            }
        }
    }

    private fun onTemple(action: TempleAction) {
        when (action) {
            is TempleAction.SlideForward, is TempleAction.SlideDownwards -> scroll(+VISIBLE_ROWS - 1)
            is TempleAction.SlideBackward, is TempleAction.SlideUpwards -> scroll(-(VISIBLE_ROWS - 1))
            is TempleAction.DoubleClick -> finish()
            else -> Unit
        }
    }

    private fun scroll(delta: Int) {
        top = (top + delta).coerceIn(0, (lines.size - VISIBLE_ROWS).coerceAtLeast(0))
        render()
    }

    private fun render() {
        val textColor = ContextCompat.getColor(this, R.color.text)
        val accent = ContextCompat.getColor(this, R.color.accent)
        val warn = ContextCompat.getColor(this, R.color.mature)
        val pad = (6 * resources.displayMetrics.density).toInt()
        mBindingPair.updateView {
            tvTitle.text = getString(R.string.credits_title)
            tvHint.text = getString(R.string.credits_hint)
            rows.removeAllViews()
            lines.drop(top).take(VISIBLE_ROWS).forEach { r ->
                rows.addView(TextView(this@CreditsActivity).apply {
                    this.text = r.text
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, if (r.header) 15f else 13f)
                    setTextColor(if (r.warn) warn else if (r.header) accent else textColor)
                    setPadding(pad, pad / 2, pad, pad / 2)
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            }
        }
    }

    private companion object {
        const val VISIBLE_ROWS = 8
    }
}
