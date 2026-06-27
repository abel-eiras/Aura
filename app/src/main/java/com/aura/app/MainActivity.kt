package com.aura.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aura.app.ui.main.MainScreen
import com.aura.app.ui.permissions.BatteryOptimizationDialog
import com.aura.app.ui.permissions.PermissionRationaleDialog
import com.aura.app.ui.recordings.RecordingsScreen
import com.aura.app.ui.settings.SettingsScreen
import com.aura.app.ui.theme.AuraTheme
import com.aura.app.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

private object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val RECORDINGS = "recordings"
}

@Composable
private fun AuraApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    var showRationale by remember { mutableStateOf(false) }
    var showBatteryPrompt by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op: RECORD_AUDIO denial just means recording fails silently until granted */ }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* no-op: re-prompted on next cold launch if still not exempted */ }

    LaunchedEffect(Unit) {
        if (requiredPermissions().any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
        ) {
            showRationale = true
        }
        val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
        if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            showBatteryPrompt = true
        }
    }

    if (showRationale) {
        PermissionRationaleDialog(
            onContinue = {
                showRationale = false
                permissionLauncher.launch(requiredPermissions())
            },
            onDismiss = { showRationale = false }
        )
    } else if (showBatteryPrompt) {
        BatteryOptimizationDialog(
            onContinue = {
                showBatteryPrompt = false
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                batteryOptimizationLauncher.launch(intent)
            },
            onDismiss = { showBatteryPrompt = false }
        )
    }

    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onRecordingsClick = { navController.navigate(Routes.RECORDINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.RECORDINGS) {
            RecordingsScreen(onBackClick = { navController.popBackStack() })
        }
    }
}

private fun requiredPermissions(): Array<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.READ_PHONE_STATE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()
