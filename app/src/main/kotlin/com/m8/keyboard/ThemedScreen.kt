// app/src/main/kotlin/com/m8/keyboard/ThemedScreen.kt
package com.m8.keyboard

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

    /** The page's scroll view, once [showPage] has made it (so a screen can keep its place when it is rebuilt). */
    protected var pageScroll: ScrollView? = null

    protected val density: Float get() = resources.displayMetrics.density

    protected fun dp(v: Int): Int = (v * density).toInt()

    /** The chosen accent colour for the P5 theme (an index into [P5_ACCENTS]). */
    protected fun p5Accent(): Int =
        prefs.getInt(KeyboardSettings.KEY_P5_ACCENT, KeyboardSettings.P5_ACCENT_RED).coerceIn(0, P5_ACCENTS.lastIndex)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardSettings.migrateThemes(prefs)
        val chosen = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_DEFAULT)
        look = KeyboardTheme.resolve(this, chosen, p5Accent(), CustomParts.load(prefs))
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
        pageScroll = scroll
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
        tv.setTextColor(look.accentText) // the banner is filled with the accent colour
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

    /** One line of dim text under a section heading, saying what the section is for. */
    protected fun sectionBlurb(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 13f
        tv.setTextColor(look.subText)
        tv.setLineSpacing(0f, 1.1f)
        tv.setPadding(dp(4), 0, dp(4), dp(10))
        return tv
    }

    /** Mixes [fraction] (0 to 1) of the colour [over] into [base]; the result is always opaque. */
    protected fun blend(base: Int, over: Int, fraction: Float): Int {
        fun mix(shift: Int): Int {
            val a = (base shr shift) and 0xFF
            val b = (over shr shift) and 0xFF
            return (a + (b - a) * fraction).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    /**
     * A panel that holds one section's settings: the page colour nudged towards the text colour, with a
     * fine outline, so it reads as a surface on every theme. Settings are added to it like any other parent.
     */
    protected fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.setPadding(dp(16), dp(6), dp(16), dp(14))
        val shape = GradientDrawable()
        shape.setColor(blend(look.background, look.text, 0.07f))
        shape.setStroke(dp(1), blend(look.background, look.text, 0.22f))
        shape.cornerRadius = (if (look.squareKeys) dp(2) else dp(10)).toFloat()
        c.background = shape
        c.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP)
        return c
    }

    /** A fine rule between two settings in a [card]. */
    protected fun divider(): View {
        val v = View(this)
        v.setBackgroundColor(blend(look.background, look.text, 0.16f))
        v.layoutParams = LinearLayout.LayoutParams(MATCH, dp(1)).apply {
            topMargin = dp(8)
            bottomMargin = dp(2)
        }
        return v
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
        tv.setTextColor(if (action) look.actionText else look.keyText)
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

    /**
     * The twelve accent colours of the P5 theme as a grid of named swatches (primaries, then
     * secondaries, then tertiaries, in rows). Choosing one rebuilds the screen in that colour.
     */
    protected fun addAccentPicker(parent: LinearLayout) {
        parent.addView(label("P5 accent colour"))
        val selected = p5Accent()
        val groups = listOf(
            P5_PRIMARY_START until P5_SECONDARY_START,
            P5_SECONDARY_START until P5_TERTIARY_START,
            P5_TERTIARY_START until P5_ACCENTS.size,
        )
        for ((groupIndex, range) in groups.withIndex()) {
            // No group labels: the swatches stay in rows by group, with a little more space between groups.
            for ((rowNumber, rowIndices) in range.toList().chunked(3).withIndex()) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                for (index in rowIndices) {
                    val accent = P5_ACCENTS[index]
                    val cell = LinearLayout(this)
                    cell.orientation = LinearLayout.VERTICAL
                    cell.gravity = Gravity.CENTER_HORIZONTAL

                    val swatch = View(this)
                    val chip = GradientDrawable()
                    chip.setColor(accent.main)
                    chip.cornerRadius = dp(4).toFloat()
                    if (index == selected) {
                        chip.setStroke(dp(3), 0xFFFFFFFF.toInt())
                    } else {
                        chip.setStroke(dp(1), 0xFF727272.toInt())
                    }
                    swatch.background = chip
                    cell.addView(swatch, LinearLayout.LayoutParams(MATCH, dp(40)))

                    val caption = TextView(this)
                    caption.text = accent.name
                    caption.textSize = 12f
                    caption.gravity = Gravity.CENTER
                    caption.setTextColor(if (index == selected) look.text else look.subText)
                    if (index == selected) caption.typeface = Typeface.DEFAULT_BOLD
                    caption.setPadding(0, dp(3), 0, dp(6))
                    cell.addView(caption, LinearLayout.LayoutParams(MATCH, WRAP))

                    cell.setOnClickListener {
                        prefs.edit().putInt(KeyboardSettings.KEY_P5_ACCENT, index).apply()
                        recreate()
                    }
                    row.addView(cell, LinearLayout.LayoutParams(0, WRAP).apply {
                        weight = 1f
                        setMargins(dp(4), 0, dp(4), 0)
                    })
                }
                parent.addView(
                    row,
                    LinearLayout.LayoutParams(MATCH, WRAP).apply {
                        if (groupIndex > 0 && rowNumber == 0) topMargin = dp(8)
                    },
                )
            }
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

    /** The star density slider, for the themes with twinkling stars. */
    protected fun addStarDensityControl(parent: LinearLayout) {
        parent.addView(label("Star density"))

        val valueText = TextView(this)
        valueText.textSize = 14f
        valueText.setTextColor(look.subText)
        valueText.setPadding(dp(36), dp(4), 0, 0)

        val seek = SeekBar(this)
        seek.max = 100
        seek.progress = prefs.getInt(KeyboardSettings.KEY_STAR_DENSITY, 50)
        val tint = ColorStateList.valueOf(look.accent)
        seek.progressTintList = tint
        seek.thumbTintList = tint
        seek.progressBackgroundTintList = ColorStateList.valueOf(0xFF727272.toInt()) // dim grey

        fun refresh() {
            valueText.text = "Stars: " + seek.progress + "%"
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) prefs.edit().putInt(KeyboardSettings.KEY_STAR_DENSITY, progress).apply()
                refresh()
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}

            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        parent.addView(hint("How many stars twinkle on the keys. Set it to 0% for none."))
        parent.addView(valueText)
        parent.addView(seek, LinearLayout.LayoutParams(MATCH, WRAP))
        refresh()
    }

    /** The tap-effect amount slider, for the themes that animate wherever you tap. */
    protected fun addEffectAmountControl(parent: LinearLayout) {
        parent.addView(label("Effect amount"))

        val valueText = TextView(this)
        valueText.textSize = 14f
        valueText.setTextColor(look.subText)
        valueText.setPadding(dp(36), dp(4), 0, 0)

        val seek = SeekBar(this)
        seek.max = 100
        seek.progress = prefs.getInt(KeyboardSettings.KEY_EFFECT_AMOUNT, 50)
        val tint = ColorStateList.valueOf(look.accent)
        seek.progressTintList = tint
        seek.thumbTintList = tint
        seek.progressBackgroundTintList = ColorStateList.valueOf(0xFF727272.toInt()) // dim grey

        fun refresh() {
            valueText.text = "Amount: " + seek.progress + "%"
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) prefs.edit().putInt(KeyboardSettings.KEY_EFFECT_AMOUNT, progress).apply()
                refresh()
            }

            override fun onStartTrackingTouch(bar: SeekBar?) {}

            override fun onStopTrackingTouch(bar: SeekBar?) {}
        })

        parent.addView(hint("How much appears when you tap a key. Set it to 0% for none."))
        parent.addView(valueText)
        parent.addView(seek, LinearLayout.LayoutParams(MATCH, WRAP))
        refresh()
    }
}