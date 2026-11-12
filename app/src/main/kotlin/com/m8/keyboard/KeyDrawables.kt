// app/src/main/kotlin/com/m8/keyboard/KeyDrawables.kt
package com.m8.keyboard

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * One slanted key: a parallelogram (top edge pushed right, bottom edge left) with a white
 * outline and a dark-accent copy offset behind it. The drawable only
 * paints inside its inset; the view keeps its full cell as touch area.
 * [leftFrac]/[rightFrac] pull the shape inwards (fractions of the width) for wide end keys.
 */
class SlantKeyDrawable(
    private val fill: Int,
    private val border: Int,
    private val shadow: Int,
    private val insetX: Float,
    private val insetY: Float,
    private val slant: Float,
    private val borderWidth: Float,
    private val shadowOffset: Float,
    private val leftFrac: Float,
    private val rightFrac: Float,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val left = b.left + insetX + leftFrac * w
        val right = b.right - insetX - rightFrac * w - shadowOffset
        val top = b.top + insetY
        val bottom = b.bottom - insetY - shadowOffset
        if (right <= left || bottom <= top) return
        val s = minOf(slant, (right - left) / 3f)

        if (shadowOffset > 0f) {
            shape(left + shadowOffset, top + shadowOffset, right + shadowOffset, bottom + shadowOffset, s)
            paint.style = Paint.Style.FILL
            paint.color = shadow
            canvas.drawPath(path, paint)
        }
        shape(left, top, right, bottom, s)
        paint.style = Paint.Style.FILL
        paint.color = fill
        canvas.drawPath(path, paint)
        if (borderWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.strokeJoin = Paint.Join.MITER
            paint.color = border
            canvas.drawPath(path, paint)
        }
    }

    private fun shape(l: Float, t: Float, r: Float, b: Float, s: Float) {
        path.reset()
        path.moveTo(l + s, t)
        path.lineTo(r, t)
        path.lineTo(r - s, b)
        path.lineTo(l, b)
        path.close()
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

/** Raised, slightly domed plastic keycaps inspired by the original Nokia 3310 keypad. */
class NokiaKeyDrawable(
    private val fill: Int,
    private val shadow: Int,
    private val insetX: Float,
    private val insetY: Float,
    private val radius: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX
        val bottom = b.bottom - insetY
        if (r <= l || bottom <= t) return
        val keyRadius = radius

        // A deeper lower lip and soft cast shadow make the key stand proud of the shell.
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = shadow
        rect.set(l + 0.5f, t + 2.8f, r + 0.5f, bottom + 0.5f)
        canvas.drawRoundRect(rect, keyRadius, keyRadius, paint)

        // Gentle vertical shading suggests a convex moulded plastic surface.
        rect.set(l, t, r, bottom - 2f)
        paint.shader = LinearGradient(l, t, l, bottom - 2f,
            intArrayOf(0xFFFFFFFF.toInt(), fill, fill), null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, keyRadius, keyRadius, paint)
        paint.shader = null

        // Fine inset rim and a narrow highlight along the upper edge.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = 0x66515D61
        rect.set(l + 0.8f, t + 0.8f, r - 0.8f, bottom - 2.8f)
        canvas.drawRoundRect(rect, maxOf(1f, keyRadius - 1f), maxOf(1f, keyRadius - 1f), paint)
        paint.strokeWidth = 1.1f
        paint.color = 0x99FFFFFF.toInt()
        rect.set(l + 1.5f, t + 1.1f, r - 1.5f, t + 3f)
        canvas.drawRoundRect(rect, keyRadius, keyRadius, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Etched metal key from the RAZR V3 keypad. Number keys use the gently waisted,
 * interlocking outline of the original laser-etched keypad rather than a rounded
 * rectangular keycap. The blue edge is deliberately drawn as illumination, not paint.
 */
class RazrKeyDrawable(
    private val fill: Int,
    private val shadow: Int,
    private val insetX: Float,
    private val insetY: Float,
    private val radius: Float,
    private val pressed: Boolean,
    private val numberKey: Boolean = false,
    private val row: Int = -1,
    private val column: Int = -1,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX
        val bottom = b.bottom - insetY
        if (r <= l || bottom <= t) return

        val cy = (t + bottom) / 2f
        val w = r - l
        val h = bottom - t
        val corner = minOf(radius * 1.8f, h * 0.22f, w * 0.18f)
        val waist = if (numberKey) minOf(3.2f, w * 0.045f, h * 0.07f) else 0f
        val sideBias = when (column) { 0 -> -0.25f; 2 -> 0.25f; else -> 0f }
        val rowShape = if (row % 2 == 0) 1f else 0.9f
        val n = waist * (1f + sideBias) * rowShape

        path.reset()
        path.moveTo(l + corner, t)
        path.lineTo(r - corner, t)
        path.quadTo(r, t, r, t + corner)
        if (numberKey) {
            path.lineTo(r, cy - n * 1.5f)
            path.cubicTo(r, cy - n * 0.35f, r - n * 0.2f, cy - n * 0.1f, r - n, cy)
            path.cubicTo(r - n * 0.2f, cy + n * 0.1f, r, cy + n * 0.35f, r, cy + n * 1.5f)
        }
        path.lineTo(r, bottom - corner)
        path.quadTo(r, bottom, r - corner, bottom)
        path.lineTo(l + corner, bottom)
        path.quadTo(l, bottom, l, bottom - corner)
        if (numberKey) {
            path.lineTo(l, cy + n * 1.5f)
            path.cubicTo(l, cy + n * 0.35f, l + n * 0.2f, cy + n * 0.1f, l + n, cy)
            path.cubicTo(l + n * 0.2f, cy - n * 0.1f, l, cy - n * 0.35f, l, cy - n * 1.5f)
        }
        path.lineTo(l, t + corner)
        path.quadTo(l, t, l + corner, t)
        path.close()

        // A very shallow dark offset gives the etched plate a tactile boundary.
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = shadow
        canvas.save()
        canvas.translate(0f, 1.5f)
        canvas.drawPath(path, paint)
        canvas.restore()

        paint.shader = LinearGradient(l, t, r, bottom,
            intArrayOf(0xFF4A555D.toInt(), fill, 0xFF20282E.toInt()),
            floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP)
        paint.color = android.graphics.Color.WHITE
        canvas.drawPath(path, paint)
        paint.shader = null

        // Wide, low-opacity blue bloom under a crisp electroluminescent outline.
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = if (pressed) 4.2f else 2.6f
        paint.color = if (pressed) 0xAA55D8FF.toInt() else 0x6650BCEB
        canvas.drawPath(path, paint)
        paint.strokeWidth = if (pressed) 1.8f else 1.15f
        paint.color = if (pressed) 0xFFB8F0FF.toInt() else 0xFF399BC8.toInt()
        canvas.drawPath(path, paint)

        // A fine cool-metal inner line keeps the plate visible in daylight too.
        paint.strokeWidth = 0.65f
        paint.color = 0x887F9AA7.toInt()
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** The marker in front of a choice: a diamond, filled with the accent when picked. */
class DiamondDrawable(
    private val selected: Boolean,
    private val accent: Int,
    private val density: Float,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun draw(canvas: Canvas) {
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val half = 6f * density
        canvas.save()
        canvas.rotate(45f, cx, cy)
        if (selected) {
            paint.style = Paint.Style.FILL
            paint.color = accent
            canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = if (selected) accent else 0xFF727272.toInt() // dim grey
        canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
        canvas.restore()
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

/** Screen backdrop: black, faint diagonal pinstripes, accent wedges in two corners. */
class BackdropDrawable(
    private val base: Int,
    private val stripe: Int,
    private val red: Int,
    private val deep: Int,
    private val density: Float,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())

        paint.style = Paint.Style.FILL
        paint.color = base
        canvas.drawRect(0f, 0f, w, h, paint)

        // Pinstripes.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = stripe
        val gap = 22f * density
        var x = -h
        while (x < w) {
            canvas.drawLine(x, h, x + h, 0f, paint)
            x += gap
        }

        // Top right: a dark wedge with a bright one on top of it. Bottom left: a dark one.
        paint.style = Paint.Style.FILL
        wedge(canvas, deep, w * 0.40f, 0f, w, 0f, w, h * 0.15f)
        wedge(canvas, red, w * 0.66f, 0f, w, 0f, w, h * 0.09f)
        wedge(canvas, deep, 0f, h, 0f, h * 0.90f, w * 0.30f, h)
        canvas.restore()
    }

    private fun wedge(c: Canvas, color: Int, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        path.reset()
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
        path.lineTo(x3, y3)
        path.close()
        paint.color = color
        c.drawPath(path, paint)
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

/** The keyboard's background for this theme (plain colour, or the pinstriped backdrop). */
fun KeyboardTheme.backgroundDrawable(view: View): Drawable =
    if (isSlanted) {
        BackdropDrawable(
            base = background,
            stripe = 0x08FFFFFF,
            red = accent,
            deep = deep,
            density = view.resources.displayMetrics.density,
        )
    } else {
        ColorDrawable(background)
    }

/** Sets [view]'s background for this theme. */
fun KeyboardTheme.applyBackground(view: View) {
    view.background = backgroundDrawable(view)
}

/**
 * Wraps a background and, on [pulse], washes it with a glow of the accent colour (rising from the
 * bottom centre, where thumbs are) that fades out. [strength] is the glow's peak opacity; 0 disables it.
 */
class PulseBackground(
    private val base: Drawable,
    private val accent: Int,
    private val strength: Float,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var level = 0f
    private var animator: ValueAnimator? = null

    override fun onBoundsChange(bounds: Rect) {
        base.bounds = bounds
        val size = maxOf(bounds.width(), bounds.height()).toFloat()
        paint.shader = RadialGradient(
            bounds.exactCenterX(),
            bounds.bottom.toFloat(),
            size * 0.95f,
            accent,
            accent and 0x00FFFFFF,
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        base.draw(canvas)
        if (level > 0f && strength > 0f) {
            paint.alpha = (255f * strength * level).toInt().coerceIn(0, 255)
            canvas.drawRect(bounds, paint)
        }
    }

    /** Flash now and fade out. */
    fun pulse() {
        if (strength <= 0f) return
        animator?.cancel()
        animator = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 450L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                level = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/**
 * Background for a key. The gap between keys is drawn *inside* the key's cell, so the whole cell
 * is touchable. [action] picks the action-key colours (DEL, ENTER, shift).
 */
fun KeyboardTheme.keyBackground(
    density: Float,
    action: Boolean,
    gapXDp: Int,
    gapYDp: Int,
    cornerDp: Int,
    leftFrac: Float = 0f,
    rightFrac: Float = 0f,
    razrRow: Int = -1,
    razrColumn: Int = -1,
): StateListDrawable {
    val normal = if (action) actionKey else key
    val pressed = if (action) actionKeyPressed else keyPressed

    fun layer(color: Int): Drawable {
        if (isRazr) {
            return RazrKeyDrawable(
                fill = color,
                shadow = this.shadow,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                radius = 3.5f * density,
                pressed = color == pressed,
                numberKey = razrRow >= 0 && razrColumn >= 0,
                row = razrRow,
                column = razrColumn,
            )
        }
        if (isNokia) {
            return NokiaKeyDrawable(
                fill = color,
                shadow = this.shadow,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                radius = 7f * density,
            )
        }
        if (isSlanted) {
            return SlantKeyDrawable(
                fill = color,
                border = border,
                shadow = shadow,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                slant = 7f * density,
                borderWidth = 1.5f * density,
                shadowOffset = 2f * density,
                leftFrac = leftFrac,
                rightFrac = rightFrac,
            )
        }
        val shape = GradientDrawable().apply {
            setColor(color)
            cornerRadius = cornerDp * density
        }
        val gx = (gapXDp * density).toInt()
        val gy = (gapYDp * density).toInt()
        val gapped = InsetDrawable(shape, gx, gy, gx, gy)
        return if (leftFrac > 0f || rightFrac > 0f) InsetDrawable(gapped, leftFrac, 0f, rightFrac, 0f) else gapped
    }

    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), layer(pressed))
        addState(intArrayOf(), layer(normal))
    }
}

/** Highlight behind the selected candidate word. */
fun KeyboardTheme.candidateBackground(density: Float): Drawable {
    if (isRazr) {
        return GradientDrawable().apply {
            setColor(accent)
            cornerRadius = 2f * density
            setStroke((1f * density).toInt().coerceAtLeast(1), 0xFF73D9FF.toInt())
        }
    }
    if (isNokia) {
        return GradientDrawable().apply {
            setColor(accent)
            cornerRadius = 3f * density
        }
    }
    if (isSlanted) {
        return SlantKeyDrawable(
            fill = accent,
            border = border,
            shadow = shadow,
            insetX = 2f * density,
            insetY = 6f * density,
            slant = 6f * density,
            borderWidth = 1.5f * density,
            shadowOffset = 2f * density,
            leftFrac = 0f,
            rightFrac = 0f,
        )
    }
    val shape = GradientDrawable().apply {
        setColor(accent)
        cornerRadius = 8f * density
    }
    val gx = (2f * density).toInt()
    val gy = (6f * density).toInt()
    return InsetDrawable(shape, gx, gy, gx, gy)
}
