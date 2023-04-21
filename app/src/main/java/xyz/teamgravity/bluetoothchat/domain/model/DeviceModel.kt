package xyz.teamgravity.bluetoothchat.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DeviceModel(
    val name: String?,
    val address: String,
) : Parcelable
