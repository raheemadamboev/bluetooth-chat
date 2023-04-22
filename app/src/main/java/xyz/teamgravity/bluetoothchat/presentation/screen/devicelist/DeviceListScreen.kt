package xyz.teamgravity.bluetoothchat.presentation.screen.devicelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.flow.collectLatest
import xyz.teamgravity.bluetoothchat.R
import xyz.teamgravity.bluetoothchat.presentation.navigation.MainNavGraph
import xyz.teamgravity.bluetoothchat.presentation.screen.destinations.ChatScreenDestination

@MainNavGraph
@Destination
@Composable
fun DeviceListScreen(
    navigator: DestinationsNavigator,
    viewmodel: DeviceListViewModel = hiltViewModel(),
) {

    LaunchedEffect(key1 = viewmodel.event) {
        viewmodel.event.collectLatest { event ->
            when (event) {
                is DeviceListViewModel.DeviceListEvent.NavigateChat -> navigator.navigate(ChatScreenDestination(event.device))
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            item {
                Text(
                    text = stringResource(id = R.string.paired_devices),
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(
                items = viewmodel.pairedDevices
            ) { device ->
                Text(
                    text = device.name ?: stringResource(id = R.string.no_name),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewmodel.onConnect(device)
                        }
                        .padding(16.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Button(
                onClick = viewmodel::onRefresh,
                modifier = Modifier.weight(1F)
            ) {
                Text(text = stringResource(id = R.string.refresh))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = viewmodel::onStartServer,
                modifier = Modifier.weight(1F)
            ) {
                Text(text = stringResource(id = R.string.start_server))
            }
        }
    }
}