package xyz.teamgravity.bluetoothchat.data.mapper

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import xyz.teamgravity.bluetoothchat.domain.model.DeviceModel

@SuppressLint("MissingPermission")
fun BluetoothDevice.toDevice(): DeviceModel {
    return DeviceModel(
        name = name,
        address = address
    )
}