package com.inialpha.executiveai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.inialpha.executiveai.ui.ExecutiveAIApp
import com.inialpha.executiveai.ui.theme.ExecutiveAITheme

class MainActivity : ComponentActivity() {

    // GoogleAuthManager registers this style of launcher too — both must be created here,
    // before the Activity reaches STARTED.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op: Settings surfaces the current state either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as ExecutiveAIApplication
        app.container.attachActivity(this)

        requestNotificationPermissionIfNeeded()

        setContent {
            ExecutiveAITheme {
                ExecutiveAIApp()
            }
        }
    }

    override fun onDestroy() {
        (application as ExecutiveAIApplication).container.detachActivity()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
