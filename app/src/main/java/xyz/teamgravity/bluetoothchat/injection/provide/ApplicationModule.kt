package xyz.teamgravity.bluetoothchat.injection.provide

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.location.LocationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import xyz.teamgravity.bluetoothchat.data.controller.AndroidBluetoothController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationModule {

    @Provides
    @Singleton
    fun provideTimberDebugTree(): Timber.DebugTree = Timber.DebugTree()

    @Provides
    @Singleton
    fun provideBluetoothManager(application: Application): BluetoothManager = application.getSystemService(BluetoothManager::class.java)

    @Provides
    @Singleton
    fun provideBluetoothAdapter(bluetoothManager: BluetoothManager): BluetoothAdapter? = bluetoothManager.adapter

    @Provides
    @Singleton
    fun provideAndroidBluetoothController(
        application: Application,
        bluetoothAdapter: BluetoothAdapter?,
    ): AndroidBluetoothController = AndroidBluetoothController(
        context = application,
        adapter = bluetoothAdapter
    )

    @Provides
    @Singleton
    fun provideLocationManager(application: Application): LocationManager = application.getSystemService(LocationManager::class.java)
}