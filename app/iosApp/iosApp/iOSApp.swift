import SwiftUI
import AppIntents

@main
struct iOSApp: App {
    init() {
        if #available(iOS 16.0, *) {
            // App Shortcut の語句・パラメータを初回起動時にも Siri へ再登録する。
            SpikeShortcuts.updateAppShortcutParameters()
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
