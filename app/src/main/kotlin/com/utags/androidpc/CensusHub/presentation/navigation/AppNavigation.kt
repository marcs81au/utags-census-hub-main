package com.utags.androidpc.CensusHub.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utags.androidpc.CensusHub.presentation.connect.ConnectScreen
import com.utags.androidpc.CensusHub.presentation.scan.ScanScreen
import com.utags.androidpc.CensusHub.presentation.settings.SettingsScreen
import com.utags.androidpc.CensusHub.presentation.splash.SplashScreen
import com.utags.androidpc.CensusHub.presentation.write.WriteScreen

object Routes {
    const val SPLASH   = "splash"
    const val CONNECT  = "connect"
    const val SCAN     = "scan"
    const val SETTINGS = "settings"
    const val WRITE    = "write"
}

@Composable
fun AppNavigation(
    appVersion: String,
    onExit: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    // Shared antenna number state — set by Settings, used by Scan and Write
    var antennaNo by remember { mutableStateOf(3) } // default ant1+ant2

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CONNECT) {
            ConnectScreen(
                appVersion = appVersion,
                onConnected = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.CONNECT) { inclusive = false }
                    }
                },
                onExit = onExit
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onBack = {
                    navController.navigate(Routes.CONNECT) {
                        popUpTo(Routes.CONNECT) { inclusive = false }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onNavigateToWrite = {
                    navController.navigate(Routes.WRITE)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                appVersion = appVersion,
                onBack = {
                    navController.popBackStack()
                },
                onAntennaNoUpdated = { no ->
                    antennaNo = no
                }
            )
        }
        composable(Routes.WRITE) {
            WriteScreen(
                antennaNo = antennaNo,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
