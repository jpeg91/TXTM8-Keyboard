// app/src/main/kotlin/com/m8/keyboard/ThemeComposer.kt
package com.m8.keyboard

import android.graphics.Color
import android.graphics.Typeface

/**
 * Builds the "Custom mix" theme by taking each part of the look from a different theme:
 *
 *  - colours: every colour of the keys, strip, text and action keys (and the Game Boy Advance's bright legends)
 *  - keys: the key style, square corners, the gloss, the Game Boy Advance's pixel colours and the candidate highlight
 *  - background: the backdrop pattern or gradient, and what sits behind the candidate words
 *  - animation: the stars, the tap animation, the ambient animation and the cursor, each keeping its own colours
 *  - font: the typeface, capital-letter labels and the candidate text view
 *
 * A null keys, background, animation or font means the plain version of that part. Some key styles and backdrops
 * need colours the chosen palette never had (a shadow, an outline); when the parts come from different themes those
 * are worked out from the palette. When every part comes from one theme that theme is returned as it is.
 *
 * Each combination is built once and kept, so the keyboard can tell by identity whether the theme has changed.
 */
object ThemeComposer {
    private val cache = HashMap<List<KeyboardTheme?>, KeyboardTheme>()

    /** The key styles that draw an outline and the ones that draw a shadow or edge, in the theme's own colours. */
    private val OUTLINED = setOf(
        KeyStyle.LCD, KeyStyle.MEMPHIS, KeyStyle.CHAMFER, KeyStyle.SLANTED,
        KeyStyle.BRACKET, KeyStyle.RETICLE, KeyStyle.SCANLINE, KeyStyle.FRAME, KeyStyle.GLITCH, KeyStyle.NEON,
        KeyStyle.BACKLIT, KeyStyle.BUBBLE, KeyStyle.UNDERGLOW, KeyStyle.PETAL, KeyStyle.DISH, KeyStyle.PLATE,
        KeyStyle.ETCHED, KeyStyle.MOULDED, KeyStyle.RIVETED, KeyStyle.WINDOW, KeyStyle.PHOSPHOR, KeyStyle.DECO,
        KeyStyle.TERRAIN, KeyStyle.CHECKER, KeyStyle.BLOCK, KeyStyle.SCRATCHED, KeyStyle.STITCH, KeyStyle.TICKET,
        KeyStyle.SPEECH, KeyStyle.GOTHIC, KeyStyle.CLAY, KeyStyle.MENU, KeyStyle.SHELL, KeyStyle.GLOWLINE,
    )
    private val SHADOWED = setOf(
        KeyStyle.LCD, KeyStyle.MEMPHIS, KeyStyle.NOKIA, KeyStyle.PIXEL, KeyStyle.SLANTED, KeyStyle.TERRAIN, KeyStyle.CHECKER,
        KeyStyle.SCRATCHED, KeyStyle.SHELL,
    )

    @Synchronized
    fun compose(
        colours: KeyboardTheme,
        keys: KeyboardTheme?,
        background: KeyboardTheme?,
        animation: KeyboardTheme?,
        font: KeyboardTheme?,
    ): KeyboardTheme {
        if (colours === keys && colours === background && colours === animation && colours === font) return colours
        return cache.getOrPut(listOf(colours, keys, background, animation, font)) {
            build(colours, keys, background, animation, font)
        }
    }

    private fun build(
        c: KeyboardTheme,
        keys: KeyboardTheme?,
        background: KeyboardTheme?,
        animation: KeyboardTheme?,
        font: KeyboardTheme?,
    ): KeyboardTheme {
        val keyStyle = keys?.keyStyle ?: KeyStyle.DEFAULT
        val backdrop = background?.backdrop ?: Backdrop.FLAT

        // Colours the chosen key style or backdrop needs but the palette may not have; only worked out for a real mix.
        var border = c.border
        var shadow = c.shadow
        if (keys != null && keys !== c) {
            if (keyStyle in OUTLINED && border == 0) border = c.text
            if (keyStyle in SHADOWED && shadow == 0) shadow = darken(c.key, 0.55f)
        }
        // A handheld screen needs a colour for its lit panel.
        if (background?.stripStyle == StripStyle.HANDHELD_SCREEN && background !== c && border == 0) border = c.text
        var deep = c.deep
        if (backdrop == Backdrop.STRIPES && background !== c && deep == 0) deep = darken(c.accent, 0.5f)

        // A gradient belongs to the background part; from another theme it fades from this palette's background.
        val backgroundBottom = when {
            background == null -> 0
            background === c -> c.backgroundBottom
            background.backgroundBottom != 0 -> mix(c.background, c.accent, 0.3f)
            else -> 0
        }

        return KeyboardTheme(
            background = c.background,
            strip = c.strip,
            key = c.key,
            keyPressed = c.keyPressed,
            text = c.text,
            accent = c.accent,
            accentText = c.accentText,
            actionKey = c.actionKey,
            actionKeyPressed = c.actionKeyPressed,
            actionText = c.actionText,
            subText = c.subText,
            border = border,
            shadow = shadow,
            deep = deep,
            pixelBorder = c.pixelBorder,
            pixelColours = keys?.pixelColours,
            stars = animation?.stars ?: false,
            starColours = animation?.starColours,
            backdrop = backdrop,
            stripStyle = background?.stripStyle ?: StripStyle.FLAT,
            candidateFont = font?.candidateFont ?: CandidateFont.NORMAL,
            candidateStyle = keys?.candidateStyle,
            shoutLabels = font?.shoutLabels ?: false,
            rainbowLetters = c.rainbowLetters,
            backgroundBottom = backgroundBottom,
            squareKeys = keys?.squareKeys ?: false,
            tapEffect = animation?.tapEffect,
            effectColours = animation?.effectColours,
            actionKeyColours = c.actionKeyColours,
            actionTextColours = c.actionTextColours,
            keyStyle = keyStyle,
            keyRing = keys?.keyRing ?: 0,
            keyGloss = keys?.keyGloss ?: 90,
            keyCycle = c.keyCycle,
            ambient = animation?.ambient,
            ambientColours = animation?.ambientColours,
            overlay = animation?.overlay,
            overlayColours = animation?.overlayColours,
            pulseColour = c.pulseColour,
            cursorColour = animation?.cursorColour ?: 0,
            keyLabel = c.keyLabel,
            keySubLabel = c.keySubLabel,
            stripText = c.stripText,
            typeface = font?.typeface ?: Typeface.DEFAULT,
        )
    }

    private fun darken(colour: Int, factor: Float): Int = Color.argb(
        0xFF,
        (Color.red(colour) * factor).toInt(),
        (Color.green(colour) * factor).toInt(),
        (Color.blue(colour) * factor).toInt(),
    )

    private fun mix(a: Int, b: Int, t: Float): Int = Color.argb(
        0xFF,
        (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt(),
    )
}
