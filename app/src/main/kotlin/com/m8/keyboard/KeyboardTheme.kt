// app/src/main/kotlin/com/m8/keyboard/KeyboardTheme.kt
package com.m8.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build

/**
 * Colours (and, for the red and purple themes, shapes and type) for the keyboard views.
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
    val isSlanted: Boolean = false,
    val border: Int = 0,
    val shadow: Int = 0,
    val deep: Int = 0,
    val isNokia: Boolean = false,
    val isRazr: Boolean = false,
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
         * The bold look shared by the red and purple themes: black, white outlines, a strong
         * accent, slanted keys with a dark-accent drop shadow and heavy italic type. The three
         * accent shades are the main colour, a brighter one (pressed action keys) and a deep one
         * (shadows and corner wedges).
         */
        private fun accented(red: Int, redBright: Int, redDeep: Int) = KeyboardTheme(
            background = 0xFF080808.toInt(),        // near black
            strip = Color.TRANSPARENT,              // candidates sit on the pinstriped backdrop
            key = 0xFF151515.toInt(),               // ink
            keyPressed = red,
            text = 0xFFFFFFFF.toInt(),              // white
            accent = red,
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = red,
            actionKeyPressed = redBright,
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFC4C4C4.toInt(),           // light grey
            isSlanted = true,
            border = 0xFFFFFFFF.toInt(),
            shadow = redDeep,
            deep = redDeep,
            typeface = heavyItalic(),
        )

        /** The red theme. */
        val RED = accented(0xFFE60012.toInt(), 0xFFFF4553.toInt(), 0xFF7A000B.toInt())

        /** The purple theme: the same look with purple in place of red. */
        val PURPLE = accented(0xFF755FBE.toInt(), 0xFFA897EC.toInt(), 0xFF3A2E6B.toInt())

        /** Original 3310-inspired blue-grey shell and soft green LCD. */
        val NOKIA = KeyboardTheme(
            background = 0xFF8799A6.toInt(),
            strip = 0xFFB7C99A.toInt(),
            key = 0xFFD5D9D7.toInt(),
            keyPressed = 0xFFADB8B6.toInt(),
            text = 0xFF26343A.toInt(),
            accent = 0xFF526B4B.toInt(),
            accentText = 0xFFE4EBCF.toInt(),
            actionKey = 0xFFBFC8C5.toInt(),
            actionKeyPressed = 0xFF9EAAA7.toInt(),
            actionText = 0xFF26343A.toInt(),
            subText = 0xFF53645A.toInt(),
            shadow = 0xFF53636A.toInt(),
            deep = 0xFF53636A.toInt(),
            isNokia = true,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Motorola RAZR V3: a dark etched-metal keypad with electroluminescent blue numerals.
         * See RazrTheme.kt for how the keys, the navigation ring and the glow are drawn.
         */
        val RAZR = KeyboardTheme(
            background = 0xFF0D1115.toInt(),
            strip = 0xFF020A11.toInt(),
            key = 0xFF2C353C.toInt(),
            keyPressed = 0xFF1C5F86.toInt(),
            text = 0xFF7FD6FF.toInt(),           // numerals at rest: backlit pale blue
            accent = 0xFF1E9BE0.toInt(),
            accentText = 0xFF03131D.toInt(),     // dark text on the lit candidate chip
            actionKey = 0xFF343D45.toInt(),
            actionKeyPressed = 0xFF1C5F86.toInt(),
            actionText = 0xFF8DDCFF.toInt(),
            subText = 0xFF4C9CC4.toInt(),
            shadow = 0xFF03060A.toInt(),
            deep = 0xFF000000.toInt(),
            isRazr = true,
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

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
                KeyboardSettings.THEME_RED -> RED
                KeyboardSettings.THEME_PURPLE -> PURPLE
                KeyboardSettings.THEME_NOKIA -> NOKIA
                KeyboardSettings.THEME_RAZR -> RAZR
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                else -> if (night) DARK else LIGHT
            }
        }
    }
}
