package com.utags.androidpc.CensusHub.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.utags.androidpc.CensusHub.presentation.navigation.AppNavigation
import com.utags.androidpc.CensusHub.presentation.theme.CensusHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val appVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "--"
        } catch (_: Exception) { "--" }

        setContent {
            CensusHubTheme {
                AppNavigation(
                    appVersion = appVersion,
                    onExit = {
                        finishAffinity()
                    }
                )
            }
        }
    }
}
