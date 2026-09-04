# 03. 技術選定 — スタックと根拠(2026-08-15 裏取り済み)

> 裏取り調査の生データは `research/tech-verification.md`。バージョンは着手時に最新を再確認すること。

## 結論(スタック一覧)

| レイヤー | 選定 | バージョン目安(2026-08) | 根拠 |
|---|---|---|---|
| 共有言語/基盤 | **Kotlin Multiplatform** | Kotlin 2.x 最新stable | JetBrains賞の必須条件+実利(下記) |
| UI | **Compose Multiplatform**(iOS/Android共通) | 1.11.x(iOSは1.8.0でstable宣言済) | 画面UIを一度で書く。Feres/Physics Wallah等の大規模採用実績 |
| 音声レイヤー | **expect/actual でネイティブ実装**(Swift/Kotlin) | — | 認識/TTS/バックグラウンドはOS固有。ここがKMP賞の見せ場 |
| iOS音声認識 | SFSpeechRecognizer(iOS 26+なら SpeechAnalyzer/SpeechTranscriber を優先採用検討) | iOS 26でja_JP含む39ロケール(開発者実測情報) | オンデバイス・無料・低遅延 |
| Android音声認識 | SpeechRecognizer + `EXTRA_PREFER_OFFLINE` | — | **オンデバイス日本語対応は端末依存で保証なし**(裏取り結果)。初回起動時に実機検出→不可端末はオンライン認識にフォールバック(設定で明示) |
| TTS | AVSpeechSynthesizer / Android TextToSpeech | OS標準 | 無料・オフライン・低遅延。ElevenLabs等は使わない(深夜にネットワーク依存させない) |
| ローカルDB | SQLDelight | 2.x | KMP標準。共通コードから型安全 |
| バックエンド | **Supabase**(supabase-kt) | supabase-kt 3.x | KMP公式対応クライアント。Auth/Postgres+RLS/Realtime/Edge Functions |
| 課金 | **RevenueCat purchases-kmp** | **v3.5.0**(stable、iOS 5.71+/Android 10.4+をラップ) | Shipaton必須要件。**Paywalls UIもCMP対応**(`purchases-kmp-ui`、複数ページは3.4.0+) |
| 通知 | **OneSignal ネイティブSDK×2** | Android 5.9.8 / iOS 5.5.6 | **KMP版は存在しない**(裏取り済)。expect/actualで各OSネイティブSDKを統合。iOSはSPMで `OneSignalFramework`+NSE用 `OneSignalExtension` |
| グロース計測 | Layers SDK | — | Growth Loop賞要件。導入は P1 |
| AIメモ構造化 | Claude Haiku系(Supabase Edge Function経由) | 実装時に `claude-api` スキルで最新モデルID・料金を確認 | クライアントにAPIキーを置かない。P1機能 |
| Web(LP+課金) | RevenueCat Funnels + RevenueCat Billing(Stripe接続) | — | Funnel Vision賞要件。P1 |
| CI/CD | GitHub Actions + Fastlane | — | TestFlight/内部テスト配信の自動化 |

## なぜ KMP + Compose Multiplatform か(Flutter/ネイティブ2本との比較)

1. **JetBrains賞の対象になる**(Flutterでは対象外)。競技的には決定打。
2. 実利でも成立する: 課金(purchases-kmp)・DB(SQLDelight)・バックエンド(supabase-kt)まで**公式/準公式のKMP対応が揃っている**ことを裏取り済み。2ヶ月で2プラットフォーム完成品を出すには共通化が必須で、ネイティブ2本は工数的に不可能。
3. 音声レイヤーはどのみちネイティブ必須(Flutterでも同じ)。KMPは「共通はKotlin・固有はexpect/actualで各OS言語」という構造がフレームワークの思想そのものなので、音声アプリと相性が良い。

## アーキテクチャ上の重要な裏取り結果と対応方針

### 1. iOS拡張ターゲットとKMPの関係(要注意)
- 共有Kotlinフレームワークは**メインアプリターゲットにリンクする**のが公式パターン。Widget/Live Activities の Extension ターゲットからの利用は公式サポートなし。
- **App Intents は iOS 16+ ではメインアプリターゲット内に定義できる** → App Intents(Siri連携=L3音声)からは共有Kotlinコードを直接呼べる。**本アプリの核であるSiri連携に技術的障害はない。**
- Widget / Live Activities(Extension必須)は、App Group 経由のスナップショット(JSON/共有UserDefaults)を読む薄いSwift実装にする。ロジックは持たせない。

### 2. Compose Multiplatform の日本語TextField問題(既知の制約)
- iOSのCMPでは日本語IME(変換候補非表示等)の既知Issueがある(#3416)。1.11.0の新ネイティブテキスト入力実装は実験的段階。
- 対応: 本アプリは**テキスト入力が極端に少ない**(子の名前・メモ程度。メモは音声が主)。数少ない入力箇所は、問題が残っていれば UIKitView で UITextField をラップして回避する。実装初週に実機で検証するタスクを `12-schedule.md` に入れてある。

### 3. Android オンデバイス日本語認識は保証がない
- 公式ドキュメントに言語リストなし(裏取りで確定)。Pixel系はオフライン日本語が動く実績が多いが、端末依存。
- 対応: 初回セットアップで実機の認識能力を検出し、(a) オンデバイス可→そのまま、(b) 不可→OSのオンライン認識で動作+設定画面に明示。プライバシー文言は「音声を**当社サーバーに**送信・保存しません」で統一(過大表現しない)。
- 最悪ケースの保険として Vosk(オフラインOSS認識、小型日本語モデル)の組込みを P2 候補としてメモ(P0では過剰)。

### 4. RevenueCat ネイティブ最新(参考)
- purchases-ios v5.83.x / purchases-android v10.16.x(2026-08-13時点)。purchases-kmp 3.5.0 が内包するバージョンとの乖離に注意。Android最小APIは23(Android 6)。

## リポジトリ構成(モジュール)

```
lullalog/
  app/                     # このディレクトリ直下にKMPプロジェクトを作る
    composeApp/            # 共通UI (Compose Multiplatform)
    shared/                # ドメイン・データ・音声抽象
      domain/              #   イベントモデル・タイマーステートマシン・コマンド解釈(純Kotlin・テスト厚め)
      data/                #   SQLDelight・Supabase同期・RevenueCat
      voice/               #   VoiceEngine expect宣言(認識/TTS/セッション制御の抽象)
    iosApp/                # Xcodeプロジェクト。VoiceEngine actual(Swift)、App Intents、Widget/LiveActivity Extension
    androidApp/            # VoiceEngine actual(Kotlin/Android)、FGS、クイック設定タイル、ウィジェット
  backend/                 # Supabase(マイグレーション・RLS・Edge Functions)
  web/                     # LP + RevenueCat Funnels 設定メモ
  docs/ research/ marketing/
```

## 開発環境前提

- macOS(本機)+ Xcode 最新 / Android Studio 最新 + JetBrains Fleet or AS の KMP プラグイン
- 実機: ユーザーのiPhoneとGoogle Pixel(**Pixelがあるので Android 音声検証は初日から可能**)
- Apple Developer Program / Google Play Console は既存アカウント確認、なければ**今週中に登録**(Play の新規個人アカウントはクローズドテスト要件(12人×14日)がある点に注意 → `13-store-release.md` の最重要リスク)
