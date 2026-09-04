// Week 1 技術スパイクから昇格したP0 App Intents実装。
//
// 検証項目(docs/03 の裏取り結果の実機確認):
//   (a) メインアプリターゲット内に定義した AppIntent から KMP 共有フレームワーク
//       (import ComposeApp)の Kotlin クラスを直接呼べるか
//   (b) IntentDialog の文字列を Siri が読み上げるか(=L3 の voice-out)
//   (c) 「Hey Siri, <アプリ名>で右スタート」フレーズの認識(AppShortcutsProvider)
//
// app/iosApp の iosApp ターゲットへ直接追加済み。
// ショートカット appでも Intent 単体テスト可。結果は research/spike-results.md へ転記。

import AppIntents
import Foundation
import ComposeApp // KMP 共有フレームワーク(:composeApp が生成)

@available(iOS 16.0, *)
struct StartNursingIntent: AppIntent {
    static let title: LocalizedStringResource = "Start Nursing Timer"
    static let description = IntentDescription("Starts the nursing timer without opening the app.")
    // アプリを起動せずバックグラウンドで実行できることが L3 の核
    static let openAppWhenRun = false

    @Parameter(title: "Side")
    var side: SideAppEnum

    init() {
        side = .right
    }

    init(side: SideAppEnum) {
        self.side = side
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        // 共有KotlinサービスがActiveSessionをSQLiteへ永続化する。
        let response = AppIntentDomainBridge().startNursingResponse(
            sideCode: side.rawValue,
            epochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            localeCode: Locale.preferredLanguages.first ?? "en"
        )
        return .result(dialog: IntentDialog(stringLiteral: response))
    }
}

@available(iOS 16.0, *)
struct StopNursingIntent: AppIntent {
    static let title: LocalizedStringResource = "Stop Nursing Timer"
    static let description = IntentDescription("Stops and saves the current nursing timer.")
    static let openAppWhenRun = false

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let response = AppIntentDomainBridge().stopNursingResponse(
            epochMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            localeCode: Locale.preferredLanguages.first ?? "en"
        )
        return .result(dialog: IntentDialog(stringLiteral: response))
    }
}

@available(iOS 16.0, *)
enum SideAppEnum: String, AppEnum {
    case left, right

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Side"
    static let caseDisplayRepresentations: [SideAppEnum: DisplayRepresentation] = [
        .left: "左", .right: "右",
    ]
}

@available(iOS 16.0, *)
struct SpikeShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: StartNursingIntent(),
            phrases: [
                // (c) アプリ名+sideパラメータ。例:「ネネログで右スタート」
                "\(.applicationName)で\(\.$side)スタート",
                "Start \(\.$side) on \(.applicationName)",
            ],
            shortTitle: "Start Nursing",
            systemImageName: "timer"
        )
        AppShortcut(
            intent: StopNursingIntent(),
            phrases: [
                "\(.applicationName)でストップ",
                "Stop nursing on \(.applicationName)",
            ],
            shortTitle: "Stop Nursing",
            systemImageName: "stop.circle"
        )
    }
}
