// app/src/main/kotlin/com/m8/keyboard/DeviceEffects.kt
package com.m8.keyboard

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.sin

/** The ambient animations for the themes that copy a phone or a piece of software. Each is drawn by [AmbientBackground]. */
internal object DeviceAmbient {
    /**
     * A keypad's backlight: a faint even glow over everything, and a soft band of brighter light that rolls up the keyboard
     * from the bottom to the top and then starts again, with a second, dimmer one a little behind it, like the keypad of a
     * phone lighting up. Meant to show through keys of glass. [colours]: 0 the light (ARGB; its alpha is the strongest the
     * band gets).
     */
    fun backlight(canvas: Canvas, paint: Paint, w: Float, h: Float, frame: Int, colours: IntArray) {
        val light = if (colours.isNotEmpty()) colours[0] else 0x3022B8E8
        val strength = (light ushr 24).toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = withAlpha(light, (strength * 0.22f).toInt())
        canvas.drawRect(0f, 0f, w, h, paint)

        for (band in 0 until 2) {
            val progress = ((frame * 0.0065f + band * 0.37f) % 1.5f) - 0.25f
            val centre = h * (1f - progress)
            val half = h * 0.28f
            val alpha = (strength * (if (band == 0) 1f else 0.55f) * (0.75f + 0.25f * sin(frame * 0.05f + band))).toInt()
            paint.shader = LinearGradient(
                0f, centre - half, 0f, centre + half,
                intArrayOf(withAlpha(light, 0), withAlpha(light, alpha), withAlpha(light, 0)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, centre - half, w, centre + half, paint)
        }
        paint.shader = null
    }
}
