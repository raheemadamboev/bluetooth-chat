package xyz.teamgravity.bluetoothchat.core.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import xyz.teamgravity.bluetoothchat.core.extension.getParcelableCompat

class BluetoothFoundReceiver(
    private val onDeviceFound: (device: BluetoothDevice) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val device = intent.getParcelableCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let(onDeviceFound)
            }

            else -> Unit
        }
    }
}