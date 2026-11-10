// app/src/main/kotlin/com/m8/keyboard/SetupActivity.kt
package com.m8.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Launcher screen: shortcuts to enable/select the keyboard, plus a field to test it. */
class SetupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, pad, pad, pad)

        val intro = TextView(this)
        intro.text = "M8 Keyboard: predictive number-pad typing.\n\n" +
            "Hold # to switch between predictive and multi-tap. " +
            "Hold a number key to type the digit."
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

        setContentView(root)
    }
}
