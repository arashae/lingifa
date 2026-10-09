package com.example.srs

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round

/** FSRS-6, official default parameters, deterministic intervals (no fuzz).
 * Reference: py-fsrs 9446cb06605c597a063aeee49f7d188d42e34dc2.
 * The app's 30-minute relearning step is a separate scheduling policy.
 */
object Fsrs6 {
    const val VERSION = "fsrs6-default-2026-07"
    const val DAY_MS = 86_400_000L
    const val MAX_INTERVAL = 36_500
    data class Memory(val stability: Double, val difficulty: Double)
    private val w = doubleArrayOf(0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334,
        3.0194, 0.001, 1.8722, 0.1666, 0.796, 1.4835, 0.0614, 0.2629,
        1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542)
    private val decay = -w[20]
    private val factor = 0.9.pow(1.0 / decay) - 1.0

    fun retrievability(stability: Double, elapsedDays: Double): Double {
        require(stability.isFinite() && stability > 0 && elapsedDays.isFinite())
        return (1.0 + factor * floor(elapsedDays.coerceAtLeast(0.0)) / stability).pow(decay)
    }

    fun interval(stability: Double, retention: Double = 0.9): Int {
        require(stability.isFinite() && stability > 0 && retention.isFinite() && retention in 0.8..0.97)
        return round(stability / factor * (retention.pow(1.0 / decay) - 1.0))
            .coerceIn(1.0, MAX_INTERVAL.toDouble()).toInt()
    }

    private fun initialDifficulty(grade: Int) = w[4] - exp(w[5] * (grade - 1)) + 1.0

    fun update(previous: Memory?, elapsedDays: Double, rating: ReviewRating): Memory {
        require(elapsedDays.isFinite())
        val grade = rating.value
        if (previous == null) return Memory(w[grade - 1], initialDifficulty(grade).coerceIn(1.0, 10.0))
        require(previous.stability.isFinite() && previous.stability > 0 && previous.difficulty.isFinite())
        val s = previous.stability.coerceAtLeast(0.001)
        val d = previous.difficulty.coerceIn(1.0, 10.0)
        val nextD = (w[7] * initialDifficulty(4) + (1 - w[7]) *
            (d - w[6] * (grade - 3) * (10 - d) / 9)).coerceIn(1.0, 10.0)
        val nextS = if (elapsedDays < 1) {
            var increase = exp(w[17] * (grade - 3 + w[18])) * s.pow(-w[19])
            if (rating != ReviewRating.AGAIN) increase = increase.coerceAtLeast(1.0)
            s * increase
        } else {
            val r = retrievability(s, elapsedDays)
            if (rating == ReviewRating.AGAIN) {
                minOf(w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp(w[14] * (1 - r)),
                    s / exp(w[17] * w[18]))
            } else {
                val hard = if (rating == ReviewRating.HARD) w[15] else 1.0
                val easy = if (rating == ReviewRating.EASY) w[16] else 1.0
                s * (1 + exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp(w[10] * (1 - r)) - 1) * hard * easy)
            }
        }
        return Memory(nextS.coerceAtLeast(0.001), nextD)
    }
}
