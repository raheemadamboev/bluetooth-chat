package xyz.teamgravity.bluetoothchat.presentation.screen.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest
import xyz.teamgravity.bluetoothchat.R
import xyz.teamgravity.bluetoothchat.domain.model.DeviceModel
import xyz.teamgravity.bluetoothchat.presentation.component.keyboardAsState
import xyz.teamgravity.bluetoothchat.presentation.navigation.MainNavGraph
import xyz.teamgravity.bluetoothchat.presentation.theme.OldRose
import xyz.teamgravity.bluetoothchat.presentation.theme.Vanilla

@MainNavGraph
@Destination(navArgsDelegate = ChatScreenNavArgs::class)
@Composable
fun ChatScreen(
    state: LazyListState = rememberLazyListState(),
    navigator: DestinationsNavigator,
    viewmodel: ChatViewModel = hiltViewModel(),
) {

    val context = LocalContext.current

    LaunchedEffect(key1 = viewmodel.event) {
        viewmodel.event.collectLatest { event ->
            when (event) {
                is ChatViewModel.ChatEvent.ShowError -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                ChatViewModel.ChatEvent.Finish -> navigator.popBackStack()
            }
        }
    }

    LaunchedEffect(
        key1 = viewmodel.messages.size,
        key2 = keyboardAsState().value
    ) {
        if (viewmodel.messages.isNotEmpty()) state.animateScrollToItem(viewmodel.messages.size - 1)
    }

    BackHandler {
        viewmodel.onDisconnect()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.messages),
                modifier = Modifier.weight(1F)
            )
            IconButton(onClick = viewmodel::onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.disconnect)
                )
            }
        }
        if (viewmodel.connecting) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(text = stringResource(id = R.string.loading))
            }
        } else {
            LazyColumn(
                state = state,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            ) {
                items(
                    items = viewmodel.messages,
                ) { message ->
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (message.local) 15.dp else 0.dp,
                                        topEnd = 15.dp,
                                        bottomStart = 15.dp,
                                        bottomEnd = if (message.local) 0.dp else 15.dp
                                    )
                                )
                                .background(if (message.local) OldRose else Vanilla)
                                .padding(16.dp)
                                .align(if (message.local) Alignment.CenterEnd else Alignment.CenterStart)
                        ) {
                            Text(
                                text = message.sender,
                                fontSize = 10.sp,
                                color = Color.Black,
                            )
                            Text(
                                text = message.message,
                                color = Color.Black,
                            )
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                TextField(
                    value = viewmodel.message,
                    onValueChange = viewmodel::onMessageChange,
                    placeholder = {
                        Text(text = stringResource(id = R.string.message))
                    },
                    modifier = Modifier.weight(1F)
                )
                IconButton(onClick = viewmodel::onSendMessage) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = stringResource(id = R.string.send_message)
                    )
                }
            }
        }
    }
}

data class ChatScreenNavArgs(
    val device: DeviceModel?,
)