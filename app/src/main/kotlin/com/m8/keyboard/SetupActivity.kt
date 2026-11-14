// app/src/main/kotlin/com/m8/keyboard/SetupActivity.kt
package com.m8.keyboard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val WRAP = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * Launcher screen: enable/select the keyboard, try it, and change its settings. Everything that
 * can be changed is here, grouped by what it affects; the About page has the full feature guide.
 */
class SetupActivity : ThemedScreen() {

    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = newPage()
        root.addView(title("TXTM8 Keyboard"))
        root.addView(body("Predictive number-pad typing, the way phones used to do it. Everything stays on your phone."))

        // ---- Setup ----
        root.addView(sectionTitle("Setup"))
        val statusView = body("")
        statusView.setPadding(0, 0, 0, dp(10))
        status = statusView
        root.addView(statusView)
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
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) },
        )

        // ---- Try it ----
        root.addView(sectionTitle("Try it"))
        val test = EditText(this)
        test.hint = "Tap here to try it"
        test.setHintTextColor(0xFF727272.toInt()) // dim grey
        test.setTextColor(look.text)
        test.textSize = 16f
        test.setPadding(dp(18), dp(14), dp(18), dp(14))
        test.background = look.keyBackground(density, false, 4, 4, 10)
        root.addView(test, LinearLayout.LayoutParams(MATCH, WRAP))
        root.addView(
            hint("Tip: hold # to switch between predictive and multi-tap, and tap the keyboard icon for the full keyboard.")
                .apply { setPadding(0, dp(8), 0, 0) },
        )

        // ---- Look ----
        root.addView(sectionTitle("Look"))
        addChoice(
            root, "Theme", KeyboardSettings.KEY_THEME,
            listOf("Red", "Purple", "Nokia 3310", "Game Boy", "Game Boy Advance", "Glitter Graphics", "Follow system", "Dark", "Light"),
            listOf(
                KeyboardSettings.THEME_RED,
                KeyboardSettings.THEME_PURPLE,
                KeyboardSettings.THEME_NOKIA,
                KeyboardSettings.THEME_GAME_BOY,
                KeyboardSettings.THEME_GAME_BOY_ADVANCE,
                KeyboardSettings.THEME_GLITTER,
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
        root.addView(label("Typing pulse"))
        addPulseControls(root)

        // ---- Typing ----
        root.addView(sectionTitle("Typing"))
        addToggle(
            root, "Suggest the next word", KeyboardSettings.KEY_NEXT_WORD, true,
            note = "After a word and a space, the strip offers the words that usually follow it.",
        )
        addToggle(
            root, "Capitalise sentences", KeyboardSettings.KEY_AUTO_CAP, true,
            note = "The first letter after a full stop, and at the start of a field, is a capital.",
        )
        addToggle(
            root, "Double space adds a full stop", KeyboardSettings.KEY_DOUBLE_SPACE, true,
            note = "Tap space twice quickly after a word to get \". \".",
        )
        addToggle(
            root, "Fix contractions", KeyboardSettings.KEY_CONTRACTIONS, true,
            note = "When typing letter by letter, dont becomes don't and im becomes I'm.",
        )
        addToggle(
            root, "Backspace re-opens the previous word", KeyboardSettings.KEY_REOPEN, true,
            note = "Backspace straight after a word brings its suggestions back, so you can pick another.",
        )
        addChoice(
            root, "Multi-tap timeout", KeyboardSettings.KEY_TIMEOUT,
            listOf("Fast (500 ms)", "Normal (800 ms)", "Slow (1200 ms)"), listOf(500, 800, 1200), 800,
            note = "How long to wait before a multi-tap letter is locked in.",
        )

        // ---- Gestures ----
        root.addView(sectionTitle("Gestures"))
        addToggle(
            root, "Slide gestures", KeyboardSettings.KEY_SLIDE, true,
            note = "Slide along space to move the cursor, or to adjust a selection. Slide left from backspace to select text, then let go to delete it. Hold the paste key and slide to select from the cursor. Switch off if they trigger by accident.",
        )
        addChoice(
            root, "Slide speed", KeyboardSettings.KEY_SLIDE_SPEED,
            listOf("Slow", "Normal", "Fast"), listOf(0, 1, 2), 1,
            note = "How far your finger travels for each character.",
        )

        // ---- Feedback ----
        root.addView(sectionTitle("Feedback"))
        addToggle(root, "Vibrate on key press", KeyboardSettings.KEY_HAPTIC, true)
        addToggle(root, "Key click sound", KeyboardSettings.KEY_SOUND, false)

        // ---- Your words ----
        root.addView(sectionTitle("Your words"))
        addToggle(
            root, "Learn new words", KeyboardSettings.KEY_LEARN, true,
            note = "Words you use rank higher, and words you type letter by letter are added. Nothing is learned in password fields, and it all stays on your phone.",
        )
        root.addView(
            button("Clear learned words", action = false) {
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
            },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) },
        )

        // ---- More ----
        root.addView(sectionTitle("More"))
        root.addView(
            button("About and feature guide", action = true) {
                startActivity(Intent(this, AboutActivity::class.java))
            },
        )
        root.addView(
            button("Reset settings", action = false) {
                AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Reset settings?")
                    .setMessage("Every setting on this screen goes back to its default. Your learned words are kept.")
                    .setPositiveButton("Reset") { _, _ ->
                        val token = prefs.getLong(KeyboardSettings.KEY_CLEAR_TOKEN, 0L)
                        prefs.edit().clear().putLong(KeyboardSettings.KEY_CLEAR_TOKEN, token).apply()
                        recreate()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) },
        )

        showPage(root)
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    /** Says where the person is in the two setup steps (this changes while they are in system settings). */
    private fun refreshStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        val selected = current.startsWith("$packageName/")
        status?.text = when {
            !enabled -> "Not enabled yet. Do step 1, then step 2."
            !selected -> "Enabled, but not your current keyboard yet. Do step 2."
            else -> "Enabled and selected. You are all set: try it below."
        }
    }
}
