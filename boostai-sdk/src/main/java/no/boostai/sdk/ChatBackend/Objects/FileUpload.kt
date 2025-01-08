package no.boostai.sdk.ChatBackend.Objects

class FileUpload(
    val file: java.io.File,
    val filename: String,
    val mimeType: String
)