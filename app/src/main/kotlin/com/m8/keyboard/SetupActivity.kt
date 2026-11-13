// app/src/main/kotlin/com/m8/keyboard/SetupActivity.kt
package com.m8.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/**
 * Launcher screen: enable/select the keyboard, try it, and change its settings.
 * Drawn in the same bold look as the red theme (with a purple accent when the purple theme is chosen).
 */
class SetupActivity : Activity() {

    private val prefs by lazy { getSharedPreferences(KeyboardSettings.PREFS, MODE_PRIVATE) }
    private lateinit var look: KeyboardTheme

    private val density: Float get() = resources.displayMetrics.density

    private fun dp(v: Int): Int = (v * density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chosen = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_RED)
        look = KeyboardTheme.resolve(this, chosen)
        window.statusBarColor = look.background
        window.navigationBarColor = look.background
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(dp(16), dp(20), dp(16), dp(24))

        root.addView(title("TXTM8 Keyboard"))
        root.addView(
            body(
                "Predictive number-pad typing.\n\n" +
                    "Hold # to switch between predictive and multi-tap. " +
                    "Hold a number key to type the digit, then keep tapping it to reach the letters " +
                    "(hold 7, tap 4 more times for S). Tap the keyboard icon for the full keyboard.\n\n" +
                    "Slide along the space key to move the cursor. Slide left from backspace to select " +
                    "text, and let go to delete it. Hold a suggestion to remove it.",
            ),
        )

        root.addView(sectionTitle("Setup"))
        root.addView(
            button("1. Enable the keyboard", action = true) {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            },
        )
        root.addView(
            button("2. Switch to TXTM8 Keyboard", action = true) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            },
        )

        root.addView(sectionTitle("Try it"))
        val test = EditText(this)
        test.hint = "Tap here to try it"
        test.setHintTextColor(0xFF727272.toInt()) // dim grey
        test.setTextColor(look.text)
        test.textSize = 16f
        test.setPadding(dp(18), dp(14), dp(18), dp(14))
        test.background = look.keyBackground(density, false, 4, 4, 10)
        root.addView(test, LinearLayout.LayoutParams(MATCH, WRAP))

        root.addView(sectionTitle("Settings"))
        addChoice(
            root, "Theme", KeyboardSettings.KEY_THEME,
            listOf("Red", "Purple", "Nokia 3310", "Motorola RAZR V3", "Follow system", "Dark", "Light"),
            listOf(
                KeyboardSettings.THEME_RED,
                KeyboardSettings.THEME_PURPLE,
                KeyboardSettings.THEME_NOKIA,
                KeyboardSettings.THEME_RAZR,
                KeyboardSettings.THEME_SYSTEM,
                KeyboardSettings.THEME_DARK,
                KeyboardSettings.THEME_LIGHT,
            ),
            KeyboardSettings.THEME_RED,
            recreateOnChange = true, // so this screen picks up the new accent
        )
        addChoice(
            root, "Key size", KeyboardSettings.KEY_SIZE,
            listOf("Small", "Medium", "Large"), listOf(0, 1, 2), 1,
        )
        addChoice(
            root, "Multi-tap timeout", KeyboardSettings.KEY_TIMEOUT,
            listOf("Fast (500 ms)", "Normal (800 ms)", "Slow (1200 ms)"), listOf(500, 800, 1200), 800,
        )
        root.addView(label("Feedback and editing"))
        addToggle(root, "Vibrate on key press", KeyboardSettings.KEY_HAPTIC, true)
        addToggle(root, "Key click sound", KeyboardSettings.KEY_SOUND, false)
        addToggle(root, "Backspace re-opens the previous word", KeyboardSettings.KEY_REOPEN, true)
        addToggle(root, "Suggest the next word", KeyboardSettings.KEY_NEXT_WORD, true)
        root.addView(label("Typing pulse"))
        addPulseControls(root)

        val clear = button("Clear learned words", action = false) {
            AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Clear learned words?")
                .setMessage("This forgets the words and word pairs the keyboard has learned from you, and brings back any suggestions you removed.")
                .setPositiveButton("Clear") { _, _ ->
                    UserDataStore(this).clear()
                    prefs.edit().putLong(KeyboardSettings.KEY_CLEAR_TOKEN, System.currentTimeMillis()).apply()
                    Toast.makeText(this, "Learned words cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        root.addView(clear, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        val scroll = ScrollView(this)
        look.applyBackground(scroll) // the pinstripe backdrop stays put while the content scrolls
        scroll.addView(root)
        setContentView(scroll)
    }

    // ---- building blocks ----

    /** The big screen title: heavy italic capitals with a hard accent drop shadow. */
    private fun title(text: String): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 32f
        tv.typeface = look.typeface
        tv.setTextColor(look.text)
        tv.setShadowLayer(0.01f, 5f * density, 5f * density, look.accent)
        tv.setPadding(0, 0, 0, dp(10))
        return tv
    }

    private fun body(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 14f
        tv.setTextColor(look.text)
        tv.setLineSpacing(0f, 1.15f)
        return tv
    }

    /** A section heading: an accent slanted banner, tilted a touch, with a hard white shadow. */
    private fun sectionTitle(text: String): TextView {
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
    private fun label(text: String): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 13f
        tv.typeface = look.typeface
        tv.setTextColor(look.subText)
        tv.setPadding(0, dp(14), 0, dp(2))
        return tv
    }

    /** A slanted, shouty button: [action] gives the solid accent fill, otherwise black with a white outline. */
    private fun button(text: String, action: Boolean, onClick: () -> Unit): TextView {
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
    private fun optionRow(diamond: View, text: String): LinearLayout {
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

    private fun addChoice(
        parent: LinearLayout,
        title: String,
        key: String,
        options: List<String>,
        values: List<Int>,
        default: Int,
        recreateOnChange: Boolean = false,
    ) {
        parent.addView(label(title))
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

    /** A switch for the typing pulse plus an intensity slider that is greyed out while it is off. */
    private fun addPulseControls(parent: LinearLayout) {
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

    private fun addToggle(parent: LinearLayout, title: String, key: String, default: Boolean) {
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
    }
}
