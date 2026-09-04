# 12. スケジュール — 2026-08-15 → 10-01(週次ゲート付き)

> 原則: **P0がストアに出ることが全賞の前提条件。** 各週末のゲートで遅延したら、P1の賞(Galaxy/Stripe/Layers/Noise)から切る。P0は切らない。

## Week 1: 8/15(金)〜8/24(日)— 基盤+音声の技術検証

- [ ] アカウント確認(即日): Apple Developer / **Play Console(作成日・個人/組織を確認 → クローズドテスト要件の有無を確定)** / Galaxy Seller Portal 登録開始 / RevenueCat / OneSignal / Supabase / Sentry
- [ ] 正式名称決定(`15-naming.md` のプロセス、〜8/19)→ Bundle ID・ドメイン取得
- [ ] KMPプロジェクト雛形(composeApp/shared/iosApp/androidApp)+CI(GitHub Actions: ビルド+JVMテスト)
- [x] **技術スパイク(この週の本丸)** — 9/1に4件すべて実機合格(`research/spike-results.md`):
  - iOS: SFSpeechRecognizer/SpeechTranscriber 日本語オンデバイス実機確認(supportedLocales)+背景audio下の連続認識
  - Android(Pixel実機): SpeechRecognizer オフライン日本語可否+FGS×画面オフ×連続認識ループ
  - App Intents から共有Kotlin呼び出し+IntentDialog読み上げのPoC
  - CMP日本語TextField実機確認(問題あればUIKitViewラップ方針を確定)
- [x] domain実装開始: EventStore+NursingSessionStateMachine+テスト
- [ ] #BuildInPublic: キックオフ投稿(英日)+市場調査スレッド
- **ゲートG1(8/24)**: 4スパイク全部が実機で動いている。動かないものがあれば `16-risks.md` の代替案に即切替。

## Week 2: 8/25〜8/31 — 音声エンジン+コア記録

- [ ] VoiceEngine actual(iOS/Android)実装、TTS応答、speak中マイク停止(授乳L2の本番導線は9/1実装済み。割込み/AudioFocusと全コマンドは残り)
- [ ] VoiceCommandInterpreter+ResponseComposer(日英、テスト網羅)(授乳の左/右/停止+エッジケースは完了、照会・単発記録は残り)
- [x] SQLDelight スキーマ+記録CRUD+undo
- [x] タイマー画面+ホーム(タイムライン)最小版(実機G2は未完)
- [ ] 発信: 「初めて声でタイマーが動いた」動画
- **G2**: 自分の子で最初のドッグフーディング(L2で1回の授乳を完走)
  - 9/1: Pixel本番導線は日英・画面オフ・完全オフラインで技術完走。OS認識の静音は全体18/30(60%)、Vosk固定文法+700ms起動待機は19/20(95%)。50cm小声は無補正6/10、3倍入力9/10、5倍入力7/10だったため3倍を採用候補に確定。自然な「右/左スタート」を維持して実タイマー画面へ接続し、生活音・誤作動・容量を確認する。R7と実授乳完走が終わるまでゲートは閉じない。
  - 9/3: Pixelの通常ホーム/実タイマーFGSへVosk固定文法+3倍入力を接続。「左→右→左→右→ストップ」5発話を言い直しなしで認識し、TTS・左右各1分の保存・FGS終了まで完走。iPhone XSも通常ホーム版へ更新し、実画面での操作・音声応答を確認。両OSとも診断画面から実製品導線への接続は合格。生活音/背景会話、画面オフ長時間、容量判断と実授乳ドッグフーディングは継続する。

## Week 3: 9/1〜9/7 — 全記録種別+同期+課金

- [ ] 全イベント種別の音声・タップ記録、サマリ画面
- [ ] Supabase 同期+家族共有+招待フロー
- [ ] RevenueCat 統合+ペイウォール(深夜長押し仕様)+EntitlementGate
- [ ] App Intents 5本(L3)+日英フレーズ
- [ ] OneSignal 統合+連携プレー通知
- [x] ナイトモード+デザインシステム適用
  - 9/4: Quiet Linenを共通Compose UIへ実装。主要5画面・全手動記録・空/エラーを実DBへ接続し、Light/Dark/OLED Night、Auto(22:00〜06:00)、日英、音声5状態、48dp、スクリーンリーダー、fontScale 200%、Reduced Motionに対応。JVM/data、SQLDelight移行、Android APK、iOS arm64、基準画像25件の自動ゲート合格。iPhone/Pixelの最終UIスモークのみ継続する
- **G3**: 全P0機能がfeature complete(粗くても通しで動く)

## Week 4: 9/8〜9/14 — 磨き+テスト配信開始(締切連動)

- [ ] **9/8: Android クローズドテスト開始(新規個人アカウントの場合、14日間要件の逆算デッドライン)**+iOS TestFlight 内部→外部
- [ ] アクセシビリティ完全対応(VoiceOver/TalkBack監査)+日英ローカライズ完成
- [ ] モーション・空状態・エラー状態の作り込み(Design Award水準へ)
- [ ] 音声認識成功率の計測→辞書チューニング(目標90%+)
- [ ] ストア素材(アイコン・スクショ・説明文・プライバシーポリシー)
- [ ] 発信: テスター募集(Discord・じゃけえ)、技術記事1本目(KMP×音声、英訳もセット)
- **G4**: テスター10人以上が実使用、クラッシュフリー率99.5%+

## Week 5: 9/15〜9/21 — 審査提出→リリース

- [ ] 9/16 両ストア審査提出(審査ノート・実演動画付き)
- [ ] リジェクト対応バッファ(最大2往復)
- [ ] **9/20 製品版公開(iOS+Android同時)** → リリース告知(英日)
- [ ] P1着手(公開待ち時間で): Live Activities・ウィジェット・AIメモ構造化・Galaxy申請・Stripe Funnel・Layers導入
- **G5**: 両ストアで公開済み。未公開ならP1全停止で審査対応に全振り

## Week 6: 9/22〜9/30 — グロース+提出物

- [ ] ローンチグロース: ぴよログ比較記事・ProductHunt・Noise発注動画・毎日の数字公開
- [ ] v1.1(P1機能)提出(9/26まで)
- [ ] デモ動画撮影・編集(`14-demo-video.md`、9/27まで)
- [ ] Devpost 提出文(英語、賞ごとの段落)+発信ハイライト集+成長数値レポート
- [ ] **9/29 Devpost 提出完了**(締切10/1 15:45 JSTに対し2日バッファ)

## ゲート運用ルール

- 毎ゲートで判断するのは「P1のどれを切るか」だけ。P0の機能削減は G3 まで禁止、G4 以降は音声の安定性>機能数で判断。
- 各ゲートの結果は `CLAUDE.md` の Decision Log に追記する。
