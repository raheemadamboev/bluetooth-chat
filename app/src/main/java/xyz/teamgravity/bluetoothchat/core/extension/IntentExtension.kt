package xyz.teamgravity.bluetoothchat.core.extension

import android.content.Intent
import android.os.Parcelable
import xyz.teamgravity.bluetoothchat.core.util.BuildUtil

@Suppress("DEPRECATION")
inline fun <reified T : Parcelable> Intent.getParcelableCompat(key: String): T? {
    return if (BuildUtil.deviceAtLeastTiramisu()) getParcelableExtra(key, T::class.java)
    else getParcelableExtra(key) as T?
}