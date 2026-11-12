// app/src/main/kotlin/com/m8/keyboard/NumberPadView.kt
package com.m8.keyboard

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import predictive.KeypadMap

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

// Visible gap between keys. The gap is drawn *inside* each key's touch area (see
// KeyboardTheme.keyBackground), so there are no dead zones between keys.
private const val GAP_DP = 3

/**
 * The number pad: [keyboard icon] [candidate strip] [enter] [backspace] on top, followed by
 * (in the RAZR theme: the suggestion screen, then the soft keys and navigation ring), then a 4x3 key grid.
 * The grid can be swapped for a symbol picker with [showSymbols].
 */
class NumberPadView(
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
    private val pulseBackground = PulseBackground(theme.backgroundDrawable(this), theme.accent, settings.pulseStrength)
    private lateinit var hashKey: TextView
    private var candidateWords: List<String> = emptyList()
    private var selectedCandidate: Int = -1

    private val repeatBackspace = object : Runnable {
        override fun run() {
            listener.onKeypadBackspace()
            uiHandler.postDelayed(this, 60L)
        }
    }

    init {
        orientation = VERTICAL
        background = pulseBackground
        setPadding(0, dp(4), 0, 0) // no side padding: the outer keys reach the screen edge

        // --- top bar: QWERTY | candidates | DEL | ENTER ---
        strip.orientation = HORIZONTAL
        scroll.isHorizontalScrollBarEnabled = false
        if (theme.isRazr) scroll.background = RazrDisplayDrawable(density()) else scroll.setBackgroundColor(theme.strip)
        scroll.addView(strip, FrameLayout.LayoutParams(WRAP, MATCH))

        val barHeight = if (theme.isRazr) 44 else 52

        val del = theme.iconKey(context, KeyIcon.BACKSPACE, true, GAP_DP, GAP_DP, 10, "Backspace")
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

        if (theme.isRazr) {
            // The suggestion strip is the phone's screen; below it, the V3's soft keys and
            // navigation ring (keyboard switch, enter, and candidate navigation).
            val screen = newRow()
            screen.gravity = Gravity.CENTER_VERTICAL
            screen.addView(scroll, LinearLayout.LayoutParams(0, dp(barHeight)).apply { weight = 1f })
            screen.addView(del, LinearLayout.LayoutParams(dp(60), dp(barHeight)))
            addView(screen, LinearLayout.LayoutParams(MATCH, WRAP))

            val cluster = RazrControlCluster(
                context,
                settings,
                onQwerty = { listener.onKeypadQwerty() },
                onEnter = { listener.onKeypadEnter() },
                onMove = { delta -> moveCandidate(delta) },
                onSelect = { selectCandidate() },
            )
            addView(cluster, LinearLayout.LayoutParams(MATCH, dp(68)))
        } else {
            val qwerty = theme.iconKey(context, KeyIcon.KEYBOARD, false, GAP_DP, GAP_DP, 10, "Full keyboard")
            qwerty.setOnClickListener {
                settings.feedback(it)
                listener.onKeypadQwerty()
            }

            val enter = theme.iconKey(context, KeyIcon.ENTER, true, GAP_DP, GAP_DP, 10, "Enter")
            enter.setOnClickListener {
                settings.feedback(it)
                listener.onKeypadEnter()
            }

            val top = newRow()
            top.gravity = Gravity.CENTER_VERTICAL
            top.addView(qwerty, LinearLayout.LayoutParams(dp(60), dp(barHeight)))
            top.addView(scroll, LinearLayout.LayoutParams(0, dp(barHeight)).apply { weight = 1f })
            top.addView(enter, LinearLayout.LayoutParams(dp(68), dp(barHeight)))
            top.addView(del, LinearLayout.LayoutParams(dp(60), dp(barHeight)))
            addView(top, LinearLayout.LayoutParams(MATCH, WRAP))
        }

        // --- 4x3 number grid ---
        grid.orientation = VERTICAL
        for ((rowIndex, rowKeys) in listOf("123", "456", "789", "*0#").withIndex()) {
            val r = newRow()
            for ((columnIndex, k) in rowKeys.withIndex()) {
                val key = makeKey(labelFor(k), razrRow = rowIndex, razrColumn = columnIndex)
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
        val back = makeKey("ABC", 16f, action = true)
        back.setOnClickListener {
            settings.feedback(it)
            showSymbols(false)
        }
        val space = makeKey(caps("space"), 16f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadKey('0')
        }
        bottom.addView(back, keyParams(1f))
        bottom.addView(space, keyParams(3f))
        symbolPanel.addView(bottom, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(symbolPanel, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    /** Flashes the background in the accent colour (a character was typed). */
    fun pulse() = pulseBackground.pulse()

    /** Swaps the number grid for the symbol picker (or back). */
    fun showSymbols(show: Boolean) {
        symbolPanel.visibility = if (show) VISIBLE else GONE
        grid.visibility = if (show) GONE else VISIBLE
    }

    /** Shows candidate words; [selected] is highlighted. Pass an empty list to clear. */
    fun setCandidates(words: List<String>, selected: Int) {
        candidateWords = words
        selectedCandidate = selected
        strip.removeAllViews()
        words.forEachIndexed { i, w ->
            val tv = TextView(context)
            tv.text = w
            tv.textSize = 18f
            tv.typeface = theme.typeface
            tv.gravity = Gravity.CENTER
            tv.minWidth = dp(48) // short words like "I" or "a" stay easy to tap
            tv.setPadding(dp(12), 0, dp(12), 0)
            if (i == selected) {
                tv.setTextColor(theme.accentText)
                tv.background = theme.candidateBackground(density())
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

    /** Text under the # key, e.g. "Abc · predict". */
    fun setModeLabel(label: String) {
        hashKey.text = twoLine("#", label)
    }

    // ---- helpers ----

    private fun moveCandidate(delta: Int) {
        if (candidateWords.isEmpty()) return
        val next = (selectedCandidate + delta).coerceIn(0, candidateWords.lastIndex)
        if (next == selectedCandidate) return
        selectedCandidate = next
        setCandidates(candidateWords, next)
        listener.onKeypadCandidate(next)
        settings.feedback(this)
    }

    private fun selectCandidate() {
        if (selectedCandidate in candidateWords.indices) {
            listener.onKeypadCandidate(selectedCandidate)
            settings.feedback(this)
        }
    }

    private fun density(): Float = resources.displayMetrics.density

    private fun dp(v: Int): Int = (v * density()).toInt()

    /** The red and purple themes shout their labels. */
    private fun caps(s: String): String = if (theme.isSlanted) s.uppercase() else s

    private fun newRow(): LinearLayout {
        val r = LinearLayout(context)
        r.orientation = HORIZONTAL
        return r
    }

    private fun keyParams(weight: Float = 1f): LinearLayout.LayoutParams =
        // The cell includes the gap, so neighbouring keys touch and every pixel hits a key.
        LinearLayout.LayoutParams(0, dp(settings.keyHeightDp + 2 * GAP_DP)).apply {
            this.weight = weight
        }

    private fun labelFor(k: Char): CharSequence = when (k) {
        '1' -> twoLine("1", ".,?!")
        '0' -> twoLine("0", caps("space"))
        '*' -> twoLine("*", caps("next"))
        '#' -> twoLine("#", "")
        else -> twoLine(k.toString(), KeypadMap.keyLetters.getValue(k).uppercase())
    }

    private fun twoLine(big: String, small: String): CharSequence {
        if (small.isEmpty()) return big
        val s = SpannableString("$big\n$small")
        s.setSpan(RelativeSizeSpan(0.55f), big.length + 1, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(theme.subText), big.length + 1, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    private fun makeKey(
        label: CharSequence,
        size: Float = 20f,
        action: Boolean = false,
        razrRow: Int = -1,
        razrColumn: Int = -1,
    ): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.textSize = size
        tv.typeface = theme.typeface
        tv.gravity = Gravity.CENTER
        tv.setTextColor(if (action) theme.actionText else theme.text)
        tv.background = theme.keyBackground(
            density(), action, GAP_DP, GAP_DP, 10,
            razrRow = razrRow,
            razrColumn = razrColumn,
        )
        theme.bindKeyGlow(tv)
        return tv
    }
}

