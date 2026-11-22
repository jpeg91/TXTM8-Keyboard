// app/src/main/kotlin/com/m8/keyboard/MusicEffects.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The moving and still backgrounds added for the music and TV themes ([AmbientKind.SPIRAL] and so on). They are
 * drawn by [AmbientBackground], which only has to pick the right one; the drawing lives here. Each takes the shared
 * paint and path, the size, the frame number, the screen density and the theme's colour list, and leaves the paint
 * as it found it (butt caps, mitre joins) for the next animation.
 */

/** The colour at [i] in [colours] (wrapping round), or [fallback] when the list is empty. */
private fun colourAt(colours: IntArray, i: Int, fallback: Int = 0xFFFFFFFF.toInt()): Int =
    if (colours.isEmpty()) fallback else colours[i % colours.size]

/** A repeatable pseudo-random number in 0 until 1 from three seeds. */
private fun unit(a: Int, b: Int, c: Int): Float = ((hash3(a, b, c) ushr 1) % 1000) / 1000f

private const val TWO_PI = (2.0 * PI).toFloat()

/** Puts the paint back to the plain stroke settings the other animations expect. */
private fun tidy(paint: Paint) {
    paint.shader = null
    paint.strokeCap = Paint.Cap.BUTT
    paint.strokeJoin = Paint.Join.MITER
}

internal object MusicAmbient {
    private val oval = RectF()

    /**
     * The Downward Spiral: two arms of a spiral turn slowly and breathe in and out, descending into the centre, with
     * the odd band of photocopier noise flickering across the screen. [colours]: 0 the thin arm, 1 the heavy arm
     * (rust), 2 a photocopier band.
     */
    fun spiral(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val thin = colourAt(colours, 0, 0x55B89A6C)
        val heavy = colourAt(colours, 1, 0x557A3B2E)
        val band = colourAt(colours, 2, 0x18D4C5B2)
        val cx = w * 0.5f
        val cy = h * 0.58f
        val turns = 5f
        val end = turns * TWO_PI
        val k = sqrt(w * w + h * h) * 0.62f / end
        val phase = frame * 0.018f
        val breathe = 1f + 0.035f * sin(frame * 0.021f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        for (arm in 0 until 2) {
            path.reset()
            var theta = 0.4f
            var first = true
            while (theta < end) {
                val radius = theta * k * breathe
                val a = theta + phase + arm * PI.toFloat()
                val x = cx + cos(a) * radius
                val y = cy + sin(a) * radius * 0.9f
                if (first) path.moveTo(x, y) else path.lineTo(x, y)
                first = false
                theta += 0.1f
            }
            paint.color = if (arm == 0) thin else heavy
            paint.strokeWidth = (if (arm == 0) 1.3f else 2.8f) * d
            canvas.drawPath(path, paint)
        }
        tidy(paint)

        // Photocopier bands: a few full-width streaks that jump to a new place every few frames.
        paint.style = Paint.Style.FILL
        paint.color = band
        val tick = frame / 5
        for (i in 0 until 4) {
            val y = unit(i, tick, 7) * h
            canvas.drawRect(0f, y, w, y + (1f + (hash3(i, tick, 8) % 5)) * d, paint)
        }
    }

    /**
     * A bubble-chamber photograph, like the Is This It sleeve: thin tracks leave a point, curve, and curl tighter and
     * tighter as the particle loses energy, then fade; each cycle a track starts somewhere new. [colours]: the tracks
     * take them in turn (ARGB, so the alpha is how strong they are).
     */
    fun tracks(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val count = 13
        val steps = 150
        val hold = 70
        val cycle = steps + hold
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = 1.2f * d
        for (i in 0 until count) {
            val shifted = frame + hash3(i, 1, 1) % cycle
            val age = shifted % cycle
            val round = shifted / cycle
            val visible = min(age, steps)
            if (visible < 3) continue
            val fade = if (age > steps) 1f - (age - steps) / hold.toFloat() else 1f
            var x = w * (0.1f + 0.8f * unit(i, round, 2))
            var y = h * (0.15f + 0.7f * unit(i, round, 3))
            var angle = unit(i, round, 4) * TWO_PI
            var curve = (unit(i, round, 5) - 0.5f) * 0.05f
            path.reset()
            path.moveTo(x, y)
            for (s in 0 until visible) {
                angle += curve
                curve *= 1.022f
                x += cos(angle) * 2.2f * d
                y += sin(angle) * 2.2f * d
                path.lineTo(x, y)
            }
            val colour = colourAt(colours, i, 0xCCF5B800.toInt())
            paint.color = withAlpha(colour, ((colour ushr 24) * fade).toInt())
            canvas.drawPath(path, paint)
        }
        tidy(paint)
    }

    /**
     * The cross of the Justice sleeve, drawn as the cover draws it: not a filled shape but a spray-painted gold outline
     * round a black block cross seen from above, wide at the foot and narrow at the head, with a few fine lines marking
     * the edges of the arms and the foot. The outline is soft with spray, breathes slowly and glitters a little.
     * Only lines are drawn, so it works over the keys. [colours]: 0 the outline, 1 a faint halo behind it, 2 the fine
     * inner lines (ARGB, so the alpha is how strong each is).
     */
    fun cross(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val line = colourAt(colours, 0, 0xE0E5B23A.toInt())
        val glow = colourAt(colours, 1, 0x18F5D478)
        val inner = colourAt(colours, 2, 0xB0F5D478.toInt())
        val cx = w * 0.5f
        val cy = h * 0.5f
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, h * 0.7f, glow, withAlpha(glow, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // The outline, in the cover's own coordinates (a 600 unit square; the cross spans x 150..450, y 97..512).
        val xs = floatArrayOf(275f, 332f, 338f, 432f, 447f, 432f, 372f, 402f, 388f, 213f, 198f, 228f, 168f, 152f, 165f, 268f)
        val ys = floatArrayOf(97f, 97f, 145f, 153f, 185f, 262f, 268f, 422f, 510f, 510f, 422f, 268f, 262f, 185f, 153f, 145f)
        // The fine lines inside it: x1, y1, x2, y2 for each.
        val lines = floatArrayOf(
            152f, 185f, 255f, 187f, // the left arm
            345f, 187f, 447f, 185f, // the right arm
            255f, 187f, 228f, 268f, // the left edge of the stem
            345f, 187f, 372f, 268f, // the right edge of the stem
            198f, 422f, 402f, 422f, // the top of the foot
        )
        val scale = h * 0.92f / 415f
        fun px(x: Float) = cx + (x - 300f) * scale
        fun py(y: Float) = cy + (y - 304f) * scale

        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        val breath = 0.75f + 0.25f * sin(frame * 0.08f)
        path.reset()
        for (i in xs.indices) if (i == 0) path.moveTo(px(xs[i]), py(ys[i])) else path.lineTo(px(xs[i]), py(ys[i]))
        path.close()
        // A wide soft glow, then the outline itself.
        paint.strokeWidth = 9f * d
        paint.color = withAlpha(line, ((line ushr 24) * 0.22f * breath).toInt())
        canvas.drawPath(path, paint)
        paint.strokeWidth = 4.5f * d
        paint.color = withAlpha(line, ((line ushr 24) * 0.5f * breath).toInt())
        canvas.drawPath(path, paint)
        paint.strokeWidth = 2.2f * d
        paint.color = withAlpha(line, ((line ushr 24) * (0.8f + 0.2f * breath)).toInt())
        canvas.drawPath(path, paint)

        paint.strokeWidth = max(1f, 1.2f * d)
        paint.color = inner
        for (k in 0 until lines.size / 4) {
            canvas.drawLine(px(lines[k * 4]), py(lines[k * 4 + 1]), px(lines[k * 4 + 2]), py(lines[k * 4 + 3]), paint)
        }

        // The spray: specks scattered along the outline, a few of them glittering.
        paint.style = Paint.Style.FILL
        for (e in xs.indices) {
            val x1 = px(xs[e])
            val y1 = py(ys[e])
            val x2 = px(xs[(e + 1) % xs.size])
            val y2 = py(ys[(e + 1) % xs.size])
            for (k in 0 until 14) {
                val t = unit(e, k, 81)
                val off = (unit(e, k, 82) - 0.5f) * 12f * d
                val ang = unit(e, k, 83) * TWO_PI
                val twinkle = 0.45f + 0.55f * sin(frame * 0.2f + e * 3f + k * 1.7f).let { it * it }
                paint.color = withAlpha(line, ((line ushr 24) * 0.7f * twinkle).toInt())
                canvas.drawCircle(
                    x1 + (x2 - x1) * t + cos(ang) * off, y1 + (y2 - y1) * t + sin(ang) * off,
                    (0.5f + unit(e, k, 84)) * d, paint,
                )
            }
        }
        tidy(paint)
    }

    /**
     * The White Stripes peppermint, enlarged until it fills the whole keyboard (so no outer rim shows): twisted red
     * and white wedges turning slowly. [colours]: 0 the red, 1 the white.
     */
    fun peppermint(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val red = colourAt(colours, 0, 0xFFD61A21.toInt())
        val white = colourAt(colours, 1, 0xFFF8F7F4.toInt())
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = sqrt(w * w + h * h) * 0.5f + 4f * d // reaches past every corner
        val turn = frame * 0.012f
        val wedges = 8
        val twist = 1.15f
        val steps = 14
        val width = TWO_PI / wedges / 2f

        paint.style = Paint.Style.FILL
        paint.color = white
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.color = red
        for (i in 0 until wedges) {
            val base = turn + i * TWO_PI / wedges
            path.reset()
            path.moveTo(cx, cy)
            for (s in 1..steps) {
                val r = radius * s / steps
                val a = base + twist * r / radius
                path.lineTo(cx + cos(a) * r, cy + sin(a) * r)
            }
            for (s in steps downTo 1) {
                val r = radius * s / steps
                val a = base + width + twist * r / radius
                path.lineTo(cx + cos(a) * r, cy + sin(a) * r)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
        tidy(paint)
    }

    /**
     * A flood of colour that changes every second or so, as the channel idents did: the whole background is one
     * colour, held, then blended quickly into the next. [colours]: the colours, taken in turn.
     */
    fun colourCycle(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, colours: IntArray) {
        if (colours.isEmpty()) return
        val hold = 14
        val blend = 4
        val step = frame / hold
        val within = frame % hold
        val t = (within - (hold - blend)).coerceAtLeast(0) / blend.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = mixArgb(colours[step % colours.size], colours[(step + 1) % colours.size], t)
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    /**
     * Walthamstow Stadium, from the cover of Parklife: a strip of mown turf across the lower half with a white rail
     * along its top. Still; the dogs are [dogs], drawn over the keys. [colours]: 0 the turf, 1 its lighter mown
     * stripe, 2 the rail.
     */
    fun turf(canvas: Canvas, paint: Paint, w: Float, h: Float, d: Float, colours: IntArray) {
        val turf = colourAt(colours, 0, 0xFF2F7C4E.toInt())
        val mown = colourAt(colours, 1, 0xFF3A8D58.toInt())
        val rail = colourAt(colours, 2, 0xFFF7F0E1.toInt())
        val top = h * 0.55f
        paint.style = Paint.Style.FILL
        paint.color = turf
        canvas.drawRect(0f, top, w, h, paint)
        paint.color = mown
        var y = top
        var band = 0
        while (y < h) {
            if (band % 2 == 0) canvas.drawRect(0f, y, w, min(y + 14f * d, h), paint)
            y += 14f * d
            band++
        }
        paint.color = rail
        canvas.drawRect(0f, top - 2.5f * d, w, top + 0.5f * d, paint)
        var post = 10f * d
        while (post < w) {
            canvas.drawRect(post, top - 9f * d, post + 1.6f * d, top, paint)
            post += 40f * d
        }
    }

    /**
     * Four greyhounds in coloured jackets racing across the keyboard at different speeds, in lanes down the lower
     * half. Drawn over the keys so they can be seen; give the colours some transparency so the lettering still
     * reads. [colours]: 0 the dogs, then one jacket colour for each dog.
     */
    fun dogs(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val coat = colourAt(colours, 0, 0xB01A2B40.toInt())
        val top = h * 0.55f
        val size = 48f * d
        val span = w + size * 3f
        for (i in 0 until 4) {
            val speed = (1.7f + i * 0.4f) * d
            val x = (unit(i, 3, 3) * span + frame * speed) % span - size * 1.5f
            val lane = top + (h - top) * (0.14f + i * 0.22f)
            dog(canvas, paint, path, x, lane, size, frame * 0.5f + i * 1.7f, coat, colourAt(colours, 1 + i, 0xB0D12328.toInt()))
        }
        tidy(paint)
    }

    /**
     * A moonlit cathedral, for Fallen: a pointed arch frames a great rose window whose pale moon glows and pulses,
     * its tracery slowly turning, with shafts of light swaying down from it and low mist drifting across the floor.
     * [colours]: 0 the stonework and tracery, 1 the moon glow, 2 the light shafts, 3 the mist.
     */
    fun cathedral(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val stone = colourAt(colours, 0, 0x66587C9A)
        val moon = colourAt(colours, 1, 0x66B5CCDA)
        val shaft = colourAt(colours, 2, 0x22B5CCDA)
        val mist = colourAt(colours, 3, 0x30B5CCDA)
        val cx = w * 0.5f
        val cy = h * 0.32f
        val radius = min(w * 0.34f, h * 0.36f)
        val pulse = 0.85f + 0.15f * sin(frame * 0.06f)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, radius * 1.9f, withAlpha(moon, ((moon ushr 24) * pulse).toInt()), withAlpha(moon, 0), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // The shafts of light from the window, swaying.
        paint.color = shaft
        for (i in 0 until 5) {
            val lean = (i - 2) * 0.22f + sin(frame * 0.03f + i) * 0.06f
            val foot = cx + sin(lean) * h
            val spread = (0.05f + 0.01f * i) * w
            path.reset()
            path.moveTo(cx - radius * 0.25f, cy + radius * 0.6f)
            path.lineTo(cx + radius * 0.25f, cy + radius * 0.6f)
            path.lineTo(foot + spread, h)
            path.lineTo(foot - spread, h)
            path.close()
            canvas.drawPath(path, paint)
        }

        // The pointed arch round the window, and the rose window's rings and turning tracery.
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = stone
        paint.strokeWidth = 3f * d
        val archW = radius * 1.35f
        path.reset()
        path.moveTo(cx - archW, h)
        path.lineTo(cx - archW, cy + radius * 0.4f)
        path.quadTo(cx - archW, cy - radius * 1.1f, cx, cy - radius * 1.55f)
        path.quadTo(cx + archW, cy - radius * 1.1f, cx + archW, cy + radius * 0.4f)
        path.lineTo(cx + archW, h)
        canvas.drawPath(path, paint)
        paint.strokeWidth = 2f * d
        canvas.drawCircle(cx, cy, radius, paint)
        canvas.drawCircle(cx, cy, radius * 0.8f, paint)
        canvas.drawCircle(cx, cy, radius * 0.2f, paint)
        paint.strokeWidth = 1.2f * d
        val turn = frame * 0.004f
        val petals = 12
        for (i in 0 until petals) {
            val a = turn + i * TWO_PI / petals
            canvas.drawLine(cx + cos(a) * radius * 0.2f, cy + sin(a) * radius * 0.2f, cx + cos(a) * radius * 0.8f, cy + sin(a) * radius * 0.8f, paint)
            canvas.drawCircle(cx + cos(a + TWO_PI / petals / 2f) * radius * 0.52f, cy + sin(a + TWO_PI / petals / 2f) * radius * 0.52f, radius * 0.17f, paint)
        }

        // Mist drifting along the floor.
        paint.style = Paint.Style.FILL
        paint.color = mist
        for (i in 0 until 4) {
            val bandW = w * (0.7f + 0.15f * i)
            val x = ((frame * (0.5f + 0.25f * i) * d) + i * w * 0.4f) % (w + bandW) - bandW
            oval.set(x, h * (0.72f + 0.06f * i), x + bandW, h * (0.72f + 0.06f * i) + h * 0.16f)
            canvas.drawOval(oval, paint)
        }
        tidy(paint)
    }

    /**
     * Pale feathers drifting down the keyboard, turning as they fall and swaying from side to side. Drawn over the
     * keys; give the colour some transparency. [colours]: the feathers take them in turn.
     */
    fun feathers(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val count = 8
        for (i in 0 until count) {
            val size = (14f + unit(i, 1, 61) * 8f) * d
            val fall = (3.2f + unit(i, 2, 61) * 2.4f) * d
            val y = (unit(i, 3, 61) * (h + size * 4f) + frame * fall * 0.5f) % (h + size * 4f) - size * 2f
            val x = w * (0.05f + 0.9f * unit(i, 4, 61)) + sin(frame * 0.05f + i * 2.1f) * 14f * d
            val angle = sin(frame * 0.045f + i) * 40f + 20f * (i % 3 - 1)
            MusicTap.feather(canvas, paint, oval, x, y, size, angle, colourAt(colours, i, 0x80EFF3F6.toInt()), d)
        }
        tidy(paint)
    }

    /**
     * Stop-motion clay, for Primus: lumpy blobs of clay that drift about the background and are re-modelled a little
     * every few frames, so their outlines jerk the way a claymation does. [colours]: the blobs take them in turn
     * (ARGB, so the alpha is how strong they are).
     */
    fun claymation(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val step = frame / 4 // about three drawings a second
        paint.style = Paint.Style.FILL
        for (i in 0 until 9) {
            val r = (22f + unit(i, 1, 71) * 26f) * d
            val x = w * unit(i, 2, 71) + sin(step * 0.5f + i * 1.7f) * 6f * d
            val y = h * unit(i, 3, 71) + cos(step * 0.4f + i * 2.3f) * 6f * d
            val points = 11
            path.reset()
            for (k in 0 until points) {
                val a = k * TWO_PI / points
                val rr = r * (0.78f + 0.4f * unit(i * 31 + k, step, 72))
                val px = x + cos(a) * rr
                val py = y + sin(a) * rr
                if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            val colour = colourAt(colours, i, 0x55C48A31)
            paint.color = colour
            canvas.drawPath(path, paint)
            // A thumbprint pressed into it.
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = max(1f, 0.9f * d)
            paint.color = withAlpha(0xFF000000.toInt(), (colour ushr 24) / 2)
            for (k in 1..3) canvas.drawCircle(x - r * 0.15f, y + r * 0.1f, r * 0.12f * k, paint)
            paint.style = Paint.Style.FILL
        }
        tidy(paint)
    }

    /**
     * A still halftone screen, like the print of a rock magazine: staggered dots that grow from nothing at the top
     * left to nearly touching at the bottom right. [colours]: 0 the dots.
     */
    fun halftone(canvas: Canvas, paint: Paint, w: Float, h: Float, d: Float, colours: IntArray) {
        val dots = colourAt(colours, 0, 0x33D91E2A)
        val cell = 11f * d
        paint.style = Paint.Style.FILL
        paint.color = dots
        val rows = (h / (cell * 0.5f)).toInt() + 2
        val cols = (w / cell).toInt() + 2
        for (r in 0 until rows) {
            val y = r * cell * 0.5f
            val offset = if (r % 2 == 0) 0f else cell * 0.5f
            for (c in 0 until cols) {
                val x = c * cell + offset
                val t = (x / w * 0.6f + y / h * 0.8f).coerceIn(0f, 1f)
                val radius = cell * 0.5f * t * t
                if (radius > 0.3f * d) canvas.drawCircle(x, y, radius, paint)
            }
        }
    }

    /**
     * One greyhound facing right, [size] long, in mid-stride at [phase] (a gallop: the fore legs reach out as the hind
     * legs tuck under, and the body rises and falls), in the [coat] colour wearing a jacket of [jacket].
     */
    internal fun dog(canvas: Canvas, paint: Paint, path: Path, x: Float, y: Float, size: Float, phase: Float, coat: Int, jacket: Int) {
        val s = size
        val gy = y - abs(sin(phase)) * 0.07f * s
        paint.style = Paint.Style.FILL
        paint.color = coat

        // The body: a long arched back, a deep chest and a tucked-up waist.
        path.reset()
        path.moveTo(x - 0.38f * s, gy - 0.02f * s)
        path.quadTo(x - 0.14f * s, gy - 0.19f * s, x + 0.14f * s, gy - 0.11f * s)
        path.lineTo(x + 0.30f * s, gy - 0.06f * s)
        path.quadTo(x + 0.37f * s, gy + 0.05f * s, x + 0.24f * s, gy + 0.11f * s)
        path.quadTo(x + 0.04f * s, gy + 0.04f * s, x - 0.16f * s, gy - 0.01f * s)
        path.quadTo(x - 0.32f * s, gy + 0.08f * s, x - 0.38f * s, gy - 0.02f * s)
        path.close()
        canvas.drawPath(path, paint)

        // The jacket across the ribs.
        paint.color = jacket
        path.reset()
        path.moveTo(x - 0.08f * s, gy - 0.145f * s)
        path.lineTo(x + 0.04f * s, gy - 0.125f * s)
        path.lineTo(x + 0.06f * s, gy + 0.02f * s)
        path.lineTo(x - 0.06f * s, gy - 0.02f * s)
        path.close()
        canvas.drawPath(path, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = coat
        // The long neck, the head and snout held forward, and the tail streaming behind.
        paint.strokeWidth = 0.10f * s
        canvas.drawLine(x + 0.26f * s, gy - 0.06f * s, x + 0.42f * s, gy - 0.15f * s, paint)
        paint.strokeWidth = 0.075f * s
        canvas.drawLine(x + 0.43f * s, gy - 0.155f * s, x + 0.50f * s, gy - 0.155f * s, paint)
        paint.strokeWidth = 0.045f * s
        canvas.drawLine(x + 0.50f * s, gy - 0.15f * s, x + 0.64f * s, gy - 0.12f * s, paint)
        paint.strokeWidth = 0.035f * s
        canvas.drawLine(x - 0.37f * s, gy - 0.02f * s, x - 0.58f * s, gy + 0.03f * s, paint)

        // Four long legs, each in two parts. The fore legs reach forward as the hind legs push back, and again.
        paint.strokeWidth = 0.05f * s
        for (leg in 0 until 4) {
            val fore = leg < 2
            val ax = if (fore) x + 0.24f * s else x - 0.30f * s
            val ay = gy + 0.06f * s
            val swing = sin(phase + (if (fore) 0f else PI.toFloat()) + (leg % 2) * 0.35f) * 0.9f
            val upper = 0.20f * s
            val kx = ax + sin(swing) * upper
            val ky = ay + cos(swing) * upper
            val bend = swing * 0.3f + (if (fore) -0.55f else -0.35f)
            path.reset()
            path.moveTo(ax, ay)
            path.lineTo(kx, ky)
            path.lineTo(kx + sin(bend) * upper, ky + cos(bend) * upper)
            canvas.drawPath(path, paint)
        }
    }
}

/** The tap effects added for the music and TV themes ([TapEffectKind.STARBURST] and so on), drawn by [TapEffectDrawable]. */
internal object MusicTap {
    /** One pale feather: a slim vane with a shaft that runs a little beyond it. [size] is its length, [angle] in degrees. */
    fun feather(canvas: Canvas, paint: Paint, oval: RectF, x: Float, y: Float, size: Float, angle: Float, colour: Int, d: Float) {
        paint.color = colour
        paint.style = Paint.Style.FILL
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(angle)
        oval.set(-size * 0.16f, -size * 0.5f, size * 0.16f, size * 0.5f)
        canvas.drawOval(oval, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, 0.9f * d)
        canvas.drawLine(0f, -size * 0.62f, 0f, size * 0.7f, paint)
        canvas.restore()
        paint.style = Paint.Style.FILL
    }

    /**
     * A comic-book "POW": a jagged burst that pops out larger than its final size, settles, and fades, with speed
     * lines round it. [fill] is its colour (one per tap), the outline is always near-black ink and a smaller burst of
     * a lighter shade sits inside. [t] is 0 at the tap to 1 as it fades.
     */
    fun starburst(
        canvas: Canvas, paint: Paint, path: Path, x: Float, y: Float, radius: Float, seed: Float, t: Float, d: Float, fill: Int,
    ) {
        val pop = if (t < 0.18f) 0.3f + 0.85f * (t / 0.18f) else if (t < 0.35f) 1.15f - 0.15f * ((t - 0.18f) / 0.17f) else 1f + 0.05f * (t - 0.35f)
        val alpha = (255f * min(1f, (1f - t) / 0.35f)).toInt().coerceIn(0, 255)
        val ink = withAlpha(0xFF111316.toInt(), alpha)
        val seedInt = (seed * 1000f).toInt()
        fun burst(scale: Float) {
            path.reset()
            val points = 12
            for (i in 0 until points * 2) {
                val a = i * PI.toFloat() / points + seed
                val r = radius * pop * scale * (if (i % 2 == 0) 0.8f + 0.3f * unit(i, seedInt, 81) else 0.52f)
                val px = x + cos(a) * r
                val py = y + sin(a) * r
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }
        paint.strokeJoin = Paint.Join.MITER
        burst(1f)
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(fill, alpha)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.2f * d
        paint.color = ink
        canvas.drawPath(path, paint)
        burst(0.55f)
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(lighten(fill, 0.55f), alpha)
        canvas.drawPath(path, paint)
        if (t < 0.5f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f * d
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = ink
            for (i in 0 until 8) {
                val a = i * TWO_PI / 8f + seed * 0.5f
                canvas.drawLine(x + cos(a) * radius * 1.2f * pop, y + sin(a) * radius * 1.2f * pop, x + cos(a) * radius * 1.5f * pop, y + sin(a) * radius * 1.5f * pop, paint)
            }
        }
        tidy(paint)
    }

    /**
     * One particle track from a bubble-chamber photograph: it leaves the tap, curves, and curls tighter and tighter
     * as it loses energy, growing over the first half of its life and fading over the last. [angle] is the way it
     * sets off and [curve] how fast it bends (radians per step; the sign is the direction).
     */
    fun track(
        canvas: Canvas, paint: Paint, path: Path, x: Float, y: Float, angle: Float, curve: Float, t: Float, d: Float, colour: Int,
    ) {
        val steps = (80f * min(1f, t / 0.55f)).toInt()
        if (steps < 2) return
        val alpha = ((colour ushr 24) * min(1f, (1f - t) / 0.4f)).toInt().coerceIn(0, 255)
        var a = angle
        var k = curve
        var px = x
        var py = y
        path.reset()
        path.moveTo(px, py)
        for (s in 0 until steps) {
            a += k
            k *= 1.03f
            px += cos(a) * 2.2f * d
            py += sin(a) * 2.2f * d
            path.lineTo(px, py)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = 1.6f * d
        paint.color = withAlpha(colour, alpha)
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(px, py, 1.9f * d, paint)
        tidy(paint)
    }

    /**
     * A lump of clay flung from the tap. Like a stop-motion figure it only moves a few times a second: [step] is the
     * frame it is on, and it squashes and stretches from one frame to the next. [oval] is scratch space.
     */
    fun lump(canvas: Canvas, paint: Paint, oval: RectF, x: Float, y: Float, radius: Float, step: Int, alpha: Int, colour: Int) {
        val squash = if (step % 2 == 0) 1.18f else 0.85f
        val rw = radius * squash
        val rh = radius / squash
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(darken(colour, 0.7f), alpha)
        oval.set(x - rw, y - rh + radius * 0.2f, x + rw, y + rh + radius * 0.2f)
        canvas.drawOval(oval, paint)
        paint.color = withAlpha(colour, alpha)
        oval.set(x - rw, y - rh, x + rw, y + rh)
        canvas.drawOval(oval, paint)
        paint.color = withAlpha(0xFFFFFFFF.toInt(), alpha / 3)
        oval.set(x - rw * 0.6f, y - rh * 0.65f, x, y - rh * 0.1f)
        canvas.drawOval(oval, paint)
        paint.color = withAlpha(0xFF000000.toInt(), alpha / 4)
        oval.set(x + rw * 0.05f, y + rh * 0.05f, x + rw * 0.5f, y + rh * 0.45f)
        canvas.drawOval(oval, paint)
    }
}
