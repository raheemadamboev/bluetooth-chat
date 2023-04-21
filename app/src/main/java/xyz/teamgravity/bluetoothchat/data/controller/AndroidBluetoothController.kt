package xyz.teamgravity.bluetoothchat.data.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.teamgravity.bluetoothchat.core.receiver.BluetoothFoundReceiver
import xyz.teamgravity.bluetoothchat.core.receiver.BluetoothStateReceiver
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

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _scannedDevices = MutableStateFlow(emptyList<DeviceModel>())
    override val scannedDevices: StateFlow<List<DeviceModel>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow(emptyList<DeviceModel>())
    override val pairedDevices: StateFlow<List<DeviceModel>> = _pairedDevices.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var service: BluetoothDataTransferService? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null

    init {
        scope.launch {
            updatePairedDevices()
            registerBluetoothStateReceiver()
        }
    }

    private suspend fun updatePairedDevices() {
        if (!PermissionUtil.canBluetoothConnect(context)) return
        val devices = adapter
            ?.bondedDevices
            ?.map { it.toDevice() }
        if (devices != null) _pairedDevices.emit(devices)
    }

    private fun registerBluetoothStateReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        context.registerReceiver(stateReceiver, filter)
    }

    private fun registerBluetoothFoundReceiver() {
        context.registerReceiver(foundReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
    }

    private fun unregisterBluetoothStateReceiver() {
        context.unregisterReceiver(stateReceiver)
    }

    private fun unregisterBluetoothFoundReceiver() {
        context.unregisterReceiver(foundReceiver)
    }

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    override fun bluetoothEnabled(): Boolean {
        return adapter?.isEnabled ?: false
    }

    override fun startDiscovery() {
        if (!PermissionUtil.canBluetoothScan(context)) return
        scope.launch {
            registerBluetoothFoundReceiver()
            updatePairedDevices()
            adapter?.startDiscovery()
        }
    }

    override fun stopDiscovery() {
        if (!PermissionUtil.canBluetoothScan(context)) return
        adapter?.cancelDiscovery()
    }

    override fun startServer(): Flow<BluetoothController.ConnectionResult> {
        return flow {
            if (!PermissionUtil.canBluetoothConnect(context)) {
                emit(BluetoothController.ConnectionResult.Error("No BLUETOOTH_CONNECT permission"))
                return@flow
            }

            serverSocket = adapter?.listenUsingRfcommWithServiceRecord(SERVICE_NAME, UUID.fromString(SERVICE_UUID))

            clientSocket = try {
                serverSocket?.accept()
            } catch (e: IOException) {
                Timber.e(e)
                return@flow
            }
            emit(BluetoothController.ConnectionResult.Established)
            if (clientSocket != null) {
                serverSocket?.close()
                service = BluetoothDataTransferService(clientSocket!!)
                emitAll(
                    service!!
                        .getMessages()
                        .map { BluetoothController.ConnectionResult.Transferred(it) }
                )
            }
        }.onCompletion {
            close()
        }.flowOn(Dispatchers.IO)
    }

    override fun connect(device: DeviceModel): Flow<BluetoothController.ConnectionResult> {
        return flow {
            if (!PermissionUtil.canBluetoothConnect(context)) {
                emit(BluetoothController.ConnectionResult.Error("No BLUETOOTH_CONNECT permission"))
                return@flow
            }

            clientSocket = adapter
                ?.getRemoteDevice(device.address)
                ?.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_UUID))

            stopDiscovery()

            try {
                clientSocket?.connect()
                emit(BluetoothController.ConnectionResult.Established)

                if (clientSocket != null) {
                    service = BluetoothDataTransferService(clientSocket!!)
                    emitAll(
                        service!!
                            .getMessages()
                            .map { BluetoothController.ConnectionResult.Transferred(it) }
                    )
                }
            } catch (e: IOException) {
                emit(BluetoothController.ConnectionResult.Error("Connection was interrupted"))
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

    override fun release() {
        unregisterBluetoothStateReceiver()
        unregisterBluetoothFoundReceiver()
        close()
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

    private val foundReceiver = BluetoothFoundReceiver { device ->
        _scannedDevices.update { devices ->
            val model = device.toDevice()
            if (model in devices) devices else devices + model
        }
    }

    private val stateReceiver = BluetoothStateReceiver { connected, device ->
        scope.launch {
            if (adapter?.bondedDevices?.contains(device) == true) {
                _connected.emit(connected)
            } else {
                _errors.emit("Can't connect to a non-paired device.")
            }
        }
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
                if (!socket.isConnected) return@flow
                val buffer = ByteArray(1024)
                while (true) {
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