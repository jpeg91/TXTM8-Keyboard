// app/src/main/kotlin/com/m8/keyboard/KeyShapes.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/*
 * The key drawables for the key styles that went beyond the plain rounded key: one small class per [KeyStyle].
 * [KeyboardTheme.keyBackground] picks one by the theme's key style and hands it the palette colours it needs, so
 * a drawable never knows which theme it is drawing for. See docs/THEMES.md for how to add a key style.
 */

/**
 * The shared part of the key drawables: the inset that leaves a gap between keys, the paint helpers, and a guard
 * that skips keys too small to draw. A subclass only draws inside [drawKey]'s rectangle.
 */
abstract class KeyShapeDrawable(
    private val insetX: Float,
    private val insetY: Float,
    protected val density: Float,
) : Drawable() {
    protected val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val rect = RectF()
    protected val path = Path()
    private var opacity = 255

    /** One device pixel, or more. */
    protected val px: Float = max(1f, round(density))

    /** Draws the key into the rectangle [l], [t], [r], [b] (the bounds less the gap). */
    protected abstract fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float)

    final override fun draw(canvas: Canvas) {
        val area = bounds
        val l = area.left + insetX
        val t = area.top + insetY
        val r = area.right - insetX
        val b = area.bottom - insetY
        if (r - l < 8f * density || b - t < 8f * density) return
        drawKey(canvas, l, t, r, b)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    /** Fills with one colour from now on. */
    protected fun solid(colour: Int) {
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = scaled(colour)
    }

    /** Strokes lines of [width] in [colour] from now on. */
    protected fun line(colour: Int, width: Float) {
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.strokeJoin = Paint.Join.MITER
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = scaled(colour)
    }

    /** Fills with [shader] from now on. */
    protected fun gradient(shader: Shader) {
        paint.style = Paint.Style.FILL
        paint.color = 0xFF000000.toInt()
        paint.shader = shader
        paint.alpha = opacity
    }

    protected fun roundRect(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float) {
        rect.set(l, t, r, b)
        val limit = min(rect.width(), rect.height()) / 2f
        val rad = min(max(radius, 0f), limit)
        if (rad <= 0f) canvas.drawRect(rect, paint) else canvas.drawRoundRect(rect, rad, rad, paint)
    }

    /** Strokes a rounded rectangle centred on its own edge (for glows that spill both ways). */
    protected fun ring(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float) =
        roundRect(canvas, l, t, r, b, radius)

    /** Strokes a rounded rectangle whose outer edge is the given rectangle, so the line stays inside it. */
    protected fun edge(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, radius: Float, width: Float) {
        val h = width / 2f
        ring(canvas, l + h, t + h, r - h, b - h, radius - h)
    }

    private fun scaled(colour: Int): Int =
        if (opacity == 255) colour else withAlpha(colour, (colour ushr 24) * opacity / 255)

    override fun setAlpha(alpha: Int) {
        opacity = alpha.coerceIn(0, 255)
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Text-mode buttons: a square key with a bracket on each side, like "[ OK ]" on a terminal. Pressed, the
 * brackets thicken.
 */
class BracketKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)
        if (border == 0) return
        val w = max(px, (if (pressed) 2.6f else 1.5f) * density)
        val x0 = l + 3f * density + w / 2f
        val x1 = r - 3f * density - w / 2f
        val y0 = t + 3f * density
        val y1 = b - 3f * density
        val serif = min(5f * density, (x1 - x0) / 3f)
        line(border, w)
        path.reset()
        path.moveTo(x0 + serif, y0)
        path.lineTo(x0, y0)
        path.lineTo(x0, y1)
        path.lineTo(x0 + serif, y1)
        path.moveTo(x1 - serif, y0)
        path.lineTo(x1, y0)
        path.lineTo(x1, y1)
        path.lineTo(x1 - serif, y1)
        canvas.drawPath(path, paint)
    }
}

/**
 * A targeting-reticle key: a faint hairline box with a bright tick in each corner. Pressed, the ticks lengthen
 * as if the sights had closed in.
 */
class ReticleKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)
        if (border == 0) return
        val hair = max(px, 0.75f * density)
        line(withAlpha(border, 0x70), hair)
        edge(canvas, l, t, r, b, 0f, hair)

        val w = max(px, 1.8f * density)
        val arm = min((if (pressed) 11f else 6f) * density, min(r - l, b - t) / 2f)
        val x0 = l + w / 2f
        val x1 = r - w / 2f
        val y0 = t + w / 2f
        val y1 = b - w / 2f
        line(border, w)
        path.reset()
        path.moveTo(x0 + arm, y0); path.lineTo(x0, y0); path.lineTo(x0, y0 + arm)
        path.moveTo(x1 - arm, y0); path.lineTo(x1, y0); path.lineTo(x1, y0 + arm)
        path.moveTo(x0 + arm, y1); path.lineTo(x0, y1); path.lineTo(x0, y1 - arm)
        path.moveTo(x1 - arm, y1); path.lineTo(x1, y1); path.lineTo(x1, y1 - arm)
        canvas.drawPath(path, paint)
    }
}

/**
 * A square key on a CRT: fine horizontal scanlines across the face and a one-pixel outline. Pressed, the lines
 * brighten and the outline doubles.
 */
class ScanlineKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)
        if (border == 0) return
        line(withAlpha(border, if (pressed) 0x66 else 0x38), px)
        val step = 3f * px
        var y = t + step
        while (y < b) {
            canvas.drawLine(l, y, r, y, paint)
            y += step
        }
        val w = if (pressed) 2f * px else px
        line(border, w)
        edge(canvas, l, t, r, b, 0f, w)
    }
}

/**
 * A square key in a double-line text-mode box (the BIOS setup screen's frame): an outer line and a second line
 * just inside it. Pressed, the inner line thickens.
 */
class FrameKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        canvas.drawRect(l, t, r, b, paint)
        if (border == 0) return
        val outer = max(px, 1.5f * density)
        line(border, outer)
        edge(canvas, l, t, r, b, 0f, outer)
        val gap = 3.5f * density
        if (r - l > 2f * gap + 6f * density && b - t > 2f * gap + 6f * density) {
            val inner = if (pressed) 2f * density else max(px, density)
            line(border, inner)
            edge(canvas, l + gap, t + gap, r - gap, b - gap, 0f, inner)
        }
    }
}

/**
 * A square key whose outline has come apart in two colours, like a glitching screen: the [ghostA] and [ghostB]
 * outlines sit either side of the main one, and a short torn band sits on the left of the face. Pressed, the outlines drift further apart.
 */
class GlitchKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val ghostA: Int,
    private val ghostB: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val room = 3f * density
        val ml = l + room
        val mr = r - room
        val mt = t + density
        val mb = b - density
        if (mr - ml < 6f * density || mb - mt < 6f * density) return
        val shift = (if (pressed) 3f else 2f) * density
        val w = max(px, 1.5f * density)

        line(withAlpha(ghostA, 0xCC), w)
        edge(canvas, ml - shift, mt, mr - shift, mb, 0f, w)
        line(withAlpha(ghostB, 0xCC), w)
        edge(canvas, ml + shift, mt, mr + shift, mb, 0f, w)

        solid(fill)
        canvas.drawRect(ml, mt, mr, mb, paint)

        // The torn band: a strip across the left of the face, a shade off the face colour.
        val bandTop = mt + (mb - mt) * 0.58f
        solid(withAlpha(if (border != 0) border else ghostA, 0x50))
        canvas.drawRect(ml, bandTop, ml + (mr - ml) * 0.22f, bandTop + max(2f * px, (mb - mt) * 0.1f), paint)

        if (border != 0) {
            line(border, w)
            edge(canvas, ml, mt, mr, mb, 0f, w)
        }
    }
}

/**
 * A neon-tube key: a dark face outlined in a glowing line. The glow is three wider, fainter strokes under a bright
 * core. Pressed, the glow strengthens and the face fills with a wash of the glow colour.
 */
class NeonKeyDrawable(
    private val fill: Int,
    private val glow: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val m = 3f * density
        val gl = l + m
        val gt = t + m
        val gr = r - m
        val gb = b - m
        if (gr - gl < 6f * density || gb - gt < 6f * density) return
        solid(fill)
        roundRect(canvas, gl, gt, gr, gb, radius)
        if (pressed) {
            solid(withAlpha(glow, 0x30))
            roundRect(canvas, gl, gt, gr, gb, radius)
        }
        val boost = if (pressed) 1.6f else 1f
        for (i in GLOW_WIDTHS.indices) {
            line(withAlpha(glow, min(255, (GLOW_ALPHAS[i] * boost).toInt())), GLOW_WIDTHS[i] * density)
            ring(canvas, gl, gt, gr, gb, radius)
        }
        val core = max(px, 1.2f * density)
        line(lighten(glow, 0.55f), core)
        edge(canvas, gl - core / 2f, gt - core / 2f, gr + core / 2f, gb + core / 2f, radius, core)
    }

    private companion object {
        val GLOW_WIDTHS = floatArrayOf(6f, 4f, 2.5f)
        val GLOW_ALPHAS = intArrayOf(0x1A, 0x30, 0x60)
    }
}

/**
 * A backlit rubber key: a soft halo of the [glow] colour behind the lettering, a pale line along the top edge and
 * a thin rim. Pressed, the halo is far brighter.
 */
class BacklitKeyDrawable(
    private val fill: Int,
    private val glow: Int,
    private val border: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        roundRect(canvas, l, t, r, b, radius)

        val reach = max(r - l, b - t) * 0.62f
        gradient(
            RadialGradient(
                (l + r) / 2f, (t + b) / 2f, reach,
                withAlpha(glow, if (pressed) 0xA0 else 0x4A), withAlpha(glow, 0), Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)

        val inset = min(radius * 0.6f, (r - l) / 4f)
        line(withAlpha(lighten(fill, 0.45f), 0x80), px)
        canvas.drawLine(l + inset, t + px * 1.5f, r - inset, t + px * 1.5f, paint)

        if (border != 0) {
            val w = max(px, 1.2f * density)
            line(withAlpha(border, 0xB0), w)
            edge(canvas, l, t, r, b, radius, w)
        }
    }
}

/**
 * A soap bubble: fully rounded, shaded lighter towards the upper left, with a rim and a bright glint. Pressed, the
 * bubble shrinks a little and the glint goes, as if it had been squeezed.
 */
class BubbleKeyDrawable(
    private val fill: Int,
    private val rim: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val squeeze = if (pressed) 2f * density else 0f
        val bl = l + squeeze
        val bt = t + squeeze
        val br = r - squeeze
        val bb = b - squeeze
        val w = br - bl
        val h = bb - bt
        val round = min(w, h) / 2f

        gradient(
            RadialGradient(
                bl + w * 0.35f, bt + h * 0.3f, max(w, h) * 0.8f,
                lighten(fill, 0.3f), fill, Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, bl, bt, br, bb, round)

        val rimWidth = max(px, 1.4f * density)
        line(rim, rimWidth)
        edge(canvas, bl, bt, br, bb, round, rimWidth)

        if (!pressed) {
            val white = 0xFFFFFFFF.toInt()
            solid(withAlpha(white, 0xB0))
            val gx = bl + h * 0.35f
            val gy = bt + h * 0.14f
            rect.set(gx, gy, gx + w * 0.22f, gy + 3.5f * density)
            canvas.drawOval(rect, paint)
            solid(withAlpha(white, 0x50))
            val sx = br - h * 0.45f
            val sy = bb - h * 0.2f
            rect.set(sx, sy, sx + w * 0.08f, sy + 2.5f * density)
            canvas.drawOval(rect, paint)
        }
    }
}

/**
 * A key with a fire under it: a dark face, a glow in the [glow] colour rising from the bottom edge, a hot line
 * along the base and a thin rim. Pressed, the glow climbs nearly to the top.
 */
class UnderglowKeyDrawable(
    private val fill: Int,
    private val glow: Int,
    private val border: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        solid(fill)
        roundRect(canvas, l, t, r, b, radius)

        val rise = (b - t) * (if (pressed) 0.95f else 0.6f)
        gradient(
            LinearGradient(
                0f, b, 0f, b - rise,
                withAlpha(glow, if (pressed) 0xD0 else 0x90), withAlpha(glow, 0), Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)

        val inset = min(radius * 0.8f, (r - l) / 4f)
        line(withAlpha(lighten(glow, 0.3f), if (pressed) 0xFF else 0xC0), 2f * density)
        canvas.drawLine(l + inset, b - 2f * density, r - inset, b - 2f * density, paint)

        if (border != 0) {
            val w = max(px, density)
            line(withAlpha(border, 0xB0), w)
            edge(canvas, l, t, r, b, radius, w)
        }
    }
}

/**
 * A blossom petal: two opposite corners rounded right off and the other two nearly square, with a soft shine from
 * the top. Pressed, the petal turns over (the rounded corners swap).
 */
class PetalKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    private fun petal(l: Float, t: Float, r: Float, b: Float) {
        val big = min((b - t) / 2f, (r - l) / 2f)
        val small = min(3f * density, big)
        val a = if (pressed) small else big
        val c = if (pressed) big else small
        rect.set(l, t, r, b)
        path.reset()
        path.addRoundRect(rect, floatArrayOf(a, a, c, c, a, a, c, c), Path.Direction.CW)
    }

    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        petal(l, t, r, b)
        gradient(LinearGradient(0f, t, 0f, b, lighten(fill, 0.18f), darken(fill, 0.06f), Shader.TileMode.CLAMP))
        canvas.drawPath(path, paint)
        if (border != 0) {
            val w = max(px, 1.2f * density)
            petal(l + w / 2f, t + w / 2f, r - w / 2f, b - w / 2f)
            line(border, w)
            canvas.drawPath(path, paint)
        }
    }
}

/**
 * A concave, dished key like the buttons of a 16-bit pad: fully rounded, shaded dark at the top where the dish
 * curves away and light at the bottom, with a pale lip along the lower edge. Pressed, the dish deepens.
 */
class DishKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val lip = 1.5f * px
        val round = (b - t) / 2f

        solid(withAlpha(lighten(fill, 0.55f), 0x90))
        roundRect(canvas, l, t + lip, r, b, round)

        val bodyBottom = b - lip
        gradient(
            LinearGradient(
                0f, t, 0f, bodyBottom,
                darken(fill, if (pressed) 0.28f else 0.16f), lighten(fill, if (pressed) 0.02f else 0.14f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, bodyBottom, round)

        if (border != 0) {
            val w = max(px, 1.2f * density)
            line(border, w)
            edge(canvas, l, t, r, bodyBottom, round, w)
        }
    }
}

/**
 * A convex, domed key like a big controller button: lit from the upper left, darker round the edge, standing on a
 * soft shadow. Pressed, the shadow goes and the key sinks and flattens.
 */
class DomeKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val lift = 2.5f * density
        val top = if (pressed) t + 1.5f * density else t
        val bottom = if (pressed) b else b - lift
        if (!pressed) {
            solid(withAlpha(darken(fill, 0.7f), 0x90))
            roundRect(canvas, l, t + lift, r, b, radius)
        }
        val w = r - l
        val h = bottom - top
        gradient(
            RadialGradient(
                l + w * 0.35f, top + h * 0.28f, max(w, h) * 0.8f,
                intArrayOf(lighten(fill, if (pressed) 0.12f else 0.32f), fill, darken(fill, 0.22f)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, top, r, bottom, radius)
        if (border != 0) {
            val outline = max(px, density)
            line(withAlpha(border, 0xA0), outline)
            edge(canvas, l, top, r, bottom, radius, outline)
        }
    }
}

/**
 * A slab: a flat top with a lit upper edge standing on a darker side wall, so the key looks cut from a block.
 * Pressed, the top drops onto the wall.
 */
class SlabKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val radius: Float,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val wall = 3.5f * density
        val drop = if (pressed) 1.5f * density else 0f
        val top = t + drop
        val faceBottom = b - wall + drop

        solid(darken(fill, 0.34f))
        roundRect(canvas, l, t + wall, r, b, radius)

        solid(fill)
        roundRect(canvas, l, top, r, faceBottom, radius)

        val inset = min(radius, (r - l) / 4f)
        line(withAlpha(lighten(fill, 0.5f), 0xA0), px)
        canvas.drawLine(l + inset, top + px, r - inset, top + px, paint)

        if (border != 0) {
            val w = max(px, density)
            line(border, w)
            edge(canvas, l, top, r, faceBottom, radius, w)
        }
    }
}

/**
 * A plate in a bezel: a lighter outer frame round a slightly darker inner plate, like a console button set into
 * its housing. Pressed, the plate darkens and shows a shadow under its top edge.
 */
class PlateKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val outer = 7f * density
        solid(lighten(fill, 0.22f))
        roundRect(canvas, l, t, r, b, outer)
        if (border != 0) {
            val w = max(px, 1.2f * density)
            line(border, w)
            edge(canvas, l, t, r, b, outer, w)
        }

        val gap = 3.5f * density
        if (r - l < 2f * gap + 6f * density || b - t < 2f * gap + 6f * density) return
        val inner = 4f * density
        solid(if (pressed) darken(fill, 0.12f) else fill)
        roundRect(canvas, l + gap, t + gap, r - gap, b - gap, inner)
        val w = max(px, density)
        line(withAlpha(darken(fill, 0.3f), 0xA0), w)
        edge(canvas, l + gap, t + gap, r - gap, b - gap, inner, w)

        val inset = l + gap + inner
        val end = r - gap - inner
        if (end > inset) {
            if (pressed) {
                line(withAlpha(darken(fill, 0.6f), 0xB0), 2f * density)
                canvas.drawLine(inset, t + gap + density, end, t + gap + density, paint)
            } else {
                line(withAlpha(lighten(fill, 0.6f), 0x90), px)
                canvas.drawLine(inset, t + gap + px, end, t + gap + px, paint)
            }
        }
    }
}

/**
 * An etched metal key: a nearly square plate with a brushed sheen across it, a silver edge, and fine light and
 * dark lines inside the top and bottom edges. Pressed, an inner ring lights up like a backlit key.
 */
class EtchedKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val pressed: Boolean,
    insetX: Float,
    insetY: Float,
    density: Float,
) : KeyShapeDrawable(insetX, insetY, density) {
    override fun drawKey(canvas: Canvas, l: Float, t: Float, r: Float, b: Float) {
        val radius = 2f * density
        solid(fill)
        roundRect(canvas, l, t, r, b, radius)
        if (border == 0) return

        gradient(
            LinearGradient(
                l, 0f, r, 0f,
                intArrayOf(withAlpha(border, 0), withAlpha(border, 0x30), withAlpha(border, 0)),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)

        val w = max(px, 1.2f * density)
        line(border, w)
        edge(canvas, l, t, r, b, radius, w)

        val inset = 3f * density
        line(withAlpha(lighten(border, 0.4f), 0x70), px)
        canvas.drawLine(l + inset, t + 2.5f * density, r - inset, t + 2.5f * density, paint)
        line(withAlpha(darken(fill, 0.5f), 0x90), px)
        canvas.drawLine(l + inset, b - 2.5f * density, r - inset, b - 2.5f * density, paint)

        if (pressed) {
            val glowWidth = 1.6f * density
            line(lighten(border, 0.5f), glowWidth)
            edge(canvas, l + 2f * density, t + 2f * density, r - 2f * density, b - 2f * density, radius, glowWidth)
        }
    }
}

/**
 * A moulded plastic key: a gentle top-to-bottom shading and a bevel round the face, lit along the top and left and
 * shadowed along the bottom, with a thin edge. Pressed, the bevel reverses so the key looks sunk.
 */
class MouldedKeyDrawable(
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
                if (pressed) fill else lighten(fill, 0.10f), darken(fill, if (pressed) 0.10f else 0.06f),
                Shader.TileMode.CLAMP,
            ),
        )
        roundRect(canvas, l, t, r, b, radius)

        val w = max(px, 1.6f * density)
        val i = 2f * density
        val mid = t + (b - t) * 0.5f
        val lit = if (pressed) darken(fill, 0.3f) else lighten(fill, 0.5f)
        val shaded = if (pressed) lighten(fill, 0.4f) else darken(fill, 0.3f)

        canvas.save()
        canvas.clipRect(l, t, r, mid)
        line(lit, w)
        edge(canvas, l + i, t + i, r - i, b - i, radius - i, w)
        canvas.restore()

        canvas.save()
        canvas.clipRect(l, mid, r, b)
        line(shaded, w)
        edge(canvas, l + i, t + i, r - i, b - i, radius - i, w)
        canvas.restore()

        if (border != 0) {
            val outline = max(px, density)
            line(border, outline)
            edge(canvas, l, t, r, b, radius, outline)
        }
    }
}
