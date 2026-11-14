// app/src/main/kotlin/com/m8/keyboard/KeyboardTheme.kt
package com.m8.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build

/**
 * Colours (and, for the P5 theme, shapes and type) for the keyboard views.
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
    val isGameBoy: Boolean = false,
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
         * The P5 theme: black, white outlines, one strong accent colour, slanted keys with a
         * dark-accent drop shadow and heavy italic type. One theme per entry in [P5_ACCENTS], built
         * once so the keyboard can tell whether the theme has changed by identity.
         */
        private val P5_THEMES: List<KeyboardTheme> = P5_ACCENTS.map { accent ->
            val onAccent = if (accent.darkText) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
            KeyboardTheme(
                background = 0xFF080808.toInt(),        // near black
                strip = Color.TRANSPARENT,              // candidates sit on the pinstriped backdrop
                key = 0xFF151515.toInt(),               // ink
                keyPressed = accent.main,
                text = 0xFFFFFFFF.toInt(),              // white
                accent = accent.main,
                accentText = onAccent,
                actionKey = accent.main,
                actionKeyPressed = accent.bright,
                actionText = onAccent,
                subText = 0xFFC4C4C4.toInt(),           // light grey
                isSlanted = true,
                border = 0xFFFFFFFF.toInt(),
                shadow = accent.deep,
                deep = accent.deep,
                typeface = heavyItalic(),
            )
        }

        /** The P5 theme in the accent colour at [accentIndex] of [P5_ACCENTS]. */
        fun p5(accentIndex: Int): KeyboardTheme = P5_THEMES[accentIndex.coerceIn(0, P5_THEMES.lastIndex)]

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
            background = 0xFFB7B5C3.toInt(),
            strip = 0xFF9BBC0F.toInt(),
            key = 0xFFE6E4EC.toInt(),
            keyPressed = 0xFFC9C8D5.toInt(),
            text = 0xFF303044.toInt(),
            accent = 0xFF306230.toInt(),
            accentText = 0xFFD8E8A5.toInt(),
            actionKey = 0xFFD98AA7.toInt(),
            actionKeyPressed = 0xFFBD6488.toInt(),
            actionText = 0xFF352536.toInt(),
            subText = 0xFF34452B.toInt(),
            shadow = 0xFF777684.toInt(),
            deep = 0xFF777684.toInt(),
            isGameBoy = true,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Purple Game Boy Advance shell with a bright, pixel-art inspired candidate display. */
        val GAME_BOY_ADVANCE = KeyboardTheme(
            background = 0xFF7856A4.toInt(),
            strip = 0xFF5A3D82.toInt(),
            key = 0xFFC2A6E8.toInt(),
            keyPressed = 0xFFA98AD7.toInt(),
            text = 0xFF2D2041.toInt(),
            accent = 0xFFF3C85B.toInt(),
            accentText = 0xFF302044.toInt(),
            actionKey = 0xFFFF8FB8.toInt(),
            actionKeyPressed = 0xFFFFE16A.toInt(),
            actionText = 0xFF2D2041.toInt(),
            subText = 0xFFEADDF9.toInt(),
            shadow = 0xFF3D285E.toInt(),
            deep = 0xFF3D285E.toInt(),
            isGameBoy = true,
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

        /** The theme for the [setting] (see [KeyboardSettings]); [p5Accent] only matters for the P5 theme. */
        fun resolve(context: Context, setting: Int, p5Accent: Int = 0): KeyboardTheme {
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            return when (setting) {
                KeyboardSettings.THEME_P5, KeyboardSettings.THEME_RETIRED_PURPLE, KeyboardSettings.THEME_RETIRED_RAZR ->
                    p5(p5Accent)
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

/** One selectable accent colour for the P5 theme: the main colour, a brighter one (pressed action keys) and a deep one (shadows). */
class P5Accent(
    val name: String,
    val main: Int,
    val bright: Int,
    val deep: Int,
    /** True for light colours, which need dark text and icons on top. */
    val darkText: Boolean = false,
)

/** Where each group of accents starts in [P5_ACCENTS]: primary 0-2, secondary 3-5, tertiary 6-11. */
const val P5_PRIMARY_START = 0
const val P5_SECONDARY_START = 3
const val P5_TERTIARY_START = 6

/**
 * The twelve colours of the red-yellow-blue colour wheel: the three primaries, the three
 * secondaries (what you get mixing two primaries) and the six tertiaries (a primary mixed with the
 * secondary next to it). The first is the original red of the theme.
 */
val P5_ACCENTS: List<P5Accent> = listOf(
    // primary
    P5Accent("Red", 0xFFE60012.toInt(), 0xFFFF4553.toInt(), 0xFF7A000B.toInt()),
    P5Accent("Yellow", 0xFFFFD400.toInt(), 0xFFFFE766.toInt(), 0xFF7A6500.toInt(), darkText = true),
    P5Accent("Blue", 0xFF0057D9.toInt(), 0xFF4F8EFF.toInt(), 0xFF002D73.toInt()),
    // secondary
    P5Accent("Purple", 0xFF755FBE.toInt(), 0xFFA897EC.toInt(), 0xFF3A2E6B.toInt()),
    P5Accent("Green", 0xFF12A53A.toInt(), 0xFF4CD86F.toInt(), 0xFF06501B.toInt()),
    P5Accent("Orange", 0xFFFF7A00.toInt(), 0xFFFFA040.toInt(), 0xFF7A3A00.toInt(), darkText = true),
    // tertiary
    P5Accent("Red-orange", 0xFFF2380F.toInt(), 0xFFFF6F4D.toInt(), 0xFF7C1A05.toInt()),
    P5Accent("Yellow-orange", 0xFFFFAE00.toInt(), 0xFFFFCB4D.toInt(), 0xFF7A5200.toInt(), darkText = true),
    P5Accent("Yellow-green", 0xFF9ACD1F.toInt(), 0xFFC2EB5C.toInt(), 0xFF4A6410.toInt(), darkText = true),
    P5Accent("Blue-green", 0xFF00A19B.toInt(), 0xFF3DD6CF.toInt(), 0xFF004F4B.toInt()),
    P5Accent("Blue-purple", 0xFF4B3FD6.toInt(), 0xFF8077F5.toInt(), 0xFF241C7A.toInt()),
    P5Accent("Red-purple", 0xFFD6178F.toInt(), 0xFFFF59BF.toInt(), 0xFF6B0A48.toInt()),
)
