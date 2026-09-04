import UIKit
import SwiftUI
import ComposeApp

private final class VoiceUiBridge: ObservableObject {
    let store = VoiceUiStateStore()
}

struct ComposeView: UIViewControllerRepresentable {
    @ObservedObject var voiceEngine: IosNursingVoiceEngine
    let voiceStateStore: VoiceUiStateStore

    func makeUIViewController(context: Context) -> UIViewController {
        voiceEngine.onUiStateChange = { stateCode, transcript in
            voiceStateStore.update(stateCode: stateCode, lastTranscript: transcript)
        }
        return MainViewControllerKt.MainViewControllerWithVoiceCallbacks(
            voiceStateStore: voiceStateStore,
            onVoiceSessionStart: {
                voiceEngine.startLoop(localeIdentifier: Locale.current.identifier)
            },
            onVoiceSessionStop: {
                voiceEngine.stop()
            }
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @StateObject private var voiceEngine = IosNursingVoiceEngine()
    @StateObject private var voiceUiBridge = VoiceUiBridge()

    var body: some View {
        ComposeView(voiceEngine: voiceEngine, voiceStateStore: voiceUiBridge.store)
            .ignoresSafeArea()
    }
}
