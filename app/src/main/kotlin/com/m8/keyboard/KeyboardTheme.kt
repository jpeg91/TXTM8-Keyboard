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
    /** Glitter-style themes that skip the pink pinstripe and hearts and use a plain (or gradient) background. */
    val plainBackground: Boolean = false,
    /** Border colour of the pixel keys on a glitter-style theme; 0 keeps the theme's own default. */
    val pixelBorder: Int = 0,
    /** Star colours for the twinkles on a glitter-style theme; null keeps the pink Glitter Graphics stars. */
    val starColours: IntArray? = null,
    /** When not 0, the background fades from [background] at the top to this colour at the bottom. */
    val backgroundBottom: Int = 0,
    /** Grey 90s-desktop keys with raised and sunken bevelled edges. */
    val isRetro: Boolean = false,
    /** Square-cornered keys (the terminal themes). */
    val squareKeys: Boolean = false,
    /** An animation that starts wherever the keyboard is tapped (bubbles, embers and so on); null for none. */
    val tapEffect: TapEffectKind? = null,
    /** Colours for [tapEffect]; what they mean depends on the effect. */
    val effectColours: IntArray? = null,
    /**
     * Optional per-key action colours (the console themes with multi-coloured buttons). Slot 0 is Enter,
     * 1 is Delete, 2 is the mode key (shift, ABC) and 3 is the settings key; a shorter list reuses its last
     * colour. Null means every action key uses [actionKey].
     */
    val actionKeyColours: IntArray? = null,
    /** Lettering colour for each slot of [actionKeyColours], so a yellow key can have dark lettering. */
    val actionTextColours: IntArray? = null,
    val typeface: Typeface = Typeface.DEFAULT,
) {
    /** The face colour of the action key in [slot] (see [actionKeyColours]). */
    fun actionFill(slot: Int): Int = actionKeyColours?.let { it[slot.coerceIn(0, it.size - 1)] } ?: actionKey

    /** The pressed colour of the action key in [slot]: the face colour lightened for multi-coloured keys. */
    fun actionFillPressed(slot: Int): Int {
        val c = actionKeyColours ?: return actionKeyPressed
        val base = c[slot.coerceIn(0, c.size - 1)]
        fun up(v: Int) = v + ((255 - v) * 0.3f).toInt()
        return (0xFF shl 24) or (up((base shr 16) and 0xFF) shl 16) or (up((base shr 8) and 0xFF) shl 8) or up(base and 0xFF)
    }

    /** The lettering colour of the action key in [slot]. */
    fun actionLabel(slot: Int): Int = actionTextColours?.let { it[slot.coerceIn(0, it.size - 1)] } ?: actionText

    /** True when the background flashes with the typing pulse; the star and tap-animation themes have their own effects. */
    val usesPulse: Boolean get() = !isGlitter && tapEffect == null

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

        /**
         * Night Sky: the Glitter Graphics pixel keys and twinkling stars in deep midnight blue. The
         * background, candidate strip and keys are one colour, with icy white borders and lettering.
         */
        val NIGHT_SKY = KeyboardTheme(
            background = NIGHT_SKY_MIDNIGHT,
            strip = NIGHT_SKY_MIDNIGHT,
            key = NIGHT_SKY_MIDNIGHT,
            keyPressed = 0xFF223C7A.toInt(),
            text = NIGHT_SKY_ICY,
            accent = 0xFF3E6FD1.toInt(),
            accentText = NIGHT_SKY_ICY,
            actionKey = NIGHT_SKY_MIDNIGHT,
            actionKeyPressed = 0xFF223C7A.toInt(),
            actionText = NIGHT_SKY_ICY,
            subText = NIGHT_SKY_ICY,
            shadow = 0xFF050A1F.toInt(),
            deep = 0xFF050A1F.toInt(),
            isGlitter = true,
            plainBackground = true,
            pixelBorder = NIGHT_SKY_ICY,
            starColours = NIGHT_SKY_STAR_COLOURS,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Winter: steel-blue pixel keys with snow-white borders, a touch of Christmas red, and snowy sparkles. */
        val WINTER = KeyboardTheme(
            background = 0xFF0E2238.toInt(),
            backgroundBottom = 0xFF1B3F66.toInt(),
            strip = 0xFF16365A.toInt(),
            key = 0xFF1F4B78.toInt(),
            keyPressed = 0xFF3A74A8.toInt(),
            text = 0xFFF2FAFF.toInt(),
            accent = 0xFFC62839.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFC62839.toInt(),
            actionKeyPressed = 0xFFE5495A.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFCFE8F8.toInt(),
            shadow = 0xFF081624.toInt(),
            deep = 0xFF081624.toInt(),
            isGlitter = true,
            plainBackground = true,
            pixelBorder = 0xFFF2FAFF.toInt(),
            starColours = WINTER_STAR_COLOURS,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Aurora: dark teal sky, teal and violet keys with northern-lights green borders, green and pink sparkles. */
        val AURORA = KeyboardTheme(
            background = 0xFF050B1C.toInt(),
            backgroundBottom = 0xFF0A3A38.toInt(),
            strip = 0xFF081A2B.toInt(),
            key = 0xFF123B45.toInt(),
            keyPressed = 0xFF1E6B62.toInt(),
            text = 0xFFE9FFF6.toInt(),
            accent = 0xFF8B5CF6.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF3B2A6B.toInt(),
            actionKeyPressed = 0xFF6A4BC0.toInt(),
            actionText = 0xFFE9FFF6.toInt(),
            subText = 0xFFB8F5DC.toInt(),
            shadow = 0xFF03070F.toInt(),
            deep = 0xFF03070F.toInt(),
            isGlitter = true,
            plainBackground = true,
            pixelBorder = 0xFF63F5B8.toInt(),
            starColours = AURORA_STAR_COLOURS,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Synthwave: purple dusk, indigo keys with neon pink borders and cyan lettering, pink and cyan sparkles. */
        val SYNTHWAVE = KeyboardTheme(
            background = 0xFF140A2E.toInt(),
            backgroundBottom = 0xFF3A0B53.toInt(),
            strip = 0xFF0F0626.toInt(),
            key = 0xFF1E1048.toInt(),
            keyPressed = 0xFF3B2A8C.toInt(),
            text = 0xFF2DE2E6.toInt(),
            accent = 0xFFFF2E97.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF3A0F5C.toInt(),
            actionKeyPressed = 0xFF7A1F8C.toInt(),
            actionText = 0xFFFF6EC7.toInt(),
            subText = 0xFF9BE8F0.toInt(),
            shadow = 0xFF0A0420.toInt(),
            deep = 0xFF0A0420.toInt(),
            isGlitter = true,
            plainBackground = true,
            pixelBorder = 0xFFFF2E97.toInt(),
            starColours = SYNTHWAVE_STAR_COLOURS,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Gold on Black: black keys with gold borders and lettering, and warm gold glints. */
        val GOLD_ON_BLACK = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFF0A0A0A.toInt(),
            keyPressed = 0xFF3A2F0A.toInt(),
            text = 0xFFF3D77A.toInt(),
            accent = 0xFFD4AF37.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFF1A1405.toInt(),
            actionKeyPressed = 0xFF4A3A0C.toInt(),
            actionText = 0xFFF3D77A.toInt(),
            subText = 0xFFC9A227.toInt(),
            shadow = 0xFF1A1405.toInt(),
            deep = 0xFF1A1405.toInt(),
            isGlitter = true,
            plainBackground = true,
            pixelBorder = 0xFFD4AF37.toInt(),
            starColours = GOLD_STAR_COLOURS,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Amber Backlight: an old monochrome phone with an orange backlight: dark brown, amber lettering, soft keys. */
        val AMBER_BACKLIGHT = KeyboardTheme(
            background = 0xFF1A0F05.toInt(),
            strip = 0xFF120A03.toInt(),
            key = 0xFF2B1A0A.toInt(),
            keyPressed = 0xFF5C3A10.toInt(),
            text = 0xFFFFB000.toInt(),
            accent = 0xFFFFB000.toInt(),
            accentText = 0xFF1A0F05.toInt(),
            actionKey = 0xFF3A2410.toInt(),
            actionKeyPressed = 0xFF6B4415.toInt(),
            actionText = 0xFFFFC433.toInt(),
            subText = 0xFFD98E00.toInt(),
            border = 0xFF6B4410.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Terminal, phosphor green: black screen, square keys with green outlines, monospace lettering. */
        val TERMINAL_GREEN = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFF000A00.toInt(),
            keyPressed = 0xFF0F4A1E.toInt(),
            text = 0xFF33FF66.toInt(),
            accent = 0xFF33FF66.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFF062A12.toInt(),
            actionKeyPressed = 0xFF14702F.toInt(),
            actionText = 0xFF33FF66.toInt(),
            subText = 0xFF1FB547.toInt(),
            border = 0xFF1FA040.toInt(),
            squareKeys = true,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Terminal, amber phosphor: the same square-keyed look in amber on black. */
        val TERMINAL_AMBER = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFF0A0600.toInt(),
            keyPressed = 0xFF4A3000.toInt(),
            text = 0xFFFFB000.toInt(),
            accent = 0xFFFFB000.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFF2A1A00.toInt(),
            actionKeyPressed = 0xFF6B4700.toInt(),
            actionText = 0xFFFFB000.toInt(),
            subText = 0xFFC48600.toInt(),
            border = 0xFFA86F00.toInt(),
            squareKeys = true,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /** Retro Desktop: silver bevelled keys on a teal desktop, a white text box and navy highlights. */
        val RETRO_DESKTOP = KeyboardTheme(
            background = 0xFF008080.toInt(),
            strip = 0xFFFFFFFF.toInt(),
            key = 0xFFC0C0C0.toInt(),
            keyPressed = 0xFFB0B0B0.toInt(),
            text = 0xFF000000.toInt(),
            accent = 0xFF000080.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFC0C0C0.toInt(),
            actionKeyPressed = 0xFFB0B0B0.toInt(),
            actionText = 0xFF000000.toInt(),
            subText = 0xFF000080.toInt(),
            isRetro = true,
            squareKeys = true,
            typeface = Typeface.create("sans-serif", Typeface.NORMAL),
        )

        /** Bubbles: a deep-sea gradient with aqua outlined keys; bubbles rise from every tap. */
        val BUBBLES = KeyboardTheme(
            background = 0xFF04334D.toInt(),
            backgroundBottom = 0xFF0B6A8A.toInt(),
            strip = 0xFF053C57.toInt(),
            key = 0xFF0F5C7A.toInt(),
            keyPressed = 0xFF2A93B0.toInt(),
            text = 0xFFEAFBFF.toInt(),
            accent = 0xFF38C8D8.toInt(),
            accentText = 0xFF03263A.toInt(),
            actionKey = 0xFF1B7F94.toInt(),
            actionKeyPressed = 0xFF3FB0C6.toInt(),
            actionText = 0xFFEAFBFF.toInt(),
            subText = 0xFFBFEFFA.toInt(),
            border = 0xFF7FD8E8.toInt(),
            tapEffect = TapEffectKind.BUBBLES,
            effectColours = intArrayOf(0xFFBFF3FF.toInt(), 0xFFE8FBFF.toInt(), 0xFF7FE0F0.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Embers: charcoal and deep red with glowing keys; sparks float up from every tap. */
        val EMBERS = KeyboardTheme(
            background = 0xFF0B0504.toInt(),
            backgroundBottom = 0xFF2A0A04.toInt(),
            strip = 0xFF120806.toInt(),
            key = 0xFF1E0F0B.toInt(),
            keyPressed = 0xFF7A2E10.toInt(),
            text = 0xFFFFD2A0.toInt(),
            accent = 0xFFFF6A1A.toInt(),
            accentText = 0xFF1A0804.toInt(),
            actionKey = 0xFF4A1408.toInt(),
            actionKeyPressed = 0xFF9A3A14.toInt(),
            actionText = 0xFFFFB066.toInt(),
            subText = 0xFFE0904F.toInt(),
            border = 0xFF8A3A12.toInt(),
            tapEffect = TapEffectKind.EMBERS,
            // Yellow-hot, then orange, then red, then dying dark red as each spark ages.
            effectColours = intArrayOf(0xFFFFE08A.toInt(), 0xFFFFA43A.toInt(), 0xFFFF5A1F.toInt(), 0xFFC21E0A.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Sakura: pale blossom pink with soft keys; petals burst out and flutter down from every tap. */
        val SAKURA = KeyboardTheme(
            background = 0xFFFBE4EC.toInt(),
            backgroundBottom = 0xFFF4B6C8.toInt(),
            strip = 0xFFFFE9EF.toInt(),
            key = 0xFFFFF4F7.toInt(),
            keyPressed = 0xFFF6C6D4.toInt(),
            text = 0xFF6B2B40.toInt(),
            accent = 0xFFE9789A.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFE9789A.toInt(),
            actionKeyPressed = 0xFFF29AB5.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF9A4D66.toInt(),
            border = 0xFFE58FA9.toInt(),
            tapEffect = TapEffectKind.PETALS,
            effectColours = intArrayOf(0xFFFF8FB0.toInt(), 0xFFFFA9C4.toInt(), 0xFFF76C9A.toInt(), 0xFFFFB7C9.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Neon Ripples: dark blue with cyan outlines; neon rings spread out from every tap. */
        val RIPPLES = KeyboardTheme(
            background = 0xFF06101F.toInt(),
            backgroundBottom = 0xFF0A1F3D.toInt(),
            strip = 0xFF081529.toInt(),
            key = 0xFF0C1B33.toInt(),
            keyPressed = 0xFF16407A.toInt(),
            text = 0xFFD6F6FF.toInt(),
            accent = 0xFF00E5FF.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFF12294A.toInt(),
            actionKeyPressed = 0xFF1F5C9C.toInt(),
            actionText = 0xFF7AF0FF.toInt(),
            subText = 0xFF8FD3E6.toInt(),
            border = 0xFF2AA7D6.toInt(),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFF00E5FF.toInt(), 0xFFFF3DCB.toInt(), 0xFF00E5FF.toInt(), 0xFF7C4DFF.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** 16-bit console pad (US): cool grey shell with purple action keys and a red highlight. */
        val SNES = KeyboardTheme(
            background = 0xFFB8B8B8.toInt(),
            strip = 0xFFEDEDED.toInt(),
            key = 0xFFB7A6D2.toInt(),
            keyPressed = 0xFF9E8CC0.toInt(),
            text = 0xFF2C2A3A.toInt(),
            accent = 0xFFB51F2A.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF654A91.toInt(),
            actionKeyPressed = 0xFF9380B2.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF3E3A55.toInt(),
            border = 0xFF7E7A94.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * 16-bit console pad (EU): the same cool grey shell, with the European controller's coloured buttons
         * (green, red, blue and yellow) in place of the purple action keys.
         */
        val SNES_EU = KeyboardTheme(
            background = 0xFFB8B8B8.toInt(),
            strip = 0xFFEDEDED.toInt(),
            key = 0xFFC9C9C9.toInt(),
            keyPressed = 0xFFAFAFAF.toInt(),
            text = 0xFF2A2A2E.toInt(),
            accent = 0xFFB51F2A.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF168A45.toInt(),
            actionKeyPressed = 0xFF5BAD7C.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF4A4A52.toInt(),
            border = 0xFF7A7A7A.toInt(),
            actionKeyColours = intArrayOf(0xFF168A45.toInt(), 0xFFC92B32.toInt(), 0xFF245AA6.toInt(), 0xFFE8C52A.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF2A2A2E.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * 64-bit console pad: grey controller body with the A (blue), Start (red), B (green) and C (yellow)
         * button colours as the action keys, and a yellow highlight.
         */
        val N64 = KeyboardTheme(
            background = 0xFFA5A5A5.toInt(),
            strip = 0xFFDCDCDC.toInt(),
            key = 0xFFC4C4C4.toInt(),
            keyPressed = 0xFF9A9A9A.toInt(),
            text = 0xFF22242A.toInt(),
            accent = 0xFFE6C51A.toInt(),
            accentText = 0xFF22242A.toInt(),
            actionKey = 0xFF1764B2.toInt(),
            actionKeyPressed = 0xFF5C92C9.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF3A3A3A.toInt(),
            border = 0xFF666666.toInt(),
            actionKeyColours = intArrayOf(0xFF1764B2.toInt(), 0xFFD71920.toInt(), 0xFF168A45.toInt(), 0xFFE6C51A.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF22242A.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** 32-bit console pad (Saturn): dark grey shell, mid-grey keys with silver lettering and blue action keys. */
        val SATURN = KeyboardTheme(
            background = 0xFF171717.toInt(),
            strip = 0xFF0F0F0F.toInt(),
            key = 0xFF242424.toInt(),
            keyPressed = 0xFF3C3C3C.toInt(),
            text = 0xFFE6E8EC.toInt(),
            accent = 0xFF315FA8.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF244A91.toInt(),
            actionKeyPressed = 0xFF3F6CC0.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFB6BAC4.toInt(),
            border = 0xFF6E727C.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** 32-bit grey console pad: mid-grey shell with the blue cross and red circle button colours. */
        val PLAYSTATION = KeyboardTheme(
            background = 0xFF8F9297.toInt(),
            strip = 0xFFDADBDE.toInt(),
            key = 0xFFB9BCC2.toInt(),
            keyPressed = 0xFF9A9DA4.toInt(),
            text = 0xFF24252B.toInt(),
            accent = 0xFFC83A43.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF315A9B.toInt(),
            actionKeyPressed = 0xFF6E8BB9.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF3E4048.toInt(),
            border = 0xFF6A6C74.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Early-2000s black console (PlayStation 2): black pad with the blue of the PS2 logo for outlines and the
         * highlight, a black-to-navy background, and the four symbol colours (blue, red, green, pink) as the
         * action keys. The symbol colours are estimates, not researched values.
         */
        val PS2 = KeyboardTheme(
            background = 0xFF0A0A0C.toInt(),
            backgroundBottom = 0xFF0D0A3A.toInt(),
            strip = 0xFF121216.toInt(),
            key = 0xFF1C1C21.toInt(),
            keyPressed = 0xFF34343C.toInt(),
            text = 0xFFF2F4F8.toInt(),
            accent = 0xFF118FDB.toInt(),
            accentText = 0xFF0A0A0C.toInt(),
            actionKey = 0xFF0072CE.toInt(),
            actionKeyPressed = 0xFF4C9CDC.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFA8AAB4.toInt(),
            border = 0xFF118FDB.toInt(),
            actionKeyColours = intArrayOf(0xFF0072CE.toInt(), 0xFFD22630.toInt(), 0xFF2EB67D.toInt(), 0xFFE8A0C4.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF0A0A0C.toInt(), 0xFF0A0A0C.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Late-90s white console (US): white keys on a light grey shell with orange action keys. */
        val DREAMCAST = KeyboardTheme(
            background = 0xFFF2F2F0.toInt(),
            strip = 0xFFE4E4E1.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE2E2DF.toInt(),
            text = 0xFF2D3036.toInt(),
            accent = 0xFFBB3322.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFBB3322.toInt(),
            actionKeyPressed = 0xFFCF7064.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF5E626B.toInt(),
            border = 0xFFA8A8A4.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Late-90s white console (Japan): the same white keys and light grey shell, with the orange swirl.
         * The orange is an estimate, not a researched value; the lettering is dark because white on this
         * orange is hard to read.
         */
        val DREAMCAST_JP = KeyboardTheme(
            background = 0xFFF2F2F0.toInt(),
            strip = 0xFFE4E4E1.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE2E2DF.toInt(),
            text = 0xFF2D3036.toInt(),
            accent = 0xFFF26A1B.toInt(),
            accentText = 0xFF1E2024.toInt(),
            actionKey = 0xFFF26A1B.toInt(),
            actionKeyPressed = 0xFFF5965F.toInt(),
            actionText = 0xFF1E2024.toInt(),
            subText = 0xFF5E626B.toInt(),
            border = 0xFFA8A8A4.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Late-90s white console (EU): the same white keys and light grey shell, with the European blue instead of orange. */
        val DREAMCAST_EU = KeyboardTheme(
            background = 0xFFF2F2F0.toInt(),
            strip = 0xFFE4E4E1.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE2E2DF.toInt(),
            text = 0xFF2D3036.toInt(),
            accent = 0xFF0066CC.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF0066CC.toInt(),
            actionKeyPressed = 0xFF4C93DB.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF5E626B.toInt(),
            border = 0xFFA8A8A4.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Early-2000s black console (Xbox): near-black keys with lime green outlines, action keys and highlight. */
        val XBOX = KeyboardTheme(
            background = 0xFF050505.toInt(),
            backgroundBottom = 0xFF0E2208.toInt(),
            strip = 0xFF141414.toInt(),
            key = 0xFF171917.toInt(),
            keyPressed = 0xFF2F4A12.toInt(),
            text = 0xFFEFF5E6.toInt(),
            accent = 0xFF94C83F.toInt(),
            accentText = 0xFF0A0A0A.toInt(),
            actionKey = 0xFF94C83F.toInt(),
            actionKeyPressed = 0xFFB4D878.toInt(),
            actionText = 0xFF0A0A0A.toInt(),
            subText = 0xFF9FB58A.toInt(),
            border = 0xFF4F7A25.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /** Early-2000s indigo console: deep indigo shell, purple keys, silver action keys and a green highlight. */
        val GAMECUBE = KeyboardTheme(
            background = 0xFF493B78.toInt(),
            strip = 0xFF2F2552.toInt(),
            key = 0xFF51458A.toInt(),
            keyPressed = 0xFF7466B8.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFF238A4B.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFD6D6D6.toInt(),
            actionKeyPressed = 0xFFEEEEEE.toInt(),
            actionText = 0xFF2B2150.toInt(),
            subText = 0xFFD2CAF5.toInt(),
            border = 0xFF9C8FE8.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
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
                KeyboardSettings.THEME_NIGHT_SKY -> NIGHT_SKY
                KeyboardSettings.THEME_WINTER -> WINTER
                KeyboardSettings.THEME_AURORA -> AURORA
                KeyboardSettings.THEME_SYNTHWAVE -> SYNTHWAVE
                KeyboardSettings.THEME_GOLD_ON_BLACK -> GOLD_ON_BLACK
                KeyboardSettings.THEME_AMBER_BACKLIGHT -> AMBER_BACKLIGHT
                KeyboardSettings.THEME_TERMINAL_GREEN -> TERMINAL_GREEN
                KeyboardSettings.THEME_TERMINAL_AMBER -> TERMINAL_AMBER
                KeyboardSettings.THEME_RETRO_DESKTOP -> RETRO_DESKTOP
                KeyboardSettings.THEME_BUBBLES -> BUBBLES
                KeyboardSettings.THEME_EMBERS -> EMBERS
                KeyboardSettings.THEME_SAKURA -> SAKURA
                KeyboardSettings.THEME_RIPPLES -> RIPPLES
                KeyboardSettings.THEME_SNES -> SNES
                KeyboardSettings.THEME_SNES_EU -> SNES_EU
                KeyboardSettings.THEME_N64 -> N64
                KeyboardSettings.THEME_SATURN -> SATURN
                KeyboardSettings.THEME_DREAMCAST_EU -> DREAMCAST_EU
                KeyboardSettings.THEME_DREAMCAST_JP -> DREAMCAST_JP
                KeyboardSettings.THEME_PS2 -> PS2
                KeyboardSettings.THEME_XBOX -> XBOX
                KeyboardSettings.THEME_PLAYSTATION -> PLAYSTATION
                KeyboardSettings.THEME_DREAMCAST -> DREAMCAST
                KeyboardSettings.THEME_GAMECUBE -> GAMECUBE
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                else -> if (night) DARK else LIGHT
            }
        }
    }
}

/** Night Sky's deep midnight blue (background and keys) and its icy white (borders, lettering, stars). */
const val NIGHT_SKY_MIDNIGHT = 0xFF0B1437.toInt()
const val NIGHT_SKY_ICY = 0xFFE8F6FF.toInt()

/** Star colours for the twinkling themes (a colour listed more than once is that much more common). */
internal val NIGHT_SKY_STAR_COLOURS = intArrayOf(
    0xFFE8F6FF.toInt(), 0xFFE8F6FF.toInt(), 0xFFE8F6FF.toInt(), 0xFFFFFFFF.toInt(),
    0xFFBFDFFF.toInt(), 0xFFBFDFFF.toInt(), 0xFF9EEBFF.toInt(),
)
internal val WINTER_STAR_COLOURS = intArrayOf(
    0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFE3F4FF.toInt(),
    0xFFBFE3FF.toInt(), 0xFFBFE3FF.toInt(), 0xFFFF8E99.toInt(),
)
internal val AURORA_STAR_COLOURS = intArrayOf(
    0xFF7CFFC4.toInt(), 0xFF7CFFC4.toInt(), 0xFFB6FFE0.toInt(), 0xFFFF8AD8.toInt(),
    0xFFC4A8FF.toInt(), 0xFFFFFFFF.toInt(),
)
internal val SYNTHWAVE_STAR_COLOURS = intArrayOf(
    0xFF2DE2E6.toInt(), 0xFF2DE2E6.toInt(), 0xFFFF2E97.toInt(), 0xFFFF7BC3.toInt(), 0xFFFFFFFF.toInt(),
)
internal val GOLD_STAR_COLOURS = intArrayOf(
    0xFFFFE9A3.toInt(), 0xFFFFE9A3.toInt(), 0xFFFFD86B.toInt(), 0xFFD4AF37.toInt(), 0xFFFFFFFF.toInt(),
)

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
