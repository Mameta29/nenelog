package app.nenelog.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.nenelog.integration.iosServices
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App(services = iosServices())
}

fun MainViewControllerWithVoiceCallbacks(
    voiceStateStore: app.nenelog.ui.model.VoiceUiStateStore,
    onVoiceSessionStart: () -> Unit,
    onVoiceSessionStop: () -> Unit,
): UIViewController = ComposeUIViewController {
    App(
        services = iosServices(),
        voiceControlAvailable = true,
        voiceStateStore = voiceStateStore,
        onVoiceSessionStart = onVoiceSessionStart,
        onVoiceSessionStop = onVoiceSessionStop,
    )
}
