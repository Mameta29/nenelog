# 技術裏取り調査(2026-08-15 実施・一次情報ベース)

> `03-tech-stack.md` の根拠となる生データ。着手時にバージョンは再確認すること。

## 1. RevenueCat Kotlin Multiplatform SDK (purchases-kmp)
- 存在する: github.com/RevenueCat/purchases-kmp、最新 v3.5.0
- production-ready(公式ブログで stable 明言。2026/5 に KMP SDK 3.0.0 記事)
  - https://www.revenuecat.com/blog/engineering/how-we-built-the-revenuecat-sdk-for-kotlin-multiplatform
  - https://www.revenuecat.com/blog/engineering/kmp-sdk-3
- iOS/Android両対応。3.0.0時点で purchases-ios 5.71.0 / purchases-android 10.4.0 をラップ
- Paywalls UI: `purchases-kmp-ui` で Compose Multiplatform 上に表示可。複数ページは 3.4.0+
- 注意: docsとREADMEで最小Android APIに差異(21+ vs 23+)

## 2. Compose Multiplatform for iOS
- **1.8.0(2025/5)で iOS stable / production-ready 宣言**
  - https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/
- 最新 1.11.1。1.11.0(2026/5)で並行レンダリングデフォルト化
- 採用実績: Feres(100万+DL)、Markaz(500万+DL)、Physics Wallah(1700万MAU)、Wrike、BiliBili中国版
- 制約:
  - **日本語IME/TextField不具合の既知Issue #3416**(変換候補非表示等)。1.11.0の新ネイティブテキスト入力(UIViewベース)は実験的。実機検証必須
  - アクセシビリティは1.8.0でVoiceOver等対応済み
  - x86_64ターゲット削除済み、Wasm版はBeta

## 3. KMP × iOSネイティブ機能(App Intents / Widget / Live Activities)
- 公式ドキュメントに App Intents/Widget/LA への言及なし。公式統合パターンは共有フレームワークを**メインアプリターゲット**へリンクする方式のみ
- → App Intents(iOS 16+はメインターゲット内定義可)からは共有Kotlin呼び出し可能
- → Widget/LA(Extension必須)への共有コード組込みは公式サポートなし。App Group 経由のデータ受け渡しで対応(非公式実装例: software-mansion-labs/kmp-live-activity)

## 4. OneSignal
- **KMP SDK は存在しない**(公式対応: Android/iOS/Unity/RN/Flutter/Cordova のみ。Issue #2038 は回答なしクローズ)
- Android SDK 最新 5.9.8(2026/7/30)/ iOS SDK 最新 5.5.6(2026/8/1、SPM推奨、OneSignalFramework+OneSignalExtension(NSE))
  - https://documentation.onesignal.com/docs/android-sdk-setup / https://documentation.onesignal.com/docs/ios-sdk-setup

## 5. Samsung Galaxy Store(個人開発者)
- Seller Portal 登録・公開とも無料。商用配信には Commercial Seller Status(政府身分証+D-U-N-S or DBA+金融情報(PayPal推奨))
- 承認: 数日〜、D-U-N-S/銀行検証は最大10営業日
- 収益配分(2025/5/15〜): 有料/IAP 80%、サブスク85%
- **日本の個人開発者可否は公開ページで未確定** → 登録試行で確認するしかない
  - https://developer.samsung.com/galaxy-store/prepare.html / faq.html

## 6. RevenueCat Funnels + Stripe
- Funnels = RevenueCatホストのノーコードWebファネル(オンボーディング/アンケート/チェックアウト)
- RevenueCat Billing(Web SDK)が Stripe を決済ゲートウェイに使用。Stripe接続→Billing設定→商品/オファリング→Apple Pay/GPayドメイン登録
  - https://www.revenuecat.com/docs/tools/funnels / https://www.revenuecat.com/docs/web/revenuecat-billing / https://www.revenuecat.com/docs/web/integrations/stripe
- 注意: Stripe商品インポート後の価格タイプ変更は非サポート。2026/5/1以降の新規設定は税抜表示デフォルト

## 7. Shipatonスポンサー Layers / Noise の実態
- **Layers**(layers.com): グロース実験・コンテンツ生成・広告費最適化・インストール単位アトリビューション・ASO の統合プラットフォーム(ペイウォールA/B専業ではない)
- **Noise**(getnoise.com): 実在の人間UGCクリエイターへの動画制作発注マーケットプレイス。ビュー課金制・初期費用なし

## 8. 音声認識(信頼度に注意 — 実機検証必須)
- iOS SFSpeechRecognizer ja-JP オンデバイス: 開発者実測(iOS 15.4.1)で対応報告。2026年一次情報は未達(信頼度: 中)
- iOS 26 SpeechAnalyzer/SpeechTranscriber: 開発者実測で **ja_JP含む39ロケール**(信頼度: 中、公式リスト未確認)
- Android SpeechRecognizer オンデバイス日本語: **公式の言語リストが存在せず確認不能**(信頼度: 低)。Week1で Pixel 実機検証が必須

## 9. RevenueCat ネイティブSDK最新
- purchases-ios v5.83.2(2026/8/13)/ purchases-android v10.16.2(2026/8/13、v10.0.0で最小API 23)

## 10. Google Assistant App ActionsとPixelのL3可否(2026-09-01再確認)

- Google Assistant App Actionsは`shortcuts.xml`のCapabilityをAndroid Intentへ変換し、
  Activity・Deep Link・Widgetを起動してアプリ機能を実行する仕組み。
  - https://developer.android.com/develop/devices/assistant/overview
  - https://developer.android.com/develop/devices/assistant/action-schema
- 授乳開始に該当するBuilt-in Intent(BII)はないためCustom Intentが必要だが、公式の制約は
  **`en-US`のみ**で、端末とAssistantの言語一致も必須。
  - https://developer.android.com/develop/devices/assistant/custom-intents
- Widget fulfillmentでは開発者指定TTSをAssistantに読ませられるが、元になるBII/Custom Intentの
  ロケール制約は残る。
  - https://developer.android.com/develop/devices/assistant/widgets
- Android 16+のAppFunctionsはアプリをon-device MCP serverとして扱い、Gemini等が
  UIを開かず関数を発見・実行できる将来的なL3候補。しかし2026-09-01時点で
  APIはexperimental preview、Gemini連携はtrusted testers向けprivate preview。P0利用は不可。
  - https://developer.android.com/ai/appfunctions
  - https://developer.android.com/ai/appfunctions/add-appfunctions
- 結論: 日本語PixelでSiriと同じ「Hey Google、ネネログで右スタート」→アプリ非表示実行→
  任意応答をP0品質として保証できない。`docs/01`の確定方針どおりAndroidはL2
  (アプリ内開始後のFGS画面オフ連続認識+アプリTTS)で代替する。`en-US` App Actionsと
  公開後のAppFunctionsはP2。

## 要追加確認(実装セッションのTODO)
- [ ] Galaxy Store 日本個人開発者の登録可否(実際に登録して確認)
- [ ] RevenueCat の Samsung Galaxy Store(Samsung IAP)対応状況
- [ ] iOS/Android 実機での supportedLocales 確認(日英)
- [ ] purchases-kmp 3.5.x が内包するネイティブSDKバージョンと最新の乖離
