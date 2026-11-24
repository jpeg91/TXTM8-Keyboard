// app/src/main/kotlin/com/m8/keyboard/KeyboardTheme.kt
package com.m8.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build

/**
 * Everything the keyboard views and settings screens need to draw a theme: colours, key style, backdrop,
 * animations and type. "Action" keys (DEL, ENTER, shift) can be drawn differently from ordinary keys.
 *
 * A theme is made of five parts (colours, keys, background, animation, font) so the Custom mix theme can take each
 * from a different preset. Read docs/THEMES.md before adding a theme or changing a field.
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
    val border: Int = 0,
    val shadow: Int = 0,
    val deep: Int = 0,
    /** Border colour of the [KeyStyle.PIXEL] keys; 0 uses [shadow]. */
    val pixelBorder: Int = 0,
    /** Colours the [KeyStyle.PIXEL] keys' corner pixels take in turn (the Game Boy Advance); null for plain pixels. */
    val pixelColours: IntArray? = null,
    /** Twinkling stars drawn over the keys (their own slot: they can sit alongside a tap animation or the cursor). */
    val stars: Boolean = false,
    /** Star colours for [stars]; null keeps the pink Glitter Graphics stars. */
    val starColours: IntArray? = null,
    /** What is drawn behind the keys when [backgroundBottom] does not make it a gradient. */
    val backdrop: Backdrop = Backdrop.FLAT,
    /** What is drawn behind the candidate words. */
    val stripStyle: StripStyle = StripStyle.FLAT,
    /** The text view the candidate words use. */
    val candidateFont: CandidateFont = CandidateFont.NORMAL,
    /**
     * The highlight behind the selected candidate. Null follows the key style (see [candidateShape]); a theme
     * only sets it when it wants something different, like the Game Boy's rounded highlight on pixel keys.
     */
    val candidateStyle: CandidateStyle? = null,
    /** Capital-letter labels ("SPACE", "NEXT") for the themes that shout. */
    val shoutLabels: Boolean = false,
    /** Digit legends and QWERTY keys in the bright Game Boy Advance colours. */
    val rainbowLetters: Boolean = false,
    /** When not 0, the background fades from [background] at the top to this colour at the bottom. */
    val backgroundBottom: Int = 0,
    /** Square corners on the keys (and on the settings screens' buttons), whatever the key style. */
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
    /** How the keys are drawn. */
    val keyStyle: KeyStyle = KeyStyle.DEFAULT,
    /** For [KeyStyle.GLOSS]: the colour of the ring inside a pressed key (0 for none), and the strength (0..255) of the shine. */
    val keyRing: Int = 0,
    val keyGloss: Int = 90,
    /** Letter and digit keys take these colours in turn (the bright themes); null keeps every key one colour. */
    val keyCycle: IntArray? = null,
    /** A slow animation drawn behind the keys, and the colours it uses (what they mean depends on the animation). */
    val ambient: AmbientKind? = null,
    val ambientColours: IntArray? = null,
    /**
     * An animation drawn over the keys instead of behind them, for backgrounds the keys would otherwise hide, and
     * the colours it uses (what they mean depends on the animation; give them a low alpha so the lettering stays
     * readable). It is its own slot, so it can run alongside [ambient], [stars] and [tapEffect].
     */
    val overlay: AmbientKind? = null,
    val overlayColours: IntArray? = null,
    /** The colour the background flashes when a character is typed (the typing pulse); 0 uses [accent]. */
    val pulseColour: Int = 0,
    /** A blinking block cursor that sits at the bottom-left of the last key pressed (0 for none). */
    val cursorColour: Int = 0,
    /** Draws the keys without the sheen or gradient their style normally has (the [KeyStyle.ETCHED] keys), for solid colour. */
    val flatKeys: Boolean = false,
    /**
     * Lettering on ordinary keys, the small letters under the digits, and the candidate words on the strip, for the
     * themes where those differ from [text] and [subText] (which are what the settings screens print on the page
     * background). 0 keeps [text] or [subText].
     */
    val keyLabel: Int = 0,
    val keySubLabel: Int = 0,
    val stripText: Int = 0,
    val typeface: Typeface = Typeface.DEFAULT,
) {
    /** The lettering of an ordinary key. */
    val keyText: Int get() = if (keyLabel != 0) keyLabel else text

    /** The small letters under a digit key. */
    val keySub: Int get() = if (keySubLabel != 0) keySubLabel else subText

    /** The candidate words on the strip. */
    val candidateText: Int get() = if (stripText != 0) stripText else text

    /** The lettering of the selected candidate: on bevelled themes it is a raised button, so it keeps the key lettering. */
    val selectedCandidateText: Int get() = if (keyStyle == KeyStyle.BEVEL) keyText else accentText

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

    /** True when the "Effect amount" setting also switches the [ambient] animation (it is shared with the tap animation). */
    val ambientFollowsEffectAmount: Boolean get() = tapEffect != null

    /**
     * The highlight behind the selected candidate word: [candidateStyle] when the theme sets one, otherwise
     * whatever goes with its key style and corners.
     */
    val candidateShape: CandidateStyle
        get() = candidateStyle ?: when {
            keyStyle == KeyStyle.BEVEL -> CandidateStyle.BEVEL
            keyStyle == KeyStyle.PILL -> CandidateStyle.PILL
            keyStyle == KeyStyle.CHAMFER -> CandidateStyle.CHAMFER
            keyStyle == KeyStyle.RETRO || squareKeys || keyStyle == KeyStyle.LCD || keyStyle in SQUARE_STYLES ->
                CandidateStyle.SQUARE
            keyStyle == KeyStyle.MEMPHIS -> CandidateStyle.OUTLINED_SQUARE
            keyStyle == KeyStyle.PIXEL -> CandidateStyle.PIXEL
            keyStyle == KeyStyle.NOKIA -> CandidateStyle.NOKIA
            keyStyle == KeyStyle.SLANTED -> CandidateStyle.SLANTED
            else -> CandidateStyle.ROUNDED
        }

    companion object {
        /** The key styles whose keys have square corners, so their candidate highlight does too. */
        private val SQUARE_STYLES = setOf(
            KeyStyle.BRACKET, KeyStyle.RETICLE, KeyStyle.SCANLINE, KeyStyle.FRAME, KeyStyle.GLITCH, KeyStyle.ETCHED,
            KeyStyle.RIVETED, KeyStyle.PHOSPHOR, KeyStyle.DECO, KeyStyle.STENCIL, KeyStyle.CHECKER, KeyStyle.BLOCK,
            KeyStyle.SCRATCHED, KeyStyle.TICKET, KeyStyle.GOTHIC, KeyStyle.MENU, KeyStyle.PLAYER, KeyStyle.TEXTBOX,
        )

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
                keyStyle = KeyStyle.SLANTED,
                backdrop = Backdrop.STRIPES,
                shoutLabels = true,
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
            keyStyle = KeyStyle.NOKIA,
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
            keyStyle = KeyStyle.PIXEL,
            candidateStyle = CandidateStyle.ROUNDED,
            candidateFont = CandidateFont.PIXEL,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * The Game Boy Advance in Indigo, built from the console itself. The whole keyboard is the moulded indigo shell with a
         * fine plastic grain and a slow fall of shade towards the bottom. Every key is a domed plastic button standing in
         * a dark cut-out, with a sheen across its top, that sinks into the cut-out when pressed; Enter is a magenta
         * button and Delete a pale lavender one, like the A and B buttons. The candidate strip is the screen: a lit,
         * slightly greenish panel with a pixel grid and a glare on a near-black bezel, with a red power lamp, and the
         * words on it are in pixels. Every tap pops eight coloured sprites out in steps, which flicker out. Slanted
         * lettering, like the labels on the shell.
         */
        val GAME_BOY_ADVANCE = KeyboardTheme(
            background = 0xFF5A4B9E.toInt(),
            backgroundBottom = 0xFF43377F.toInt(),
            strip = 0xFF1E1E26.toInt(),
            key = 0xFF6F60BA.toInt(),
            keyPressed = 0xFF4F4390.toInt(),
            text = 0xFFF1EEFB.toInt(),
            accent = 0xFF2F6FE0.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF9484DE.toInt(),
            actionKeyPressed = 0xFF7868C4.toInt(),
            actionText = 0xFF241C4A.toInt(),
            subText = 0xFFC3BBEC.toInt(),
            border = 0xFFD3E0C2.toInt(),
            shadow = 0xFF2F2561.toInt(),
            actionKeyColours = intArrayOf(0xFFD94F8C.toInt(), 0xFF9484DE.toInt(), 0xFF3C3173.toInt(), 0xFF3C3173.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF241C4A.toInt(), 0xFFF1EEFB.toInt(), 0xFFF1EEFB.toInt()),
            keyStyle = KeyStyle.SHELL,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.HANDHELD_SCREEN,
            stripText = 0xFF1B2620.toInt(),
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0x00000000, 0x14FFFFFF, 0x16000000),
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFE8453C.toInt(), 0xFFF5D13A.toInt(), 0xFF4CC24A.toInt(), 0xFF3C7BE8.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD_ITALIC),
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
            keyStyle = KeyStyle.PIXEL,
            stars = true,
            backdrop = Backdrop.HEARTS,
            stripStyle = StripStyle.PATTERN,
            pixelBorder = 0xFFFFE2F2.toInt(),
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
            pixelBorder = NIGHT_SKY_ICY,
            starColours = NIGHT_SKY_STAR_COLOURS,
            keyStyle = KeyStyle.PIXEL,
            stars = true,
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
            pixelBorder = 0xFFF2FAFF.toInt(),
            starColours = WINTER_STAR_COLOURS,
            keyStyle = KeyStyle.PIXEL,
            stars = true,
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
            pixelBorder = 0xFF63F5B8.toInt(),
            starColours = AURORA_STAR_COLOURS,
            keyStyle = KeyStyle.PIXEL,
            stars = true,
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
            pixelBorder = 0xFFFF2E97.toInt(),
            starColours = SYNTHWAVE_STAR_COLOURS,
            keyStyle = KeyStyle.PIXEL,
            stars = true,
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
            pixelBorder = 0xFFD4AF37.toInt(),
            starColours = GOLD_STAR_COLOURS,
            keyStyle = KeyStyle.PIXEL,
            stars = true,
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
            keyStyle = KeyStyle.BACKLIT,
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
            keyStyle = KeyStyle.BRACKET,
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
            keyStyle = KeyStyle.BRACKET,
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
            squareKeys = true,
            keyStyle = KeyStyle.RETRO,
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
            keyStyle = KeyStyle.BUBBLE,
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
            keyStyle = KeyStyle.UNDERGLOW,
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
            keyStyle = KeyStyle.PETAL,
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
            keyStyle = KeyStyle.NEON,
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
            keyStyle = KeyStyle.DISH,
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
            keyStyle = KeyStyle.DISH,
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
            keyStyle = KeyStyle.DOME,
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
            keyStyle = KeyStyle.SLAB,
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
            keyStyle = KeyStyle.PLATE,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Early-2000s black console (PlayStation 2): black pad with the blue of the PS2 logo for outlines and the
         * highlight, a black-to-navy background, and the four symbol colours (blue, red, green, pink) as the
         * action keys. The logo blue and symbol colours are researched visual estimates, not Sony-published values.
         */
        val PS2 = KeyboardTheme(
            background = 0xFF000000.toInt(),
            backgroundBottom = 0xFF0D0A3A.toInt(),
            strip = 0xFF121216.toInt(),
            key = 0xFF1C1C21.toInt(),
            keyPressed = 0xFF34343C.toInt(),
            text = 0xFFF2F4F8.toInt(),
            accent = 0xFF003791.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF0070CC.toInt(),
            actionKeyPressed = 0xFF4C9ADB.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFA8AAB4.toInt(),
            border = 0xFF003791.toInt(),
            actionKeyColours = intArrayOf(0xFF0070CC.toInt(), 0xFFE60012.toInt(), 0xFF00A651.toInt(), 0xFFE6007E.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF0A0A0C.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.UNDERGLOW,
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
            keyStyle = KeyStyle.SLAB,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Late-90s white console (Japan): the same white keys and light grey shell, with the orange swirl.
         * The orange is a researched visual estimate from Japanese hardware imagery, not a published value; the
         * lettering is dark because white on this orange is hard to read.
         */
        val DREAMCAST_JP = KeyboardTheme(
            background = 0xFFF2F2F0.toInt(),
            strip = 0xFFE4E4E1.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFE2E2DF.toInt(),
            text = 0xFF2D3036.toInt(),
            accent = 0xFFF58220.toInt(),
            accentText = 0xFF1E2024.toInt(),
            actionKey = 0xFFF58220.toInt(),
            actionKeyPressed = 0xFFF8A762.toInt(),
            actionText = 0xFF1E2024.toInt(),
            subText = 0xFF5E626B.toInt(),
            border = 0xFFA8A8A4.toInt(),
            keyStyle = KeyStyle.SLAB,
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
            keyStyle = KeyStyle.SLAB,
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
            keyStyle = KeyStyle.UNDERGLOW,
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
            keyStyle = KeyStyle.DOME,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Windows XP: the Bliss hill and sky behind glassy silver keys with a dark blue outline and an orange
         * ring when pressed; green, red and blue action keys, a blue title-bar candidate strip. Clouds drift
         * across the sky and a loading bar of blue blocks marches away from every tap.
         */
        val XP = KeyboardTheme(
            background = 0xFF3E8FD4.toInt(),
            backgroundBottom = 0xFF8FC4EC.toInt(),
            strip = 0xFF0054E3.toInt(),
            key = 0xEBFCFCFB.toInt(),
            keyPressed = 0xFFEAE9E1.toInt(),
            text = 0xFF14181F.toInt(),
            accent = 0xFF3C963C.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF3C963C.toInt(),
            actionKeyPressed = 0xFF5CB45C.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF4A5A78.toInt(),
            border = 0xFF003C74.toInt(),
            actionKeyColours = intArrayOf(0xFF3C963C.toInt(), 0xFFD0432A.toInt(), 0xFF316AC5.toInt(), 0xFF316AC5.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFFF8B330.toInt(),
            keyGloss = 110,
            ambient = AmbientKind.CLOUDS,
            tapEffect = TapEffectKind.BLOCKS,
            effectColours = intArrayOf(0xFF316AC5.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Media Player 10, from the Windows XP era: deep navy glass with a bright blue glow, smoked navy keys with a
         * glowing blue edge, aqua when pressed, and glossy blue orbs (with the logo's orange for delete) on the action
         * keys. Thin blue and aqua lines drift behind the keys like the visualiser, and a glowing orb blooms wherever
         * you tap, like the glow round its round buttons.
         */
        val MEDIA_PLAYER = KeyboardTheme(
            background = 0xFF030918.toInt(),
            backgroundBottom = 0xFF0E3478.toInt(),
            strip = 0xCC040B20.toInt(),
            key = 0xCC0F2556.toInt(),
            keyPressed = 0xFF2C86F0.toInt(),
            text = 0xFFEAF4FF.toInt(),
            accent = 0xFF3B9AF0.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xCC2A7BE8.toInt(),
            actionKeyPressed = 0xFF63B4FF.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF86B0E8.toInt(),
            border = 0xFF3F7FD6.toInt(),
            actionKeyColours = intArrayOf(0xCC2A7BE8.toInt(), 0xCCD9741A.toInt(), 0xCC1F5FB8.toInt(), 0xCC1F5FB8.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFF8FE0FF.toInt(),
            keyGloss = 80,
            ambient = AmbientKind.WAVES,
            ambientColours = intArrayOf(0x804FB4FF.toInt(), 0x7040D8F0.toInt(), 0x609CC8FF.toInt()),
            tapEffect = TapEffectKind.ORB,
            effectColours = intArrayOf(0xFF4FC3FF.toInt(), 0xFF8FE0FF.toInt(), 0xFF2A7BE8.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * MS-DOS and the BIOS setup screen: white-on-blue, square keys with light grey outlines, a red
         * highlight and inverse-video (blue on grey) action keys. A faint band rolls down the screen, and a
         * block cursor blinks at the bottom-left of whichever key you pressed last.
         */
        val BIOS = KeyboardTheme(
            background = 0xFF0000AA.toInt(),
            strip = 0xFF0000AA.toInt(),
            key = 0xFF0000AA.toInt(),
            keyPressed = 0xFFAA0000.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFAA0000.toInt(),
            accentText = 0xFFFFFF55.toInt(),
            actionKey = 0xFFAAAAAA.toInt(),
            actionKeyPressed = 0xFFFFFF55.toInt(),
            actionText = 0xFF0000AA.toInt(),
            subText = 0xFF55FFFF.toInt(),
            border = 0xFFAAAAAA.toInt(),
            squareKeys = true,
            ambient = AmbientKind.SCAN,
            ambientColours = intArrayOf(0x1CFFFFFF),
            cursorColour = 0xFFFFFFFF.toInt(),
            keyStyle = KeyStyle.FRAME,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * GeoCities and the early web: a teal page with tiled twinkling stars and "under construction" tape,
         * bevelled grey-style buttons in loud web-safe colours, and a rainbow sparkle trail from every tap.
         */
        val GEOCITIES = KeyboardTheme(
            background = 0xFF008080.toInt(),
            strip = 0xFFFFFFFF.toInt(),
            key = 0xFFFFFF66.toInt(),
            keyPressed = 0xFFD8D84A.toInt(),
            text = 0xFF000000.toInt(),
            accent = 0xFF0000EE.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFFF0000.toInt(),
            actionKeyPressed = 0xFFFF7070.toInt(),
            actionText = 0xFFFFFF00.toInt(),
            subText = 0xFF000080.toInt(),
            actionKeyColours = intArrayOf(0xFF00AA00.toInt(), 0xFFFF0000.toInt(), 0xFF0000EE.toInt(), 0xFF551A8B.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFF00.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            squareKeys = true,
            keyCycle = intArrayOf(0xFFFFFF66.toInt(), 0xFF66FFFF.toInt(), 0xFFFF99FF.toInt(), 0xFF99FF66.toInt()),
            ambient = AmbientKind.HAZARD,
            ambientColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFF66.toInt(), 0xFFFFFF00.toInt(), 0xFF000000.toInt()),
            tapEffect = TapEffectKind.SPARKLE,
            effectColours = intArrayOf(
                0xFFFF3030.toInt(), 0xFFFFA000.toInt(), 0xFFFFE000.toInt(),
                0xFF30D030.toInt(), 0xFF2090FF.toInt(), 0xFFB040FF.toInt(),
            ),
            keyStyle = KeyStyle.RETRO,
            typeface = Typeface.create("casual", Typeface.BOLD),
        )

        /**
         * Palm Pilot and early PDAs: a grey-green reflective screen, flat notched soft-buttons in dark outline
         * and inverse-video selections. Every tap leaves a cluster of pixels that fades out in coarse steps,
         * like the ghosting of a slow LCD.
         */
        val PALM = KeyboardTheme(
            background = 0xFFA9AEB4.toInt(),
            strip = 0xFFD2DAB8.toInt(),
            key = 0xFFC3CCA7.toInt(),
            keyPressed = 0xFFA4AE88.toInt(),
            text = 0xFF1B2415.toInt(),
            accent = 0xFF2A3322.toInt(),
            accentText = 0xFFD2DAB8.toInt(),
            actionKey = 0xFF2A3322.toInt(),
            actionKeyPressed = 0xFF4A5640.toInt(),
            actionText = 0xFFD2DAB8.toInt(),
            subText = 0xFF4A5640.toInt(),
            border = 0xFF2A3322.toInt(),
            shadow = 0xFF8E9874.toInt(),
            keyStyle = KeyStyle.LCD,
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFF1B2415.toInt(), 0xFF2A3322.toInt()),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Walkman: charcoal metal keys with a silver edge, orange action keys and orange highlights. A row of
         * sound-wave bars lies flat behind the keys until you press one, then rings out from it like a short
         * sound; music notes float up from every tap.
         */
        val WALKMAN = KeyboardTheme(
            background = 0xFF232427.toInt(),
            backgroundBottom = 0xFF101113.toInt(),
            strip = 0xB3101113.toInt(),
            key = 0xC72D3035.toInt(),
            keyPressed = 0xFFC96A00.toInt(),
            text = 0xFFE8EAED.toInt(),
            accent = 0xFFFFA800.toInt(),
            accentText = 0xFF111111.toInt(),
            actionKey = 0xFFFF8A00.toInt(),
            actionKeyPressed = 0xFFFFB35C.toInt(),
            actionText = 0xFF111111.toInt(),
            subText = 0xFFFFB35C.toInt(),
            border = 0xFF7C818A.toInt(),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFFFFD27A.toInt(),
            keyGloss = 45,
            ambient = AmbientKind.SOUND,
            ambientColours = intArrayOf(0xCCFFA800.toInt()),
            tapEffect = TapEffectKind.NOTES,
            effectColours = intArrayOf(0xFFFF8A00.toInt(), 0xFFFFD27A.toInt(), 0xFFFFFFFF.toInt()),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Virtual Boy: red on black, square keys with a dark red wash and a bright red outline. A red band
         * sweeps across the screen like the console's mirror scanner, and taps leave a fading red glow.
         */
        val VIRTUAL_BOY = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xB3000000.toInt(),
            key = 0x992A0000.toInt(),
            keyPressed = 0xFF7A0000.toInt(),
            text = 0xFFFF0000.toInt(),
            accent = 0xFFFF2020.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xB33A0000.toInt(),
            actionKeyPressed = 0xFF9A0000.toInt(),
            actionText = 0xFFFF6A6A.toInt(),
            subText = 0xFFC02828.toInt(),
            border = 0xFF9A0000.toInt(),
            squareKeys = true,
            ambient = AmbientKind.RED_SCAN,
            ambientColours = intArrayOf(0x55FF1A1A),
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFFFF2A2A.toInt(), 0xFFFF6A6A.toInt()),
            keyStyle = KeyStyle.SCANLINE,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * PSP: a deep blue menu wave behind black glass keys with a silver-blue edge, and the four symbol
         * colours (green, red, blue, pink) on the action keys. Broad ribbons sway behind the keys and soft
         * rings spread from every tap.
         */
        val PSP = KeyboardTheme(
            background = 0xFF03123A.toInt(),
            backgroundBottom = 0xFF1B56C9.toInt(),
            strip = 0x8002091E.toInt(),
            key = 0xBF0A0F1E.toInt(),
            keyPressed = 0xFF3A6FD8.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFBFD9FF.toInt(),
            accentText = 0xFF03123A.toInt(),
            actionKey = 0xFF00A651.toInt(),
            actionKeyPressed = 0xFF6AD8B0.toInt(),
            actionText = 0xFF04140E.toInt(),
            subText = 0xFFA9C4EE.toInt(),
            border = 0xFF8FB4F0.toInt(),
            actionKeyColours = intArrayOf(0xFF00A651.toInt(), 0xFFE60012.toInt(), 0xFF0070CC.toInt(), 0xFFE6007E.toInt()),
            actionTextColours = intArrayOf(0xFF04140E.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFFFFFFFF.toInt(),
            keyGloss = 60,
            ambient = AmbientKind.RIBBONS,
            ambientColours = intArrayOf(0x2EFFFFFF, 0x2A9CC8FF, 0x24FFFFFF),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF9CC8FF.toInt(), 0xFFD6E8FF.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Matrix: black with bright green square keys behind which green glyphs rain down, and a column of
         * falling code that starts wherever you tap.
         */
        val MATRIX = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xB3000000.toInt(),
            key = 0x99021A08.toInt(),
            keyPressed = 0xFF0F4A1E.toInt(),
            text = 0xFF00FF41.toInt(),
            accent = 0xFF33FF66.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xB3042A0C.toInt(),
            actionKeyPressed = 0xFF14702F.toInt(),
            actionText = 0xFFB6FFC8.toInt(),
            subText = 0xFF1FB547.toInt(),
            border = 0xFF0F8F2F.toInt(),
            squareKeys = true,
            ambient = AmbientKind.RAIN,
            ambientColours = intArrayOf(0xFFD8FFE0.toInt(), 0xFF22DD55.toInt()),
            tapEffect = TapEffectKind.GLYPHS,
            effectColours = intArrayOf(0xFFE6FFEA.toInt(), 0xFF33FF66.toInt(), 0xFF1FB547.toInt()),
            keyStyle = KeyStyle.NEON,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Saved by the Bell: a bright teal screen with hard black outlines and solid shadows, every letter on a
         * different loud colour, and squiggles and triangles that wobble as if redrawn by hand. Taps throw
         * confetti.
         */
        val SATURDAY = KeyboardTheme(
            background = 0xFF27D3C3.toInt(),
            strip = 0xB3FFFFFF.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFFFF2A0.toInt(),
            text = 0xFF1A1033.toInt(),
            accent = 0xFFFF5FA8.toInt(),
            accentText = 0xFF1A1033.toInt(),
            actionKey = 0xFFFFE23B.toInt(),
            actionKeyPressed = 0xFFFFF08A.toInt(),
            actionText = 0xFF1A1033.toInt(),
            subText = 0xFF3B2A6B.toInt(),
            border = 0xFF1A1033.toInt(),
            shadow = 0xFF1A1033.toInt(),
            actionKeyColours = intArrayOf(0xFFFFE23B.toInt(), 0xFFFF5FA8.toInt(), 0xFF8E5BFF.toInt(), 0xFF3DA5FF.toInt()),
            actionTextColours = intArrayOf(0xFF1A1033.toInt(), 0xFF1A1033.toInt(), 0xFFFFFFFF.toInt(), 0xFF1A1033.toInt()),
            keyStyle = KeyStyle.MEMPHIS,
            keyCycle = intArrayOf(
                0xFFFF8FC4.toInt(), 0xFFFFE23B.toInt(), 0xFFFFFFFF.toInt(),
                0xFFB794FF.toInt(), 0xFFFFB06B.toInt(), 0xFF7CC4FF.toInt(),
            ),
            ambient = AmbientKind.MEMPHIS,
            ambientColours = intArrayOf(
                0xFFFF5FA8.toInt(), 0xFFFFE23B.toInt(), 0xFF8E5BFF.toInt(),
                0xFFFFFFFF.toInt(), 0xFF3DA5FF.toInt(), 0xFFFF9248.toInt(),
            ),
            tapEffect = TapEffectKind.CONFETTI,
            effectColours = intArrayOf(
                0xFFFF5FA8.toInt(), 0xFFFFE23B.toInt(), 0xFF8E5BFF.toInt(),
                0xFFFFFFFF.toInt(), 0xFF3DA5FF.toInt(), 0xFFFF9248.toInt(),
            ),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Nokia 3210 (1999) in its standard grey-black case: a dark metallic shell and dark grey keys with pale
         * lettering and a light edge. The green of its backlit LCD shows in the candidate strip (dark green-grey), in
         * the selected word (shown inverse, like the LCD's dark pixels) and in the typing pulse. Colours are
         * estimates, not researched values.
         */
        val NOKIA_3210 = KeyboardTheme(
            background = 0xFF383B3E.toInt(),
            backgroundBottom = 0xFF26282A.toInt(),
            strip = 0xFF2A3326.toInt(),
            key = 0xFF2C2F31.toInt(),
            keyPressed = 0xFF4A5B3A.toInt(),
            text = 0xFFE4E7E2.toInt(),
            accent = 0xFFA9C25A.toInt(),
            accentText = 0xFF1E2A12.toInt(),
            actionKey = 0xFF3E4346.toInt(),
            actionKeyPressed = 0xFF5D6468.toInt(),
            actionText = 0xFFE4E7E2.toInt(),
            subText = 0xFF9AA39A.toInt(),
            border = 0xFF5A5E62.toInt(),
            keyStyle = KeyStyle.MOULDED,
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Nokia 3510i (2002): a slate-blue shell with pale silver, domed keys and a colour-screen candidate strip.
         * The orange of its translucent sides (they flashed on a call) is the action keys, the highlight and the
         * typing pulse. Colours are estimates, not researched values.
         */
        val NOKIA_3510I = KeyboardTheme(
            background = 0xFF2A4A78.toInt(),
            strip = 0xFFD8E8F8.toInt(),
            key = 0xFFDDE3EA.toInt(),
            keyPressed = 0xFFB3BECB.toInt(),
            text = 0xFFF0F4FA.toInt(),
            accent = 0xFFFF8A1F.toInt(),
            accentText = 0xFF14233F.toInt(),
            actionKey = 0xFFFF8A1F.toInt(),
            actionKeyPressed = 0xFFFFB066.toInt(),
            actionText = 0xFF14233F.toInt(),
            subText = 0xFFB9C8E0.toInt(),
            shadow = 0xFF182C4A.toInt(),
            deep = 0xFF182C4A.toInt(),
            keyStyle = KeyStyle.NOKIA,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
            keyLabel = 0xFF14233F.toInt(),
            keySubLabel = 0xFF4A5C7A.toInt(),
            stripText = 0xFF14233F.toInt(),
        )

        /**
         * Nokia 7210 (2002), the fashion phone, in its turquoise Xpress-on cover: light grey keys with a thin
         * silver edge, a pale aqua colour-screen strip, deep teal action keys and a navy selection bar. Colours
         * are estimates, not researched values.
         */
        val NOKIA_7210 = KeyboardTheme(
            background = 0xFF12A5A8.toInt(),
            strip = 0xFFD6F4F1.toInt(),
            key = 0xFFE8ECEC.toInt(),
            keyPressed = 0xFFBFC9C9.toInt(),
            text = 0xFF0C2A2C.toInt(),
            accent = 0xFF1B3A8A.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF0E7F83.toInt(),
            actionKeyPressed = 0xFF3FA9AC.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF3A5E60.toInt(),
            border = 0xFF8FA9AA.toInt(),
            keyStyle = KeyStyle.MOULDED,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * Sony Ericsson T610 (2003) in Abyss Blue: silver pill-shaped keys with an aluminium edge, a pale
         * colour-screen strip, and the green of the lit joystick centre (and the Sony Ericsson logo) on the Enter
         * key, the highlight and the ring that appears inside a pressed key. Colours are estimates, not
         * researched values.
         */
        val SONY_ERICSSON_T610 = KeyboardTheme(
            background = 0xFF1B3868.toInt(),
            strip = 0xFFD5DEEA.toInt(),
            key = 0xFFC8CDD5.toInt(),
            keyPressed = 0xFFA9B0BB.toInt(),
            text = 0xFFE8EEF8.toInt(),
            accent = 0xFF86C232.toInt(),
            accentText = 0xFF10200A.toInt(),
            actionKey = 0xFFAAB2BE.toInt(),
            actionKeyPressed = 0xFFC4CAD3.toInt(),
            actionText = 0xFF161C28.toInt(),
            subText = 0xFFA9BBD8.toInt(),
            border = 0xFF7E8795.toInt(),
            actionKeyColours = intArrayOf(0xFF86C232.toInt(), 0xFFAAB2BE.toInt(), 0xFFAAB2BE.toInt(), 0xFFAAB2BE.toInt()),
            actionTextColours = intArrayOf(0xFF10200A.toInt(), 0xFF161C28.toInt(), 0xFF161C28.toInt(), 0xFF161C28.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFF86C232.toInt(),
            keyGloss = 70,
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
            keyLabel = 0xFF161C28.toInt(),
            keySubLabel = 0xFF4A5362.toInt(),
            stripText = 0xFF161C28.toInt(),
        )

        /**
         * N-Gage (2003): the grey "taco" with an orange backlight and a green accent. Dark grey keys, orange
         * lettering and an amber edge; Enter is green, Delete is orange and the other action keys are grey.
         * Colours are estimates, not researched values.
         */
        val NGAGE = KeyboardTheme(
            background = 0xFF3B3E44.toInt(),
            backgroundBottom = 0xFF26282C.toInt(),
            strip = 0xFF14161A.toInt(),
            key = 0xFF23262B.toInt(),
            keyPressed = 0xFF5A3A12.toInt(),
            text = 0xFFFF9B2E.toInt(),
            accent = 0xFF9ACD1F.toInt(),
            accentText = 0xFF11180A.toInt(),
            actionKey = 0xFF9ACD1F.toInt(),
            actionKeyPressed = 0xFFC2E76A.toInt(),
            actionText = 0xFF11180A.toInt(),
            subText = 0xFFC77A22.toInt(),
            border = 0xFF9A5418.toInt(),
            actionKeyColours = intArrayOf(0xFF9ACD1F.toInt(), 0xFFF07A1A.toInt(), 0xFF5A5F68.toInt(), 0xFF5A5F68.toInt()),
            actionTextColours = intArrayOf(0xFF11180A.toInt(), 0xFF1A0E04.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.BACKLIT,
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Motorola RAZR V3 (2004): the electroluminescent keypad lit in the dark. Every row of keys is one glowing cyan
         * line that snakes through the keys (down the left of the first, over its top, down between the first and second, under
         * the second, and so on to the side of the last), on black glass; the digits and letters glow in the same cyan, and a soft band of backlight rolls up the keypad behind the glass, and a pressed key's line glows brighter. Enter is the
         * green of the call key, Delete the red of the end key. The candidate strip is the black front screen with cyan
         * lettering, and the typing pulse glows cyan.
         * Palette and shapes from photographs of the keypad.
         */
        val RAZR_V3 = KeyboardTheme(
            background = 0xFF030507.toInt(),
            backgroundBottom = 0xFF0A1118.toInt(),
            strip = 0xFF05080B.toInt(),
            key = 0x70081017.toInt(),
            keyPressed = 0xFF0C3A4D.toInt(),
            text = 0xFF46D6FF.toInt(),
            accent = 0xFF1EBEEB.toInt(),
            accentText = 0xFF03131B.toInt(),
            actionKey = 0xFF1EBEEB.toInt(),
            actionKeyPressed = 0xFF1EBEEB.toInt(),
            actionText = 0xFF8FE6FF.toInt(),
            subText = 0xFF1B93B8.toInt(),
            border = 0xFF1EBEEB.toInt(),
            stripText = 0xFF46D6FF.toInt(),
            actionKeyColours = intArrayOf(0xFF2EDC7A.toInt(), 0xFFFF3B3B.toInt(), 0xFF1EBEEB.toInt(), 0xFF1EBEEB.toInt()),
            actionTextColours = intArrayOf(0xFF8FFFC0.toInt(), 0xFFFF8F8F.toInt(), 0xFF8FE6FF.toInt(), 0xFF8FE6FF.toInt()),
            keyStyle = KeyStyle.GLOWLINE,
            candidateStyle = CandidateStyle.OUTLINED_SQUARE,
            ambient = AmbientKind.BACKLIGHT,
            ambientColours = intArrayOf(0x5022B8E8.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Kawaii flip phone: a pink-to-lilac screen, glossy sticker keys with white outlines in pastel pink, lilac,
         * mint, sky, lemon and peach, candy-coloured action keys, and little hearts that float up from every tap.
         * Colours are estimates, not researched values.
         */
        val KAWAII = KeyboardTheme(
            background = 0xFFFFD6E8.toInt(),
            backgroundBottom = 0xFFD9C8FF.toInt(),
            strip = 0xFFFFF4FA.toInt(),
            key = 0xFFFFF0F7.toInt(),
            keyPressed = 0xFFFFB3D4.toInt(),
            text = 0xFF7A2E64.toInt(),
            accent = 0xFFFF6FAE.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFFF6FAE.toInt(),
            actionKeyPressed = 0xFFFF9CC8.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFFB24F8F.toInt(),
            border = 0xFFFFFFFF.toInt(),
            actionKeyColours = intArrayOf(0xFFFF6FAE.toInt(), 0xFFB48CFF.toInt(), 0xFF5FD3B3.toInt(), 0xFF6FB8FF.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF3A1A40.toInt(), 0xFF3A1A40.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFFFFFFFF.toInt(),
            keyGloss = 150,
            keyCycle = intArrayOf(
                0xFFFFC2DD.toInt(), 0xFFE0CFFF.toInt(), 0xFFC4F2E4.toInt(),
                0xFFC2E4FF.toInt(), 0xFFFFF0B8.toInt(), 0xFFFFD6C2.toInt(),
            ),
            tapEffect = TapEffectKind.HEARTS,
            effectColours = intArrayOf(0xFFFF6FAE.toInt(), 0xFFFF9CC8.toInt(), 0xFFB48CFF.toInt(), 0xFF6FB8FF.toInt()),
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD),
        )

        /**
         * Evangelion Unit-00, the prototype: orange armour plates with white edges on a dark screen, a white
         * armour candidate strip, white Enter, a red Delete and a red "eye" highlight. The keys are angular plates
         * with two cut corners. A faint orange band rolls down the screen, and large hexagonal shields flash out
         * from every tap. Colours are estimates, not researched values.
         */
        val EVANGELION_00 = KeyboardTheme(
            background = 0xFF0B0A0E.toInt(),
            backgroundBottom = 0xFF2A1206.toInt(),
            strip = 0xFFEDEAE4.toInt(),
            key = 0xFFE9751B.toInt(),
            keyPressed = 0xFFFFA24D.toInt(),
            text = 0xFFF2EEE8.toInt(),
            accent = 0xFFD7191C.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFEDEAE4.toInt(),
            actionKeyPressed = 0xFFFFFFFF.toInt(),
            actionText = 0xFF1A0E06.toInt(),
            subText = 0xFFFFB067.toInt(),
            border = 0xFFF4F1EA.toInt(),
            actionKeyColours = intArrayOf(0xFFEDEAE4.toInt(), 0xFFD7191C.toInt(), 0xFF8C8F96.toInt(), 0xFF8C8F96.toInt()),
            actionTextColours = intArrayOf(0xFF1A0E06.toInt(), 0xFFFFFFFF.toInt(), 0xFF111111.toInt(), 0xFF111111.toInt()),
            keyStyle = KeyStyle.CHAMFER,
            ambient = AmbientKind.SCAN,
            ambientColours = intArrayOf(0x22FF8A1F),
            tapEffect = TapEffectKind.HEXAGONS,
            effectColours = intArrayOf(0xFFFF8A1F.toInt(), 0xFFFFC14D.toInt(), 0xFFFFFFFF.toInt()),
            typeface = evaTypeface(),
            keyLabel = 0xFF1A0E06.toInt(),
            keySubLabel = 0xFF3A1A06.toInt(),
            stripText = 0xFF1A0E06.toInt(),
        )

        /**
         * Evangelion Unit-01: purple armour plates edged in neon green on a deep purple screen, neon green Enter
         * and highlight, an orange Delete, and green hexagonal shields from every tap. Colours are estimates, not
         * researched values.
         */
        val EVANGELION_01 = KeyboardTheme(
            background = 0xFF0B0714.toInt(),
            backgroundBottom = 0xFF1C0F33.toInt(),
            strip = 0xFF1A1030.toInt(),
            key = 0xFF6B3FB0.toInt(),
            keyPressed = 0xFF9A6BE6.toInt(),
            text = 0xFFF2EEFF.toInt(),
            accent = 0xFF76FF3A.toInt(),
            accentText = 0xFF0A1A04.toInt(),
            actionKey = 0xFF76FF3A.toInt(),
            actionKeyPressed = 0xFFA8FF7A.toInt(),
            actionText = 0xFF0A1A04.toInt(),
            subText = 0xFFD2BFFF.toInt(),
            border = 0xFF76FF3A.toInt(),
            actionKeyColours = intArrayOf(0xFF76FF3A.toInt(), 0xFFFF8A1A.toInt(), 0xFF2B1B4D.toInt(), 0xFF2B1B4D.toInt()),
            actionTextColours = intArrayOf(0xFF0A1A04.toInt(), 0xFF1A0E04.toInt(), 0xFF76FF3A.toInt(), 0xFF76FF3A.toInt()),
            keyStyle = KeyStyle.CHAMFER,
            ambient = AmbientKind.SCAN,
            ambientColours = intArrayOf(0x2276FF3A),
            tapEffect = TapEffectKind.HEXAGONS,
            effectColours = intArrayOf(0xFF76FF3A.toInt(), 0xFFB894FF.toInt(), 0xFFFFFFFF.toInt()),
            typeface = evaTypeface(),
        )

        /**
         * Evangelion Unit-02: red armour plates edged in amber on a near-black red screen, an orange Enter, a silver
         * Delete and a yellow "eye" highlight, with red and yellow hexagonal shields from every tap. Colours are
         * estimates, not researched values.
         */
        val EVANGELION_02 = KeyboardTheme(
            background = 0xFF0E0506.toInt(),
            backgroundBottom = 0xFF2E0A0C.toInt(),
            strip = 0xFF1F0A0C.toInt(),
            key = 0xFFC8171C.toInt(),
            keyPressed = 0xFFFF5A52.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFD400.toInt(),
            accentText = 0xFF1A1400.toInt(),
            actionKey = 0xFFFF8A1A.toInt(),
            actionKeyPressed = 0xFFFFB066.toInt(),
            actionText = 0xFF1A0E04.toInt(),
            subText = 0xFFFFD2C8.toInt(),
            border = 0xFFFFC21A.toInt(),
            actionKeyColours = intArrayOf(0xFFFF8A1A.toInt(), 0xFFE6E7EA.toInt(), 0xFF3A1214.toInt(), 0xFF3A1214.toInt()),
            actionTextColours = intArrayOf(0xFF1A0E04.toInt(), 0xFF1A1014.toInt(), 0xFFFFD400.toInt(), 0xFFFFD400.toInt()),
            keyStyle = KeyStyle.CHAMFER,
            ambient = AmbientKind.SCAN,
            ambientColours = intArrayOf(0x22FF3A2A),
            tapEffect = TapEffectKind.HEXAGONS,
            effectColours = intArrayOf(0xFFFF5A3C.toInt(), 0xFFFFD400.toInt(), 0xFFFFFFFF.toInt()),
            typeface = evaTypeface(),
        )

        /**
         * MonsterGear: a lavender-grey handheld communicator with pale grey keys, a lime-green screen strip for the
         * candidates, yellow-gold tabs for Enter and the highlight, a red Delete, green mode and settings keys, and
         * signal rings spreading from every tap. Inspired by the look of a late-90s handheld gadget; it uses no logos
         * or artwork. Colours are researched visual estimates from the device illustration in an official guide.
         */
        val MONSTERGEAR = KeyboardTheme(
            background = 0xFFA9A6B6.toInt(),
            backgroundBottom = 0xFF777486.toInt(),
            strip = 0xFFC8E98A.toInt(),
            key = 0xFFE0DFE5.toInt(),
            keyPressed = 0xFFB9B8BE.toInt(),
            text = 0xFF2B2A30.toInt(),
            accent = 0xFFE5C84D.toInt(),
            accentText = 0xFF2B2A30.toInt(),
            actionKey = 0xFFE5C84D.toInt(),
            actionKeyPressed = 0xFFECD882.toInt(),
            actionText = 0xFF2B2A30.toInt(),
            subText = 0xFF5A5864.toInt(),
            shadow = 0xFF8F8C9C.toInt(),
            border = 0xFF38363D.toInt(),
            actionKeyColours = intArrayOf(0xFFE5C84D.toInt(), 0xFFE33E32.toInt(), 0xFF55B84A.toInt(), 0xFF55B84A.toInt()),
            actionTextColours = intArrayOf(0xFF2B2A30.toInt(), 0xFFFFFFFF.toInt(), 0xFF2B2A30.toInt(), 0xFF2B2A30.toInt()),
            keyStyle = KeyStyle.LCD,
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFFE5C84D.toInt(), 0xFF55B84A.toInt(), 0xFF38363D.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )


        /**
         * MonsterNav: a beige handheld navigator with a slate-grey candidate strip. Cyan-blue keys with pale-blue
         * lettering and a dark brown outline, an orange Enter and highlight, a peach Delete, cream mode and settings
         * keys, a faint radar band behind the keys and blips of pixels wherever you tap. It uses no logos or artwork.
         * Colours are researched visual estimates from the official artwork.
         */
        val MONSTERNAV = KeyboardTheme(
            background = 0xFFE9D0A5.toInt(),
            backgroundBottom = 0xFFB99A68.toInt(),
            strip = 0xFF66686D.toInt(),
            key = 0xFF0799CF.toInt(),
            keyPressed = 0xFF067AA6.toInt(),
            text = 0xFF2A2118.toInt(),
            accent = 0xFFF18B3C.toInt(),
            accentText = 0xFF1A1000.toInt(),
            actionKey = 0xFFF18B3C.toInt(),
            actionKeyPressed = 0xFFF5AE76.toInt(),
            actionText = 0xFF1A1000.toInt(),
            subText = 0xFF51463B.toInt(),
            border = 0xFF51463B.toInt(),
            actionKeyColours = intArrayOf(0xFFF18B3C.toInt(), 0xFFF0B58D.toInt(), 0xFFF5E3C4.toInt(), 0xFFF5E3C4.toInt()),
            actionTextColours = intArrayOf(0xFF1A1000.toInt(), 0xFF2A2118.toInt(), 0xFF2A2118.toInt(), 0xFF2A2118.toInt()),
            ambient = AmbientKind.SCAN,
            ambientColours = intArrayOf(0x1E0799CF),
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFF0799CF.toInt(), 0xFF51463B.toInt()),
            keyStyle = KeyStyle.PLATE,
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
            keyLabel = 0xFFD9F4FF.toInt(),
            keySubLabel = 0xFFC4ECFA.toInt(),
            stripText = 0xFFD9F4FF.toInt(),
        )

        /** The lettering of the Evangelion themes: heavy and condensed, like the signage on the NERV screens. */
        private fun evaTypeface(): Typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)

        /**
         * Ghost in the Shell (1995): a dark navy screen with thin cyan outlines, translucent square panels, pale
         * green lettering, a fine technical grid with a scanning line and small blinking markers behind the keys, and
         * a targeting reticle that closes in on every tap. Colours are estimates, not researched values.
         */
        val GHOST_IN_THE_SHELL = KeyboardTheme(
            background = 0xFF040D1C.toInt(),
            backgroundBottom = 0xFF08213A.toInt(),
            strip = 0xFF07192E.toInt(),
            key = 0x3318405E.toInt(),
            keyPressed = 0x6638D8E8.toInt(),
            text = 0xFFBFF5E4.toInt(),
            accent = 0xFF38E8F0.toInt(),
            accentText = 0xFF031018.toInt(),
            actionKey = 0x4438D8E8.toInt(),
            actionKeyPressed = 0x8838D8E8.toInt(),
            actionText = 0xFF7FF3FF.toInt(),
            subText = 0xFF38D8E8.toInt(),
            border = 0xFF2FC7D6.toInt(),
            squareKeys = true,
            ambient = AmbientKind.GRID,
            ambientColours = intArrayOf(0x2038E8F0, 0x7AA8FFD8.toInt()),
            tapEffect = TapEffectKind.TARGET,
            effectColours = intArrayOf(0xFF7FF3FF.toInt(), 0xFFA8FFD8.toInt()),
            keyStyle = KeyStyle.RETICLE,
            typeface = Typeface.create("monospace", Typeface.NORMAL),
        )

        /**
         * Star Trek LCARS: a black screen and flat coloured panels in orange, peach, lilac, periwinkle, salmon and
         * gold, each key a tab with a fully rounded left end. The candidate strip is a peach bar with a periwinkle
         * selection, and the lettering is black on the panels. Colours are estimates, not researched values.
         */
        val LCARS = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFFFFCC99.toInt(),
            key = 0xFFFF9900.toInt(),
            keyPressed = 0xFFFFE0B0.toInt(),
            text = 0xFFFFCC99.toInt(),
            accent = 0xFF9999CC.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFFCC6666.toInt(),
            actionKeyPressed = 0xFFFFB0B0.toInt(),
            actionText = 0xFF000000.toInt(),
            subText = 0xFFCC99CC.toInt(),
            actionKeyColours = intArrayOf(0xFFCC6666.toInt(), 0xFF9999CC.toInt(), 0xFFFF9900.toInt(), 0xFFCC99CC.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.PILL,
            keyCycle = intArrayOf(
                0xFFFF9900.toInt(), 0xFFFFCC99.toInt(), 0xFFCC99CC.toInt(),
                0xFF9999CC.toInt(), 0xFFCC6666.toInt(), 0xFFFFCC66.toInt(),
            ),
            keyLabel = 0xFF000000.toInt(),
            keySubLabel = 0xFF2A1400.toInt(),
            stripText = 0xFF000000.toInt(),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Hackers (1995): acid green, electric blue and magenta on black. Dark translucent square keys with acid
         * green outlines and lettering, streams of scrolling text in the three colours behind the keys, with the odd
         * glitch bar, and a column of glyphs falling from every tap. Colours are estimates, not researched values.
         */
        val HACKERS = KeyboardTheme(
            background = 0xFF020204.toInt(),
            strip = 0xFF000008.toInt(),
            key = 0xB0050510.toInt(),
            keyPressed = 0xFF0F3A1A.toInt(),
            text = 0xFF39FF14.toInt(),
            accent = 0xFFFF2BD6.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xB0050510.toInt(),
            actionKeyPressed = 0xFF1E6BFF.toInt(),
            actionText = 0xFF39FF14.toInt(),
            subText = 0xFF1E90FF.toInt(),
            border = 0xFF39FF14.toInt(),
            actionKeyColours = intArrayOf(0xFFFF2BD6.toInt(), 0xFF1E6BFF.toInt(), 0xFF39FF14.toInt(), 0xFF7A1CFF.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            squareKeys = true,
            ambient = AmbientKind.SCROLL,
            ambientColours = intArrayOf(0x5539FF14, 0x551E90FF, 0x55FF2BD6),
            tapEffect = TapEffectKind.GLYPHS,
            effectColours = intArrayOf(0xFF39FF14.toInt(), 0xFF1E90FF.toInt(), 0xFFFF2BD6.toInt()),
            keyStyle = KeyStyle.GLITCH,
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Serial Experiments Lain: CRT green on black with washed-out grey outlines, a red warning highlight and
         * Delete, and a screen that is alive behind the keys: scanlines, a slow rolling band, flicker, fragments of
         * grey blocks that jump about, and a blinking red indicator. A small grey window opens at every tap, fills
         * with lines of text and tears away, like the Wired. Colours are estimates, not researched values.
         */
        val LAIN = KeyboardTheme(
            background = 0xFF050805.toInt(),
            strip = 0xFF070C07.toInt(),
            key = 0xB00A110A.toInt(),
            keyPressed = 0xFF1F3A1F.toInt(),
            text = 0xFF9BE59B.toInt(),
            accent = 0xFFD8272B.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xB0101810.toInt(),
            actionKeyPressed = 0xFF2F5A34.toInt(),
            actionText = 0xFFB8BDB8.toInt(),
            subText = 0xFF8A968A.toInt(),
            border = 0xFF59695A.toInt(),
            actionKeyColours = intArrayOf(0xFF2F5A34.toInt(), 0xFFB5202A.toInt(), 0xFF3A413A.toInt(), 0xFF3A413A.toInt()),
            actionTextColours = intArrayOf(0xFFCFF5CF.toInt(), 0xFFFFFFFF.toInt(), 0xFFB8BDB8.toInt(), 0xFFB8BDB8.toInt()),
            squareKeys = true,
            ambient = AmbientKind.CRT,
            ambientColours = intArrayOf(0x1A36FF5A, 0xFFB8BDB8.toInt(), 0xFFD8272B.toInt()),
            tapEffect = TapEffectKind.WINDOW,
            effectColours = intArrayOf(0xFF3C8F4A.toInt(), 0xFFB8BDB8.toInt(), 0xFFD8272B.toInt()),
            keyStyle = KeyStyle.SCANLINE,
            typeface = Typeface.create("monospace", Typeface.NORMAL),
        )

        /**
         * Windows 95: the silver dialog-box grey with the real raised bevel (white and black outside, light and dark
         * grey inside) and the real "pushed" button, a navy title bar for the candidate strip with the words in white
         * and the selected word as a raised button on it, and navy banners on the settings screens. Colours are the
         * standard system colours of the time.
         */
        val WINDOWS_95 = KeyboardTheme(
            background = 0xFFC0C0C0.toInt(),
            strip = 0xFF000080.toInt(),
            key = 0xFFC0C0C0.toInt(),
            keyPressed = 0xFFC0C0C0.toInt(),
            text = 0xFF000000.toInt(),
            accent = 0xFF000080.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFC0C0C0.toInt(),
            actionKeyPressed = 0xFFC0C0C0.toInt(),
            actionText = 0xFF000000.toInt(),
            subText = 0xFF000080.toInt(),
            keyStyle = KeyStyle.BEVEL,
            stripText = 0xFFFFFFFF.toInt(),
            typeface = Typeface.create("sans-serif", Typeface.NORMAL),
        )

        /**
         * Winamp, in its classic skin: the dark slate-violet panels of the player with its silver buttons, each a square
         * metal button with a black outline and a light and dark bevel like the transport buttons, which reverses when
         * pressed. Action keys are toggle buttons with a small lamp in the corner that lights when pressed, and Enter is
         * the skin's gold. The candidate strip is the playlist: black, with lime-green lettering and the selected word as
         * the playlist's blue highlight bar. A spectrum analyser leaps behind the keys with every key you press, and the
         * pulse as you type is lime. Lettering is a small monospace in capitals, like the skin's.
         * Palette from the skin itself; the look is drawn from the player.
         */
        val WINAMP = KeyboardTheme(
            background = 0xFF30324F.toInt(),
            backgroundBottom = 0xFF1D1F37.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xF0959AB4.toInt(),
            keyPressed = 0xFF7C819C.toInt(),
            text = 0xFF00E800.toInt(),
            accent = 0xFF0000C6.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xF0959AB4.toInt(),
            actionKeyPressed = 0xFF7C819C.toInt(),
            actionText = 0xFF12142B.toInt(),
            subText = 0xFF3A3D5E.toInt(),
            keyLabel = 0xFF12142B.toInt(),
            keySubLabel = 0xFF3A3D5E.toInt(),
            stripText = 0xFF00E800.toInt(),
            pulseColour = 0xFF22DD22.toInt(),
            border = 0xFFE02020.toInt(),
            actionKeyColours = intArrayOf(0xFFC9A44C.toInt(), 0xF0959AB4.toInt(), 0xF0959AB4.toInt(), 0xF0959AB4.toInt()),
            actionTextColours = intArrayOf(0xFF2B1D05.toInt(), 0xFF12142B.toInt(), 0xFF12142B.toInt(), 0xFF12142B.toInt()),
            keyStyle = KeyStyle.PLAYER,
            candidateStyle = CandidateStyle.SQUARE,
            squareKeys = true,
            shoutLabels = true,
            ambient = AmbientKind.EQUALISER,
            ambientColours = intArrayOf(0xFF22DD22.toInt(), 0xFFE8E020.toInt(), 0xFFFF8A1F.toInt(), 0xFFE02020.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Netscape Navigator: grey toolbar buttons with the same bevel, a white location-bar strip, blue link
         * lettering on the action keys (purple, like a visited link, on Delete), a grey textured page behind the
         * keys, and page lettering in a serif face. Colours are estimates, not researched values.
         */
        val NETSCAPE = KeyboardTheme(
            background = 0xFFC0C0C0.toInt(),
            strip = 0xFFFFFFFF.toInt(),
            key = 0xFFC0C0C0.toInt(),
            keyPressed = 0xFFC0C0C0.toInt(),
            text = 0xFF000000.toInt(),
            accent = 0xFF0000EE.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFFC0C0C0.toInt(),
            actionKeyPressed = 0xFFC0C0C0.toInt(),
            actionText = 0xFF0000EE.toInt(),
            subText = 0xFF404040.toInt(),
            actionKeyColours = intArrayOf(0xFFC0C0C0.toInt(), 0xFFC0C0C0.toInt(), 0xFFC0C0C0.toInt(), 0xFFC0C0C0.toInt()),
            actionTextColours = intArrayOf(0xFF0000EE.toInt(), 0xFF551A8B.toInt(), 0xFF0000EE.toInt(), 0xFF0000EE.toInt()),
            keyStyle = KeyStyle.BEVEL,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0xFFC0C0C0.toInt(), 0xFFCDCDCD.toInt(), 0xFFB0B0B0.toInt()),
            typeface = Typeface.create("serif", Typeface.BOLD),
        )

        /**
         * WipEout 2097: black, white and orange with electric cyan and magenta, angular armour-plate keys with orange
         * outlines, heavy lettering, and rows of speed chevrons streaming behind the keys. Colours are estimates,
         * not researched values.
         */
        val WIPEOUT_2097 = KeyboardTheme(
            background = 0xFF050507.toInt(),
            backgroundBottom = 0xFF14141A.toInt(),
            strip = 0xFF0B0B10.toInt(),
            key = 0xFF15151B.toInt(),
            keyPressed = 0xFFFF6A00.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFF00E5FF.toInt(),
            accentText = 0xFF000000.toInt(),
            actionKey = 0xFFFF6A00.toInt(),
            actionKeyPressed = 0xFFFF9A4D.toInt(),
            actionText = 0xFF000000.toInt(),
            subText = 0xFF9AA3AE.toInt(),
            border = 0xFFFF6A00.toInt(),
            actionKeyColours = intArrayOf(0xFFFF6A00.toInt(), 0xFFFF1E8C.toInt(), 0xFF00E5FF.toInt(), 0xFF2A2A33.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.CHAMFER,
            ambient = AmbientKind.CHEVRONS,
            ambientColours = intArrayOf(0xAAFF6A00.toInt(), 0x9900E5FF.toInt(), 0x80FFFFFF.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * WipEout HD: white and light grey angular plates with fine grey outlines, graphite action keys, an orange
         * Enter and highlight, cyan accents, and pale chevrons drifting behind the keys. Colours are estimates, not
         * researched values.
         */
        val WIPEOUT_HD = KeyboardTheme(
            background = 0xFFE9EDF1.toInt(),
            backgroundBottom = 0xFFCBD3DB.toInt(),
            strip = 0xFFF7F9FB.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFBDEBFA.toInt(),
            text = 0xFF1B2229.toInt(),
            accent = 0xFFFF7A00.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            actionKey = 0xFF232B33.toInt(),
            actionKeyPressed = 0xFF4A5560.toInt(),
            actionText = 0xFFFFFFFF.toInt(),
            subText = 0xFF5A6773.toInt(),
            border = 0xFFAEB8C2.toInt(),
            actionKeyColours = intArrayOf(0xFFFF7A00.toInt(), 0xFF232B33.toInt(), 0xFF00B8E6.toInt(), 0xFF232B33.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.CHAMFER,
            ambient = AmbientKind.CHEVRONS,
            ambientColours = intArrayOf(0xAA00B8E6.toInt(), 0xAAFF7A00.toInt(), 0x99232B33.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * A pager: a dark plastic body, a pale grey-green monochrome LCD strip with dark pixel-style lettering and
         * the selected word inverse, and soft grey rubber buttons with a matte sheen. Colours are estimates, not
         * researched values.
         */
        val PAGER = KeyboardTheme(
            background = 0xFF1F2227.toInt(),
            backgroundBottom = 0xFF16181C.toInt(),
            strip = 0xFFB6C1A2.toInt(),
            key = 0xFF8E9290.toInt(),
            keyPressed = 0xFF6F7370.toInt(),
            text = 0xFFB6C1A2.toInt(),
            accent = 0xFF1A1F1A.toInt(),
            accentText = 0xFFB6C1A2.toInt(),
            actionKey = 0xFF7A7E7B.toInt(),
            actionKeyPressed = 0xFF5E625F.toInt(),
            actionText = 0xFF111111.toInt(),
            subText = 0xFF8E9A7C.toInt(),
            border = 0xFF4A4D4A.toInt(),
            keyStyle = KeyStyle.GLOSS,
            keyGloss = 22,
            keyLabel = 0xFF111111.toInt(),
            keySubLabel = 0xFF2F332F.toInt(),
            stripText = 0xFF111111.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Super Mario Bros. (NES, 1985): the overworld sky as a flat background and a black HUD strip with white
         * lettering. Ordinary keys are brick blocks with the lit and shaded edges and bonded mortar of the original; the
         * action keys are ? blocks with a rivet in each corner (Enter in cap red, Delete and Settings in question-block
         * gold, the mode key in warp-pipe green). Pixel clouds drift across the sky behind the keys, with hills and bushes
         * along the bottom, and every tap pops a spinning coin out of the keyboard.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val SUPER_MARIO_BROS = KeyboardTheme(
            background = 0xFF5C94FC.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFFC84C0C.toInt(),
            keyPressed = 0xFFA03C08.toInt(),
            text = 0xFFFCFCFC.toInt(),
            accent = 0xFFD82800.toInt(),
            accentText = 0xFFFCFCFC.toInt(),
            actionKey = 0xFFFC9838.toInt(),
            actionKeyPressed = 0xFFFCC078.toInt(),
            actionText = 0xFF000000.toInt(),
            subText = 0xFFFCD8A8.toInt(),
            border = 0xFF000000.toInt(),
            actionKeyColours = intArrayOf(0xFFD82800.toInt(), 0xFFFC9838.toInt(), 0xFF00A800.toInt(), 0xFFFC9838.toInt()),
            actionTextColours = intArrayOf(0xFFFCFCFC.toInt(), 0xFF000000.toInt(), 0xFFFCFCFC.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.BLOCK,
            shoutLabels = true,
            ambient = AmbientKind.OVERWORLD,
            ambientColours = intArrayOf(0xFFFCFCFC.toInt(), 0xFF9CC4FC.toInt(), 0xFF00A800.toInt(), 0xFF005800.toInt()),
            tapEffect = TapEffectKind.COINS,
            effectColours = intArrayOf(0xFFFC9838.toInt(), 0xFF000000.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Sonic the Hedgehog (1991), Green Hill Zone: a bright sky, keys with a border of the zone's two-tone
         * checkered earth round a deep cobalt face, and gold Chaos Ring lettering for the small letters. Clouds drift over
         * round green hills and a strip of grass-topped checkers scrolls along the bottom as if Sonic were running; every
         * tap scatters spinning gold rings that blink out like the ones you drop when you are hit.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val SONIC_THE_HEDGEHOG = KeyboardTheme(
            background = 0xFF2474F0.toInt(),
            strip = 0xCC1848C8.toInt(),
            key = 0xFF1848C8.toInt(),
            keyPressed = 0xFF2474F0.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFF8D800.toInt(),
            accentText = 0xFF101010.toInt(),
            actionKey = 0xFFF8D800.toInt(),
            actionKeyPressed = 0xFFFFF27A.toInt(),
            actionText = 0xFF101010.toInt(),
            subText = 0xFFF8D800.toInt(),
            border = 0xFFA84800.toInt(),
            shadow = 0xFFE89830.toInt(),
            actionKeyColours = intArrayOf(0xFFF8D800.toInt(), 0xFFA84800.toInt(), 0xFF24A824.toInt(), 0xFFE89830.toInt()),
            actionTextColours = intArrayOf(0xFF101010.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF101010.toInt()),
            keyStyle = KeyStyle.CHECKER,
            shoutLabels = true,
            ambient = AmbientKind.GREEN_HILL,
            ambientColours = intArrayOf(0xB8FFFFFF.toInt(), 0xFFA84800.toInt(), 0xFFE89830.toInt(), 0xFF24A824.toInt()),
            tapEffect = TapEffectKind.RINGS,
            effectColours = intArrayOf(0xFFF8D800.toInt(), 0xFFFFF27A.toInt()),
            typeface = heavyItalic(),
        )

        /**
         * DOOM (1993): the status bar. Keys are riveted steel plates with a bevelled grey frame round a recessed dark
         * panel; the lettering is HUD gold and the small letters status-bar grey. A pressed key lights blood red like the
         * face of the Doomguy, Enter is a Cacodemon-red plate and Delete Hellfire orange. Flames from the console fire
         * effect lick up from the bottom of the screen behind the keys, and every tap throws embers.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val DOOM = KeyboardTheme(
            background = 0xFF0B0B0B.toInt(),
            backgroundBottom = 0xFF240606.toInt(),
            strip = 0xFF4B4B4B.toInt(),
            key = 0xFF2E2E2E.toInt(),
            keyPressed = 0xFF8B0000.toInt(),
            text = 0xFFE2B84A.toInt(),
            accent = 0xFFD75F00.toInt(),
            accentText = 0xFF0B0B0B.toInt(),
            subText = 0xFF979797.toInt(),
            border = 0xFF979797.toInt(),
            keyLabel = 0xFFE2B84A.toInt(),
            keySubLabel = 0xFF979797.toInt(),
            stripText = 0xFFF0D585.toInt(),
            actionKeyColours = intArrayOf(0xFF8B0000.toInt(), 0xFFD75F00.toInt(), 0xFF4B4B4B.toInt(), 0xFF4B4B4B.toInt()),
            actionTextColours = intArrayOf(0xFFFFE08A.toInt(), 0xFF0B0B0B.toInt(), 0xFFE2B84A.toInt(), 0xFFE2B84A.toInt()),
            keyStyle = KeyStyle.RIVETED,
            shoutLabels = true,
            ambient = AmbientKind.FIRE,
            ambientColours = intArrayOf(0xFF3A0000.toInt(), 0xFF8B0000.toInt(), 0xFFD75F00.toInt(), 0xFFC79B2B.toInt()),
            tapEffect = TapEffectKind.EMBERS,
            effectColours = intArrayOf(0xFFC79B2B.toInt(), 0xFFD75F00.toInt(), 0xFF8B0000.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Final Fantasy VII (1997): the menu. Every key and the candidate strip is one of the game's blue windows,
         * fading from bright blue at the top left to deep navy at the bottom right, with the silver rim and fine inner line
         * of the originals and white menu lettering. A pressed key brightens like the highlighted entry, Enter is Mako
         * green, and a pointing glove slides in to touch wherever you tap, as the menu cursor does.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val FINAL_FANTASY_VII = KeyboardTheme(
            background = 0xFF000078.toInt(),
            backgroundBottom = 0xFF000038.toInt(),
            strip = 0xFF000078.toInt(),
            key = 0xFF000078.toInt(),
            keyPressed = 0xFF0048B8.toInt(),
            text = 0xFFF0F4F8.toInt(),
            accent = 0xFF4CE6A8.toInt(),
            accentText = 0xFF000038.toInt(),
            subText = 0xFFB8C4D0.toInt(),
            border = 0xFFB8C4D0.toInt(),
            actionKeyColours = intArrayOf(0xFF4CE6A8.toInt(), 0xFFB8C4D0.toInt(), 0xFF0048B8.toInt(), 0xFFB8C4D0.toInt()),
            actionTextColours = intArrayOf(0xFF000038.toInt(), 0xFF000038.toInt(), 0xFFF0F4F8.toInt(), 0xFF000038.toInt()),
            keyStyle = KeyStyle.WINDOW,
            stripStyle = StripStyle.WINDOW,
            tapEffect = TapEffectKind.POINTER,
            effectColours = intArrayOf(0xFFF0F4F8.toInt(), 0xFF000038.toInt(), 0xFFB8C4D0.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Worms Armageddon (1999): the landscape. The background is dark soil, and every key is a chunk of terrain: a
         * bumpy cap of grass over flecked topsoil with a dark outline, sinking a little when pressed. Enter is Bazooka blast
         * orange, Delete Worm pink, the mode key Holy Hand Grenade gold and Settings grass green. Each tap sets off a
         * ragged orange explosion that throws lumps of earth.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val WORMS_ARMAGEDDON = KeyboardTheme(
            background = 0xFF3A2410.toInt(),
            backgroundBottom = 0xFF241609.toInt(),
            strip = 0xFF2B1A0C.toInt(),
            key = 0xFF8C5828.toInt(),
            keyPressed = 0xFFC49A45.toInt(),
            text = 0xFFFFF2D6.toInt(),
            accent = 0xFFF29CA3.toInt(),
            accentText = 0xFF2B1A0C.toInt(),
            subText = 0xFFFFE08A.toInt(),
            border = 0xFF3A2410.toInt(),
            shadow = 0xFF4E9A26.toInt(),
            actionKeyColours = intArrayOf(0xFFE64819.toInt(), 0xFFF29CA3.toInt(), 0xFFF5C724.toInt(), 0xFF4E9A26.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF2B1A0C.toInt(), 0xFF2B1A0C.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.TERRAIN,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0xFF3A2410.toInt(), 0xFF47301A.toInt(), 0xFF2B1A0B.toInt()),
            tapEffect = TapEffectKind.BLAST,
            effectColours = intArrayOf(0xFFE64819.toInt(), 0xFFF5C724.toInt(), 0xFF8C5828.toInt(), 0xFF4E9A26.toInt(), 0xFFC49A45.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * The Sims (2000): the control panel. A blue gradient background and glassy blue panel buttons with a light rim
         * and a dark line inside it; the candidate strip is a white thought-bubble panel, and a pressed key lights its
         * outline plumbob green. Enter is Plumbob green, Delete Simoleon gold. Every tap raises a turning green plumbob.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val THE_SIMS = KeyboardTheme(
            background = 0xFF1B3B6F.toInt(),
            backgroundBottom = 0xFF2E6DB4.toInt(),
            strip = 0xFFEEF3F8.toInt(),
            key = 0xFF2E6DB4.toInt(),
            keyPressed = 0xFF4A8BD6.toInt(),
            text = 0xFFEEF3F8.toInt(),
            accent = 0xFF4CBB17.toInt(),
            accentText = 0xFF10260A.toInt(),
            subText = 0xFFC4DAF0.toInt(),
            stripText = 0xFF1B3B6F.toInt(),
            actionKeyColours = intArrayOf(0xFF4CBB17.toInt(), 0xFFF5B82E.toInt(), 0xFF1B3B6F.toInt(), 0xFF1B3B6F.toInt()),
            actionTextColours = intArrayOf(0xFF10260A.toInt(), 0xFF2A1C00.toInt(), 0xFFEEF3F8.toInt(), 0xFFEEF3F8.toInt()),
            keyStyle = KeyStyle.PANEL,
            tapEffect = TapEffectKind.PLUMBOB,
            effectColours = intArrayOf(0xFF4CBB17.toInt(), 0xFF9BE53D.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Grand Theft Auto: Vice City (2002): 1980s Miami at dusk. Translucent Art Deco keys with stepped corners and two
         * neon outlines (cyan, then pink) over a purple night sky, with a striped neon sun sinking into the sea, its
         * slits scrolling down it as it bobs, twinkling stars, a glow along the horizon that breathes, a reflection
         * shimmering below it and a palm tree swaying at each side. Enter is neon pink, Delete sunset
         * orange. Every tap throws neon sparkles.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val GTA_VICE_CITY = KeyboardTheme(
            background = 0xFF1D1135.toInt(),
            backgroundBottom = 0xFF5C2A78.toInt(),
            strip = 0x991D1135.toInt(),
            key = 0xB31D1135.toInt(),
            keyPressed = 0xE05C2A78.toInt(),
            text = 0xFFFBE4C8.toInt(),
            accent = 0xFFF25CA2.toInt(),
            accentText = 0xFF1D1135.toInt(),
            subText = 0xFF33D1D6.toInt(),
            border = 0xFF33D1D6.toInt(),
            actionKeyColours = intArrayOf(0xFFF25CA2.toInt(), 0xFFF7943D.toInt(), 0xFF33D1D6.toInt(), 0xFF5C2A78.toInt()),
            actionTextColours = intArrayOf(0xFF1D1135.toInt(), 0xFF1D1135.toInt(), 0xFF1D1135.toInt(), 0xFFFBE4C8.toInt()),
            keyStyle = KeyStyle.DECO,
            ambient = AmbientKind.SUNSET,
            ambientColours = intArrayOf(0xFFF7943D.toInt(), 0xFFF25CA2.toInt(), 0xFF33D1D6.toInt(), 0xFF120922.toInt()),
            tapEffect = TapEffectKind.SPARKLE,
            effectColours = intArrayOf(0xFFF25CA2.toInt(), 0xFF33D1D6.toInt(), 0xFFF7943D.toInt(), 0xFFFBE4C8.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Grand Theft Auto: San Andreas (2004): the pause menu. Every key is a flat dark menu row with a fine rule above
         * and a heavier one below in sandy cream, lettered in the game's cream capitals; a pressed key is the highlighted
         * entry, in Grove Street green with a bar down its left side. Behind them is speckled asphalt. Action keys
         * carry the gang colours (Grove green, Ballas purple, clay) and every tap spray-paints one colour of those
         * with a wet centre, a mist of flecks and a drip or two running down.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val GTA_SAN_ANDREAS = KeyboardTheme(
            background = 0xFF1D1E1C.toInt(),
            strip = 0xFF2A2B28.toInt(),
            key = 0xE6151614.toInt(),
            keyPressed = 0xFF2E5A36.toInt(),
            text = 0xFFECE5D4.toInt(),
            accent = 0xFF2E5A36.toInt(),
            accentText = 0xFFECE5D4.toInt(),
            subText = 0xFFB5A27A.toInt(),
            border = 0xFFB5A27A.toInt(),
            stripText = 0xFFECE5D4.toInt(),
            actionKeyColours = intArrayOf(0xFF2E5A36.toInt(), 0xFFA8563B.toInt(), 0xFF5C3A6B.toInt(), 0xFFB5A27A.toInt()),
            actionTextColours = intArrayOf(0xFFECE5D4.toInt(), 0xFFECE5D4.toInt(), 0xFFECE5D4.toInt(), 0xFF1D1E1C.toInt()),
            keyStyle = KeyStyle.MENU,
            shoutLabels = true,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0xFF1D1E1C.toInt(), 0xFF2A2B27.toInt(), 0xFF131412.toInt()),
            tapEffect = TapEffectKind.SPRAY,
            effectColours = intArrayOf(0xFF4C8F5B.toInt(), 0xFF8A5FA0.toInt(), 0xFFECE5D4.toInt(), 0xFFD27A58.toInt()),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * The Pip-Boy 3000 (Fallout 3 and New Vegas): a monochrome green phosphor screen. Keys are dark green boxes with
         * clipped corners, fine scanlines and an outline that glows; a pressed key floods dim green and its outline blooms.
         * Enter is lit solid like a selected entry and Delete is the New Vegas amber. Scanlines, a rolling bar, a flicker and
         * a dark vignette play over the glass, a block cursor blinks on the last key, and taps ping like a radar.
         * Palette from the theme-ideas sheet; the look is drawn from the game itself.
         */
        val PIP_BOY = KeyboardTheme(
            background = 0xFF050F08.toInt(),
            backgroundBottom = 0xFF0A1C10.toInt(),
            strip = 0xFF0D2615.toInt(),
            key = 0xFF0D2615.toInt(),
            keyPressed = 0xFF1A662E.toInt(),
            text = 0xFF1AFF80.toInt(),
            accent = 0xFF1AFF80.toInt(),
            accentText = 0xFF050F08.toInt(),
            subText = 0xFF17B15A.toInt(),
            border = 0xFF18B35A.toInt(),
            deep = 0xFF80FFB8.toInt(),
            actionKeyColours = intArrayOf(0xFF1AFF80.toInt(), 0xFFFFB642.toInt(), 0xFF1A662E.toInt(), 0xFF1A662E.toInt()),
            actionTextColours = intArrayOf(0xFF050F08.toInt(), 0xFF050F08.toInt(), 0xFF80FFB8.toInt(), 0xFF80FFB8.toInt()),
            keyStyle = KeyStyle.PHOSPHOR,
            shoutLabels = true,
            ambient = AmbientKind.PIPBOY,
            ambientColours = intArrayOf(0x40000000.toInt(), 0x2E1AFF80.toInt(), 0xDD000000.toInt(), 0x141AFF80.toInt()),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFF1AFF80.toInt(), 0xFF80FFB8.toInt()),
            cursorColour = 0xFF1AFF80.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * The Pip-Boy 3000 in amber: the same screen as [PIP_BOY] in the amber phosphor of the later games. Keys are dark
         * brown boxes with clipped corners, fine scanlines and a glowing amber outline; a pressed key floods dim amber and
         * its outline blooms. Enter is lit solid like a selected entry and Delete is a pale cream. Scanlines, a rolling
         * bar, a flicker and a dark vignette play over the glass, a block cursor blinks on the last key, and taps ping
         * like a radar.
         */
        val PIP_BOY_AMBER = KeyboardTheme(
            background = 0xFF140D03.toInt(),
            backgroundBottom = 0xFF241604.toInt(),
            strip = 0xFF2B1B06.toInt(),
            key = 0xFF2B1B06.toInt(),
            keyPressed = 0xFF6B4410.toInt(),
            text = 0xFFFFB02E.toInt(),
            accent = 0xFFFFB02E.toInt(),
            accentText = 0xFF140D03.toInt(),
            subText = 0xFFB87A14.toInt(),
            border = 0xFFB87F1A.toInt(),
            deep = 0xFFFFD98A.toInt(),
            actionKeyColours = intArrayOf(0xFFFFB02E.toInt(), 0xFFFFE9B8.toInt(), 0xFF6B4410.toInt(), 0xFF6B4410.toInt()),
            actionTextColours = intArrayOf(0xFF140D03.toInt(), 0xFF140D03.toInt(), 0xFFFFD98A.toInt(), 0xFFFFD98A.toInt()),
            keyStyle = KeyStyle.PHOSPHOR,
            shoutLabels = true,
            ambient = AmbientKind.PIPBOY,
            ambientColours = intArrayOf(0x40000000.toInt(), 0x2EFFB02E.toInt(), 0xDD000000.toInt(), 0x14FFB02E.toInt()),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFFFFB02E.toInt(), 0xFFFFD98A.toInt()),
            cursorColour = 0xFFFFB02E.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Generation 1 on the original Game Boy: the four greens of its screen and nothing else. Every key is a pixel-stepped
         * text box with a double line round it, like the game's menus, and a pressed key gets the selection arrow. The candidates are in
         * the game's block lettering, and every tap pops a few pixels in the two darkest greens.
         */
        val GEN_1 = KeyboardTheme(
            background = 0xFF8BAC0F.toInt(),
            strip = 0xFF9BBC0F.toInt(),
            key = 0xFF9BBC0F.toInt(),
            keyPressed = 0xFF8BAC0F.toInt(),
            text = 0xFF0F380F.toInt(),
            accent = 0xFF0F380F.toInt(),
            accentText = 0xFF9BBC0F.toInt(),
            subText = 0xFF306230.toInt(),
            border = 0xFF0F380F.toInt(),
            shadow = 0xFF306230.toInt(),
            stripText = 0xFF0F380F.toInt(),
            actionKeyColours = intArrayOf(0xFF306230.toInt(), 0xFF8BAC0F.toInt(), 0xFF9BBC0F.toInt(), 0xFF9BBC0F.toInt()),
            actionTextColours = intArrayOf(0xFF9BBC0F.toInt(), 0xFF0F380F.toInt(), 0xFF0F380F.toInt(), 0xFF0F380F.toInt()),
            keyStyle = KeyStyle.TEXTBOX,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFF0F380F.toInt(), 0xFF306230.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Generation 2 on a Game Boy Color: the same text boxes in the soft, slightly washed colours of its screen, a pale sky
         * fading into meadow behind them. A pressed key turns butter yellow and gets the selection arrow; Enter is the green of a full
         * health bar, Delete the red of an empty one. The candidates are in block lettering, and every tap pops a few coloured pixels.
         */
        val GEN_2 = KeyboardTheme(
            background = 0xFFC8DCF0.toInt(),
            backgroundBottom = 0xFFA8CC98.toInt(),
            strip = 0xFFF8F8F0.toInt(),
            key = 0xFFF8F8F0.toInt(),
            keyPressed = 0xFFFFE8A0.toInt(),
            text = 0xFF181820.toInt(),
            accent = 0xFF2858C8.toInt(),
            accentText = 0xFFF8F8F0.toInt(),
            subText = 0xFF586078.toInt(),
            border = 0xFF181820.toInt(),
            shadow = 0xFFA0A8C0.toInt(),
            stripText = 0xFF181820.toInt(),
            actionKeyColours = intArrayOf(0xFF58B848.toInt(), 0xFFE86050.toInt(), 0xFF68A0E8.toInt(), 0xFFF0D048.toInt()),
            actionTextColours = intArrayOf(0xFF102010.toInt(), 0xFF201010.toInt(), 0xFF101828.toInt(), 0xFF282008.toInt()),
            keyStyle = KeyStyle.TEXTBOX,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFE86050.toInt(), 0xFFF0D048.toInt(), 0xFF58B848.toInt(), 0xFF68A0E8.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Generation 3 on a Game Boy Advance: the same text boxes, brighter and sharper, on a deep sea blue like its screen, with a
         * slate frame and a pale second line. A pressed key gets the selection arrow; Enter is red and Delete blue, with green and
         * yellow for the others. The candidates sit on a slate bar in block lettering, and every tap pops a few bright pixels.
         */
        val GEN_3 = KeyboardTheme(
            background = 0xFF3C8CD8.toInt(),
            backgroundBottom = 0xFF2A64B0.toInt(),
            strip = 0xFF303850.toInt(),
            key = 0xFFF8F8F8.toInt(),
            keyPressed = 0xFFDCEBFF.toInt(),
            text = 0xFF282830.toInt(),
            accent = 0xFFE04838.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFF68708C.toInt(),
            border = 0xFF404860.toInt(),
            shadow = 0xFFB0B8D0.toInt(),
            stripText = 0xFFF8F8F8.toInt(),
            actionKeyColours = intArrayOf(0xFFE85040.toInt(), 0xFF3880E0.toInt(), 0xFF48B868.toInt(), 0xFFF0C838.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF0C2010.toInt(), 0xFF282008.toInt()),
            keyStyle = KeyStyle.TEXTBOX,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFE04838.toInt(), 0xFFF0C030.toInt(), 0xFF48B868.toInt(), 0xFF3878E0.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.BOLD),
        )

        /**
         * A BlackBerry thumb keyboard: near-black plastic with a faint grain, and every key a slightly domed cap with a lit upper edge,
         * standing on a chrome fret that runs the width of its row so the rows are divided by metal lines. The candidates sit on
         * the bright, bluish screen in its black frame, the chosen one picked out in blue. Enter is the blue of the selection.
         */
        val BLACKBERRY = KeyboardTheme(
            background = 0xFF0E0F11.toInt(),
            backgroundBottom = 0xFF1A1B1E.toInt(),
            strip = 0xFF050506.toInt(),
            deep = 0xFFEEF3F9.toInt(),
            key = 0xFF2B2C30.toInt(),
            keyPressed = 0xFF45474D.toInt(),
            text = 0xFFE6E8EC.toInt(),
            accent = 0xFF2E6FD8.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFF9A9EA6.toInt(),
            border = 0xFFB9BDC4.toInt(),
            stripText = 0xFF18222E.toInt(),
            actionKeyColours = intArrayOf(0xFF1F5FC8.toInt(), 0xFF3A3B40.toInt(), 0xFF303236.toInt(), 0xFF303236.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFE6E8EC.toInt(), 0xFFE6E8EC.toInt(), 0xFFE6E8EC.toInt()),
            keyStyle = KeyStyle.FRETTED,
            candidateStyle = CandidateStyle.SQUARE,
            stripStyle = StripStyle.LCD_PANEL,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0x00000000, 0x12FFFFFF, 0x24000000),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * A toy pet: a mint egg shell with a pale dot pattern, the grey-green LCD in a darker frame with its words in pixels, and
         * every key a plump glossy button in a different pastel, standing on a soft shadow, like the three under the screen.
         * Enter is yellow and Delete coral. Each tap floats a few little hearts up from the button.
         */
        val TAMAGOTCHI = KeyboardTheme(
            background = 0xFF74D3C0.toInt(),
            backgroundBottom = 0xFF5FC3B0.toInt(),
            strip = 0xFF3E9E8C.toInt(),
            deep = 0xFFB7C4A5.toInt(),
            key = 0xFFFFB3C7.toInt(),
            keyPressed = 0xFFE88DA8.toInt(),
            text = 0xFF4A2B3A.toInt(),
            accent = 0xFF2B3226.toInt(),
            accentText = 0xFFB7C4A5.toInt(),
            subText = 0xFF6B4A5A.toInt(),
            shadow = 0xFF2F7C6D.toInt(),
            stripText = 0xFF22281E.toInt(),
            keyCycle = intArrayOf(0xFFFFB3C7.toInt(), 0xFFFFE066.toInt(), 0xFF9ED8FF.toInt(), 0xFFD2B8FF.toInt(), 0xFFFFC38A.toInt()),
            actionKeyColours = intArrayOf(0xFFFFD84D.toInt(), 0xFFFF7A7A.toInt(), 0xFF8EC9FF.toInt(), 0xFFC7A6FF.toInt()),
            actionTextColours = intArrayOf(0xFF4A3A00.toInt(), 0xFF4A1010.toInt(), 0xFF10304A.toInt(), 0xFF2A104A.toInt()),
            keyStyle = KeyStyle.BUTTON,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.LCD_PANEL,
            ambient = AmbientKind.HALFTONE,
            ambientColours = intArrayOf(0x26FFFFFF),
            tapEffect = TapEffectKind.HEARTS,
            effectColours = intArrayOf(0xFFFF6F91.toInt(), 0xFFFFD84D.toInt(), 0xFFFFFFFF.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD),
        )

        /**
         * A silver single-screen Game & Watch: a brushed silver faceplate, the LCD in its black frame with the faint ghosts of unlit
         * digits and its words in pixels, silver plates in their bezels for keys, with Enter a red button, as the A and B buttons
         * were, and Delete a cream one. Each tap pops a few black LCD dots.
         */
        val GAME_AND_WATCH = KeyboardTheme(
            background = 0xFFC4C7CC.toInt(),
            backgroundBottom = 0xFFA9ADB3.toInt(),
            strip = 0xFF2A2C30.toInt(),
            deep = 0xFFD2D6C2.toInt(),
            key = 0xFFD7DADF.toInt(),
            keyPressed = 0xFFB9BDC4.toInt(),
            text = 0xFF2B2D31.toInt(),
            accent = 0xFFD3322B.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFF5A5E66.toInt(),
            border = 0xFF6E7279.toInt(),
            stripText = 0xFF22241C.toInt(),
            actionKeyColours = intArrayOf(0xFFD3322B.toInt(), 0xFFE8E2CF.toInt(), 0xFF3A3D42.toInt(), 0xFF3A3D42.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF2B2D31.toInt(), 0xFFF0F0F0.toInt(), 0xFFF0F0F0.toInt()),
            keyStyle = KeyStyle.PLATE,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.LCD_DIGITS,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0x00000000, 0x22FFFFFF, 0x18000000),
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFF22241C.toInt(), 0xFF4A4E40.toInt()),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * A pocket calculator: a dark case, the LCD in its frame with the faint ghosts of unlit seven-segment digits and its words in
         * pixels, and rubber keys in two tones, grey for the digits, with Enter orange like the equals key and Delete the red of the
         * clear key.
         */
        val CASIO_CALCULATOR = KeyboardTheme(
            background = 0xFF2A2C2F.toInt(),
            backgroundBottom = 0xFF1E2023.toInt(),
            strip = 0xFF3A3D41.toInt(),
            deep = 0xFFB3BDA0.toInt(),
            key = 0xFF3C3F44.toInt(),
            keyPressed = 0xFF2A2C30.toInt(),
            text = 0xFFF0F0F0.toInt(),
            accent = 0xFFE8872B.toInt(),
            accentText = 0xFF1A1A1A.toInt(),
            subText = 0xFFA0A5AB.toInt(),
            border = 0xFF111214.toInt(),
            stripText = 0xFF23281C.toInt(),
            actionKeyColours = intArrayOf(0xFFE8872B.toInt(), 0xFFD8453A.toInt(), 0xFF5E7C9A.toInt(), 0xFF7A8087.toInt()),
            actionTextColours = intArrayOf(0xFF1A1A1A.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.MOULDED,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.LCD_DIGITS,
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD),
        )

        /**
         * A Mega Drive: a clean white background and, for the keys, the black ribbed plastic of the console, each with a lit edge
         * and a band of fine ribs. The candidates are in the blue of the start-up screen on white, the chosen one picked out in solid
         * blue. Enter is that blue, Delete gold, and each tap sends a ring of blue rippling out.
         */
        val MEGA_DRIVE = KeyboardTheme(
            background = 0xFFFFFFFF.toInt(),
            backgroundBottom = 0xFFE4E7EC.toInt(),
            strip = 0xFFFFFFFF.toInt(),
            key = 0xFF16161A.toInt(),
            keyPressed = 0xFF34343A.toInt(),
            text = 0xFFEDEDED.toInt(),
            accent = 0xFF0C4FC9.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFFA8ABB2.toInt(),
            border = 0xFF000000.toInt(),
            stripText = 0xFF0C4FC9.toInt(),
            actionKeyColours = intArrayOf(0xFF0C4FC9.toInt(), 0xFFC9A44C.toInt(), 0xFFB8BBC2.toInt(), 0xFF2A2A30.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF1A1400.toInt(), 0xFF16161A.toInt(), 0xFFEDEDED.toInt()),
            keyStyle = KeyStyle.RIBBED,
            candidateStyle = CandidateStyle.SQUARE,
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFF0C4FC9.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD),
        )

        /**
         * A ZX Spectrum: a black case with grey rubber keys, the candidates on white paper in pixel lettering with the four slanted
         * rainbow stripes in its corner, the chosen word in blue, and a red flashing cursor on the key you pressed. Every key press
         * loads a burst of tape: bands of red and cyan, then ragged yellow and blue, race down behind the keys and cut out.
         */
        val ZX_SPECTRUM = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFFD7D7D7.toInt(),
            key = 0xFFCACACE.toInt(),
            keyPressed = 0xFF9A9AA0.toInt(),
            text = 0xFF000000.toInt(),
            accent = 0xFF0000D7.toInt(),
            accentText = 0xFFD7D7D7.toInt(),
            subText = 0xFF0000D7.toInt(),
            border = 0xFF000000.toInt(),
            stripText = 0xFF000000.toInt(),
            actionKeyColours = intArrayOf(0xFF0000D7.toInt(), 0xFFD70000.toInt(), 0xFF00D700.toInt(), 0xFFD7D700.toInt()),
            actionTextColours = intArrayOf(0xFFD7D7D7.toInt(), 0xFFD7D7D7.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.MOULDED,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.SPECTRUM_PAPER,
            ambient = AmbientKind.TAPE_LOADING,
            ambientColours = intArrayOf(0xFFD70000.toInt(), 0xFF00D7D7.toInt(), 0xFFD7D700.toInt(), 0xFF0000D7.toInt()),
            cursorColour = 0xFFD70000.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * A Commodore 64: the light blue border and the blue screen, with the words on it in light blue pixels and the chosen one in
         * reverse video. The keys are beige with dark lettering, Enter and the others a function-key brown, and a light blue block
         * cursor blinks on the last key you pressed.
         */
        val COMMODORE_64 = KeyboardTheme(
            background = 0xFF6C5EB5.toInt(),
            strip = 0xFF6C5EB5.toInt(),
            deep = 0xFF352879.toInt(),
            key = 0xFFD9CFB8.toInt(),
            keyPressed = 0xFFB5AB94.toInt(),
            text = 0xFF3B3226.toInt(),
            accent = 0xFF9A90E8.toInt(),
            accentText = 0xFF352879.toInt(),
            subText = 0xFF6B5F4A.toInt(),
            border = 0xFF6B5F4A.toInt(),
            stripText = 0xFF9A90E8.toInt(),
            actionKeyColours = intArrayOf(0xFF7A5F4A.toInt(), 0xFF7A5F4A.toInt(), 0xFF7A5F4A.toInt(), 0xFF7A5F4A.toInt()),
            actionTextColours = intArrayOf(0xFFE8DFC8.toInt(), 0xFFE8DFC8.toInt(), 0xFFE8DFC8.toInt(), 0xFFE8DFC8.toInt()),
            keyStyle = KeyStyle.MOULDED,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.LCD_PANEL,
            cursorColour = 0xFF6C5EB5.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * A teletext page: black, with every key a frame of chunky block-graphics cells in one of the page colours round a black inside
         * and white lettering, and the four coloured buttons of a teletext remote (red, green, yellow, cyan) for Enter, Delete and the
         * others. The candidates are in yellow pixels with the chosen word on blue, and each tap pops blocks in the page colours.
         */
        val TELETEXT = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFFFFFFFF.toInt(),
            keyPressed = 0xFFC0C0C0.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFF0000FF.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFF00FFFF.toInt(),
            stripText = 0xFFFFFF00.toInt(),
            keyCycle = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFFFFFF00.toInt(), 0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFFFFF.toInt()),
            actionKeyColours = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.MOSAIC,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFFFFFF00.toInt(), 0xFF00FFFF.toInt(), 0xFFFF00FF.toInt(), 0xFFFFFFFF.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * MSN Messenger, circa 2003: a pale sky-blue window fading to white, glossy white buttons with an amber ring when pressed,
         * and the status colours on the action keys (green for online, red for busy, amber for away). A white strip carries blue
         * candidate words, and each tap lets a yellow smiley float up.
         */
        val MSN_MESSENGER = KeyboardTheme(
            background = 0xFFA8D0F0.toInt(),
            backgroundBottom = 0xFFFFFFFF.toInt(),
            strip = 0xFFFFFFFF.toInt(),
            key = 0xEBFCFCFB.toInt(),
            keyPressed = 0xFFEAE9E1.toInt(),
            text = 0xFF14181F.toInt(),
            accent = 0xFF0A4DB5.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFF4A5A78.toInt(),
            border = 0xFF6C9BD2.toInt(),
            stripText = 0xFF0A4DB5.toInt(),
            actionKeyColours = intArrayOf(0xFF3CA03C.toInt(), 0xFFD0432A.toInt(), 0xFFF0A020.toInt(), 0xFF7A8CA8.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF1A1200.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.GLOSS,
            keyRing = 0xFFF8B330.toInt(),
            keyGloss = 120,
            tapEffect = TapEffectKind.SMILEYS,
            effectColours = intArrayOf(0xFFFFD21E.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Tetris on the original Game Boy: four shades of green, every key a framed tile with notched corners and a light and dark
         * bevel like a block, the candidates in an LCD panel, and a tetromino that tumbles down and flickers out wherever you tap.
         */
        val TETRIS = KeyboardTheme(
            background = 0xFF9BBC0F.toInt(),
            strip = 0xFF9BBC0F.toInt(),
            key = 0xFF8BAC0F.toInt(),
            keyPressed = 0xFF306230.toInt(),
            text = 0xFF0F380F.toInt(),
            accent = 0xFF0F380F.toInt(),
            accentText = 0xFF9BBC0F.toInt(),
            subText = 0xFF306230.toInt(),
            border = 0xFF0F380F.toInt(),
            deep = 0xFF9BBC0F.toInt(),
            shadow = 0xFF306230.toInt(),
            stripText = 0xFF0F380F.toInt(),
            actionKeyColours = intArrayOf(0xFF0F380F.toInt(), 0xFF306230.toInt(), 0xFF306230.toInt(), 0xFF306230.toInt()),
            actionTextColours = intArrayOf(0xFF9BBC0F.toInt(), 0xFF9BBC0F.toInt(), 0xFF9BBC0F.toInt(), 0xFF9BBC0F.toInt()),
            keyStyle = KeyStyle.TILE,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            stripStyle = StripStyle.LCD_PANEL,
            tapEffect = TapEffectKind.TETROMINO,
            effectColours = intArrayOf(0xFF0F380F.toInt(), 0xFF306230.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Pac-Man: the black maze with its blue double walls round every key and a pellet at the foot of each, which Pac-Man eats
         * (it vanishes) when you press. Candidates are yellow, the action keys are the four ghosts' colours, and taps scatter pixels
         * in the ghost colours. No characters are drawn, only the maze, the dots and the colours.
         */
        val PAC_MAN = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xFF000000.toInt(),
            keyPressed = 0xFF10104A.toInt(),
            text = 0xFFFFE000.toInt(),
            accent = 0xFFFFE000.toInt(),
            accentText = 0xFF000000.toInt(),
            subText = 0xFFFFB8AE.toInt(),
            border = 0xFF2121DE.toInt(),
            shadow = 0xFFFFB8AE.toInt(),
            stripText = 0xFFFFE000.toInt(),
            actionKeyColours = intArrayOf(0xFFFF0000.toInt(), 0xFFFFB8FF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFB852.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.MAZE,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFFF0000.toInt(), 0xFFFFB8FF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFB852.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * A codec call from Metal Gear Solid: a dark teal screen, keys that are green targeting boxes with corner brackets, a green
         * equaliser pulsing behind them like the voice meter, and a scan line sweeping over the glass.
         */
        val CODEC_CALL = KeyboardTheme(
            background = 0xFF062A2A.toInt(),
            backgroundBottom = 0xFF031818.toInt(),
            strip = 0xFF031818.toInt(),
            key = 0xFF0A3A36.toInt(),
            keyPressed = 0xFF1E7A62.toInt(),
            text = 0xFF5BFFA0.toInt(),
            accent = 0xFF5BFFA0.toInt(),
            accentText = 0xFF062A2A.toInt(),
            subText = 0xFF2E9A6A.toInt(),
            border = 0xFF2E9A6A.toInt(),
            deep = 0xFF5BFFA0.toInt(),
            stripText = 0xFF5BFFA0.toInt(),
            actionKeyColours = intArrayOf(0xFF5BFFA0.toInt(), 0xFFFF7A3A.toInt(), 0xFF2E9A6A.toInt(), 0xFF2E9A6A.toInt()),
            actionTextColours = intArrayOf(0xFF031818.toInt(), 0xFF031818.toInt(), 0xFF031818.toInt(), 0xFF031818.toInt()),
            keyStyle = KeyStyle.RETICLE,
            ambient = AmbientKind.EQUALISER,
            ambientColours = intArrayOf(0xFF1E9A5A.toInt(), 0xFF5BFFA0.toInt()),
            overlay = AmbientKind.SCAN,
            overlayColours = intArrayOf(0x335BFFA0.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * The HEV suit from Half-Life: the orange HUD on near-black, bracketed readout keys, and an orange pulse behind the keys
         * each time you type, like the suit's charge indicators.
         */
        val HEV_SUIT = KeyboardTheme(
            background = 0xFF0A0600.toInt(),
            strip = 0xFF0A0600.toInt(),
            key = 0xFF1A1000.toInt(),
            keyPressed = 0xFF5A3A00.toInt(),
            text = 0xFFFFB000.toInt(),
            accent = 0xFFFFB000.toInt(),
            accentText = 0xFF0A0600.toInt(),
            subText = 0xFFB07800.toInt(),
            border = 0xFFFFB000.toInt(),
            deep = 0xFFFFB000.toInt(),
            stripText = 0xFFFFB000.toInt(),
            actionKeyColours = intArrayOf(0xFFFFB000.toInt(), 0xFFFF5A00.toInt(), 0xFF5A3A00.toInt(), 0xFF5A3A00.toInt()),
            actionTextColours = intArrayOf(0xFF0A0600.toInt(), 0xFF0A0600.toInt(), 0xFFFFB000.toInt(), 0xFFFFB000.toInt()),
            keyStyle = KeyStyle.RETICLE,
            pulseColour = 0xFF5A3A00.toInt(),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * Minecraft: every key a block of speckled stone with a black outline and a light and dark bevel, turning bluish when
         * pressed like a hovered button, over a backdrop of dirt tiles. The candidates are white on a dark bar with grass green
         * for the chosen word, and each tap breaks a few pixels of dirt, grass and stone away.
         */
        val MINECRAFT = KeyboardTheme(
            background = 0xFF3B2A1C.toInt(),
            strip = 0xFF1E1E1E.toInt(),
            key = 0xFF727272.toInt(),
            keyPressed = 0xFF7C8AD6.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFF5DAA2E.toInt(),
            accentText = 0xFFFFFFFF.toInt(),
            subText = 0xFFC6C6C6.toInt(),
            border = 0xFF000000.toInt(),
            stripText = 0xFFFFFFFF.toInt(),
            actionKeyColours = intArrayOf(0xFF5DAA2E.toInt(), 0xFFB03A2E.toInt(), 0xFF8B6B3E.toInt(), 0xFF8B6B3E.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.CRAFT,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            ambient = AmbientKind.PIXEL_TILES,
            ambientColours = intArrayOf(0xFF5A4128.toInt(), 0xFF6B4C30.toInt(), 0xFF4A3420.toInt(), 0xFF7A5A38.toInt()),
            tapEffect = TapEffectKind.DEBRIS,
            effectColours = intArrayOf(0xFF866043.toInt(), 0xFF5DAA2E.toInt(), 0xFF727272.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Joy Division, Unknown Pleasures (1979): black, with the sleeve's stacked white waveforms filling the background, each row
         * of line hiding the one behind it. The keys are black outlined boxes in plain white sans-serif, and a tap sends a thin
         * ring out like a pulse.
         */
        val JOY_DIVISION = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF000000.toInt(),
            key = 0xCC000000.toInt(),
            keyPressed = 0xFF2A2A2A.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFFFFF.toInt(),
            accentText = 0xFF000000.toInt(),
            subText = 0xFF9A9A9A.toInt(),
            border = 0xFFFFFFFF.toInt(),
            stripText = 0xFFFFFFFF.toInt(),
            actionKeyColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF3A3A3A.toInt(), 0xFF3A3A3A.toInt(), 0xFF3A3A3A.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()),
            keyStyle = KeyStyle.DEFAULT,
            ambient = AmbientKind.PULSAR,
            ambientColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt()),
            tapEffect = TapEffectKind.RINGS,
            effectColours = intArrayOf(0xFFFFFFFF.toInt()),
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL),
        )

        /**
         * The Nostromo's computer terminal from Alien (1979): amber lettering with pale cyan sub-labels on black, every key in a
         * double-line text-mode box, a fine measuring grid with a scan line behind them, and the scanlines and flicker of an old
         * monitor over the glass. Labels are in capitals, and a block cursor blinks on the last key.
         */
        val NOSTROMO = KeyboardTheme(
            background = 0xFF050403.toInt(),
            strip = 0xFF050403.toInt(),
            key = 0xFF120B04.toInt(),
            keyPressed = 0xFF4A2A06.toInt(),
            text = 0xFFFF9A1F.toInt(),
            accent = 0xFFFF9A1F.toInt(),
            accentText = 0xFF050403.toInt(),
            subText = 0xFF8FD8E8.toInt(),
            border = 0xFFC06A10.toInt(),
            stripText = 0xFFFF9A1F.toInt(),
            actionKeyColours = intArrayOf(0xFFFF9A1F.toInt(), 0xFFD8401A.toInt(), 0xFF3A2208.toInt(), 0xFF3A2208.toInt()),
            actionTextColours = intArrayOf(0xFF050403.toInt(), 0xFF050403.toInt(), 0xFFFF9A1F.toInt(), 0xFFFF9A1F.toInt()),
            squareKeys = true,
            keyStyle = KeyStyle.FRAME,
            shoutLabels = true,
            ambient = AmbientKind.GRID,
            ambientColours = intArrayOf(0x33FF9A1F.toInt()),
            overlay = AmbientKind.CRT,
            overlayColours = intArrayOf(0x55000000.toInt()),
            cursorColour = 0xFFFF9A1F.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * A VHS tape: the blue screen of the video, white on-screen-display lettering, and a band of tracking noise that rolls up
         * the screen with a strip of head noise along the bottom. Keys are faint glass panels outlined in white, and Enter,
         * Delete and the others are the red, green, yellow and white of the recorder's labels.
         */
        val VHS = KeyboardTheme(
            background = 0xFF1A2FC0.toInt(),
            backgroundBottom = 0xFF0C1A7A.toInt(),
            strip = 0xFF0C1A7A.toInt(),
            key = 0x26FFFFFF.toInt(),
            keyPressed = 0x66FFFFFF.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFFFFF.toInt(),
            accentText = 0xFF0C1A7A.toInt(),
            subText = 0xFFB8C4FF.toInt(),
            border = 0x99FFFFFF.toInt(),
            stripText = 0xFFFFFFFF.toInt(),
            actionKeyColours = intArrayOf(0xFFE02020.toInt(), 0xFF20C040.toInt(), 0xFFF0D020.toInt(), 0xFFF0F0F0.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFF102060.toInt(), 0xFF102060.toInt()),
            keyStyle = KeyStyle.DEFAULT,
            candidateFont = CandidateFont.PIXEL,
            ambient = AmbientKind.VHS,
            ambientColours = intArrayOf(0xFFFFFFFF.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Rain on a window at night: a dark blue-grey pane with soft out-of-focus lights glowing behind it, beads of water and
         * a few drops running down in fits and starts. The keys are smoked glass with a pale rim, and each tap ripples.
         */
        val RAIN_ON_WINDOW = KeyboardTheme(
            background = 0xFF141B26.toInt(),
            backgroundBottom = 0xFF0A0F18.toInt(),
            strip = 0x66000000.toInt(),
            key = 0x4010161E.toInt(),
            keyPressed = 0x66C0D8F0.toInt(),
            text = 0xFFEAF2FF.toInt(),
            accent = 0xFF8FB8E8.toInt(),
            accentText = 0xFF141B26.toInt(),
            subText = 0xFF8898B0.toInt(),
            border = 0x55C8DCF0.toInt(),
            stripText = 0xFFEAF2FF.toInt(),
            actionKeyColours = intArrayOf(0xFF5A86B8.toInt(), 0xFFB85A5A.toInt(), 0xFF3A4A60.toInt(), 0xFF3A4A60.toInt()),
            actionTextColours = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFEAF2FF.toInt(), 0xFFEAF2FF.toInt()),
            keyStyle = KeyStyle.DEFAULT,
            ambient = AmbientKind.WINDOW_RAIN,
            ambientColours = intArrayOf(0xFFFFB050.toInt(), 0xFFFF6080.toInt(), 0xFF50C0D0.toInt(), 0xFFFFD878.toInt(), 0xFF8090FF.toInt()),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFFC8DCF0.toInt(), 0xFF8FB8E8.toInt()),
            typeface = Typeface.create("sans-serif", Typeface.NORMAL),
        )

        /**
         * The demoscene on an Amiga: a black screen crossed by copper bars in pink, orange, yellow, green and blue that weave up
         * and down on sine waves, chunky pixel-cut keys, a pixel font, and a burst of pixels where you tap.
         */
        val DEMOSCENE = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF14143A.toInt(),
            key = 0xCC10102A.toInt(),
            keyPressed = 0xFF3A2A7A.toInt(),
            text = 0xFFFFFFFF.toInt(),
            accent = 0xFFFFE040.toInt(),
            accentText = 0xFF000000.toInt(),
            subText = 0xFF40D8FF.toInt(),
            border = 0xFF40D8FF.toInt(),
            shadow = 0xFF14143A.toInt(),
            stripText = 0xFFFFE040.toInt(),
            actionKeyColours = intArrayOf(0xFFFF4090.toInt(), 0xFF40D0FF.toInt(), 0xFFFF9020.toInt(), 0xFF60E060.toInt()),
            actionTextColours = intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt()),
            keyStyle = KeyStyle.PIXEL,
            candidateStyle = CandidateStyle.SQUARE,
            candidateFont = CandidateFont.PIXEL,
            ambient = AmbientKind.COPPER_BARS,
            ambientColours = intArrayOf(0xFFFF2080.toInt(), 0xFFFF7020.toInt(), 0xFFFFE020.toInt(), 0xFF30E070.toInt(), 0xFF20B0FF.toInt(), 0xFF8040FF.toInt()),
            tapEffect = TapEffectKind.PIXELS,
            effectColours = intArrayOf(0xFFFF2080.toInt(), 0xFFFF7020.toInt(), 0xFFFFE020.toInt(), 0xFF30E070.toInt(), 0xFF20B0FF.toInt(), 0xFF8040FF.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Fireflies on a summer night: deep blue-green dusk with small yellow-green lights wandering in loops and glowing softly
         * on and off. The keys are dark translucent glass with a faint green rim, and a tap lets a glowing orb drift up.
         */
        val FIREFLIES = KeyboardTheme(
            background = 0xFF0B1A1F.toInt(),
            backgroundBottom = 0xFF050C0A.toInt(),
            strip = 0x55000000.toInt(),
            key = 0x5510201A.toInt(),
            keyPressed = 0x884A6A2A.toInt(),
            text = 0xFFE6F2D8.toInt(),
            accent = 0xFFD8FF50.toInt(),
            accentText = 0xFF0B1A1F.toInt(),
            subText = 0xFF86A070.toInt(),
            border = 0x445A8A4A.toInt(),
            stripText = 0xFFE6F2D8.toInt(),
            actionKeyColours = intArrayOf(0xFF8AC840.toInt(), 0xFFC86A40.toInt(), 0xFF3A5A30.toInt(), 0xFF3A5A30.toInt()),
            actionTextColours = intArrayOf(0xFF0B1A1F.toInt(), 0xFF0B1A1F.toInt(), 0xFFE6F2D8.toInt(), 0xFFE6F2D8.toInt()),
            keyStyle = KeyStyle.DEFAULT,
            ambient = AmbientKind.FIREFLIES,
            ambientColours = intArrayOf(0xFFD8FF50.toInt(), 0xFFFFE070.toInt()),
            tapEffect = TapEffectKind.ORB,
            effectColours = intArrayOf(0xFFD8FF50.toInt(), 0xFFFFE070.toInt()),
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL),
        )

        /**
         * Nine Inch Nails, The Downward Spiral (1994): the industrial, photocopied world of Russell Mills' artwork. The keys
         * have no body at all: only the sepia scratches and the rust creeping up from each bottom edge are drawn, so the
         * spiral shows right through them, and a pressed key flares Halo red. A spiral turns slowly and breathes behind
         * them, bands of photocopier noise jump across the screen, and every tap leaves a ghost of pixels like a bad
         * copy. Typewriter lettering; Enter is Halo red.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val NINE_INCH_NAILS = KeyboardTheme(
            background = 0xFF12100E.toInt(),
            backgroundBottom = 0xFF3D2E26.toInt(),
            strip = 0xFF1D1815.toInt(),
            key = 0x00000000,
            keyPressed = 0xFFB01C24.toInt(),
            text = 0xFFD4C5B2.toInt(),
            accent = 0xFFB01C24.toInt(),
            accentText = 0xFFD4C5B2.toInt(),
            subText = 0xFF8A755C.toInt(),
            border = 0xFF8A755C.toInt(),
            shadow = 0xFF7A3B2E.toInt(),
            stripText = 0xFFD4C5B2.toInt(),
            actionKeyColours = intArrayOf(0xFFB01C24.toInt(), 0xFF7A3B2E.toInt(), 0xFF8A755C.toInt(), 0xFF3D2E26.toInt()),
            actionTextColours = intArrayOf(0xFFD4C5B2.toInt(), 0xFFD4C5B2.toInt(), 0xFF12100E.toInt(), 0xFFD4C5B2.toInt()),
            keyStyle = KeyStyle.SCRATCHED,
            ambient = AmbientKind.SPIRAL,
            ambientColours = intArrayOf(0x668A755C.toInt(), 0x667A3B2E.toInt(), 0x1AD4C5B2.toInt()),
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFFD4C5B2.toInt(), 0xFFB01C24.toInt(), 0xFF8A755C.toInt()),
            cursorColour = 0xFFB01C24.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * The Prodigy, The Fat of the Land (1997): rave-flyer acid on black. Keys are slanted, hard-edged and outlined in
         * acid chartreuse with a coral shadow, lettered in hazard yellow and flashing coral when pressed. Rows of chevrons
         * stream across the background at different speeds like a strobing flyer, and every tap is a burst of spray paint
         * in one colour, with a wet centre, a mist of flecks and a drip or two. Enter is chartreuse, Delete coral.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val THE_PRODIGY = KeyboardTheme(
            background = 0xFF0B0B0B.toInt(),
            strip = 0xFF171717.toInt(),
            key = 0xFF171717.toInt(),
            keyPressed = 0xFFE64825.toInt(),
            text = 0xFFFFE600.toInt(),
            accent = 0xFFB8E614.toInt(),
            accentText = 0xFF0B0B0B.toInt(),
            subText = 0xFFDCD0B8.toInt(),
            border = 0xFFB8E614.toInt(),
            shadow = 0xFFE64825.toInt(),
            stripText = 0xFFFFE600.toInt(),
            actionKeyColours = intArrayOf(0xFFB8E614.toInt(), 0xFFE64825.toInt(), 0xFFFFE600.toInt(), 0xFF226B70.toInt()),
            actionTextColours = intArrayOf(0xFF0B0B0B.toInt(), 0xFF0B0B0B.toInt(), 0xFF0B0B0B.toInt(), 0xFFDCD0B8.toInt()),
            keyStyle = KeyStyle.SLANTED,
            shoutLabels = true,
            ambient = AmbientKind.CHEVRONS,
            ambientColours = intArrayOf(0x44B8E614.toInt(), 0x3CE64825.toInt(), 0x38FFE600.toInt()),
            tapEffect = TapEffectKind.SPRAY,
            effectColours = intArrayOf(0xFFB8E614.toInt(), 0xFFE64825.toInt(), 0xFFFFE600.toInt(), 0xFFDCD0B8.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Massive Attack, Blue Lines and Mezzanine (1991, 1998): nocturnal Bristol. Keys are etched carapace-steel plates
         * with a brushed sheen and a pale steel edge on a near-black field; a pressed key goes Blue Lines cobalt. Thin
         * cobalt, crimson and steel lines ripple slowly across the whole keyboard, over the keys as well as behind them,
         * like a trip-hop pulse, and every tap sends a ring outward. Enter is Blue Lines crimson, Delete cobalt.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val MASSIVE_ATTACK = KeyboardTheme(
            background = 0xFF0A0C0E.toInt(),
            backgroundBottom = 0xFF14181D.toInt(),
            strip = 0xFF14181D.toInt(),
            key = 0xFF1B1F24.toInt(),
            keyPressed = 0xFF1A5296.toInt(),
            text = 0xFFEFEEEA.toInt(),
            accent = 0xFFD11F2E.toInt(),
            accentText = 0xFFEFEEEA.toInt(),
            subText = 0xFF6C757D.toInt(),
            border = 0xFF6C757D.toInt(),
            actionKeyColours = intArrayOf(0xFFD11F2E.toInt(), 0xFF1A5296.toInt(), 0xFF6C757D.toInt(), 0xFF32373D.toInt()),
            actionTextColours = intArrayOf(0xFFEFEEEA.toInt(), 0xFFEFEEEA.toInt(), 0xFF0A0C0E.toInt(), 0xFFEFEEEA.toInt()),
            keyStyle = KeyStyle.ETCHED,
            overlay = AmbientKind.WAVES,
            overlayColours = intArrayOf(0x881A5296.toInt(), 0x78D11F2E.toInt(), 0x686C757D.toInt()),
            tapEffect = TapEffectKind.RIPPLES,
            effectColours = intArrayOf(0xFF1A5296.toInt(), 0xFFD11F2E.toInt(), 0xFFEFEEEA.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Blur, Parklife (1994): Walthamstow Stadium on a bright day. Keys are cream betting slips with a half-round notch
         * either side, a crimson stripe and a dotted perforation, lettered in navy; a pressed key turns bumper gold. Mown
         * turf with a white rail lies behind the keys under a Walthamstow-blue sky, and four greyhounds in coloured
         * jackets race across it over the keys, where they can be seen. Every tap throws confetti. Enter is jacket crimson.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val BLUR = KeyboardTheme(
            background = 0xFF4CA2D8.toInt(),
            strip = 0xFF1A2B40.toInt(),
            key = 0xE6F7F0E1.toInt(),
            keyPressed = 0xF2F4C41D.toInt(),
            text = 0xFF1A2B40.toInt(),
            accent = 0xFFD12328.toInt(),
            accentText = 0xFFF7F0E1.toInt(),
            subText = 0xFF2F7C4E.toInt(),
            border = 0xFFD12328.toInt(),
            stripText = 0xFFF7F0E1.toInt(),
            actionKeyColours = intArrayOf(0xFFD12328.toInt(), 0xFFF4C41D.toInt(), 0xFF2F7C4E.toInt(), 0xFF1A2B40.toInt()),
            actionTextColours = intArrayOf(0xFFF7F0E1.toInt(), 0xFF1A2B40.toInt(), 0xFFF7F0E1.toInt(), 0xFFF7F0E1.toInt()),
            keyStyle = KeyStyle.TICKET,
            shoutLabels = true,
            ambient = AmbientKind.TURF,
            ambientColours = intArrayOf(0xFF2F7C4E.toInt(), 0xFF378A57.toInt(), 0xFFF7F0E1.toInt()),
            overlay = AmbientKind.DOGS,
            overlayColours = intArrayOf(0xF0594F46.toInt(), 0xF0D12328.toInt(), 0xF02B5FA8.toInt(), 0xF0F7F0E1.toInt(), 0xF0F4C41D.toInt()),
            tapEffect = TapEffectKind.CONFETTI,
            effectColours = intArrayOf(0xFFD12328.toInt(), 0xFFF4C41D.toInt(), 0xFF2F7C4E.toInt(), 0xFFF7F0E1.toInt(), 0xFF1A2B40.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Daft Punk, Homework (1997): the embroidered tour jacket. Keys are slate twill patches with a raised gold merrow
         * edge and a line of running stitches, lettered in label white; a pressed key goes embroidered crimson. A row of
         * crimson sound bars lies flat behind them until you press a key, then rings like a kick drum. Enter is crimson,
         * Delete gold.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val DAFT_PUNK_HOMEWORK = KeyboardTheme(
            background = 0xFF0D0D0F.toInt(),
            strip = 0xFF292B2E.toInt(),
            key = 0xFF292B2E.toInt(),
            keyPressed = 0xFFD91E26.toInt(),
            text = 0xFFF4F4F2.toInt(),
            accent = 0xFFD91E26.toInt(),
            accentText = 0xFFF4F4F2.toInt(),
            subText = 0xFF989DA3.toInt(),
            border = 0xFFE6B532.toInt(),
            stripText = 0xFFF4F4F2.toInt(),
            actionKeyColours = intArrayOf(0xFFD91E26.toInt(), 0xFFE6B532.toInt(), 0xFF989DA3.toInt(), 0xFF292B2E.toInt()),
            actionTextColours = intArrayOf(0xFFF4F4F2.toInt(), 0xFF0D0D0F.toInt(), 0xFF0D0D0F.toInt(), 0xFFF4F4F2.toInt()),
            keyStyle = KeyStyle.STITCH,
            shoutLabels = true,
            ambient = AmbientKind.SOUND,
            ambientColours = intArrayOf(0xCCD91E26.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * Daft Punk, Discovery (2001): the chrome helmets. Keys are liquid-mercury plates with a dark horizon, a shine and a
         * magenta LED strip along the bottom that wraps the whole key when pressed, over a visor-black field. A spectrum
         * of magenta, cyan and gold leaps behind them when you type, and every tap throws LED sparkles. Enter is LED
         * magenta, Delete LED cyan.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val DAFT_PUNK_DISCOVERY = KeyboardTheme(
            background = 0xFF101217.toInt(),
            backgroundBottom = 0xFF1B2030.toInt(),
            strip = 0xFFCCD4DC.toInt(),
            key = 0xFFCCD4DC.toInt(),
            keyPressed = 0xFFF8F9FA.toInt(),
            text = 0xFF101217.toInt(),
            accent = 0xFFE62E8B.toInt(),
            accentText = 0xFFF8F9FA.toInt(),
            subText = 0xFF4F5766.toInt(),
            stripText = 0xFF101217.toInt(),
            actionKeyColours = intArrayOf(0xFFE62E8B.toInt(), 0xFF00AEEF.toInt(), 0xFFFFD100.toInt(), 0xFFF8F9FA.toInt()),
            actionTextColours = intArrayOf(0xFFF8F9FA.toInt(), 0xFF101217.toInt(), 0xFF101217.toInt(), 0xFF101217.toInt()),
            keyStyle = KeyStyle.CHROME,
            ambient = AmbientKind.EQUALISER,
            ambientColours = intArrayOf(0xCCE62E8B.toInt(), 0xCC00AEEF.toInt(), 0xCCFFD100.toInt()),
            tapEffect = TapEffectKind.SPARKLE,
            effectColours = intArrayOf(0xFFE62E8B.toInt(), 0xFF00AEEF.toInt(), 0xFFFFD100.toInt(), 0xFFF8F9FA.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD),
        )

        /**
         * Daft Punk, Human After All (2005): a dead CRT and a red visor. Square scanlined keys in raster charcoal on a
         * void-black screen, lettered in broadcast white with a static-silver edge; a pressed key flashes visor red. A
         * CRT rolls scanlines and flicker behind them with a blinking red indicator. Monospace, cold and stark; Enter is
         * visor red.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val DAFT_PUNK_HUMAN_AFTER_ALL = KeyboardTheme(
            background = 0xFF070709.toInt(),
            strip = 0xFF1C1E22.toInt(),
            key = 0xFF1C1E22.toInt(),
            keyPressed = 0xFFDD202A.toInt(),
            text = 0xFFF2F3F5.toInt(),
            accent = 0xFFDD202A.toInt(),
            accentText = 0xFFF2F3F5.toInt(),
            subText = 0xFF74787E.toInt(),
            border = 0xFF74787E.toInt(),
            stripText = 0xFFF2F3F5.toInt(),
            actionKeyColours = intArrayOf(0xFFDD202A.toInt(), 0xFFC2C6CA.toInt(), 0xFF1C1E22.toInt(), 0xFF74787E.toInt()),
            actionTextColours = intArrayOf(0xFFF2F3F5.toInt(), 0xFF070709.toInt(), 0xFFF2F3F5.toInt(), 0xFF070709.toInt()),
            keyStyle = KeyStyle.SCANLINE,
            shoutLabels = true,
            squareKeys = true,
            ambient = AmbientKind.CRT,
            ambientColours = intArrayOf(0x22C2C6CA.toInt(), 0x33F2F3F5.toInt(), 0xFFDD202A.toInt()),
            cursorColour = 0xFFDD202A.toInt(),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Justice, Cross (2007): the monolith. Solid black plates with a burnished-brass edge and warm-gold lettering on pure
         * black; a pressed key burns brass. The cross of the album cover, a block cross seen from above and picked out
         * in a spray-painted gold outline with fine edge lines, stands over the keyboard, breathing and glittering
         * a little, and every tap blooms a gold orb. Heavy serif lettering; Enter is Cross gold.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val JUSTICE = KeyboardTheme(
            background = 0xFF000000.toInt(),
            strip = 0xFF050505.toInt(),
            key = 0xFF000000.toInt(),
            keyPressed = 0xFFB88422.toInt(),
            text = 0xFFF5D478.toInt(),
            accent = 0xFFE5B23A.toInt(),
            accentText = 0xFF0A0A0B.toInt(),
            subText = 0xFFB88422.toInt(),
            border = 0xFFB88422.toInt(),
            stripText = 0xFFF5D478.toInt(),
            actionKeyColours = intArrayOf(0xFFE5B23A.toInt(), 0xFFB88422.toInt(), 0xFFFAF6EB.toInt(), 0xFF242426.toInt()),
            actionTextColours = intArrayOf(0xFF0A0A0B.toInt(), 0xFF0A0A0B.toInt(), 0xFF0A0A0B.toInt(), 0xFFF5D478.toInt()),
            keyStyle = KeyStyle.ETCHED,
            flatKeys = true,
            overlay = AmbientKind.CROSS,
            overlayColours = intArrayOf(0xE6E5B23A.toInt(), 0x16F5D478.toInt(), 0xA0F5D478.toInt()),
            tapEffect = TapEffectKind.ORB,
            effectColours = intArrayOf(0xFFF5D478.toInt(), 0xFFFAF6EB.toInt(), 0xFFE5B23A.toInt()),
            typeface = Typeface.create("serif", Typeface.BOLD),
        )

        /**
         * Gorillaz, Gorillaz and Demon Days (2001, 2005): the cartoon band. Every key is a newsprint speech bubble with a heavy
         * ink outline, a tail at the lower left and halftone shading, on Geep camo olive speckle; a pressed key fills 2D
         * sky blue. Enter is Demon Days crimson and Delete Murdoc teal. Every tap bursts into a comic-book POW, and the
         * pulse behind the keys as you type is Demon Days dark green. Comic lettering.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val GORILLAZ = KeyboardTheme(
            background = 0xFF5D6C3A.toInt(),
            strip = 0xFF111316.toInt(),
            key = 0xFFEEE6D3.toInt(),
            keyPressed = 0xFF579EC8.toInt(),
            text = 0xFF111316.toInt(),
            accent = 0xFFB8242A.toInt(),
            accentText = 0xFFEEE6D3.toInt(),
            subText = 0xFF2A5A6B.toInt(),
            border = 0xFF111316.toInt(),
            stripText = 0xFFEEE6D3.toInt(),
            actionKeyColours = intArrayOf(0xFFB8242A.toInt(), 0xFF2A5A6B.toInt(), 0xFF579EC8.toInt(), 0xFF111316.toInt()),
            actionTextColours = intArrayOf(0xFFEEE6D3.toInt(), 0xFFEEE6D3.toInt(), 0xFF111316.toInt(), 0xFFEEE6D3.toInt()),
            keyStyle = KeyStyle.SPEECH,
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0xFF5D6C3A.toInt(), 0xFF6B7B45.toInt(), 0xFF4A5830.toInt()),
            pulseColour = 0xFF1E5A2A.toInt(),
            tapEffect = TapEffectKind.STARBURST,
            effectColours = intArrayOf(0xFFF5C518.toInt(), 0xFFEEE6D3.toInt(), 0xFFB8242A.toInt(), 0xFF579EC8.toInt()),
            typeface = Typeface.create("casual", Typeface.BOLD),
        )

        /**
         * The White Stripes, Elephant (2003): red, white and black, nothing else. Squared white keys with a black outline and
         * heavy lettering, translucent so the peppermint behind them shows through; a pressed key is peppermint red. The
         * peppermint candy fills the whole keyboard and turns slowly behind it, with no outer ring in view. Enter is red,
         * Delete black, and the cursor is red.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val THE_WHITE_STRIPES = KeyboardTheme(
            background = 0xFF101010.toInt(),
            strip = 0xFF101010.toInt(),
            key = 0xD0F8F7F4.toInt(),
            keyPressed = 0xE0D61A21.toInt(),
            text = 0xFF101010.toInt(),
            accent = 0xFFD61A21.toInt(),
            accentText = 0xFFF8F7F4.toInt(),
            subText = 0xFF7C1115.toInt(),
            border = 0xFF101010.toInt(),
            stripText = 0xFFF8F7F4.toInt(),
            actionKeyColours = intArrayOf(0xFFD61A21.toInt(), 0xFF101010.toInt(), 0xFF7C1115.toInt(), 0xFF2B2B2D.toInt()),
            actionTextColours = intArrayOf(0xFFF8F7F4.toInt(), 0xFFF8F7F4.toInt(), 0xFFF8F7F4.toInt(), 0xFFF8F7F4.toInt()),
            keyStyle = KeyStyle.DEFAULT,
            squareKeys = true,
            shoutLabels = true,
            ambient = AmbientKind.PEPPERMINT,
            ambientColours = intArrayOf(0xFFD61A21.toInt(), 0xFFF8F7F4.toInt(), 0xFF101010.toInt()),
            cursorColour = 0xFFD61A21.toInt(),
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD),
        )

        /**
         * The Strokes, Is This It (2001): the bubble-chamber sleeve. Dark leather slab keys with a slate edge and cream
         * lettering; a pressed key goes CERN cobalt. Thin gold, cobalt and cream particle tracks curl and fade across the
         * keyboard, over the keys, as they would in the photograph, and every tap sends a few tracks spiralling out of
         * it. Enter is Bubble Chamber gold.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val THE_STROKES = KeyboardTheme(
            background = 0xFF161618.toInt(),
            strip = 0xFF23252A.toInt(),
            key = 0xD023252A.toInt(),
            keyPressed = 0xE61A4C9E.toInt(),
            text = 0xFFEFE7D7.toInt(),
            accent = 0xFFF5B800.toInt(),
            accentText = 0xFF161618.toInt(),
            subText = 0xFFC69B7B.toInt(),
            border = 0xFF4C545F.toInt(),
            stripText = 0xFFEFE7D7.toInt(),
            actionKeyColours = intArrayOf(0xFFF5B800.toInt(), 0xFF1A4C9E.toInt(), 0xFFC69B7B.toInt(), 0xFF4C545F.toInt()),
            actionTextColours = intArrayOf(0xFF161618.toInt(), 0xFFEFE7D7.toInt(), 0xFF161618.toInt(), 0xFFEFE7D7.toInt()),
            keyStyle = KeyStyle.SLAB,
            overlay = AmbientKind.TRACKS,
            overlayColours = intArrayOf(0xB0F5B800.toInt(), 0xB04C7BD6.toInt(), 0x90EFE7D7.toInt()),
            tapEffect = TapEffectKind.TRACK,
            effectColours = intArrayOf(0xFFEFE7D7.toInt(), 0xFFF5B800.toInt(), 0xFF1A4C9E.toInt()),
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )

        /**
         * Evanescence, Fallen (2003): a cathedral at night. Tall gothic-arch keys with a line of tracery inside, in
         * Fallen slate on midnight, outlined in porcelain cyan and lettered in pale ivory; a pressed key glows. Behind
         * them stands the cathedral itself: a pointed arch round a rose window, a moon that pulses, shafts of light and
         * mist along the floor. Pale feathers drift down over the keys, and every tap lets fall one more, from the tap itself.
         * Enter is mist blue.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val EVANESCENCE = KeyboardTheme(
            background = 0xFF0C1724.toInt(),
            backgroundBottom = 0xFF06090E.toInt(),
            strip = 0xFF06090E.toInt(),
            key = 0xFF253748.toInt(),
            keyPressed = 0xFF587C9A.toInt(),
            text = 0xFFEFF3F6.toInt(),
            accent = 0xFFB5CCDA.toInt(),
            accentText = 0xFF06090E.toInt(),
            subText = 0xFFB5CCDA.toInt(),
            border = 0xFF587C9A.toInt(),
            stripText = 0xFFEFF3F6.toInt(),
            actionKeyColours = intArrayOf(0xFFB5CCDA.toInt(), 0xFF587C9A.toInt(), 0xFF253748.toInt(), 0xFF06090E.toInt()),
            actionTextColours = intArrayOf(0xFF06090E.toInt(), 0xFFEFF3F6.toInt(), 0xFFEFF3F6.toInt(), 0xFFB5CCDA.toInt()),
            keyStyle = KeyStyle.GOTHIC,
            ambient = AmbientKind.CATHEDRAL,
            ambientColours = intArrayOf(0x66587C9A.toInt(), 0x88EFF3F6.toInt(), 0x26B5CCDA.toInt(), 0x30B5CCDA.toInt()),
            overlay = AmbientKind.FEATHERS,
            overlayColours = intArrayOf(0xA0EFF3F6.toInt(), 0x90B5CCDA.toInt(), 0x80EFF3F6.toInt()),
            tapEffect = TapEffectKind.FEATHER,
            effectColours = intArrayOf(0xFFEFF3F6.toInt(), 0xFFB5CCDA.toInt(), 0xFFD9E4EC.toInt()),
            typeface = Typeface.create("serif", Typeface.NORMAL),
        )

        /**
         * Aphex Twin, Selected Ambient Works Volume II (1994): restrained and unsettling. Plain peat-brown keys in aged
         * parchment lettering on a grainy bark-black field with lichen sub-labels; a pressed key goes moss slate. Nothing
         * moves but a slow ghost of pixels where you tap. Light lettering; Enter is lichen.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val APHEX_TWIN = KeyboardTheme(
            background = 0xFF211F1C.toInt(),
            strip = 0xFF2D2A25.toInt(),
            key = 0xFF494239.toInt(),
            keyPressed = 0xFF6C6E65.toInt(),
            text = 0xFFDECED6.toInt(),
            accent = 0xFFC3B8A2.toInt(),
            accentText = 0xFF211F1C.toInt(),
            subText = 0xFFC3B8A2.toInt(),
            stripText = 0xFFDECED6.toInt(),
            actionKeyColours = intArrayOf(0xFFC3B8A2.toInt(), 0xFF8D6D51.toInt(), 0xFF6C6E65.toInt(), 0xFF494239.toInt()),
            actionTextColours = intArrayOf(0xFF211F1C.toInt(), 0xFFDECED6.toInt(), 0xFFDECED6.toInt(), 0xFFDECED6.toInt()),
            ambient = AmbientKind.TEXTURE,
            ambientColours = intArrayOf(0xFF211F1C.toInt(), 0xFF2D2A25.toInt(), 0xFF171512.toInt()),
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFFC3B8A2.toInt(), 0xFF8D6D51.toInt()),
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL),
        )

        /**
         * MTV (1981 onwards): the channel idents. White keys with a hard black outline and an offset magenta shadow
         * under a broadcast-yellow strip; a pressed key flips yellow. The whole background floods with one colour and
         * then the next, yellow, magenta, cyan and red, holding each for a moment and blending at an even rate, and
         * every tap bursts into confetti. Enter is hot magenta, Delete electric cyan.
         * Palette from the theme-ideas sheet; the look is drawn from the channel's colours alone, with no logo.
         */
        val MTV = KeyboardTheme(
            background = 0xFF101010.toInt(),
            strip = 0xFFFFE600.toInt(),
            key = 0xFFF8F8F6.toInt(),
            keyPressed = 0xFFFFE600.toInt(),
            text = 0xFF101010.toInt(),
            accent = 0xFFE6007E.toInt(),
            accentText = 0xFFF8F8F6.toInt(),
            subText = 0xFFE6007E.toInt(),
            border = 0xFF101010.toInt(),
            shadow = 0xFFE6007E.toInt(),
            stripText = 0xFF101010.toInt(),
            actionKeyColours = intArrayOf(0xFFE6007E.toInt(), 0xFF00AEEF.toInt(), 0xFFFFE600.toInt(), 0xFFED1C24.toInt()),
            actionTextColours = intArrayOf(0xFFF8F8F6.toInt(), 0xFF101010.toInt(), 0xFF101010.toInt(), 0xFFF8F8F6.toInt()),
            keyStyle = KeyStyle.MEMPHIS,
            shoutLabels = true,
            ambient = AmbientKind.COLOUR_CYCLE,
            ambientColours = intArrayOf(0xFFFFE600.toInt(), 0xFFE6007E.toInt(), 0xFF00AEEF.toInt(), 0xFFED1C24.toInt()),
            tapEffect = TapEffectKind.CONFETTI,
            effectColours = intArrayOf(0xFFFFE600.toInt(), 0xFFE6007E.toInt(), 0xFF00AEEF.toInt(), 0xFFED1C24.toInt(), 0xFFF8F8F6.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        /**
         * MTV2 (late 1990s): the underground channel. Graphite keys whose outline has split into chartreuse, cyan and amber
         * ghosts, lettered in crisp white; a pressed key goes signal cyan. Lines of scrolling text run past behind them in
         * the same three colours and every tap leaves a ghost of pixels like bad reception. Monospace; Enter is volt
         * chartreuse, Delete alert amber.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val MTV2 = KeyboardTheme(
            background = 0xFF0A0B0D.toInt(),
            strip = 0xFF282B30.toInt(),
            key = 0xFF282B30.toInt(),
            keyPressed = 0xFF0099CC.toInt(),
            text = 0xFFF3F3F1.toInt(),
            accent = 0xFF0099CC.toInt(),
            accentText = 0xFFF3F3F1.toInt(),
            subText = 0xFFF59E0B.toInt(),
            border = 0xFFA8E61D.toInt(),
            stripText = 0xFFF3F3F1.toInt(),
            actionKeyColours = intArrayOf(0xFFA8E61D.toInt(), 0xFFF59E0B.toInt(), 0xFF0099CC.toInt(), 0xFF282B30.toInt()),
            actionTextColours = intArrayOf(0xFF0A0B0D.toInt(), 0xFF0A0B0D.toInt(), 0xFFF3F3F1.toInt(), 0xFFF3F3F1.toInt()),
            keyStyle = KeyStyle.GLITCH,
            shoutLabels = true,
            ambient = AmbientKind.SCROLL,
            ambientColours = intArrayOf(0x55A8E61D.toInt(), 0x4A0099CC.toInt(), 0x40F59E0B.toInt()),
            tapEffect = TapEffectKind.GHOST,
            effectColours = intArrayOf(0xFFA8E61D.toInt(), 0xFF0099CC.toInt()),
            typeface = Typeface.create("monospace", Typeface.BOLD),
        )

        /**
         * Kerrang! (1981 onwards): the magazine. Dark newsprint slab keys with a masthead-crimson bar along the bottom,
         * lettered in shatter white under a crimson masthead strip; a pressed key turns crimson. A halftone screen of
         * crimson dots builds up from the top left, and every tap blows glass and debris. Enter is crimson.
         * Palette from the theme-ideas sheet; the look is drawn from the artwork and identity itself.
         */
        val KERRANG = KeyboardTheme(
            background = 0xFF111111.toInt(),
            strip = 0xFFD91E2A.toInt(),
            key = 0xFF2B2B2D.toInt(),
            keyPressed = 0xFFD91E2A.toInt(),
            text = 0xFFF5F4F0.toInt(),
            accent = 0xFFD91E2A.toInt(),
            accentText = 0xFFF5F4F0.toInt(),
            subText = 0xFF8A8A88.toInt(),
            border = 0xFFF5F4F0.toInt(),
            stripText = 0xFFF5F4F0.toInt(),
            actionKeyColours = intArrayOf(0xFFD91E2A.toInt(), 0xFFF5F4F0.toInt(), 0xFF8A8A88.toInt(), 0xFFD8D4CA.toInt()),
            actionTextColours = intArrayOf(0xFFF5F4F0.toInt(), 0xFF111111.toInt(), 0xFF111111.toInt(), 0xFF111111.toInt()),
            keyStyle = KeyStyle.SLAB,
            shoutLabels = true,
            ambient = AmbientKind.HALFTONE,
            ambientColours = intArrayOf(0x80D91E2A.toInt()),
            tapEffect = TapEffectKind.BLAST,
            effectColours = intArrayOf(0xFFF5F4F0.toInt(), 0xFFD91E2A.toInt(), 0xFF8A8A88.toInt(), 0xFFD8D4CA.toInt(), 0xFF2B2B2D.toInt()),
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL),
        )

        private fun heavyItalic(): Typeface =
            if (Build.VERSION.SDK_INT >= 28) {
                Typeface.create(Typeface.SANS_SERIF, 900, true)
            } else {
                Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
            }

        /** The theme for the [setting] (see [KeyboardSettings]); [p5Accent] only matters for the P5 theme. */
        fun resolve(context: Context, setting: Int, p5Accent: Int = 0, custom: CustomParts = CustomParts()): KeyboardTheme {
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
                KeyboardSettings.THEME_XP -> XP
                KeyboardSettings.THEME_MEDIA_PLAYER -> MEDIA_PLAYER
                KeyboardSettings.THEME_BIOS -> BIOS
                KeyboardSettings.THEME_GEOCITIES -> GEOCITIES
                KeyboardSettings.THEME_PALM -> PALM
                KeyboardSettings.THEME_WALKMAN -> WALKMAN
                KeyboardSettings.THEME_VIRTUAL_BOY -> VIRTUAL_BOY
                KeyboardSettings.THEME_PSP -> PSP
                KeyboardSettings.THEME_MATRIX -> MATRIX
                KeyboardSettings.THEME_SATURDAY -> SATURDAY
                KeyboardSettings.THEME_NOKIA_3210 -> NOKIA_3210
                KeyboardSettings.THEME_NOKIA_3510I -> NOKIA_3510I
                KeyboardSettings.THEME_NOKIA_7210 -> NOKIA_7210
                KeyboardSettings.THEME_SONY_ERICSSON_T610 -> SONY_ERICSSON_T610
                KeyboardSettings.THEME_NGAGE -> NGAGE
                KeyboardSettings.THEME_RAZR_V3 -> RAZR_V3
                KeyboardSettings.THEME_KAWAII -> KAWAII
                KeyboardSettings.THEME_EVANGELION_00 -> EVANGELION_00
                KeyboardSettings.THEME_MONSTERGEAR -> MONSTERGEAR
                KeyboardSettings.THEME_EVANGELION_01 -> EVANGELION_01
                KeyboardSettings.THEME_EVANGELION_02 -> EVANGELION_02
                KeyboardSettings.THEME_MONSTERNAV -> MONSTERNAV
                KeyboardSettings.THEME_GHOST_IN_THE_SHELL -> GHOST_IN_THE_SHELL
                KeyboardSettings.THEME_LCARS -> LCARS
                KeyboardSettings.THEME_HACKERS -> HACKERS
                KeyboardSettings.THEME_LAIN -> LAIN
                KeyboardSettings.THEME_WINDOWS_95 -> WINDOWS_95
                KeyboardSettings.THEME_WINAMP -> WINAMP
                KeyboardSettings.THEME_NETSCAPE -> NETSCAPE
                KeyboardSettings.THEME_WIPEOUT_2097 -> WIPEOUT_2097
                KeyboardSettings.THEME_WIPEOUT_HD -> WIPEOUT_HD
                KeyboardSettings.THEME_PAGER -> PAGER
                KeyboardSettings.THEME_SUPER_MARIO_BROS -> SUPER_MARIO_BROS
                KeyboardSettings.THEME_SONIC_THE_HEDGEHOG -> SONIC_THE_HEDGEHOG
                KeyboardSettings.THEME_DOOM -> DOOM
                KeyboardSettings.THEME_FINAL_FANTASY_VII -> FINAL_FANTASY_VII
                KeyboardSettings.THEME_WORMS_ARMAGEDDON -> WORMS_ARMAGEDDON
                KeyboardSettings.THEME_THE_SIMS -> THE_SIMS
                KeyboardSettings.THEME_GTA_VICE_CITY -> GTA_VICE_CITY
                KeyboardSettings.THEME_GTA_SAN_ANDREAS -> GTA_SAN_ANDREAS
                KeyboardSettings.THEME_PIP_BOY -> PIP_BOY
                KeyboardSettings.THEME_PIP_BOY_AMBER -> PIP_BOY_AMBER
                KeyboardSettings.THEME_GEN_1 -> GEN_1
                KeyboardSettings.THEME_GEN_2 -> GEN_2
                KeyboardSettings.THEME_GEN_3 -> GEN_3
                KeyboardSettings.THEME_BLACKBERRY -> BLACKBERRY
                KeyboardSettings.THEME_TAMAGOTCHI -> TAMAGOTCHI
                KeyboardSettings.THEME_GAME_AND_WATCH -> GAME_AND_WATCH
                KeyboardSettings.THEME_CASIO_CALCULATOR -> CASIO_CALCULATOR
                KeyboardSettings.THEME_MEGA_DRIVE -> MEGA_DRIVE
                KeyboardSettings.THEME_ZX_SPECTRUM -> ZX_SPECTRUM
                KeyboardSettings.THEME_COMMODORE_64 -> COMMODORE_64
                KeyboardSettings.THEME_TELETEXT -> TELETEXT
                KeyboardSettings.THEME_MSN_MESSENGER -> MSN_MESSENGER
                KeyboardSettings.THEME_TETRIS -> TETRIS
                KeyboardSettings.THEME_PAC_MAN -> PAC_MAN
                KeyboardSettings.THEME_CODEC_CALL -> CODEC_CALL
                KeyboardSettings.THEME_HEV_SUIT -> HEV_SUIT
                KeyboardSettings.THEME_MINECRAFT -> MINECRAFT
                KeyboardSettings.THEME_JOY_DIVISION -> JOY_DIVISION
                KeyboardSettings.THEME_NOSTROMO -> NOSTROMO
                KeyboardSettings.THEME_VHS -> VHS
                KeyboardSettings.THEME_RAIN_ON_WINDOW -> RAIN_ON_WINDOW
                KeyboardSettings.THEME_DEMOSCENE -> DEMOSCENE
                KeyboardSettings.THEME_FIREFLIES -> FIREFLIES
                KeyboardSettings.THEME_NINE_INCH_NAILS -> NINE_INCH_NAILS
                KeyboardSettings.THEME_THE_PRODIGY -> THE_PRODIGY
                KeyboardSettings.THEME_MASSIVE_ATTACK -> MASSIVE_ATTACK
                KeyboardSettings.THEME_BLUR -> BLUR
                KeyboardSettings.THEME_DAFT_PUNK_HOMEWORK -> DAFT_PUNK_HOMEWORK
                KeyboardSettings.THEME_DAFT_PUNK_DISCOVERY -> DAFT_PUNK_DISCOVERY
                KeyboardSettings.THEME_DAFT_PUNK_HUMAN_AFTER_ALL -> DAFT_PUNK_HUMAN_AFTER_ALL
                KeyboardSettings.THEME_JUSTICE -> JUSTICE
                KeyboardSettings.THEME_GORILLAZ -> GORILLAZ
                KeyboardSettings.THEME_THE_WHITE_STRIPES -> THE_WHITE_STRIPES
                KeyboardSettings.THEME_THE_STROKES -> THE_STROKES
                KeyboardSettings.THEME_EVANESCENCE -> EVANESCENCE
                KeyboardSettings.THEME_APHEX_TWIN -> APHEX_TWIN
                KeyboardSettings.THEME_MTV -> MTV
                KeyboardSettings.THEME_MTV2 -> MTV2
                KeyboardSettings.THEME_KERRANG -> KERRANG
                KeyboardSettings.THEME_DARK -> DARK
                KeyboardSettings.THEME_LIGHT -> LIGHT
                KeyboardSettings.THEME_CUSTOM -> {
                    // Each part comes from its own theme; the sources are themselves ordinary themes (never the custom mix).
                    fun source(id: Int): KeyboardTheme =
                        resolve(context, if (id == KeyboardSettings.THEME_CUSTOM) KeyboardSettings.THEME_DARK else id, p5Accent)
                    fun part(id: Int): KeyboardTheme? = if (id == CustomParts.NONE) null else source(id)
                    ThemeComposer.compose(
                        colours = source(custom.colours),
                        keys = part(custom.keys),
                        background = part(custom.background),
                        animation = part(custom.animation),
                        font = part(custom.font),
                    )
                }
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