// app/src/main/kotlin/com/m8/keyboard/SettingsActivity.kt
package com.m8.keyboard

import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/**
 * The Settings page, opened by the gear at the top right of the main screen: typing, gestures, feedback and the
 * learned words. The look and the layout stay on the main screen, where the test box shows them straight away.
 */
class SettingsActivity : ThemedScreen() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = newPage()
        root.addView(title("Settings"))
        root.addView(body("Typing, gestures, feedback and your learned words. The look and the layout are on the main page."))

        // ---- Typing ----
        addHeading(root, "Typing", "Suggestions, capitals and punctuation.")
        val typingCard = card()
        root.addView(typingCard)
        addToggle(
            typingCard, "Suggest the next word", KeyboardSettings.KEY_NEXT_WORD, true,
            note = "After a word and a space, the strip offers the words that usually follow it.",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Add a space after a suggestion", KeyboardSettings.KEY_SPACE_AFTER_SUGGESTION, true,
            note = "Tapping a suggestion types the word and a space. Switch off to get the word alone; the next-word suggestions then wait until you type a space.",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Capitalise sentences", KeyboardSettings.KEY_AUTO_CAP, true,
            note = "The first letter after a full stop, and at the start of a field, is a capital.",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Double space adds a full stop", KeyboardSettings.KEY_DOUBLE_SPACE, true,
            note = "Tap space twice quickly after a word to get \". \".",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Capitalise I and I'm", KeyboardSettings.KEY_CAPITALISE_I, true,
            note = "I, I'm, I'll, I've and I'd always get a capital I.",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Fix contractions", KeyboardSettings.KEY_CONTRACTIONS, true,
            note = "When typing letter by letter, dont becomes don't and im becomes I'm.",
        )
        typingCard.addView(divider())
        addToggle(
            typingCard, "Backspace re-opens the previous word", KeyboardSettings.KEY_REOPEN, true,
            note = "Backspace straight after a word brings its suggestions back, so you can pick another.",
        )
        typingCard.addView(divider())
        addChoice(
            typingCard, "Start typing in", KeyboardSettings.KEY_START_MODE,
            listOf("Predictive", "Multi-tap"), listOf(KeyboardSettings.START_PREDICTIVE, KeyboardSettings.START_MULTI_TAP),
            KeyboardSettings.START_PREDICTIVE,
            note = "The mode the number pad starts in. Holding # still switches between them. Password fields always use multi-tap.",
        )
        typingCard.addView(divider())
        addChoice(
            typingCard, "Multi-tap timeout", KeyboardSettings.KEY_TIMEOUT,
            listOf("Fast (500 ms)", "Normal (800 ms)", "Slow (1200 ms)"), listOf(500, 800, 1200), 800,
            note = "How long to wait before a multi-tap letter is locked in.",
        )

        // ---- Gestures ----
        addHeading(root, "Gestures", "Slide controls for the cursor and for selecting text.")
        val gesturesCard = card()
        root.addView(gesturesCard)
        addToggle(
            gesturesCard, "Slide gestures", KeyboardSettings.KEY_SLIDE, true,
            note = "Slide along space to move the cursor, or to adjust a selection. Slide left from backspace to select text, then let go to delete it, or flick quickly left to delete the whole word. Hold the paste key and slide to select from the cursor. Switch off if they trigger by accident.",
        )
        gesturesCard.addView(divider())
        addChoice(
            gesturesCard, "Slide speed", KeyboardSettings.KEY_SLIDE_SPEED,
            listOf("Slow", "Normal", "Fast"), listOf(0, 1, 2), 1,
            note = "How far your finger travels for each character.",
        )

        // ---- Feedback ----
        addHeading(root, "Feedback", "What you feel and hear as you type.")
        val feedbackCard = card()
        root.addView(feedbackCard)
        addToggle(feedbackCard, "Vibrate on key press", KeyboardSettings.KEY_HAPTIC, true)
        feedbackCard.addView(divider())
        addToggle(feedbackCard, "Key click sound", KeyboardSettings.KEY_SOUND, false)

        // ---- Your words ----
        addHeading(root, "Your words", "What the keyboard learns from you. It never leaves your phone.")
        val wordsCard = card()
        root.addView(wordsCard)
        addToggle(
            wordsCard, "Learn new words", KeyboardSettings.KEY_LEARN, true,
            note = "Words you use rank higher. A word typed in multi-tap is added when you tap it in the strip, and words typed on the full keyboard are added as you go. Nothing is learned in password fields, and it all stays on your phone.",
        )
        wordsCard.addView(
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
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) },
        )

        showPage(root)
    }

    /** A section heading with a line of explanation under it. */
    private fun addHeading(root: LinearLayout, name: String, blurb: String) {
        root.addView(sectionTitle(name))
        root.addView(sectionBlurb(blurb))
    }
}
