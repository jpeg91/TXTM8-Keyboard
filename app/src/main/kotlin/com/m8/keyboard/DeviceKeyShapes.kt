// app/src/main/kotlin/com/m8/keyboard/DeviceKeyShapes.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import kotlin.math.max
import kotlin.math.min

/*
 * The key drawables for the themes that copy a piece of software or a phone: one small class per [KeyStyle] added for
 * them. Like the ones in KeyShapes.kt they are picked by [KeyboardTheme.keyBackground] and never know which theme they
 * draw for.
 */

/**
 * A classic media-player button: a square-cornered metal button with a black outline, a light bevel along its top and
 * left and a dark one along its bottom and right, as on the player's transport buttons. Pressed, the bevel reverses and
 * the face darkens. When [led] is not 0 the button also carries a small lamp in its top-left corner, like the toggle
 * buttons: dim in [led]'s colour, and lit when the button is pressed.
 */
class PlayerKeyDrawable(
    private val fill: Int,
    private val led: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        // The black outline.
        solid(0xFF000000.toInt())
        roundRect(canvas, l, t, r, b, 0f)
        val o = px
        val bevel = max(px, 2f * density)
        val il = l + o
        val it = t + o
        val ir = r - o
        val ib = b - o

        // The face.
        val top = if (pressed) darken(fill, 0.1f) else lighten(fill, 0.1f)
        val bottom = if (pressed) darken(fill, 0.22f) else darken(fill, 0.08f)
        gradient(LinearGradient(0f, it, 0f, ib, top, bottom, Shader.TileMode.CLAMP))
        roundRect(canvas, il, it, ir, ib, 0f)

        // The bevel: two light edges and two dark ones, mitred at the corners.
        val light = if (pressed) darken(fill, 0.5f) else lighten(fill, 0.6f)
        val dark = if (pressed) lighten(fill, 0.35f) else darken(fill, 0.55f)
        solid(light)
        path.reset()
        path.moveTo(il, it)
        path.lineTo(ir, it)
        path.lineTo(ir - bevel, it + bevel)
        path.lineTo(il + bevel, it + bevel)
        path.lineTo(il + bevel, ib - bevel)
        path.lineTo(il, ib)
        path.close()
        canvas.drawPath(path, paint)
        solid(dark)
        path.reset()
        path.moveTo(ir, ib)
        path.lineTo(il, ib)
        path.lineTo(il + bevel, ib - bevel)
        path.lineTo(ir - bevel, ib - bevel)
        path.lineTo(ir - bevel, it + bevel)
        path.lineTo(ir, it)
        path.close()
        canvas.drawPath(path, paint)

        if (led != 0) {
            // The lamp: a black housing with a small square of colour in it.
            val size = 4f * density
            val x = il + bevel + 3f * density
            val y = it + bevel + 3f * density
            solid(0xFF000000.toInt())
            roundRect(canvas, x, y, x + size + 2f * px, y + size * 0.7f + 2f * px, 0f)
            solid(if (pressed) led else darken(led, 0.7f))
            roundRect(canvas, x + px, y + px, x + px + size, y + px + size * 0.7f, 0f)
        }
    }
}

/**
 * A key on an electroluminescent keypad, like the Motorola RAZR V3: dark glass with a glowing line in [line]. The keys
 * of one row share a single line that snakes through them: it comes down the left of the first key, over the top of
 * it, down between it and the second key, under the second key, up between the second and third, over the third, and
 * so on, ending with the side of the last key. Each key draws its own part of that line ([position] is the key's place
 * in its row of [count] keys; even places are crossed over the top, odd places under the bottom), and the parts meet
 * on the edges between keys. A key on its own ([count] 1) is a closed, softly curved outline instead. Pressed, the
 * glass lights up from inside.
 */
class GlowLineKeyDrawable(
    private val fill: Int,
    private val line: Int,
    private val pressed: Boolean,
    private val position: Int,
    private val count: Int,
    private val gapX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(gapX, insetY, density) {
    private val strokePath = android.graphics.Path()

    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val h = b - t
        val rc = min(h * 0.28f, 12f * density)
        val bow = min(h * 0.06f, 2.5f * density)
        val first = position == 0
        val last = position == count - 1
        val single = count == 1
        // Where the sides sit: on the edge between two keys, so both keys' halves of the line meet; on the key's own edge at the ends of a row.
        val xl = if (first) l else l - gapX
        val xr = if (last) r else r + gapX
        val onTop = position % 2 == 0
        val cx = (xl + xr) / 2f

        strokePath.reset()
        if (single) {
            strokePath.moveTo(xl, t + rc)
            strokePath.quadTo(xl, t - bow, xl + rc, t)
            strokePath.quadTo(cx, t - bow * 2, xr - rc, t)
            strokePath.quadTo(xr, t - bow, xr, t + rc)
            strokePath.lineTo(xr, b - rc)
            strokePath.quadTo(xr, b + bow, xr - rc, b)
            strokePath.quadTo(cx, b + bow * 2, xl + rc, b)
            strokePath.quadTo(xl, b + bow, xl, b - rc)
            strokePath.close()
        } else if (onTop) {
            // Up the left side, over the top, down the right side.
            strokePath.moveTo(xl, b - rc)
            strokePath.lineTo(xl, t + rc)
            strokePath.quadTo(xl, t, xl + rc, t)
            strokePath.quadTo(cx, t - bow * 2, xr - rc, t)
            strokePath.quadTo(xr, t, xr, t + rc)
            strokePath.lineTo(xr, b - rc)
        } else {
            // Down the left side, under the bottom, up the right side.
            strokePath.moveTo(xl, t + rc)
            strokePath.lineTo(xl, b - rc)
            strokePath.quadTo(xl, b, xl + rc, b)
            strokePath.quadTo(cx, b + bow * 2, xr - rc, b)
            strokePath.quadTo(xr, b, xr, b - rc)
            strokePath.lineTo(xr, t + rc)
        }
        // The glass: the whole key, so the row is one slab of glass with the line drawn on it.
        // It stops short of the sides, so that a neighbour's glass never covers this key's half of the line between them.
        val fl = xl + 4f * density
        val fr = xr - 4f * density
        path.reset()
        path.moveTo(fl, t)
        path.lineTo(fr, t)
        path.lineTo(fr, b)
        path.lineTo(fl, b)
        path.close()
        gradient(LinearGradient(0f, t, 0f, b, lighten(fill, 0.12f), darken(fill, 0.2f), Shader.TileMode.CLAMP))
        if (single) roundRect(canvas, xl, t, xr, b, rc) else canvas.drawPath(path, paint)
        if (pressed) {
            solid(withAlpha(line, 80))
            if (single) roundRect(canvas, xl, t, xr, b, rc) else canvas.drawPath(path, paint)
        }

        // The glow, then the line itself.
        val lit = if (pressed) 1.4f else 1f
        for ((alpha, width) in listOf(
            Pair(if (pressed) 70 else 34, 6f * density),
            Pair(if (pressed) 150 else 90, 3.2f * density),
            Pair(255, max(px, 1.5f * density * lit)),
        )) {
            line(withAlpha(line, alpha), width)
            paint.strokeJoin = android.graphics.Paint.Join.ROUND
            paint.strokeCap = android.graphics.Paint.Cap.ROUND
            canvas.drawPath(strokePath, paint)
        }
    }
}
