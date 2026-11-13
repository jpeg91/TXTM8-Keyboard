// app/src/main/kotlin/com/m8/keyboard/SlideTouch.kt
package com.m8.keyboard

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/** Distance the finger must travel before a press turns into a slide. */
private const val SLIDE_START_DP = 12f

/**
 * Touch handling for the backspace key.
 *
 * A tap deletes one character, when the finger is lifted. Holding repeats. Sliding left selects
 * text a character at a time (sliding back right shrinks the selection), and lifting the finger
 * deletes what is selected. Nothing is deleted until the finger lifts, so a slide never costs
 * a character at its start.
 */
class BackspaceTouch(
    private val settings: KeyboardSettings,
    private val onDelete: () -> Unit,
    private val onSlide: (Int) -> Unit,
    private val onSlideEnd: (Boolean) -> Unit,
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var sliding = false
    private var repeating = false
    private var applied = 0

    private val repeatTask = object : Runnable {
        override fun run() {
            repeating = true
            onDelete()
            handler.postDelayed(this, 60L)
        }
    }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val density = v.resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                sliding = false
                repeating = false
                applied = 0
                v.isPressed = true
                settings.feedback(v)
                handler.removeCallbacks(repeatTask)
                handler.postDelayed(repeatTask, 400L)
            }
            MotionEvent.ACTION_MOVE -> if (!repeating && settings.slideGestures) {
                val dx = downX - e.x // positive: the finger moved left
                if (!sliding && dx > SLIDE_START_DP * density) {
                    sliding = true
                    handler.removeCallbacks(repeatTask)
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (sliding) {
                    val target = (dx / (settings.slideStepDp() * density)).toInt().coerceAtLeast(0)
                    if (target != applied) {
                        onSlide(target - applied)
                        applied = target
                        settings.tick(v)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(repeatTask)
                v.isPressed = false
                if (sliding) onSlideEnd(true) else if (!repeating) onDelete()
                reset()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(repeatTask)
                v.isPressed = false
                if (sliding) onSlideEnd(false) // leave the selection alone: something interrupted us
                reset()
            }
        }
        return true
    }

    private fun reset() {
        sliding = false
        repeating = false
        applied = 0
    }
}

/**
 * Sliding a finger along the space key moves the cursor, one step per character (negative =
 * left). [onMove] is given the steps just moved and which way the slide started (-1 = left,
 * 1 = right): with text selected, that says which end of the selection the slide controls.
 * Plain taps and long presses are left to the key's own click listeners.
 */
class SpaceSlideTouch(
    private val settings: KeyboardSettings,
    private val onMove: (steps: Int, startDirection: Int) -> Unit,
) : View.OnTouchListener {

    private var downX = 0f
    private var sliding = false
    private var applied = 0
    private var startDirection = 0

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val density = v.resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.x
                sliding = false
                applied = 0
            }
            MotionEvent.ACTION_MOVE -> {
                if (!settings.slideGestures) return false
                val dx = e.x - downX
                if (!sliding && abs(dx) > SLIDE_START_DP * density) {
                    sliding = true
                    startDirection = if (dx < 0) -1 else 1
                    // Cancel the key's own press, so neither a click nor a long press follows.
                    val cancel = MotionEvent.obtain(e)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    v.onTouchEvent(cancel)
                    cancel.recycle()
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
                if (sliding) {
                    val target = (dx / (settings.slideStepDp() * density)).toInt()
                    if (target != applied) {
                        onMove(target - applied, startDirection)
                        applied = target
                        settings.tick(v)
                    }
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val wasSliding = sliding
                sliding = false
                applied = 0
                return wasSliding
            }
        }
        return false
    }
}
