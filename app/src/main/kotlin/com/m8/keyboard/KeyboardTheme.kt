// app/src/main/kotlin/com/m8/keyboard/KeyboardTheme.kt
package com.m8.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build

/**
 * Colours (and, for P5, shapes and type) for the keyboard views.
 * "Action" keys (DEL, ENTER, shift) can be drawn differently from ordinary keys.
 */
class KeyboardTheme(
    val background: Int,
    val strip: Int,
    val key: Int,
    val keyPressed: Int,
    val text: Int,
    val accent: Int,
    val accentText: Int,
    val actionKey: Int = key,
    val actionKeyPressed: Int = keyPressed,
    val actionText: Int = text,
    val subText: Int = text,
    val isP5: Boolean = false,
    val border: Int = 0,
    val shadow: Int = 0,
    val deep: Int = 0,
    val typeface: Typeface = Typeface.DEFAULT,
) {
    companion object {
        val DARK = KeyboardTheme(
            background = 0xFF15171A.toInt(),
            strip = 0xFF1E2126.toInt(),
            key = 0xFF2B2F36.toInt(),
            keyPressed = 0xFF4A515C.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFC107.toInt(),
            accentText = 0xFF000000.toInt(),
        )

        val LIGHT = KeyboardTheme(
            background = 0xFFD3D6DB.toInt(),
            strip = 0xFFE8EAEE.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFB4BAC4.toInt(),
            text = 0xFF111111.toInt(),
            accent = 0xFFFFC107.toInt(),
            accentText = 0xFF000000.toInt(),
        )

        /**
         * WebMChan's Persona 5 look: black, white outlines, a bold accent, slanted keys with a
         * dark-accent drop shadow and heavy italic type. The three accent shades are the app's
         * Red / RedBright / RedDeep (or their purple stand-ins in the "princetheme").
         */
        private fun p5(red: Int, redBright: Int, redDeep: Int) = KeyboardTheme(
            background = 0xFF080808.toInt(),        // P5.Black
            strip = Color.TRANSPARENT,              // candidates sit on the pinstriped backdrop
            key = 0xFF151515.toInt(),               // P5.Ink
            keyPressed = red,
            text = 0xFFFFFFFF.toInt(),              // P5.White
            accent = red,
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = red,
            actionKeyPressed = redBright,
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFC4C4C4.toInt(),           // P5.Grey
            isP5 = true,
            border = 0xFFFFFFFF.toInt(),
            shadow = redDeep,
            deep = redDeep,
            typeface = heavyItalic(),
        )

        /** WebMChan's red P5 theme. */
        val P5 = p5(0xFFE60012.toInt(), 0xFFFF4553.toInt(), 0xFF7A000B.toInt())

        /** WebMChan's "princetheme" (Audio Mode): the same look with purple in place of red. */
        val P5_PURPLE = p5(0xFF755FBE.toInt(), 0xFFA897EC.toInt(), 0xFF3A2E6B.toInt())

        private fun heavyItalic(): Typeface =
            if (Build.VERSION.SDK_INT >= 28) {
                Typeface.create(Typeface.SANS_SERIF, 900, true)
            } else {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
            }

        fun resolve(context: Context, setting: Int): KeyboardTheme {
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            return when (setting) {
                KeyboardSettings.THEME_P5 -> P5
                KeyboardSettings.THEME_P5_PURPLE -> P5_PURPLE
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                else -> if (night) DARK else LIGHT
            }
        }
    }
}
