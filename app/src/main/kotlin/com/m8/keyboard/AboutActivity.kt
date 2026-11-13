// app/src/main/kotlin/com/m8/keyboard/AboutActivity.kt
package com.m8.keyboard

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

private const val GITHUB_URL = "https://github.com/jpeg91/TXTM8-Keyboard"

/** The About page: what the keyboard is, an in-depth guide to every feature, and the credits. */
class AboutActivity : ThemedScreen() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = newPage()
        root.addView(title("About"))
        root.addView(body("TXTM8 Keyboard" + versionText()))

        // ---- What it is ----
        root.addView(sectionTitle("What it is"))
        root.addView(
            body(
                "TXTM8 Keyboard is an Android keyboard that types the way phones did before touchscreens: " +
                    "a number pad with letters on the keys, and a predictive engine that works out the word " +
                    "you want from a single press per letter.\n\n" +
                    "The engine aims for the quality of the old Sony Ericsson predictive text. It starts from a " +
                    "large English dictionary ranked by how common each word is, then learns from you as you type.",
            ),
        )

        // ---- Number pad ----
        root.addView(sectionTitle("Typing on the number pad"))
        feature(
            root, "Predictive mode",
            "Press each key once per letter. To type \"good\", press 4, 6, 6, 3. The best guess appears in the " +
                "text as you go, and the other possibilities appear in the strip above the keys, most likely " +
                "first.\n\n" +
                "Press space (the 0 key) to accept the highlighted word and add a space, or tap a different word " +
                "in the strip to use that one instead. Press * to step through the guesses one at a time.",
        )
        feature(
            root, "Multi-tap mode",
            "Hold # to switch between predictive and multi-tap; the label under # shows which one you are in. " +
                "In multi-tap you press a key repeatedly to cycle through its letters: 7 once is P, twice Q, " +
                "three times R, four times S.\n\n" +
                "Pause (the timeout is in Settings) or press another key and the letter is locked in. Words " +
                "typed this way are added to your own dictionary, so predictive mode finds them next time.",
        )
        feature(
            root, "Digits",
            "Hold a number key to type its digit, then keep tapping it to reach its letters (hold 7, tap 4 more " +
                "times for S). Hold 0 to type a zero. In phone and number fields the pad simply types digits.",
        )
        feature(
            root, "Punctuation",
            "Tap 1 repeatedly to cycle through . , ? ! and the other common punctuation marks. Right after you tap " +
                "a suggestion, punctuation takes back the automatic space so it sits against the word.",
        )
        feature(
            root, "Capitals",
            "Tap # to cycle through lower case, a capital for the next letter only, and caps lock. The letters " +
                "printed on the keys change to show what the next letter will be. The first letter of a sentence " +
                "is capitalised for you (you can turn that off in Settings).",
        )
        feature(
            root, "Symbols",
            "Hold * (or tap it when nothing is being typed) to open the symbol page. Tap ABC to come back.",
        )

        // ---- Smart suggestions ----
        root.addView(sectionTitle("Smart suggestions"))
        feature(
            root, "It learns from you",
            "Words you use are ranked higher over time, and the keyboard remembers which words tend to follow " +
                "which. Everything it learns stays on your phone, and nothing is learned in password fields. " +
                "\"Clear learned words\" in Settings wipes it.",
        )
        feature(
            root, "Next-word suggestions",
            "After you finish a word and press space, the strip offers the words that have followed it before, " +
                "most often first. Tap one to type it, with a space after. It starts empty and fills up as you " +
                "type. You can switch it off in Settings.",
        )
        feature(
            root, "Removing suggestions",
            "Hold a suggestion to remove it. A word you are typing is taken out of the suggestions for good " +
                "(typing it out letter by letter brings it back). A next-word suggestion only forgets that " +
                "pairing of words.",
        )
        feature(
            root, "Automatic spaces",
            "Tapping a suggestion adds the space for you. Type . , ! ? ; or : straight afterwards and the space is " +
                "taken back, so the punctuation sits against the word.",
        )
        feature(
            root, "Double space",
            "Tap space twice quickly after a word to get a full stop and a space.",
        )
        feature(
            root, "Contractions",
            "When typing letter by letter, \"dont\" becomes \"don't\" and \"im\" becomes \"I'm\" when you press " +
                "space or enter. Capitals are kept.",
        )
        feature(
            root, "Backspace re-opens a word",
            "Press backspace straight after a word and the word re-opens with its suggestions, so you can choose " +
                "a different one instead of retyping.",
        )

        // ---- Editing ----
        root.addView(sectionTitle("Editing"))
        feature(
            root, "Move the cursor",
            "Slide a finger along the space key (the 0 key on the number pad) to move the cursor left or right.",
        )
        feature(
            root, "Select text",
            "With text selected, sliding along space adjusts the selection from either end. Slide off the space " +
                "key to the left and you control the left end: keep sliding left to grow the selection, slide back " +
                "right to shrink it. Slide off to the right and you control the right end in the same way. The " +
                "selection always keeps at least one character.\n\n" +
                "Slide left from backspace to select backwards a character at a time, slide back to give characters " +
                "back, and lift your finger to delete the selection. Nothing is deleted until you let go, so " +
                "changing your mind costs nothing. A plain tap deletes one character, and holding repeats.",
        )
        feature(
            root, "Copy and paste",
            "The clipboard key pastes whatever you last copied. Hold it and let go to copy the selected text.\n\n" +
                "Hold it and slide to start selecting from the cursor: slide left to select leftwards, or right to " +
                "select rightwards, and slide back to shrink the selection. Let go and the text stays selected, " +
                "ready to copy, paste over or delete. If you slide before the hold registers, nothing happens.",
        )
        feature(
            root, "Slide settings",
            "Slide gestures can be switched off, or made slower or faster, in Settings.",
        )

        // ---- Full keyboard, emoji ----
        root.addView(sectionTitle("Full keyboard and emoji"))
        feature(
            root, "QWERTY",
            "Tap the keyboard icon on the number pad for a full QWERTY keyboard, with a symbols page (?123), " +
                "paste, space, enter and backspace. The slide gestures work here too. Password fields always use " +
                "the plain keyboard, with no learning.",
        )
        feature(
            root, "Emoji and smileys",
            "On either symbol page, tap ;) to open the emoji page. Scroll up and down through the sections: emoji " +
                "grouped by kind, then old-school text smileys and kaomoji. Each section scrolls sideways. Tap " +
                "anything to type it. ABC takes you back, and space, enter and backspace sit along the top, " +
                "where they are on the number pad.",
        )
        feature(
            root, "Settings shortcut",
            "The cog on the symbol pages opens this app's settings.",
        )

        // ---- Look and feel ----
        root.addView(sectionTitle("Look and feel"))
        feature(
            root, "Themes",
            "Choose a theme, or follow the system's light and dark mode. Key size can be small, medium or large.",
        )
        feature(
            root, "Typing pulse",
            "The keyboard background flashes in the theme's accent colour as you type, with an adjustable " +
                "strength. It is drawn by the keyboard itself, so Android's animation settings do not speed it up, " +
                "slow it down or switch it off.",
        )
        feature(
            root, "Feedback",
            "Vibration and key clicks can each be switched on or off.",
        )

        // ---- Privacy ----
        root.addView(sectionTitle("Privacy"))
        root.addView(
            body(
                "This app has no internet permission, so the keyboard cannot send anything off your phone. What it " +
                    "learns is stored privately on the device, and you can erase it at any time.",
            ),
        )

        // ---- Source ----
        root.addView(sectionTitle("Source code"))
        root.addView(body("The source code, updates and issues live on GitHub."))
        root.addView(
            button("Open on GitHub", action = true) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, GITHUB_URL, Toast.LENGTH_LONG).show()
                }
            },
            LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) },
        )
        root.addView(
            hint("github.com/jpeg91/TXTM8-Keyboard").apply { setPadding(0, dp(6), 0, 0) },
        )

        // ---- Credits ----
        val rule = View(this)
        rule.setBackgroundColor(look.accent)
        root.addView(rule, LinearLayout.LayoutParams(MATCH, dp(3)).apply { topMargin = dp(32) })
        val credits = TextView(this)
        credits.text = "Created by jpeg91\nWith great thanks to my darling wife Lauren\n\nMade in England"
        credits.textSize = 15f
        credits.gravity = Gravity.CENTER
        credits.setTextColor(look.text)
        credits.setLineSpacing(0f, 1.2f)
        credits.setPadding(0, dp(20), 0, dp(12))
        root.addView(credits, LinearLayout.LayoutParams(MATCH, WRAP))

        showPage(root)
    }

    /** A feature: a bold name, then what it does. */
    private fun feature(parent: LinearLayout, name: String, text: String) {
        val heading = TextView(this)
        heading.text = name
        heading.textSize = 16f
        heading.typeface = Typeface.DEFAULT_BOLD
        heading.setTextColor(look.text)
        heading.setPadding(0, dp(14), 0, dp(3))
        parent.addView(heading)
        parent.addView(body(text))
    }

    private fun versionText(): String = try {
        val name = packageManager.getPackageInfo(packageName, 0).versionName
        if (name.isNullOrEmpty()) "" else "  ·  version $name"
    } catch (e: Exception) {
        ""
    }
}
