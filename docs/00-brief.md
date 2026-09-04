# 00. プロジェクトブリーフ — Nenelog

> 1ページで全体像を掴むためのドキュメント。詳細は各docsへ。

## 何を作るか

**世界初の「voice-in / voice-out で完結する授乳・育児記録アプリ」。**

- 「右スタート」→ 右の授乳タイマーが走る。「ストップ」→ 停止して記録。
- 「今何分?」→ **アプリが声で答える**(「右side、12分です」)。ここが世界的空白。
- うんち・おしっこ・ミルク・睡眠・自由メモも声だけで記録。
- 画面を見ない・触らない・スマートスピーカー不要。深夜の暗い部屋で、赤ちゃんを抱いたまま使える。

キャッチコピー(案):
- 日本語: **「画面を見ない育児記録。声で記録、声が答える。」**
- 英語: **"The hands-free baby tracker that talks back."**

## なぜ作るか(検証済みの市場空白)

2026-08 実施の競合調査(国内・海外、詳細は `research/competitive-analysis.md`)の結論:

1. 国内: ぴよログの音声授乳タイマー(2021, 世界初)は **iOS限定・読み上げなし・画面表示前提**。Android には音声タイマー自体が存在しない。
2. 海外: 音声入力対応は普及(Nara Baby, Huckleberry, Robin Baby, EasyBaby 等)したが、**TTS読み上げ(voice-out)を実装したスマホ単体アプリはゼロ**。読み上げがあるのは Alexa スキル(Echo デバイス前提)のみ。
3. → **「スマホ単体 voice-in/voice-out 完結」は国内外どこにも存在しない**。

## 提出先

**RevenueCat Shipaton 2026** (https://revenuecat-shipaton-2026.devpost.com/)

- 提出期限: **2026-10-01 15:45 JST**(提出窓: 8/1〜9/30)
- 必須要件: 新規アプリを App Store / Google Play / Galaxy Store に期間内に初公開、RevenueCat SDK で IAP 1つ以上、2分デモ動画、審査員用プロモコード
- 実質的なストア提出デッドライン: **2026-09-20**(審査リジェクトのバッファ込み)
- 狙う賞と戦略: `02-award-strategy.md`

## 開発者・発信

- 開発: mameta(個人)。このリポジトリで Claude Code が実装を主導する。
- #BuildInPublic 発信: **@jakeee_ai(じゃけえ)** — bio「子育て以外なんでも自動化する0歳児パパ」。「子育て以外なんでも自動化してきたパパが、ついに育児記録を自動化するアプリを自分の子のために作る」という物語で発信する。運用基盤は `~/dev/sns/x-auto-account/`。詳細は `11-buildinpublic.md`。

## ドキュメントマップ

| # | ファイル | 内容 |
|---|---|---|
| 00 | 本書 | 全体像 |
| 01 | product-spec | 機能仕様・ペルソナ・スコープ階層 |
| 02 | award-strategy | Shipaton 各賞への対応戦略 |
| 03 | tech-stack | 技術選定と根拠 |
| 04 | architecture | モジュール構成・データフロー |
| 05 | voice-ux | 音声コマンド文法・対話フロー・TTS台本 |
| 06 | data-model | エンティティ・DB・同期 |
| 07 | monetization | RevenueCat 課金設計・HAMM戦略 |
| 08 | notifications | OneSignal 通知設計 |
| 09 | design-system | ブランド・UI・モーション |
| 10 | platform-integrations | Siri/App Intents・FGS・ウィジェット・Galaxy対応 |
| 11 | buildinpublic | じゃけえ発信計画 |
| 12 | schedule | 週次スケジュール(8/15→10/1) |
| 13 | store-release | 3ストア申請・審査リスク対策 |
| 14 | demo-video | 2分デモ動画の絵コンテ |
| 15 | naming | 正式名称の決定プロセス |
| 16 | risks | リスク台帳 |

## 絶対に守る原則

1. **締切は動かない。品質の作り込みはスコープ階層(P0/P1/P2)の中でやる。** P0 が 9/20 にストアに出ていることが最優先。
2. **音声体験がプロダクトの心臓。** 音声が不安定なら全ての賞が消える。実機テストを最優先する。
3. **英語ファースト+日本語完全対応。** 審査員は英語圏。UI・音声コマンド・ストア文言・デモ動画すべて英日両対応。
4. **発信は開発と同時進行。** #BuildInPublic は後から遡れない。
