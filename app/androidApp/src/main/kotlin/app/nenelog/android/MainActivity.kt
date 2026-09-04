package app.nenelog.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.nenelog.ui.App

class MainActivity : ComponentActivity() {

    private lateinit var voiceController: AndroidNursingVoiceSessionController
    private var voiceListening by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED && hasActiveSession()
            ) {
                voiceListening = voiceController.start()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceController = AndroidNursingVoiceSessionController(this)
        setContent {
            App(
                service = AndroidAppGraph.nursing(this),
                voiceControlAvailable = true,
                voiceListening = voiceListening,
                onVoiceSessionStart = ::startVoiceSessionWithPermission,
                onVoiceSessionStop = {
                    voiceController.stop()
                    voiceListening = false
                },
            )
        }
    }

    private fun startVoiceSessionWithPermission() {
        val missingPermissions = buildList {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.RECORD_AUDIO)
            }
            if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (missingPermissions.isEmpty()) {
            voiceListening = voiceController.start()
        } else {
            voiceListening = false
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun hasActiveSession(): Boolean =
        AndroidAppGraph.nursing(this).status(System.currentTimeMillis()).stateCode == "running"
}
