# アカウント確認結果 — Week 1(2026-08-15 実施)

> 確認方法: 登録用Gmailの受信履歴から各サービスの登録・課金証跡を検索(個人メールアドレスは非公開、Claude Code による調査)。
> **Gmail 証跡ベースの推定であり、最終確認は各コンソールへのログインで行うこと。**

## サマリ

| サービス | 状態(証跡ベース) | 根拠 | 必要アクション |
|---|---|---|---|
| **Google Play Console** | ✅ **登録完了(2026-08-15、個人アカウント、$25支払い済)+本人確認承認済み(8/17)** — R1の14日クローズドテスト要件が正式に確定。Account ID: 9061920853706928764 | 8/15 に mameta が新規登録を実施(収益化: Subscriptions+IAP 申告、カテゴリ: None of the above)。8/17 Console 通知「Your identity has been verified successfully」で身分証審査の承認を確認。⚠️ 開発者表示名が「YuyaA」になっている(Console ヘッダ・通知で確認)— ストアに公開される名前のため Settings → Developer profile で「Shinei Kikkawa」等へ要変更 | **検証3タスク(公開の前提)**: ①身分証 = **✅ 8/17 承認済み** ②Android実機確認 = **✅ 8/15 完了(Pixel 8 の Play Console アプリでサインイン済み)** ③電話番号SMS認証 = **✅ 8/18 完了**。**検証3タスク全完了。✅ 8/19 アプリ「Nenelog」作成完了(package: app.nenelog.nenelog、Free、en-US)**。Dashboard で R1 要件を再確認: 製品版アクセス申請には「クローズドテストのリリース公開+**12人以上オプトイン+14日以上実施**」が必要(現在テスター0人)。開発者表示名は当面「YuyaA」のまま据え置き(8/18 mameta判断。ストア公開前に再検討)。**Console での次アクションは AAB ができてから(内部テスト配信、Week 2〜3)** |
| **Apple Developer Program** | ✅ **加入・実機署名環境まで開通**(8/19決済、8/31実機確認) | Xcode AccountsでDeveloper Team(Admin)とCertificates/Identifiers/Profilesの正常状態を確認。Apple Development証明書・自動プロビジョニングでNenelogを署名し、iPhone XSへのインストールと起動に成功 | Week 3: App Store ConnectにNenelogのアプリ枠を作成し、TestFlight配信設定へ進む |
| Supabase | ✅ アカウントあり(org: mameta、無料枠) | 既存プロジェクト(x-kaiwai-watcher, ai-dlc-sample)の pause 通知あり | 新プロジェクト作成のみ。無料枠の7日 pause に注意(開発中は定期アクセスで回避) |
| RevenueCat | ✅ **登録完了+初期設定済み(2026-08-16、Email認証済み)** — プロジェクト「Nenelog」、Entitlement `premium`(REST API ID: entl468b0b02dc)に Monthly/Yearly/Lifetime の3商品を紐付け(現状は Test Store 上のプレースホルダ) | mameta が設定。ウィザードが作った「Nenelog Pro」エンタイトルメントは削除し docs/07 準拠の `premium` に統一 | Week 3: 実ストアアプリ追加+実商品ID(`premium_monthly`/`premium_annual`/`premium_lifetime`)接続+SDK統合 |
| OneSignal | ✅ **登録完了+アプリ枠作成済み(2026-08-16)** — **App ID: `5b82af9f-0109-4554-90e2-7af78fc99d63`** | mameta がサインアップ。SDK 導入ステップは「Skip — I'll set this up later」でスキップ | SDK 統合は Week 3(docs/08)。チャネルは Push + In-App のみ |
| Sentry | ✅ **登録完了(2026-08-16、org: nenelog、Data Storage: US)** | mameta がサインアップ(Google 連携)。Business トライアル14日は放置で無料プランに自動降格(カード未登録につき課金なし) | プロジェクト作成は実装時(Android/iOS 各1) |
| Galaxy Seller Portal | ⚠️ **Commercial Seller 申請は却下(8/17)** — 理由「Please apply as a Corporate Commercial seller」= **個人(Private Seller)は Commercial 不可、法人のみ**。Private Seller 登録自体は有効 | 8/17 の Seller Portal通知メールで確認。Seller DeepLink ID確認済み(値は非公開)。PayPal 設定済み | **8/18 決定: Private Seller のまま無料構成で Galaxy 提出**。公式ルール上、Galaxy 賞(Best App for Galaxy)は「Galaxy Store 公開+Galaxy 最適化(フォルダブル対応等)」が評価軸で課金不要。賞品は非金銭(Times Square 掲載・Galaxy Store フィーチャー3週間等)。Galaxy 版はペイウォール・課金導線を外した構成にする(docs/16 R5 更新済み) |

## R1(Play 新規個人アカウントのクローズドテスト要件)への影響

- **8/15 確定: 作成日 2026-08-15 の新規個人アカウント** → 製品版公開前に **12人以上のテスターで14日間のクローズドテスト** が必須(要件適用が確定)。
- 9/20 公開 ← 審査バッファ ← **9/8 クローズドテスト開始が逆算デッドライン**(docs/12)。
- ~~残る不確定要素は本人確認(身分証)の所要日数と電話番号のSMS認証~~ → **8/17 更新: 本人確認は承認済み。残りは電話番号SMS認証のみ**。これが完了するまで Console でアプリ自体が作成できない(=クローズドテスト開始の前提)。所要は数分なので即実施すれば R1 スケジュールへの影響なし。

## mameta の即日 TODO(Claude Code では代行不可)

1. [x] ~~play.google.com/console にログイン → 確認・登録~~ → **8/15 登録完了、8/17 本人確認承認。残タスク = ①電話番号SMS認証(アプリ作成のブロッカー)②開発者表示名「YuyaA」の変更**
2. [x] ~~developer.apple.com → Program 加入~~ → **✅ 8/31 Team・証明書・実機署名・iPhone起動まで確認完了**
3. [x] ~~Galaxy Seller Portal の Commercial Seller 登録開始~~ → **8/15 完了: Private Seller 登録+Commercial Seller 申請提出(審査中)。PayPal 日本アカウントも新規作成し受取口座に設定済み。残タスク = 承認メール確認のみ**
4. [x] ~~RevenueCat / OneSignal / Sentry のアカウント作成~~ → **8/16 完了(3サービスとも登録済み、詳細は上表)**
5. [x] ~~Xcode を App Store からインストール~~ → **✅ 8/18 確認完了: Xcode 26.6(Build 17F113)+ iOS 26.5 シミュレータランタイム導入済み(`xcodebuild -version` で確認)**
6. [x] ~~Apple Developer Program 加入~~ → **✅ 完了(上記2に統合)**

確認結果(作成日・種別)が判明したら本ファイルと `docs/16-risks.md` R1 を更新すること。
