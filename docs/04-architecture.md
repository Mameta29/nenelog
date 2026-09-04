# 04. アーキテクチャ — モジュール・データフロー・音声レイヤー設計

## 全体像

```
┌─────────────────────────────────────────────────────┐
│ composeApp (Compose Multiplatform, 共通UI)           │
│   Home / Timer / Timeline / Summary / Family / Paywall│
└──────────────┬──────────────────────────────────────┘
               │ ViewModel (共通, MVI風の単方向データフロー)
┌──────────────┴──────────────────────────────────────┐
│ shared/domain (純Kotlin, プラットフォーム依存ゼロ)     │
│   ・NursingSessionStateMachine(タイマーの唯一の真実)  │
│   ・VoiceCommandInterpreter(文字列→Command, 日英)    │
│   ・ResponseComposer(状態→TTS応答文, 日英)          │
│   ・EventStore(追記型イベント, undo)                 │
└──────┬───────────────┬──────────────────┬───────────┘
       │               │                  │
┌──────┴─────┐ ┌───────┴────────┐ ┌──────┴───────────┐
│ shared/data│ │ shared/voice   │ │ shared/billing    │
│ SQLDelight │ │ VoiceEngine    │ │ purchases-kmp     │
│ supabase-kt│ │ (expect)       │ │ + paywalls-ui     │
└────────────┘ └───┬────────┬───┘ └──────────────────┘
                   │        │
        ┌──────────┴──┐  ┌──┴─────────────┐
        │ iosApp      │  │ androidApp      │
        │ Swift actual│  │ Kotlin actual   │
        │ ・SFSpeech/ │  │ ・SpeechRecognizer│
        │  SpeechAnalyzer│ │ ・TextToSpeech  │
        │ ・AVSpeech  │  │ ・FGS(mic type) │
        │ ・AppIntents│  │ ・QS Tile/Widget│
        │ ・Widget/LA │  │ ・OneSignal SDK │
        │ ・OneSignal │  │                 │
        └─────────────┘  └─────────────────┘
```

## 設計の要点

### 1. ドメインを純Kotlinに閉じ込める(テスト戦略の核)
- `NursingSessionStateMachine`: idle → running(L|R) → paused → completed。副作用なし・時刻は注入。**タイマーのバグは信頼を殺すので、ここに単体テストを最も厚く書く**(左右切替・一時停止・90分自動クローズ・undo・プロセス再起動からの復元)。
- `VoiceCommandInterpreter`: 認識文字列(+信頼度)→ `Command` の純関数。同義語辞書・数値抽出(「80ミリ」「36度8分」)・単独発話判定を含む。**日英のテストケースを表形式(パラメタライズド)で網羅**。認識エンジンをモックすれば音声UX全体がJVMテストで回る。
- `ResponseComposer`: `(Command結果, 状態, 音声設定)` → 応答文字列+ハプティクスパターン。台本は `05-voice-ux.md` と1:1対応させ、テストで台本との一致を検証。

### 2. VoiceEngine 抽象(expect/actual 境界)

```kotlin
// shared/voice (expect側 = 共通コードが知っている全て)
interface VoiceEngine {
    val transcripts: Flow<TranscriptChunk>   // 部分/確定認識結果+信頼度
    suspend fun startListening(mode: ListeningMode)  // PUSH_TO_TALK | SESSION
    suspend fun stopListening()
    suspend fun speak(text: String, profile: SpeechProfile) // NORMAL|QUIET|HAPTICS_ONLY
    val state: StateFlow<VoiceEngineState>   // idle/listening/speaking/error(理由付き)
}
```

- **actual(iOS/Swift)**: AVAudioSession(category: playAndRecord, mode: voiceChat, duckOthers)。SFSpeechRecognizer(iOS 26+ は SpeechTranscriber を検出して優先)。speak中は認識タスクを停止→終了後300msで再開。バックグラウンド継続は Background Modes: audio。
- **actual(Android)**: Foreground Service(`foregroundServiceType="microphone"`)。日本語L2の実機評価ビルドはVosk固定文法+入力3倍補正を優先し、モデル利用不可または英語ではSpeechRecognizerへフォールバックする。両経路とも連続認識向けに自動再起動し、TTS発話中は認識停止、画面オフ時はPARTIAL_WAKE_LOCKを使う。Voskの製品採用は生活音・容量評価後に確定する。
- エコー対策は「speak中はマイク停止」で構造的に解決(AECに依存しない)。

### 3. セッションと復元
- ActiveSession はDBに永続化し、プロセスキル・再起動後も復元(タイマーは実時刻差分で計算するので、バックグラウンドでの計測継続に依存しない。**"走っているタイマー"とは開始時刻の記録のこと**)。
- iOS L3(Siri/App Intents)はこの性質に完全に乗る: App Intent は「イベントを書いて応答文を返す」だけ。アプリプロセスの生存に依存しない。

### 4. 同期
- 書込みは常にローカル先行 → OutboxテーブルからSupabaseへ非同期push。Realtime購読で他メンバーの変更をpull。
- 競合はUUIDイベント追記なので原理的に発生しない(削除も打ち消しイベント)。

### 5. 課金ゲート
- `EntitlementGate`(shared/billing): RevenueCat の entitlement `premium` を StateFlow で公開。音声L2/L3・家族3人以上・AI構造化をゲート。
- **オフライン時はキャッシュされた entitlement を信用する**(深夜に「確認できません」で音声が止まるのは最悪の体験)。

## エラー・観測性

- 統一 `AppError`(Voice/ Sync / Billing)+ ユーザー向け文言は ResponseComposer 経由(音声でもエラーを言える)。
- クラッシュ: Firebase Crashlytics ではなく **Sentry KMP** を採用(KMP一次対応)。
- 音声KPIログはローカル集計→日次で匿名メトリクスのみ送信(`05`のKPI)。

## テスト全体方針

| 層 | 手段 | 目標 |
|---|---|---|
| domain | JVM単体テスト(kotest) | ステートマシン・インタープリタ・台本を網羅。カバレッジ90%+ |
| data | SQLDelight in-memory + 同期の結合テスト | outbox/realtime往復 |
| voice actual | 実機手動テストのチェックリスト(`research/device-test-checklist.md` を実装期に作成) | 認識成功率90%+/コマンド |
| UI | AndroidホストのCompose Preview Screenshot Testingで共通CMP UIを検証(主要5画面×ライト/ダーク/ナイト) | デグレ検知 |
| E2E | Maestro(iOS/Android両対応)で記録→タイムライン反映のスモーク | リリース前ゲート |

- 2026-09-04: Quiet Linenの基準画像25件を追加。主要5画面×3テーマに加え、日英、fontScale 200%、空/エラー、音声5状態を網羅し、`validateDebugScreenshotTest`を製品UIの回帰ゲートとする
- SQLDelightは各リリースschemaの`.db`を保存し、`verifySqlDelightMigration`で既存ユーザーDBからの移行を検証する。初回の`1.db`から設定永続化を追加したversion 2への移行は2026-09-04に合格
