package no.boostai.sdk.UI.Helpers

import no.boostai.sdk.ChatBackend.ChatBackend

object TimingHelper {

    private const val baseStaggerDelay: Long = 150
    private const val defaultDelay: Long = 1500

    fun calculatePace(pace: String): Double = when (pace) {
        "glacial" -> 0.333
        "slower" -> 0.5
        "slow" -> 0.8
        "normal" -> 1.0
        "fast" -> 1.25
        "faster" -> 2.0
        "supersonic" -> 3.0
        else -> 1.0
    }

    fun calculateStaggerDelay(pace: String, idx: Int): Long {
        val delay = baseStaggerDelay * idx
        val multiplier = calculatePace(pace)

        return (delay / multiplier).toLong()
    }

    fun calcTimeToRead(pace: Double): Long = (defaultDelay / pace).toLong()

    fun timeUntilReveal(): Long {
        val pace = ChatBackend.config?.pace ?: "normal"
        val paceFactor = calculatePace(pace)

        return calcTimeToRead(paceFactor)
    }

}