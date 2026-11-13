// app/src/main/kotlin/com/m8/keyboard/EmojiKeyboardView.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

// Visible gaps between the keys of the bottom row (drawn inside each key's touch area).
private const val GAP_X_DP = 2
private const val GAP_Y_DP = 3

/**
 * The emoji page: a scrolling row of emoji on top, a scrolling row of old-school text smileys
 * under it, and a bottom row with a key to go back, space, enter and backspace.
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

    private val pulseBackground = PulseBackground(theme.backgroundDrawable(this), theme.accent, settings.pulseStrength)

    init {
        orientation = VERTICAL
        background = pulseBackground
        setPadding(0, dp(4), 0, 0) // no side padding: the outer keys reach the screen edge

        val rowHeight = dp(settings.keyHeightDp + 2 * GAP_Y_DP)
        addView(scrollingRow(EmojiData.emojis, 26f), LinearLayout.LayoutParams(MATCH, rowHeight))
        addView(scrollingRow(EmojiData.textFaces, 18f), LinearLayout.LayoutParams(MATCH, rowHeight))
        addView(bottomRow(), LinearLayout.LayoutParams(MATCH, WRAP))
    }

    /** Flashes the background in the accent colour (a character was typed). */
    fun pulse() = pulseBackground.pulse()

    /** One row that scrolls sideways; tapping an item types it. */
    private fun scrollingRow(items: List<String>, textSizeSp: Float): HorizontalScrollView {
        val scroll = HorizontalScrollView(context)
        scroll.isHorizontalScrollBarEnabled = false
        scroll.overScrollMode = OVER_SCROLL_NEVER
        scroll.setBackgroundColor(theme.strip)
        val row = LinearLayout(context)
        row.orientation = HORIZONTAL
        for (item in items) {
            val tv = TextView(context)
            tv.text = item
            tv.textSize = textSizeSp
            tv.typeface = theme.typeface
            tv.gravity = Gravity.CENTER
            tv.minWidth = dp(48)
            tv.setPadding(dp(10), 0, dp(10), 0)
            tv.setTextColor(theme.text)
            tv.background = pressHighlight()
            tv.setOnClickListener {
                settings.feedback(it)
                listener.onEmojiText(item)
            }
            row.addView(tv, LinearLayout.LayoutParams(WRAP, MATCH))
        }
        scroll.addView(row, FrameLayout.LayoutParams(WRAP, MATCH))
        return scroll
    }

    private fun bottomRow(): LinearLayout {
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
        val enter = theme.iconKey(context, KeyIcon.ENTER, true, GAP_X_DP, GAP_Y_DP, 8, "Enter")
        enter.setOnClickListener {
            settings.feedback(it)
            listener.onEmojiEnter()
        }
        val del = theme.iconKey(context, KeyIcon.BACKSPACE, true, GAP_X_DP, GAP_Y_DP, 8, "Backspace")
        del.setOnTouchListener(
            BackspaceTouch(
                settings,
                onDelete = { listener.onEmojiBackspace() },
                onSlide = { listener.onEmojiBackspaceSlide(it) },
                onSlideEnd = { listener.onEmojiBackspaceSlideEnd(it) },
            ),
        )

        row.addView(back, keyParams(1.6f))
        row.addView(space, keyParams(4f))
        row.addView(enter, keyParams(1.4f))
        row.addView(del, keyParams(1.6f))
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
        tv.background = theme.keyBackground(resources.displayMetrics.density, false, GAP_X_DP, GAP_Y_DP, 8)
        theme.bindKeyGlow(tv)
        return tv
    }

    private fun keyParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dp(settings.keyHeightDp + 2 * GAP_Y_DP)).apply { this.weight = weight }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
