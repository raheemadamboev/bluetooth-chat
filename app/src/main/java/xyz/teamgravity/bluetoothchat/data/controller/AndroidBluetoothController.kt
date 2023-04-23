package xyz.teamgravity.bluetoothchat.data.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.teamgravity.bluetoothchat.core.util.PermissionUtil
import xyz.teamgravity.bluetoothchat.data.mapper.toByteArray
import xyz.teamgravity.bluetoothchat.data.mapper.toDevice
import xyz.teamgravity.bluetoothchat.data.mapper.toMessage
import xyz.teamgravity.bluetoothchat.domain.controller.BluetoothController
import xyz.teamgravity.bluetoothchat.domain.model.DeviceModel
import xyz.teamgravity.bluetoothchat.domain.model.MessageModel
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
) : BluetoothController {

    companion object {
        private const val SERVICE_UUID = "27b7d1da-08c7-4505-a6d1-2459987e5e2d"
        private const val SERVICE_NAME = "chat_service"
    }

    private val _pairedDevices = MutableStateFlow(emptyList<DeviceModel>())
    override val pairedDevices: StateFlow<List<DeviceModel>> = _pairedDevices.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var service: BluetoothDataTransferService? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var getPairedDevicesJob: Job? = null

    init {
        cancelAndGetPairedDevices()
    }

    private fun cancelAndGetPairedDevices() {
        getPairedDevicesJob?.cancel()
        getPairedDevicesJob = scope.launch {
            getPairedDevices()
        }
    }

    private suspend fun getPairedDevices() {
        if (!PermissionUtil.canBluetoothConnect(context)) return
        val devices = adapter
            ?.bondedDevices
            ?.map { it.toDevice() }
        if (devices != null) _pairedDevices.emit(devices)
    }

    private suspend fun emitAllMessages(collector: FlowCollector<BluetoothController.ConnectionResult>) {
        collector.emitAll(
            service!!
                .getMessages()
                .catch { e ->
                    if (e !is IOException) throw e
                    Timber.e(e)
                    collector.emit(BluetoothController.ConnectionResult.Error("Connection was interrupted!"))
                }.map { BluetoothController.ConnectionResult.Transferred(it) }
        )
    }

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    override fun bluetoothEnabled(): Boolean {
        return adapter?.isEnabled ?: false
    }

    override fun refreshPairedDevices() {
        cancelAndGetPairedDevices()
    }

    override fun startServer(): Flow<BluetoothController.ConnectionResult> {
        return flow {
            if (!PermissionUtil.canBluetoothConnect(context)) {
                emit(BluetoothController.ConnectionResult.Error("No BLUETOOTH_CONNECT permission!"))
                return@flow
            }

            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, UUID.fromString(SERVICE_UUID))

                clientSocket = serverSocket?.accept()
                emit(BluetoothController.ConnectionResult.Established)
                serverSocket?.close()

                if (clientSocket != null) {
                    service = BluetoothDataTransferService(clientSocket!!)
                    emitAllMessages(this)
                }
            } catch (e: IOException) {
                Timber.e(e)
                emit(BluetoothController.ConnectionResult.Error("Connection was interrupted!"))
            }
        }.onCompletion {
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun connect(device: DeviceModel): Flow<BluetoothController.ConnectionResult> {
        return flow {
            if (!PermissionUtil.canBluetoothConnect(context)) {
                emit(BluetoothController.ConnectionResult.Error("No BLUETOOTH_CONNECT permission!"))
                return@flow
            }

            try {
                clientSocket = adapter
                    ?.getRemoteDevice(device.address)
                    ?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))

                clientSocket?.connect()
                emit(BluetoothController.ConnectionResult.Established)

                if (clientSocket != null) {
                    service = BluetoothDataTransferService(clientSocket!!)
                    emitAllMessages(this)
                }
            } catch (e: IOException) {
                Timber.e(e)
                emit(BluetoothController.ConnectionResult.Error("Connection was interrupted!"))
            }
        }.onCompletion {
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun close() {
        serverSocket?.close()
        serverSocket = null
        clientSocket?.close()
        clientSocket = null
    }

    override suspend fun sendMessage(message: String): MessageModel? {
        if (!PermissionUtil.canBluetoothConnect(context)) return null
        if (service == null) return null
        val model = MessageModel(
            message = message,
            sender = adapter?.name ?: "Unknown name",
            local = true
        )
        val success = service?.sendMessage(model) ?: false
        return if (success) model else null
    }

    ///////////////////////////////////////////////////////////////////////////
    // MISC
    ///////////////////////////////////////////////////////////////////////////

    class BluetoothDataTransferService(
        private val socket: BluetoothSocket,
    ) {

        suspend fun sendMessage(message: MessageModel): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    socket.outputStream.write(message.toByteArray())
                    return@withContext true
                } catch (e: IOException) {
                    Timber.e(e)
                    return@withContext false
                }
            }
        }

        fun getMessages(): Flow<MessageModel> {
            return flow {
                val buffer = ByteArray(1024)
                while (socket.isConnected) {
                    val count = socket.inputStream.read(buffer)
                    val message = buffer.toMessage(
                        count = count,
                        local = false
                    )
                    emit(message)
                }
            }.flowOn(Dispatchers.IO)
        }
    }
}