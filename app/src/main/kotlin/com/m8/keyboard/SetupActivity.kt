// app/src/main/kotlin/com/m8/keyboard/SetupActivity.kt
package com.m8.keyboard

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

private const val WRAP = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
private const val MATCH = android.view.ViewGroup.LayoutParams.MATCH_PARENT

/**
 * Launcher screen: enable/select the keyboard, try it, and change its settings. Everything that
 * can be changed is here, grouped by what it affects; the About page has the full feature guide.
 */
class SetupActivity : ThemedScreen() {

    /** What [recreate] saves so the rebuilt screen can carry on where this one was. */
    private class Restore(val text: String, val selStart: Int, val selEnd: Int, val scrollY: Int, val keyboardUp: Boolean)

    /** A named group of themes in the picker: the label shown and the value stored. */
    private class ThemeGroup(val title: String, val themes: List<Pair<String, Int>>)

    /** The parts of the custom mix: where each is saved, and what its plain version (none for the colours) is called. */
    private enum class MixPart(val title: String, val prefKey: String, val plain: String?) {
        COLOURS("Colours", KeyboardSettings.KEY_CUSTOM_COLOURS, null),
        KEYS("Keys", KeyboardSettings.KEY_CUSTOM_KEYS, "Plain keys"),
        BACKGROUND("Background", KeyboardSettings.KEY_CUSTOM_BACKGROUND, "Plain background"),
        ANIMATION("Animation", KeyboardSettings.KEY_CUSTOM_ANIMATION, "No animation"),
        FONT("Font", KeyboardSettings.KEY_CUSTOM_FONT, "Standard font"),
        ;

        fun source(parts: CustomParts): Int = when (this) {
            COLOURS -> parts.colours
            KEYS -> parts.keys
            BACKGROUND -> parts.background
            ANIMATION -> parts.animation
            FONT -> parts.font
        }
    }

    private var status: TextView? = null
    private var statusTitle: TextView? = null
    private var setupHeading: View? = null
    private var setupButtons: View? = null
    private var step1Button: TextView? = null
    private var step2Button: TextView? = null
    private var testField: EditText? = null
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Only registered while this screen is in front (onResume to onPause), so the keyboard
     * service itself never watches for changes. It is a field because SharedPreferences keeps
     * only a weak reference to its listeners.
     */
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == KeyboardSettings.KEY_CLEAR_TOKEN) return@OnSharedPreferenceChangeListener
        // A slider fires many times while it is dragged, so wait for the changes to settle.
        handler.removeCallbacks(restartKeyboard)
        handler.postDelayed(restartKeyboard, 300L)
    }

    /** Closes the keyboard and opens it again; it reloads its settings whenever it starts. */
    private val restartKeyboard = Runnable {
        val field = testField ?: return@Runnable
        if (!field.hasFocus()) return@Runnable // keyboard not up: the next start picks the settings up
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(field.windowToken, 0)
        handler.postDelayed({ if (field.hasFocus()) imm.showSoftInput(field, 0) }, 250L)
    }

    private companion object {
        /** Set just before the screen is rebuilt, read once by the new copy. */
        var restore: Restore? = null

        /**
         * Sections of the theme picker that are open, and the theme the picker last showed. [autoOpened] holds the
         * ones that opened by themselves to show the chosen theme (stepping with Previous and Next, or arriving
         * on the screen): those close again once the chosen theme has moved on. A section the person opened by
         * tapping its heading, or used by tapping a theme in it, stays open until they close it.
         */
        val openSections = HashSet<String>()
        val autoOpened = HashSet<String>()
        var lastThemeShown = -1

        val themeGroups: List<ThemeGroup> = listOf(
            ThemeGroup(
                "Essentials",
                listOf(
                    "Follow system" to KeyboardSettings.THEME_SYSTEM,
                    "P5" to KeyboardSettings.THEME_P5,
                    "Dark" to KeyboardSettings.THEME_DARK,
                    "Light" to KeyboardSettings.THEME_LIGHT,
                    "Custom mix" to KeyboardSettings.THEME_CUSTOM,
                ),
            ),
            ThemeGroup(
                "Phones & gadgets",
                listOf(
                    "Nokia 3210" to KeyboardSettings.THEME_NOKIA_3210,
                    "Nokia 3310" to KeyboardSettings.THEME_NOKIA,
                    "Nokia 3510i" to KeyboardSettings.THEME_NOKIA_3510I,
                    "Nokia 7210" to KeyboardSettings.THEME_NOKIA_7210,
                    "Sony Ericsson T610" to KeyboardSettings.THEME_SONY_ERICSSON_T610,
                    "Razr V3" to KeyboardSettings.THEME_RAZR_V3,
                    "Kawaii Flip Phone" to KeyboardSettings.THEME_KAWAII,
                    "Amber Backlight" to KeyboardSettings.THEME_AMBER_BACKLIGHT,
                    "Pager" to KeyboardSettings.THEME_PAGER,
                    "Palm Pilot" to KeyboardSettings.THEME_PALM,
                    "Walkman" to KeyboardSettings.THEME_WALKMAN,
                ),
            ),
            // Every games console, handhelds included, grouped by maker and in order of release.
            ThemeGroup(
                "Game consoles",
                listOf(
                    "Game Boy" to KeyboardSettings.THEME_GAME_BOY,
                    "SNES (US)" to KeyboardSettings.THEME_SNES,
                    "SNES (EU)" to KeyboardSettings.THEME_SNES_EU,
                    "Virtual Boy" to KeyboardSettings.THEME_VIRTUAL_BOY,
                    "N64" to KeyboardSettings.THEME_N64,
                    "Game Boy Advance" to KeyboardSettings.THEME_GAME_BOY_ADVANCE,
                    "GameCube" to KeyboardSettings.THEME_GAMECUBE,
                    "Saturn" to KeyboardSettings.THEME_SATURN,
                    "Dreamcast (US)" to KeyboardSettings.THEME_DREAMCAST,
                    "Dreamcast (JP)" to KeyboardSettings.THEME_DREAMCAST_JP,
                    "Dreamcast (EU)" to KeyboardSettings.THEME_DREAMCAST_EU,
                    "PlayStation" to KeyboardSettings.THEME_PLAYSTATION,
                    "PS2" to KeyboardSettings.THEME_PS2,
                    "PSP" to KeyboardSettings.THEME_PSP,
                    "Xbox" to KeyboardSettings.THEME_XBOX,
                    "N-Gage" to KeyboardSettings.THEME_NGAGE,
                ),
            ),
            ThemeGroup(
                "Video games",
                listOf(
                    "WipEout 2097" to KeyboardSettings.THEME_WIPEOUT_2097,
                    "WipEout HD" to KeyboardSettings.THEME_WIPEOUT_HD,
                    "MonsterGear" to KeyboardSettings.THEME_MONSTERGEAR,
                    "MonsterNav" to KeyboardSettings.THEME_MONSTERNAV,
                ),
            ),
            ThemeGroup(
                "Film & TV",
                listOf(
                    "Saved by the Bell" to KeyboardSettings.THEME_SATURDAY,
                    "Star Trek LCARS" to KeyboardSettings.THEME_LCARS,
                    "Evangelion Unit-00" to KeyboardSettings.THEME_EVANGELION_00,
                    "Evangelion Unit-01" to KeyboardSettings.THEME_EVANGELION_01,
                    "Evangelion Unit-02" to KeyboardSettings.THEME_EVANGELION_02,
                    "Serial Experiments Lain" to KeyboardSettings.THEME_LAIN,
                    "Ghost in the Shell" to KeyboardSettings.THEME_GHOST_IN_THE_SHELL,
                    "Hackers" to KeyboardSettings.THEME_HACKERS,
                    "Matrix" to KeyboardSettings.THEME_MATRIX,
                ),
            ),
            ThemeGroup(
                "Retro computers",
                listOf(
                    "MS-DOS / BIOS" to KeyboardSettings.THEME_BIOS,
                    "Terminal Green" to KeyboardSettings.THEME_TERMINAL_GREEN,
                    "Terminal Amber" to KeyboardSettings.THEME_TERMINAL_AMBER,
                    "Retro Desktop" to KeyboardSettings.THEME_RETRO_DESKTOP,
                    "Windows 95" to KeyboardSettings.THEME_WINDOWS_95,
                    "Windows XP" to KeyboardSettings.THEME_XP,
                ),
            ),
            ThemeGroup(
                "Web & media players",
                listOf(
                    "Netscape Navigator" to KeyboardSettings.THEME_NETSCAPE,
                    "GeoCities" to KeyboardSettings.THEME_GEOCITIES,
                    "Winamp" to KeyboardSettings.THEME_WINAMP,
                    "Media Player" to KeyboardSettings.THEME_MEDIA_PLAYER,
                ),
            ),
            // The themes with twinkling stars: they share the Star density slider.
            ThemeGroup(
                "Twinkling stars",
                listOf(
                    "Glitter Graphics" to KeyboardSettings.THEME_GLITTER,
                    "Night Sky" to KeyboardSettings.THEME_NIGHT_SKY,
                    "Winter" to KeyboardSettings.THEME_WINTER,
                    "Aurora" to KeyboardSettings.THEME_AURORA,
                    "Synthwave" to KeyboardSettings.THEME_SYNTHWAVE,
                    "Gold on Black" to KeyboardSettings.THEME_GOLD_ON_BLACK,
                ),
            ),
            // The themes that animate wherever you tap: they share the Effect amount slider.
            ThemeGroup(
                "Tap animations",
                listOf(
                    "Bubbles" to KeyboardSettings.THEME_BUBBLES,
                    "Embers" to KeyboardSettings.THEME_EMBERS,
                    "Sakura" to KeyboardSettings.THEME_SAKURA,
                    "Neon Ripples" to KeyboardSettings.THEME_RIPPLES,
                ),
            ),
        )

        fun groupFor(theme: Int): ThemeGroup? = themeGroups.firstOrNull { g -> g.themes.any { it.second == theme } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = newPage()
        val anchors = LinkedHashMap<String, View>() // section headings the jump bar scrolls to

        // ---- Header ----
        val headerRow = LinearLayout(this)
        headerRow.orientation = LinearLayout.HORIZONTAL
        headerRow.gravity = Gravity.TOP
        headerRow.addView(title("TXTM8 Keyboard"), LinearLayout.LayoutParams(0, WRAP, 1f))
        // A small gear in the top right opens the Settings page (typing, gestures, feedback, your words).
        val settingsButton = look.iconKey(this, KeyIcon.SETTINGS, false, 3, 3, 10, "Settings")
        settingsButton.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        headerRow.addView(settingsButton, LinearLayout.LayoutParams(dp(54), dp(50)))
        root.addView(headerRow, LinearLayout.LayoutParams(MATCH, WRAP))
        root.addView(body("Predictive number-pad typing, the way phones used to do it. Everything stays on your phone."))
        val jumpRow = LinearLayout(this)
        jumpRow.orientation = LinearLayout.HORIZONTAL
        val jumpScroll = HorizontalScrollView(this)
        jumpScroll.isHorizontalScrollBarEnabled = false
        jumpScroll.addView(jumpRow, FrameLayout.LayoutParams(WRAP, WRAP))
        root.addView(jumpScroll, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(14) })

        // ---- Setup ----
        // Shown while either step is outstanding. Once both are done only the message stays (see refreshStatus).
        val setupHead = sectionTitle("Setup")
        setupHeading = setupHead
        root.addView(setupHead)

        val statusCard = card()
        val statusTitleView = TextView(this)
        statusTitleView.textSize = 16f
        statusTitleView.typeface = look.typeface
        statusTitleView.setTextColor(look.text)
        statusTitleView.setPadding(0, dp(8), 0, dp(2))
        statusTitle = statusTitleView
        val statusView = body("")
        status = statusView
        statusCard.addView(statusTitleView)
        statusCard.addView(statusView)
        root.addView(statusCard, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(10) })

        val steps = LinearLayout(this)
        steps.orientation = LinearLayout.VERTICAL
        val enableButton = button("1. Enable the keyboard", action = true) {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        val switchButton = button("2. Switch to TXTM8 Keyboard", action = true) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        step1Button = enableButton
        step2Button = switchButton
        steps.addView(enableButton)
        steps.addView(switchButton, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        setupButtons = steps
        root.addView(steps, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })

        // ---- Try it ----
        addHeading(root, anchors, "Try it", "Type in the box, or flip through the themes without leaving it.")
        val tryCard = card()
        root.addView(tryCard)

        // Flip through the themes without leaving the test box; the keyboard reopens in each new theme.
        val flat = themeGroups.flatMap { it.themes }
        val currentTheme = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_DEFAULT)
        val position = flat.indexOfFirst { it.second == currentTheme }.coerceAtLeast(0)
        val themeName = TextView(this)
        themeName.text = flat[position].first
        themeName.textSize = 20f
        themeName.typeface = look.typeface
        themeName.setTextColor(look.text)
        themeName.gravity = Gravity.CENTER
        themeName.setPadding(0, dp(10), 0, dp(2))
        tryCard.addView(themeName, LinearLayout.LayoutParams(MATCH, WRAP))
        val themeMeta = TextView(this)
        themeMeta.text = groupFor(currentTheme)?.title.orEmpty() + "  \u00B7  " + (position + 1) + " of " + flat.size
        themeMeta.textSize = 12.5f
        themeMeta.setTextColor(look.subText)
        themeMeta.gravity = Gravity.CENTER
        themeMeta.setPadding(0, 0, 0, dp(8))
        tryCard.addView(themeMeta, LinearLayout.LayoutParams(MATCH, WRAP))
        val stepRow = LinearLayout(this)
        stepRow.orientation = LinearLayout.HORIZONTAL
        stepRow.addView(
            button("< Previous", action = false) { stepTheme(flat, -1) },
            LinearLayout.LayoutParams(0, WRAP, 1f).apply { rightMargin = dp(4) },
        )
        stepRow.addView(
            button("Next >", action = false) { stepTheme(flat, 1) },
            LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(4) },
        )
        tryCard.addView(stepRow, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(10) })

        val test = EditText(this)
        testField = test
        test.hint = "Tap here to try it"
        test.setHintTextColor(0xFF727272.toInt()) // dim grey
        test.setTextColor(look.text)
        test.textSize = 16f
        // A real multi-line text field: sentence capitals and suggestions only happen when the
        // field asks for them (a bare EditText has no capitalisation flag).
        test.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE
        test.minLines = 6
        test.gravity = Gravity.TOP or Gravity.START
        test.setPadding(dp(18), dp(14), dp(18), dp(14))
        test.background = look.keyBackground(density, false, 4, 4, 10)
        tryCard.addView(test, LinearLayout.LayoutParams(MATCH, WRAP))
        tryCard.addView(
            hint("Tip: hold # to switch between predictive and multi-tap, and tap the keyboard icon for the full keyboard.")
                .apply { setPadding(0, dp(8), 0, 0) },
        )

        // ---- Look ----
        addHeading(root, anchors, "Look", "Choose a theme, then adjust the keys and the animation.")
        addThemePicker(root)
        val lookCard = card()
        root.addView(lookCard, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) })
        // The P5 theme's accent colour: red, yellow, blue, purple, green, orange and six in-betweens.
        if (currentTheme == KeyboardSettings.THEME_CUSTOM) addCustomizeControls(lookCard)
        if (currentTheme == KeyboardSettings.THEME_P5 ||
            (currentTheme == KeyboardSettings.THEME_CUSTOM && CustomParts.load(prefs).uses(KeyboardSettings.THEME_P5))
        ) {
            addAccentPicker(lookCard)
        }
        addChoice(
            lookCard, "Key size", KeyboardSettings.KEY_SIZE,
            listOf("Small", "Medium", "Large"), listOf(0, 1, 2), 1,
        )
        // One control for each animation the theme has (twinkling stars, a tap animation), and the typing pulse for every theme.
        if (look.stars) addStarDensityControl(lookCard)
        if (look.tapEffect != null) addEffectAmountControl(lookCard)
        lookCard.addView(label("Typing pulse"))
        addPulseControls(lookCard)

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

        // The jump bar at the top: one chip per section.
        for ((name, heading) in anchors) {
            jumpRow.addView(
                chip(name) { pageScroll?.smoothScrollTo(0, (heading.top - dp(8)).coerceAtLeast(0)) },
                LinearLayout.LayoutParams(WRAP, WRAP).apply { rightMargin = dp(2) },
            )
        }

        showPage(root)

        // Rebuilt after a theme change (or a reset): put the text, scroll position and keyboard back as they were.
        val saved = restore
        restore = null
        if (saved != null) {
            test.setText(saved.text)
            test.setSelection(saved.selStart.coerceIn(0, test.length()), saved.selEnd.coerceIn(0, test.length()))
            pageScroll?.post { pageScroll?.scrollTo(0, saved.scrollY) }
            if (saved.keyboardUp) {
                test.postDelayed({
                    test.requestFocus()
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(test, 0)
                }, 350L)
            }
        }
    }

    /** A section heading with a line of explanation under it; the heading is remembered so the jump bar can scroll to it. */
    private fun addHeading(root: LinearLayout, anchors: MutableMap<String, View>, name: String, blurb: String) {
        val heading = sectionTitle(name)
        anchors[name] = heading
        root.addView(heading)
        root.addView(sectionBlurb(blurb))
    }

    /** A small tappable pill for the jump bar. */
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

    /** Remembers the test box and scroll position, so [onCreate] can put them back after the screen is rebuilt. */
    override fun recreate() {
        val field = testField
        restore = Restore(
            text = field?.text?.toString().orEmpty(),
            selStart = field?.selectionStart ?: 0,
            selEnd = field?.selectionEnd ?: 0,
            scrollY = pageScroll?.scrollY ?: 0,
            keyboardUp = field?.hasFocus() == true,
        )
        super.recreate()
    }

    /** Moves to the previous or next theme in the order they are listed, wrapping round at the ends. */
    private fun stepTheme(flat: List<Pair<String, Int>>, direction: Int) {
        val current = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_DEFAULT)
        val position = flat.indexOfFirst { it.second == current }.coerceAtLeast(0)
        chooseTheme(flat[(position + direction + flat.size) % flat.size].second)
    }

    private fun chooseTheme(value: Int) {
        // The first time the custom mix is chosen it starts as a copy of the theme being left, so nothing jumps.
        if (value == KeyboardSettings.THEME_CUSTOM && !CustomParts.isStored(prefs)) {
            val from = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_DEFAULT)
            CustomParts.save(prefs, CustomParts.allFrom(if (from == KeyboardSettings.THEME_CUSTOM) KeyboardSettings.THEME_DARK else from))
        }
        prefs.edit().putInt(KeyboardSettings.KEY_THEME, value).apply()
        recreate() // so this screen takes on the new theme's colours
    }

    /** The Customize controls of the custom mix: one button per part, showing which theme it comes from. */
    private fun addCustomizeControls(parent: LinearLayout) {
        parent.addView(label("Customize"))
        parent.addView(hint("Take each part of the look from any theme. Type in the box below to see the result."))
        val parts = CustomParts.load(prefs)
        for (part in MixPart.values()) {
            val source = part.source(parts)
            val name = if (source == CustomParts.NONE) part.plain.orEmpty() else themeName(source)
            parent.addView(
                button(part.title + ": " + name, action = false) { chooseMixPart(part) },
                LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) },
            )
        }
    }

    private fun themeName(id: Int): String =
        themeGroups.flatMap { it.themes }.firstOrNull { it.second == id }?.first ?: "Dark"

    /** Lets the person pick which theme [part] of the custom mix comes from; only themes that have something to offer are listed. */
    private fun chooseMixPart(part: MixPart) {
        val accent = p5Accent()
        val ids = ArrayList<Int>()
        val names = ArrayList<String>()
        part.plain?.let {
            ids.add(CustomParts.NONE)
            names.add(it)
        }
        for ((name, id) in themeGroups.flatMap { it.themes }) {
            if (id == KeyboardSettings.THEME_CUSTOM) continue
            val note = mixNote(part, KeyboardTheme.resolve(this, id, accent)) ?: continue
            ids.add(id)
            names.add(if (note.isEmpty()) name else "$name ($note)")
        }
        val checked = ids.indexOf(part.source(CustomParts.load(prefs)))
        AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
            .setTitle(part.title)
            .setSingleChoiceItems(names.toTypedArray(), checked) { dialog, which ->
                prefs.edit().putInt(part.prefKey, ids[which]).apply()
                dialog.dismiss()
                recreate() // so this screen takes on the new mix
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * A few words on what [theme] would give [part] of the mix, an empty string when there is nothing to add, or
     * null when the theme has nothing to offer for that part (it has plain keys, no animation and so on).
     */
    private fun mixNote(part: MixPart, theme: KeyboardTheme): String? {
        val notes = ArrayList<String>()
        when (part) {
            MixPart.COLOURS -> return ""
            MixPart.KEYS -> {
                when (theme.keyStyle) {
                    KeyStyle.GLOSS -> notes.add("glossy")
                    KeyStyle.LCD -> notes.add("LCD")
                    KeyStyle.MEMPHIS -> notes.add("Memphis")
                    KeyStyle.CHAMFER -> notes.add("cut corners")
                    KeyStyle.BEVEL -> notes.add("bevelled")
                    KeyStyle.PILL -> notes.add("pill")
                    KeyStyle.RETRO -> notes.add("retro")
                    KeyStyle.PIXEL -> notes.add("pixel")
                    KeyStyle.NOKIA -> notes.add("soft")
                    KeyStyle.SLANTED -> notes.add("slanted")
                    KeyStyle.DEFAULT -> if (theme.squareKeys) notes.add("square")
                }
            }
            MixPart.BACKGROUND -> {
                if (theme.backgroundBottom != 0) notes.add("gradient")
                if (theme.backdrop == Backdrop.HEARTS) notes.add("hearts")
                if (theme.backdrop == Backdrop.STRIPES) notes.add("stripes")
                if (theme.stripStyle == StripStyle.PIXEL_SCREEN) notes.add("pixel screen")
            }
            MixPart.ANIMATION -> {
                if (theme.stars) notes.add("stars")
                if (theme.tapEffect != null) notes.add("tap animation")
                if (theme.ambient != null) notes.add("moving background")
                if (theme.cursorColour != 0) notes.add("cursor")
            }
            MixPart.FONT -> {
                val special = theme.typeface !== android.graphics.Typeface.DEFAULT || theme.shoutLabels ||
                    theme.candidateFont != CandidateFont.NORMAL
                return if (special) "" else null
            }
        }
        return if (notes.isEmpty()) null else notes.joinToString(", ")
    }

    /**
     * The theme list, in collapsible sections. Tap a section's heading to open or close it. The section holding the
     * chosen theme is lit up; if it was closed it opens to show the theme, and closes again once the chosen theme
     * has moved on to another section (see [autoOpened]). Sections you open yourself stay open when the screen rebuilds.
     */
    private fun addThemePicker(root: LinearLayout) {
        root.addView(label("Theme"))
        val current = prefs.getInt(KeyboardSettings.KEY_THEME, KeyboardSettings.THEME_DEFAULT)
        if (current != lastThemeShown) {
            val leftTitle = groupFor(lastThemeShown)?.title
            val enteredTitle = groupFor(current)?.title
            // A section that only opened to show the theme closes again now the theme has left it.
            if (leftTitle != null && leftTitle != enteredTitle && leftTitle in autoOpened) {
                openSections.remove(leftTitle)
                autoOpened.remove(leftTitle)
            }
            // A new theme was picked (or this is the first time here): make sure it can be seen.
            if (enteredTitle != null && enteredTitle !in openSections) {
                openSections.add(enteredTitle)
                autoOpened.add(enteredTitle)
            }
            lastThemeShown = current
        }
        for (group in themeGroups) {
            val holdsCurrent = group.themes.any { it.second == current }
            val body = LinearLayout(this)
            body.orientation = LinearLayout.VERTICAL
            body.setPadding(dp(8), dp(2), 0, dp(6))
            body.visibility = if (group.title in openSections) View.VISIBLE else View.GONE
            for ((name, value) in group.themes) {
                val diamond = View(this)
                diamond.background = DiamondDrawable(value == current, look.accent, density)
                val row = optionRow(diamond, name)
                row.setOnClickListener {
                    autoOpened.remove(group.title) // used by hand now, so it stays open
                    chooseTheme(value)
                }
                body.addView(row)
            }

            val header = TextView(this)
            fun headerText() = (if (body.visibility == View.VISIBLE) "\u25BE  " else "\u25B8  ") +
                group.title.uppercase() + "  (" + group.themes.size + ")"
            header.text = headerText()
            header.textSize = 14f
            header.typeface = look.typeface
            // The section holding the chosen theme is drawn like an action button, so it is easy to spot.
            header.setTextColor(if (holdsCurrent) look.actionText else look.keyText)
            header.gravity = Gravity.CENTER_VERTICAL
            header.minHeight = dp(46)
            header.setPadding(dp(16), 0, dp(16), 0)
            header.background = look.keyBackground(density, holdsCurrent, 4, 3, 10)
            header.setOnClickListener {
                autoOpened.remove(group.title) // the person is in charge of this one now
                if (body.visibility == View.VISIBLE) {
                    body.visibility = View.GONE
                    openSections.remove(group.title)
                } else {
                    body.visibility = View.VISIBLE
                    openSections.add(group.title)
                }
                header.text = headerText()
            }
            root.addView(header, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(6) })
            root.addView(body, LinearLayout.LayoutParams(MATCH, WRAP))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        handler.removeCallbacks(restartKeyboard)
        super.onPause()
    }

    /** The keyboard picker and the system settings take focus without pausing this screen, so check again when focus returns. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) refreshStatus()
    }

    /**
     * Checks the two setup steps (this changes while the person is in system settings). While either is outstanding
     * the Setup heading and both buttons are shown, with the next step highlighted. When both are done they are
     * hidden entirely and only the confirmation message is left.
     */
    private fun refreshStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        val selected = current.startsWith("$packageName/")
        val done = enabled && selected

        val visibility = if (done) View.GONE else View.VISIBLE
        setupHeading?.visibility = visibility
        setupButtons?.visibility = visibility

        statusTitle?.text = when {
            done -> "\u2713 Setup complete"
            !enabled -> "Step 1 of 2"
            else -> "Step 2 of 2"
        }
        status?.text = when {
            !enabled -> "Not enabled yet. Do step 1, then step 2."
            !selected -> "Enabled, but not your current keyboard yet. Do step 2."
            else -> "Enabled and selected. You are all set: try it below."
        }
        styleStep(step1Button, if (enabled) "1. Keyboard enabled \u2713" else "1. Enable the keyboard", active = !enabled)
        styleStep(step2Button, if (selected) "2. TXTM8 Keyboard selected \u2713" else "2. Switch to TXTM8 Keyboard", active = enabled && !selected)
    }

    /** A setup button: solid accent while it is the step to do next, outlined once it is done (or not reachable yet). */
    private fun styleStep(button: TextView?, label: String, active: Boolean) {
        if (button == null) return
        button.text = label.uppercase()
        button.setTextColor(if (active) look.actionText else look.keyText)
        button.background = look.keyBackground(density, active, 4, 4, 10)
    }
}
