// app/src/main/kotlin/com/m8/keyboard/GadgetKeyShapes.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.min

/*
 * The key and strip drawables for the themes that copy a small gadget: a smartphone with a keyboard, a toy pet, a
 * calculator. See docs/THEMES.md for how to add a key style.
 */

/**
 * A key of a thumb keyboard, like a BlackBerry's: a slightly domed black cap with a lit upper edge and a fine rim, and
 * a chrome fret that runs the whole width of the cell under it, so the keys of a row stand on one metal bar and the
 * rows are divided by chrome lines. [rim] is the colour of the chrome and the rim. Pressed, the cap darkens and loses
 * its sheen.
 */
class FretKeyDrawable(
    private val fill: Int,
    private val rim: Int,
    private val pressed: Boolean,
    private val gapX: Float,
    private val gapY: Float,
    density: Float,
) : KeyShapeDrawable(gapX, gapY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val radius = 5f * density
        gradient(
            LinearGradient(
                0f, t, 0f, b,
                if (pressed) darken(fill, 0.04f) else lighten(fill, 0.16f), darken(fill, if (pressed) 0.2f else 0.12f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)
        if (!pressed) {
            // The sheen along the top of the cap.
            line(withAlpha(lighten(fill, 0.6f), 0x70), px)
            canvas.drawLine(l + radius, t + px, r - radius, t + px, paint)
        }
        val w = max(px, density)
        line(withAlpha(rim, if (pressed) 0x60 else 0x90), w)
        edge(canvas, l, t, r, b, radius, w)

        // The fret: a bright bar with a dark line under it, across the whole cell.
        val y = b + gapY * 0.5f
        val bar = max(px, 1.6f * density)
        solid(rim)
        canvas.drawRect(l - gapX, y - bar / 2f, r + gapX, y + bar / 2f, paint)
        solid(withAlpha(0xFF000000.toInt(), 0x90))
        canvas.drawRect(l - gapX, y + bar / 2f, r + gapX, y + bar / 2f + px, paint)
    }
}

/**
 * A glossy round button, like the three under the screen of a toy pet: a plump stadium of [fill] with a bright gloss
 * across its top, a darker rim, standing on a soft shadow of [shade]. Pressed, it drops onto its shadow and the gloss
 * dims.
 */
class ButtonKeyDrawable(
    private val fill: Int,
    private val shade: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val lift = 2.5f * density
        val drop = if (pressed) lift else 0f
        val h = b - t
        val radius = h * 0.42f
        solid(withAlpha(shade, 0xA0))
        roundRect(canvas, l, t + lift, r, b, radius)

        val top = t + drop
        val bottom = b - lift + drop
        gradient(LinearGradient(0f, top, 0f, bottom, lighten(fill, 0.12f), darken(fill, 0.08f), Shader.TileMode.CLAMP))
        roundRect(canvas, l, top, r, bottom, radius)

        val w = max(px, density)
        line(darken(fill, 0.3f), w)
        edge(canvas, l, top, r, bottom, radius, w)

        val gh = (bottom - top) * 0.38f
        val gi = (r - l) * 0.12f
        gradient(
            LinearGradient(
                0f, top, 0f, top + gh,
                withAlpha(0xFFFFFFFF.toInt(), if (pressed) 0x50 else 0xA0), withAlpha(0xFFFFFFFF.toInt(), 0),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l + gi, top + (bottom - top) * 0.08f, r - gi, top + gh, radius * 0.6f)
    }
}

/**
 * A small LCD set into a frame, like the screen of a calculator or a toy pet: a [bezel] with the lit panel of [screen]
 * inside it, a soft shadow along the panel's top edge, and a fine dark outline. With [ghost] the panel also shows the
 * unlit segments of three seven-segment digits at its right, very faint, as a real LCD does.
 */
class LcdPanelDrawable(
    private val bezel: Int,
    private val screen: Int,
    private val ghost: Boolean,
    private val density: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        val d = density
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = bezel
        rect.set(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat())
        canvas.drawRect(rect, paint)

        val l = b.left + 7f * d
        val t = b.top + 4f * d
        val r = b.right - 7f * d
        val bt = b.bottom - 4f * d
        paint.color = screen
        rect.set(l, t, r, bt)
        canvas.drawRoundRect(rect, 3f * d, 3f * d, paint)

        paint.shader = LinearGradient(0f, t, 0f, t + 9f * d, withAlpha(0xFF000000.toInt(), 0x45), withAlpha(0xFF000000.toInt(), 0), Shader.TileMode.CLAMP)
        rect.set(l, t, r, t + 9f * d)
        canvas.drawRoundRect(rect, 3f * d, 3f * d, paint)
        paint.shader = null

        if (ghost) {
            // Three unlit 8s and a point, at the right of the panel.
            val ink = withAlpha(darken(screen, 0.6f), 0x16)
            val dh = (bt - t) * 0.62f
            val dw = dh * 0.55f
            val seg = max(1f, dh * 0.11f)
            val top = t + ((bt - t) - dh) / 2f
            paint.color = ink
            for (i in 0 until 3) {
                val x = r - (3 - i) * (dw + 7f * d) + 2f * d
                digitEight(canvas, x, top, dw, dh, seg)
            }
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = max(1f, d)
        paint.color = withAlpha(0xFF000000.toInt(), 0x40)
        rect.set(l, t, r, bt)
        canvas.drawRoundRect(rect, 3f * d, 3f * d, paint)
        paint.style = Paint.Style.FILL
    }

    /** A seven-segment 8 with every segment drawn, [w] wide and [h] tall, its segments [s] thick. */
    private fun digitEight(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, s: Float) {
        val m = y + h / 2f
        canvas.drawRect(x + s, y, x + w - s, y + s, paint)
        canvas.drawRect(x + s, m - s / 2f, x + w - s, m + s / 2f, paint)
        canvas.drawRect(x + s, y + h - s, x + w - s, y + h, paint)
        canvas.drawRect(x, y + s, x + s, m - s / 2f, paint)
        canvas.drawRect(x + w - s, y + s, x + w, m - s / 2f, paint)
        canvas.drawRect(x, m + s / 2f, x + s, y + h - s, paint)
        canvas.drawRect(x + w - s, m + s / 2f, x + w, y + h - s, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * A key in ribbed black plastic, like the console of the early nineties: a gently shaded cap with a lit upper edge and a
 * band of fine horizontal ribs across its lower half. [border] is the rim. Pressed, the cap darkens and the ribs stay.
 */
class RibbedKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        gradient(
            LinearGradient(
                0f, t, 0f, b,
                if (pressed) fill else lighten(fill, 0.1f), darken(fill, if (pressed) 0.2f else 0.12f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)

        val pitch = max(2f * px, 2.6f * density)
        val inner = 3f * density
        canvas.save()
        canvas.clipRect(l + inner, t, r - inner, b - inner)
        var y = t + (b - t) * 0.58f
        while (y < b - inner) {
            solid(withAlpha(0xFF000000.toInt(), 0x66))
            canvas.drawRect(l, y, r, y + px, paint)
            solid(withAlpha(0xFFFFFFFF.toInt(), 0x1C))
            canvas.drawRect(l, y + px, r, y + 2f * px, paint)
            y += pitch
        }
        canvas.restore()

        if (!pressed) {
            line(withAlpha(0xFFFFFFFF.toInt(), 0x44), px)
            canvas.drawLine(l + radius, t + px, r - radius, t + px, paint)
        }
        if (border != 0) {
            val w = max(px, density)
            line(border, w)
            edge(canvas, l, t, r, b, radius, w)
        }
    }
}

/**
 * A key drawn the way a teletext page draws a box: a frame of chunky block-graphics cells in the key's colour round a
 * black [interior]. Pressed, the inside fills with the colour at half strength. When [solid] the whole key is the
 * colour, like the coloured buttons on a teletext remote.
 */
class MosaicKeyDrawable(
    private val fill: Int,
    private val interior: Int,
    private val solidKey: Boolean,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        if (solidKey) {
            solid(if (pressed) darken(fill, 0.25f) else fill)
            canvas.drawRect(l, t, r, b, paint)
            return
        }
        val s = max(px, Math.round(3.5f * density).toFloat())
        solid(interior)
        canvas.drawRect(l, t, r, b, paint)
        if (pressed) {
            solid(withAlpha(fill, 0x66))
            canvas.drawRect(l + s, t + s, r - s, b - s, paint)
        }
        solid(fill)
        canvas.drawRect(l, t, r, t + s, paint)
        canvas.drawRect(l, b - s, r, b, paint)
        canvas.drawRect(l, t, l + s, b, paint)
        canvas.drawRect(r - s, t, r, b, paint)
        // A block cut out of each corner, as a mosaic frame has when it is built from cells.
        solid(interior)
        canvas.drawRect(l, t, l + s, t + s, paint)
        canvas.drawRect(r - s, t, r, t + s, paint)
        canvas.drawRect(l, b - s, l + s, b, paint)
        canvas.drawRect(r - s, b - s, r, b, paint)
    }
}

/**
 * Paper for the candidate words, like the screen of a home computer of the early eighties: flat [paper] with the four
 * slanted stripes of the rainbow in the lower right corner.
 */
class SpectrumPaperDrawable(
    private val paper: Int,
    private val density: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = android.graphics.Path()
    private val stripes = intArrayOf(0xFFD70000.toInt(), 0xFFD7D700.toInt(), 0xFF00D700.toInt(), 0xFF00D7D7.toInt())

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.width() <= 0 || b.height() <= 0) return
        paint.style = Paint.Style.FILL
        paint.color = paper
        canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), paint)
        canvas.save()
        canvas.clipRect(b)
        val h = b.height().toFloat()
        val slant = h * 0.8f
        val sw = 7f * density
        val x0 = b.right - slant - 4f * sw - 6f * density
        for (i in 0 until 4) {
            val x = x0 + i * sw
            path.reset()
            path.moveTo(x, b.bottom.toFloat())
            path.lineTo(x + sw, b.bottom.toFloat())
            path.lineTo(x + sw + slant, b.top.toFloat())
            path.lineTo(x + slant, b.top.toFloat())
            path.close()
            paint.color = stripes[i]
            canvas.drawPath(path, paint)
        }
        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/**
 * A tile like a block of a falling-block puzzle on a handheld with a four-shade screen: a one-cell [frame] with its
 * corner cells missing, a light bevel ([hi]) along the top and left and a dark one ([lo]) along the bottom and right
 * round the [fill]. Pressed, the bevel is reversed, so the tile looks pushed in.
 */
class TileKeyDrawable(
    private val fill: Int,
    private val frame: Int,
    private val hi: Int,
    private val lo: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val u = max(px, Math.round(2f * density).toFloat())
        solid(frame)
        canvas.drawRect(l + u, t, r - u, t + u, paint)
        canvas.drawRect(l + u, b - u, r - u, b, paint)
        canvas.drawRect(l, t + u, l + u, b - u, paint)
        canvas.drawRect(r - u, t + u, r, b - u, paint)
        solid(fill)
        canvas.drawRect(l + u, t + u, r - u, b - u, paint)
        val light = if (pressed) lo else hi
        val dark = if (pressed) hi else lo
        solid(light)
        canvas.drawRect(l + u, t + u, r - u, t + 2f * u, paint)
        canvas.drawRect(l + u, t + u, l + 2f * u, b - u, paint)
        solid(dark)
        canvas.drawRect(l + u, b - 2f * u, r - u, b - u, paint)
        canvas.drawRect(r - 2f * u, t + u, r - u, b - u, paint)
    }
}

/**
 * A key drawn as a piece of the wall of a maze: a rounded box of two parallel lines in [wall] round a black inside, with a
 * small [pellet] resting at the bottom of it that is eaten (gone) while the key is pressed. Action keys ([solidKey])
 * are plain solid colour.
 */
class MazeKeyDrawable(
    private val fill: Int,
    private val wall: Int,
    private val pellet: Int,
    private val solidKey: Boolean,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val d = density
        if (solidKey) {
            solid(if (pressed) darken(fill, 0.25f) else fill)
            roundRect(canvas, l, t, r, b, 6f * d)
            return
        }
        solid(0xFF000000.toInt())
        roundRect(canvas, l, t, r, b, 7f * d)
        if (pressed) {
            solid(withAlpha(wall, 0x44))
            roundRect(canvas, l + 3.6f * d, t + 3.6f * d, r - 3.6f * d, b - 3.6f * d, 4f * d)
        }
        val w = max(px, 1.6f * d)
        line(wall, w)
        edge(canvas, l, t, r, b, 7f * d, w)
        edge(canvas, l + 3.8f * d, t + 3.8f * d, r - 3.8f * d, b - 3.8f * d, 4f * d, w)
        if (!pressed && pellet != 0) {
            val s = max(2f * px, 2.8f * d)
            solid(pellet)
            canvas.drawRect((l + r) / 2f - s / 2f, b - 9f * d - s / 2f, (l + r) / 2f + s / 2f, b - 9f * d + s / 2f, paint)
        }
    }
}

/**
 * A button like those of a block-building game's menus: a flat grey slab with coarse speckle, a black outline one pixel
 * wide, a light bevel along its top and left and a dark one along its bottom and right. The colour it is given is the
 * stone; pressed, the stone is the highlight colour the theme gives it.
 */
class CraftKeyDrawable(
    private val fill: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val o = max(px, Math.round(density).toFloat())
        val u = max(px, Math.round(2f * density).toFloat())
        solid(0xFF000000.toInt())
        canvas.drawRect(l, t, r, b, paint)
        solid(fill)
        canvas.drawRect(l + o, t + o, r - o, b - o, paint)

        // The speckle of the stone, in cells of two pixels, different on every key.
        val cell = u * 2f
        val seed = (l * 7f + t * 13f).toInt()
        val bright = lighten(fill, 0.07f)
        val dim = darken(fill, 0.08f)
        var y = t + o + u
        var j = 0
        while (y + cell < b - o - u) {
            var x = l + o + u
            var i = 0
            while (x + cell < r - o - u) {
                val v = hash3(i, j, seed) % 100
                if (v < 22) {
                    solid(bright)
                    canvas.drawRect(x, y, x + cell, y + cell, paint)
                } else if (v > 78) {
                    solid(dim)
                    canvas.drawRect(x, y, x + cell, y + cell, paint)
                }
                x += cell
                i++
            }
            y += cell
            j++
        }

        solid(lighten(fill, 0.3f))
        canvas.drawRect(l + o, t + o, r - o, t + o + u, paint)
        canvas.drawRect(l + o, t + o, l + o + u, b - o, paint)
        solid(darken(fill, 0.4f))
        canvas.drawRect(l + o, b - o - u, r - o, b - o, paint)
        canvas.drawRect(r - o - u, t + o, r - o, b - o, paint)
    }
}
