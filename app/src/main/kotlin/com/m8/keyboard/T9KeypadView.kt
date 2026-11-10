// app/src/main/kotlin/com/m8/keyboard/T9KeypadView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import t9.T9Keymap

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/**
 * The number pad: [QWERTY] [candidate strip] [DEL] [ENTER] on top, a 4x3 key grid below.
 * The grid can be swapped for a symbol picker with [showSymbols].
 */
class T9KeypadView(
    context: Context,
    private val listener: Listener,
    private val settings: KeyboardSettings,
    private val theme: KeyboardTheme,
) : LinearLayout(context) {

    interface Listener {
        fun onKeypadKey(key: Char)
        fun onKeypadLongKey(key: Char)
        fun onKeypadBackspace()
        fun onKeypadEnter()
        fun onKeypadCandidate(index: Int)
        fun onKeypadSymbol(symbol: String)
        fun onKeypadQwerty()
    }

    private val uiHandler = Handler(Looper.getMainLooper())
    private val strip = LinearLayout(context)
    private val scroll = HorizontalScrollView(context)
    private val grid = LinearLayout(context)
    private val symbolPanel = LinearLayout(context)
    private lateinit var hashKey: TextView

    private val repeatBackspace = object : Runnable {
        override fun run() {
            listener.onKeypadBackspace()
            uiHandler.postDelayed(this, 60L)
        }
    }

    init {
        orientation = VERTICAL
        setBackgroundColor(theme.background)
        setPadding(dp(4), dp(4), dp(4), dp(4))

        // --- top bar: QWERTY | candidates | DEL | ENTER ---
        strip.orientation = HORIZONTAL
        scroll.isHorizontalScrollBarEnabled = false
        scroll.setBackgroundColor(theme.strip)
        scroll.addView(strip, FrameLayout.LayoutParams(WRAP, MATCH))

        val qwerty = makeKey("QWERTY", 11f)
        qwerty.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadQwerty()
        }

        val del = makeKey("DEL", 14f)
        del.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    settings.feedback(v)
                    listener.onKeypadBackspace()
                    uiHandler.postDelayed(repeatBackspace, 400L)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    uiHandler.removeCallbacks(repeatBackspace)
                }
            }
            true
        }

        val enter = makeKey("ENTER", 14f)
        enter.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadEnter()
        }

        val top = newRow()
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(qwerty, LinearLayout.LayoutParams(dp(56), dp(44)).apply { setMargins(0, 0, dp(3), 0) })
        top.addView(scroll, LinearLayout.LayoutParams(0, dp(48)).apply { weight = 1f })
        top.addView(del, LinearLayout.LayoutParams(dp(56), dp(44)).apply { setMargins(dp(3), 0, dp(3), 0) })
        top.addView(enter, LinearLayout.LayoutParams(dp(64), dp(44)))
        addView(top, LinearLayout.LayoutParams(MATCH, WRAP))

        // --- 4x3 number grid ---
        grid.orientation = VERTICAL
        for (rowKeys in listOf("123", "456", "789", "*0#")) {
            val r = newRow()
            for (k in rowKeys) {
                val key = makeKey(labelFor(k))
                key.setOnClickListener {
                    settings.feedback(it)
                    listener.onKeypadKey(k)
                }
                key.setOnLongClickListener {
                    settings.feedback(it, isLong = true)
                    listener.onKeypadLongKey(k)
                    true
                }
                if (k == '#') hashKey = key
                r.addView(key, keyParams())
            }
            grid.addView(r, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        addView(grid, LinearLayout.LayoutParams(MATCH, WRAP))

        // --- symbol picker (hidden until showSymbols(true)) ---
        symbolPanel.orientation = VERTICAL
        symbolPanel.visibility = GONE
        val symbolRows = listOf(".,?!'\":;", "-_()/\\@#", "\$%&*+=<>", "[]{}~^|\u20AC")
        for (rowSymbols in symbolRows) {
            val r = newRow()
            for (ch in rowSymbols) {
                val key = makeKey(ch.toString())
                key.setOnClickListener {
                    settings.feedback(it)
                    listener.onKeypadSymbol(ch.toString())
                }
                r.addView(key, keyParams())
            }
            symbolPanel.addView(r, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        val bottom = newRow()
        val back = makeKey("ABC", 16f)
        back.setOnClickListener {
            settings.feedback(it)
            showSymbols(false)
        }
        val space = makeKey("space", 16f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadKey('0')
        }
        bottom.addView(back, keyParams(1f))
        bottom.addView(space, keyParams(3f))
        symbolPanel.addView(bottom, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(symbolPanel, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    /** Swaps the number grid for the symbol picker (or back). */
    fun showSymbols(show: Boolean) {
        symbolPanel.visibility = if (show) VISIBLE else GONE
        grid.visibility = if (show) GONE else VISIBLE
    }

    /** Shows candidate words; [selected] is highlighted. Pass an empty list to clear. */
    fun setCandidates(words: List<String>, selected: Int) {
        strip.removeAllViews()
        words.forEachIndexed { i, w ->
            val tv = TextView(context)
            tv.text = w
            tv.textSize = 18f
            tv.gravity = Gravity.CENTER_VERTICAL
            tv.setPadding(dp(12), 0, dp(12), 0)
            if (i == selected) {
                tv.setTextColor(theme.accentText)
                tv.background = GradientDrawable().apply {
                    setColor(theme.accent)
                    cornerRadius = dp(8).toFloat()
                }
            } else {
                tv.setTextColor(theme.text)
            }
            tv.setOnClickListener { listener.onKeypadCandidate(i) }
            strip.addView(tv, LinearLayout.LayoutParams(WRAP, MATCH))
        }
        if (selected in words.indices) {
            val child = strip.getChildAt(selected)
            scroll.post { scroll.smoothScrollTo(maxOf(0, child.left - dp(24)), 0) }
        } else {
            scroll.scrollTo(0, 0)
        }
    }

    /** Text under the # key, e.g. "Abc · T9". */
    fun setModeLabel(label: String) {
        hashKey.text = twoLine("#", label)
    }

    // ---- helpers ----

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun newRow(): LinearLayout {
        val r = LinearLayout(context)
        r.orientation = HORIZONTAL
        return r
    }

    private fun keyParams(weight: Float = 1f): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(settings.keyHeightDp)).apply {
            this.weight = weight
            setMargins(dp(3), dp(3), dp(3), dp(3))
        }

    private fun labelFor(k: Char): CharSequence = when (k) {
        '1' -> twoLine("1", ".,?!")
        '0' -> twoLine("0", "space")
        '*' -> twoLine("*", "next")
        '#' -> twoLine("#", "")
        else -> twoLine(k.toString(), T9Keymap.keyLetters.getValue(k).uppercase())
    }

    private fun twoLine(big: String, small: String): CharSequence {
        if (small.isEmpty()) return big
        val s = SpannableString("$big\n$small")
        s.setSpan(RelativeSizeSpan(0.55f), big.length + 1, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
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
            cornerRadius = dp(10).toFloat()
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(theme.keyPressed))
            addState(intArrayOf(), shape(theme.key))
        }
    }
}
