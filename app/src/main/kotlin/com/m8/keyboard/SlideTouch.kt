// app/src/main/kotlin/com/m8/keyboard/SlideTouch.kt
package com.m8.keyboard

import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.abs
import kotlin.math.max

/** Distance the finger must travel before a press turns into a slide. */
private const val SLIDE_START_DP = 12f

/**
 * Touch handling for the backspace key.
 *
 * A tap deletes one character, when the finger is lifted. Holding repeats. Sliding left selects
 * text a character at a time (sliding back right shrinks the selection), and lifting the finger
 * deletes what is selected. Nothing is deleted until the finger lifts, so a slide never costs
 * a character at its start.
 *
 * A fast, careless swipe to the left is a flick instead: [onFlick] is called (once) as soon as the finger has
 * travelled far enough, and nothing is selected. The swipe may wander up or down as long as it is mostly leftwards.
 * What counts as fast is judged from how quickly the finger gets going, so a slow slide that speeds up later stays
 * a slide.
 */
class BackspaceTouch(
    private val settings: KeyboardSettings,
    private val onDelete: () -> Unit,
    private val onSlide: (Int) -> Unit,
    private val onSlideEnd: (Boolean) -> Unit,
    private val onFlick: (() -> Unit)? = null,
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var downX = 0f
    private var downY = 0f
    private var sliding = false
    private var flickPending = false // started fast: waiting to see whether it travels far enough to be a flick
    private var flicked = false
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
                downY = e.y
                sliding = false
                flickPending = false
                flicked = false
                repeating = false
                applied = 0
                v.isPressed = true
                settings.feedback(v)
                handler.removeCallbacks(repeatTask)
                handler.postDelayed(repeatTask, 400L)
            }
            MotionEvent.ACTION_MOVE -> if (!repeating && !flicked && settings.slideGestures) {
                val dx = downX - e.x // positive: the finger moved left
                val dy = abs(e.y - downY)
                val elapsed = e.eventTime - e.downTime
                if (!sliding && !flickPending && dx > SLIDE_START_DP * density) {
                    handler.removeCallbacks(repeatTask)
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    val fastStart = elapsed <= FLICK_WINDOW_MS && dx / max(elapsed, 1L) >= FLICK_START_SPEED_DP_PER_MS * density
                    if (onFlick != null && fastStart) flickPending = true else sliding = true
                }
                if (flickPending) {
                    if (dx >= FLICK_MIN_DP * density && dx >= dy * 0.5f) {
                        flickPending = false
                        flicked = true
                        settings.tick(v)
                        onFlick?.invoke()
                    } else if (elapsed > FLICK_WINDOW_MS) {
                        flickPending = false // too slow to be a flick after all: an ordinary slide
                        sliding = true
                    }
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
                if (flickPending) {
                    // Started fast but stopped short of a flick: an ordinary slide of however far it went.
                    flickPending = false
                    sliding = true
                    val target = ((downX - e.x) / (settings.slideStepDp() * density)).toInt().coerceAtLeast(0)
                    if (target != 0) onSlide(target)
                    applied = target
                }
                if (flicked) {
                    // the word has already gone
                } else if (sliding) {
                    onSlideEnd(true)
                } else if (!repeating) {
                    onDelete()
                }
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
        flickPending = false
        flicked = false
        repeating = false
        applied = 0
    }

    private companion object {
        /** A flick must get going within this long of the finger touching down, and finish its distance by then too. */
        const val FLICK_WINDOW_MS = 300L

        /** How far left (dp) a flick must travel. */
        const val FLICK_MIN_DP = 60f

        /** How fast (dp per millisecond) the finger must be going by the time it has moved a little. */
        const val FLICK_START_SPEED_DP_PER_MS = 0.2f
    }
}

/**
 * Watches the touches on the 1 key without consuming them (the key's tap and long press carry on as normal):
 * if the finger stays down [extraMs] after the long press has registered, [onHeld] is called once.
 */
class LongerHoldTouch(
    private val extraMs: Long,
    private val onHeld: () -> Unit,
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private val task = Runnable { onHeld() }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handler.removeCallbacks(task)
                handler.postDelayed(task, ViewConfiguration.getLongPressTimeout() + extraMs)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(task)
        }
        return false
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

/**
 * Touch handling for the paste key.
 *
 *  - Tap: paste.
 *  - Hold, then let go without moving: copy the selected text.
 *  - Hold, then slide: start selecting from the cursor. [onSelect] is given the steps just moved
 *    and which way the slide started (-1 = left, 1 = right), which says which end of the selection
 *    the slide controls. Sliding back shrinks the selection; letting go leaves it selected.
 *
 * Moving a finger away before the hold has registered cancels the press (nothing is pasted).
 */
class PasteTouch(
    private val settings: KeyboardSettings,
    private val onPaste: () -> Unit,
    private val onCopy: () -> Unit,
    private val onSelect: (steps: Int, startDirection: Int) -> Unit,
) : View.OnTouchListener {

    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var downX = 0f
    private var armed = false     // held long enough to count as a hold
    private var sliding = false
    private var cancelled = false
    private var applied = 0
    private var startDirection = 0

    private val armTask = Runnable {
        armed = true
        view?.let { settings.feedback(it, isLong = true) }
    }

    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val density = v.resources.displayMetrics.density
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view = v
                downX = e.x
                armed = false
                sliding = false
                cancelled = false
                applied = 0
                v.isPressed = true
                settings.feedback(v)
                handler.removeCallbacks(armTask)
                handler.postDelayed(armTask, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (settings.slideGestures && !cancelled) {
                val dx = e.x - downX
                if (!sliding && abs(dx) > SLIDE_START_DP * density) {
                    if (armed) {
                        sliding = true
                        startDirection = if (dx < 0) -1 else 1
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        cancelled = true // moved before the hold registered
                        handler.removeCallbacks(armTask)
                        v.isPressed = false
                    }
                }
                if (sliding) {
                    val target = (dx / (settings.slideStepDp() * density)).toInt()
                    if (target != applied) {
                        onSelect(target - applied, startDirection)
                        applied = target
                        settings.tick(v)
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(armTask)
                v.isPressed = false
                when {
                    sliding || cancelled -> {}
                    armed -> onCopy()
                    else -> onPaste()
                }
                reset()
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(armTask)
                v.isPressed = false
                reset()
            }
        }
        return true
    }

    private fun reset() {
        armed = false
        sliding = false
        cancelled = false
        applied = 0
    }
}
