import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    @ObservedObject var voiceEngine: IosNursingVoiceEngine

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewControllerWithVoiceCallbacks(
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

    var body: some View {
        ComposeView(voiceEngine: voiceEngine)
            .ignoresSafeArea()
    }
}
