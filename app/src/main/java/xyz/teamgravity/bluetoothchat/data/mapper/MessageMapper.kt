package xyz.teamgravity.bluetoothchat.data.mapper

import xyz.teamgravity.bluetoothchat.domain.model.MessageModel

fun MessageModel.toByteArray(): ByteArray {
    return "$sender#$message".encodeToByteArray()
}

fun ByteArray.toMessage(count: Int, local: Boolean): MessageModel {
    val data = decodeToString(endIndex = count)
    val message = data.substringAfter("#")
    val sender = data.substringBefore("#")
    return MessageModel(
        message = message,
        sender = sender,
        local = local
    )
}