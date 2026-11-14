// app/src/main/kotlin/com/m8/keyboard/NumberPadView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.Gravity
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
 * The number pad: [keyboard icon] [candidate strip] [paste] [enter] [backspace] on top, followed by
 * a 4x3 key grid.
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
        fun onKeypadBackspaceSlide(steps: Int)
        fun onKeypadBackspaceSlideEnd(delete: Boolean)
        fun onKeypadEnter()
        fun onKeypadCursorMove(steps: Int, startDirection: Int)
        fun onKeypadPaste()
        fun onKeypadCopy()
        fun onKeypadPasteSlide(steps: Int, startDirection: Int)
        fun onKeypadSettings()
        fun onKeypadCandidate(index: Int)
        fun onKeypadCandidateLong(index: Int)
        fun onKeypadSymbol(symbol: String)
        fun onKeypadQwerty()
        fun onKeypadEmoji()
    }

    private val strip = LinearLayout(context)
    private val scroll = HorizontalScrollView(context)
    private val grid = LinearLayout(context)
    private val symbolPanel = LinearLayout(context)
    private val pulseBackground: Drawable = if (theme.isGlitter) GlitterPulseBackground(theme.backgroundDrawable(this), settings.pulseStrength, resources.displayMetrics.density) else PulseBackground(theme.backgroundDrawable(this), theme.accent, settings.pulseStrength)
    private lateinit var hashKey: TextView
    private var candidateWords: List<String> = emptyList()
    private var selectedCandidate: Int = -1
    private val letterKeys = ArrayList<Pair<TextView, Char>>()
    private var upperLetters = false // the letters on the keys follow the case the next letter will have

    init {
        orientation = VERTICAL
        background = pulseBackground
        if (theme.isGlitter) foreground = GlitterTwinkleDrawable(this, resources.displayMetrics.density, settings.pulseStrength)
        setPadding(0, dp(4), 0, 0) // no side padding: the outer keys reach the screen edge

        // --- top bar: QWERTY | candidates | PASTE | ENTER | DEL ---
        strip.orientation = HORIZONTAL
        scroll.isHorizontalScrollBarEnabled = false
        scroll.background = when {
            theme.isGlitter -> GlitterPatternDrawable(theme.strip, context.resources.displayMetrics.density, stripMode = true)
            theme.isGameBoyAdvance -> PixelScreenDrawable(theme.strip, context.resources.displayMetrics.density)
            else -> android.graphics.drawable.ColorDrawable(theme.strip)
        }
        scroll.addView(strip, FrameLayout.LayoutParams(WRAP, MATCH))

        val barHeight = 52

        val del = theme.iconKey(context, KeyIcon.BACKSPACE, true, GAP_DP, GAP_DP, 10, "Backspace")
        // Tap or hold to delete; slide left to select text and let go to delete it.
        del.setOnTouchListener(
            BackspaceTouch(
                settings,
                onDelete = { listener.onKeypadBackspace() },
                onSlide = { listener.onKeypadBackspaceSlide(it) },
                onSlideEnd = { listener.onKeypadBackspaceSlideEnd(it) },
            ),
        )

        val paste = theme.iconKey(context, KeyIcon.PASTE, false, GAP_DP, GAP_DP, 10, "Paste")
        // Tap to paste. Hold and let go to copy the selection. Hold and slide to select from the cursor.
        paste.setOnTouchListener(
            PasteTouch(
                settings,
                onPaste = { listener.onKeypadPaste() },
                onCopy = { listener.onKeypadCopy() },
                onSelect = { steps, dir -> listener.onKeypadPasteSlide(steps, dir) },
            ),
        )

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
        top.addView(qwerty, LinearLayout.LayoutParams(dp(54), dp(barHeight)))
        top.addView(scroll, LinearLayout.LayoutParams(0, dp(barHeight)).apply { weight = 1f })
        top.addView(paste, LinearLayout.LayoutParams(dp(46), dp(barHeight)))
        top.addView(enter, LinearLayout.LayoutParams(dp(62), dp(barHeight)))
        top.addView(del, LinearLayout.LayoutParams(dp(56), dp(barHeight)))
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
                if (k in '2'..'9') letterKeys.add(Pair(key, k))
                // Slide along the space key to move the cursor.
                if (k == '0') key.setOnTouchListener(SpaceSlideTouch(settings) { steps, dir -> listener.onKeypadCursorMove(steps, dir) })
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
        val emoji = makeKey(";)", 16f)
        emoji.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadEmoji()
        }
        val gear = theme.iconKey(context, KeyIcon.SETTINGS, false, GAP_DP, GAP_DP, 10, "Keyboard settings")
        gear.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadSettings()
        }
        val space = makeKey(caps("space"), 16f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onKeypadKey('0')
        }
        bottom.addView(back, keyParams(1f))
        bottom.addView(emoji, keyParams(1f))
        bottom.addView(gear, keyParams(1f))
        bottom.addView(space, keyParams(2f))
        symbolPanel.addView(bottom, LinearLayout.LayoutParams(MATCH, WRAP))
        addView(symbolPanel, LinearLayout.LayoutParams(MATCH, WRAP))
    }

    /** Flashes the background in the accent colour (a character was typed). */
    fun pulse() = (pulseBackground as KeyboardPulseEffect).pulse()

    /** Swaps the number grid for the symbol picker (or back). */
    fun showSymbols(show: Boolean) {
        symbolPanel.visibility = if (show) VISIBLE else GONE
        grid.visibility = if (show) GONE else VISIBLE
    }

    /** Shows candidate words; [selected] is highlighted (-1: none). Pass an empty list to clear. */
    fun setCandidates(words: List<String>, selected: Int) {
        if (words == candidateWords && selected == selectedCandidate && strip.childCount == words.size) return
        candidateWords = words
        selectedCandidate = selected
        strip.removeAllViews()
        words.forEachIndexed { i, w ->
            val tv: TextView = if (theme.isGameBoy && !theme.isGameBoyAdvance) {
                GameBoyPixelTextView(context)
            } else {
                TextView(context)
            }
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
                tv.setTextColor(if (theme.isGameBoyAdvance) 0xFFFFFFFF.toInt() else theme.text)
            }
            tv.setOnClickListener { listener.onKeypadCandidate(i) }
            tv.setOnLongClickListener {
                settings.feedback(it, isLong = true)
                listener.onKeypadCandidateLong(i)
                true
            }
            strip.addView(tv, LinearLayout.LayoutParams(WRAP, MATCH))
        }
        if (selected in words.indices) {
            val child = strip.getChildAt(selected)
            scroll.post { scroll.smoothScrollTo(maxOf(0, child.left - dp(24)), 0) }
        } else {
            scroll.scrollTo(0, 0)
        }
    }

    /** Shows the letters on keys 2-9 as capitals (the next letter will be upper case) or lower case. */
    fun setUpperCase(upper: Boolean) {
        if (upper == upperLetters) return
        upperLetters = upper
        for ((key, k) in letterKeys) key.text = labelFor(k)
    }

    /** Text under the # key, e.g. "Abc · predict". */
    fun setModeLabel(label: String) {
        hashKey.text = twoLine("#", label)
    }

    // ---- helpers ----

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
        else -> twoLine(
            k.toString(),
            KeypadMap.keyLetters.getValue(k).let { if (upperLetters) it.uppercase() else it },
            if (theme.isGameBoyAdvance) gbaLetterColour(k) else null,
        )
    }

    private fun twoLine(big: String, small: String, smallColour: Int? = null): CharSequence {
        if (small.isEmpty()) return big
        val s = SpannableString("$big\n$small")
        s.setSpan(RelativeSizeSpan(0.55f), big.length + 1, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        s.setSpan(ForegroundColorSpan(smallColour ?: theme.subText), big.length + 1, s.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return s
    }

    /** Bright GBA letter legends; yellow is deliberately excluded for contrast. */
    private fun gbaLetterColour(key: Char): Int = when (key) {
        '2', '5' -> GBA_PIXEL_COLOURS[0] // pink-red
        '3', '6' -> GBA_PIXEL_COLOURS[2] // mint
        '4', '7' -> GBA_PIXEL_COLOURS[3] // blue
        '8' -> GBA_PIXEL_COLOURS[4] // violet
        '9' -> GBA_PIXEL_COLOURS[5] // orange
        else -> GBA_PIXEL_COLOURS[0]
    }

    private fun makeKey(
        label: CharSequence,
        size: Float = 20f,
        action: Boolean = false,
    ): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.textSize = size
        tv.typeface = theme.typeface
        tv.gravity = Gravity.CENTER
        tv.setTextColor(if (action) theme.actionText else theme.text)
        tv.background = theme.keyBackground(density(), action, GAP_DP, GAP_DP, 10)
        return tv
    }
}



/** 5x7, one-colour bitmap lettering for the original Game Boy suggestion LCD. */
private class GameBoyPixelTextView(context: Context) : TextView(context) {
    private val pixelPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = false
        isFilterBitmap = false
    }

    override fun onDraw(canvas: Canvas) {
        val value = text?.toString()?.uppercase().orEmpty()
        if (value.isEmpty()) return

        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        if (availableWidth <= 0f || availableHeight <= 0f) return

        // Each glyph is 5x7 pixels, with one blank pixel between characters.
        val columns = value.length * 6 - 1
        val pixel = minOf(textSize / 7f, availableHeight / 7f, availableWidth / columns.coerceAtLeast(1))
        if (pixel <= 0f) return
        val glyphWidth = columns * pixel
        val left = paddingLeft + (availableWidth - glyphWidth) / 2f
        val top = paddingTop + (availableHeight - 7f * pixel) / 2f

        pixelPaint.color = currentTextColor
        value.forEachIndexed { index, character ->
            val glyph = GLYPHS[character] ?: GLYPHS['?']!!
            val x = left + index * 6f * pixel
            glyph.forEachIndexed { row, bits ->
                bits.forEachIndexed { column, bit ->
                    if (bit == '1') {
                        val l = x + column * pixel
                        val t = top + row * pixel
                        canvas.drawRect(l, t, l + pixel, t + pixel, pixelPaint)
                    }
                }
            }
        }
    }

    companion object {
        private val GLYPHS = mapOf(
            'A' to arrayOf("01110", "10001", "10001", "11111", "10001", "10001", "10001"),
            'B' to arrayOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
            'C' to arrayOf("01111", "10000", "10000", "10000", "10000", "10000", "01111"),
            'D' to arrayOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
            'E' to arrayOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
            'F' to arrayOf("11111", "10000", "10000", "11110", "10000", "10000", "10000"),
            'G' to arrayOf("01111", "10000", "10000", "10111", "10001", "10001", "01111"),
            'H' to arrayOf("10001", "10001", "10001", "11111", "10001", "10001", "10001"),
            'I' to arrayOf("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
            'J' to arrayOf("00111", "00010", "00010", "00010", "10010", "10010", "01100"),
            'K' to arrayOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
            'L' to arrayOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
            'M' to arrayOf("10001", "11011", "10101", "10101", "10001", "10001", "10001"),
            'N' to arrayOf("10001", "11001", "11001", "10101", "10011", "10011", "10001"),
            'O' to arrayOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
            'P' to arrayOf("11110", "10001", "10001", "11110", "10000", "10000", "10000"),
            'Q' to arrayOf("01110", "10001", "10001", "10001", "10101", "10010", "01101"),
            'R' to arrayOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
            'S' to arrayOf("01111", "10000", "10000", "01110", "00001", "00001", "11110"),
            'T' to arrayOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
            'U' to arrayOf("10001", "10001", "10001", "10001", "10001", "10001", "01110"),
            'V' to arrayOf("10001", "10001", "10001", "10001", "10001", "01010", "00100"),
            'W' to arrayOf("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
            'X' to arrayOf("10001", "10001", "01010", "00100", "01010", "10001", "10001"),
            'Y' to arrayOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
            'Z' to arrayOf("11111", "00001", "00010", "00100", "01000", "10000", "11111"),
            '0' to arrayOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
            '1' to arrayOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
            '2' to arrayOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
            '3' to arrayOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
            '4' to arrayOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
            '5' to arrayOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
            '6' to arrayOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
            '7' to arrayOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
            '8' to arrayOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
            '9' to arrayOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
            '.' to arrayOf("00000", "00000", "00000", "00000", "00000", "00110", "00110"),
            ',' to arrayOf("00000", "00000", "00000", "00000", "00110", "00100", "01000"),
            '!' to arrayOf("00100", "00100", "00100", "00100", "00100", "00000", "00100"),
            '?' to arrayOf("01110", "10001", "00001", "00010", "00100", "00000", "00100"),
            '-' to arrayOf("00000", "00000", "00000", "11111", "00000", "00000", "00000"),
            '\'' to arrayOf("00100", "00100", "01000", "00000", "00000", "00000", "00000"),
            ' ' to arrayOf("00000", "00000", "00000", "00000", "00000", "00000", "00000"),
        )
    }
}
