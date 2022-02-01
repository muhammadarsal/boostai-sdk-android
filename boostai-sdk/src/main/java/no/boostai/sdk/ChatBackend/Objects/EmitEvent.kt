package no.boostai.sdk.ChatBackend.Objects
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class EmitEvent(
    val emitEvent: EmitEventContent
)

@Serializable
data class EmitEventContent(
    val detail: JsonElement? = null,
    val emitOnResume: Boolean = false,
    val type: String
)
