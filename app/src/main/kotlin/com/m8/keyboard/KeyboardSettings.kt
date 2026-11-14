// app/src/main/kotlin/com/m8/keyboard/KeyboardSettings.kt
package com.m8.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View

/** User-adjustable options, stored in SharedPreferences by [SetupActivity]. */
data class KeyboardSettings(
    val theme: Int = THEME_P5,
    val p5Accent: Int = P5_ACCENT_RED,
    val keySize: Int = 1,
    val multiTapMs: Int = 800,
    val haptic: Boolean = true,
    val sound: Boolean = false,
    val reopenWord: Boolean = true,
    val pulse: Boolean = true,
    val pulseIntensity: Int = 50, // 0..100
    val nextWord: Boolean = true,
    val autoCap: Boolean = true,
    val doubleSpace: Boolean = true,
    val fixContractions: Boolean = true,
    val learnWords: Boolean = true,
    val slideGestures: Boolean = true,
    val slideSpeed: Int = 1, // 0 slow, 1 normal, 2 fast
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
        const val KEY_PULSE = "pulse"
        const val KEY_PULSE_LEVEL = "pulse_level"
        const val KEY_NEXT_WORD = "next_word"
        const val KEY_AUTO_CAP = "auto_cap"
        const val KEY_DOUBLE_SPACE = "double_space"
        const val KEY_CONTRACTIONS = "fix_contractions"
        const val KEY_LEARN = "learn_words"
        const val KEY_SLIDE = "slide_gestures"
        const val KEY_SLIDE_SPEED = "slide_speed"
        const val KEY_CLEAR_TOKEN = "clear_token"
        const val KEY_P5_ACCENT = "p5_accent"

        const val THEME_SYSTEM = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2
        const val THEME_P5 = 3 // was the red theme: its accent colour is now chosen separately
        /** Was the purple theme, now the P5 theme with the purple accent. */
        const val THEME_RETIRED_PURPLE = 4
        const val THEME_NOKIA = 5
        /** Was the Motorola RAZR theme, which has been removed: anyone who had it gets the P5 theme. */
        const val THEME_RETIRED_RAZR = 6
        const val THEME_GAME_BOY = 7
        const val THEME_GAME_BOY_ADVANCE = 8
        const val THEME_GLITTER = 9

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
                theme = p.getInt(KEY_THEME, THEME_P5),
                p5Accent = p.getInt(KEY_P5_ACCENT, P5_ACCENT_RED).coerceIn(0, P5_ACCENTS.lastIndex),
                keySize = p.getInt(KEY_SIZE, 1),
                multiTapMs = p.getInt(KEY_TIMEOUT, 800),
                haptic = p.getBoolean(KEY_HAPTIC, true),
                sound = p.getBoolean(KEY_SOUND, false),
                reopenWord = p.getBoolean(KEY_REOPEN, true),
                pulse = p.getBoolean(KEY_PULSE, true),
                pulseIntensity = p.getInt(KEY_PULSE_LEVEL, 50),
                nextWord = p.getBoolean(KEY_NEXT_WORD, true),
                autoCap = p.getBoolean(KEY_AUTO_CAP, true),
                doubleSpace = p.getBoolean(KEY_DOUBLE_SPACE, true),
                fixContractions = p.getBoolean(KEY_CONTRACTIONS, true),
                learnWords = p.getBoolean(KEY_LEARN, true),
                slideGestures = p.getBoolean(KEY_SLIDE, true),
                slideSpeed = p.getInt(KEY_SLIDE_SPEED, 1),
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
