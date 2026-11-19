// app/src/main/kotlin/com/m8/keyboard/ThemeParts.kt
package com.m8.keyboard

/*
 * The parts a theme is built from, apart from its colours and its key style ([KeyStyle]) and animations
 * ([AmbientKind], [TapEffectKind]): each one is chosen on its own, so a theme can combine any of them.
 * See docs/THEMES.md for how to add a theme or a new value here.
 */

/** What is drawn behind the keys (a gradient from [KeyboardTheme.backgroundBottom] takes over when a theme has one). */
enum class Backdrop {
    /** The plain background colour. */
    FLAT,

    /** The Glitter Graphics pink pinstripe with its little hearts. */
    HEARTS,

    /** Dark pinstripes with the accent colour slashing across them: the P5 backdrop. */
    STRIPES,
}

/** What is drawn behind the candidate words on the strip. */
enum class StripStyle {
    /** The strip colour. */
    FLAT,

    /** The same pinstripe-and-hearts pattern as [Backdrop.HEARTS], in the strip colour. */
    PATTERN,

    /** A pixelated handheld screen (the Game Boy Advance). */
    PIXEL_SCREEN,

    /** A blue gradient window with a silver line along its top and bottom, like a Final Fantasy VII menu (uses [KeyboardTheme.keyPressed] as its bright end). */
    WINDOW,
}

/** The text view the candidate words are shown in. */
enum class CandidateFont {
    /** An ordinary text view in the theme's typeface. */
    NORMAL,

    /** Words drawn in the big green-screen pixels of the original Game Boy. */
    PIXEL,
}

/** The highlight drawn behind the selected candidate word. */
enum class CandidateStyle {
    /** A softly rounded block in the accent colour. */
    ROUNDED,

    /** A square-cornered block in the accent colour. */
    SQUARE,

    /** A square-cornered block with an outline in the theme's border colour. */
    OUTLINED_SQUARE,

    /** A square block with a white outline, to go with the pixel keys. */
    PIXEL,

    /** A small-cornered block, like a highlighted line on an old phone screen. */
    NOKIA,

    /** A slanted block with an outline and shadow, to go with the slanted keys. */
    SLANTED,

    /** A raised bevelled button (the grey desktop themes). */
    BEVEL,

    /** A flat tab with a rounded left end. */
    PILL,

    /** An angular plate with two corners cut off. */
    CHAMFER,
}
