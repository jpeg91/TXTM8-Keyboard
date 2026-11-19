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
     * The cross of the Justice sleeve: a huge gold † standing in a halo that slowly pulses, with long rays of light
     * turning behind it. [colours]: 0 the cross, 1 the halo, 2 the rays (ARGB, so the alpha is how strong they are).
     */
    fun cross(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val body = colourAt(colours, 0, 0xFFE5B23A.toInt())
        val glow = colourAt(colours, 1, 0xFFF5D478.toInt())
        val ray = colourAt(colours, 2, 0x22F5D478)
        val stem = h * 0.07f
        val barY = h * 0.22f
        val cx = w * 0.5f
        val cy = barY + stem * 0.45f
        val pulse = 0.75f + 0.25f * sin(frame * 0.05f)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, h * 0.85f,
            withAlpha(glow, ((glow ushr 24) * pulse).toInt()), withAlpha(glow, 0), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.color = ray
        val reach = sqrt(w * w + h * h)
        val turn = frame * 0.004f
        val rays = 14
        for (i in 0 until rays) {
            val a = turn + i * TWO_PI / rays
            path.reset()
            path.moveTo(cx, cy)
            path.lineTo(cx + cos(a - 0.05f) * reach, cy + sin(a - 0.05f) * reach)
            path.lineTo(cx + cos(a + 0.05f) * reach, cy + sin(a + 0.05f) * reach)
            path.close()
            canvas.drawPath(path, paint)
        }

        val top = h * 0.03f
        val bottom = h * 0.97f
        paint.shader = LinearGradient(0f, top, 0f, bottom, lighten(body, 0.35f), body, Shader.TileMode.CLAMP)
        canvas.drawRect(cx - stem / 2f, top, cx + stem / 2f, bottom, paint)
        canvas.drawRect(cx - h * 0.27f, barY, cx + h * 0.27f, barY + stem * 0.9f, paint)
        tidy(paint)
    }

    /**
     * The White Stripes peppermint: a candy disc of twisted red and white wedges, turning slowly, with a black rim.
     * [colours]: 0 the red, 1 the white, 2 the rim.
     */
    fun peppermint(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val red = colourAt(colours, 0, 0xFFD61A21.toInt())
        val white = colourAt(colours, 1, 0xFFF8F7F4.toInt())
        val rim = colourAt(colours, 2, 0xFF101010.toInt())
        val cx = w * 0.5f
        val cy = h * 0.5f
        val radius = min(w * 0.46f, h * 0.62f)
        val turn = frame * 0.012f
        val wedges = 8
        val twist = 1.15f
        val steps = 14
        val width = TWO_PI / wedges / 2f

        paint.style = Paint.Style.FILL
        paint.color = white
        canvas.drawCircle(cx, cy, radius, paint)
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
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * d
        paint.color = rim
        canvas.drawCircle(cx, cy, radius, paint)
        tidy(paint)
    }

    /**
     * The MTV logo: a big blocky M, tilted, with the scrawled "TV" over its lower right. The colours change every
     * second or so, as the idents did; the scrawl is redrawn a little differently every few frames, like hand-drawn
     * animation. [colours]: the logo colours, taken in turn.
     */
    fun mtvLogo(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        if (colours.isEmpty()) return
        val step = frame / 14
        val letter = colours[step % colours.size]
        val scrawl = colours[(step + 2) % colours.size]
        val tall = h * 0.62f
        val wide = tall * 1.15f
        val x0 = -wide / 2f
        val y0 = -tall / 2f
        canvas.save()
        canvas.translate(w * 0.5f, h * 0.5f)
        canvas.rotate(-6f)

        path.reset()
        path.moveTo(x0, y0 + tall)
        path.lineTo(x0, y0)
        path.lineTo(x0 + wide * 0.22f, y0)
        path.lineTo(x0 + wide * 0.5f, y0 + tall * 0.5f)
        path.lineTo(x0 + wide * 0.78f, y0)
        path.lineTo(x0 + wide, y0)
        path.lineTo(x0 + wide, y0 + tall)
        path.lineTo(x0 + wide * 0.78f, y0 + tall)
        path.lineTo(x0 + wide * 0.78f, y0 + tall * 0.34f)
        path.lineTo(x0 + wide * 0.5f, y0 + tall * 0.82f)
        path.lineTo(x0 + wide * 0.22f, y0 + tall * 0.34f)
        path.lineTo(x0 + wide * 0.22f, y0 + tall)
        path.close()
        paint.style = Paint.Style.FILL
        paint.color = letter
        canvas.drawPath(path, paint)

        // The scrawl.
        val boil = frame / 3
        fun jitter(i: Int) = ((hash3(boil, i, 9) % 5) - 2) * 0.5f * d
        val tx = x0 + wide * 0.46f
        val ty = y0 + tall * 0.66f
        val tw = wide * 0.22f
        val th = tall * 0.28f
        val vx = tx + tw * 1.25f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = tall * 0.05f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = scrawl
        canvas.drawLine(tx + jitter(0), ty + jitter(1), tx + tw + jitter(2), ty + jitter(3), paint)
        canvas.drawLine(tx + tw / 2f + jitter(4), ty + jitter(5), tx + tw / 2f + jitter(6), ty + th + jitter(7), paint)
        canvas.drawLine(vx + jitter(8), ty + jitter(9), vx + tw / 2f + jitter(10), ty + th + jitter(11), paint)
        canvas.drawLine(vx + tw / 2f + jitter(12), ty + th + jitter(13), vx + tw + jitter(14), ty + jitter(15), paint)
        canvas.restore()
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
     * A parquet floor, like the bedroom of the Definitely Maybe sleeve: squares of four planks, each square turned
     * across the one next to it, every plank its own shade of wood. Still. [colours]: 0 and 1 the lightest and
     * darkest plank, 2 the gaps between them.
     */
    fun parquet(canvas: Canvas, paint: Paint, w: Float, h: Float, d: Float, colours: IntArray) {
        val light = colourAt(colours, 0, 0xFFB37B40.toInt())
        val dark = colourAt(colours, 1, 0xFF8F5E2E.toInt())
        val gap = colourAt(colours, 2, 0xFF4A2E14.toInt())
        val side = 30f * d
        val plank = side / 4f
        val seam = 0.6f * d
        paint.style = Paint.Style.FILL
        paint.color = gap
        canvas.drawRect(0f, 0f, w, h, paint)
        val cols = (w / side).toInt() + 1
        val rows = (h / side).toInt() + 1
        for (gy in 0 until rows) {
            for (gx in 0 until cols) {
                val x = gx * side
                val y = gy * side
                val across = (gx + gy) % 2 == 0
                for (p in 0 until 4) {
                    paint.color = mixArgb(light, dark, (hash3(gx, gy, p) % 100) / 100f)
                    if (across) {
                        canvas.drawRect(x + seam, y + p * plank + seam, x + side - seam, y + (p + 1) * plank - seam, paint)
                    } else {
                        canvas.drawRect(x + p * plank + seam, y + seam, x + (p + 1) * plank - seam, y + side - seam, paint)
                    }
                }
            }
        }
    }

    /**
     * Walthamstow Stadium, from the cover of Parklife: a mown strip of turf with a white rail along its top and four
     * greyhounds in coloured jackets racing across it at different speeds. [colours]: 0 the turf, 1 its lighter mown
     * stripe, 2 the rail, 3 the dogs, then one jacket colour for each dog.
     */
    fun greyhounds(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val turf = colourAt(colours, 0, 0xFF2F7C4E.toInt())
        val mown = colourAt(colours, 1, 0xFF3A8D58.toInt())
        val rail = colourAt(colours, 2, 0xFFF7F0E1.toInt())
        val coat = colourAt(colours, 3, 0xFF1A2B40.toInt())
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

        // The rail, with a post every so often.
        paint.color = rail
        canvas.drawRect(0f, top - 2.5f * d, w, top + 0.5f * d, paint)
        var post = 10f * d
        while (post < w) {
            canvas.drawRect(post, top - 9f * d, post + 1.6f * d, top, paint)
            post += 40f * d
        }

        // The dogs, the nearest lane drawn last.
        val size = 44f * d
        val span = w + size * 3f
        for (i in 0 until 4) {
            val speed = (1.5f + i * 0.35f) * d
            val x = (unit(i, 3, 3) * span + frame * speed) % span - size * 1.5f
            val lane = top + (h - top) * (0.18f + i * 0.2f)
            dog(canvas, paint, path, x, lane, size, frame * 0.5f + i * 1.7f, coat, colourAt(colours, 4 + i, 0xFFD12328.toInt()))
        }
        tidy(paint)
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
