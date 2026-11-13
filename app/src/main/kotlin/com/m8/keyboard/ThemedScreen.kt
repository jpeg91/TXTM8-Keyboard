// app/src/main/kotlin/com/m8/keyboard/ThemedScreen.kt
package com.m8.keyboard

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/**
 * The look shared by the settings screen and the About page: the chosen keyboard theme's colours,
 * the bold slanted headings and buttons, and the diamond switches. Subclasses call [applyLook]
 * first, build their content on a [newPage], and hand it to [showPage].
 */
abstract class ThemedScreen : Activity() {

    protected val prefs by lazy { getSharedPreferences(KeyboardSettings.PREFS, MODE_PRIVATE) }
    protected lateinit var look: KeyboardTheme

    protected val density: Float get() = resources.displayMetrics.density

    protected fun dp(v: Int): Int = (v * density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chosen = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_RED)
        look = KeyboardTheme.resolve(this, chosen)
        window.statusBarColor = look.background
        window.navigationBarColor = look.background
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    protected fun newPage(): LinearLayout {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(20), dp(16), dp(24))
        return root
    }

    protected fun showPage(root: LinearLayout) {
        val scroll = ScrollView(this)
        look.applyBackground(scroll) // the pinstripe backdrop stays put while the content scrolls
        scroll.addView(root)
        setContentView(scroll)
    }

    // ---- building blocks ----

    /** The big screen title: heavy italic capitals with a hard accent drop shadow. */
    protected fun title(text: String): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 32f
        tv.typeface = look.typeface
        tv.setTextColor(look.text)
        tv.setShadowLayer(0.01f, 5f * density, 5f * density, look.accent)
        tv.setPadding(0, 0, 0, dp(10))
        return tv
    }

    protected fun body(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(look.text)
        tv.setLineSpacing(0f, 1.15f)
        return tv
    }

    /** Small, dim explanatory text under a setting. */
    protected fun hint(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 12.5f
        tv.setTextColor(look.subText)
        tv.setLineSpacing(0f, 1.1f)
        tv.setPadding(dp(36), 0, 0, dp(6))
        return tv
    }

    /** A section heading: an accent slanted banner, tilted a touch, with a hard white shadow. */
    protected fun sectionTitle(text: String): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 17f
        tv.typeface = look.typeface
        tv.setTextColor(look.text)
        tv.setPadding(dp(20), dp(5), dp(24), dp(9))
        tv.background = SlantKeyDrawable(
            fill = look.accent,
            border = 0,
            shadow = look.text,
            insetX = 0f,
            insetY = 0f,
            slant = 10f * density,
            borderWidth = 0f,
            shadowOffset = 4f * density,
            leftFrac = 0f,
            rightFrac = 0f,
        )
        tv.rotation = -1.5f
        tv.pivotX = 0f
        tv.layoutParams = LinearLayout.LayoutParams(WRAP, WRAP).apply {
            topMargin = dp(22)
            bottomMargin = dp(8)
        }
        return tv
    }

    /** A small heading above a group of choices. */
    protected fun label(text: String): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 13f
        tv.typeface = look.typeface
        tv.setTextColor(look.subText)
        tv.setPadding(0, dp(14), 0, dp(2))
        return tv
    }

    /** A slanted, shouty button: [action] gives the solid accent fill, otherwise black with a white outline. */
    protected fun button(text: String, action: Boolean, onClick: () -> Unit): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 15f
        tv.typeface = look.typeface
        tv.gravity = Gravity.CENTER
        tv.setTextColor(look.text)
        tv.minHeight = dp(58)
        tv.setPadding(dp(16), 0, dp(16), 0)
        tv.background = look.keyBackground(density, action, 4, 4, 10)
        tv.setOnClickListener { onClick() }
        tv.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        return tv
    }

    /** A tappable row: a diamond marker followed by the option's text. */
    protected fun optionRow(diamond: View, text: String): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.minimumHeight = dp(48)
        row.addView(diamond, LinearLayout.LayoutParams(dp(26), dp(26)).apply { rightMargin = dp(10) })
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 16f
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.setTextColor(look.text)
        row.addView(tv, LinearLayout.LayoutParams(0, WRAP).apply { weight = 1f })
        row.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        return row
    }

    protected fun addChoice(
        parent: LinearLayout,
        title: String,
        key: String,
        options: List<String>,
        values: List<Int>,
        default: Int,
        note: String? = null,
        recreateOnChange: Boolean = false,
    ) {
        parent.addView(label(title))
        if (note != null) parent.addView(hint(note).apply { setPadding(0, 0, 0, dp(4)) })
        val diamonds = ArrayList<View>()
        var selected = values.indexOf(prefs.getInt(key, default)).coerceAtLeast(0)
        options.forEachIndexed { i, option ->
            val diamond = View(this)
            diamond.background = DiamondDrawable(i == selected, look.accent, density)
            diamonds.add(diamond)
            val row = optionRow(diamond, option)
            row.setOnClickListener {
                selected = i
                prefs.edit().putInt(key, values[i]).apply()
                diamonds.forEachIndexed { j, d ->
                    d.background = DiamondDrawable(j == selected, look.accent, density)
                }
                if (recreateOnChange) recreate()
            }
            parent.addView(row)
        }
    }

    protected fun addToggle(parent: LinearLayout, title: String, key: String, default: Boolean, note: String? = null) {
        var on = prefs.getBoolean(key, default)
        val diamond = View(this)
        diamond.background = DiamondDrawable(on, look.accent, density)
        val row = optionRow(diamond, title)
        row.setOnClickListener {
            on = !on
            prefs.edit().putBoolean(key, on).apply()
            diamond.background = DiamondDrawable(on, look.accent, density)
        }
        parent.addView(row)
        if (note != null) parent.addView(hint(note))
    }

    /** A switch for the typing pulse plus an intensity slider that is greyed out while it is off. */
    protected fun addPulseControls(parent: LinearLayout) {
        var on = prefs.getBoolean(KeyboardSettings.KEY_PULSE, true)

        val valueText = TextView(this)
        valueText.textSize = 14f
        valueText.setTextColor(look.subText)
        valueText.setPadding(dp(36), dp(4), 0, 0)

        val seek = SeekBar(this)
        seek.max = 100
        seek.progress = prefs.getInt(KeyboardSettings.KEY_PULSE_LEVEL, 50)
        val tint = ColorStateList.valueOf(look.accent)
        seek.progressTintList = tint
        seek.thumbTintList = tint
        seek.progressBackgroundTintList = ColorStateList.valueOf(0xFF727272.toInt()) // dim grey

        fun refresh() {
            seek.isEnabled = on
            seek.alpha = if (on) 1f else 0.35f
            valueText.alpha = if (on) 1f else 0.35f
            valueText.text = "Pulse intensity: " + seek.progress + "%"
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) prefs.edit().putInt(KeyboardSettings.KEY_PULSE_LEVEL, progress).apply()
                refresh()
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}

            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        val diamond = View(this)
        diamond.background = DiamondDrawable(on, look.accent, density)
        val row = optionRow(diamond, "Pulse the background when typing")
        row.setOnClickListener {
            on = !on
            prefs.edit().putBoolean(KeyboardSettings.KEY_PULSE, on).apply()
            diamond.background = DiamondDrawable(on, look.accent, density)
            refresh()
        }
        parent.addView(row)
        parent.addView(valueText)
        parent.addView(seek, LinearLayout.LayoutParams(MATCH, WRAP))
        refresh()
    }
}
