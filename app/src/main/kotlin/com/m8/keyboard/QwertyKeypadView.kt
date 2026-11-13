// app/src/main/kotlin/com/m8/keyboard/QwertyKeypadView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.Canvas
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

// Visible gaps between keys. They are drawn *inside* each key's touch area (see
// KeyboardTheme.keyBackground), so there are no dead zones between keys.
private const val GAP_X_DP = 2
private const val GAP_Y_DP = 3

/** A plain full QWERTY keyboard (letters page and numbers/symbols page) with a key to switch back to the number pad. */
class QwertyKeypadView(
    context: Context,
    private val listener: Listener,
    private val settings: KeyboardSettings,
    private val theme: KeyboardTheme,
) : LinearLayout(context) {

    interface Listener {
        fun onQwertyText(text: String, isLetter: Boolean)
        fun onQwertySpace()
        fun onQwertyBackspace()
        fun onQwertyBackspaceSlide(steps: Int)
        fun onQwertyBackspaceSlideEnd(delete: Boolean)
        fun onQwertyCursorMove(steps: Int)
        fun onQwertyPaste()
        fun onQwertyCopy()
        fun onQwertySettings()
        fun onQwertyEnter()
        fun onQwertyShift()
        fun onQwertyPad()
        fun onQwertyEmoji()
    }

    private val pulseBackground = PulseBackground(theme.backgroundDrawable(this), theme.accent, settings.pulseStrength)
    private val letterKeys = ArrayList<Pair<TextView, Char>>()
    private var shiftKey: TextView? = null
    private var shiftLabel = "abc"
    private var showUpper = false
    private var symbolsPage = false

    init {
        orientation = VERTICAL
        background = pulseBackground
        setPadding(0, dp(4), 0, 0) // no side padding: the outer keys reach the screen edge
        buildPage()
    }

    /** Flashes the background in the accent colour (a character was typed). */
    fun pulse() = pulseBackground.pulse()

    /** [label] is the shift key text (abc / Abc / ABC); [upper] shows capital letters on the keys. */
    fun setCase(label: String, upper: Boolean) {
        shiftLabel = label
        showUpper = upper
        shiftKey?.text = label
        for ((tv, ch) in letterKeys) tv.text = shown(ch)
    }

    private fun shown(ch: Char): String = if (showUpper) ch.uppercaseChar().toString() else ch.toString()

    private fun buildPage() {
        removeAllViews()
        letterKeys.clear()
        shiftKey = null

        if (!symbolsPage) {
            addKeysRow("qwertyuiop", isLetter = true)
            addKeysRow("asdfghjkl", isLetter = true, pad = 0.5f)
            val shift = makeKey(shiftLabel, 13f, action = true)
            shift.setOnClickListener {
                settings.feedback(it)
                listener.onQwertyShift()
            }
            shiftKey = shift
            addKeysRow("zxcvbnm", isLetter = true, leading = shift, trailing = makeEnterKey())
        } else {
            addKeysRow("1234567890", isLetter = false)
            addKeysRow("@#\$%&-+()/", isLetter = false)
            addKeysRow("*\"':;!?", isLetter = false, leading = makeSettingsKey(), trailing = makeEnterKey())
        }
        addBottomRow()
    }

    private fun addKeysRow(
        chars: String,
        isLetter: Boolean,
        leading: View? = null,
        trailing: View? = null,
        pad: Float = 0f,
    ) {
        val row = newRow()
        if (leading != null) row.addView(leading, keyParams(1.5f))
        for ((i, ch) in chars.withIndex()) {
            // An indented row (pad > 0) folds the indent into its first and last key, so the
            // blank space at the row ends still registers taps on the nearest key.
            val first = pad > 0f && i == 0
            val last = pad > 0f && i == chars.lastIndex
            val weight = 1f + (if (first) pad else 0f) + (if (last) pad else 0f)
            val leftFrac = if (first) pad / weight else 0f
            val rightFrac = if (last) pad / weight else 0f
            val key = makeKey(if (isLetter) shown(ch) else ch.toString(), 20f, leftFrac, rightFrac)
            key.setOnClickListener {
                settings.feedback(it)
                listener.onQwertyText(ch.toString(), isLetter)
            }
            if (isLetter) letterKeys.add(Pair(key, ch))
            row.addView(key, keyParams(weight))
        }
        if (trailing != null) row.addView(trailing, keyParams(1.5f))
        addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun addBottomRow() {
        val row = newRow()

        val page = makeKey(if (symbolsPage) "ABC" else "?123", 14f)
        page.setOnClickListener {
            settings.feedback(it)
            symbolsPage = !symbolsPage
            buildPage()
        }
        val pad = makeKey("PAD", 14f)
        pad.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyPad()
        }
        val comma = makeKey(",")
        comma.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyText(",", false)
        }
        val paste = theme.iconKey(context, KeyIcon.PASTE, false, GAP_X_DP, GAP_Y_DP, 8, "Paste")
        paste.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyPaste()
        }
        // Hold paste to copy the selected text.
        paste.setOnLongClickListener {
            settings.feedback(it, isLong = true)
            listener.onQwertyCopy()
            true
        }
        // The symbols page also has the emoji key.
        val emoji = if (symbolsPage) makeKey(";)", 14f) else null
        emoji?.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyEmoji()
        }
        val space = makeKey(if (theme.isSlanted) "SPACE" else "space", 14f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onQwertySpace()
        }
        // Slide along the space key to move the cursor.
        space.setOnTouchListener(SpaceSlideTouch(settings) { listener.onQwertyCursorMove(it) })
        val period = makeKey(".")
        period.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyText(".", false)
        }
        val del = makeDelKey()

        row.addView(page, keyParams(1.4f))
        row.addView(pad, keyParams(1.2f))
        row.addView(paste, keyParams(1.2f))
        if (emoji != null) row.addView(emoji, keyParams(1.2f))
        row.addView(comma, keyParams(1f))
        row.addView(space, keyParams(if (emoji != null) 2.4f else 3.4f))
        row.addView(period, keyParams(1f))
        row.addView(del, keyParams(1.6f))
        addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun makeDelKey(): TextView {
        val del = theme.iconKey(context, KeyIcon.BACKSPACE, true, GAP_X_DP, GAP_Y_DP, 8, "Backspace")
        // Tap or hold to delete; slide left to select text and let go to delete it.
        del.setOnTouchListener(
            BackspaceTouch(
                settings,
                onDelete = { listener.onQwertyBackspace() },
                onSlide = { listener.onQwertyBackspaceSlide(it) },
                onSlideEnd = { listener.onQwertyBackspaceSlideEnd(it) },
            ),
        )
        return del
    }

    private fun makeSettingsKey(): TextView {
        val gear = theme.iconKey(context, KeyIcon.SETTINGS, true, GAP_X_DP, GAP_Y_DP, 8, "Keyboard settings")
        gear.setOnClickListener {
            settings.feedback(it)
            listener.onQwertySettings()
        }
        return gear
    }

    private fun makeEnterKey(): TextView {
        val enter = theme.iconKey(context, KeyIcon.ENTER, true, GAP_X_DP, GAP_Y_DP, 8, "Enter")
        enter.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyEnter()
        }
        return enter
    }

    // ---- helpers ----

    private fun density(): Float = resources.displayMetrics.density

    private fun dp(v: Int): Int = (v * density()).toInt()

    private fun newRow(): LinearLayout {
        val r = LinearLayout(context)
        r.orientation = HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        return r
    }

    private fun keyParams(weight: Float = 1f): LinearLayout.LayoutParams =
        // The cell includes the gap, so neighbouring keys touch and every pixel hits a key.
        LinearLayout.LayoutParams(0, dp(settings.keyHeightDp + 2 * GAP_Y_DP)).apply {
            this.weight = weight
        }

    /**
     * [leftFrac]/[rightFrac] shift the drawn shape inwards for the wide end keys of indented rows;
     * [action] uses the theme's action-key colours (shift, DEL, ENTER).
     */
    private fun makeKey(
        label: CharSequence,
        size: Float = 20f,
        leftFrac: Float = 0f,
        rightFrac: Float = 0f,
        action: Boolean = false,
    ): TextView {
        val tv = KeyTextView(context, leftFrac, rightFrac)
        tv.text = label
        tv.textSize = size
        tv.typeface = theme.typeface
        tv.gravity = Gravity.CENTER
        tv.setTextColor(if (action) theme.actionText else theme.text)
        tv.background = theme.keyBackground(density(), action, GAP_X_DP, GAP_Y_DP, 8, leftFrac, rightFrac)
        theme.bindKeyGlow(tv)
        return tv
    }
}

/**
 * A key label that stays centred inside a key shape that is pulled in from one or both sides
 * (the wide end keys of an indented row), instead of centred in the whole touch cell.
 */
private class KeyTextView(
    context: Context,
    private val leftFrac: Float,
    private val rightFrac: Float,
) : TextView(context) {

    override fun onDraw(canvas: Canvas) {
        val shift = (leftFrac - rightFrac) * width / 2f
        if (shift == 0f) {
            super.onDraw(canvas)
            return
        }
        canvas.save()
        canvas.translate(shift, 0f)
        super.onDraw(canvas)
        canvas.restore()
    }
}
