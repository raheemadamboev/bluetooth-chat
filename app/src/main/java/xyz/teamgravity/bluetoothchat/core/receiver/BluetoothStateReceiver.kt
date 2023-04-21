package xyz.teamgravity.bluetoothchat.core.receiver

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import xyz.teamgravity.bluetoothchat.core.extension.getParcelableCompat

class BluetoothStateReceiver(
    private val onChanged: (connected: Boolean, device: BluetoothDevice) -> Unit,
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val device = intent?.getParcelableCompat<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        when (intent?.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (device != null) onChanged(true, device)
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                if (device != null) onChanged(false, device)
            }

            else -> Unit
        }
    }
}