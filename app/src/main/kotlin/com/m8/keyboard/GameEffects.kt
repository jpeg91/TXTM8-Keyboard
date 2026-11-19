// app/src/main/kotlin/com/m8/keyboard/GameEffects.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * The animations added for the video-game themes. The moving backgrounds ([AmbientKind.FIRE] and so on) and the tap
 * animations ([TapEffectKind.POINTER] and so on) are drawn by [AmbientBackground] and [TapEffectDrawable]; the drawing
 * itself lives here so those two classes only have to pick the right one.
 */

/** The colour at [i] in [colours] (wrapping round), or [fallback] when the list is empty. */
private fun colourAt(colours: IntArray, i: Int, fallback: Int = 0xFFFFFFFF.toInt()): Int =
    if (colours.isEmpty()) fallback else colours[i % colours.size]

/** Two ARGB colours blended, alpha included. */
private fun lerpArgb(a: Int, b: Int, t: Float): Int {
    fun ch(shift: Int): Int {
        val x = (a ushr shift) and 0xFF
        val y = (b ushr shift) and 0xFF
        return (x + (y - x) * t).toInt().coerceIn(0, 255)
    }
    return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

// ------------------------------------------------------------------ moving backgrounds

/**
 * The DOOM fire: the classic fire effect of the console ports. A grid of coarse heat cells with a white-hot bottom row;
 * every frame each cell passes its heat to a neighbour above it, losing a little, so flames lick upwards and die out.
 * [colours] are the flame colours from the coolest (dark red) to the hottest, with the alpha of the coolest ignored.
 */
internal class DoomFire(private val density: Float, colours: IntArray) {
    private val levels = 22
    private val ramp = IntArray(levels)
    private val paint = Paint()
    private var cols = 0
    private var rows = 0
    private var cell = 0f
    private var heat = IntArray(0)
    private var lastFrame = Int.MIN_VALUE
    private var sizedFor = 0f

    init {
        val stops = if (colours.size >= 2) colours else intArrayOf(0xFF8B0000.toInt(), 0xFFD75F00.toInt(), 0xFFC79B2B.toInt())
        for (i in 1 until levels) {
            val pos = (i - 1) / (levels - 2).toFloat() * (stops.size - 1)
            val k = min(pos.toInt(), stops.size - 2)
            val c = lerpArgb(stops[k], stops[k + 1], pos - k)
            val fade = min(1f, i / 6f) * 0.9f // the coolest cells are thin smoke
            ramp[i] = withAlpha(c, (255 * fade).toInt())
        }
        paint.isAntiAlias = false
    }

    private fun resize(w: Float, h: Float) {
        cell = 6f * density
        cols = ceil(w / cell).toInt() + 1
        rows = max(3, ceil(h * 0.42f / cell).toInt())
        heat = IntArray(cols * rows)
        for (x in 0 until cols) heat[(rows - 1) * cols + x] = levels - 1
        sizedFor = w * 31f + h
        // Run it for a while so the first frame already has flames.
        for (f in 0 until rows * 2) step(f - rows * 2)
    }

    private fun step(frame: Int) {
        for (y in 1 until rows) {
            for (x in 0 until cols) {
                val src = y * cols + x
                val p = heat[src]
                if (p == 0) {
                    heat[src - cols] = 0
                } else {
                    val rand = hash3(frame, x, y) % 4
                    val dx = (x - rand + 1).coerceIn(0, cols - 1)
                    val cooled = p - (rand and 1) - (if (rand == 3) 1 else 0)
                    heat[(y - 1) * cols + dx] = max(0, cooled)
                }
            }
        }
    }

    fun draw(canvas: Canvas, w: Float, h: Float, frame: Int) {
        if (heat.isEmpty() || sizedFor != w * 31f + h) resize(w, h)
        if (frame != lastFrame) {
            step(frame)
            lastFrame = frame
        }
        val top = h - rows * cell
        paint.style = Paint.Style.FILL
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val level = heat[y * cols + x]
                if (level <= 0) continue
                paint.color = ramp[min(level, levels - 1)]
                val px = x * cell
                val py = top + y * cell
                canvas.drawRect(px, py, px + cell, py + cell, paint)
            }
        }
    }
}

/**
 * The Pip-Boy screen: fine scanlines over everything, a faint bright band that rolls slowly down, a flicker, and a
 * dark vignette round the edges of the glass. [colours]: 0 the scanlines, 1 the rolling band, 2 the vignette, 3 the
 * flicker's tint.
 */
internal class PipBoyScreen(private val density: Float, private val colours: IntArray) {
    private val paint = Paint()
    private var vignette: Shader? = null
    private var vw = 0f
    private var vh = 0f

    fun draw(canvas: Canvas, w: Float, h: Float, frame: Int) {
        val d = density
        paint.style = Paint.Style.FILL
        paint.shader = null

        // Scanlines.
        paint.color = colourAt(colours, 0, 0x38000000)
        val pitch = max(2f, 3f * d)
        var y = 0f
        while (y < h) {
            canvas.drawRect(0f, y, w, y + max(1f, d), paint)
            y += pitch
        }

        // The rolling band: it fades in over its top half and out over its bottom half.
        val band = 46f * d
        val period = h + band * 2f
        val top = (frame * 2.4f * d) % period - band
        val bandColour = colourAt(colours, 1, 0x2A1AFF80)
        paint.shader = LinearGradient(0f, top, 0f, top + band / 2f, withAlpha(bandColour, 0), bandColour, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, top, w, top + band / 2f, paint)
        paint.shader = LinearGradient(0f, top + band / 2f, 0f, top + band, bandColour, withAlpha(bandColour, 0), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, top + band / 2f, w, top + band, paint)
        paint.shader = null

        // A flicker: now and then the whole glass brightens for a frame.
        if (hash3(frame, 3, 1) % 9 == 0) {
            paint.color = colourAt(colours, 3, 0x141AFF80)
            canvas.drawRect(0f, 0f, w, h, paint)
        }

        // The vignette.
        if (vignette == null || vw != w || vh != h) {
            val edge = colourAt(colours, 2, 0xCC000000.toInt())
            vignette = RadialGradient(
                w / 2f, h / 2f, max(w, h) * 0.72f,
                intArrayOf(withAlpha(edge, 0), withAlpha(edge, 0), edge),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            vw = w
            vh = h
        }
        paint.shader = vignette
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null
    }
}

/** The scenery drawn for the other game backgrounds. Each function draws one frame and leaves [paint] clean. */
internal object GameAmbient {
    /**
     * Vice City at dusk: a striped neon sun sinking into the sea, its reflection shimmering in bars below the
     * horizon, and a palm tree at each side swaying slightly. [colours]: 0 the top of the sun, 1 its bottom,
     * 2 the horizon glow, 3 the palm silhouettes.
     */
    fun sunset(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val horizon = h * 0.58f
        val sunTop = colourAt(colours, 0, 0xFFF7943D.toInt())
        val sunBottom = colourAt(colours, 1, 0xFFF25CA2.toInt())
        val glow = colourAt(colours, 2, 0xFF33D1D6.toInt())
        val palm = colourAt(colours, 3, 0xFF120922.toInt())
        paint.style = Paint.Style.FILL

        // The sun, sliced into stripes that thin out towards the horizon.
        val r = min(w * 0.26f, h * 0.30f)
        val cx = w / 2f
        val cy = horizon - r * 0.55f
        paint.shader = LinearGradient(0f, cy - r, 0f, horizon, sunTop, sunBottom, Shader.TileMode.CLAMP)
        var y0 = cy - r
        var y1 = cy + r * 0.05f
        canvas.save()
        canvas.clipRect(0f, y0, w, min(y1, horizon))
        canvas.drawCircle(cx, cy, r, paint)
        canvas.restore()
        for (k in 0 until 6) {
            val gap = (1.2f + k * 1.1f) * d
            y0 = y1 + gap
            y1 = y0 + (r * 0.30f - k * 0.04f * r)
            if (y0 >= horizon) break
            canvas.save()
            canvas.clipRect(0f, y0, w, min(y1, horizon))
            canvas.drawCircle(cx, cy, r, paint)
            canvas.restore()
        }
        paint.shader = null

        // The glow along the horizon, and the horizon line.
        paint.shader = LinearGradient(0f, horizon - 22f * d, 0f, horizon, withAlpha(glow, 0), withAlpha(glow, 0x50), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, horizon - 22f * d, w, horizon, paint)
        paint.shader = null
        paint.color = withAlpha(glow, 0xB0)
        canvas.drawRect(0f, horizon, w, horizon + max(1f, 1.5f * d), paint)

        // The reflection: bars that widen and narrow, each a little different every few frames.
        var by = horizon + 5f * d
        var k = 0
        while (by < h) {
            val wobble = hash3(frame / 5, k, 11) % 100 / 100f
            val half = r * (0.9f - k * 0.045f).coerceAtLeast(0.25f) * (0.55f + 0.45f * wobble)
            paint.color = withAlpha(lerpArgb(sunBottom, sunTop, wobble), max(0x18, 0x7A - k * 9))
            canvas.drawRect(cx - half, by, cx + half, by + (2f + k * 0.35f) * d, paint)
            by += (6f + k * 1.8f) * d
            k++
        }

        // The palms.
        val sway = sin(frame * 0.07f) * 3f
        palm(canvas, paint, path, w * 0.06f, h, h * 0.50f, 18f * d, sway, d, palm)
        palm(canvas, paint, path, w * 0.95f, h, h * 0.44f, -16f * d, -sway, d, palm)
    }

    /** One palm: a curved trunk and a crown of drooping fronds, all one silhouette colour. */
    private fun palm(
        canvas: Canvas, paint: Paint, path: Path, baseX: Float, baseY: Float, height: Float,
        lean: Float, swayDeg: Float, d: Float, colour: Int,
    ) {
        paint.style = Paint.Style.FILL
        paint.color = colour
        val topX = baseX + lean
        val topY = baseY - height
        val half = 2.6f * d
        path.reset()
        path.moveTo(baseX - half * 1.4f, baseY)
        path.quadTo(baseX + lean * 0.1f - half, baseY - height * 0.55f, topX - half * 0.5f, topY)
        path.lineTo(topX + half * 0.5f, topY)
        path.quadTo(baseX + lean * 0.1f + half, baseY - height * 0.55f, baseX + half * 1.4f, baseY)
        path.close()
        canvas.drawPath(path, paint)

        val frond = 36f * d
        for (i in 0 until 7) {
            val deg = -170f + i * 26.7f + swayDeg
            val a = deg * 0.0174533f
            val ex = topX + cos(a) * frond
            val ey = topY + sin(a) * frond * 0.55f + frond * 0.38f
            val mx = topX + cos(a) * frond * 0.55f
            val my = topY + sin(a) * frond * 0.9f - frond * 0.12f
            path.reset()
            path.moveTo(topX, topY)
            path.quadTo(mx, my - 3f * d, ex, ey)
            path.quadTo(mx, my + 5f * d, topX, topY + 2f * d)
            path.close()
            canvas.drawPath(path, paint)
        }
    }

    // Pixel-art scenery for the Mario overworld. 'X' is the body colour, 'S' the shade, '.' nothing.
    private val cloud = arrayOf(
        "....XXXX..XXX....",
        "..XXXXXXXXXXXXX..",
        ".XXXXXXXXXXXXXXX.",
        "XXXXXXXXXXXXXXXXX",
        ".SSSSSSSSSSSSSSS.",
    )
    private val bush = arrayOf(
        "..XXX.XXXX.XXX..",
        ".XXXXXXXXXXXXXX.",
        "XXXXXXXXXXXXXXXX",
        "SSSSSSSSSSSSSSSS",
    )

    private fun pixels(canvas: Canvas, paint: Paint, art: Array<String>, x: Float, y: Float, u: Float, body: Int, shade: Int) {
        for (row in art.indices) {
            val line = art[row]
            var c = 0
            while (c < line.length) {
                val ch = line[c]
                if (ch == '.') {
                    c++
                    continue
                }
                var end = c
                while (end < line.length && line[end] == ch) end++
                paint.color = if (ch == 'X') body else shade
                canvas.drawRect(x + c * u, y + row * u, x + end * u, y + (row + 1) * u, paint)
                c = end
            }
        }
    }

    /** A stepped hill with dark outline and the two dark "eyes" of the original, standing on [baseY]. */
    private fun hill(canvas: Canvas, paint: Paint, cx: Float, baseY: Float, rows: Int, u: Float, body: Int, outline: Int) {
        for (r in 0 until rows) {
            val width = (3 + r * 2) * u
            val y = baseY - (rows - r) * u
            paint.color = body
            canvas.drawRect(cx - width / 2f, y, cx + width / 2f, y + u, paint)
            paint.color = outline
            canvas.drawRect(cx - width / 2f, y, cx - width / 2f + u, y + u, paint)
            canvas.drawRect(cx + width / 2f - u, y, cx + width / 2f, y + u, paint)
            if (r == 0) canvas.drawRect(cx - width / 2f, y, cx + width / 2f, y + u, paint)
        }
        if (rows >= 4) {
            paint.color = outline
            val ey = baseY - (rows - 2) * u
            canvas.drawRect(cx - 2f * u, ey, cx - u, ey + 2f * u, paint)
            canvas.drawRect(cx + u, ey, cx + 2f * u, ey + 2f * u, paint)
        }
    }

    /**
     * The Super Mario Bros. overworld in NES pixels: clouds drifting across the sky, hills and bushes along the
     * bottom. [colours]: 0 the cloud, 1 its shade, 2 the hill and bush green, 3 their dark outline.
     */
    fun overworld(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        val u = 3.2f * d
        paint.style = Paint.Style.FILL
        val white = colourAt(colours, 0)
        val shade = colourAt(colours, 1, 0xFF9CC4FC.toInt())
        val green = colourAt(colours, 2, 0xFF00A800.toInt())
        val dark = colourAt(colours, 3, 0xFF005800.toInt())

        // Hills and bushes first (they stand behind the clouds' lowest edge only on small screens).
        hill(canvas, paint, w * 0.16f, h, 9, u, green, dark)
        hill(canvas, paint, w * 0.78f, h, 5, u, green, dark)
        pixels(canvas, paint, bush, w * 0.40f, h - bush.size * u, u, green, dark)
        pixels(canvas, paint, bush, w * 0.90f, h - bush.size * u, u, green, dark)

        // Three clouds at different heights and speeds.
        val cloudW = cloud[0].length * u
        for (i in 0 until 3) {
            val span = w + cloudW * 2f
            val speed = (0.35f + i * 0.22f) * d
            val x = ((hash3(i, 5, 2) % 1000) / 1000f * span + frame * speed) % span - cloudW
            val y = h * (0.05f + 0.17f * i)
            pixels(canvas, paint, cloud, x, y, u, white, shade)
        }
    }

    /**
     * Green Hill Zone: clouds drifting over rounded hills, and a strip of grass-topped checkered earth along the
     * bottom that scrolls past as if Sonic were running. [colours]: 0 the cloud, 1 and 2 the two checker colours,
     * 3 the grass.
     */
    fun greenHill(canvas: Canvas, paint: Paint, path: Path, w: Float, h: Float, frame: Int, d: Float, colours: IntArray) {
        paint.style = Paint.Style.FILL
        val cloudColour = colourAt(colours, 0, 0xB8FFFFFF.toInt())
        val a = colourAt(colours, 1, 0xFFA84800.toInt())
        val b = colourAt(colours, 2, 0xFFE89830.toInt())
        val grass = colourAt(colours, 3, 0xFF24A824.toInt())

        // Clouds.
        paint.color = cloudColour
        for (i in 0 until 3) {
            val s = (15f + i * 6f) * d
            val span = w + s * 5f
            val x = ((hash3(i, 9, 4) % 1000) / 1000f * span + frame * (0.4f + i * 0.2f) * d) % span - s * 2.5f
            val y = h * (0.07f + 0.13f * i)
            canvas.drawCircle(x, y, s * 0.55f, paint)
            canvas.drawCircle(x + s * 0.7f, y - s * 0.25f, s * 0.72f, paint)
            canvas.drawCircle(x + s * 1.4f, y, s * 0.55f, paint)
            canvas.drawRect(x, y, x + s * 1.4f, y + s * 0.55f, paint)
        }

        // Far hills: a row of round green bumps drifting slowly the other way.
        paint.color = withAlpha(grass, 0x66)
        val bump = 46f * d
        val shift = (frame * 0.5f * d) % (bump * 2f)
        var hx = -bump * 2f - shift
        var k = 0
        while (hx < w + bump) {
            val rad = bump * (0.7f + (hash3(k % 4, 3, 6) % 40) / 100f)
            canvas.drawCircle(hx + bump, h * 0.74f + rad * 0.2f, rad, paint)
            hx += bump * 1.5f
            k++
        }

        // The ground: grass over two rows of checkers, scrolling.
        val cell = 15f * d
        val top = h * 0.80f
        val grassH = 6f * d
        val scroll = (frame * 1.8f * d) % (cell * 2f)
        var row = 0
        var y = top + grassH
        while (y < h) {
            var col = 0
            var x = -scroll
            while (x < w) {
                paint.color = if ((col + row) % 2 == 0) a else b
                canvas.drawRect(x, y, x + cell, y + cell, paint)
                x += cell
                col++
            }
            y += cell
            row++
        }
        paint.color = grass
        canvas.drawRect(0f, top, w, top + grassH, paint)
        // Grass tufts hanging over the earth.
        var tx = -scroll
        while (tx < w) {
            canvas.drawCircle(tx + cell / 2f, top + grassH, 3.4f * d, paint)
            tx += cell
        }
        path.reset()
    }
}

// ------------------------------------------------------------------ tap animations

/** The tap animations added for the video-game themes: each draws one particle at one moment of its life. */
internal object GameTap {
    private fun fadeInOut(t: Float, inEnd: Float, outStart: Float): Float {
        val i = if (t < inEnd) t / inEnd else 1f
        val o = if (t > outStart) (1f - t) / (1f - outStart) else 1f
        return (i * o).coerceIn(0f, 1f)
    }

    /**
     * The pointing-glove cursor of the Final Fantasy VII menu: a white glove with a dark outline and a cuff slides in
     * from the left, its fingertip on the tap, and fades. [colours]: 0 the glove, 1 the outline, 2 the cuff.
     */
    fun pointer(canvas: Canvas, paint: Paint, rect: RectF, x: Float, y: Float, t: Float, d: Float, colours: IntArray) {
        val glove = colourAt(colours, 0)
        val outline = colourAt(colours, 1, 0xFF000038.toInt())
        val cuff = colourAt(colours, 2, 0xFFB8C4D0.toInt())
        val alpha = (fadeInOut(t, 0.12f, 0.65f) * 255f).toInt()
        val slide = if (t < 0.2f) -(1f - t / 0.2f) * 16f * d else 0f
        // Left, top, right, bottom and corner radius of each part, in dp, fingertip at the origin.
        val parts = arrayOf(
            floatArrayOf(-35f, -7f, -27f, 7f, 2f),
            floatArrayOf(-28f, -7.5f, -11f, 7.5f, 4f),
            floatArrayOf(-16f, 3f, -10f, 8.5f, 2.5f),
            floatArrayOf(-11f, 3f, -5f, 8.5f, 2.5f),
            floatArrayOf(-23f, -12f, -13f, -6f, 3f),
            floatArrayOf(-16f, -3.2f, 0f, 3.2f, 3.2f),
        )
        canvas.save()
        canvas.translate(x + slide - 4f * d, y)
        for (pass in 0 until 3) {
            val shift = if (pass == 0) 1.6f * d else 0f
            for (i in parts.indices) {
                val p = parts[i]
                rect.set(p[0] * d + shift, p[1] * d + shift, p[2] * d + shift, p[3] * d + shift)
                when (pass) {
                    0 -> {
                        paint.style = Paint.Style.FILL
                        paint.color = withAlpha(0xFF000000.toInt(), alpha * 110 / 255)
                    }
                    1 -> {
                        paint.style = Paint.Style.FILL
                        paint.color = withAlpha(if (i == 0) cuff else glove, alpha)
                    }
                    else -> {
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = max(1f, 1.2f * d)
                        paint.color = withAlpha(outline, alpha)
                    }
                }
                canvas.drawRoundRect(rect, p[4] * d, p[4] * d, paint)
            }
        }
        canvas.restore()
        paint.style = Paint.Style.FILL
    }

    /** A Sims plumbob: a faceted green diamond that turns on its axis as it bobs upward. */
    fun plumbob(
        canvas: Canvas, paint: Paint, path: Path, x: Float, y: Float, size: Float, sec: Float, phase: Float,
        fade: Float, colour: Int,
    ) {
        val turn = cos(sec * 3.4f + phase)
        val half = size * (0.28f + 0.34f * abs(turn))
        val bob = sin(sec * 5f + phase) * size * 0.15f
        val cy = y + bob
        val frontX = x + turn * half * 0.35f
        val facets = arrayOf(
            floatArrayOf(x, cy - size, x - half, cy, frontX, cy + size * 0.1f),
            floatArrayOf(x, cy - size, x + half, cy, frontX, cy + size * 0.1f),
            floatArrayOf(x - half, cy, x, cy + size * 0.95f, frontX, cy + size * 0.1f),
            floatArrayOf(x + half, cy, x, cy + size * 0.95f, frontX, cy + size * 0.1f),
        )
        val shades = intArrayOf(lighten(colour, 0.55f), colour, darken(colour, 0.18f), darken(colour, 0.45f))
        paint.style = Paint.Style.FILL
        for (i in facets.indices) {
            val f = facets[i]
            path.reset()
            path.moveTo(f[0], f[1])
            path.lineTo(f[2], f[3])
            path.lineTo(f[4], f[5])
            path.close()
            paint.color = withAlpha(shades[i], (fade * 235f).toInt())
            canvas.drawPath(path, paint)
        }
        paint.color = withAlpha(0xFFFFFFFF.toInt(), (fade * 200f).toInt())
        canvas.drawCircle(x - half * 0.35f, cy - size * 0.45f, max(1f, size * 0.09f), paint)
    }

    /** One fleck of spray paint: it jumps out to its place in the mist, then fades. */
    fun sprayDot(canvas: Canvas, paint: Paint, ox: Float, oy: Float, dx: Float, dy: Float, radius: Float, t: Float, colour: Int) {
        val ease = 1f - (1f - min(1f, t * 4f)) * (1f - min(1f, t * 4f))
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(colour, ((1f - t) * (1f - t) * 220f).toInt())
        canvas.drawCircle(ox + dx * ease, oy + dy * ease, radius, paint)
    }

    /** A drip of paint running down from the mist, its head a little fatter than its trail. */
    fun sprayDrip(canvas: Canvas, paint: Paint, x: Float, y: Float, speed: Float, sec: Float, t: Float, colour: Int, d: Float) {
        val length = speed * sec
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, 1.5f * d)
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = withAlpha(colour, ((1f - t) * 200f).toInt())
        canvas.drawLine(x, y, x, y + length, paint)
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        canvas.drawCircle(x, y + length, 1.5f * d, paint)
    }

    /**
     * The Worms explosion: a jagged orange burst with a yellow core and a pale centre that swells quickly and fades.
     * [colours]: 0 the burst, 1 the core.
     */
    fun blast(canvas: Canvas, paint: Paint, path: Path, x: Float, y: Float, radius: Float, t: Float, colours: IntArray) {
        val grow = 1f - (1f - min(1f, t * 2.2f)) * (1f - min(1f, t * 2.2f))
        val r = radius * (0.25f + 0.75f * grow)
        val alpha = ((1f - t * t) * 255f).toInt()
        fun star(rad: Float, points: Int, notch: Float) {
            path.reset()
            for (i in 0 until points * 2) {
                val a = i * Math.PI.toFloat() / points
                val rr = if (i % 2 == 0) rad else rad * notch
                val px = x + cos(a) * rr
                val py = y + sin(a) * rr
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
        }
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(colourAt(colours, 0, 0xFFE64819.toInt()), alpha)
        star(r, 9, 0.72f)
        canvas.drawPath(path, paint)
        paint.color = withAlpha(colourAt(colours, 1, 0xFFF5C724.toInt()), alpha)
        star(r * 0.68f, 9, 0.75f)
        canvas.drawPath(path, paint)
        paint.color = withAlpha(0xFFFFF4C8.toInt(), alpha)
        canvas.drawCircle(x, y, r * 0.3f, paint)
    }

    /** A lump of earth thrown out by an explosion: it arcs under gravity, turning, and fades. */
    fun debris(canvas: Canvas, paint: Paint, x: Float, y: Float, size: Float, spin: Float, sec: Float, t: Float, colour: Int) {
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(colour, ((1f - t * t) * 255f).toInt())
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(spin * sec)
        canvas.drawRect(-size, -size, size, size, paint)
        canvas.restore()
    }

    /**
     * A Sonic ring: a gold ellipse that squashes narrow and wide as it spins, with a bright inner edge. In the last
     * third of its life it blinks, as scattered rings do.
     */
    fun ring(canvas: Canvas, paint: Paint, rect: RectF, x: Float, y: Float, sec: Float, t: Float, phase: Float, d: Float, colours: IntArray) {
        if (t > 0.65f && (sec * 18f).toInt() % 2 == 0) return
        val gold = colourAt(colours, 0, 0xFFF8D800.toInt())
        val bright = colourAt(colours, 1, lighten(gold, 0.6f))
        val squash = max(0.22f, abs(cos(sec * 9f + phase)))
        val rad = 6.5f * d
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1.5f, 2.4f * d)
        paint.color = gold
        rect.set(x - rad * squash, y - rad, x + rad * squash, y + rad)
        canvas.drawOval(rect, paint)
        paint.strokeWidth = max(1f, 1f * d)
        paint.color = bright
        rect.set(x - rad * squash * 0.62f, y - rad * 0.62f, x - rad * squash * 0.1f, y - rad * 0.1f)
        canvas.drawOval(rect, paint)
        paint.style = Paint.Style.FILL
    }

    /**
     * A Super Mario Bros. coin popping out of a block: it shoots up, turns edge-on and flat as it spins, and falls back.
     * [colours]: 0 the coin, 1 its dark outline.
     */
    fun coin(canvas: Canvas, paint: Paint, rect: RectF, x: Float, y: Float, sec: Float, t: Float, d: Float, colours: IntArray) {
        val gold = colourAt(colours, 0, 0xFFFC9838.toInt())
        val edge = colourAt(colours, 1, 0xFF000000.toInt())
        val lift = 4f * t * (1f - t) * 46f * d
        val cy = y - 8f * d - lift
        // Four steps of the spin, like the four frames of the original.
        val frames = floatArrayOf(1f, 0.6f, 0.22f, 0.6f)
        val width = 6.5f * d * frames[(sec * 14f).toInt() % frames.size]
        val halfHeight = 8f * d
        val alpha = ((if (t > 0.8f) (1f - t) / 0.2f else 1f) * 255f).toInt()
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(edge, alpha)
        rect.set(x - width - d, cy - halfHeight - d, x + width + d, cy + halfHeight + d)
        canvas.drawOval(rect, paint)
        paint.color = withAlpha(gold, alpha)
        rect.set(x - width, cy - halfHeight, x + width, cy + halfHeight)
        canvas.drawOval(rect, paint)
        paint.color = withAlpha(lighten(gold, 0.55f), alpha)
        rect.set(x - width * 0.35f, cy - halfHeight * 0.62f, x + width * 0.35f, cy + halfHeight * 0.62f)
        canvas.drawOval(rect, paint)
    }
}
