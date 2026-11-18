// app/src/main/kotlin/com/m8/keyboard/SettingsActivity.kt
package com.m8.keyboard

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

/** Set when Reset settings is used here, so the main screen rebuilds itself with the default look when it comes back. */
internal var settingsWereReset = false

/**
 * The Settings page, opened by the gear at the top right of the main screen: layout, typing, gestures, feedback, the
 * learned words, and the About page and reset. The theme picker stays on the main screen, beside the test box.
 */
class SettingsActivity : ThemedScreen() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = newPage()
        val anchors = LinkedHashMap<String, View>() // section headings the shortcut chips scroll to
        root.addView(title("Settings"))
        root.addView(body("Layout, typing, gestures, feedback and your learned words. The look is on the main page."))

        // Shortcuts to each section, filled in once the headings exist (below).
        val jumpRow = LinearLayout(this)
        jumpRow.orientation = LinearLayout.HORIZONTAL
        val jumpScroll = HorizontalScrollView(this)
        jumpScroll.isHorizontalScrollBarEnabled = false
        jumpScroll.addView(jumpRow, FrameLayout.LayoutParams(WRAP, WRAP))
        root.addView(jumpScroll, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })

        // ---- Layout ----
        addHeading(root, anchors, "Layout", "Which keyboard opens, and where the keys sit.")
        val layoutCard = card()
        root.addView(layoutCard)
        addToggle(
            layoutCard, "Default to QWERTY", KeyboardSettings.KEY_DEFAULT_QWERTY, false,
            note = "The keyboard opens on the QWERTY layout instead of the number pad. The keyboard key still switches between them while you type. Number and phone fields always use the number pad.",
        )
        layoutCard.addView(divider())
        addToggle(
            layoutCard, "Number row on QWERTY", KeyboardSettings.KEY_QWERTY_NUMBER_ROW, false,
            note = "Adds a row of digits above the letters. It makes the keyboard one row taller.",
        )
        layoutCard.addView(divider())
        layoutCard.addView(label("Swap Enter and Backspace"))
        layoutCard.addView(hint("A separate switch for each screen. On the number pad and the emoji page Backspace comes before Enter. On QWERTY, Enter moves to the bottom row and Backspace to the end of the row above."))
        addToggle(layoutCard, "Number pad", KeyboardSettings.KEY_SWAP_PAD, false)
        addToggle(layoutCard, "QWERTY keyboard", KeyboardSettings.KEY_SWAP_QWERTY, false)
        addToggle(layoutCard, "Emoji page", KeyboardSettings.KEY_SWAP_EMOJI, false)

        // ---- Typing ----
        addHeading(root, anchors, "Typing", "Suggestions, capitals and punctuation.")
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
        addHeading(root, anchors, "Gestures", "Slide controls for the cursor and for selecting text.")
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
        addHeading(root, anchors, "Feedback", "What you feel and hear as you type.")
        val feedbackCard = card()
        root.addView(feedbackCard)
        addToggle(feedbackCard, "Vibrate on key press", KeyboardSettings.KEY_HAPTIC, true)
        feedbackCard.addView(divider())
        addToggle(feedbackCard, "Key click sound", KeyboardSettings.KEY_SOUND, false)

        // ---- Your words ----
        addHeading(root, anchors, "Your words", "What the keyboard learns from you. It never leaves your phone.")
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

        // ---- More ----
        addHeading(root, anchors, "More", "The full feature guide, and a way back to the defaults.")
        root.addView(
            button("About and feature guide", action = true) {
                startActivity(Intent(this, AboutActivity::class.java))
            },
        )
        root.addView(
            button("Reset settings", action = false) {
                AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Reset settings?")
                    .setMessage("Every setting goes back to its default. Your learned words are kept.")
                    .setPositiveButton("Reset") { _, _ ->
                        val token = prefs.getLong(KeyboardSettings.KEY_CLEAR_TOKEN, 0L)
                        prefs.edit().clear().putLong(KeyboardSettings.KEY_CLEAR_TOKEN, token).apply()
                        settingsWereReset = true // the main page rebuilds itself when it comes back
                        recreate()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) },
        )

        val footer = TextView(this)
        val version = versionName()
        footer.text = "TXTM8 Keyboard" + (if (version.isNotEmpty()) "  \u00B7  version $version" else "") + "\nMade in England"
        footer.textSize = 12f
        footer.setTextColor(look.subText)
        footer.gravity = Gravity.CENTER
        footer.setLineSpacing(0f, 1.2f)
        footer.setPadding(0, dp(28), 0, dp(4))
        root.addView(footer, LinearLayout.LayoutParams(MATCH, WRAP))

        for ((name, heading) in anchors) {
            jumpRow.addView(
                chip(name) { pageScroll?.smoothScrollTo(0, (heading.top - dp(8)).coerceAtLeast(0)) },
                LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(2) },
            )
        }

        showPage(root)
    }

    /** A section heading with a line of explanation under it; the heading is remembered so a chip can scroll to it. */
    private fun addHeading(root: LinearLayout, anchors: MutableMap<String, View>, name: String, blurb: String) {
        val heading = sectionTitle(name)
        anchors[name] = heading
        root.addView(heading)
        root.addView(sectionBlurb(blurb))
    }

    /** A small tappable pill that scrolls to a section. */
    private fun chip(text: String, onClick: () -> Unit): TextView {
        val tv = TextView(this)
        tv.text = text.uppercase()
        tv.textSize = 12.5f
        tv.typeface = look.typeface
        tv.setTextColor(look.keyText)
        tv.gravity = Gravity.CENTER
        tv.minHeight = dp(40)
        tv.setPadding(dp(14), 0, dp(14), 0)
        tv.background = look.keyBackground(density, false, 3, 3, 10)
        tv.setOnClickListener { onClick() }
        return tv
    }

    @Suppress("DEPRECATION")
    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
    } catch (e: Exception) {
        ""
    }
}
