// app/src/main/kotlin/com/m8/keyboard/KeyboardSettings.kt
package com.m8.keyboard

import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View

/** User-adjustable options, stored in SharedPreferences by [SetupActivity]. */
data class KeyboardSettings(
    val theme: Int = THEME_RED,
    val keySize: Int = 1,
    val multiTapMs: Int = 800,
    val haptic: Boolean = true,
    val sound: Boolean = false,
    val reopenWord: Boolean = true,
    val pulse: Boolean = true,
    val pulseIntensity: Int = 50, // 0..100
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
        const val KEY_CLEAR_TOKEN = "clear_token"

        const val THEME_SYSTEM = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2
        const val THEME_RED = 3
        const val THEME_PURPLE = 4
        const val THEME_NOKIA = 5

        fun load(context: Context): KeyboardSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return KeyboardSettings(
                theme = p.getInt(KEY_THEME, THEME_RED),
                keySize = p.getInt(KEY_SIZE, 1),
                multiTapMs = p.getInt(KEY_TIMEOUT, 800),
                haptic = p.getBoolean(KEY_HAPTIC, true),
                sound = p.getBoolean(KEY_SOUND, false),
                reopenWord = p.getBoolean(KEY_REOPEN, true),
                pulse = p.getBoolean(KEY_PULSE, true),
                pulseIntensity = p.getInt(KEY_PULSE_LEVEL, 50),
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
