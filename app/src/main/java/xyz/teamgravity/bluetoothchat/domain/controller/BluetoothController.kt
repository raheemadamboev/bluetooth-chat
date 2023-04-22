package xyz.teamgravity.bluetoothchat.domain.controller

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import xyz.teamgravity.bluetoothchat.domain.model.DeviceModel
import xyz.teamgravity.bluetoothchat.domain.model.MessageModel

interface BluetoothController {

    val pairedDevices: StateFlow<List<DeviceModel>>

    fun bluetoothEnabled(): Boolean
    fun refreshPairedDevices()
    fun startServer(): Flow<ConnectionResult>
    fun connect(device: DeviceModel): Flow<ConnectionResult>
    fun close()

    suspend fun sendMessage(message: String): MessageModel?

    ///////////////////////////////////////////////////////////////////////////
    // MISC
    ///////////////////////////////////////////////////////////////////////////

    sealed interface ConnectionResult {
        object Established : ConnectionResult
        data class Transferred(val message: MessageModel) : ConnectionResult
        data class Error(val message: String) : ConnectionResult
    }
}