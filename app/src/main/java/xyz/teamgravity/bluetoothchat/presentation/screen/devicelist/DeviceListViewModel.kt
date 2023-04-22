package xyz.teamgravity.bluetoothchat.presentation.screen.devicelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import xyz.teamgravity.bluetoothchat.domain.controller.BluetoothController
import xyz.teamgravity.bluetoothchat.domain.model.DeviceModel
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val controller: BluetoothController,
) : ViewModel() {

    private val _event = Channel<DeviceListEvent>()
    val event: Flow<DeviceListEvent> = _event.receiveAsFlow()

    var pairedDevices: List<DeviceModel> by mutableStateOf(emptyList())
        private set

    init {
        observe()
    }

    private fun observe() {
        observePairedDevices()
    }

    private fun observePairedDevices() {
        viewModelScope.launch {
            controller.pairedDevices.collectLatest { pairedDevices ->
                this@DeviceListViewModel.pairedDevices = pairedDevices
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        controller.close()
    }

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    fun onConnect(device: DeviceModel) {
        viewModelScope.launch {
            _event.send(DeviceListEvent.NavigateChat(device))
        }
    }

    fun onRefresh() {
        controller.refreshPairedDevices()
    }

    fun onStartServer() {
        viewModelScope.launch {
            _event.send(DeviceListEvent.NavigateChat(null))
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MISC
    ///////////////////////////////////////////////////////////////////////////

    sealed interface DeviceListEvent {
        data class NavigateChat(val device: DeviceModel?) : DeviceListEvent
    }
}