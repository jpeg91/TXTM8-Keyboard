// app/src/main/kotlin/com/m8/keyboard/KeyIcons.kt
package com.m8.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.sin

/** Symbols drawn on the DEL / ENTER / full-keyboard / paste / settings keys (drawn, so no font has to supply them). */
enum class KeyIcon { BACKSPACE, ENTER, KEYBOARD, PASTE, SETTINGS }

/**
 * Draws [icon] centred on ([cx], [cy]); one icon unit is [u] pixels and the line is [strokePx] thick.
 * Used by the icon keys.
 */
internal fun drawKeyIcon(
    canvas: Canvas,
    icon: KeyIcon,
    cx: Float,
    cy: Float,
    u: Float,
    color: Int,
    strokePx: Float,
    paint: Paint,
    path: Path,
) {
    if (u <= 0f) return
    val (iconW, iconH) = when (icon) {
        KeyIcon.BACKSPACE -> 1.3f to 0.9f
        KeyIcon.ENTER -> 1.1f to 0.9f
        KeyIcon.KEYBOARD -> 1.5f to 0.95f
        KeyIcon.PASTE -> 0.85f to 1.05f
        KeyIcon.SETTINGS -> 1.0f to 1.0f
    }

    canvas.save()
    canvas.translate(cx - iconW * u / 2f, cy - iconH * u / 2f)
    canvas.scale(u, u) // draw in unit coordinates
    paint.shader = null
    paint.color = color
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = strokePx / u
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeJoin = Paint.Join.ROUND
    path.reset()

    when (icon) {
        KeyIcon.BACKSPACE -> {
            // A left-pointing tag with a cross in it.
            path.moveTo(0f, 0.45f)
            path.lineTo(0.3f, 0f)
            path.lineTo(1.3f, 0f)
            path.lineTo(1.3f, 0.9f)
            path.lineTo(0.3f, 0.9f)
            path.close()
            path.moveTo(0.63f, 0.28f)
            path.lineTo(0.97f, 0.62f)
            path.moveTo(0.97f, 0.28f)
            path.lineTo(0.63f, 0.62f)
            canvas.drawPath(path, paint)
        }
        KeyIcon.ENTER -> {
            // The "return" arrow: down, then left with an arrow head.
            path.moveTo(1.0f, 0f)
            path.lineTo(1.0f, 0.6f)
            path.lineTo(0.1f, 0.6f)
            path.moveTo(0.38f, 0.32f)
            path.lineTo(0.1f, 0.6f)
            path.lineTo(0.38f, 0.88f)
            canvas.drawPath(path, paint)
        }
        KeyIcon.KEYBOARD -> {
            // A keyboard outline with two rows of keys and a space bar.
            canvas.drawRoundRect(0f, 0f, 1.5f, 0.95f, 0.12f, 0.12f, paint)
            paint.style = Paint.Style.FILL
            for (x in floatArrayOf(0.27f, 0.57f, 0.87f, 1.17f)) canvas.drawCircle(x, 0.27f, 0.05f, paint)
            for (x in floatArrayOf(0.42f, 0.72f, 1.02f)) canvas.drawCircle(x, 0.48f, 0.05f, paint)
            paint.style = Paint.Style.STROKE
            path.moveTo(0.35f, 0.72f)
            path.lineTo(1.15f, 0.72f)
            canvas.drawPath(path, paint)
        }
        KeyIcon.PASTE -> {
            // A clipboard: a board with a clip at the top and two lines of text.
            canvas.drawRoundRect(0f, 0.12f, 0.85f, 1.05f, 0.1f, 0.1f, paint)
            canvas.drawRoundRect(0.27f, 0f, 0.58f, 0.24f, 0.07f, 0.07f, paint)
            path.moveTo(0.2f, 0.52f)
            path.lineTo(0.65f, 0.52f)
            path.moveTo(0.2f, 0.76f)
            path.lineTo(0.5f, 0.76f)
            canvas.drawPath(path, paint)
        }
        KeyIcon.SETTINGS -> {
            // A cog: a ring with a hub, and eight teeth.
            canvas.drawCircle(0.5f, 0.5f, 0.15f, paint)
            canvas.drawCircle(0.5f, 0.5f, 0.31f, paint)
            for (i in 0 until 8) {
                val a = (i * Math.PI / 4).toFloat()
                path.moveTo(0.5f + cos(a) * 0.31f, 0.5f + sin(a) * 0.31f)
                path.lineTo(0.5f + cos(a) * 0.48f, 0.5f + sin(a) * 0.48f)
            }
            canvas.drawPath(path, paint)
        }
    }
    canvas.restore()
}

/** A key that shows an icon instead of text. It is a TextView so it can be used like any other key. */
class IconKeyView(
    context: Context,
    private val icon: KeyIcon,
    private val color: Int,
) : TextView(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val u = minOf(width / 2.2f, height * 0.42f) // one icon unit, in pixels
        if (u <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val line = maxOf(1.8f * density, u * 0.1f)

        drawKeyIcon(canvas, icon, cx, cy, u, color, line, paint, path)
    }
}

/** An icon key styled for this theme (same background and gaps as the other keys). */
fun KeyboardTheme.iconKey(
    context: Context,
    icon: KeyIcon,
    action: Boolean,
    gapXDp: Int,
    gapYDp: Int,
    cornerDp: Int,
    description: String,
): IconKeyView {
    val density = context.resources.displayMetrics.density
    val view = IconKeyView(context, icon, if (action) actionText else text)
    view.background = keyBackground(density, action, gapXDp, gapYDp, cornerDp)
    view.contentDescription = description
    return view
}
