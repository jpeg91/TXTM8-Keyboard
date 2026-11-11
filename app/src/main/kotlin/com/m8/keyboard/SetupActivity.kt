// app/src/main/kotlin/com/m8/keyboard/SetupActivity.kt
package com.m8.keyboard

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

/** Launcher screen: enable/select the keyboard, try it, and change its settings. */
class SetupActivity : Activity() {

    private val prefs by lazy { getSharedPreferences(KeyboardSettings.PREFS, MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad, pad, pad)

        val intro = TextView(this)
        intro.text = "M8 Keyboard: predictive number-pad typing.\n\n" +
            "Hold # to switch between predictive and multi-tap. " +
            "Hold a number key to type the digit, then keep tapping it to reach the letters " +
            "(hold 7, tap 4 more times for S). Tap QWERTY for the full keyboard."
        intro.textSize = 16f
        root.addView(intro)

        val enable = Button(this)
        enable.text = "1. Enable the keyboard"
        enable.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        root.addView(enable)

        val choose = Button(this)
        choose.text = "2. Switch to M8 Keyboard"
        choose.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        root.addView(choose)

        val test = EditText(this)
        test.hint = "3. Tap here to try it"
        root.addView(test)

        root.addView(heading("Settings"))
        addChoice(
            root, "Theme", KeyboardSettings.KEY_THEME,
            listOf("P5 (WebMChan)", "Follow system", "Dark", "Light"),
            listOf(
                KeyboardSettings.THEME_P5,
                KeyboardSettings.THEME_SYSTEM,
                KeyboardSettings.THEME_DARK,
                KeyboardSettings.THEME_LIGHT,
            ),
            KeyboardSettings.THEME_P5,
        )
        addChoice(
            root, "Key size", KeyboardSettings.KEY_SIZE,
            listOf("Small", "Medium", "Large"), listOf(0, 1, 2), 1,
        )
        addChoice(
            root, "Multi-tap timeout", KeyboardSettings.KEY_TIMEOUT,
            listOf("Fast (500 ms)", "Normal (800 ms)", "Slow (1200 ms)"), listOf(500, 800, 1200), 800,
        )
        addToggle(root, "Vibrate on key press", KeyboardSettings.KEY_HAPTIC, true)
        addToggle(root, "Key click sound", KeyboardSettings.KEY_SOUND, false)
        addToggle(root, "DEL re-opens the previous word", KeyboardSettings.KEY_REOPEN, true)

        val clear = Button(this)
        clear.text = "Clear learned words"
        clear.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear learned words?")
                .setMessage("This forgets the words and word pairs the keyboard has learned from you.")
                .setPositiveButton("Clear") { _, _ ->
                    UserDataStore(this).clear()
                    prefs.edit().putLong(KeyboardSettings.KEY_CLEAR_TOKEN, System.currentTimeMillis()).apply()
                    Toast.makeText(this, "Learned words cleared", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        root.addView(clear)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun heading(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 20f
        tv.setPadding(0, (24 * resources.displayMetrics.density).toInt(), 0, 0)
        return tv
    }

    private fun addChoice(
        parent: LinearLayout,
        title: String,
        key: String,
        options: List<String>,
        values: List<Int>,
        default: Int,
    ) {
        val label = TextView(this)
        label.text = title
        label.setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
        parent.addView(label)

        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(values.indexOf(prefs.getInt(key, default)).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, position: Int, id: Long) {
                prefs.edit().putInt(key, values[position]).apply()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        parent.addView(spinner)
    }

    private fun addToggle(parent: LinearLayout, title: String, key: String, default: Boolean) {
        val box = CheckBox(this)
        box.text = title
        box.isChecked = prefs.getBoolean(key, default)
        box.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(key, checked).apply()
        }
        parent.addView(box)
    }
}
