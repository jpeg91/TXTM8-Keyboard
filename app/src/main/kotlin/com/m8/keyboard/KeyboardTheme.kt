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
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                else -> if (night) DARK else LIGHT
            }
        }
    }
}
