package xyz.teamgravity.bluetoothchat.domain.model

data class MessageModel(
    val message: String,
    val sender: String,
    val local: Boolean,
)
