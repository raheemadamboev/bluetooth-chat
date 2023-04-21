package xyz.teamgravity.bluetoothchat.presentation.navigation

import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.DestinationsNavHost
import xyz.teamgravity.bluetoothchat.presentation.screen.NavGraphs

@Composable
fun Navigation() {
    DestinationsNavHost(navGraph = NavGraphs.main)
}