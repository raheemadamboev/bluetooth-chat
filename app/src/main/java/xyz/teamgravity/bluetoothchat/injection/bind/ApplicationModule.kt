package xyz.teamgravity.bluetoothchat.injection.bind

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import xyz.teamgravity.bluetoothchat.data.controller.AndroidBluetoothController
import xyz.teamgravity.bluetoothchat.domain.controller.BluetoothController
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApplicationModule {

    @Binds
    @Singleton
    abstract fun bindTimberTree(timberDebugTree: Timber.DebugTree): Timber.Tree

    @Binds
    @Singleton
    abstract fun bindBluetoothController(androidBluetoothController: AndroidBluetoothController): BluetoothController
}