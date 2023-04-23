package xyz.teamgravity.bluetoothchat.presentation.screen.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import xyz.teamgravity.bluetoothchat.domain.controller.BluetoothController
import xyz.teamgravity.bluetoothchat.domain.model.MessageModel
import xyz.teamgravity.bluetoothchat.presentation.screen.destinations.ChatScreenDestination
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val controller: BluetoothController,
) : ViewModel() {

    private val args = ChatScreenDestination.argsFrom(handle)

    private val _event = Channel<ChatEvent>()
    val event: Flow<ChatEvent> = _event.receiveAsFlow()

    var connecting: Boolean by mutableStateOf(true)
        private set

    var message: String by mutableStateOf("")
        private set

    val messages: SnapshotStateList<MessageModel> = mutableStateListOf()

    init {
        getMessages()
    }

    private fun getMessages() {
        viewModelScope.launch {
            val flow = if (args.device == null) controller.startServer() else controller.connect(args.device)
            flow.collectLatest { result ->
                when (result) {
                    BluetoothController.ConnectionResult.Established -> {
                        connecting = false
                    }

                    is BluetoothController.ConnectionResult.Transferred -> {
                        messages.add(result.message)
                    }

                    is BluetoothController.ConnectionResult.Error -> {
                        connecting = false
                        _event.send(ChatEvent.ShowError(result.message))
                        _event.send(ChatEvent.Finish)
                    }
                }
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    fun onMessageChange(value: String) {
        message = value
    }

    fun onDisconnect() {
        viewModelScope.launch {
            controller.close()
        }
    }

    fun onSendMessage() {
        viewModelScope.launch {
            val model = controller.sendMessage(message)
            if (model != null) {
                message = ""
                messages.add(model)
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MISC
    ///////////////////////////////////////////////////////////////////////////

    sealed interface ChatEvent {
        data class ShowError(val message: String) : ChatEvent
        object Finish : ChatEvent
    }
}