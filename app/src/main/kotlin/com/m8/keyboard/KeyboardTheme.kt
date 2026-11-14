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
    val isGameBoyAdvance: Boolean = false,
    val isGlitter: Boolean = false,
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



        /** Original Game Boy-inspired grey-violet shell, green LCD and pink action buttons. */
        val GAME_BOY = KeyboardTheme(
            background = 0xFFAAA9B5.toInt(),
            strip = 0xFF9BBC0F.toInt(),
            key = 0xFFD9D8E0.toInt(),
            keyPressed = 0xFFBFC0CC.toInt(),
            text = 0xFF303044.toInt(),
            accent = 0xFF306230.toInt(),
            accentText = 0xFFD8E8A5.toInt(),
            actionKey = 0xFFCF7898.toInt(),
            actionKeyPressed = 0xFFB9567B.toInt(),
            actionText = 0xFF352536.toInt(),
            subText = 0xFF34452B.toInt(),
            shadow = 0xFF777684.toInt(),
            deep = 0xFF777684.toInt(),
            isNokia = true,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Purple Game Boy Advance shell with a bright, pixel-art inspired candidate display. */
        val GAME_BOY_ADVANCE = KeyboardTheme(
            background = 0xFF59417D.toInt(),
            strip = 0xFF201A3B.toInt(),
            key = 0xFF34264F.toInt(),
            keyPressed = 0xFF514078.toInt(),
            text = 0xFFF5F0FF.toInt(),
            accent = 0xFFFF5C9A.toInt(),
            accentText = 0xFF24152F.toInt(),
            actionKey = 0xFFB65AA4.toInt(),
            actionKeyPressed = 0xFFE5B94B.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFD0C2E8.toInt(),
            shadow = 0xFF302044.toInt(),
            deep = 0xFF302044.toInt(),
            isNokia = true,
            isGameBoyAdvance = true,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Hot-pink, early-2000s glitter-graphics inspired theme with chunky pixel keys. */
        val GLITTER = KeyboardTheme(
            background = 0xFFFF69B4.toInt(),
            strip = 0xFF8F145C.toInt(),
            key = 0xFFFFA6D2.toInt(),
            keyPressed = 0xFFFF4FA8.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFD7F0.toInt(),
            accentText = 0xFF74104C.toInt(),
            actionKey = 0xFFFF3F9E.toInt(),
            actionKeyPressed = 0xFFFFD24A.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFFFE2F2.toInt(),
            shadow = 0xFF9B235F.toInt(),
            deep = 0xFF8F145C.toInt(),
            isGlitter = true,
            typeface = Typeface.create("monospace", Typeface.BOLD),
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
                KeyboardSettings.THEME_RED, KeyboardSettings.THEME_RETIRED_RAZR -> RED
                KeyboardSettings.THEME_PURPLE -> PURPLE
                KeyboardSettings.THEME_NOKIA -> NOKIA
                KeyboardSettings.THEME_GAME_BOY -> GAME_BOY
                KeyboardSettings.THEME_GAME_BOY_ADVANCE -> GAME_BOY_ADVANCE
                KeyboardSettings.THEME_GLITTER -> GLITTER
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                else -> if (night) DARK else LIGHT
            }
        }
    }
}
