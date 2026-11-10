// app/src/main/kotlin/com/m8/keyboard/QwertyKeypadView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/** A plain full QWERTY keyboard (letters page and numbers/symbols page) with a T9 switch key. */
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
        fun onQwertyEnter()
        fun onQwertyShift()
        fun onQwertyT9()
    }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val letterKeys = ArrayList<Pair<TextView, Char>>()
    private var shiftKey: TextView? = null
    private var shiftLabel = "abc"
    private var showUpper = false
    private var symbolsPage = false

    private val repeatBackspace = object : Runnable {
        override fun run() {
            listener.onQwertyBackspace()
            uiHandler.postDelayed(this, 60L)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        setPadding(dp(4), dp(4), dp(4), dp(4))
        buildPage()
    }

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
            val shift = makeKey(shiftLabel, 13f)
            shift.setOnClickListener {
                settings.feedback(it)
                listener.onQwertyShift()
            }
            shiftKey = shift
            addKeysRow("zxcvbnm", isLetter = true, leading = shift, trailing = makeDelKey())
        } else {
            addKeysRow("1234567890", isLetter = false)
            addKeysRow("@#\$%&-+()/", isLetter = false)
            addKeysRow("*\"':;!?", isLetter = false, trailing = makeDelKey())
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
        if (pad > 0f) row.addView(Space(context), LinearLayout.LayoutParams(0, 0).apply { weight = pad })
        if (leading != null) row.addView(leading, keyParams(1.5f))
        for (ch in chars) {
            val key = makeKey(if (isLetter) shown(ch) else ch.toString())
            key.setOnClickListener {
                settings.feedback(it)
                listener.onQwertyText(ch.toString(), isLetter)
            }
            if (isLetter) letterKeys.add(Pair(key, ch))
            row.addView(key, keyParams())
        }
        if (trailing != null) row.addView(trailing, keyParams(1.5f))
        if (pad > 0f) row.addView(Space(context), LinearLayout.LayoutParams(0, 0).apply { weight = pad })
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
        val t9 = makeKey("T9", 14f)
        t9.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyT9()
        }
        val comma = makeKey(",")
        comma.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyText(",", false)
        }
        val space = makeKey("space", 14f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onQwertySpace()
        }
        val period = makeKey(".")
        period.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyText(".", false)
        }
        val enter = makeKey("ENTER", 13f)
        enter.setOnClickListener {
            settings.feedback(it)
            listener.onQwertyEnter()
        }

        row.addView(page, keyParams(1.4f))
        row.addView(t9, keyParams(1.2f))
        row.addView(comma, keyParams(1f))
        row.addView(space, keyParams(4f))
        row.addView(period, keyParams(1f))
        row.addView(enter, keyParams(1.6f))
        addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    private fun makeDelKey(): TextView {
        val del = makeKey("DEL", 13f)
        del.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    settings.feedback(v)
                    listener.onQwertyBackspace()
                    uiHandler.postDelayed(repeatBackspace, 400L)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    uiHandler.removeCallbacks(repeatBackspace)
                }
            }
            true
        }
        return del
    }

    // ---- helpers ----

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun newRow(): LinearLayout {
        val r = LinearLayout(context)
        r.orientation = HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        return r
    }

    private fun keyParams(weight: Float = 1f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(settings.keyHeightDp)).apply {
            this.weight = weight
            setMargins(dp(2), dp(3), dp(2), dp(3))
        }

    private fun makeKey(label: CharSequence, size: Float = 20f): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.textSize = size
        tv.gravity = Gravity.CENTER
        tv.setTextColor(theme.text)
        tv.background = keyBackground()
        return tv
    }

    private fun keyBackground(): StateListDrawable {
        fun shape(color: Int) = GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(8).toFloat()
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(theme.keyPressed))
            addState(intArrayOf(), shape(theme.key))
        }
    }
}
