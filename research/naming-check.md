# 命名候補の空き状況調査 — Week 1(2026-08-15 実施)

> `docs/15-naming.md` の決定手順ステップ1〜2に相当。調査は Claude Code(Web調査)。
> Siri実機テスト(ステップ3)と最終決定(ステップ4)は mameta が実施。

## 結論(重要)

- **Lullalog(現仮称)は正式名称として使用不可。** 完全同名・同カテゴリの「Lullalog: Baby Tracker」(Rohank Agarwal, Health & Fitness, id6762561885)が App Store で公開中。lullalog.app も同アプリの公式サイトとして稼働中。App Store は同名アプリを登録できない。→ **改名必須**
- 初期4候補で両ストア衝突なしは **Koelog のみ**。ただし koelog.app が「声×ログ」のほぼ同コンセプト日本製音声ジャーナリングアプリのLPとして稼働中で、ブランド衝突・商標出願先行リスクあり。
- → 第2ラウンドの追加候補調査を実施(下記)。

## 第1ラウンド判定表(2026-08-15)

| 候補 | App Store | Play | 商標 | .appドメイン | Xハンドル |
|---|---|---|---|---|---|
| Lullalog | **×** 完全同名アプリ公開中 | △ 近似多数 | △ 未登録だが先使用リスク | **×** 競合が保有 | ?(402で確認不能) |
| Koelog | ○ | ○ | △ 国内に「コエログ」使用例複数(CyberAgent社内ツール等) | **×** 同コンセプトLPが稼働中 | ?(未発見=空きの可能性) |
| Nightfeed | × The Night Feed(夜間授乳支援・英チャート1位) | × 同左+Steamゲーム | △ 一般語+確立ブランドとの混同大 | ○ | × @nightfeed 他全滅 |
| Sasayaki | △ Sasayaki - Voice to Text(音声系)あり | × com.kazami.sasayaki 存在 | ? 一般語 | ○ | ?(一般語で取得済み濃厚) |

主な根拠URL:
- Lullalog: Baby Tracker — https://apps.apple.com/us/app/lullalog-baby-tracker/id6762561885 / https://lullalog.app
- Koelog LP — https://koelog.app (音声ジャーナリング・需要検証段階)
- The Night Feed — https://thenightfeed.uk/
- Sasayaki (Play) — https://play.google.com/store/apps/details?id=com.kazami.sasayaki / https://sasayaki.cc/

## 残タスク(mameta の最終確認用)

- [ ] Xハンドルのブラウザ手動確認(x.com が bot アクセスを 402 でブロックするため未確定)
- [ ] 最有力候補に絞った後、J-PlatPat 直接照会(音声系・アプリ区分)
- [ ] Siri 実機フレーズテスト(日英)— 「Hey Siri, ◯◯で右スタート」
- [ ] 最終決定 → 全ドキュメント置換・Bundle ID 確定・ドメイン取得

## 第2ラウンド候補(2026-08-15 追加、調査中)

Lullalog 脱落・Koelog リスクありのため、基準1(Siri発音)・基準4(育児文脈の温かさ)を満たす新候補:

| 候補 | 由来 | Siriフレーズ(日) | App Store | Play | 商標 | .app | X |
|---|---|---|---|---|---|---|---|
| **Nenelog(ネネログ)** | ねんね + log | 「ネネログで右スタート」 | ○ | ○ | ○ | ○ DNS未解決 | ?(web言及ゼロ=空きの公算大) |
| Hushlog(ハッシュログ) | hush + log | 「ハッシュログで右スタート」 | ○ | ○ | ○ | ○ | ?(`.hushlogin`ノイズ・ハッシュ誤読が減点) |
| Suyalog(スヤログ) | すやすや + log | 「スヤログで右スタート」 | △ | △ | ○ | ○ | ?(1字違いの Suryalog が配信中、検索が永続的に汚染) |
| Cradlo(クレイドロ) | cradle + o | — | **×** | **×** | △ | **×** | × 同カテゴリの「Cradlo: Baby Tracker」(cradlo.app)が稼働中。**除外** |

### 総合推奨(空き状況の観点)

1. **Nenelog** — 唯一の無傷候補。完全一致・近似衝突ゼロ、nenelog.app DNS未解決、商標ヒットゼロ。「nene」はスペイン語で「赤ちゃん」の意でカテゴリ好適。日本語圏でも「ねんねログ/ネネログ」のヒットなし
2. Hushlog / 3. Koelog(第1R、koelog.app の同コンセプトLPが懸念)/ 4. Suyalog

主な根拠URL(第2R): Suryalog — https://apps.apple.com/in/app/suryalog/id1555678374 / Cradlo — https://cradlo.app

### mameta の最終決定前チェック(基準1・3の残り)

- [ ] Siri 実機テスト: 「Hey Siri, ネネログで右スタート」(日)/ "Hey Siri, start right on Nenelog"(英)
- [ ] @nenelog の X ハンドルをブラウザで手動確認
- [ ] J-PlatPat で「ネネログ/NENELOG」を9類・42類あたりで照会
- [ ] nenelog.app ドメイン取得(決定次第、即)
