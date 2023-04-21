package xyz.teamgravity.bluetoothchat.presentation.screen.permission

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import xyz.teamgravity.bluetoothchat.R
import xyz.teamgravity.bluetoothchat.core.util.PermissionUtil
import xyz.teamgravity.bluetoothchat.presentation.navigation.MainNavGraph
import xyz.teamgravity.bluetoothchat.presentation.screen.destinations.DeviceListScreenDestination
import xyz.teamgravity.bluetoothchat.presentation.screen.destinations.PermissionScreenDestination

@SuppressLint("MissingPermission")
@MainNavGraph(start = true)
@Destination
@Composable
fun PermissionScreen(
    navigator: DestinationsNavigator,
    viewmodel: PermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val bluetoothPermissionState = rememberMultiplePermissionsState(permissions = PermissionViewModel.BLUETOOTH_PERMISSIONS)
    val locationPermissionState = rememberMultiplePermissionsState(permissions = PermissionViewModel.LOCATION_PERMISSIONS)

    val onEnableBluetooth = remember {
        onEnableBluetooth@{
            if (!PermissionUtil.canBluetoothConnect(context)) return@onEnableBluetooth
            context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    val onLetsChat = remember {
        onLetsChat@{
            if (viewmodel.onDeviceAtLeastS() && !bluetoothPermissionState.allPermissionsGranted) return@onLetsChat
            if (!viewmodel.onDeviceAtLeastS() && !locationPermissionState.allPermissionsGranted) return@onLetsChat
            if (!viewmodel.onDeviceAtLeastS() && !viewmodel.onLocationEnabled()) return@onLetsChat
            if (!viewmodel.onBluetoothEnabled()) return@onLetsChat

            navigator.navigate(
                direction = DeviceListScreenDestination.invoke(),
                builder = {
                    popUpTo(
                        route = PermissionScreenDestination.route,
                        popUpToBuilder = {
                            inclusive = true
                        }
                    )
                }
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        if (viewmodel.onDeviceAtLeastS()) {
            FilledTonalButton(
                onClick = {
                    bluetoothPermissionState.launchMultiplePermissionRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.grant_bluetooth_permissions))
            }
        }
        if (!viewmodel.onDeviceAtLeastS()) {
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    locationPermissionState.launchMultiplePermissionRequest()
                }
            ) {
                Text(text = stringResource(id = R.string.grant_location_permissions))
            }
            Spacer(modifier = Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            ) {
                Text(text = stringResource(id = R.string.enable_location))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        FilledTonalButton(onClick = onEnableBluetooth) {
            Text(text = stringResource(id = R.string.enable_bluetooth))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onLetsChat) {
            Text(text = stringResource(id = R.string.lets_chat))
        }
    }
}