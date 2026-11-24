package com.m8.keyboard

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * One shared clock for every animated drawable (backdrop, tap effects, typing pulse, star twinkles, cursor).
 *
 * Before, each layer ran its own timer, so a keyboard with a backdrop, stars and a cursor redrew the whole
 * background several times in every 100 ms, at slightly different moments. Now a layer says when it next needs
 * a frame ([request]) and the clock rounds that up to a fixed grid ([GRID_MS], about 24 frames a second). Every
 * layer that falls due on the same grid tick is invalidated together, from a single message, so Android draws
 * once for all of them instead of once each.
 *
 * Layers still work out their own frame from the time ([SystemClock.uptimeMillis]) when they draw, so their
 * speed does not change: the grid only decides when a redraw happens, never how far an animation has moved.
 *
 * Everything runs on the main thread.
 */
object AnimationClock {
    /** The length of one tick in milliseconds. 42 ms is about 24 frames a second. */
    const val GRID_MS = 42L

    private class Entry {
        var drawable: Drawable? = null
        var due = 0L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val waiting = ArrayList<Entry>()
    private val ready = ArrayList<Entry>()
    private val spare = ArrayList<Entry>()
    private var postedFor = Long.MAX_VALUE
    private val run = Runnable { fire() }

    /** The first grid tick at or after [uptime]. */
    fun align(uptime: Long): Long = (uptime + GRID_MS - 1) / GRID_MS * GRID_MS

    /**
     * Asks for [drawable] to be invalidated at the first grid tick at or after [uptime] (a time on the
     * [SystemClock.uptimeMillis] clock). A drawable has at most one request waiting: asking again replaces it.
     */
    fun request(drawable: Drawable, uptime: Long) {
        val due = align(uptime)
        var entry: Entry? = null
        for (e in waiting) {
            if (e.drawable === drawable) {
                entry = e
                break
            }
        }
        if (entry == null) {
            entry = if (spare.isEmpty()) Entry() else spare.removeAt(spare.size - 1)
            entry.drawable = drawable
            waiting.add(entry)
        }
        entry.due = due
        if (due < postedFor) {
            handler.removeCallbacks(run)
            postedFor = due
            handler.postAtTime(run, due)
        }
    }

    /** Drops [drawable]'s waiting request, if it has one (hidden, stopped, or about to ask again). */
    fun cancel(drawable: Drawable) {
        for (i in waiting.indices) {
            val e = waiting[i]
            if (e.drawable === drawable) {
                waiting.removeAt(i)
                e.drawable = null
                spare.add(e)
                return
            }
        }
    }

    private fun fire() {
        postedFor = Long.MAX_VALUE
        val now = SystemClock.uptimeMillis()
        var next = Long.MAX_VALUE
        var i = 0
        while (i < waiting.size) {
            val e = waiting[i]
            if (e.due <= now) {
                ready.add(e)
                waiting.removeAt(i)
            } else {
                if (e.due < next) next = e.due
                i++
            }
        }
        // All the due layers are invalidated in this one message, so Android draws once for the lot.
        for (e in ready) e.drawable?.invalidateSelf()
        for (e in ready) {
            e.drawable = null
            spare.add(e)
        }
        ready.clear()
        // Layers may have asked again while being invalidated; otherwise wake for whatever is still waiting.
        if (next < postedFor) {
            handler.removeCallbacks(run)
            postedFor = next
            handler.postAtTime(run, next)
        }
    }
}
