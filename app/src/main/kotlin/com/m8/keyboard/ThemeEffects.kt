// app/src/main/kotlin/com/m8/keyboard/ThemeEffects.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** How a theme draws its keys, beyond the older per-theme flags (retro, Nokia, glitter and so on). */
enum class KeyStyle {
    /** Whatever the older flags say: a plain, outlined or bevelled key. */
    DEFAULT,

    /** Rounded, glassy keys: a light top and dark bottom, a shine across the upper half, an outline. */
    GLOSS,

    /** Flat, pixel-aligned keys with notched corners, like the soft buttons of a handheld organiser. */
    LCD,

    /** Hard black outline and an offset solid shadow, the early-90s "Memphis" look. */
    MEMPHIS,
}

/**
 * A slow animation drawn behind the keys (it shows through the gaps and through translucent keys).
 * Every one is drawn by hand from the clock at a low, steady frame rate, so Android's animation-speed
 * settings do not change it, and it stops by itself whenever the keyboard is hidden.
 */
enum class AmbientKind {
    /** A sunlit hill with clouds drifting across the sky. */
    CLOUDS,

    /** Thin glowing lines that ripple like a music visualiser. */
    WAVES,

    /** Broad translucent ribbons that sway slowly, like a console menu background. */
    RIBBONS,

    /** A faint band that rolls down the screen. */
    SCAN,

    /** A starry tiled backdrop with a marching hazard-tape border. */
    HAZARD,

    /** A row of sound-wave bars that lies flat until a key is pressed, then rings like a short sound. */
    SOUND,

    /** A red band that sweeps across the screen like the mirror scanner of a 3D handheld. */
    RED_SCAN,

    /** Columns of falling green glyphs. */
    RAIN,

    /** Squiggles, triangles and dots that wobble as if redrawn by hand every few frames. */
    MEMPHIS,
}

// ------------------------------------------------------------------ colour helpers

/** [c] pushed towards white by [f] (0..1), keeping its alpha. */
internal fun lighten(c: Int, f: Float): Int {
    fun up(v: Int) = (v + (255 - v) * f).toInt().coerceIn(0, 255)
    return (c and 0xFF000000.toInt()) or (up((c shr 16) and 0xFF) shl 16) or (up((c shr 8) and 0xFF) shl 8) or up(c and 0xFF)
}

/** [c] pushed towards black by [f] (0..1), keeping its alpha. */
internal fun darken(c: Int, f: Float): Int {
    fun down(v: Int) = (v * (1f - f)).toInt().coerceIn(0, 255)
    return (c and 0xFF000000.toInt()) or (down((c shr 16) and 0xFF) shl 16) or (down((c shr 8) and 0xFF) shl 8) or down(c and 0xFF)
}

/** [c] with its alpha replaced by [alpha] (0..255). */
internal fun withAlpha(c: Int, alpha: Int): Int = (alpha.coerceIn(0, 255) shl 24) or (c and 0x00FFFFFF)

/** A small, stable pseudo-random number from three integers; the same inputs always give the same value. */
internal fun hash3(a: Int, b: Int, c: Int): Int {
    var h = a * 374761393 + b * 668265263 + c * 1274126177
    h = (h xor (h ushr 13)) * 1103515245
    return (h xor (h ushr 16)) and 0x7FFFFFFF
}

// ------------------------------------------------------------------ key drawables

/**
 * A glassy rounded key: the face runs from lighter (top) to darker (bottom), a pale shine covers the upper
 * half, and a one-colour outline goes round the edge. Pressed, the gradient flips, the shine goes, and an
 * inner [ring] appears (Luna orange, glass cyan and so on) when the theme gives one.
 */
class GlossKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val ring: Int,
    private val pressed: Boolean,
    private val gloss: Int,
    private val insetX: Float,
    private val insetY: Float,
    private val radius: Float,
    private val density: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX
        val bottom = b.bottom - insetY
        if (r - l < 8f * density || bottom - t < 8f * density) return
        val corner = radius

        val top = if (pressed) darken(fill, 0.10f) else lighten(fill, 0.22f)
        val low = if (pressed) lighten(fill, 0.08f) else darken(fill, 0.14f)
        rect.set(l, t, r, bottom)
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(l, t, l, bottom, top, low, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.shader = null

        if (!pressed && gloss > 0) {
            val inset = 1.5f * density
            rect.set(l + inset, t + inset, r - inset, t + (bottom - t) * 0.46f)
            paint.shader = LinearGradient(
                0f, rect.top, 0f, rect.bottom,
                withAlpha(0xFFFFFFFF.toInt(), gloss), withAlpha(0xFFFFFFFF.toInt(), gloss / 4), Shader.TileMode.CLAMP,
            )
            canvas.drawRoundRect(rect, maxOf(1f, corner - inset), maxOf(1f, corner - inset), paint)
            paint.shader = null
        }

        paint.style = Paint.Style.STROKE
        if (pressed && ring != 0) {
            paint.strokeWidth = 1.6f * density
            paint.color = ring
            val inset = 2f * density
            rect.set(l + inset, t + inset, r - inset, bottom - inset)
            canvas.drawRoundRect(rect, maxOf(1f, corner - inset), maxOf(1f, corner - inset), paint)
        }
        if (border != 0) {
            paint.strokeWidth = maxOf(1f, 1.2f * density)
            paint.color = border
            rect.set(l, t, r, bottom)
            canvas.drawRoundRect(rect, corner, corner, paint)
        }
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * A flat, pixel-aligned key with notched corners and a darker lower and right edge: the soft buttons of a
 * monochrome handheld organiser. Pressed keys lose the edge and gain a second outline pixel.
 */
class LcdKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val edge: Int,
    private val pressed: Boolean,
    private val insetX: Float,
    private val insetY: Float,
    private val density: Float,
) : Drawable() {
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val px = maxOf(1f, kotlin.math.round(density))
        val l = kotlin.math.round(b.left + insetX).toFloat()
        val t = kotlin.math.round(b.top + insetY).toFloat()
        val r = kotlin.math.round(b.right - insetX).toFloat()
        val bottom = kotlin.math.round(b.bottom - insetY).toFloat()
        if (r - l < 8f * px || bottom - t < 8f * px) return

        // Face, with the four corner pixels left out.
        paint.color = fill
        canvas.drawRect(l + px, t, r - px, bottom, paint)
        canvas.drawRect(l, t + px, r, bottom - px, paint)

        // Lower and right edge, a shade darker than the face.
        if (!pressed && edge != 0) {
            paint.color = edge
            canvas.drawRect(l + px * 2, bottom - px * 2, r - px, bottom - px, paint)
            canvas.drawRect(r - px * 2, t + px * 2, r - px, bottom - px, paint)
        }

        // One-pixel outline (two when pressed), notched at the corners.
        paint.color = border
        val w = if (pressed) px * 2 else px
        canvas.drawRect(l + px, t, r - px, t + w, paint)
        canvas.drawRect(l + px, bottom - w, r - px, bottom, paint)
        canvas.drawRect(l, t + px, l + w, bottom - px, paint)
        canvas.drawRect(r - w, t + px, r, bottom - px, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * A flat key with a heavy dark outline and a hard, offset shadow. When pressed the key slides down onto its
 * shadow, so it looks pushed in.
 */
class MemphisKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val shadow: Int,
    private val pressed: Boolean,
    private val insetX: Float,
    private val insetY: Float,
    private val density: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val off = 3f * density
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX - off
        val bottom = b.bottom - insetY - off
        if (r - l < 8f * density || bottom - t < 8f * density) return
        val shift = if (pressed) off - density else 0f
        val stroke = maxOf(1.5f, 2f * density)

        paint.style = Paint.Style.FILL
        paint.color = shadow
        rect.set(l + off, t + off, r + off, bottom + off)
        canvas.drawRect(rect, paint)

        paint.color = fill
        rect.set(l + shift, t + shift, r + shift, bottom + shift)
        canvas.drawRect(rect, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        paint.strokeJoin = Paint.Join.MITER
        paint.color = border
        rect.inset(stroke / 2f, stroke / 2f)
        canvas.drawRect(rect, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

// ------------------------------------------------------------------ ambient animation

/** Forwards a child drawable's redraw requests to [owner], so a wrapped animated background keeps running. */
internal class ForwardCallback(private val owner: Drawable) : Drawable.Callback {
    override fun invalidateDrawable(who: Drawable) = owner.invalidateSelf()
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = owner.scheduleSelf(what, `when`)
    override fun unscheduleDrawable(who: Drawable, what: Runnable) = owner.unscheduleSelf(what)
}

/**
 * A keyboard background with an animation on top of it. The animation is drawn by hand: the frame number
 * comes from the clock ([SystemClock.uptimeMillis]) and the next frame is requested a fixed time later, so
 * it neither follows Android's animation-speed settings nor runs flat out. It only keeps going while the
 * drawable is visible, so a hidden keyboard costs nothing.
 *
 * [colours] mean different things to different kinds (see each draw method).
 */
class AmbientBackground(
    private val base: Drawable,
    private val kind: AmbientKind,
    private val colours: IntArray,
    private val density: Float,
) : Drawable(), KeyboardPulseEffect {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val rect = RectF()
    private val startedAt = SystemClock.uptimeMillis()
    private var nowMs = startedAt
    private var animating = true

    // The last few key presses (for the sound-wave kind): when each happened and where along the width.
    private val soundTimes = LongArray(SOUND_EVENTS)
    private val soundX = FloatArray(SOUND_EVENTS)
    private val soundAges = FloatArray(SOUND_EVENTS)
    private var soundNext = 0
    private val tick = Runnable { invalidateSelf() }
    private val glyphs: CharArray = if (kind == AmbientKind.RAIN) matrixGlyphs() else CharArray(0)
    private val one = CharArray(1)
    private var hill: Shader? = null
    private var hillBack: Shader? = null

    /** Milliseconds per frame. Slow and stepped on purpose: it reads as hand-drawn and costs little. */
    private val frameMs: Long = when (kind) {
        AmbientKind.CLOUDS -> 100L
        AmbientKind.WAVES -> 70L
        AmbientKind.RIBBONS -> 80L
        AmbientKind.SCAN -> 90L
        AmbientKind.HAZARD -> 120L
        AmbientKind.SOUND -> 40L
        AmbientKind.RED_SCAN -> 70L
        AmbientKind.RAIN -> 85L
        AmbientKind.MEMPHIS -> 220L
    }

    init {
        if (kind == AmbientKind.RAIN) {
            paint.typeface = Typeface.MONOSPACE
            paint.textAlign = Paint.Align.CENTER
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        base.bounds = bounds
        if (kind == AmbientKind.CLOUDS) {
            val h = bounds.height().toFloat()
            hillBack = LinearGradient(0f, h * 0.6f, 0f, h, 0xFF7CC336.toInt(), 0xFF3F8F1A.toInt(), Shader.TileMode.CLAMP)
            hill = LinearGradient(0f, h * 0.7f, 0f, h, 0xFF58B02A.toInt(), 0xFF2C7512.toInt(), Shader.TileMode.CLAMP)
        }
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        base.setVisible(visible, restart)
        val changed = super.setVisible(visible, restart)
        if (!visible) unscheduleSelf(tick)
        return changed
    }

    override fun draw(canvas: Canvas) {
        base.draw(canvas)
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val now = SystemClock.uptimeMillis()
        nowMs = now
        animating = true
        val frame = ((now - startedAt) / frameMs).toInt()
        canvas.save()
        canvas.clipRect(b)
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        when (kind) {
            AmbientKind.CLOUDS -> drawClouds(canvas, w, h, frame)
            AmbientKind.WAVES -> drawWaves(canvas, w, h, frame, filled = false)
            AmbientKind.RIBBONS -> drawWaves(canvas, w, h, frame, filled = true)
            AmbientKind.SCAN -> drawScan(canvas, w, h, frame)
            AmbientKind.HAZARD -> drawHazard(canvas, w, h, frame)
            AmbientKind.SOUND -> drawSound(canvas, w, h)
            AmbientKind.RED_SCAN -> drawRedScan(canvas, w, h, frame)
            AmbientKind.RAIN -> drawRain(canvas, w, h, frame)
            AmbientKind.MEMPHIS -> drawMemphis(canvas, w, h, frame)
        }
        canvas.restore()
        paint.shader = null
        paint.alpha = 255
        // One pending tick at most: other redraws (typing, a pulse) must not start extra chains of frames.
        unscheduleSelf(tick)
        // The sound wave only needs frames while a sound is still ringing; flat, it is drawn once and rests.
        if (isVisible && animating) scheduleSelf(tick, now + frameMs)
    }

    private fun col(i: Int): Int = if (colours.isEmpty()) 0xFFFFFFFF.toInt() else colours[i % colours.size]

    // -------------------------------------------------- Windows XP: hill and drifting clouds

    private fun drawClouds(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        // Clouds first (they sit in the sky), three of different size and speed.
        paint.style = Paint.Style.FILL
        paint.color = 0xB8FFFFFF.toInt()
        for (i in 0 until 3) {
            val s = (16f + i * 7f) * d
            val span = w + s * 4f
            val speed = (0.5f + i * 0.28f) * d
            val x = ((hash3(i, 7, 1) % 1000) / 1000f * span + frame * speed) % span - s * 2f
            val y = h * (0.10f + 0.13f * i) + s * 0.2f
            canvas.drawCircle(x, y, s * 0.62f, paint)
            canvas.drawCircle(x + s * 0.7f, y - s * 0.25f, s * 0.78f, paint)
            canvas.drawCircle(x + s * 1.45f, y, s * 0.6f, paint)
            rect.set(x - s * 0.1f, y, x + s * 2.0f, y + s * 0.62f)
            canvas.drawRoundRect(rect, s * 0.3f, s * 0.3f, paint)
        }
        // Two hills, the far one lighter.
        paint.shader = hillBack
        path.reset()
        path.moveTo(0f, h * 0.74f)
        path.cubicTo(w * 0.25f, h * 0.60f, w * 0.60f, h * 0.70f, w, h * 0.58f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = hill
        path.reset()
        path.moveTo(0f, h * 0.84f)
        path.cubicTo(w * 0.35f, h * 0.70f, w * 0.70f, h * 0.90f, w, h * 0.74f)
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()
        canvas.drawPath(path, paint)
        paint.shader = null
    }

    // -------------------------------------------------- Media Player and PSP: waves and ribbons

    /** [colours]: one per line or ribbon (ARGB, so the alpha sets how strong it is). */
    private fun drawWaves(canvas: Canvas, w: Float, h: Float, frame: Int, filled: Boolean) {
        val d = density
        val lines = if (colours.isEmpty()) 1 else colours.size
        val step = 10f * d
        paint.style = if (filled) Paint.Style.FILL else Paint.Style.STROKE
        paint.strokeWidth = 1.6f * d
        paint.strokeJoin = Paint.Join.ROUND
        for (i in 0 until lines) {
            val baseY = h * (if (filled) 0.45f + 0.16f * i else 0.30f + 0.2f * i)
            val amp = (if (filled) 20f + 9f * i else 14f + 7f * i) * d
            val k = (if (filled) 0.012f else 0.018f) + 0.004f * i
            val speed = (if (filled) 0.07f else 0.16f) * (if (i % 2 == 0) 1f else -1.15f)
            val phase = frame * speed + i * 2.1f
            path.reset()
            var x = -step
            var first = true
            while (x <= w + step) {
                val y = baseY + amp * sin(x / d * k + phase) + amp * 0.45f * sin(x / d * k * 2.3f - phase * 1.3f)
                if (first) { path.moveTo(x, y); first = false } else path.lineTo(x, y)
                x += step
            }
            if (filled) {
                path.lineTo(w + step, h)
                path.lineTo(-step, h)
                path.close()
            }
            paint.color = col(i)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    // -------------------------------------------------- BIOS: rolling band and cursor

    /** [colours]: 0 is the band. (The blinking cursor is a separate overlay, because it sits on top of the keys.) */
    private fun drawScan(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        val band = 12f * d
        val period = h + band * 4f
        val y = (frame * 2.5f * d) % period - band * 3f
        paint.style = Paint.Style.FILL
        val a = (col(0) ushr 24) and 0xFF
        for (i in 0 until 3) {
            paint.color = withAlpha(col(0), if (i == 1) a else a / 2)
            canvas.drawRect(0f, y + i * band, w, y + (i + 1) * band, paint)
        }
    }

    // -------------------------------------------------- GeoCities: stars and hazard tape

    /** [colours]: 0 and 1 are the two star colours, 2 and 3 the tape's two stripes. */
    private fun drawHazard(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        val cell = 26f * d
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1
        paint.style = Paint.Style.FILL
        for (cy in 0 until rows) for (cx in 0 until cols) {
            val hs = hash3(cx, cy, 11)
            if (hs % 4 != 0) continue
            // Each star is on for most of a slow cycle and off for the rest, so they twinkle out of step.
            if ((frame / 3 + hs / 7) % 5 == 0) continue
            val x = cx * cell + (hs % 97) / 97f * cell
            val y = cy * cell + ((hs / 97) % 97) / 97f * cell
            paint.color = if (hs % 8 == 0) col(1) else col(0)
            val s = (if (hs % 3 == 0) 2f else 1.4f) * d
            canvas.drawRect(x - s, y - s * 0.35f, x + s, y + s * 0.35f, paint)
            canvas.drawRect(x - s * 0.35f, y - s, x + s * 0.35f, y + s, paint)
        }
        // Tape along the top edge, marching to the right.
        val tape = 4f * d
        paint.color = col(2)
        canvas.drawRect(0f, 0f, w, tape, paint)
        paint.color = col(3)
        val period = 14f * d
        val offset = (frame * 2f * d) % period
        var x = -period + offset
        while (x < w) {
            path.reset()
            path.moveTo(x + tape, 0f)
            path.lineTo(x + tape + period / 2f, 0f)
            path.lineTo(x + period / 2f, tape)
            path.lineTo(x, tape)
            path.close()
            canvas.drawPath(path, paint)
            x += period
        }
    }

    // -------------------------------------------------- Walkman: a sound wave

    /**
     * [colours]: 0 is the bars. A row of short bars (a flat dotted line when nothing is playing). Each key press
     * starts a burst centred on that key: ripples spread out from it and die away over about a second, the way
     * a short click or note would look on an audio editor.
     */
    private fun drawSound(canvas: Canvas, w: Float, h: Float) {
        val d = density
        val now = nowMs
        var live = false
        for (e in 0 until SOUND_EVENTS) {
            val age = if (soundTimes[e] == 0L) 99f else (now - soundTimes[e]) / 1000f
            soundAges[e] = age
            if (age < 1.1f) live = true
        }
        val baseY = h * 0.5f
        val step = 4f * d
        val half = 1f * d
        paint.style = Paint.Style.FILL
        paint.color = col(0)
        var x = step / 2f
        while (x < w) {
            var disp = 0f
            if (live) {
                for (e in 0 until SOUND_EVENTS) {
                    val a = soundAges[e]
                    if (a >= 1.1f) continue
                    val dist = abs(x - soundX[e]) / d // dp from the key
                    val attack = 1f - exp(-a * 70f)
                    val envelope = exp(-a / 0.26f) * attack
                    val reach = exp(-dist / 150f)
                    disp += envelope * reach * sin(2f * PI.toFloat() * (dist / 28f - a * 9f))
                }
            }
            val bar = maxOf(half, minOf(abs(disp) * 0.34f * h, h * 0.42f))
            canvas.drawRect(x - d, baseY - bar, x + d, baseY + bar, paint)
            x += step
        }
        animating = live
    }

    // -------------------------------------------------- Virtual Boy: sweeping red band

    /** [colours]: 0 is the band's brightest red. */
    private fun drawRedScan(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        val strip = 9f * d
        val trail = 6
        val period = w + strip * (trail + 2)
        val head = (frame * 7f * d) % period - strip
        val red = col(0)
        val a = (red ushr 24) and 0xFF
        paint.style = Paint.Style.FILL
        for (i in 0 until trail) {
            // The leading edge is the brightest; the trail behind it fades.
            val f = 1f - i / trail.toFloat()
            paint.color = withAlpha(red, (a * f * f).toInt())
            canvas.drawRect(head - (i + 1) * strip, 0f, head - i * strip, h, paint)
        }
        // A few single-pixel scan lines across the whole screen.
        paint.color = withAlpha(red, a / 5)
        var y = 0f
        val gap = 6f * d
        while (y < h) {
            canvas.drawRect(0f, y, w, y + maxOf(1f, d * 0.7f), paint)
            y += gap
        }
    }

    // -------------------------------------------------- Matrix: falling glyphs

    /** [colours]: 0 is the bright head of a column, 1 the body of its trail. */
    private fun drawRain(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        val cell = 13f * d
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1
        paint.style = Paint.Style.FILL
        paint.textSize = cell * 0.95f
        for (c in 0 until cols) {
            val hc = hash3(c, 3, 5)
            if (hc % 5 == 0) continue // a few columns stay empty, so the rain is not a solid wall
            val trail = 6 + hc % 9
            val speed = 0.22f + (hc / 13 % 40) / 100f // cells per frame
            val loop = rows + trail + 4
            val head = ((frame * speed + (hc / 7 % 97)) % loop) - trail * 0.5f
            val x = c * cell + cell / 2f
            for (i in 0 until trail) {
                val row = head.toInt() - i
                if (row < 0 || row >= rows) continue
                // The head changes glyph quickly, the body only now and then.
                val g = hash3(c, row, if (i == 0) frame / 2 else frame / 7) % glyphs.size
                one[0] = glyphs[g]
                val f = 1f - i / trail.toFloat()
                paint.color = if (i == 0) col(0) else col(1)
                paint.alpha = ((if (i == 0) 255f else 215f * f * f)).toInt().coerceIn(0, 255)
                canvas.drawText(one, 0, 1, x, row * cell + cell * 0.85f, paint)
            }
        }
        paint.alpha = 255
    }

    // -------------------------------------------------- Saved by the Bell: wobbling shapes

    /**
     * [colours]: the shape colours. One small shape in every cell of a grid, placed at random inside its cell, so
     * they are spread over the whole screen and show through every gap between the keys.
     */
    private fun drawMemphis(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        val wobble = frame % 3 // three drawings, repeated: the "line boil" of hand-drawn animation
        val cell = 24f * d
        val cols = (w / cell).toInt() + 1
        val rows = (h / cell).toInt() + 1
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * d
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        for (gy in 0 until rows) for (gx in 0 until cols) {
            val hs = hash3(gx, gy, 17)
            val jx = ((hash3(gx, gy, 20 + wobble) % 5) - 2) * 0.6f * d
            val jy = ((hash3(gx, gy, 30 + wobble) % 5) - 2) * 0.6f * d
            val cx = gx * cell + (hs % 100) / 100f * cell + jx
            val cy = gy * cell + ((hs / 100) % 100) / 100f * cell + jy
            val s = (3.4f + (hs / 10000 % 4) * 0.9f) * d
            val turn = ((hs / 40000 % 8) * 22 - 80 + (wobble - 1) * 4).toFloat()
            paint.color = col(hs / 7)
            canvas.save()
            canvas.translate(cx, cy)
            canvas.rotate(turn)
            path.reset()
            when (hs % 5) {
                0 -> { // squiggle
                    path.moveTo(-s * 1.6f, 0f)
                    for (k in 0 until 4) {
                        path.lineTo(-s * 1.6f + (k + 0.5f) * s * 0.8f, if (k % 2 == 0) -s * 0.6f else s * 0.6f)
                    }
                    path.lineTo(s * 1.6f, 0f)
                }
                1 -> { // triangle
                    path.moveTo(0f, -s)
                    path.lineTo(s * 0.95f, s * 0.7f)
                    path.lineTo(-s * 0.95f, s * 0.7f)
                    path.close()
                }
                2 -> { // ring
                    path.addCircle(0f, 0f, s * 0.7f, Path.Direction.CW)
                }
                3 -> { // plus
                    path.moveTo(-s * 0.8f, 0f)
                    path.lineTo(s * 0.8f, 0f)
                    path.moveTo(0f, -s * 0.8f)
                    path.lineTo(0f, s * 0.8f)
                }
                else -> { // a row of dots
                    for (k in -1..1) path.addCircle(k * s * 0.8f, 0f, s * 0.14f, Path.Direction.CW)
                }
            }
            canvas.drawPath(path, paint)
            canvas.restore()
        }
        paint.style = Paint.Style.FILL
    }

    override fun pulse() = pulse(null)

    /** A key was pressed: the sound-wave kind rings from that key's position. Other kinds ignore it. */
    override fun pulse(key: RectF?) {
        if (kind != AmbientKind.SOUND) return
        val i = soundNext
        soundNext = (soundNext + 1) % SOUND_EVENTS
        soundTimes[i] = SystemClock.uptimeMillis()
        soundX[i] = key?.centerX() ?: bounds.exactCenterX()
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) { base.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { base.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val SOUND_EVENTS = 4

        /** Half-width katakana, digits and a few symbols: the look of the falling code. */
        fun matrixGlyphs(): CharArray {
            val list = ArrayList<Char>()
            for (code in 0xFF66..0xFF9D) list.add(code.toChar())
            for (ch in "0123456789:.=*+-<>") list.add(ch)
            return list.toCharArray()
        }
    }
}

// ------------------------------------------------------------------ which key was pressed

/**
 * Finds the key under a touch. A key is taken to be the deepest view that contains the point and has no
 * children of its own (a key, a candidate word, an emoji), and its rectangle is given in [root]'s own
 * coordinates, scroll offsets included. Returns false when the touch is in a gap or padding.
 */
internal fun findKeyRect(root: ViewGroup, x: Float, y: Float, out: RectF): Boolean {
    fun walk(group: ViewGroup, ox: Float, oy: Float): Boolean {
        for (i in group.childCount - 1 downTo 0) {
            val c = group.getChildAt(i)
            if (c.visibility != View.VISIBLE) continue
            val l = ox + c.left - group.scrollX + c.translationX
            val t = oy + c.top - group.scrollY + c.translationY
            if (x < l || x >= l + c.width || y < t || y >= t + c.height) continue
            if (c is ViewGroup && c.childCount > 0) {
                if (walk(c, l, t)) return true
                continue
            }
            out.set(l, t, l + c.width, t + c.height)
            return true
        }
        return false
    }
    return walk(root, 0f, 0f)
}

/**
 * Remembers which key a finger last touched down on. The keyboard views use it to start the typing pulse
 * and the sound wave at that key, and to put the BIOS cursor on it. [insetPx] trims the gap that is drawn
 * inside each key's cell, so the rectangle is the visible key.
 */
class KeyTracker(private val host: ViewGroup) {
    private val rect = RectF()
    private var valid = false

    fun down(x: Float, y: Float, insetPx: Float) {
        valid = findKeyRect(host, x, y, rect)
        if (valid && rect.width() > insetPx * 4f && rect.height() > insetPx * 4f) rect.inset(insetPx, insetPx)
    }

    /** The last key touched, or null when no key has been touched yet (or the touch was in a gap). */
    fun current(): RectF? = if (valid) rect else null
}

/**
 * A blinking block cursor at the bottom-left of the last key pressed, like a DOS prompt. It is a foreground
 * drawable (the keys would hide it in the background). It blinks by hand, on the clock: it asks for a frame
 * only when it next has to change, so it costs almost nothing, and it restarts its blink whenever it moves.
 */
class KeyCursorDrawable(
    private val tracker: KeyTracker,
    private val colour: Int,
    private val density: Float,
) : Drawable() {
    private val paint = Paint()
    private var blinkStart = SystemClock.uptimeMillis()
    private val tick = Runnable { invalidateSelf() }

    /** The cursor moved to a new key: show it solid again and restart the blink. */
    fun moved() {
        blinkStart = SystemClock.uptimeMillis()
        invalidateSelf()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        if (!visible) unscheduleSelf(tick)
        return changed
    }

    override fun draw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val phase = ((now - blinkStart) / BLINK_MS).toInt()
        if (phase % 2 == 0) {
            val key = tracker.current()
            val left = if (key != null) key.left + 3f * density else bounds.left + 6f * density
            val bottom = if (key != null) key.bottom - 3f * density else bounds.bottom - 4f * density
            paint.color = colour
            canvas.drawRect(left, bottom - 3f * density, left + 8f * density, bottom, paint)
        }
        unscheduleSelf(tick)
        if (isVisible) scheduleSelf(tick, blinkStart + (phase + 1) * BLINK_MS)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object { const val BLINK_MS = 530L }
}
