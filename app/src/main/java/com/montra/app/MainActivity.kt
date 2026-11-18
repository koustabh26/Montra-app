package com.montra.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.montra.app.ui.AppRoot
import com.montra.app.ui.theme.MontraTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applySecureFlag()

        setContent {
            MontraTheme {
                val backupLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri -> if (uri != null) vm.exportBackup(uri) }

                val restoreLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri -> if (uri != null) vm.importBackup(uri) }

                val csvLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/csv")
                ) { uri -> if (uri != null) vm.exportCsv(uri) }

                // Re-apply the screenshot-protection flag whenever the setting changes.
                LaunchedEffect(vm.secureScreen) { applySecureFlag() }

                AppRoot(
                    vm = vm,
                    onBackup = {
                        vm.expectExternalScreen()
                        backupLauncher.launch("montra-backup-${LocalDate.now()}.json")
                    },
                    onRestore = {
                        vm.expectExternalScreen()
                        restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                    },
                    onExportCsv = {
                        vm.expectExternalScreen()
                        csvLauncher.launch("montra-transactions-${LocalDate.now()}.csv")
                    }
                )
            }
        }
    }

    private fun applySecureFlag() {
        if (vm.secureScreen) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onForeground()
    }

    override fun onStop() {
        super.onStop()
        // Relock whenever the app leaves the foreground (except for our own file pickers).
        if (!isChangingConfigurations) vm.onBackground()
    }
}
