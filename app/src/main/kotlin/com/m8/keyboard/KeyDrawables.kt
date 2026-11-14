// app/src/main/kotlin/com/m8/keyboard/KeyDrawables.kt
package com.m8.keyboard

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
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import kotlin.random.Random

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

/** Shared bright pixel colours used by the Game Boy Advance theme. */
internal val GBA_PIXEL_COLOURS = intArrayOf(
    0xFFFF6B8A.toInt(), // pink-red
    0xFFFFD45C.toInt(), // yellow
    0xFF65D6C4.toInt(), // mint
    0xFF78A7FF.toInt(), // blue
    0xFFC58BFF.toInt(), // violet
    0xFFFFA45C.toInt(), // orange
)

/** Small block-colour pixel pattern used behind the Game Boy Advance candidate strip. */
class PixelScreenDrawable(
    private val base: Int,
    private val density: Float,
) : Drawable() {
    private val paint = Paint().apply { isAntiAlias = false }
    private val pixels = GBA_PIXEL_COLOURS

    override fun draw(canvas: Canvas) {
        val b = bounds
        paint.color = base
        canvas.drawRect(b, paint)
        val cell = 6f * density
        val cols = (b.width() / cell).toInt().coerceAtLeast(1)
        val rows = (b.height() / cell).toInt().coerceAtLeast(1)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                // A fixed pattern keeps the tiny pixel-art blocks stable between redraws.
                val n = (x * 7 + y * 11 + (x * y) % 5) % 13
                if (n < 4) {
                    paint.color = pixels[(x + y * 2) % pixels.size]
                    val inset = cell * 0.12f
                    canvas.drawRect(
                        b.left + x * cell + inset,
                        b.top + y * cell + inset,
                        b.left + (x + 1) * cell - inset,
                        b.top + (y + 1) * cell - inset,
                        paint,
                    )
                }
            }
        }
        // Dark translucent wash keeps candidate labels readable over the coloured pixels.
        paint.color = 0x33201938
        canvas.drawRect(b, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
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

/** Hard-edged keycap with stepped corners and a dark pixel-style lower/right edge. */
class PixelGlitterKeyDrawable(
    private val fill: Int,
    private val shadow: Int,
    private val border: Int,
    private val insetX: Float,
    private val insetY: Float,
    private val density: Float,
    private val pixelColors: IntArray? = null,
) : Drawable() {
    private val paint = Paint().apply { isAntiAlias = false }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX
        val bottom = b.bottom - insetY
        if (r - l < 8f * density || bottom - t < 8f * density) return
        val step = 3f * density
        fun shape(left: Float, top: Float, right: Float, low: Float) {
            path.reset()
            path.moveTo(left + step, top)
            path.lineTo(right - step, top)
            path.lineTo(right - step, top + step)
            path.lineTo(right, top + step)
            path.lineTo(right, low - step)
            path.lineTo(right - step, low - step)
            path.lineTo(right - step, low)
            path.lineTo(left + step, low)
            path.lineTo(left + step, low - step)
            path.lineTo(left, low - step)
            path.lineTo(left, top + step)
            path.lineTo(left + step, top + step)
            path.close()
        }
        paint.style = Paint.Style.FILL
        paint.color = shadow
        shape(l + 2f * density, t + 3f * density, r, bottom)
        canvas.drawPath(path, paint)

        paint.color = border
        shape(l, t, r - 1f * density, bottom - 2f * density)
        canvas.drawPath(path, paint)

        paint.color = fill
        shape(l + 2f * density, t + 2f * density, r - 3f * density, bottom - 4f * density)
        canvas.drawPath(path, paint)

        // Small square highlights give the cap a deliberately pixel-art finish.
        val px = maxOf(1.5f * density, 2f)
        paint.color = 0x99FFFFFF.toInt()
        canvas.drawRect(l + 4f * density, t + 3f * density, l + 4f * density + px * 2, t + 3f * density + px, paint)
        canvas.drawRect(l + 4f * density, t + 3f * density + px, l + 4f * density + px, t + 3f * density + px * 2, paint)

        // GBA keys carry the same bright pixel colours as the candidate strip. Keep the blocks
        // around the edges so they decorate the cap without sitting behind its label.
        pixelColors?.let { colours ->
            val block = maxOf(2f * density, 2f)
            val topX = r - 10f * density
            val topY = t + 4f * density
            val bottomX = l + 4f * density
            val bottomY = bottom - 8f * density
            val pixels = arrayOf(
                floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(1f, 1f),
                floatArrayOf(2f, 1f), floatArrayOf(0f, 2f), floatArrayOf(1f, 2f),
            )
            pixels.forEachIndexed { index, offset ->
                paint.color = colours[index % colours.size]
                val x = if (index < 3) topX else bottomX
                val y = if (index < 3) topY else bottomY
                canvas.drawRect(
                    x + offset[0] * block,
                    y + offset[1] * block,
                    x + (offset[0] + 1f) * block,
                    y + (offset[1] + 1f) * block,
                    paint,
                )
            }
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/** Static, low-contrast pixel glitter used on the candidate strip and keyboard shell. */
class GlitterPatternDrawable(
    private val base: Int,
    private val density: Float,
    private val stripMode: Boolean = false,
) : Drawable() {
    private val paint = Paint().apply { isAntiAlias = false }
    private val pinks = intArrayOf(0x55FFFFFF, 0x66FFD8EF, 0x66FFB5DC, 0x66FFE46A)

    override fun draw(canvas: Canvas) {
        val b = bounds
        paint.color = base
        canvas.drawRect(b, paint)
        val cell = (if (stripMode) 5f else 9f) * density
        val cols = (b.width() / cell).toInt() + 1
        val rows = (b.height() / cell).toInt() + 1
        for (y in 0 until rows) for (x in 0 until cols) {
            val n = (x * 13 + y * 7 + x * y * 3) % 29
            if (n == 2 || n == 11 || (!stripMode && n == 19)) {
                paint.color = pinks[(x + y) % pinks.size]
                val px = maxOf(1f, density * if (stripMode) 1.5f else 2f)
                val left = b.left + x * cell
                val top = b.top + y * cell
                canvas.drawRect(left, top, left + px, top + px, paint)
                if (n == 2) {
                    canvas.drawRect(left + px * 2, top, left + px * 3, top + px, paint)
                    canvas.drawRect(left + px, top + px, left + px * 2, top + px * 2, paint)
                }
            }
        }
        if (!stripMode) {
            // Tiny 5x5 pixel hearts: decorative, but kept sparse so the keys remain the focus.
            val heart = arrayOf("01010", "11111", "11111", "01110", "00100")
            val px = maxOf(1.5f * density, 2f)
            val anchors = listOf(0.04f to 0.42f, 0.91f to 0.37f, 0.12f to 0.91f, 0.83f to 0.94f)
            anchors.forEachIndexed { index, anchor ->
                paint.color = pinks[(index + 1) % pinks.size]
                val left = b.left + b.width() * anchor.first
                val top = b.top + b.height() * anchor.second
                heart.forEachIndexed { row, bits ->
                    bits.forEachIndexed { col, bit ->
                        if (bit == '1') canvas.drawRect(left + col * px, top + row * px, left + (col + 1) * px, top + (row + 1) * px, paint)
                    }
                }
            }
        }
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}

/** The keyboard's optional typing pulse interface. */
interface KeyboardPulseEffect {
    fun pulse()
}

/**
 * Glitter theme background. The old fixed-position pulse has been retired; the random twinkles
 * now live in a foreground drawable so they appear over the keycaps instead of behind them.
 */
class GlitterPulseBackground(
    private val base: Drawable,
    @Suppress("unused") private val strength: Float,
    @Suppress("unused") private val density: Float,
) : Drawable(), KeyboardPulseEffect {
    override fun onBoundsChange(bounds: Rect) { base.bounds = bounds }
    override fun draw(canvas: Canvas) = base.draw(canvas)
    override fun pulse() = Unit
    override fun setAlpha(alpha: Int) { base.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { base.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = base.opacity
}

/**
 * Random, low-frame-rate star flashes drawn as a view foreground, above the keys and their labels.
 * Each flash chooses a fresh clickable key corner, size, colour and lifetime. A new set is not
 * replayed: stars are continuously spawned and expire independently.
 */
class GlitterTwinkleDrawable(
    private val host: ViewGroup,
    private val density: Float,
    private val strength: Float = 1f,
    private val colours: IntArray = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFFFE76A.toInt(), 0xFFFFC7E8.toInt(), 0xFFFF79C6.toInt(),
    ),
) : Drawable() {
    private data class Twinkle(
        val key: View,
        val corner: Int,
        val bornFrame: Int,
        val lifetime: Int,
        val radius: Float,
        val colour: Int,
    )

    private val random = Random(System.nanoTime())
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private val path = Path()
    private val twinkles = ArrayList<Twinkle>()
    private var frame = 0
    private var running = false
    private val frameTask = Runnable { invalidateSelf() }
    private val attachListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { start() }
        override fun onViewDetachedFromWindow(v: View) { stop() }
    }

    init {
        host.addOnAttachStateChangeListener(attachListener)
        if (host.isAttachedToWindow) start()
    }

    fun start() {
        if (running || strength <= 0f) return
        running = true
        scheduleSelf(frameTask, android.os.SystemClock.uptimeMillis() + FRAME_MS)
    }

    fun stop() {
        running = false
        unscheduleSelf(frameTask)
    }

    override fun draw(canvas: Canvas) {
        if (!running || strength <= 0f || colours.isEmpty()) return
        frame++
        val keys = clickableLeaves(host)
        twinkles.removeAll { frame - it.bornFrame >= it.lifetime || !it.key.isShown }

        // Spawn in small irregular bursts. The lifetime and position are independent per star.
        if (keys.isNotEmpty() && random.nextFloat() < 0.82f) {
            val count = if (random.nextFloat() < 0.24f) 2 else 1
            repeat(count) {
                val key = keys[random.nextInt(keys.size)]
                twinkles.add(
                    Twinkle(
                        key = key,
                        corner = random.nextInt(4),
                        bornFrame = frame,
                        lifetime = random.nextInt(6, 10),
                        radius = random.nextFloat() * 7f * density + 12f * density,
                        colour = colours[random.nextInt(colours.size)],
                    ),
                )
            }
        }

        val hostLocation = IntArray(2)
        host.getLocationOnScreen(hostLocation)
        for (twinkle in twinkles) {
            val key = twinkle.key
            if (key.width <= 0 || key.height <= 0 || !key.isShown) continue
            val location = IntArray(2)
            key.getLocationOnScreen(location)
            val inset = 3f * density
            val x = when (twinkle.corner) {
                0, 2 -> location[0] - hostLocation[0] + inset
                else -> location[0] - hostLocation[0] + key.width - inset
            }
            val y = when (twinkle.corner) {
                0, 1 -> location[1] - hostLocation[1] + inset
                else -> location[1] - hostLocation[1] + key.height - inset
            }
            val age = frame - twinkle.bornFrame
            val fadeAlpha = when (age) {
                0 -> 145
                1 -> 255 // fast appearance
                else -> (255f * (1f - (age - 1f) / (twinkle.lifetime - 1f))).toInt().coerceIn(0, 255)
            }
            val alpha = (fadeAlpha * (0.72f + strength.coerceIn(0f, 1f) * 0.28f)).toInt()
            if (alpha <= 0) continue
            paint.color = twinkle.colour
            paint.alpha = alpha
            drawStar(canvas, x, y, twinkle.radius, paint)
        }
        paint.alpha = 255
        scheduleSelf(frameTask, android.os.SystemClock.uptimeMillis() + FRAME_MS)
    }

    private fun clickableLeaves(parent: ViewGroup): List<View> {
        val found = ArrayList<View>()
        fun visit(view: View) {
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) visit(view.getChildAt(i))
            } else if (view.hasOnClickListeners() && view.width > 0 && view.height > 0 && view.isShown) {
                found.add(view)
            }
        }
        visit(parent)
        return found
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        path.reset()
        // Four long points with four short shoulders make a sharp, classic twinkle.
        for (i in 0 until 8) {
            val angle = -Math.PI / 2.0 + i * Math.PI / 4.0
            val r = if (i % 2 == 0) radius else radius * 0.19f
            val x = cx + kotlin.math.cos(angle).toFloat() * r
            val y = cy + kotlin.math.sin(angle).toFloat() * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
        // A tiny bright centre gives the flash a glassy, glittering point.
        paint.alpha = (paint.alpha * 0.9f).toInt()
        canvas.drawRect(cx - 1.5f * density, cy - 1.5f * density, cx + 1.5f * density, cy + 1.5f * density, paint)
    }

    override fun onBoundsChange(bounds: Rect) = Unit
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object { const val FRAME_MS = 125L }
}

/** The keyboard's background for this theme (plain colour, or the pinstriped backdrop). */
fun KeyboardTheme.backgroundDrawable(view: View): Drawable =
    if (isGlitter) {
        GlitterPatternDrawable(background, view.resources.displayMetrics.density)
    } else if (isSlanted) {
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
) : Drawable(), KeyboardPulseEffect {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var level = 0f
    private var startedAt = 0L
    private var running = false

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
        if (running) {
            // Drawn by hand from the clock, not with a ValueAnimator: Android's animation-speed
            // settings (including "off") must not change how this looks.
            val t = (SystemClock.uptimeMillis() - startedAt) / FADE_MS.toFloat()
            if (t >= 1f) {
                running = false
                level = 0f
            } else {
                level = (1f - t.coerceAtLeast(0f)).let { it * it } // fades out, slowing down
            }
        }
        if (level > 0f && strength > 0f) {
            paint.alpha = (255f * strength * level).toInt().coerceIn(0, 255)
            canvas.drawRect(bounds, paint)
        }
        if (running) invalidateSelf() // ask for the next frame
    }

    /** Flash now and fade out. */
    override fun pulse() {
        if (strength <= 0f) return
        startedAt = SystemClock.uptimeMillis()
        running = true
        level = 1f
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    private companion object {
        const val FADE_MS = 450L
    }
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
    keyColor: Int? = null,
    pressedKeyColor: Int? = null,
): Drawable {
    val normal = keyColor ?: if (action) actionKey else key
    val pressed = pressedKeyColor ?: if (action) actionKeyPressed else keyPressed

    fun layer(color: Int): Drawable {
        if (isGlitter || isGameBoy) {
            return PixelGlitterKeyDrawable(
                fill = color,
                shadow = this.shadow,
                border = when {
                    isGlitter -> 0xFFFFE2F2.toInt()
                    isGameBoyAdvance -> 0xFFE8D9FF.toInt()
                    else -> this.shadow
                },
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
                pixelColors = if (isGameBoyAdvance) GBA_PIXEL_COLOURS else null,
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
    if (isGlitter) {
        return GradientDrawable().apply {
            setColor(accent)
            setStroke(maxOf(1, (2f * density).toInt()), 0xFFFFFFFF.toInt())
            cornerRadius = 0f
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
