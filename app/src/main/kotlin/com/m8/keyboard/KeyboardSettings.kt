// app/src/main/kotlin/com/m8/keyboard/KeyboardSettings.kt
package com.m8.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View

/** User-adjustable options, stored in SharedPreferences by [SetupActivity]. */
data class KeyboardSettings(
    val theme: Int = THEME_DEFAULT,
    val p5Accent: Int = P5_ACCENT_RED,
    val keySize: Int = 1,
    val multiTapMs: Int = 800,
    val haptic: Boolean = true,
    val sound: Boolean = false,
    val reopenWord: Boolean = true,
    val tapReopen: Boolean = true,
    val pulse: Boolean = true,
    val pulseIntensity: Int = 50, // 0..100
    val starDensity: Int = 50, // 0..100: how many twinkling stars appear (the twinkling-star themes)
    val effectAmount: Int = 50, // 0..100: how much appears when you tap (the tap-animation themes)
    val nextWord: Boolean = true,
    val autoCap: Boolean = true,
    val doubleSpace: Boolean = true,
    val fixContractions: Boolean = true,
    val learnWords: Boolean = true,
    val slideGestures: Boolean = true,
    val slideSpeed: Int = 1, // 0 slow, 1 normal, 2 fast
    val custom: CustomParts = CustomParts(), // where each part of the "Custom mix" theme comes from
    val defaultQwerty: Boolean = false, // new typing sessions open on the QWERTY keyboard instead of the number pad
    val startMode: Int = START_PREDICTIVE, // the input mode a new typing session starts in
    val spaceAfterSuggestion: Boolean = true, // tapping a suggestion also types a space after it
    val capitaliseI: Boolean = true, // I, I'm, I'll and so on always get a capital I
    val qwertyNumberRow: Boolean = false, // a row of digits above the letters on the QWERTY keyboard
    val swapPadKeys: Boolean = false, // number pad: backspace before enter in the top bar
    val swapQwertyKeys: Boolean = false, // QWERTY: enter and backspace trade places
    val swapEmojiKeys: Boolean = false, // emoji page: backspace before enter in the top bar
) {
    /** How strong the typing pulse is, 0 (off) to 0.85. */
    val pulseStrength: Float
        get() = if (pulse) pulseIntensity.coerceIn(0, 100) / 100f * 0.85f else 0f

    val keyHeightDp: Int
        get() = when (keySize) {
            0 -> 48
            2 -> 66
            else -> 56
        }

    companion object {
        const val PREFS = "m8_settings"

        // "theme_v2": a new key so everyone starts on the red theme (the old "theme" key is ignored).
        const val KEY_THEME = "theme_v2"
        const val KEY_SIZE = "key_size"
        const val KEY_TIMEOUT = "multitap_ms"
        const val KEY_HAPTIC = "haptic"
        const val KEY_SOUND = "sound"
        const val KEY_REOPEN = "reopen_word"
    const val KEY_TAP_REOPEN = "tap_reopen_word"
        const val KEY_PULSE = "pulse"
        const val KEY_PULSE_LEVEL = "pulse_level"
        const val KEY_STAR_DENSITY = "star_density"
        const val KEY_EFFECT_AMOUNT = "effect_amount"
        const val KEY_NEXT_WORD = "next_word"
        const val KEY_AUTO_CAP = "auto_cap"
        const val KEY_DOUBLE_SPACE = "double_space"
        const val KEY_CONTRACTIONS = "fix_contractions"
        const val KEY_LEARN = "learn_words"
        const val KEY_SLIDE = "slide_gestures"
        const val KEY_SLIDE_SPEED = "slide_speed"
        const val KEY_CLEAR_TOKEN = "clear_token"
        const val KEY_P5_ACCENT = "p5_accent"
        const val KEY_CUSTOM_COLOURS = "custom_colours"
        const val KEY_CUSTOM_KEYS = "custom_keys"
        const val KEY_CUSTOM_BACKGROUND = "custom_background"
        const val KEY_CUSTOM_ANIMATION = "custom_animation"
        const val KEY_CUSTOM_FONT = "custom_font"
        const val KEY_DEFAULT_QWERTY = "default_qwerty"
        const val KEY_START_MODE = "start_mode"
        const val KEY_SPACE_AFTER_SUGGESTION = "space_after_suggestion"
        const val KEY_CAPITALISE_I = "capitalise_i"
        const val KEY_QWERTY_NUMBER_ROW = "qwerty_number_row"
        const val KEY_SWAP_PAD = "swap_pad_keys"
        const val KEY_SWAP_QWERTY = "swap_qwerty_keys"
        const val KEY_SWAP_EMOJI = "swap_emoji_keys"

        /** The input mode a new typing session starts in. */
        const val START_PREDICTIVE = 0
        const val START_MULTI_TAP = 1

        const val THEME_SYSTEM = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2
        const val THEME_P5 = 3 // was the red theme: its accent colour is now chosen separately

        /** What a new install starts with: follow the phone's light and dark mode. */
        const val THEME_DEFAULT = THEME_SYSTEM
        /** Was the purple theme, now the P5 theme with the purple accent. */
        const val THEME_RETIRED_PURPLE = 4
        const val THEME_NOKIA = 5
        /** Was the Motorola RAZR theme, which has been removed: anyone who had it gets the P5 theme. */
        const val THEME_RETIRED_RAZR = 6
        const val THEME_GAME_BOY = 7
        const val THEME_GAME_BOY_ADVANCE = 8
        const val THEME_GLITTER = 9
        const val THEME_NIGHT_SKY = 10
        const val THEME_WINTER = 11
        const val THEME_AURORA = 12
        const val THEME_SYNTHWAVE = 13
        const val THEME_GOLD_ON_BLACK = 14
        const val THEME_AMBER_BACKLIGHT = 15
        const val THEME_TERMINAL_GREEN = 16
        const val THEME_TERMINAL_AMBER = 17
        const val THEME_RETRO_DESKTOP = 18
        const val THEME_BUBBLES = 19
        const val THEME_EMBERS = 20
        const val THEME_SAKURA = 21
        const val THEME_RIPPLES = 22
        const val THEME_SNES = 23
        const val THEME_PLAYSTATION = 24
        const val THEME_DREAMCAST = 25
        const val THEME_GAMECUBE = 26
        const val THEME_SNES_EU = 27
        const val THEME_DREAMCAST_EU = 28
        const val THEME_N64 = 29
        const val THEME_XBOX = 30
        const val THEME_SATURN = 31
        const val THEME_DREAMCAST_JP = 32
        const val THEME_PS2 = 33
        const val THEME_XP = 34
        const val THEME_MEDIA_PLAYER = 35
        const val THEME_BIOS = 36
        const val THEME_GEOCITIES = 37
        const val THEME_PALM = 38
        const val THEME_WALKMAN = 39
        const val THEME_VIRTUAL_BOY = 40
        const val THEME_PSP = 41
        const val THEME_MATRIX = 42
        const val THEME_SATURDAY = 43 // the bright, Memphis-patterned "Saved by the Bell" theme
        const val THEME_NOKIA_3210 = 44
        const val THEME_NOKIA_3510I = 45
        const val THEME_NOKIA_7210 = 46
        const val THEME_SONY_ERICSSON_T610 = 47
        const val THEME_NGAGE = 48
        const val THEME_RAZR_V3 = 49 // the new RAZR V3 theme (THEME_RETIRED_RAZR is the old, removed one)
        const val THEME_KAWAII = 50
        const val THEME_EVANGELION_00 = 51
        const val THEME_MONSTERGEAR = 52
        const val THEME_EVANGELION_01 = 53
        const val THEME_EVANGELION_02 = 54
        const val THEME_MONSTERNAV = 55
        const val THEME_GHOST_IN_THE_SHELL = 56
        const val THEME_LCARS = 57
        const val THEME_HACKERS = 58
        const val THEME_LAIN = 59
        const val THEME_WINDOWS_95 = 60
        const val THEME_WINAMP = 61
        const val THEME_NETSCAPE = 62
        const val THEME_WIPEOUT_2097 = 63
        const val THEME_WIPEOUT_HD = 64
        const val THEME_PAGER = 65
        /** The mix-and-match theme: each part of the look comes from the theme chosen for it (see [CustomParts]). */
        const val THEME_CUSTOM = 66
        const val THEME_SUPER_MARIO_BROS = 67
        const val THEME_SONIC_THE_HEDGEHOG = 68
        const val THEME_DOOM = 69
        const val THEME_FINAL_FANTASY_VII = 70
        const val THEME_WORMS_ARMAGEDDON = 71
        const val THEME_THE_SIMS = 72
        const val THEME_GTA_VICE_CITY = 73
        const val THEME_GTA_SAN_ANDREAS = 74
        const val THEME_PIP_BOY = 75

        /** Index in [P5_ACCENTS] of the original red (the default) and of the purple. */
        const val P5_ACCENT_RED = 0
        const val P5_ACCENT_PURPLE = 3

        /**
         * Moves anyone on a theme that no longer exists to its replacement: the purple theme
         * becomes P5 with the purple accent, and the removed RAZR theme becomes P5.
         */
        fun migrateThemes(p: SharedPreferences) {
            val stored = p.getInt(KEY_THEME, THEME_P5)
            if (stored != THEME_RETIRED_PURPLE && stored != THEME_RETIRED_RAZR) return
            val edit = p.edit().putInt(KEY_THEME, THEME_P5)
            if (stored == THEME_RETIRED_PURPLE) edit.putInt(KEY_P5_ACCENT, P5_ACCENT_PURPLE)
            edit.apply()
        }

        fun load(context: Context): KeyboardSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            migrateThemes(p)
            return KeyboardSettings(
                theme = p.getInt(KEY_THEME, THEME_DEFAULT),
                p5Accent = p.getInt(KEY_P5_ACCENT, P5_ACCENT_RED).coerceIn(0, P5_ACCENTS.lastIndex),
                keySize = p.getInt(KEY_SIZE, 1),
                multiTapMs = p.getInt(KEY_TIMEOUT, 800),
                haptic = p.getBoolean(KEY_HAPTIC, true),
                sound = p.getBoolean(KEY_SOUND, false),
                reopenWord = p.getBoolean(KEY_REOPEN, true),
                tapReopen = p.getBoolean(KEY_TAP_REOPEN, true),
                pulse = p.getBoolean(KEY_PULSE, true),
                pulseIntensity = p.getInt(KEY_PULSE_LEVEL, 50),
                starDensity = p.getInt(KEY_STAR_DENSITY, 50).coerceIn(0, 100),
                effectAmount = p.getInt(KEY_EFFECT_AMOUNT, 50).coerceIn(0, 100),
                nextWord = p.getBoolean(KEY_NEXT_WORD, true),
                autoCap = p.getBoolean(KEY_AUTO_CAP, true),
                doubleSpace = p.getBoolean(KEY_DOUBLE_SPACE, true),
                fixContractions = p.getBoolean(KEY_CONTRACTIONS, true),
                learnWords = p.getBoolean(KEY_LEARN, true),
                slideGestures = p.getBoolean(KEY_SLIDE, true),
                slideSpeed = p.getInt(KEY_SLIDE_SPEED, 1),
                custom = CustomParts.load(p),
                defaultQwerty = p.getBoolean(KEY_DEFAULT_QWERTY, false),
                startMode = p.getInt(KEY_START_MODE, START_PREDICTIVE).coerceIn(START_PREDICTIVE, START_MULTI_TAP),
                spaceAfterSuggestion = p.getBoolean(KEY_SPACE_AFTER_SUGGESTION, true),
                capitaliseI = p.getBoolean(KEY_CAPITALISE_I, true),
                qwertyNumberRow = p.getBoolean(KEY_QWERTY_NUMBER_ROW, false),
                swapPadKeys = p.getBoolean(KEY_SWAP_PAD, false),
                swapQwertyKeys = p.getBoolean(KEY_SWAP_QWERTY, false),
                swapEmojiKeys = p.getBoolean(KEY_SWAP_EMOJI, false),
            )
        }
    }
}

/** Vibration / click sound for a key press, according to the settings. */
fun KeyboardSettings.feedback(view: View, isLong: Boolean = false) {
    if (haptic) {
        view.performHapticFeedback(
            if (isLong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP,
        )
    }
    if (sound) {
        val am = view.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
    }
}

/** A light tick for each step of a slide (cursor or selection), if vibration is on. */
fun KeyboardSettings.tick(view: View) {
    if (haptic) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/** How far a finger slides for one step (one character), in dp: a shorter distance is a faster slide. */
fun KeyboardSettings.slideStepDp(): Float = when (slideSpeed) {
    0 -> 16f
    2 -> 7f
    else -> 11f
}

/**
 * The "Custom mix" theme: for each part of the look, the theme it is taken from (a `KeyboardSettings.THEME_*` id), or
 * [NONE] for the plain version of that part. The colours always come from a theme, so they have no plain version.
 */
data class CustomParts(
    val colours: Int = KeyboardSettings.THEME_DARK,
    val keys: Int = KeyboardSettings.THEME_DARK,
    val background: Int = KeyboardSettings.THEME_DARK,
    val animation: Int = KeyboardSettings.THEME_DARK,
    val font: Int = KeyboardSettings.THEME_DARK,
) {
    /** True when [theme] is the source of any part. */
    fun uses(theme: Int): Boolean = colours == theme || keys == theme || background == theme || animation == theme || font == theme

    companion object {
        /** The plain version of a part: ordinary rounded keys, a flat background, no animation, the standard font. */
        const val NONE = -1

        /** Every part taken from [theme]: the starting point when someone first switches to the custom mix. */
        fun allFrom(theme: Int) = CustomParts(theme, theme, theme, theme, theme)

        fun isStored(p: SharedPreferences): Boolean = p.contains(KeyboardSettings.KEY_CUSTOM_COLOURS)

        fun load(p: SharedPreferences): CustomParts = CustomParts(
            colours = p.getInt(KeyboardSettings.KEY_CUSTOM_COLOURS, KeyboardSettings.THEME_DARK),
            keys = p.getInt(KeyboardSettings.KEY_CUSTOM_KEYS, KeyboardSettings.THEME_DARK),
            background = p.getInt(KeyboardSettings.KEY_CUSTOM_BACKGROUND, KeyboardSettings.THEME_DARK),
            animation = p.getInt(KeyboardSettings.KEY_CUSTOM_ANIMATION, KeyboardSettings.THEME_DARK),
            font = p.getInt(KeyboardSettings.KEY_CUSTOM_FONT, KeyboardSettings.THEME_DARK),
        )

        fun save(p: SharedPreferences, parts: CustomParts) {
            p.edit()
                .putInt(KeyboardSettings.KEY_CUSTOM_COLOURS, parts.colours)
                .putInt(KeyboardSettings.KEY_CUSTOM_KEYS, parts.keys)
                .putInt(KeyboardSettings.KEY_CUSTOM_BACKGROUND, parts.background)
                .putInt(KeyboardSettings.KEY_CUSTOM_ANIMATION, parts.animation)
                .putInt(KeyboardSettings.KEY_CUSTOM_FONT, parts.font)
                .apply()
        }
    }
}
