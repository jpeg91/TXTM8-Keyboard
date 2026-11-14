// app/src/main/kotlin/com/m8/keyboard/EmojiKeyboardView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

// Visible gaps between the keys of the top bar (drawn inside each key's touch area).
private const val GAP_X_DP = 2
private const val GAP_Y_DP = 3

/** How many sections show at once (the last one is cut off, so it is clear the page scrolls). */
private const val VISIBLE_SECTIONS = 3.3f

/**
 * The emoji page. A bar along the top has the same keys, in the same places, as the number pad's
 * top bar: back to the letters, space, enter and backspace (right-hand end). Under it the
 * sections scroll up and down; each section is a row that scrolls left and right. Emoji come
 * first, then old-school text smileys. Tapping an item types it.
 */
class EmojiKeyboardView(
    context: Context,
    private val listener: Listener,
    private val settings: KeyboardSettings,
    private val theme: KeyboardTheme,
) : LinearLayout(context) {

    interface Listener {
        fun onEmojiText(text: String)
        fun onEmojiBack()
        fun onEmojiSpace()
        fun onEmojiBackspace()
        fun onEmojiBackspaceSlide(steps: Int)
        fun onEmojiBackspaceSlideEnd(delete: Boolean)
        fun onEmojiEnter()
    }

    private class Row(val scroll: HorizontalScrollView, val section: EmojiSection, var built: Boolean = false)

    private val pulseBackground: Drawable = if (theme.isGlitter) GlitterPulseBackground(theme.backgroundDrawable(this), settings.pulseStrength, resources.displayMetrics.density) else PulseBackground(theme.backgroundDrawable(this), theme.accent, settings.pulseStrength)

    private val rowPx = dp(settings.keyHeightDp + 2 * GAP_Y_DP)
    private val headerPx = dp(24)
    private val sectionPx = headerPx + rowPx
    private val scrollAreaPx = (sectionPx * VISIBLE_SECTIONS).toInt()

    private val vScroll = ScrollView(context)
    private val rows = ArrayList<Row>()

    init {
        orientation = VERTICAL
        background = pulseBackground
        addView(topBar(), LinearLayout.LayoutParams(MATCH, rowPx))

        val content = LinearLayout(context)
        content.orientation = VERTICAL
        for (section in EmojiData.sections) {
            content.addView(header(section.title), LinearLayout.LayoutParams(MATCH, headerPx))
            val scroll = HorizontalScrollView(context)
            scroll.isHorizontalScrollBarEnabled = false
            scroll.overScrollMode = OVER_SCROLL_NEVER
            scroll.setBackgroundColor(theme.strip)
            content.addView(scroll, LinearLayout.LayoutParams(MATCH, rowPx))
            rows.add(Row(scroll, section))
        }
        vScroll.isVerticalScrollBarEnabled = false
        vScroll.overScrollMode = OVER_SCROLL_NEVER
        vScroll.addView(content, FrameLayout.LayoutParams(MATCH, WRAP))
        vScroll.setOnScrollChangeListener { _, _, _, _, _ -> buildNearby() }
        addView(vScroll, LinearLayout.LayoutParams(MATCH, scrollAreaPx))
        buildNearby()
    }

    /** Flashes the background in the accent colour (a character was typed). */
    fun pulse() = (pulseBackground as KeyboardPulseEffect).pulse()

    /**
     * Fills in the rows on screen, and the section after them. Rows further down are built when
     * they get near, so opening the page stays quick. Every section has the same height, so where
     * each one sits is known without waiting for the layout.
     */
    private fun buildNearby() {
        val limit = vScroll.scrollY + scrollAreaPx + sectionPx
        for ((i, row) in rows.withIndex()) {
            if (!row.built && i * sectionPx <= limit) build(row)
        }
    }

    private fun build(row: Row) {
        row.built = true
        val cells = LinearLayout(context)
        cells.orientation = HORIZONTAL
        val section = row.section
        for (item in section.items) {
            val tv = TextView(context)
            tv.text = item
            tv.textSize = if (section.isText) 18f else 26f
            if (section.isText) tv.typeface = theme.typeface
            tv.gravity = Gravity.CENTER
            tv.minWidth = dp(48)
            tv.setPadding(dp(10), 0, dp(10), 0)
            tv.setTextColor(theme.text)
            tv.background = pressHighlight()
            tv.setOnClickListener {
                settings.feedback(it)
                listener.onEmojiText(item)
            }
            cells.addView(tv, LinearLayout.LayoutParams(WRAP, MATCH))
        }
        row.scroll.addView(cells, FrameLayout.LayoutParams(WRAP, MATCH))
    }

    private fun header(title: String): TextView {
        val tv = TextView(context)
        tv.text = title
        tv.textSize = 12f
        tv.typeface = theme.typeface
        tv.setTextColor(theme.subText)
        tv.gravity = Gravity.BOTTOM or Gravity.START
        tv.setPadding(dp(12), 0, 0, dp(2))
        return tv
    }

    /** Same keys, in the same places, as the number pad's top bar (enter, then backspace at the end). */
    private fun topBar(): LinearLayout {
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL

        val back = makeKey("ABC", 14f)
        back.setOnClickListener {
            settings.feedback(it)
            listener.onEmojiBack()
        }
        val space = makeKey(if (theme.isSlanted) "SPACE" else "space", 14f)
        space.setOnClickListener {
            settings.feedback(it)
            listener.onEmojiSpace()
        }
        val enter = theme.iconKey(context, KeyIcon.ENTER, true, GAP_X_DP, GAP_Y_DP, 10, "Enter")
        enter.setOnClickListener {
            settings.feedback(it)
            listener.onEmojiEnter()
        }
        val del = theme.iconKey(context, KeyIcon.BACKSPACE, true, GAP_X_DP, GAP_Y_DP, 10, "Backspace")
        del.setOnTouchListener(
            BackspaceTouch(
                settings,
                onDelete = { listener.onEmojiBackspace() },
                onSlide = { listener.onEmojiBackspaceSlide(it) },
                onSlideEnd = { listener.onEmojiBackspaceSlideEnd(it) },
            ),
        )

        row.addView(back, LinearLayout.LayoutParams(dp(54), rowPx))
        row.addView(space, LinearLayout.LayoutParams(0, rowPx).apply { weight = 1f })
        row.addView(enter, LinearLayout.LayoutParams(dp(62), rowPx))
        row.addView(del, LinearLayout.LayoutParams(dp(56), rowPx))
        return row
    }

    private fun pressHighlight(): StateListDrawable = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(theme.keyPressed))
        addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    }

    private fun makeKey(label: CharSequence, size: Float): TextView {
        val tv = TextView(context)
        tv.text = label
        tv.textSize = size
        tv.typeface = theme.typeface
        tv.gravity = Gravity.CENTER
        tv.setTextColor(theme.text)
        tv.background = theme.keyBackground(resources.displayMetrics.density, false, GAP_X_DP, GAP_Y_DP, 10)
        return tv
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
