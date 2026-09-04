package app.nenelog.ui

import androidx.compose.ui.window.ComposeUIViewController
import app.nenelog.integration.iosNursingSessionService
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App(iosNursingSessionService())
}

fun MainViewControllerWithVoiceCallbacks(
    onVoiceSessionStart: () -> Unit,
    onVoiceSessionStop: () -> Unit,
): UIViewController = ComposeUIViewController {
    App(
        service = iosNursingSessionService(),
        onVoiceSessionStart = onVoiceSessionStart,
        onVoiceSessionStop = onVoiceSessionStop,
    )
}
