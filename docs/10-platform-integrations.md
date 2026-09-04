# 10. プラットフォーム統合 — Siri/App Intents・FGS・ウィジェット・Galaxy

## iOS

### App Intents(L3音声の実装、最重要)
- iOS 16+ の App Intents をメインアプリターゲットに Swift で定義(**メインターゲットなので共有Kotlinフレームワークを直接呼べる** — 裏取り済み)。
- 定義するインテント:
  | Intent | フレーズ例(App Shortcuts で事前登録) | 応答(IntentDialog=Siriが読み上げ) |
  |---|---|---|
  | StartNursingIntent(side) | 「ネネログで右スタート」/ "start right side on Nenelog" | 「右、スタート」 |
  | StopNursingIntent | 「ネネログでストップ」 | 「右10分、左8分を記録しました」 |
  | QueryTimerIntent | 「ネネログで今何分?」 | 「右side、12分です」 |
  | LogEventIntent(type, amount) | 「ネネログでミルク80」「ネネログでうんち」 | 「記録しました」 |
  | LastFeedingIntent | 「ネネログで前回の授乳」 | 「2時間15分前、左10分右5分です」 |
- App Shortcuts のフレーズは日英両方を `AppShortcutsProvider` に登録(ローカライズ済フレーズ)。
- 注意: フレーズにはアプリ名が含まれる必要がある(iOSの仕様)。アプリ名が短く発音しやすいことが重要 → `15-naming.md` の選定基準に反映済み。
- インテントはイベント書込み+応答文生成のみ(タイマー=開始時刻の記録、なのでプロセス生存に依存しない。`04` 参照)。

### Background Modes(L2音声)
- `audio` バックグラウンドモード+AVAudioSession(playAndRecord)。セッション中のみ有効化し、終了時に確実に解放(審査対策: 常時録音アプリと誤解されない実装・説明)。
- Info.plist 用途文言(審査重要): NSMicrophoneUsageDescription /NSSpeechRecognitionUsageDescription は「授乳セッション中の音声コマンド認識のためだけに使用。録音データの保存・送信はしません」と明記(日英)。

### Widget / Live Activities(Extension、P1)
- Extension からは共有Kotlin不可(裏取り済)→ App Group の共有ストレージにスナップショットJSONを書き、SwiftUIで表示するだけの薄い実装。
- Live Activities: 授乳タイマー進行中に Dynamic Island / ロック画面に経過時間表示。「声で操作+目視はロック画面」の組み合わせは体験の完成度を大きく上げる。

## Android

### Google Assistant / App Actions(L3相当の扱い)

- **P0では採用しない。AndroidはL2で代替する**(`01-product-spec.md`)。
- 2026-09-01にGoogle公式仕様を再確認。App Actionsは`shortcuts.xml`のCapabilityを
  Google AssistantがAndroid Intentへ変換し、原則としてActivity・Deep Link・Widgetを
  起動する仕組みで、iOS App Intentsのような任意のバックグラウンド処理+`IntentDialog`と
  同一ではない。
- 授乳開始に一致するBuilt-in Intent(BII)はなく、Custom Intentが必要。しかしCustom Intentは
  **`en-US`のみ**で、端末とAssistantの言語も一致が必要。日本語の
  「Hey Google、ネネログで右スタート」はP0品質として保証できない。
- Android 16+の新しい**AppFunctions(Android MCP)**は、アプリの処理を
  Gemini等のエージェントから発見・実行させるため、将来的にはSiri L3に近い本命候補。
  ただし2026-09-01時点でAPIは実験的プレビュー、Gemini連携はtrusted testers向け
  private previewであり、一般ユーザー向けP0経路には使えない。公開後にP2で再評価する。
- Pixelの日本語ハンズフリー経路は、アプリ前面でL2セッションを一度開始した後、
  Foreground Serviceで画面オフ連続認識し、アプリのTTSで応答する方式とする。実機評価ビルドは
  Vosk日本語smallの固定文法+3倍入力を優先し、モデル利用不可時はSpeechRecognizerへ戻す。
  これはPixel 8実機で機内モード・画面オフ5分08秒まで合格済み
  (`research/spike-results.md`)。
- 英語`en-US`向けCustom App Action+Widget TTS、および公開後のAppFunctionsはP2候補。
  P0の9/20公開を脅かさない範囲でのみ検証する。

### Foreground Service(L2音声)
- `foregroundServiceType="microphone"`。**サービス開始はアプリ前面時のみ可**(Android 11+)→ 開始導線: アプリ内ボタン/クイック設定タイル/ウィジェット。
- 常駐通知: 「🎙 授乳セッション中 右12分」(通知自体がミニダッシュボード。停止ボタン付き)。
- 画面オフ対応: PARTIAL_WAKE_LOCK+認識エンジン再起動ループ。Doze対策としてセッション中はバッテリー最適化の例外を案内(設定画面から、強制はしない)。

### クイック設定タイル・ウィジェット
- QSタイル「Nenelog セッション」: 通知シェードから1タップでL2開始(画面ロック中でも到達が速い)。
- Glanceウィジェット: 前回授乳からの経過+ワンタップ記録ボタン4つ。

## Samsung Galaxy Store / フォルダブル(P1)

- Seller Portal 登録は無料。ただし**商用配信には Commercial Seller Status(身分証+D-U-N-S/DBA+金融情報)が必要で最大10営業日**、かつ**日本の個人開発者の可否が公開情報で未確定**(裏取り済)→ **8月中に登録を試みて可否を確定させる**(`12-schedule.md` Week1タスク・`16-risks.md`)。
- 収益配分はサブスク85%(参考)。ビルドは Play 版と同一AAB/APK(課金はGalaxy Store課金ではなくRevenueCat経由のGoogle Play Billingが使えないため、**Galaxy Store版の課金方式は要調査** — 実装前に RevenueCat の Galaxy Store 対応状況を確認すること。非対応なら Galaxy 版は機能制限なし無料+Play誘導は規約違反の恐れがあるため、Samsung IAP対応の要否を判断)。
- フォルダブル対応: CMPの WindowSizeClass で2ペイン(タイムライン+サマリ)。Flex mode(半開き)でタイマー画面を上半分に。

## 権限取得UX(共通)

- マイク・音声認識・通知の許可は**必要になる文脈で1つずつ**(オンボーディングで一括要求しない)。
- 初回音声体験はオンボーディング内のデモ(許可→即成功体験)で完結させる。拒否時も全機能がタップで使えることを明示。
