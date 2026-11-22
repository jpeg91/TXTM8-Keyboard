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
import android.graphics.Typeface
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

/**
 * A grey 90s-desktop button: a flat face with a one-pixel white and black bevel (raised), swapped
 * round when [pressed] so the key looks pushed in.
 */
class RetroKeyDrawable(
    private val fill: Int,
    private val pressed: Boolean,
    private val insetX: Float,
    private val insetY: Float,
    private val density: Float,
) : Drawable() {
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val l = b.left + insetX
        val t = b.top + insetY
        val r = b.right - insetX
        val bottom = b.bottom - insetY
        val px = maxOf(1f, density)
        if (r - l < 6f * px || bottom - t < 6f * px) return

        paint.color = fill
        canvas.drawRect(l, t, r, bottom, paint)

        val outerLight = if (pressed) 0xFF808080.toInt() else 0xFFFFFFFF.toInt()
        val outerDark = if (pressed) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val innerLight = if (pressed) 0xFF000000.toInt() else 0xFFDFDFDF.toInt()
        val innerDark = if (pressed) 0xFFDFDFDF.toInt() else 0xFF808080.toInt()

        // Inner bevel first, then the outer one on top of it.
        paint.color = innerLight
        canvas.drawRect(l + px, t + px, r - px, t + 2 * px, paint)
        canvas.drawRect(l + px, t + px, l + 2 * px, bottom - px, paint)
        paint.color = innerDark
        canvas.drawRect(l + px, bottom - 2 * px, r - px, bottom - px, paint)
        canvas.drawRect(r - 2 * px, t + px, r - px, bottom - px, paint)

        paint.color = outerLight
        canvas.drawRect(l, t, r, t + px, paint)
        canvas.drawRect(l, t, l + px, bottom, paint)
        paint.color = outerDark
        canvas.drawRect(l, bottom - px, r, bottom, paint)
        canvas.drawRect(r - px, t, r, bottom, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
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

    /** The same, from a particular key: [key] is that key's rectangle in the keyboard's coordinates (null if unknown). */
    fun pulse(key: RectF?) { pulse() }
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
    /** 0..100: how many stars appear. 50 is the original amount; 0 is none and 100 is about double. */
    private val starDensity: Int = 50,
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
        // At the default density of 50 this averages about one new star per frame; the slider
        // scales that from none (0) to about two per frame (100).
        val expected = 1.02f * starDensity.coerceIn(0, 100) / 50f
        val whole = expected.toInt()
        val count = whole + if (random.nextFloat() < expected - whole) 1 else 0
        if (keys.isNotEmpty() && count > 0) {
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

/** The animations that start where the keyboard is tapped. */
enum class TapEffectKind {
    BUBBLES, EMBERS, PETALS, RIPPLES,

    /** A short row of blue blocks that marches away from the tap, like a loading bar. */
    BLOCKS,

    /** A soft glowing orb that blooms at the tap and fades, with a thin ring spreading out from it. */
    ORB,

    /** Little four-point sparkles that fall away, like an old web page's cursor trail. */
    SPARKLE,

    /** A cluster of pixels that fades in coarse steps, like the ghosting of a slow LCD. */
    GHOST,

    /** Music notes that float up and sway. */
    NOTES,

    /** A column of falling glyphs that starts at the tap. */
    GLYPHS,

    /** Squiggles, triangles, rings and plus signs that burst out and tumble down. */
    CONFETTI,

    /** Little hearts that float up and sway. */
    HEARTS,

    /** Hexagonal shields that flash out from the tap, grow and fade. */
    HEXAGONS,

    /** A targeting reticle: four corner brackets close in on the tap, then a crosshair and a dot. */
    TARGET,

    /** The white pointing-glove cursor of a Final Fantasy VII menu, which slides in to touch the tap and fades. */
    POINTER,

    /** Turning green plumbobs (a Sims diamond) that float up and bob. */
    PLUMBOB,

    /** A burst of spray-paint dots with a drip or two running down. */
    SPRAY,

    /** A ragged Worms-style explosion that throws out lumps of earth. */
    BLAST,

    /** Gold rings that scatter in an arc, spin and blink out, like the ones Sonic drops when he is hit. */
    RINGS,

    /** A Mario coin that pops up from the tap, spinning, and falls back. */
    COINS,

    /** A comic-book "POW" burst that pops out at the tap, with speed lines, and fades. */
    STARBURST,

    /** A few particle tracks, as in a bubble-chamber photograph, that leave the tap, curl tighter and fade. */
    TRACK,

    /** Lumps of modelling clay flung from the tap that move in jerky stop-motion steps. */
    CLAY,

    /** A burst of coloured square sprites that fly out in eight directions in steps, and flicker out, like a handheld game. */
    PIXELS,

    /** A single pale feather that drifts down from the tap, turning as it falls, like the ones that fall in the background. */
    FEATHER,

    /** A small desktop window that opens at the tap, fills with lines of text and tears away, like the Wired. */
    WINDOW,
}

/**
 * An animation drawn over the keyboard that starts wherever a finger touches down (not just on a key):
 * rising bubbles, floating embers, fluttering petals or spreading ripples. It is drawn by hand from the
 * clock, so Android's animation-speed settings do not change it. [amount] is 0..100: 50 is the normal
 * amount per tap, 0 is none and 100 is about double.
 */
class TapEffectDrawable(
    private val kind: TapEffectKind,
    private val density: Float,
    private val colours: IntArray,
    private val amount: Int,
) : Drawable() {
    private class Particle(
        val x: Float,
        val y: Float,
        val vx: Float,
        val vy: Float,
        val size: Float,
        val bornAt: Long,
        val life: Long,
        val colour: Int,
        val phase: Float,
        val spin: Float,
    )

    private val random = Random(System.nanoTime())
    private val particles = ArrayList<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val oval = RectF()
    private val path = Path()
    private val glyph = CharArray(1)
    private val glyphChars: CharArray by lazy {
        val list = ArrayList<Char>()
        for (code in 0xFF66..0xFF9D) list.add(code.toChar())
        for (ch in "0123456789:.=*+-<>") list.add(ch)
        list.toCharArray()
    }

    private fun rnd(from: Float, to: Float): Float = from + random.nextFloat() * (to - from)

    /** Start the effect at [x], [y] (in this view's own coordinates). */
    fun spawn(x: Float, y: Float) {
        val scale = amount.coerceIn(0, 100) / 50f
        if (scale <= 0f || particles.size >= MAX_PARTICLES) return
        val now = SystemClock.uptimeMillis()
        val expected = scale * when (kind) {
            TapEffectKind.BUBBLES -> 5f
            TapEffectKind.EMBERS -> 8f
            TapEffectKind.PETALS -> 6f
            TapEffectKind.RIPPLES -> 2f
            TapEffectKind.BLOCKS -> 1f
            TapEffectKind.ORB -> 1f
            TapEffectKind.SPARKLE -> 5f
            TapEffectKind.GHOST -> 1f
            TapEffectKind.NOTES -> 2f
            TapEffectKind.GLYPHS -> 1f
            TapEffectKind.CONFETTI -> 9f
            TapEffectKind.HEARTS -> 4f
            TapEffectKind.HEXAGONS -> 3f
            TapEffectKind.TARGET -> 1f
            TapEffectKind.POINTER -> 1f
            TapEffectKind.PLUMBOB -> 2f
            TapEffectKind.SPRAY -> 42f
            TapEffectKind.BLAST -> 8f
            TapEffectKind.RINGS -> 5f
            TapEffectKind.COINS -> 1f
            TapEffectKind.STARBURST -> 1f
            TapEffectKind.TRACK -> 3f
            TapEffectKind.CLAY -> 6f
            TapEffectKind.FEATHER -> 1f
            TapEffectKind.PIXELS -> 8f
            TapEffectKind.WINDOW -> 1f
        }
        var count = expected.toInt() + if (random.nextFloat() < expected - expected.toInt()) 1 else 0
        if (count < 1) count = 1
        val d = density
        // A burst of spray paint is all one colour, chosen afresh for each tap.
        val sprayColour = colours[random.nextInt(colours.size)]
        repeat(count) { i ->
            val colour = colours[random.nextInt(colours.size)]
            particles.add(
                when (kind) {
                    TapEffectKind.BUBBLES -> Particle(
                        x = x + rnd(-12f, 12f) * d, y = y + rnd(-6f, 6f) * d,
                        vx = rnd(-10f, 10f) * d, vy = -rnd(45f, 95f) * d,
                        size = rnd(3f, 9f) * d * (if (random.nextFloat() < 0.15f) 1.5f else 1f),
                        bornAt = now + rnd(0f, 260f).toLong(), life = rnd(1300f, 2300f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.EMBERS -> Particle(
                        x = x + rnd(-8f, 8f) * d, y = y + rnd(-4f, 4f) * d,
                        vx = rnd(-45f, 45f) * d, vy = -rnd(80f, 190f) * d,
                        size = rnd(1.5f, 3.2f) * d,
                        bornAt = now + rnd(0f, 120f).toLong(), life = rnd(700f, 1400f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.PETALS -> Particle(
                        x = x + rnd(-8f, 8f) * d, y = y,
                        vx = rnd(-70f, 70f) * d, vy = -rnd(50f, 130f) * d,
                        size = rnd(0.8f, 1.25f),
                        bornAt = now + rnd(0f, 150f).toLong(), life = rnd(1700f, 2700f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = rnd(-240f, 240f),
                    )
                    TapEffectKind.RIPPLES -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = (34f + i * 14f + rnd(0f, 8f)) * d,
                        bornAt = now + i * 150L, life = rnd(750f, 950f).toLong(),
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.BLOCKS -> Particle(
                        x = x - 18f * d, y = y + rnd(-12f, 12f) * d,
                        vx = rnd(110f, 150f) * d, vy = 0f,
                        size = 5.5f * d,
                        bornAt = now + i * 90L, life = 850L,
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.ORB -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = rnd(34f, 46f) * d,
                        bornAt = now, life = 750L,
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.SPARKLE -> Particle(
                        x = x + rnd(-10f, 10f) * d, y = y + rnd(-6f, 6f) * d,
                        vx = rnd(-40f, 40f) * d, vy = -rnd(10f, 60f) * d,
                        size = rnd(3.5f, 7f) * d,
                        bornAt = now + rnd(0f, 160f).toLong(), life = rnd(500f, 900f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.GHOST -> Particle(
                        x = x + rnd(-8f, 8f) * d, y = y + rnd(-8f, 8f) * d,
                        vx = 0f, vy = 0f,
                        size = 3.2f * d,
                        bornAt = now, life = 1100L,
                        colour = colour, phase = rnd(0f, 1f), spin = 0f,
                    )
                    TapEffectKind.NOTES -> Particle(
                        x = x + rnd(-10f, 10f) * d, y = y,
                        vx = rnd(-20f, 20f) * d, vy = -rnd(50f, 90f) * d,
                        size = rnd(0.9f, 1.4f),
                        bornAt = now + rnd(0f, 200f).toLong(), life = rnd(1100f, 1700f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.GLYPHS -> Particle(
                        x = x + rnd(-12f, 12f) * d, y = y,
                        vx = 0f, vy = rnd(220f, 320f) * d,
                        size = 13f * d,
                        bornAt = now + rnd(0f, 120f).toLong(), life = rnd(900f, 1300f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.CONFETTI -> Particle(
                        x = x, y = y,
                        vx = rnd(-170f, 170f) * d, vy = -rnd(120f, 260f) * d,
                        size = rnd(5f, 9f) * d,
                        bornAt = now, life = rnd(1100f, 1800f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = rnd(-300f, 300f),
                    )
                    TapEffectKind.HEARTS -> Particle(
                        x = x + rnd(-10f, 10f) * d, y = y,
                        vx = rnd(-25f, 25f) * d, vy = -rnd(45f, 95f) * d,
                        size = rnd(0.8f, 1.3f),
                        bornAt = now + rnd(0f, 200f).toLong(), life = rnd(1200f, 1900f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.HEXAGONS -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = (64f + i * 34f + rnd(0f, 10f)) * d,
                        bornAt = now + i * 110L, life = rnd(850f, 1050f).toLong(),
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.TARGET -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = 44f * d,
                        bornAt = now, life = 650L,
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.POINTER -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = d,
                        bornAt = now, life = 800L,
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.PLUMBOB -> Particle(
                        x = x + rnd(-10f, 10f) * d, y = y,
                        vx = rnd(-14f, 14f) * d, vy = -rnd(40f, 75f) * d,
                        size = rnd(7f, 10f) * d,
                        bornAt = now + rnd(0f, 200f).toLong(), life = rnd(1100f, 1600f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.SPRAY -> {
                        if (i == 0) {
                            // The wet centre of the burst ([phase] 1 marks it).
                            Particle(
                                x = x, y = y, vx = 0f, vy = 0f,
                                size = rnd(8f, 11f) * d,
                                bornAt = now, life = 750L,
                                colour = sprayColour, phase = 1f, spin = 0f,
                            )
                        } else if (i % 9 == 8) {
                            // A drip: [vy] is how fast it runs down; [spin] marks it as a drip.
                            Particle(
                                x = x + rnd(-9f, 9f) * d, y = y + rnd(-2f, 4f) * d,
                                vx = 0f, vy = rnd(18f, 36f) * d,
                                size = d,
                                bornAt = now + rnd(0f, 80f).toLong(), life = rnd(750f, 1100f).toLong(),
                                colour = sprayColour, phase = 0f, spin = 1f,
                            )
                        } else {
                            // A fleck: [vx] and [vy] are where it ends up, relative to the tap. Most land close to the
                            // middle and a few fly far, like a real mist.
                            val angle = rnd(0f, 6.28f)
                            val spread = rnd(0f, 1f)
                            val dist = (2f + 36f * spread * spread) * d
                            Particle(
                                x = x, y = y,
                                vx = kotlin.math.cos(angle) * dist, vy = kotlin.math.sin(angle) * dist,
                                size = rnd(0.7f, 2.1f) * d,
                                bornAt = now + rnd(0f, 70f).toLong(), life = rnd(550f, 950f).toLong(),
                                colour = sprayColour, phase = 0f, spin = 0f,
                            )
                        }
                    }
                    TapEffectKind.BLAST -> {
                        if (i == 0) {
                            // The explosion itself ([phase] 1 marks it); the rest are lumps of earth.
                            Particle(
                                x = x, y = y, vx = 0f, vy = 0f,
                                size = rnd(30f, 38f) * d,
                                bornAt = now, life = 520L,
                                colour = colour, phase = 1f, spin = 0f,
                            )
                        } else {
                            Particle(
                                x = x, y = y,
                                vx = rnd(-130f, 130f) * d, vy = -rnd(90f, 210f) * d,
                                size = rnd(1.6f, 3.2f) * d,
                                bornAt = now, life = rnd(650f, 1000f).toLong(),
                                colour = if (colours.size > 2) colours[2 + random.nextInt(colours.size - 2)] else colour,
                                phase = 0f, spin = rnd(-400f, 400f),
                            )
                        }
                    }
                    TapEffectKind.RINGS -> Particle(
                        x = x + rnd(-6f, 6f) * d, y = y,
                        vx = rnd(-95f, 95f) * d, vy = -rnd(150f, 260f) * d,
                        size = d,
                        bornAt = now + rnd(0f, 80f).toLong(), life = rnd(1000f, 1300f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.COINS -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = d,
                        bornAt = now + i * 140L, life = 640L,
                        colour = colour, phase = 0f, spin = 0f,
                    )
                    TapEffectKind.STARBURST -> Particle(
                        x = x, y = y - 6f * d, vx = 0f, vy = 0f,
                        size = rnd(26f, 32f) * d,
                        bornAt = now, life = 800L,
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.TRACK -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = d,
                        bornAt = now + rnd(0f, 60f).toLong(), life = rnd(1100f, 1500f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f),
                        spin = rnd(0.02f, 0.05f) * (if (random.nextBoolean()) 1f else -1f),
                    )
                    TapEffectKind.PIXELS -> {
                        val angle = i * 0.785398f + 0.39f
                        Particle(
                            x = x, y = y,
                            vx = kotlin.math.cos(angle), vy = kotlin.math.sin(angle),
                            size = 4f * d,
                            bornAt = now, life = 650L,
                            colour = colours[i % colours.size], phase = 0f, spin = 0f,
                        )
                    }
                    TapEffectKind.FEATHER -> Particle(
                        x = x, y = y - 4f * d,
                        vx = rnd(-14f, 14f) * d, vy = -rnd(10f, 26f) * d,
                        size = rnd(20f, 26f) * d,
                        bornAt = now, life = 2300L,
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                    TapEffectKind.WINDOW -> Particle(
                        x = x, y = y, vx = 0f, vy = 0f,
                        size = d,
                        bornAt = now, life = 1000L,
                        colour = colour, phase = rnd(0f, 1000f), spin = 0f,
                    )
                    TapEffectKind.CLAY -> Particle(
                        x = x + rnd(-6f, 6f) * d, y = y,
                        vx = rnd(-120f, 120f) * d, vy = -rnd(90f, 200f) * d,
                        size = rnd(3f, 6f) * d,
                        bornAt = now, life = rnd(800f, 1200f).toLong(),
                        colour = colour, phase = rnd(0f, 6.28f), spin = 0f,
                    )
                },
            )
        }
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (particles.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            val age = now - p.bornAt
            if (age >= p.life) {
                iterator.remove()
                continue
            }
            if (age < 0) continue // not born yet
            val sec = age / 1000f
            val t = age / p.life.toFloat()
            // Fades in quickly and out over the last third of its life.
            val fade = minOf(t / 0.1f, 1f).coerceIn(0f, 1f) * minOf((1f - t) / 0.35f, 1f).coerceIn(0f, 1f)
            when (kind) {
                TapEffectKind.BUBBLES -> drawBubble(canvas, p, sec, t, fade)
                TapEffectKind.EMBERS -> drawEmber(canvas, p, sec, t)
                TapEffectKind.PETALS -> drawPetal(canvas, p, sec, fade)
                TapEffectKind.RIPPLES -> drawRipple(canvas, p, t)
                TapEffectKind.BLOCKS -> drawBlocks(canvas, p, sec, fade)
                TapEffectKind.ORB -> drawOrb(canvas, p, t)
                TapEffectKind.SPARKLE -> drawSparkle(canvas, p, sec, t)
                TapEffectKind.GHOST -> drawGhost(canvas, p, t)
                TapEffectKind.NOTES -> drawNote(canvas, p, sec, fade)
                TapEffectKind.GLYPHS -> drawGlyphs(canvas, p, sec, t)
                TapEffectKind.CONFETTI -> drawConfetti(canvas, p, sec, fade)
                TapEffectKind.HEARTS -> drawHeart(canvas, p, sec, fade)
                TapEffectKind.HEXAGONS -> drawHexagon(canvas, p, t)
                TapEffectKind.TARGET -> drawTarget(canvas, p, t)
                TapEffectKind.POINTER -> GameTap.pointer(canvas, paint, oval, p.x, p.y, t, density, colours)
                TapEffectKind.PLUMBOB ->
                    GameTap.plumbob(canvas, paint, path, p.x + p.vx * sec, p.y + p.vy * sec, p.size, sec, p.phase, fade, p.colour)
                TapEffectKind.SPRAY ->
                    if (p.phase > 0.5f) {
                        GameTap.spraySplat(canvas, paint, p.x, p.y, p.size, t, p.colour, density)
                    } else if (p.spin > 0.5f) {
                        GameTap.sprayDrip(canvas, paint, p.x, p.y, p.vy, sec, t, p.colour, density)
                    } else {
                        GameTap.sprayDot(canvas, paint, p.x, p.y, p.vx, p.vy, p.size, t, p.colour)
                    }
                TapEffectKind.BLAST ->
                    if (p.phase > 0.5f) {
                        GameTap.blast(canvas, paint, path, p.x, p.y, p.size, t, colours)
                    } else {
                        val gravity = 520f * density
                        GameTap.debris(
                            canvas, paint, p.x + p.vx * sec, p.y + p.vy * sec + 0.5f * gravity * sec * sec,
                            p.size, p.spin, sec, t, p.colour,
                        )
                    }
                TapEffectKind.RINGS -> {
                    val gravity = 620f * density
                    GameTap.ring(
                        canvas, paint, oval, p.x + p.vx * sec, p.y + p.vy * sec + 0.5f * gravity * sec * sec,
                        sec, t, p.phase, density, colours,
                    )
                }
                TapEffectKind.COINS -> GameTap.coin(canvas, paint, oval, p.x, p.y, sec, t, density, colours)
                TapEffectKind.STARBURST -> MusicTap.starburst(canvas, paint, path, p.x, p.y, p.size, p.phase, t, density, p.colour)
                TapEffectKind.TRACK -> MusicTap.track(canvas, paint, path, p.x, p.y, p.phase, p.spin, t, density, p.colour)
                TapEffectKind.PIXELS -> GameTap.pixel(canvas, paint, p.x, p.y, p.vx, p.vy, p.size, t, p.colour, density)
                TapEffectKind.FEATHER -> {
                    val gravity = 38f * density
                    val sway = kotlin.math.sin(sec * 2.3f + p.phase)
                    MusicTap.feather(
                        canvas, paint, oval,
                        p.x + p.vx * sec + sway * 15f * density, p.y + p.vy * sec + 0.5f * gravity * sec * sec,
                        p.size, sway * 42f + p.phase * 6f, withAlpha(p.colour, (fade * 230f).toInt().coerceIn(0, 255)), density,
                    )
                }
                TapEffectKind.WINDOW -> GameTap.window(canvas, paint, p.x, p.y, t, p.phase.toInt(), density, colours)
                TapEffectKind.CLAY -> {
                    // Moves only eight times a second, like a stop-motion figure.
                    val step = (sec * 8f).toInt()
                    val frozen = step / 8f
                    val gravity = 500f * density
                    MusicTap.lump(
                        canvas, paint, oval, p.x + p.vx * frozen, p.y + p.vy * frozen + 0.5f * gravity * frozen * frozen,
                        p.size, step, (fade * 255f).toInt().coerceIn(0, 255), p.colour,
                    )
                }
            }
        }
        if (particles.isNotEmpty()) invalidateSelf() // ask for the next frame
    }

    private fun drawBubble(canvas: Canvas, p: Particle, sec: Float, t: Float, fade: Float) {
        val r = p.size * (0.85f + 0.35f * t) // bubbles swell a little as they rise
        val cx = p.x + p.vx * sec + kotlin.math.sin(sec * 3.2f + p.phase) * 5f * density
        val cy = p.y + p.vy * sec
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = (fade * 45f).toInt()
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, 1.2f * density)
        paint.color = p.colour
        paint.alpha = (fade * 210f).toInt()
        canvas.drawCircle(cx, cy, r, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (fade * 200f).toInt()
        canvas.drawCircle(cx - r * 0.35f, cy - r * 0.35f, maxOf(1f, r * 0.18f), paint)
    }

    private fun drawEmber(canvas: Canvas, p: Particle, sec: Float, t: Float) {
        val gravity = 90f * density // slows the climb
        val cx = p.x + p.vx * sec + kotlin.math.sin(sec * 9f + p.phase) * 3f * density
        val cy = p.y + p.vy * sec + 0.5f * gravity * sec * sec
        val index = (t * colours.size).toInt().coerceIn(0, colours.size - 1)
        val flicker = 0.65f + 0.35f * kotlin.math.sin(sec * 38f + p.phase)
        paint.style = Paint.Style.FILL
        paint.color = colours[index]
        paint.alpha = ((1f - t * t) * flicker * 255f).toInt().coerceIn(0, 255)
        val s = p.size * (1f - 0.5f * t)
        canvas.drawRect(cx - s, cy - s, cx + s, cy + s, paint)
    }

    private fun drawPetal(canvas: Canvas, p: Particle, sec: Float, fade: Float) {
        val gravity = 100f * density
        val cx = p.x + p.vx * sec + kotlin.math.sin(sec * 3.5f + p.phase) * 12f * density
        val cy = p.y + p.vy * sec + 0.5f * gravity * sec * sec
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = (fade * 235f).toInt()
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(p.phase * 57.3f + p.spin * sec)
        // Tumbling: the petal narrows and widens as it turns over.
        canvas.scale(kotlin.math.abs(kotlin.math.cos(sec * 4f + p.phase)).coerceAtLeast(0.25f), 1f)
        oval.set(-3.2f * density * p.size, -5f * density * p.size, 3.2f * density * p.size, 5f * density * p.size)
        canvas.drawOval(oval, paint)
        canvas.restore()
    }

    private fun drawRipple(canvas: Canvas, p: Particle, t: Float) {
        val ease = 1f - (1f - t) * (1f - t)
        val start = 4f * density
        val radius = start + (p.size - start) * ease
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, (2.5f * (1f - t) + 0.6f) * density)
        paint.color = p.colour
        paint.alpha = ((1f - t) * (1f - t) * 220f).toInt()
        canvas.drawCircle(p.x, p.y, radius, paint)
    }

    /** Three blocks, a loading bar on the move: the lead block is the strongest. */
    private fun drawBlocks(canvas: Canvas, p: Particle, sec: Float, fade: Float) {
        val cx = p.x + p.vx * sec
        val bw = p.size * 1.3f
        val bh = p.size * 2.3f
        val gap = p.size * 0.5f
        paint.style = Paint.Style.FILL
        for (i in 0 until 3) {
            // Blocks move in steps of one block, as the real bar does, not smoothly.
            val step = (cx / (bw + gap)).toInt() * (bw + gap)
            val x = step - i * (bw + gap)
            paint.color = p.colour
            paint.alpha = (fade * (255f - i * 70f)).toInt().coerceIn(0, 255)
            oval.set(x, p.y - bh / 2f, x + bw, p.y + bh / 2f)
            canvas.drawRoundRect(oval, p.size * 0.35f, p.size * 0.35f, paint)
            paint.color = 0xFFFFFFFF.toInt()
            paint.alpha = (fade * (110f - i * 30f)).toInt().coerceIn(0, 255)
            oval.set(x + bw * 0.15f, p.y - bh / 2f + bh * 0.1f, x + bw * 0.85f, p.y - bh / 2f + bh * 0.42f)
            canvas.drawRoundRect(oval, p.size * 0.25f, p.size * 0.25f, paint)
        }
    }

    /**
     * A glowing orb: it blooms fast and then fades, built from six translucent discs (a soft halo that gets brighter
     * towards the middle), a small white core, and a thin ring that spreads a little further out.
     */
    private fun drawOrb(canvas: Canvas, p: Particle, t: Float) {
        val grow = 1f - (1f - t) * (1f - t) // fast at first, then settles
        val r = p.size * (0.3f + 0.7f * grow)
        val fade = 1f - t
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        for (i in 0 until 6) {
            val f = 1f - i / 6f
            paint.alpha = (fade * (18f + i * 22f)).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, r * f, paint)
        }
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (fade * fade * 230f).toInt().coerceIn(0, 255)
        canvas.drawCircle(p.x, p.y, r * 0.16f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, 1.4f * density)
        paint.color = p.colour
        paint.alpha = (fade * 190f).toInt().coerceIn(0, 255)
        canvas.drawCircle(p.x, p.y, r * 1.12f, paint)
        paint.style = Paint.Style.FILL
    }

    /** A four-point sparkle that falls away and flickers between two sizes. */
    private fun drawSparkle(canvas: Canvas, p: Particle, sec: Float, t: Float) {
        val cx = p.x + p.vx * sec
        val cy = p.y + p.vy * sec + 0.5f * 140f * density * sec * sec
        val flick = if ((sec * 16f).toInt() % 2 == 0) 1f else 0.55f
        val r = p.size * flick
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = ((1f - t) * 255f).toInt().coerceIn(0, 255)
        path.reset()
        for (i in 0 until 8) {
            val angle = -Math.PI / 2.0 + i * Math.PI / 4.0
            val rr = if (i % 2 == 0) r else r * 0.22f
            val x = cx + kotlin.math.cos(angle).toFloat() * rr
            val y = cy + kotlin.math.sin(angle).toFloat() * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    /** A 4x4 cluster of pixels (the middle four always lit) whose brightness drops in eight coarse steps. */
    private fun drawGhost(canvas: Canvas, p: Particle, t: Float) {
        val level = 1f - (t * 8f).toInt() / 8f
        if (level <= 0f) return
        val bits = (p.phase * 65535f).toInt() or 0x0660
        val cell = p.size
        val left = (p.x / cell).toInt() * cell - 2f * cell
        val top = (p.y / cell).toInt() * cell - 2f * cell
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = (level * level * 215f).toInt().coerceIn(0, 255)
        for (i in 0 until 16) {
            if ((bits shr i) and 1 == 0) continue
            val x = left + (i % 4) * cell
            val y = top + (i / 4) * cell
            canvas.drawRect(x, y, x + cell - 0.5f * density, y + cell - 0.5f * density, paint)
        }
    }

    /** A music note that rises, sways and tilts. */
    private fun drawNote(canvas: Canvas, p: Particle, sec: Float, fade: Float) {
        val sway = kotlin.math.sin(sec * 4f + p.phase)
        val cx = p.x + p.vx * sec + sway * 10f * density
        val cy = p.y + p.vy * sec
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = (fade * 235f).toInt().coerceIn(0, 255)
        paint.textSize = 20f * density * p.size
        paint.textAlign = Paint.Align.CENTER
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(sway * 12f)
        canvas.drawText(if (p.phase < 3.14f) "\u266A" else "\u266B", 0f, 0f, paint)
        canvas.restore()
    }

    /** A column of glyphs falling from the tap: a white head and a green trail that fades. */
    private fun drawGlyphs(canvas: Canvas, p: Particle, sec: Float, t: Float) {
        val cell = p.size
        val column = (p.x / cell).toInt()
        val x = column * cell + cell / 2f
        val headY = p.y + p.vy * sec
        val trail = 9
        val out = 1f - t * t
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = cell * 0.95f
        for (i in 0 until trail) {
            val g = hash3(column, i, if (i == 0) (sec * 14f).toInt() else (sec * 7f).toInt() + (p.phase * 10f).toInt()) % glyphChars.size
            glyph[0] = glyphChars[g]
            val f = 1f - i / trail.toFloat()
            paint.color = when {
                i == 0 -> colours[0]
                colours.size > 1 -> colours[1 + i % (colours.size - 1)]
                else -> colours[0]
            }
            paint.alpha = ((if (i == 0) 255f else 220f * f * f) * out).toInt().coerceIn(0, 255)
            canvas.drawText(glyph, 0, 1, x, headY - i * cell, paint)
        }
        paint.typeface = Typeface.DEFAULT
    }

    /** A squiggle, triangle, ring or plus that bursts out, tumbles in 30-degree steps and falls. */
    private fun drawConfetti(canvas: Canvas, p: Particle, sec: Float, fade: Float) {
        val gravity = 420f * density
        val cx = p.x + p.vx * sec
        val cy = p.y + p.vy * sec + 0.5f * gravity * sec * sec
        val turn = ((p.phase * 57.3f + p.spin * sec) / 30f).toInt() * 30f
        val s = p.size
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1.5f, 2.2f * density)
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = p.colour
        paint.alpha = (fade * 255f).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(turn)
        path.reset()
        when ((p.phase * 100f).toInt() % 4) {
            0 -> {
                path.moveTo(-s * 1.4f, 0f)
                for (k in 0 until 4) path.lineTo(-s * 1.4f + (k + 0.5f) * s * 0.7f, if (k % 2 == 0) -s * 0.5f else s * 0.5f)
                path.lineTo(s * 1.4f, 0f)
            }
            1 -> {
                path.moveTo(0f, -s)
                path.lineTo(s * 0.95f, s * 0.7f)
                path.lineTo(-s * 0.95f, s * 0.7f)
                path.close()
            }
            2 -> path.addCircle(0f, 0f, s * 0.6f, Path.Direction.CW)
            else -> {
                path.moveTo(-s * 0.8f, 0f)
                path.lineTo(s * 0.8f, 0f)
                path.moveTo(0f, -s * 0.8f)
                path.lineTo(0f, s * 0.8f)
            }
        }
        canvas.drawPath(path, paint)
        canvas.restore()
        paint.style = Paint.Style.FILL
    }

    /** A small heart with a white shine that rises, sways and tilts. */
    private fun drawHeart(canvas: Canvas, p: Particle, sec: Float, fade: Float) {
        val sway = kotlin.math.sin(sec * 3.6f + p.phase)
        val cx = p.x + p.vx * sec + sway * 9f * density
        val cy = p.y + p.vy * sec
        val s = 6.5f * density * p.size // half the width of the heart
        paint.style = Paint.Style.FILL
        paint.color = p.colour
        paint.alpha = (fade * 235f).toInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(sway * 14f)
        path.reset()
        path.moveTo(0f, s * 0.95f)
        path.cubicTo(-s * 1.7f, -s * 0.1f, -s * 0.9f, -s * 1.25f, 0f, -s * 0.45f)
        path.cubicTo(s * 0.9f, -s * 1.25f, s * 1.7f, -s * 0.1f, 0f, s * 0.95f)
        path.close()
        canvas.drawPath(path, paint)
        paint.color = 0xFFFFFFFF.toInt()
        paint.alpha = (fade * 170f).toInt().coerceIn(0, 255)
        canvas.drawCircle(-s * 0.5f, -s * 0.3f, maxOf(1f, s * 0.16f), paint)
        canvas.restore()
    }

    /** A targeting reticle: four corner brackets close in on the tap, then a small crosshair and a dot appear in it. */
    private fun drawTarget(canvas: Canvas, p: Particle, t: Float) {
        val d = density
        val ease = 1f - (1f - t) * (1f - t)
        val half = p.size * (1f - 0.62f * ease)
        val arm = 7f * d
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = maxOf(1f, 1.4f * d)
        paint.strokeJoin = Paint.Join.MITER
        paint.color = p.colour
        paint.alpha = ((1f - t * t) * 235f).toInt().coerceIn(0, 255)
        path.reset()
        for (sx in -1..1 step 2) for (sy in -1..1 step 2) {
            val cx = p.x + sx * half
            val cy = p.y + sy * half
            path.moveTo(cx - sx * arm, cy)
            path.lineTo(cx, cy)
            path.lineTo(cx, cy - sy * arm)
        }
        canvas.drawPath(path, paint)
        if (t > 0.2f) {
            val a = 5f * d
            canvas.drawLine(p.x - a, p.y, p.x + a, p.y, paint)
            canvas.drawLine(p.x, p.y - a, p.x, p.y + a, paint)
            paint.style = Paint.Style.FILL
            canvas.drawCircle(p.x, p.y, 1.8f * d, paint)
        }
        paint.style = Paint.Style.FILL
    }

    /**
     * A hexagonal shield that flashes out from the tap: two nested outlines that thin and fade as they grow, and a
     * faint fill at first. Every hexagon is drawn the same way up, with a flat top and bottom.
     */
    private fun drawHexagon(canvas: Canvas, p: Particle, t: Float) {
        val ease = 1f - (1f - t) * (1f - t)
        val start = 8f * density
        val radius = start + (p.size - start) * ease
        for (layer in 0 until 2) {
            val r = if (layer == 0) radius else radius * 0.62f
            path.reset()
            for (i in 0 until 6) {
                val angle = i * Math.PI / 3.0 // vertex at 0 degrees: flat top and bottom, every time
                val x = p.x + kotlin.math.cos(angle).toFloat() * r
                val y = p.y + kotlin.math.sin(angle).toFloat() * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            if (layer == 0 && t < 0.3f) {
                paint.style = Paint.Style.FILL
                paint.color = p.colour
                paint.alpha = ((0.3f - t) / 0.3f * 70f).toInt().coerceIn(0, 255)
                canvas.drawPath(path, paint)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = maxOf(1.5f, ((if (layer == 0) 3.6f else 2f) * (1f - t) + 1f) * density)
            paint.strokeJoin = Paint.Join.MITER
            paint.color = p.colour
            paint.alpha = ((1f - t) * (1f - t) * (if (layer == 0) 240f else 150f)).toInt().coerceIn(0, 255)
            canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object { const val MAX_PARTICLES = 160 }
}

/** The tap animation for this theme drawn over [host], or null when the theme has none. */
fun KeyboardTheme.tapEffectDrawable(host: ViewGroup, settings: KeyboardSettings): TapEffectDrawable? =
    tapEffect?.let {
        TapEffectDrawable(it, host.resources.displayMetrics.density, effectColours ?: intArrayOf(0xFFFFFFFF.toInt()), settings.effectAmount)
    }

/** Fixed star brightness (the stars no longer follow the typing pulse); matches the old look at the default pulse. */
private const val STAR_BRIGHTNESS = 0.425f

/** The twinkling stars drawn over [host], in this theme's colours and the chosen star density. */
fun KeyboardTheme.twinkleDrawable(host: ViewGroup, settings: KeyboardSettings): GlitterTwinkleDrawable =
    if (starColours != null) {
        GlitterTwinkleDrawable(host, host.resources.displayMetrics.density, STAR_BRIGHTNESS, starColours, settings.starDensity)
    } else {
        GlitterTwinkleDrawable(host, host.resources.displayMetrics.density, STAR_BRIGHTNESS, starDensity = settings.starDensity)
    }

/**
 * The keyboard's background for this theme (plain colour, or the pinstriped backdrop). With [animated] a
 * theme that has an ambient animation draws it on top; the settings screens leave it off, so they stay still.
 * [effectAmount] is the "Effect amount" setting (see above).
 */
fun KeyboardTheme.backgroundDrawable(view: View, animated: Boolean = false, effectAmount: Int = 100): Drawable {
    val flat = flatBackgroundDrawable(view)
    val kind = ambient
    // A theme with a tap animation shares one "Effect amount" slider between that and its background
    // animation, so 0% turns both off. (Themes without a tap animation have no such slider.)
    val on = animated && kind != null && (!ambientFollowsEffectAmount || effectAmount > 0)
    return if (kind != null && on) {
        AmbientBackground(flat, kind, ambientColours ?: IntArray(0), view.resources.displayMetrics.density)
    } else {
        flat
    }
}

/**
 * The theme's [KeyboardTheme.overlay] animation, to be drawn over the keys, or null when it has none (or the "Effect
 * amount" slider it shares with the tap animation is at 0).
 */
fun KeyboardTheme.overlayDrawable(view: View, effectAmount: Int): Drawable? {
    val kind = overlay ?: return null
    if (ambientFollowsEffectAmount && effectAmount <= 0) return null
    return AmbientBackground(ColorDrawable(0), kind, overlayColours ?: IntArray(0), view.resources.displayMetrics.density)
}

private fun KeyboardTheme.flatBackgroundDrawable(view: View): Drawable =
    if (backgroundBottom != 0) {
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(background, backgroundBottom))
    } else {
        when (backdrop) {
            Backdrop.FLAT -> ColorDrawable(background)
            Backdrop.HEARTS -> GlitterPatternDrawable(background, view.resources.displayMetrics.density)
            Backdrop.STRIPES -> BackdropDrawable(
                base = background,
                stripe = 0x08FFFFFF,
                red = accent,
                deep = deep,
                density = view.resources.displayMetrics.density,
            )
        }
    }

/** The drawable behind the candidate words on the strip. */
fun KeyboardTheme.stripDrawable(density: Float): Drawable = when (stripStyle) {
    StripStyle.FLAT -> ColorDrawable(strip)
    StripStyle.PATTERN -> GlitterPatternDrawable(strip, density, stripMode = true)
    StripStyle.PIXEL_SCREEN -> PixelScreenDrawable(strip, density)
    StripStyle.HANDHELD_SCREEN -> HandheldScreenDrawable(strip, border, density)
    StripStyle.WINDOW -> WindowStripDrawable(strip, keyPressed, border, density)
}

/** The blinking key cursor for themes that have one (MS-DOS), or null. */
fun KeyboardTheme.cursorDrawable(tracker: KeyTracker, density: Float): KeyCursorDrawable? =
    if (cursorColour != 0) KeyCursorDrawable(tracker, cursorColour, density) else null

/** Sets [view]'s background for this theme. */
fun KeyboardTheme.applyBackground(view: View) {
    view.background = backgroundDrawable(view)
}

/**
 * Wraps a background and, on [pulse], washes it with a glow of the accent colour that spreads out from the key
 * that was pressed (the bottom centre when that is not known) and fades. [strength] is the glow's peak opacity;
 * 0 disables it. Key presses are also passed on to the background underneath, for the animated ones that react.
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

    // Keeps an animated base (see AmbientBackground) running: it asks this drawable for its next frame.
    private val forward = ForwardCallback(this)

    init { base.callback = forward }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        base.setVisible(visible, restart)
        return super.setVisible(visible, restart)
    }

    override fun onBoundsChange(bounds: Rect) {
        base.bounds = bounds
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

    override fun pulse() = pulse(null)

    /** Flash now, from [key], and fade out. */
    override fun pulse(key: RectF?) {
        (base as? KeyboardPulseEffect)?.pulse(key)
        if (strength <= 0f) return
        val b = bounds
        if (b.isEmpty) return
        paint.shader = RadialGradient(
            key?.centerX() ?: b.exactCenterX(),
            key?.centerY() ?: b.bottom.toFloat(),
            maxOf(b.width(), b.height()) * 0.55f,
            accent,
            accent and 0x00FFFFFF,
            Shader.TileMode.CLAMP,
        )
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
    actionSlot: Int = 0,
    rowPosition: Int = 0,
    rowCount: Int = 1,
): Drawable {
    val normal = keyColor ?: if (action) actionFill(actionSlot) else key
    val pressed = pressedKeyColor ?: if (action) actionFillPressed(actionSlot) else keyPressed

    // The corner radius for the styles that round their keys (none when the theme asks for square keys).
    val corner = if (squareKeys) 0f else cornerDp * density

    // The wide end keys of an indented QWERTY row pull their shape in, so the lettering stays centred on it.
    fun framed(shape: Drawable): Drawable =
        if (leftFrac > 0f || rightFrac > 0f) InsetDrawable(shape, leftFrac, 0f, rightFrac, 0f) else shape

    fun layer(color: Int, isPressed: Boolean = false): Drawable {
        when (keyStyle) {
            KeyStyle.GLOSS -> return GlossKeyDrawable(
                fill = color,
                border = border,
                ring = keyRing,
                pressed = isPressed,
                gloss = keyGloss,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                radius = cornerDp * density,
                density = density,
            )
            KeyStyle.LCD -> return LcdKeyDrawable(
                fill = color,
                border = border,
                edge = shadow,
                pressed = isPressed,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.MEMPHIS -> return MemphisKeyDrawable(
                fill = color,
                border = border,
                shadow = shadow,
                pressed = isPressed,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.CHAMFER -> return ChamferKeyDrawable(
                fill = color,
                border = border,
                pressed = isPressed,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.BEVEL -> return BevelKeyDrawable(
                fill = color,
                pressed = isPressed,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.PILL -> return PillKeyDrawable(
                fill = color,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.RETRO -> return RetroKeyDrawable(
                fill = color,
                pressed = isPressed,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
            )
            KeyStyle.PIXEL -> return PixelGlitterKeyDrawable(
                fill = color,
                shadow = this.shadow,
                border = if (pixelBorder != 0) pixelBorder else this.shadow,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                density = density,
                pixelColors = pixelColours,
            )
            KeyStyle.NOKIA -> return NokiaKeyDrawable(
                fill = color,
                shadow = this.shadow,
                insetX = gapXDp * density,
                insetY = gapYDp * density,
                radius = 7f * density,
            )
            KeyStyle.SLANTED -> return SlantKeyDrawable(
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
            KeyStyle.BRACKET -> return framed(
                BracketKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.RETICLE -> return framed(
                ReticleKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.SCANLINE -> return framed(
                ScanlineKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.FRAME -> return framed(
                FrameKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.GLITCH -> return framed(
                GlitchKeyDrawable(color, border, accent, keySub, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.NEON -> return framed(
                NeonKeyDrawable(
                    color, if (border != 0) border else accent, corner, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.BACKLIT -> return framed(
                BacklitKeyDrawable(
                    color, if (action) lighten(color, 0.35f) else keyText, border, corner, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.BUBBLE -> return framed(
                BubbleKeyDrawable(
                    color, if (border != 0) border else lighten(color, 0.5f), isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.UNDERGLOW -> return framed(
                UnderglowKeyDrawable(
                    color, accent, border, corner * 0.8f, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.PETAL -> return framed(
                PetalKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.DISH -> return framed(
                DishKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.DOME -> return framed(
                DomeKeyDrawable(
                    color, border, corner * 1.3f, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.SLAB -> return framed(
                SlabKeyDrawable(
                    color, border, if (squareKeys) 0f else 5f * density, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.PLATE -> return framed(
                PlateKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.ETCHED -> return framed(
                EtchedKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.MOULDED -> return framed(
                MouldedKeyDrawable(color, border, corner, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.RIVETED -> return framed(
                RivetedKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.WINDOW -> return framed(
                WindowKeyDrawable(
                    color, if (isPressed) lighten(color, 0.4f) else pressed, border, isPressed, corner,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.PHOSPHOR -> return framed(
                PhosphorKeyDrawable(color, border, deep, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.DECO -> return framed(
                DecoKeyDrawable(color, border, accent, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.STENCIL -> return framed(
                StencilKeyDrawable(
                    color, if (action) keyText else accent, isPressed,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.PANEL -> return framed(
                PanelKeyDrawable(color, accent, isPressed, corner, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.TERRAIN -> return framed(
                TerrainKeyDrawable(
                    color, shadow, border, isPressed, corner,
                    gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.CHECKER -> return framed(
                CheckerKeyDrawable(color, border, shadow, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.BLOCK -> return framed(
                BlockKeyDrawable(color, border, action, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.SCRATCHED -> return framed(
                ScratchedKeyDrawable(color, border, shadow, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.STITCH -> return framed(
                StitchKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.CHROME -> return framed(
                ChromeKeyDrawable(color, accent, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.TICKET -> return framed(
                TicketKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.SPEECH -> return framed(
                SpeechKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.GOTHIC -> return framed(
                GothicKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.PLAYER -> return framed(
                PlayerKeyDrawable(color, if (action) accent else 0, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.GLOWLINE -> return framed(
                GlowLineKeyDrawable(
                    // Action keys take their line from their own colour and keep a near-black body.
                    if (action) darken(color, 0.9f) else color, if (action) color else border,
                    isPressed, rowPosition, rowCount, gapXDp * density, gapYDp * density, density,
                ),
            )
            KeyStyle.SHELL -> return framed(
                ShellKeyDrawable(color, shadow, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.CLAY -> return framed(
                ClayKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.MENU -> return framed(
                MenuKeyDrawable(color, border, isPressed, gapXDp * density, gapYDp * density, density),
            )
            KeyStyle.DEFAULT -> Unit
        }
        val shape = GradientDrawable().apply {
            setColor(color)
            cornerRadius = if (squareKeys) 0f else cornerDp * density
            if (border != 0) setStroke(maxOf(1, (1.5f * density).toInt()), border) // outlined keys
        }
        val gx = (gapXDp * density).toInt()
        val gy = (gapYDp * density).toInt()
        val gapped = InsetDrawable(shape, gx, gy, gx, gy)
        return if (leftFrac > 0f || rightFrac > 0f) InsetDrawable(gapped, leftFrac, 0f, rightFrac, 0f) else gapped
    }

    return StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), layer(pressed, true))
        addState(intArrayOf(), layer(normal))
    }
}

/** Highlight behind the selected candidate word; the shape comes from [KeyboardTheme.candidateShape]. */
fun KeyboardTheme.candidateBackground(density: Float): Drawable = when (candidateShape) {
    CandidateStyle.BEVEL ->
        BevelKeyDrawable(key, pressed = false, insetX = 2f * density, insetY = 6f * density, density = density)
    CandidateStyle.PILL ->
        PillKeyDrawable(accent, insetX = 2f * density, insetY = 6f * density, density = density)
    CandidateStyle.CHAMFER -> ChamferKeyDrawable(
        fill = accent,
        border = border,
        pressed = false,
        insetX = 2f * density,
        insetY = 6f * density,
        density = density,
    )
    CandidateStyle.SQUARE -> GradientDrawable().apply {
        setColor(accent)
        cornerRadius = 0f
    }
    CandidateStyle.OUTLINED_SQUARE -> GradientDrawable().apply {
        setColor(accent)
        setStroke(maxOf(1, (2f * density).toInt()), border)
        cornerRadius = 0f
    }
    CandidateStyle.PIXEL -> GradientDrawable().apply {
        setColor(accent)
        setStroke(maxOf(1, (2f * density).toInt()), 0xFFFFFFFF.toInt())
        cornerRadius = 0f
    }
    CandidateStyle.NOKIA -> GradientDrawable().apply {
        setColor(accent)
        cornerRadius = 3f * density
    }
    CandidateStyle.SLANTED -> SlantKeyDrawable(
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
    CandidateStyle.ROUNDED -> {
        val shape = GradientDrawable().apply {
            setColor(accent)
            cornerRadius = 8f * density
        }
        val gx = (2f * density).toInt()
        val gy = (6f * density).toInt()
        InsetDrawable(shape, gx, gy, gx, gy)
    }
}
