package app.nenelog.android

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import app.nenelog.ui.App
import app.nenelog.ui.model.VoiceUiStateStore
import app.nenelog.android.spike.NursingVoiceService

class MainActivity : ComponentActivity() {

    private lateinit var voiceController: AndroidNursingVoiceSessionController
    private val voiceStateStore = VoiceUiStateStore()
    private var voiceReceiverRegistered = false
    private val voiceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != NursingVoiceService.ACTION_VOICE_STATE_CHANGED) return
            voiceStateStore.update(
                stateCode = intent.getStringExtra(NursingVoiceService.EXTRA_STATE_CODE) ?: "waiting",
                lastTranscript = intent.getStringExtra(NursingVoiceService.EXTRA_TRANSCRIPT),
            )
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED && hasActiveSession()
            ) {
                updateVoiceStartState(voiceController.start())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceController = AndroidNursingVoiceSessionController(this)
        setContent {
            App(
                services = AndroidAppGraph.services(this),
                voiceControlAvailable = true,
                voiceStateStore = voiceStateStore,
                onVoiceSessionStart = ::startVoiceSessionWithPermission,
                onVoiceSessionStop = {
                    voiceController.stop()
                    voiceStateStore.update("waiting")
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(NursingVoiceService.ACTION_VOICE_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(voiceStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(voiceStateReceiver, filter)
        }
        voiceReceiverRegistered = true
        voiceStateStore.update(
            NursingVoiceService.currentUiStateCode(),
            NursingVoiceService.currentUiTranscript(),
        )
    }

    override fun onStop() {
        if (voiceReceiverRegistered) {
            unregisterReceiver(voiceStateReceiver)
            voiceReceiverRegistered = false
        }
        super.onStop()
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
            updateVoiceStartState(voiceController.start())
        } else {
            voiceStateStore.update("failure")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun updateVoiceStartState(started: Boolean) {
        voiceStateStore.update(if (started) "waiting" else "failure")
    }

    private fun hasActiveSession(): Boolean =
        AndroidAppGraph.nursing(this).status(System.currentTimeMillis()).stateCode == "running"
}
