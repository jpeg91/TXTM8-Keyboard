// app/src/main/kotlin/com/m8/keyboard/MusicKeyShapes.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * The key drawables added for the music and TV themes, one small class per [KeyStyle], in the same shape as the
 * ones in KeyShapes.kt: each is drawn from the colours it is given, so action keys and pressed keys work, and none of
 * them knows which theme it belongs to. Anything that looks random (scratches, cracks, holes) is seeded from the
 * key's position, so a key looks the same every time it is drawn but no two keys are alike.
 */

/** A repeatable pseudo-random number in 0 until 1 from three seeds. */
private fun musicUnit(a: Int, b: Int, c: Int): Float = ((hash3(a, b, c) ushr 1) % 1000) / 1000f

/**
 * A scuffed industrial plate, like the distressed artwork of The Downward Spiral: a near-black face crossed by
 * fine scratches in [scratch], rust running up from the bottom edge and down from the top in [rust], and a hairline
 * edge. Pressed, the key picks up more scratches.
 */
class ScratchedKeyDrawable(
    private val fill: Int,
    private val scratch: Int,
    private val rust: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val sx = bounds.left
        val sy = bounds.top
        val stain = if (rust == 0) lighten(fill, 0.2f) else rust
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)

        // Rust creeping up from the bottom edge, and a few streaks running down from the top.
        gradient(
            LinearGradient(
                0f, b - 16f * density, 0f, b,
                withAlpha(stain, 0), withAlpha(stain, if (pressed) 60 else 150), Shader.TileMode.CLAMP,
            ),
        )
        canvas.drawRect(l, b - 16f * density, r, b, paint)
        line(withAlpha(stain, 110), density)
        for (i in 0 until 3) {
            val x = l + (0.1f + musicUnit(i, sx, 21) * 0.8f) * (r - l)
            canvas.drawLine(x, t, x, t + (5f + musicUnit(i, sy, 22) * 12f) * density, paint)
        }

        // Scratches, mostly along one diagonal like a scrape with the same tool.
        canvas.save()
        canvas.clipRect(l, t, r, b)
        val count = if (pressed) 17 else 11
        for (i in 0 until count) {
            val x0 = l + musicUnit(i, sx, 1) * (r - l)
            val y0 = t + musicUnit(i, sy, 2) * (b - t)
            val angle = 0.45f + (musicUnit(i, sx + sy, 3) - 0.5f) * 0.7f
            val length = (8f + musicUnit(i, sx, 4) * 30f) * density
            line(withAlpha(scratch, 50 + (musicUnit(i, sy, 5) * 90f).toInt()), px)
            canvas.drawLine(x0, y0, x0 + cos(angle) * length, y0 + sin(angle) * length, paint)
        }
        canvas.restore()

        line(withAlpha(scratch, 140), px)
        edge(canvas, l, t, r, b, 0f, px)
    }
}

/**
 * An embroidered tour-jacket patch, like the Homework logo: a twill face under a raised merrow edge in [thread]
 * with a line of running stitches just inside it. Pressed, the thread catches the light.
 */
class StitchKeyDrawable(
    private val fill: Int,
    private val thread: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val corner = 5f * density
        solid(fill)
        roundRect(canvas, l, t, r, b, corner)

        // The twill weave: fine diagonal lines.
        canvas.save()
        canvas.clipRect(l + px, t + px, r - px, b - px)
        line(withAlpha(darken(fill, 0.35f), 70), px)
        val step = 3f * density
        var x = l - (b - t)
        while (x < r) {
            canvas.drawLine(x, b, x + (b - t), t, paint)
            x += step
        }
        canvas.restore()

        // The raised edge, then the running stitch inside it.
        val colour = if (pressed) lighten(thread, 0.35f) else thread
        val rim = 2.4f * density
        line(colour, rim)
        edge(canvas, l, t, r, b, corner, rim)
        line(withAlpha(colour, 210), 1.2f * density)
        val inset = 6f * density
        val dash = 3f * density
        val gap = 2.5f * density
        x = l + inset
        while (x < r - inset) {
            val x2 = min(x + dash, r - inset)
            canvas.drawLine(x, t + inset, x2, t + inset, paint)
            canvas.drawLine(x, b - inset, x2, b - inset, paint)
            x += dash + gap
        }
        var y = t + inset
        while (y < b - inset) {
            val y2 = min(y + dash, b - inset)
            canvas.drawLine(l + inset, y, l + inset, y2, paint)
            canvas.drawLine(r - inset, y, r - inset, y2, paint)
            y += dash + gap
        }
    }
}

/**
 * Liquid mercury, like the Discovery sleeve and the helmets: a chrome key with a bright top, a dark horizon across
 * the middle and a lighter reflection below, a soft shine, and a strip of [led] light along the bottom. Pressed,
 * the whole outline lights up in [led].
 */
class ChromeKeyDrawable(
    private val fill: Int,
    private val led: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val corner = 8f * density
        gradient(LinearGradient(0f, t, 0f, b, lighten(fill, 0.55f), darken(fill, 0.15f), Shader.TileMode.CLAMP))
        roundRect(canvas, l, t, r, b, corner)

        // The horizon: the lower half reflects a dark band that fades back to light at the bottom edge.
        val mid = t + (b - t) * 0.5f
        gradient(
            LinearGradient(
                0f, mid, 0f, b,
                withAlpha(darken(fill, 0.4f), 170), withAlpha(lighten(fill, 0.5f), 140), Shader.TileMode.CLAMP,
            ),
        )
        path.reset()
        path.moveTo(l, mid)
        path.lineTo(r, mid)
        path.lineTo(r, b - corner)
        path.quadTo(r, b, r - corner, b)
        path.lineTo(l + corner, b)
        path.quadTo(l, b, l, b - corner)
        path.close()
        canvas.drawPath(path, paint)

        // The shine across the top.
        solid(0x55FFFFFF)
        roundRect(canvas, l + 3f * density, t + 2f * density, r - 3f * density, t + (b - t) * 0.36f, corner - 2f * density)

        line(withAlpha(darken(fill, 0.5f), 220), px)
        edge(canvas, l, t, r, b, corner, px)

        // The LED: a strip along the bottom, or the whole outline when pressed.
        if (pressed) {
            line(withAlpha(led, 90), 4f * density)
            edge(canvas, l, t, r, b, corner, 4f * density)
            line(led, 2f * density)
            edge(canvas, l, t, r, b, corner, 2f * density)
        } else {
            solid(led)
            roundRect(canvas, l + 9f * density, b - 5f * density, r - 9f * density, b - 3f * density, density)
        }
    }
}

/**
 * A betting slip, from the greyhound-racing cover of Parklife: a ticket with a half-round notch cut from each side,
 * a stripe of [stripe] along the top, a dotted perforation near the right-hand end and a fine outline.
 */
class TicketKeyDrawable(
    private val fill: Int,
    private val stripe: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val notch = min(4.5f * density, (b - t) / 4f)
        val cy = (t + b) / 2f
        path.reset()
        path.moveTo(l, t)
        path.lineTo(r, t)
        path.lineTo(r, cy - notch)
        path.arcTo(RectF(r - notch, cy - notch, r + notch, cy + notch), 270f, -180f)
        path.lineTo(r, b)
        path.lineTo(l, b)
        path.lineTo(l, cy + notch)
        path.arcTo(RectF(l - notch, cy - notch, l + notch, cy + notch), 90f, -180f)
        path.close()
        solid(fill)
        canvas.drawPath(path, paint)

        solid(stripe)
        canvas.drawRect(l, t, r, t + (if (pressed) 5f else 3f) * density, paint)

        // The perforation.
        line(withAlpha(stripe, 150), 1.2f * density)
        val x = r - 13f * density
        var y = t + 7f * density
        while (y < b - 5f * density) {
            canvas.drawLine(x, y, x, min(y + 3f * density, b - 5f * density), paint)
            y += 6f * density
        }

        line(withAlpha(stripe, 200), px)
        canvas.drawPath(path, paint)
    }
}

/**
 * A slab of cheese, from the Sailing the Seas of Cheese sleeve: a rounded gold block with a darker rind along the
 * bottom and a few holes, each shaded so it looks scooped out. The holes are different on every key.
 */
class HolesKeyDrawable(
    private val fill: Int,
    private val rind: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val sx = bounds.left
        val sy = bounds.top
        val corner = 6f * density
        gradient(
            LinearGradient(0f, t, 0f, b, lighten(fill, if (pressed) 0.3f else 0.18f), fill, Shader.TileMode.CLAMP),
        )
        roundRect(canvas, l, t, r, b, corner)

        canvas.save()
        canvas.clipRect(l, t, r, b - 3f * density)
        for (i in 0 until 5) {
            val cx = l + (0.06f + musicUnit(i, sx, 5) * 0.88f) * (r - l)
            val cy = t + (0.12f + musicUnit(i, sy, 6) * 0.70f) * (b - t)
            val radius = (2.2f + musicUnit(i, sx + sy, 7) * 3.2f) * density
            solid(withAlpha(lighten(fill, 0.5f), 150))
            canvas.drawCircle(cx + 0.7f * density, cy + 0.8f * density, radius, paint)
            solid(darken(fill, 0.22f))
            canvas.drawCircle(cx, cy, radius, paint)
        }
        canvas.restore()

        solid(rind)
        roundRect(canvas, l + density, b - 3.2f * density, r - density, b, 1.6f * density)
        line(rind, 1.4f * density)
        edge(canvas, l, t, r, b, corner, 1.4f * density)
    }
}

/**
 * A comic speech bubble, like the Gorillaz band cartoons: a rounded balloon with a heavy [outline], a pointed tail
 * at the lower left and a few halftone dots shading the lower right.
 */
class SpeechKeyDrawable(
    private val fill: Int,
    private val outline: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val w = 2f * density
        val tail = 6f * density
        val corner = 9f * density
        val bl = l + w
        val bt = t + w
        val br = r - w
        val bb = b - tail
        val tx = l + (r - l) * 0.22f
        path.reset()
        path.moveTo(tx, bb - density)
        path.lineTo(tx + 7f * density, bb - density)
        path.lineTo(tx - 1f * density, b - w)
        path.close()

        // The outline is drawn first, twice as thick, so only its outer half shows once the fill goes on top.
        rect.set(bl, bt, br, bb)
        line(outline, 2f * w)
        canvas.drawRoundRect(rect, corner, corner, paint)
        canvas.drawPath(path, paint)
        solid(fill)
        canvas.drawRoundRect(rect, corner, corner, paint)
        canvas.drawPath(path, paint)

        // Halftone dots, larger towards the corner.
        solid(withAlpha(outline, if (pressed) 90 else 50))
        for (row in 0 until 3) {
            for (col in 0 until 4 - row) {
                val dotRadius = (0.7f + (3 - row - col) * 0.28f) * density
                canvas.drawCircle(br - (3f + col * 3.4f) * density, bb - (3f + row * 3.4f) * density, dotRadius, paint)
            }
        }
    }
}

/**
 * A gothic window, like the arches of the Fallen sleeve: a tall key with a pointed arch for its top, a fine line of
 * tracery just inside the outline, and a face that deepens towards the bottom. Pressed, the outline glows.
 */
class GothicKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    /** Builds the pointed-arch outline into [path]. */
    private fun arch(l: Float, t: Float, r: Float, b: Float, rise: Float) {
        val cx = (l + r) / 2f
        path.reset()
        path.moveTo(l, b)
        path.lineTo(l, t + rise)
        path.quadTo(l, t + rise * 0.25f, cx, t)
        path.quadTo(r, t + rise * 0.25f, r, t + rise)
        path.lineTo(r, b)
        path.close()
    }

    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val rise = max(4f * density, min((b - t) * 0.42f, (r - l) * 0.5f))
        arch(l, t, r, b, rise)
        gradient(LinearGradient(0f, t, 0f, b, lighten(fill, 0.14f), darken(fill, 0.25f), Shader.TileMode.CLAMP))
        canvas.drawPath(path, paint)

        if (pressed) {
            line(withAlpha(border, 80), 4f * density)
            canvas.drawPath(path, paint)
        }
        val gap = 3f * density
        arch(l + gap, t + gap, r - gap, b - gap, max(2f * density, rise - 2f * gap))
        line(withAlpha(border, if (pressed) 230 else 130), 1.1f * density)
        canvas.drawPath(path, paint)
        arch(l, t, r, b, rise)
        line(border, 1.5f * density)
        canvas.drawPath(path, paint)
    }
}

/**
 * A shattered pane, like the Kerrang! masthead: a dark square cracked from an impact point (somewhere different on
 * each key) by hairlines that kink as they run out to the edge and are joined by two rings of fine cracks, with a
 * bar of [bar] colour along the bottom. Pressed, the cracks are heavier.
 */
class ShatterKeyDrawable(
    private val fill: Int,
    private val crack: Int,
    private val bar: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val sx = bounds.left
        val sy = bounds.top
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)

        val ix = l + (0.25f + musicUnit(sx, sy, 41) * 0.5f) * (r - l)
        val iy = t + (0.25f + musicUnit(sy, sx, 42) * 0.5f) * (b - t)
        val spokes = 8
        val step = (2.0 * PI / spokes).toFloat()
        val reach = (r - l) + (b - t)
        val ringRadius = floatArrayOf(7f * density, 16f * density)

        canvas.save()
        canvas.clipRect(l, t, r, b)
        val heavy = if (pressed) 1.5f * density else px
        line(withAlpha(crack, if (pressed) 230 else 120), heavy)
        for (i in 0 until spokes) {
            var angle = i * step + (musicUnit(i, sx, 43) - 0.5f) * 0.5f
            var x = ix
            var y = iy
            var travelled = 0f
            var s = 0
            while (travelled < reach && s < 6) {
                angle += (musicUnit(i, s, 44) - 0.5f) * 0.35f
                val length = (6f + musicUnit(i, s, 45) * 10f) * density
                val nx = x + cos(angle) * length
                val ny = y + sin(angle) * length
                canvas.drawLine(x, y, nx, ny, paint)
                x = nx
                y = ny
                travelled += length
                s++
            }
        }
        // The rings join one spoke to the next.
        line(withAlpha(crack, if (pressed) 170 else 100), px)
        for (k in ringRadius.indices) {
            for (i in 0 until spokes) {
                val ra = ringRadius[k] * (0.8f + musicUnit(i, k, 46) * 0.4f)
                val rb = ringRadius[k] * (0.8f + musicUnit(i + 1, k, 46) * 0.4f)
                val a1 = i * step
                val a2 = (i + 1) * step
                canvas.drawLine(ix + cos(a1) * ra, iy + sin(a1) * ra, ix + cos(a2) * rb, iy + sin(a2) * rb, paint)
            }
        }
        canvas.restore()

        solid(bar)
        canvas.drawRect(l, b - 3f * density, r, b, paint)
        line(withAlpha(crack, 90), px)
        edge(canvas, l, t, r, b, 0f, px)
    }
}
