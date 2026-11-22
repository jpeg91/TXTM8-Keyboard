// app/src/main/kotlin/com/m8/keyboard/GameKeyShapes.kt
package com.m8.keyboard

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.min

/*
 * The key drawables for the video-game themes: one small class per [KeyStyle] added for them. Like the ones in
 * KeyShapes.kt they are picked by [KeyboardTheme.keyBackground] and never know which theme they draw for.
 */

/** [a] blended towards [b] by [t] (0..1), keeping full opacity. */
internal fun mixArgb(a: Int, b: Int, t: Float): Int {
    fun ch(shift: Int): Int {
        val x = (a shr shift) and 0xFF
        val y = (b shr shift) and 0xFF
        return (x + (y - x) * t).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

/**
 * A riveted steel plate, like the DOOM status bar: a bevelled grey frame (light on the top and left, dark on the
 * bottom and right) round a recessed panel in the key colour, with a rivet in each corner. Pressed, the bevel
 * flips so the panel looks sunk in, and the panel takes the lit key colour.
 */
class RivetedKeyDrawable(
    private val fill: Int,
    private val plate: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val metal = if (plate == 0) 0xFF979797.toInt() else plate
        val bw = 3f * density
        solid(darken(metal, 0.55f))
        canvas.drawRect(l, t, r, b, paint)

        // The bevel: two mirrored L-shaped bands.
        solid(if (pressed) darken(metal, 0.45f) else metal)
        path.reset()
        path.moveTo(l, t)
        path.lineTo(r, t)
        path.lineTo(r - bw, t + bw)
        path.lineTo(l + bw, t + bw)
        path.lineTo(l + bw, b - bw)
        path.lineTo(l, b)
        path.close()
        canvas.drawPath(path, paint)
        solid(if (pressed) metal else darken(metal, 0.72f))
        path.reset()
        path.moveTo(r, b)
        path.lineTo(l, b)
        path.lineTo(l + bw, b - bw)
        path.lineTo(r - bw, b - bw)
        path.lineTo(r - bw, t + bw)
        path.lineTo(r, t)
        path.close()
        canvas.drawPath(path, paint)

        // The recessed panel.
        val top = if (pressed) lighten(fill, 0.22f) else lighten(fill, 0.05f)
        val low = if (pressed) darken(fill, 0.06f) else darken(fill, 0.24f)
        gradient(LinearGradient(0f, t + bw, 0f, b - bw, top, low, Shader.TileMode.CLAMP))
        canvas.drawRect(l + bw, t + bw, r - bw, b - bw, paint)
        line(0xFF000000.toInt(), px)
        edge(canvas, l, t, r, b, 0f, px)

        // A rivet in each corner (left out on a key too small for them).
        if (r - l > 34f * density && b - t > 26f * density) {
            val m = bw + 3.2f * density
            val rad = 1.5f * density
            val xs = floatArrayOf(l + m, r - m)
            val ys = floatArrayOf(t + m, b - m)
            for (x in xs) for (y in ys) {
                solid(darken(metal, 0.62f))
                canvas.drawCircle(x, y, rad, paint)
                solid(lighten(metal, 0.35f))
                canvas.drawCircle(x - rad * 0.3f, y - rad * 0.3f, rad * 0.5f, paint)
            }
        }
    }
}

/**
 * A Final Fantasy VII menu window: a slightly rounded panel that fades from bright blue at the top left to
 * deep navy at the bottom right, with a silver rim and a fine light line just inside it. [lit] is the bright end of
 * the fade. Pressed, the whole window brightens and its rim turns white, like the highlighted entry of a menu.
 */
class WindowKeyDrawable(
    private val fill: Int,
    private val lit: Int,
    private val border: Int,
    private val pressed: Boolean,
    private val corner: Float,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val c = min(corner, 6f * density)
        val silver = if (border == 0) 0xFFB8C4D0.toInt() else border
        val topColour = if (pressed) lit else mixArgb(fill, lit, 0.85f)
        val bottomColour = if (pressed) mixArgb(fill, lit, 0.5f) else darken(fill, 0.5f)
        gradient(LinearGradient(l, t, r, b, topColour, bottomColour, Shader.TileMode.CLAMP))
        roundRect(canvas, l, t, r, b, c)

        // The thin light line just inside the rim, brighter on the top and left.
        val inset = 2.2f * density
        line(withAlpha(silver, 110), px)
        edge(canvas, l + inset, t + inset, r - inset, b - inset, max(0f, c - inset), px)
        line(withAlpha(0xFFFFFFFF.toInt(), if (pressed) 200 else 120), px)
        path.reset()
        path.moveTo(l + inset + c, t + inset + px / 2f)
        path.lineTo(r - inset - c, t + inset + px / 2f)
        path.moveTo(l + inset + px / 2f, t + inset + c)
        path.lineTo(l + inset + px / 2f, b - inset - c)
        canvas.drawPath(path, paint)

        // The rim itself.
        val rim = max(px, 1.4f * density)
        line(if (pressed) 0xFFFFFFFF.toInt() else silver, rim)
        edge(canvas, l, t, r, b, c, rim)
    }
}

/**
 * A Pip-Boy menu box: a dark green phosphor panel with its corners clipped by a pixel or two, fine scanlines across
 * it, and an outline that glows (a soft halo drawn inside the edge). Pressed, the panel takes the lit key colour and
 * the outline blooms brighter and thicker. The outline is [outlineColour]; [bloom] is its pressed colour.
 */
class PhosphorKeyDrawable(
    private val fill: Int,
    private val outlineColour: Int,
    private val bloom: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    private fun body(l: Float, t: Float, r: Float, b: Float, inset: Float) {
        val cut = 2f * density
        path.reset()
        path.moveTo(l + inset + cut, t + inset)
        path.lineTo(r - inset - cut, t + inset)
        path.lineTo(r - inset, t + inset + cut)
        path.lineTo(r - inset, b - inset - cut)
        path.lineTo(r - inset - cut, b - inset)
        path.lineTo(l + inset + cut, b - inset)
        path.lineTo(l + inset, b - inset - cut)
        path.lineTo(l + inset, t + inset + cut)
        path.close()
    }

    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val outline = if (outlineColour == 0) 0xFF1A662E.toInt() else outlineColour
        val lit = if (bloom == 0) lighten(outline, 0.5f) else bloom
        val colour = if (pressed) lit else outline

        solid(fill)
        body(l, t, r, b, 0f)
        canvas.drawPath(path, paint)

        // Scanlines.
        solid(withAlpha(0xFF000000.toInt(), 0x3A))
        var y = t + px * 2f
        while (y < b - px) {
            canvas.drawRect(l + px, y, r - px, y + px, paint)
            y += 3f * px
        }

        // The halo, from the wide faint ring to the crisp line.
        val widths = floatArrayOf(7f * density, 4f * density)
        val alphas = if (pressed) intArrayOf(0x30, 0x58) else intArrayOf(0x14, 0x28)
        for (i in widths.indices) {
            line(withAlpha(colour, alphas[i]), widths[i])
            body(l, t, r, b, widths[i] / 2f)
            canvas.drawPath(path, paint)
        }
        val crisp = max(px, (if (pressed) 2f else 1.4f) * density)
        line(colour, crisp)
        body(l, t, r, b, crisp / 2f)
        canvas.drawPath(path, paint)
    }
}

/**
 * An Art Deco panel (Vice City's Ocean Drive): the key is a stepped, ziggurat-cornered plate with an [outer] outline
 * and a finer [inner] line inside it, in two neon colours. Pressed, the panel lights and a glow spills inside the edge.
 */
class DecoKeyDrawable(
    private val fill: Int,
    private val outer: Int,
    private val inner: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    /** The stepped outline: two steps of [s] cut from every corner. */
    private fun steps(l: Float, t: Float, r: Float, b: Float, s: Float) {
        path.reset()
        path.moveTo(l + 2f * s, t)
        path.lineTo(r - 2f * s, t)
        path.lineTo(r - 2f * s, t + s)
        path.lineTo(r - s, t + s)
        path.lineTo(r - s, t + 2f * s)
        path.lineTo(r, t + 2f * s)
        path.lineTo(r, b - 2f * s)
        path.lineTo(r - s, b - 2f * s)
        path.lineTo(r - s, b - s)
        path.lineTo(r - 2f * s, b - s)
        path.lineTo(r - 2f * s, b)
        path.lineTo(l + 2f * s, b)
        path.lineTo(l + 2f * s, b - s)
        path.lineTo(l + s, b - s)
        path.lineTo(l + s, b - 2f * s)
        path.lineTo(l, b - 2f * s)
        path.lineTo(l, t + 2f * s)
        path.lineTo(l + s, t + 2f * s)
        path.lineTo(l + s, t + s)
        path.lineTo(l + 2f * s, t + s)
        path.close()
    }

    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val s = 2.2f * density
        val outerColour = if (outer == 0) 0xFF33D1D6.toInt() else outer
        val innerColour = if (inner == 0) 0xFFF25CA2.toInt() else inner

        solid(fill)
        steps(l, t, r, b, s)
        canvas.drawPath(path, paint)

        if (pressed) {
            // The glow spills in from the edge.
            line(withAlpha(innerColour, 0x40), 6f * density)
            steps(l + 3f * density, t + 3f * density, r - 3f * density, b - 3f * density, s * 0.7f)
            canvas.drawPath(path, paint)
        }

        val w = max(px, 1.5f * density)
        line(outerColour, w)
        steps(l + w / 2f, t + w / 2f, r - w / 2f, b - w / 2f, s)
        canvas.drawPath(path, paint)

        val iw = max(px, (if (pressed) 1.6f else 1f) * density)
        val gap = 3.4f * density
        line(innerColour, iw)
        steps(l + gap, t + gap, r - gap, b - gap, s * 0.55f)
        canvas.drawPath(path, paint)
    }
}

/**
 * A spray-painted stencil tile (San Andreas): a flat concrete-dark tile with a band of the [spray] colour sprayed
 * along its top edge, ragged underneath, with a mist of overspray dots below it and a couple of drips running down.
 * Every key is sprayed a little differently, worked out from where it sits. Pressed, the whole tile is sprayed solid.
 */
class StencilKeyDrawable(
    private val fill: Int,
    private val spray: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val paintColour = if (spray == 0) 0xFF2E5A36.toInt() else spray
        val seed = hash3(bounds.left, bounds.top, 31)
        val d = density
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)

        val bandBottom = if (pressed) b else t + (b - t) * 0.26f
        // The sprayed band, ragged along its lower edge.
        solid(withAlpha(paintColour, if (pressed) 0xF0 else 0xE0))
        path.reset()
        path.moveTo(l, t)
        path.lineTo(r, t)
        path.lineTo(r, bandBottom)
        var x = r
        var i = 0
        while (x > l) {
            x = max(l, x - 4f * d)
            val jitter = ((hash3(seed, i, 7) % 100) / 100f - 0.5f) * 3.6f * d
            path.lineTo(x, if (pressed) b else bandBottom + jitter)
            i++
        }
        path.close()
        canvas.drawPath(path, paint)

        // Overspray: a mist of dots below the band, thinning out with distance (all over the key when pressed).
        val reach = if (pressed) (b - t) else 14f * d
        val dots = if (pressed) 24 else 40
        for (k in 0 until dots) {
            val fx = (hash3(seed, k, 1) % 1000) / 1000f
            val fy = (hash3(seed, k, 2) % 1000) / 1000f
            val near = fy * fy // most dots land close to the band
            val radius = (0.6f + (hash3(seed, k, 3) % 100) / 100f * 1.1f) * d
            solid(withAlpha(paintColour, 0x50 + hash3(seed, k, 4) % 0x80))
            val py = if (pressed) t + fy * reach else bandBottom + near * reach
            if (py < b) canvas.drawCircle(l + fx * (r - l), py, radius, paint)
        }

        // Drips running down from the band (none on a pressed key: it is covered).
        if (!pressed) {
            for (k in 0 until 3) {
                if (hash3(seed, k, 5) % 100 > 55) continue
                val dx = l + (0.1f + 0.8f * ((hash3(seed, k, 6) % 1000) / 1000f)) * (r - l)
                val len = (5f + (hash3(seed, k, 8) % 100) / 100f * 9f) * d
                line(withAlpha(paintColour, 0xE0), max(px, 1.5f * d))
                paint.strokeCap = Paint.Cap.ROUND
                canvas.drawLine(dx, bandBottom, dx, min(b - 2f * d, bandBottom + len), paint)
                paint.strokeCap = Paint.Cap.BUTT
            }
        }

        line(darken(fill, 0.55f), px)
        edge(canvas, l, t, r, b, 0f, px)
    }
}

/**
 * A Sims control-panel button: a rounded panel with a light blue rim, a thin dark outline and a glassy blue face.
 * Pressed, the face brightens and a [glow] (plumbob green) lights the outline.
 */
class PanelKeyDrawable(
    private val fill: Int,
    private val glow: Int,
    private val pressed: Boolean,
    private val corner: Float,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val c = min(corner, 5f * density)
        val rim = 2f * density
        // Rim: light blue, brighter on top.
        gradient(LinearGradient(0f, t, 0f, b, lighten(fill, 0.55f), lighten(fill, 0.18f), Shader.TileMode.CLAMP))
        roundRect(canvas, l, t, r, b, c)
        // Dark line between rim and face.
        solid(darken(fill, 0.6f))
        roundRect(canvas, l + rim, t + rim, r - rim, b - rim, max(0f, c - rim))
        // The face.
        val top = if (pressed) lighten(fill, 0.3f) else lighten(fill, 0.14f)
        val low = if (pressed) lighten(fill, 0.05f) else darken(fill, 0.2f)
        gradient(LinearGradient(0f, t + rim + px, 0f, b - rim - px, top, low, Shader.TileMode.CLAMP))
        roundRect(canvas, l + rim + px, t + rim + px, r - rim - px, b - rim - px, max(0f, c - rim - px))
        // A glint along the top of the face.
        solid(withAlpha(0xFFFFFFFF.toInt(), if (pressed) 40 else 70))
        roundRect(canvas, l + rim + 3f * px, t + rim + 2f * px, r - rim - 3f * px, t + (b - t) * 0.42f, max(0f, c - rim))
        if (pressed && glow != 0) {
            line(glow, max(px, 2f * density))
            edge(canvas, l, t, r, b, c, max(px, 2f * density))
        } else {
            line(darken(fill, 0.65f), px)
            edge(canvas, l, t, r, b, c, px)
        }
    }
}

/**
 * A chunk of Worms terrain: soil in the key colour flecked with lighter and darker grit, a bumpy cap of [grass] along
 * the top, and a dark outline. Pressed, the chunk lightens and sinks a little.
 */
class TerrainKeyDrawable(
    private val fill: Int,
    private val grass: Int,
    private val edgeColour: Int,
    private val pressed: Boolean,
    private val corner: Float,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val d = density
        val top = if (pressed) t + 1.5f * d else t
        val c = min(corner, 6f * d)
        val turf = if (grass == 0) 0xFF4E9A26.toInt() else grass
        val outline = if (edgeColour == 0) darken(fill, 0.65f) else edgeColour
        val seed = hash3(bounds.left, bounds.top, 53)

        // Soil.
        val soilTop = if (pressed) lighten(fill, 0.16f) else lighten(fill, 0.06f)
        gradient(LinearGradient(0f, top, 0f, b, soilTop, darken(fill, 0.2f), Shader.TileMode.CLAMP))
        roundRect(canvas, l, top, r, b, c)
        // Grit.
        val dots = max(10, ((r - l) * (b - top) / (90f * d * d)).toInt())
        for (k in 0 until dots) {
            val fx = (hash3(seed, k, 1) % 1000) / 1000f
            val fy = 0.3f + (hash3(seed, k, 2) % 1000) / 1000f * 0.65f
            val light = hash3(seed, k, 3) % 3 != 0
            solid(withAlpha(if (light) lighten(fill, 0.4f) else darken(fill, 0.45f), 0x90))
            val s = (0.8f + (hash3(seed, k, 4) % 100) / 100f) * d
            canvas.drawRect(l + c / 2f + fx * (r - l - c), top + fy * (b - top), l + c / 2f + fx * (r - l - c) + s, top + fy * (b - top) + s, paint)
        }
        // The cap of grass: a band along the top with a scalloped lower edge.
        val gh = (b - top) * 0.2f
        canvas.save()
        canvas.clipRect(l, top, r, top + gh + 3f * d)
        solid(if (pressed) lighten(turf, 0.12f) else turf)
        roundRect(canvas, l, top, r, b, c)
        canvas.restore()
        solid(if (pressed) lighten(turf, 0.12f) else turf)
        var x = l + 2f * d
        var i = 0
        while (x < r - 2f * d) {
            val rad = (1.6f + (hash3(seed, i, 9) % 100) / 100f * 1.6f) * d
            canvas.drawCircle(x, top + gh + 0.5f * d, rad, paint)
            x += 4.5f * d
            i++
        }
        // A darker lip under the grass.
        solid(withAlpha(darken(turf, 0.45f), 0x80))
        canvas.drawRect(l + c / 2f, top + gh - 1f * d, r - c / 2f, top + gh, paint)

        line(outline, max(px, 1.3f * d))
        edge(canvas, l, top, r, b, c, max(px, 1.3f * d))
    }
}

/**
 * A Green Hill Zone key: a flat face ringed by a one-cell border of two-tone checkerboard (the zone's checkered
 * earth). The two checker colours are [colourA] and [colourB]; pressed, they swap and the face brightens.
 */
class CheckerKeyDrawable(
    private val fill: Int,
    private val colourA: Int,
    private val colourB: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val one = if (colourA == 0) darken(fill, 0.4f) else colourA
        val two = if (colourB == 0) lighten(fill, 0.3f) else colourB
        val first = if (pressed) two else one
        val second = if (pressed) one else two
        val cell = 4.5f * density

        // The border: top and bottom rows, then the columns between them.
        var i = 0
        var x = l
        while (x < r) {
            val w = min(cell, r - x)
            solid(if (i % 2 == 0) first else second)
            canvas.drawRect(x, t, x + w, t + cell, paint)
            solid(if (i % 2 == 0) second else first)
            canvas.drawRect(x, b - cell, x + w, b, paint)
            x += cell
            i++
        }
        var j = 1
        var y = t + cell
        while (y < b - cell) {
            val h = min(cell, b - cell - y)
            solid(if (j % 2 == 0) first else second)
            canvas.drawRect(l, y, l + cell, y + h, paint)
            solid(if (j % 2 == 0) second else first)
            canvas.drawRect(r - cell, y, r, y + h, paint)
            y += cell
            j++
        }

        // The face.
        val top = if (pressed) lighten(fill, 0.3f) else lighten(fill, 0.12f)
        val low = if (pressed) lighten(fill, 0.05f) else darken(fill, 0.18f)
        gradient(LinearGradient(0f, t + cell, 0f, b - cell, top, low, Shader.TileMode.CLAMP))
        canvas.drawRect(l + cell, t + cell, r - cell, b - cell, paint)
        line(0xFF000000.toInt(), px)
        edge(canvas, l, t, r, b, 0f, px)
        edge(canvas, l + cell, t + cell, r - cell, b - cell, 0f, px)
    }
}

/**
 * A Super Mario Bros. block with a black outline and a lit and shaded edge. With [question] false it is a brick
 * (mortar lines in the bonded pattern of the original); with it true it is a ? block: a bright face with a rivet in
 * each corner. Pressed, the block's edges swap, as if it had been bumped from below.
 */
class BlockKeyDrawable(
    private val fill: Int,
    private val outline: Int,
    private val question: Boolean,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val black = if (outline == 0) 0xFF000000.toInt() else outline
        val lightEdge = lighten(fill, 0.5f)
        val darkEdge = darken(fill, 0.4f)
        val ow = max(px, 1.6f * density)
        val bev = 2f * density

        solid(fill)
        canvas.drawRect(l, t, r, b, paint)

        if (!question) {
            // Mortar: one line across the middle, then the vertical joints (one in the top course, two in the
            // bottom, so the bricks are bonded).
            line(black, ow)
            val mid = (t + b) / 2f
            path.reset()
            path.moveTo(l, mid)
            path.lineTo(r, mid)
            path.moveTo((l + r) / 2f, t)
            path.lineTo((l + r) / 2f, mid)
            path.moveTo(l + (r - l) * 0.25f, mid)
            path.lineTo(l + (r - l) * 0.25f, b)
            path.moveTo(l + (r - l) * 0.75f, mid)
            path.lineTo(l + (r - l) * 0.75f, b)
            canvas.drawPath(path, paint)
        }

        // Lit top and left edges, shaded bottom and right edges (swapped when pressed).
        val topLeft = if (pressed) darkEdge else lightEdge
        val bottomRight = if (pressed) lightEdge else darkEdge
        solid(topLeft)
        canvas.drawRect(l + ow, t + ow, r - ow, t + ow + bev, paint)
        canvas.drawRect(l + ow, t + ow, l + ow + bev, b - ow, paint)
        solid(bottomRight)
        canvas.drawRect(l + ow, b - ow - bev, r - ow, b - ow, paint)
        canvas.drawRect(r - ow - bev, t + ow, r - ow, b - ow, paint)

        if (question) {
            // Rivets.
            val m = ow + bev + 2.5f * density
            val s = 1.6f * density
            val xs = floatArrayOf(l + m, r - m)
            val ys = floatArrayOf(t + m, b - m)
            solid(black)
            for (x in xs) for (y in ys) canvas.drawRect(x - s, y - s, x + s, y + s, paint)
        }

        line(black, ow)
        edge(canvas, l, t, r, b, 0f, ow)
    }
}

/**
 * The candidate strip of a Final Fantasy VII-style theme: the same blue window as the keys (bright [lit] at the top
 * left fading to [fill] and then deep navy at the bottom right) with a silver line along the top and bottom.
 */
class WindowStripDrawable(
    private val fill: Int,
    private val lit: Int,
    private val border: Int,
    private val density: Float,
) : Drawable() {
    private val paint = Paint()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left.toFloat()
        val t = b.top.toFloat()
        val r = b.right.toFloat()
        val bottom = b.bottom.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            l, t, r, bottom,
            intArrayOf(mixArgb(fill, lit, 0.85f), fill, darken(fill, 0.5f)), floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(l, t, r, bottom, paint)
        paint.shader = null
        paint.color = if (border == 0) 0xFFB8C4D0.toInt() else border
        val w = max(1f, 1.4f * density)
        canvas.drawRect(l, t, r, t + w, paint)
        canvas.drawRect(l, bottom - w, r, bottom, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/**
 * A moulded plastic button set into a handheld's shell, like the buttons of the Game Boy Advance: a dark cut-out in
 * the shell with a softly domed button standing in it, a pale sheen along its top and a darker underside that shows
 * its height. Pressed, the button sinks into the cut-out and loses its sheen. [recess] is the colour of the cut-out.
 */
class ShellKeyDrawable(
    private val fill: Int,
    private val recess: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val corner = min((b - t) * 0.4f, 16f * density)
        solid(recess)
        roundRect(canvas, l, t, r, b, corner)

        val m = 1.8f * density
        val height = if (pressed) 0.8f * density else 2.4f * density
        val sink = if (pressed) 1.4f * density else 0f
        val bl = l + m
        val br = r - m
        val bt = t + m + sink
        val bb = b - m - (height - sink).coerceAtLeast(0f)

        // The underside of the button, which shows below its face.
        solid(darken(fill, 0.5f))
        roundRect(canvas, bl, bt + height, br, b - m, corner - m)

        // The face: lighter at the top, a shade darker at the bottom; flat and darker when pressed.
        if (pressed) {
            solid(darken(fill, 0.06f))
        } else {
            gradient(LinearGradient(0f, bt, 0f, bb, lighten(fill, 0.22f), darken(fill, 0.1f), Shader.TileMode.CLAMP))
        }
        roundRect(canvas, bl, bt, br, bb, corner - m)

        if (!pressed) {
            // A sheen across the top of the dome.
            solid(withAlpha(0xFFFFFFFF.toInt(), 46))
            roundRect(
                canvas, bl + (br - bl) * 0.14f, bt + 1.6f * density,
                br - (br - bl) * 0.14f, bt + 1.6f * density + (bb - bt) * 0.3f, corner * 0.6f,
            )
        }
    }
}

/**
 * A handheld's screen on its dark bezel, like the one on the Game Boy Advance: a lit, slightly greenish panel set into a
 * near-black frame with a fine grid of pixels over it, a soft diagonal glare in one corner and a small red power lamp on
 * the bezel. [bezel] is the frame colour and [screen] the colour of the lit panel.
 */
class HandheldScreenDrawable(
    private val bezel: Int,
    private val screen: Int,
    private val density: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private val grid: BitmapShader by lazy {
        // One cell of the pixel grid: a faint dark column and row, repeated across the screen.
        val cell = max(2, Math.round(2f * density))
        val bitmap = Bitmap.createBitmap(cell, cell, Bitmap.Config.ARGB_8888)
        for (i in 0 until cell) {
            bitmap.setPixel(0, i, 0x1A000000)
            bitmap.setPixel(i, 0, 0x12000000)
        }
        BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val l = b.left.toFloat()
        val t = b.top.toFloat()
        val r = b.right.toFloat()
        val bt = b.bottom.toFloat()
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = bezel
        canvas.drawRect(l, t, r, bt, paint)

        val insetX = 9f * density
        val insetY = min(5f * density, (bt - t) * 0.14f)
        val sl = l + insetX
        val st = t + insetY
        val sr = r - insetX
        val sb = bt - insetY
        val corner = 3f * density

        // The lit panel, a little brighter in the middle than at its edges.
        rect.set(sl, st, sr, sb)
        paint.color = screen
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.shader = LinearGradient(0f, st, 0f, sb, withAlpha(0xFFFFFFFF.toInt(), 40), withAlpha(0xFF000000.toInt(), 26), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.shader = grid
        canvas.drawRoundRect(rect, corner, corner, paint)
        paint.shader = null

        // The glare: a pale wedge across the top-left corner.
        path.reset()
        path.moveTo(sl, st)
        path.lineTo(sl + (sr - sl) * 0.34f, st)
        path.lineTo(sl, sb)
        path.close()
        paint.color = withAlpha(0xFFFFFFFF.toInt(), 22)
        canvas.drawPath(path, paint)

        // The shadow the bezel casts on the panel.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, 1.4f * density)
        paint.color = withAlpha(0xFF000000.toInt(), 90)
        canvas.drawRoundRect(rect, corner, corner, paint)

        // The power lamp, on the left bezel, with a little glow.
        paint.style = Paint.Style.FILL
        val lampX = l + insetX / 2f
        val lampY = (t + bt) / 2f
        paint.color = withAlpha(0xFFFF3B30.toInt(), 70)
        canvas.drawCircle(lampX, lampY, 2.6f * density, paint)
        paint.color = 0xFFFF3B30.toInt()
        canvas.drawCircle(lampX, lampY, 1.3f * density, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
