package no.boostai.sdk.UI.Helpers

import no.boostai.sdk.ChatBackend.ChatBackend
import no.boostai.sdk.ChatBackend.Objects.ChatPanelDefaults
import no.boostai.sdk.ChatBackend.Objects.ConversationPace

object TimingHelper {

    private const val baseStaggerDelay: Long = 150
    private const val defaultDelay: Long = 1500

    fun calculatePace(pace: ConversationPace): Double = when (pace) {
        ConversationPace.GLACIAL -> 0.333
        ConversationPace.SLOWER -> 0.5
        ConversationPace.SLOW -> 0.8
        ConversationPace.NORMAL -> 1.0
        ConversationPace.FAST -> 1.25
        ConversationPace.FASTER -> 2.0
        ConversationPace.SUPERSONIC -> 3.0
    }

    fun calculateStaggerDelay(pace: ConversationPace, idx: Int): Long {
        val delay = baseStaggerDelay * idx
        val multiplier = calculatePace(pace)

        return (delay / multiplier).toLong()
    }

    fun calcTimeToRead(pace: Double): Long = (defaultDelay / pace).toLong()

    fun timeUntilReveal(): Long {
        val pace = ChatBackend.config?.chatPanel?.styling?.pace ?: ChatPanelDefaults.Styling.pace
        val paceFactor = calculatePace(pace)

        return calcTimeToRead(paceFactor)
    }

}