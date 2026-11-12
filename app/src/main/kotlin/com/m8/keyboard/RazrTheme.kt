// app/src/main/kotlin/com/m8/keyboard/RazrTheme.kt
package com.m8.keyboard

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Motorola RAZR V3 look.
 *
 * The V3 keypad was a single sheet of etched metal with a flat, wide, chamfered key for each
 * number, thin dark grooves between the keys, and electroluminescent blue light that shows
 * through the numerals and leaks out of the grooves. This file draws that:
 *
 *  - [RazrPlate]            one etched metal key plate, including the blue "light up" effect.
 *  - [RazrKeyDrawable]      a plate that lights when its view is pressed, then fades like an EL panel.
 *  - [RazrControlCluster]   the two soft keys and the circular navigation ring above the keypad.
 *  - [RazrDisplayDrawable]  the glass screen behind the suggestion strip.
 *  - [RazrBackdropDrawable] the brushed gunmetal body of the phone.
 */

internal object Razr {
    val BLUE = 0xFF2AB4FF.toInt()      // electroluminescent blue
    val CYAN = 0xFFA8EBFF.toInt()      // the lit, hot part of a glowing edge
    val CORE = 0xFFEAFBFF.toInt()      // near-white centre of a lit numeral
    val IDLE_EDGE = 0xFF2A7FAA.toInt() // outline of a key that is not being pressed
    val GROOVE = 0xFF03060A.toInt()    // the gap between keys
}

internal fun withAlpha(color: Int, a: Float): Int =
    (color and 0x00FFFFFF) or ((a.coerceIn(0f, 1f) * 255f + 0.5f).toInt() shl 24)

internal fun lerpColor(a: Int, b: Int, t: Float): Int {
    val k = t.coerceIn(0f, 1f)
    fun ch(shift: Int): Int {
        val x = (a shr shift) and 0xFF
        val y = (b shr shift) and 0xFF
        return (x + (y - x) * k).toInt().coerceIn(0, 255)
    }
    return (ch(24) shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

internal fun shade(color: Int, factor: Float): Int {
    fun ch(shift: Int) = (((color shr shift) and 0xFF) * factor).toInt().coerceIn(0, 255)
    return (color and 0xFF000000.toInt()) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
}

/**
 * One piece of etched metal. [set] gives it a shape; [draw] paints it with [glow] from 0 (idle,
 * backlight barely visible in the grooves) to 1 (pressed: the plate is lit from underneath, the
 * edge burns bright and a halo spills into the gap around it).
 */
internal class RazrPlate(
    private val d: Float,
    private val topColor: Int,
    private val bottomColor: Int,
    private val idle: Float = 0.16f,
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val box = RectF()
    private var body: Shader? = null
    private var bevel: Shader? = null
    private var underlight: Shader? = null
    private var brushed = FloatArray(0)

    val bounds: RectF get() = box

    fun set(shape: Path, bounds: RectF) {
        path.set(shape)
        box.set(bounds)
        if (box.width() <= 0f || box.height() <= 0f) return

        body = LinearGradient(0f, box.top, 0f, box.bottom, topColor, bottomColor, Shader.TileMode.CLAMP)
        bevel = LinearGradient(
            0f, box.top, 0f, box.bottom,
            intArrayOf(0x88FFFFFF.toInt(), 0x1CFFFFFF, 0x00000000, 0x77000000),
            floatArrayOf(0f, 0.16f, 0.58f, 1f),
            Shader.TileMode.CLAMP,
        )
        underlight = RadialGradient(
            box.centerX(), box.centerY() + box.height() * 0.08f,
            max(box.width(), box.height()) * 0.62f,
            0xD02AB4FF.toInt(), 0x002AB4FF, Shader.TileMode.CLAMP,
        )

        // Fine horizontal grain, like brushed aluminium.
        val step = max(1.6f, 1.9f * d)
        val n = (box.height() / step).toInt() + 1
        brushed = FloatArray(n * 4)
        for (i in 0 until n) {
            val y = box.top + i * step
            brushed[i * 4] = box.left
            brushed[i * 4 + 1] = y
            brushed[i * 4 + 2] = box.right
            brushed[i * 4 + 3] = y
        }
    }

    fun draw(canvas: Canvas, g: Float) {
        if (box.isEmpty) return
        val cx = box.centerX()
        val cy = box.centerY()
        canvas.save()
        val s = 1f - 0.03f * g // the plate sinks a hair when pressed
        canvas.scale(s, s, cx, cy)

        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.strokeJoin = Paint.Join.ROUND

        // Groove / lower lip.
        paint.color = Razr.GROOVE
        canvas.save()
        canvas.translate(0f, 1.2f * d)
        canvas.drawPath(path, paint)
        canvas.restore()

        // Halo spilling out into the gap around a lit key.
        if (g > 0.01f) {
            paint.style = Paint.Style.STROKE
            for (i in BLOOM_W.indices) {
                paint.strokeWidth = BLOOM_W[i] * d
                paint.color = withAlpha(Razr.BLUE, BLOOM_A[i] * g)
                canvas.drawPath(path, paint)
            }
        }

        // Metal body.
        paint.style = Paint.Style.FILL
        paint.shader = body
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawPath(path, paint)
        paint.shader = null

        canvas.save()
        canvas.clipPath(path)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0x0EFFFFFF
        canvas.drawLines(brushed, paint)

        // Light from beneath the plate.
        paint.style = Paint.Style.FILL
        paint.shader = underlight
        paint.alpha = ((0.08f + 0.92f * g) * 255f).toInt().coerceIn(0, 255)
        canvas.drawRect(box, paint)
        paint.shader = null
        paint.alpha = 255

        // Light leaking in from the edges.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f * d
        paint.color = withAlpha(Razr.BLUE, 0.08f + 0.42f * g)
        canvas.drawPath(path, paint)
        paint.strokeWidth = 2.2f * d
        paint.color = withAlpha(Razr.CYAN, 0.12f + 0.68f * g)
        canvas.drawPath(path, paint)
        canvas.restore()

        // Bevel: catches light along the top, falls into shadow along the bottom.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.1f * d
        paint.shader = bevel
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawPath(path, paint)
        paint.shader = null

        // Crisp outline: dull steel-blue at rest, white-hot when lit.
        paint.strokeWidth = 1f * d
        paint.color = withAlpha(lerpColor(Razr.IDLE_EDGE, Razr.CORE, g), 0.55f + 0.45f * g)
        canvas.drawPath(path, paint)

        canvas.restore()
    }

    private companion object {
        val BLOOM_W = floatArrayOf(6f, 4f, 2.5f)
        val BLOOM_A = floatArrayOf(0.10f, 0.18f, 0.34f)
    }
}

/**
 * A V3 number or function key. Number keys are wide and flat with chamfered corners and a
 * barely arched top and bottom edge. The two outer keys of the bottom row follow the curve of
 * the phone's body. When the view is pressed the key lights almost instantly, then fades out
 * over about half a second like an EL panel, so a quick tap still leaves a visible glow.
 *
 * [onGlow] reports the light level so the label can glow with the plate.
 */
class RazrKeyDrawable(
    density: Float,
    private val insetX: Float,
    private val insetY: Float,
    fill: Int,
    private val row: Int = -1,
    private val column: Int = -1,
) : Drawable() {
    private val plate = RazrPlate(density, shade(fill, 1.28f), shade(fill, 0.68f))
    private val shape = Path()
    private val box = RectF()
    private var down = false
    private var animator: ValueAnimator? = null

    var glow = 0f
        private set
    var onGlow: ((Float) -> Unit)? = null

    override fun onBoundsChange(bounds: Rect) {
        val l = bounds.left + insetX
        val t = bounds.top + insetY
        val r = bounds.right - insetX
        val b = bounds.bottom - insetY
        if (r <= l || b <= t) {
            box.setEmpty()
            plate.set(shape.apply { reset() }, box)
            return
        }
        box.set(l, t, r, b)
        buildShape(l, t, r, b)
        plate.set(shape, box)
    }

    private fun buildShape(l: Float, t: Float, r: Float, b: Float) {
        val w = r - l
        val h = b - t
        val c = min(h * 0.30f, w * 0.14f)      // chamfer
        val bow = h * 0.035f                   // top and bottom edges arch outward a touch
        val body = min(h * 0.55f, w * 0.30f)   // radius of the phone-body corners on the bottom row
        val roundLeft = row == 3 && column == 0
        val roundRight = row == 3 && column == 2
        val mid = (l + r) / 2f

        shape.reset()
        shape.moveTo(l + c, t)
        shape.quadTo(mid, t - bow * 2f, r - c, t)
        shape.lineTo(r, t + c)
        if (roundRight) {
            shape.lineTo(r, b - body)
            shape.quadTo(r, b, r - body, b)
        } else {
            shape.lineTo(r, b - c)
            shape.lineTo(r - c, b)
        }
        shape.quadTo(mid, b + bow * 2f, if (roundLeft) l + body else l + c, b)
        if (roundLeft) {
            shape.quadTo(l, b, l, b - body)
        } else {
            shape.lineTo(l, b - c)
        }
        shape.lineTo(l, t + c)
        shape.close()
    }

    override fun draw(canvas: Canvas) {
        plate.draw(canvas, glow)
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val pressed = state.contains(android.R.attr.state_pressed)
        if (pressed == down) return false
        down = pressed
        if (pressed) {
            animateTo(1f, 45L, glow, LinearInterpolator())
        } else {
            // Even a very quick tap flashes to (nearly) full before it fades.
            animateTo(0f, 650L, max(glow, 0.92f), DecelerateInterpolator(1.8f))
        }
        return true
    }

    private fun animateTo(target: Float, ms: Long, from: Float, interpolator: android.animation.TimeInterpolator) {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(from, target).apply {
            duration = ms
            this.interpolator = interpolator
            addUpdateListener { setGlow(it.animatedValue as Float) }
            start()
        }
    }

    private fun setGlow(g: Float) {
        glow = g
        onGlow?.invoke(g)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Makes the label (or icon) on a RAZR key glow in step with its plate. Does nothing for other themes. */
fun KeyboardTheme.bindKeyGlow(tv: TextView) {
    val bg = tv.background as? RazrKeyDrawable ?: return
    val d = tv.resources.displayMetrics.density
    val idleColor = tv.currentTextColor
    val icon = tv as? IconKeyView
    fun light(g: Float) {
        if (icon != null) {
            icon.glow = g
        } else {
            tv.setTextColor(lerpColor(idleColor, Razr.CORE, g))
            tv.setShadowLayer((3f + 9f * g) * d, 0f, 0f, withAlpha(Razr.BLUE, 0.35f + 0.65f * g))
        }
    }
    bg.onGlow = { light(it) }
    light(0f)
}

/** The brushed gunmetal body of the phone: faint grain, a soft sheen and a polished top edge. */
class RazrBackdropDrawable(private val d: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var base: Shader? = null
    private var sheen: Shader? = null
    private var rim: Shader? = null
    private var grain = FloatArray(0)

    override fun onBoundsChange(b: Rect) {
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        base = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF1B222A.toInt(), 0xFF0D1115.toInt(), 0xFF080B0E.toInt()),
            floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP,
        )
        sheen = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(0x00FFFFFF, 0x0DFFFFFF, 0x00FFFFFF, 0x09FFFFFF, 0x00FFFFFF),
            floatArrayOf(0f, 0.25f, 0.5f, 0.8f, 1f), Shader.TileMode.CLAMP,
        )
        rim = LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(0x00C4CED5, 0xFFC4CED5.toInt(), 0xFFC4CED5.toInt(), 0x00C4CED5),
            floatArrayOf(0f, 0.12f, 0.88f, 1f), Shader.TileMode.CLAMP,
        )
        val step = max(1.6f, 2f * d)
        val n = (h / step).toInt() + 1
        grain = FloatArray(n * 4)
        for (i in 0 until n) {
            grain[i * 4] = 0f
            grain[i * 4 + 1] = i * step
            grain[i * 4 + 2] = w
            grain[i * 4 + 3] = i * step
        }
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())

        paint.style = Paint.Style.FILL
        paint.shader = base
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = sheen
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = 0x09FFFFFF
        canvas.drawLines(grain, paint)

        // Polished edge where the keypad meets the screen half of the phone.
        paint.shader = rim
        paint.strokeWidth = 1.2f * d
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawLine(0f, 0.6f * d, w, 0.6f * d, paint)
        paint.shader = null
        paint.strokeWidth = 1f * d
        paint.color = 0xAA000000.toInt()
        canvas.drawLine(0f, 1.8f * d, w, 1.8f * d, paint)

        canvas.restore()
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/** The dark glass screen behind the suggestion strip, with a thin lit-blue edge. */
class RazrDisplayDrawable(private val d: Float) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var glass: Shader? = null

    override fun onBoundsChange(b: Rect) {
        rect.set(b.left + 3f * d, b.top + 3f * d, b.right - 3f * d, b.bottom - 3f * d)
        glass = LinearGradient(
            0f, rect.top, 0f, rect.bottom,
            0xFF020A11.toInt(), 0xFF082131.toInt(), Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        if (rect.isEmpty) return
        val r = 8f * d
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * d
        paint.color = withAlpha(Razr.BLUE, 0.10f)
        canvas.drawRoundRect(rect, r, r, paint)
        paint.strokeWidth = 2.4f * d
        paint.color = withAlpha(Razr.BLUE, 0.18f)
        canvas.drawRoundRect(rect, r, r, paint)

        paint.style = Paint.Style.FILL
        paint.shader = glass
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawRoundRect(rect, r, r, paint)
        paint.shader = null

        // Glare across the top half of the glass.
        canvas.save()
        canvas.clipRect(rect.left, rect.top, rect.right, rect.centerY())
        paint.color = 0x12FFFFFF
        canvas.drawRoundRect(rect, r, r, paint)
        canvas.restore()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.1f * d
        paint.color = 0xFF2A86B5.toInt()
        canvas.drawRoundRect(rect, r, r, paint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * The control cluster above the keypad: a soft key either side of the circular navigation ring,
 * as on the V3. The left soft key switches to the full keyboard, the right one is Enter. The ring
 * moves through suggestions (left / right) and its centre button picks the highlighted one.
 * Each part lights up, then fades, exactly like the number keys.
 */
class RazrControlCluster(
    context: Context,
    private val settings: KeyboardSettings,
    private val onQwerty: () -> Unit,
    private val onEnter: () -> Unit,
    private val onMove: (Int) -> Unit,
    private val onSelect: () -> Unit,
) : View(context) {

    private val d = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scratch = Path()
    private val iconPath = Path()

    private val plateTop = 0xFF3A444C.toInt()
    private val plateBottom = 0xFF1B2127.toInt()
    private val plates = Array(ZONES) { RazrPlate(d, plateTop, plateBottom) }
    private val glow = FloatArray(ZONES)
    private var held = NONE
    private var lastTick = 0L

    private var cx = 0f
    private var cy = 0f
    private var ringR = 0f
    private var bezel: Shader? = null
    private var leftIconX = 0f
    private var rightIconX = 0f

    init {
        isClickable = true
        contentDescription =
            "Soft keys and navigation pad. Left soft key: full keyboard. Right soft key: enter. " +
                "Pad left and right move through suggestions, centre selects."
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        cx = w / 2f
        cy = h / 2f
        ringR = min(h / 2f - 2f * d, 38f * d)

        bezel = SweepGradient(
            cx, cy,
            intArrayOf(
                0xFF4A545C.toInt(), 0xFFA9B4BB.toInt(), 0xFF2F373D.toInt(), 0xFF98A3AA.toInt(),
                0xFF4A545C.toInt(), 0xFFA9B4BB.toInt(), 0xFF2F373D.toInt(), 0xFF98A3AA.toInt(),
                0xFF4A545C.toInt(),
            ),
            null,
        )

        // Four direction segments of the ring, angles measured clockwise from "right".
        val rOut = ringR * 0.80f
        val rIn = ringR * 0.40f
        val outer = RectF(cx - rOut, cy - rOut, cx + rOut, cy + rOut)
        val inner = RectF(cx - rIn, cy - rIn, cx + rIn, cy + rIn)
        val gap = 5f
        for (zone in intArrayOf(UP, RIGHT, DOWN, LEFT)) {
            val start = CENTRE_ANGLE[zone] - 45f + gap / 2f
            val sweep = 90f - gap
            val p = Path()
            p.arcTo(outer, start, sweep, true)
            p.arcTo(inner, start + sweep, -sweep, false)
            p.close()
            val bb = RectF()
            p.computeBounds(bb, true)
            plates[zone].set(p, bb)
        }

        // Centre button.
        val okR = ringR * 0.34f
        val ok = Path().apply { addCircle(cx, cy, okR, Path.Direction.CW) }
        plates[OK].set(ok, RectF(cx - okR, cy - okR, cx + okR, cy + okR))

        // Soft keys: wide chamfered plates whose inner edge is scooped out to wrap the ring.
        val pad = 2f * d
        val c = 10f * d
        val top = cy - ringR * 0.80f
        val bottom = cy + ringR * 0.80f
        val hole = Path().apply { addCircle(cx, cy, ringR + 5f * d, Path.Direction.CW) }

        val left = Path().apply {
            moveTo(pad + c, top); lineTo(cx, top); lineTo(cx, bottom); lineTo(pad + c, bottom)
            lineTo(pad, bottom - c); lineTo(pad, top + c); close()
            op(hole, Path.Op.DIFFERENCE)
        }
        val right = Path().apply {
            moveTo(cx, top); lineTo(w - pad - c, top); lineTo(w - pad, top + c)
            lineTo(w - pad, bottom - c); lineTo(w - pad - c, bottom); lineTo(cx, bottom); close()
            op(hole, Path.Op.DIFFERENCE)
        }
        val lb = RectF(); left.computeBounds(lb, true)
        val rb = RectF(); right.computeBounds(rb, true)
        plates[SOFT_L].set(left, lb)
        plates[SOFT_R].set(right, rb)
        leftIconX = (pad + cx - ringR - 5f * d) / 2f
        rightIconX = w - leftIconX
    }

    override fun onDraw(canvas: Canvas) {
        if (ringR <= 0f) return

        // Chrome bezel around the ring, with the groove and a thin ring of blue light inside it.
        paint.style = Paint.Style.FILL
        paint.shader = bezel
        canvas.drawCircle(cx, cy, ringR, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * d
        paint.color = 0xFF04070A.toInt()
        canvas.drawCircle(cx, cy, ringR - 0.5f * d, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF04080C.toInt()
        canvas.drawCircle(cx, cy, ringR * 0.85f, paint)
        var ringLight = 0f
        for (z in UP..OK) ringLight = max(ringLight, glow[z])
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * d
        paint.color = withAlpha(Razr.BLUE, 0.08f + 0.22f * ringLight)
        canvas.drawCircle(cx, cy, ringR * 0.825f, paint)
        paint.strokeWidth = 1.2f * d
        paint.color = withAlpha(Razr.BLUE, 0.35f + 0.50f * ringLight)
        canvas.drawCircle(cx, cy, ringR * 0.825f, paint)

        for (z in intArrayOf(UP, RIGHT, DOWN, LEFT, OK, SOFT_L, SOFT_R)) plates[z].draw(canvas, glow[z])

        // Direction arrows on the ring.
        for (zone in intArrayOf(UP, RIGHT, DOWN, LEFT)) {
            val a = Math.toRadians(CENTRE_ANGLE[zone].toDouble())
            val x = cx + ringR * 0.60f * cos(a).toFloat()
            val y = cy + ringR * 0.60f * sin(a).toFloat()
            drawArrow(canvas, x, y, CENTRE_ANGLE[zone] + 90f, glow[zone])
        }

        // Centre button: a small lit dot.
        val g = glow[OK]
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(Razr.BLUE, 0.20f + 0.35f * g)
        canvas.drawCircle(cx, cy, (4.6f + 2f * g) * d, paint)
        paint.color = lerpColor(Razr.BLUE, Razr.CORE, g)
        canvas.drawCircle(cx, cy, 2.2f * d, paint)

        // Soft key icons.
        drawIcon(canvas, KeyIcon.KEYBOARD, leftIconX, glow[SOFT_L])
        drawIcon(canvas, KeyIcon.ENTER, rightIconX, glow[SOFT_R])
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float, rotation: Float, g: Float) {
        val s = 3.8f * d
        scratch.reset()
        scratch.moveTo(0f, -s)
        scratch.lineTo(-s * 0.95f, s * 0.65f)
        scratch.lineTo(s * 0.95f, s * 0.65f)
        scratch.close()
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(rotation)
        paint.strokeJoin = Paint.Join.ROUND
        if (g > 0.02f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5f * d
            paint.color = withAlpha(Razr.BLUE, 0.28f * g)
            canvas.drawPath(scratch, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = lerpColor(0xFF6FCBF5.toInt(), Razr.CORE, g)
        canvas.drawPath(scratch, paint)
        canvas.restore()
    }

    private fun drawIcon(canvas: Canvas, icon: KeyIcon, x: Float, g: Float) {
        val u = 15f * d
        val idle = 0xFF8DDCFF.toInt()
        if (g > 0.02f) {
            drawKeyIcon(canvas, icon, x, cy, u, withAlpha(Razr.BLUE, 0.30f * g), 5f * d, paint, iconPath)
            drawKeyIcon(canvas, icon, x, cy, u, withAlpha(Razr.CYAN, 0.50f * g), 3f * d, paint, iconPath)
        }
        drawKeyIcon(canvas, icon, x, cy, u, lerpColor(idle, Razr.CORE, g), 1.8f * d, paint, iconPath)
    }

    private fun zoneAt(x: Float, y: Float): Int {
        val dx = x - cx
        val dy = y - cy
        if (hypot(dx, dy) <= ringR) {
            if (hypot(dx, dy) < ringR * 0.40f) return OK
            return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                if (dx < 0f) LEFT else RIGHT
            } else {
                if (dy < 0f) UP else DOWN
            }
        }
        return if (x < cx) SOFT_L else SOFT_R
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                held = zoneAt(event.x, event.y)
                settings.feedback(this)
                startTicking()
            }
            MotionEvent.ACTION_MOVE -> {
                // Sliding off the part you pressed cancels the press.
                if (held != NONE && zoneAt(event.x, event.y) != held) held = NONE
            }
            MotionEvent.ACTION_UP -> {
                val zone = held
                held = NONE
                if (zone != NONE) {
                    glow[zone] = max(glow[zone], 0.92f) // a quick tap still flashes
                    startTicking()
                    when (zone) {
                        SOFT_L -> onQwerty()
                        SOFT_R -> onEnter()
                        LEFT -> onMove(-1)
                        RIGHT -> onMove(1)
                        OK -> onSelect()
                        else -> Unit
                    }
                    performClick()
                }
            }
            MotionEvent.ACTION_CANCEL -> held = NONE
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val dt = (now - lastTick).coerceIn(1L, 50L).toFloat()
            lastTick = now
            var active = false
            for (z in 0 until ZONES) {
                if (z == held) {
                    glow[z] = min(1f, glow[z] + dt / 45f)
                    active = true // keep running until the finger lifts
                } else if (glow[z] > 0f) {
                    glow[z] *= exp(-dt / 210f) // EL afterglow: quick drop, long soft tail
                    if (glow[z] < 0.01f) glow[z] = 0f
                    if (glow[z] > 0f) active = true
                }
            }
            invalidate()
            if (active) postOnAnimation(this)
        }
    }

    private fun startTicking() {
        removeCallbacks(tick)
        lastTick = SystemClock.uptimeMillis()
        postOnAnimation(tick)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    private companion object {
        const val NONE = -1
        const val SOFT_L = 0
        const val SOFT_R = 1
        const val UP = 2
        const val RIGHT = 3
        const val DOWN = 4
        const val LEFT = 5
        const val OK = 6
        const val ZONES = 7

        /** Centre of each ring segment, in degrees clockwise from the +x axis. */
        val CENTRE_ANGLE = floatArrayOf(0f, 0f, 270f, 0f, 90f, 180f, 0f)
    }
}
