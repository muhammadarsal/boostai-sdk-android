package no.boostai.sdk.UI

interface ChatMessageButtonDelegate {
    fun didTapActionButton()
    fun enableActionButtons()
    fun disableActionButtons()
}