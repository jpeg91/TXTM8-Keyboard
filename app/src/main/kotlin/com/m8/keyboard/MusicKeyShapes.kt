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
 * Polished chrome, like the Discovery sleeve and the helmets: a bevelled rim round a face that reflects a bright sky
 * above a sharp dark horizon and a lighter ground below, with two slanting highlights, and a strip of [led] light
 * along the bottom. Pressed, the whole face is outlined in [led]. The tones are worked out from [fill] (a light grey
 * gives silver; a colour gives tinted chrome).
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
        val white = 0xFFFFFFFF.toInt()
        val black = 0xFF000000.toInt()
        val bright = mixArgb(fill, white, 0.9f)
        val skyLow = mixArgb(fill, black, 0.3f)
        val groundTop = mixArgb(fill, black, 0.32f)
        val groundLow = mixArgb(fill, white, 0.7f)
        val darkest = mixArgb(fill, black, 0.8f)

        // The rim: a bevel, bright on top and dark below.
        gradient(LinearGradient(0f, t, 0f, b, bright, darkest, Shader.TileMode.CLAMP))
        roundRect(canvas, l, t, r, b, corner)

        val i = 2.2f * density
        val fl = l + i
        val ft = t + i
        val fr = r - i
        val fb = b - i
        val fc = corner - i
        val horizon = ft + (fb - ft) * 0.52f

        // The sky above the horizon.
        gradient(LinearGradient(0f, ft, 0f, horizon, bright, skyLow, Shader.TileMode.CLAMP))
        path.reset()
        path.moveTo(fl, horizon)
        path.lineTo(fl, ft + fc)
        path.quadTo(fl, ft, fl + fc, ft)
        path.lineTo(fr - fc, ft)
        path.quadTo(fr, ft, fr, ft + fc)
        path.lineTo(fr, horizon)
        path.close()
        canvas.drawPath(path, paint)

        // The ground below it, dark at the horizon and bright at the bottom.
        gradient(LinearGradient(0f, horizon, 0f, fb, groundTop, groundLow, Shader.TileMode.CLAMP))
        path.reset()
        path.moveTo(fl, horizon)
        path.lineTo(fr, horizon)
        path.lineTo(fr, fb - fc)
        path.quadTo(fr, fb, fr - fc, fb)
        path.lineTo(fl + fc, fb)
        path.quadTo(fl, fb, fl, fb - fc)
        path.close()
        canvas.drawPath(path, paint)

        // The horizon itself: a thin, sharp dark line.
        solid(withAlpha(darkest, 230))
        canvas.drawRect(fl, horizon - 0.8f * density, fr, horizon + 0.9f * density, paint)

        // Two slanting highlights across the sky.
        val w = fr - fl
        solid(withAlpha(white, 150))
        path.reset()
        path.moveTo(fl + w * 0.14f, ft + density)
        path.lineTo(fl + w * 0.30f, ft + density)
        path.lineTo(fl + w * 0.18f, horizon - density)
        path.lineTo(fl + w * 0.06f, horizon - density)
        path.close()
        canvas.drawPath(path, paint)
        solid(withAlpha(white, 90))
        path.reset()
        path.moveTo(fl + w * 0.36f, ft + density)
        path.lineTo(fl + w * 0.42f, ft + density)
        path.lineTo(fl + w * 0.30f, horizon - density)
        path.lineTo(fl + w * 0.24f, horizon - density)
        path.close()
        canvas.drawPath(path, paint)

        // A thin bright line on the top lip of the face, and the dark outer edge.
        line(withAlpha(white, 210), density)
        canvas.drawLine(fl + fc, ft + 0.5f * density, fr - fc, ft + 0.5f * density, paint)
        line(withAlpha(darkest, 230), px)
        edge(canvas, l, t, r, b, corner, px)

        // The LED: a strip along the bottom, or the whole face outlined when pressed.
        if (pressed) {
            line(withAlpha(led, 90), 4f * density)
            edge(canvas, fl, ft, fr, fb, fc, 4f * density)
            line(led, 1.8f * density)
            edge(canvas, fl, ft, fr, fb, fc, 1.8f * density)
        } else {
            solid(led)
            roundRect(canvas, l + 9f * density, b - 5.5f * density, r - 9f * density, b - 3.5f * density, density)
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
 * A lump of modelling clay, like the Primus videos: a pillowy slab whose outline is a little different on every key
 * (hand-pressed, never straight), with a soft highlight, a darker underside, a thumbprint pushed into one corner and a
 * faint seam round the edge. Pressed, the clay sinks and flattens. [seam] is the colour of the edge line.
 */
class ClayKeyDrawable(
    private val fill: Int,
    private val seam: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l0: Float, t0: Float, r0: Float, b0: Float) {
        val sx = bounds.left
        val sy = bounds.top
        val sink = if (pressed) 1.6f * density else 0f
        val l = l0
        val r = r0
        val t = t0 + sink
        val b = b0 - 2f * density + sink * 0.5f
        val rc = 10f * density
        fun j(i: Int) = (musicUnit(i, sx, 51) - 0.5f) * 2.6f * density

        fun blob() {
            path.reset()
            path.moveTo(l + rc + j(0), t + j(1))
            path.quadTo((l + r) / 2f + j(2), t - 1.5f * density + j(3), r - rc + j(4), t + j(5))
            path.quadTo(r + j(6), t + j(7), r + j(8), t + rc + j(9))
            path.quadTo(r + 1.2f * density + j(10), (t + b) / 2f + j(11), r + j(12), b - rc + j(13))
            path.quadTo(r + j(14), b + j(15), r - rc + j(16), b + j(17))
            path.quadTo((l + r) / 2f + j(18), b + 1.8f * density + j(19), l + rc + j(20), b + j(21))
            path.quadTo(l + j(22), b + j(23), l + j(24), b - rc + j(25))
            path.quadTo(l - 1.2f * density + j(26), (t + b) / 2f + j(27), l + j(28), t + rc + j(29))
            path.quadTo(l + j(30), t + j(31), l + rc + j(32), t + j(33))
            path.close()
        }

        // The contact shadow under the clay.
        canvas.save()
        canvas.translate(0f, (if (pressed) 0.8f else 2f) * density)
        solid(withAlpha(0xFF000000.toInt(), 80))
        blob()
        canvas.drawPath(path, paint)
        canvas.restore()

        solid(fill)
        blob()
        canvas.drawPath(path, paint)

        // Darker towards the bottom.
        gradient(LinearGradient(0f, t, 0f, b, withAlpha(darken(fill, 0.6f), 0), withAlpha(darken(fill, 0.6f), 150), Shader.TileMode.CLAMP))
        canvas.drawPath(path, paint)

        canvas.save()
        canvas.clipRect(l, t, r, b)
        // The soft highlight and the thumbprint.
        solid(withAlpha(0xFFFFFFFF.toInt(), if (pressed) 30 else 75))
        rect.set(l + 4f * density, t + 2.5f * density, l + (r - l) * 0.62f, t + (b - t) * 0.42f)
        canvas.drawOval(rect, paint)
        val tx = if (musicUnit(0, sx, 52) < 0.5f) r - 9f * density else l + 9f * density
        val ty = b - 7f * density
        line(withAlpha(darken(fill, 0.5f), 70), density * 0.9f)
        for (k in 1..3) {
            rect.set(tx - k * 2.2f * density, ty - k * 1.7f * density, tx + k * 2.2f * density, ty + k * 1.7f * density)
            canvas.drawOval(rect, paint)
        }
        canvas.restore()

        line(withAlpha(seam, 120), px)
        blob()
        canvas.drawPath(path, paint)
    }
}

/**
 * A game menu row, like the pause menu of San Andreas: a flat dark bar with a fine rule along its top and a heavier one
 * along its bottom in [rule], the bottom rule brightening at the left end. Pressed (the row is highlighted), a bar of
 * [rule] colour runs down the left edge.
 */
class MenuKeyDrawable(
    private val fill: Int,
    private val rule: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        // Dark at the left, fading a little towards the right like a menu bar.
        gradient(LinearGradient(l, 0f, r, 0f, fill, withAlpha(fill, ((fill ushr 24) * 0.75f).toInt()), Shader.TileMode.CLAMP))
        canvas.drawRect(l, t, r, b, paint)
        line(withAlpha(rule, 70), px)
        canvas.drawLine(l, t + px / 2f, r, t + px / 2f, paint)
        line(withAlpha(rule, 190), 1.4f * density)
        canvas.drawLine(l, b - 0.7f * density, r, b - 0.7f * density, paint)
        line(rule, 2.4f * density)
        canvas.drawLine(l, b - 1.2f * density, l + (r - l) * 0.22f, b - 1.2f * density, paint)
        if (pressed) {
            solid(rule)
            canvas.drawRect(l, t, l + 3.5f * density, b, paint)
        }
    }
}
