package de.robnice.elly.wear.remote

import android.os.Process
import android.os.SystemClock
import java.util.concurrent.locks.LockSupport
import kotlin.math.roundToLong

// The watch delivers gyroscope samples in bursts, often a tenth of a second late, and sent as they arrive the TV
// pointer jumps. The samples are lined up as a path over their sample times instead, and a steady tick sends the
// way along that path from a moment slightly in the past: between two samples it interpolates, past the last one
// it goes on with the recent speed for a short while until the late samples arrive and the path takes over again.
class PointerPacer(private val send: (dx: Int, dy: Int) -> Unit) {

    private val lock = Any()
    private val samples = ArrayDeque<Sample>()
    private var pathX = 0.0
    private var pathY = 0.0
    private var finishing = false
    private var worker: Thread? = null

    fun start() {
        synchronized(lock) {
            finishing = false
            if (worker != null) return
            samples.clear()
            pathX = 0.0
            pathY = 0.0
            worker = Thread(::run, THREAD_NAME).apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    // The steps still on their way are sent, then the tick stops.
    fun finish() {
        synchronized(lock) { finishing = true }
    }

    fun stop() {
        synchronized(lock) {
            worker?.interrupt()
            worker = null
        }
    }

    // sampleNanos is the sensor event time, on the elapsedRealtimeNanos clock.
    fun add(dx: Float, dy: Float, sampleNanos: Long) {
        synchronized(lock) {
            pathX += dx
            pathY += dy
            samples.addLast(Sample(sampleNanos, pathX, pathY))
            while (samples.size > MAX_SAMPLES) samples.removeFirst()
        }
    }

    private fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY)
        var sentX = 0L
        var sentY = 0L
        var next = SystemClock.elapsedRealtimeNanos()
        while (!Thread.currentThread().isInterrupted) {
            next += TICK_NANOS
            val wait = next - SystemClock.elapsedRealtimeNanos()
            if (wait > 0) LockSupport.parkNanos(wait) else next = SystemClock.elapsedRealtimeNanos()
            val moment = SystemClock.elapsedRealtimeNanos() - DELAY_NANOS
            val position = positionAt(moment) ?: continue
            val x = position.first.roundToLong()
            val y = position.second.roundToLong()
            if (x != sentX || y != sentY) {
                send((x - sentX).toInt(), (y - sentY).toInt())
                sentX = x
                sentY = y
            }
            if (done(moment)) break
        }
        synchronized(lock) { if (worker == Thread.currentThread()) worker = null }
    }

    private fun positionAt(moment: Long): Pair<Double, Double>? = synchronized(lock) {
        val last = samples.lastOrNull() ?: return null
        // After the finger was lifted no more samples come, so the pointer ends exactly on the last one.
        if (moment >= last.nanos) return if (finishing) last.x to last.y else extrapolate(last, moment)
        val after = samples.indexOfFirst { it.nanos > moment }
        if (after <= 0) return samples.first().x to samples.first().y
        val a = samples[after - 1]
        val b = samples[after]
        val share = (moment - a.nanos).toDouble() / (b.nanos - a.nanos)
        (a.x + (b.x - a.x) * share) to (a.y + (b.y - a.y) * share)
    }

    // Goes on with the speed of the last samples, for a short while only, so a lifted finger stops the pointer.
    private fun extrapolate(last: Sample, moment: Long): Pair<Double, Double> {
        val from = samples.lastOrNull { last.nanos - it.nanos >= SPEED_WINDOW_NANOS } ?: samples.first()
        if (from === last) return last.x to last.y
        val span = (last.nanos - from.nanos).toDouble()
        val ahead = minOf(moment - last.nanos, MAX_EXTRAPOLATION_NANOS).toDouble()
        return (last.x + (last.x - from.x) / span * ahead) to (last.y + (last.y - from.y) / span * ahead)
    }

    private fun done(moment: Long): Boolean = synchronized(lock) {
        val last = samples.lastOrNull()
        finishing && (last == null || moment > last.nanos)
    }

    private class Sample(val nanos: Long, val x: Double, val y: Double)

    private companion object {
        const val THREAD_NAME = "EllyPointerPacer"
        const val TICK_NANOS = 10_000_000L
        const val DELAY_NANOS = 40_000_000L
        const val SPEED_WINDOW_NANOS = 30_000_000L
        const val MAX_EXTRAPOLATION_NANOS = 60_000_000L
        const val MAX_SAMPLES = 64
    }
}
