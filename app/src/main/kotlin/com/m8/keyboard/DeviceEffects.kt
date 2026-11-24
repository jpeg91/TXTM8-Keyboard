// app/src/main/kotlin/com/m8/keyboard/DeviceEffects.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import kotlin.math.sin

private fun colourAt(colours: IntArray, i: Int, fallback: Int): Int =
    if (colours.isEmpty()) fallback else colours[i % colours.size]

/** The ambient animations for the themes that copy a phone or a piece of software. Each is drawn by [AmbientBackground]. */
internal object DeviceAmbient {
    /**
     * A keypad's backlight: a faint even glow over everything, and a soft band of brighter light that rolls up the keyboard
     * from the bottom to the top and then starts again, with a second, dimmer one a little behind it, like the keypad of a
     * phone lighting up. Meant to show through keys of glass. [colours]: 0 the light (ARGB; its alpha is the strongest the
     * band gets).
     */
    fun backlight(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, colours: IntArray) {
        val light = if (colours.isNotEmpty()) colours[0] else 0x3022B8E8
        val strength = (light ushr 24).toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = withAlpha(light, (strength * 0.22f).toInt())
        canvas.drawRect(0f, 0f, w, h, paint)

        for (band in 0 until 2) {
            val progress = ((frame * 0.0065f + band * 0.37f) % 1.5f) - 0.25f
            val centre = h * (1f - progress)
            val half = h * 0.28f
            val alpha = (strength * (if (band == 0) 1f else 0.55f) * (0.75f + 0.25f * sin(frame * 0.05f + band))).toInt()
            paint.shader = LinearGradient(
                0f, centre - half, 0f, centre + half,
                intArrayOf(withAlpha(light, 0), withAlpha(light, alpha), withAlpha(light, 0)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, centre - half, w, centre + half, paint)
        }
        paint.shader = null
    }

    /**
     * The border of a home computer loading from tape: for a moment after each key press, bands of colour race down the
     * whole keyboard, first an even pilot tone of two colours and then ragged data bands of two others, and then it cuts
     * back to the plain background. [sinceMs] is how long ago the key was pressed. [colours]: 0 and 1 the pilot tone, 2
     * and 3 the data.
     */
    fun tapeLoading(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, sinceMs: Long, d: Float, colours: IntArray) {
        if (sinceMs < 0 || sinceMs > 650) return
        paint.style = Paint.Style.FILL
        paint.shader = null
        val pilot = sinceMs < 260
        val a = colourAt(colours, if (pilot) 0 else 2, if (pilot) 0xFFD70000.toInt() else 0xFFD7D700.toInt())
        val b = colourAt(colours, if (pilot) 1 else 3, if (pilot) 0xFF00D7D7.toInt() else 0xFF0000D7.toInt())
        var y = -((frame * 5) % 12) * d
        var i = 0
        while (y < h) {
            val band = if (pilot) 6f * d else (2f + (hash3(i, frame / 2, 5) % 8)) * d
            paint.color = if (i % 2 == 0) a else b
            canvas.drawRect(0f, y, w, y + band, paint)
            y += band
            i++
        }
    }

    /**
     * A coarse mosaic of square tiles in [colours], each tile picking one of them by chance but always the same one, like
     * the earth texture of a block-building game. Still, so it is drawn once and rests.
     */
    fun pixelTiles(canvas: Canvas, paint: Paint, w: Float, h: Float, d: Float, colours: IntArray) {
        val cell = 8f * d
        paint.style = Paint.Style.FILL
        paint.shader = null
        var j = 0
        var y = 0f
        while (y < h) {
            var i = 0
            var x = 0f
            while (x < w) {
                paint.color = colourAt(colours, hash3(i, j, 17) % maxOf(1, colours.size), 0xFF5A3E24.toInt())
                canvas.drawRect(x, y, x + cell + 0.5f, y + cell + 0.5f, paint)
                x += cell
                i++
            }
            y += cell
            j++
        }
    }
    /** [c] with its alpha scaled by [f] (0..1). */
    private fun fade(c: Int, f: Float): Int =
        ((((c ushr 24) * f).toInt().coerceIn(0, 255)) shl 24) or (c and 0xFFFFFF)

    /**
     * The sleeve of Unknown Pleasures: rows of white line, each a flat run that swells into a jagged peak in the middle, drawn from
     * the top down with every row hiding the ones behind it. Still, so it is drawn once and rests. [colours]: the line, then the
     * background it hides with (black when missing).
     */
    fun pulsar(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, d: Float, colours: IntArray) {
        val line = colourAt(colours, 0, 0xFFFFFFFF.toInt())
        val bg = colourAt(colours, 1, 0xFF000000.toInt())
        val rows = 30
        val steps = 48
        val left = w * 0.14f
        val right = w * 0.86f
        val top = h * 0.12f
        val gap = (h * 0.84f - top) / rows
        paint.shader = null
        for (r in 0 until rows) {
            val base = top + gap * (r + 1)
            for (pass in 0..1) {
                path.reset()
                path.moveTo(left, base)
                for (i in 1 until steps) {
                    val u = i.toFloat() / steps
                    val e = (1f - ((u - 0.5f) / 0.3f) * ((u - 0.5f) / 0.3f)).coerceAtLeast(0f)
                    val n = (hash3(i, r, 3) % 1000) / 1000f
                    path.lineTo(left + (right - left) * u, base - e * e * gap * 4.2f * (0.15f + 0.85f * n * n))
                }
                path.lineTo(right, base)
                if (pass == 0) {
                    path.lineTo(right, h)
                    path.lineTo(left, h)
                    path.close()
                    paint.style = Paint.Style.FILL
                    paint.color = bg
                } else {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1.3f * d
                    paint.color = line
                }
                canvas.drawPath(path, paint)
            }
        }
        paint.style = Paint.Style.FILL
    }

    /**
     * A video tape that is tracking badly: a band of white streaks that rolls up the screen every few seconds, thin tears that
     * jump about, and a permanent strip of head-switching noise along the bottom. [colours]: the noise colour.
     */
    fun vhs(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val c = colourAt(colours, 0, 0xFFFFFFFF.toInt())
        paint.shader = null
        paint.style = Paint.Style.FILL
        val period = 80
        val y0 = h - ((frame % period).toFloat() / period) * (h + 40f * d)
        paint.color = fade(c, 0.07f)
        canvas.drawRect(0f, y0, w, y0 + 26f * d, paint)
        for (k in 0 until 18) {
            val sx = (hash3(k, frame, 1) % 1000) / 1000f * w
            val len = (0.08f + (hash3(k, frame, 2) % 100) / 100f * 0.5f) * w
            paint.color = fade(c, 0.12f + (hash3(k, frame, 3) % 40) / 100f)
            canvas.drawRect(sx, y0 + k * 1.4f * d, sx + len, y0 + k * 1.4f * d + 1.5f * d, paint)
        }
        for (k in 0 until 2) {
            val ty = (hash3(frame / 2, k, 4) % 1000) / 1000f * h
            paint.color = fade(c, 0.18f)
            canvas.drawRect(0f, ty, w, ty + d, paint)
        }
        for (k in 0 until 26) {
            val sx = (hash3(k, frame, 5) % 1000) / 1000f * w
            val len = (0.03f + (hash3(k, frame, 6) % 100) / 100f * 0.25f) * w
            val yy = h - 9f * d + (hash3(k, frame, 7) % 9) * d
            paint.color = fade(c, 0.2f + (hash3(k, frame, 8) % 50) / 100f)
            canvas.drawRect(sx, yy, sx + len, yy + d * 1.2f, paint)
        }
    }

    /**
     * A rainy window at night: soft out-of-focus lights drifting in brightness behind the glass, small still beads of water, and
     * a few fat drops that run down in fits and starts, each leaving a faint trail. [colours]: the lights.
     */
    fun windowRain(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        for (k in 0 until 9) {
            val x = (hash3(k, 1, 31) % 1000) / 1000f * w
            val y = (hash3(k, 2, 31) % 1000) / 1000f * h
            val r = (14f + hash3(k, 3, 31) % 14) * d
            val glow = 0.6f + 0.4f * sin(frame * 0.03f + k * 1.7f)
            val c = colourAt(colours, k, 0xFFFFC060.toInt())
            paint.color = fade(c, 0.10f * glow)
            canvas.drawCircle(x, y, r * 1.35f, paint)
            paint.color = fade(c, 0.20f * glow)
            canvas.drawCircle(x, y, r, paint)
        }
        paint.color = 0x30FFFFFF
        for (k in 0 until 40) {
            val x = (hash3(k, 1, 33) % 1000) / 1000f * w
            val y = (hash3(k, 2, 33) % 1000) / 1000f * h
            canvas.drawCircle(x, y, (1f + (hash3(k, 3, 33) % 10) / 8f) * d, paint)
        }
        for (k in 0 until 16) {
            val x = (hash3(k, 1, 35) % 1000) / 1000f * w
            val p = 150 + hash3(k, 2, 35) % 130
            val t = ((frame + hash3(k, 3, 35) % p) % p).toFloat() / p
            val y = h * (t + 0.02f * sin(t * 40f))
            val r = (2.2f + (hash3(k, 4, 35) % 14) / 10f) * d
            paint.color = 0x1CFFFFFF
            canvas.drawRect(x - 0.6f * d, y - 42f * d, x + 0.6f * d, y, paint)
            paint.color = 0x40FFFFFF
            canvas.drawCircle(x, y - 1.6f * r, r * 0.7f, paint)
            canvas.drawCircle(x, y, r, paint)
            paint.color = 0xA0FFFFFF.toInt()
            canvas.drawCircle(x - r * 0.3f, y - r * 0.3f, r * 0.35f, paint)
        }
    }

    /**
     * Amiga copper bars: soft-edged horizontal bars of colour that weave up and down the screen on sine waves, each brightest at
     * its middle. [colours]: one per bar.
     */
    fun copperBars(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        val bars = maxOf(4, colours.size)
        val slice = 2.2f * d
        for (b in 0 until bars) {
            val c = colourAt(colours, b, 0xFFFF40A0.toInt())
            val cy = h * (0.5f + 0.4f * sin(frame * 0.045f + b * 0.62f))
            for (s in -5..5) {
                val f = 1f - kotlin.math.abs(s) / 6f
                paint.color = fade(c, 0.9f * f)
                canvas.drawRect(0f, cy + s * slice, w, cy + (s + 1) * slice, paint)
            }
        }
    }

    /**
     * Fireflies on a summer night: a scatter of small lights wandering slowly in loops, each swelling into a soft glow and fading
     * out again at its own pace. [colours]: the glow, then (optionally) a second tint.
     */
    fun fireflies(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        for (k in 0 until 20) {
            val bx = (hash3(k, 1, 41) % 1000) / 1000f * w
            val by = h * 0.1f + (hash3(k, 2, 41) % 1000) / 1000f * h * 0.85f
            val sp = 0.6f + (hash3(k, 3, 41) % 100) / 100f
            val x = bx + sin(frame * 0.017f * sp + k * 2.3f) * 24f * d
            val y = by + kotlin.math.cos(frame * 0.013f * sp + k * 1.1f) * 16f * d
            val s = (sin(frame * 0.045f * sp + k * 0.9f) + 1f) / 2f
            val b = s * s * s
            if (b < 0.02f) continue
            val c = colourAt(colours, k % 2, 0xFFD8FF50.toInt())
            paint.color = fade(c, 0.16f * b)
            canvas.drawCircle(x, y, 11f * d, paint)
            paint.color = fade(c, 0.42f * b)
            canvas.drawCircle(x, y, 5.5f * d, paint)
            paint.color = fade(0xFFFFFFE0.toInt(), b)
            canvas.drawCircle(x, y, 1.8f * d, paint)
        }
    }
}


/** The tap animations for the themes that copy a phone, a piece of software or a game. Each is drawn by the key tap layer. */
internal object DeviceTap {
    /** A smiley face of [colour], [size] in radius, that rises from ([x], [y]) swaying a little, and fades in the last third of [t] (0 to 1). */
    fun smiley(canvas: Canvas, paint: Paint, x: Float, y: Float, size: Float, t: Float, phase: Float, d: Float, colour: Int) {
        val rise = 70f * d * (1f - (1f - t) * (1f - t))
        val cx = x + sin(t * 7f + phase) * 8f * d
        val cy = y - rise
        val fade = minOf(1f, (1f - t) / 0.3f).coerceIn(0f, 1f)
        val a = (fade * 255f).toInt()
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(colour, a)
        canvas.drawCircle(cx, cy, size, paint)
        val ink = withAlpha(darken(colour, 0.6f), a)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, 1.2f * d)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(darken(colour, 0.3f), a)
        canvas.drawCircle(cx, cy, size, paint)
        paint.style = Paint.Style.FILL
        paint.color = ink
        canvas.drawCircle(cx - size * 0.34f, cy - size * 0.22f, size * 0.13f, paint)
        canvas.drawCircle(cx + size * 0.34f, cy - size * 0.22f, size * 0.13f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = ink
        var px = 0f
        var py = 0f
        var first = true
        var deg = 25
        while (deg <= 155) {
            val rad = deg * 0.017453292f
            val sx = cx + size * 0.56f * kotlin.math.cos(rad)
            val sy = cy + size * 0.1f + size * 0.5f * sin(rad)
            if (!first) canvas.drawLine(px, py, sx, sy, paint)
            px = sx
            py = sy
            first = false
            deg += 15
        }
        paint.style = Paint.Style.FILL
    }

    /** The four cells, as (column, row), of each of the seven falling-block shapes. */
    private val shapes = arrayOf(
        intArrayOf(0, 0, 1, 0, 2, 0, 3, 0),
        intArrayOf(0, 0, 1, 0, 0, 1, 1, 1),
        intArrayOf(0, 0, 1, 0, 2, 0, 1, 1),
        intArrayOf(1, 0, 2, 0, 0, 1, 1, 1),
        intArrayOf(0, 0, 1, 0, 1, 1, 2, 1),
        intArrayOf(0, 0, 0, 1, 1, 1, 2, 1),
        intArrayOf(2, 0, 0, 1, 1, 1, 2, 1),
    )

    /**
     * A falling block of four cells ([shape] 0 to 6) that drops one cell at a time from ([x], [y]) down a stack, turning a
     * quarter as it goes, and then flickers out. Its cells are a bevelled square of [colour].
     */
    fun tetromino(canvas: Canvas, paint: Paint, x: Float, y: Float, shape: Int, t: Float, d: Float, colour: Int) {
        if (t > 0.72f && (t * 22f).toInt() % 2 == 1) return
        val cell = 6f * d
        val cells = shapes[shape.coerceIn(0, 6)]
        val turns = (t * 3.4f).toInt() % 4
        val fall = Math.round(110f * d * t * t / cell) * cell
        val xs = IntArray(4)
        val ys = IntArray(4)
        for (k in 0 until 4) {
            var c = cells[k * 2]
            var r = cells[k * 2 + 1]
            for (q in 0 until turns) {
                val nc = -r
                r = c
                c = nc
            }
            xs[k] = c
            ys[k] = r
        }
        val minC = xs.min()
        val minR = ys.min()
        val maxC = xs.max()
        val ox = Math.round((x - (maxC - minC + 1) * cell / 2f) / cell) * cell
        val oy = Math.round((y - 40f * d + fall) / cell) * cell
        val light = lighten(colour, 0.3f)
        val dark = darken(colour, 0.45f)
        val bevel = maxOf(1f, d)
        paint.style = Paint.Style.FILL
        for (k in 0 until 4) {
            val l = ox + (xs[k] - minC) * cell
            val tp = oy + (ys[k] - minR) * cell
            paint.color = dark
            canvas.drawRect(l, tp, l + cell, tp + cell, paint)
            paint.color = colour
            canvas.drawRect(l + bevel, tp + bevel, l + cell - bevel, tp + cell - bevel, paint)
            paint.color = light
            canvas.drawRect(l + bevel, tp + bevel, l + cell - bevel, tp + bevel * 2f, paint)
            canvas.drawRect(l + bevel, tp + bevel, l + bevel * 2f, tp + cell - bevel, paint)
        }
    }

    /**
     * One chip broken off a block: a small square thrown out from ([x], [y]) at ([vx], [vy]) px/s that falls under gravity,
     * shrinks away in the last third of [t], and is snapped to a coarse grid like the blocks it came from.
     */
    fun debris(canvas: Canvas, paint: Paint, x: Float, y: Float, vx: Float, vy: Float, size: Float, sec: Float, t: Float, colour: Int, d: Float) {
        val gravity = 520f * d
        val snap = 2f * d
        val cx = Math.round((x + vx * sec) / snap) * snap
        val cy = Math.round((y + vy * sec + 0.5f * gravity * sec * sec) / snap) * snap
        val s = size * minOf(1f, (1f - t) / 0.35f).coerceIn(0.2f, 1f)
        paint.style = Paint.Style.FILL
        paint.color = colour
        canvas.drawRect(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f, paint)
        paint.color = lighten(colour, 0.25f)
        canvas.drawRect(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy - s / 2f + maxOf(1f, s * 0.3f), paint)
    }
}

