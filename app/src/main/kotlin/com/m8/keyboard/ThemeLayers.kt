// app/src/main/kotlin/com/m8/keyboard/ThemeLayers.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.ViewGroup

/**
 * Everything a theme draws behind and over a keyboard view, built in one place for all three keyboards.
 *
 * Each animation has a slot of its own, and any combination of them can be on at once:
 *  - behind the keys: the background (with the [AmbientKind] animation on it) and the typing pulse
 *  - over the keys: the twinkling stars, the tap animation and the blinking cursor
 *
 * The typing pulse is a slot of its own too, so it works with every theme, including ones with stars or a tap animation.
 */
class ThemeLayers(host: ViewGroup, theme: KeyboardTheme, settings: KeyboardSettings) {
    private val density = host.resources.displayMetrics.density
    private val tracker = KeyTracker(host)
    private val tapEffect: TapEffectDrawable? = theme.tapEffectDrawable(host, settings)
    private val cursor: KeyCursorDrawable? = theme.cursorDrawable(tracker, density)
    private val stars: GlitterTwinkleDrawable? = if (theme.stars) theme.twinkleDrawable(host, settings) else null

    /** The view's background: the theme's backdrop, with the typing pulse on top. */
    val background: Drawable = PulseBackground(
        theme.backgroundDrawable(host, animated = true, effectAmount = settings.effectAmount),
        theme.accent,
        settings.pulseStrength,
    )

    /** The view's foreground: every slot the theme uses, or null when it uses none. */
    val foreground: Drawable? = listOfNotNull<Drawable>(stars, tapEffect, cursor).let { layers ->
        when (layers.size) {
            0 -> null
            1 -> layers[0]
            else -> EffectStack(layers)
        }
    }

    /** A finger touched down at [x], [y]: remember the key, move the cursor and start the tap animation. */
    fun touchDown(x: Float, y: Float) {
        // Remember which key this is: the typing pulse, the sound wave and the cursor all start from it.
        tracker.down(x, y, 3f * density)
        cursor?.moved()
        tapEffect?.spawn(x, y)
    }

    /** Flashes the background in the accent colour from the last key touched (a character was typed). */
    fun pulse() = (background as KeyboardPulseEffect).pulse(tracker.current())
}

/**
 * Several foreground animations drawn one over the other as a single drawable. Each keeps running by asking this
 * one for its next frame, as [PulseBackground] does for an animated background.
 */
internal class EffectStack(private val layers: List<Drawable>) : Drawable() {
    private val forward = ForwardCallback(this)

    init {
        for (layer in layers) layer.callback = forward
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        for (layer in layers) layer.setVisible(visible, restart)
        return super.setVisible(visible, restart)
    }

    override fun onBoundsChange(bounds: Rect) {
        for (layer in layers) layer.bounds = bounds
    }

    override fun draw(canvas: Canvas) {
        for (layer in layers) layer.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        for (layer in layers) layer.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        for (layer in layers) layer.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
