package xyz.teamgravity.bluetoothchat.presentation.screen.permission

import android.Manifest
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import xyz.teamgravity.bluetoothchat.core.util.BuildUtil
import xyz.teamgravity.bluetoothchat.domain.controller.BluetoothController
import javax.inject.Inject

@HiltViewModel
class PermissionViewModel @Inject constructor(
    private val controller: BluetoothController,
    private val location: LocationManager,
) : ViewModel() {

    companion object {
        val BLUETOOTH_PERMISSIONS = listOf(Manifest.permission.BLUETOOTH_CONNECT)
        val LOCATION_PERMISSIONS = listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val deviceAtLeastS: Boolean = BuildUtil.deviceAtLeastS()

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    fun onDeviceAtLeastS(): Boolean {
        return deviceAtLeastS
    }

    fun onBluetoothEnabled(): Boolean {
        return controller.bluetoothEnabled()
    }

    fun onLocationEnabled(): Boolean {
        return location.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }
}