# 15. 命名 — 正式名称の決定プロセス(8/19までに確定)

> **✅ 決定(2026-08-15): 正式名称 = Nenelog(ネネログ)**。mameta 承認済み。
> 経緯: 初期4候補は全滅または高リスク(仮称 Lullalog は App Store に完全同名アプリが存在し使用不可)。
> 第2ラウンド候補 Nenelog が全項目クリーンで採用(`research/naming-check.md`)。
> Bundle ID / applicationId = `app.nenelog.nenelog`(コード反映済み)。
> 残タスク: nenelog.app ドメイン取得(Cloudflare、mameta実施予定)・Siri実機フレーズ確認・J-PlatPat照会(任意・ストア申請前まで)。
> ~~@nenelog Xハンドル取得~~ → **8/18 スキップ決定(mameta判断。公式アカウントは作らない方針のため確保もしない)**。

## 選定基準(優先順)

1. **Siriフレーズに入れて発音しやすい**(iOSの仕様でApp Shortcutsフレーズにアプリ名が必須。「Hey Siri, ◯◯で右スタート」が言いにくい名前は音声アプリとして致命傷)
2. 日英両方で読める・発音がぶれない(審査員は英語圏)
3. App Store / Google Play / 商標(日本+US)/ ドメイン(.app)/ Xハンドルの空き
4. 育児文脈で意味が通る・温かい

## 候補

| 候補 | 由来 | Siriフレーズ検証(日) | 懸念 |
|---|---|---|---|
| **Lullalog(ララログ)** ← 現仮称 | lullaby + log | 「ララログで右スタート」○ | lull系の既存アプリ(Lumi, Lullaai等)との近似を要確認 |
| Koelog(コエログ) | 声 + log | 「コエログで右スタート」○ | 英語話者が発音を迷う(コーログ?) |
| Nightfeed | 夜間授乳 | 英語のみ自然 | 日本語で言いにくい |
| Sasayaki | ささやき | 英語話者に発音困難 | × 基準2で脱落気味 |

## 決定手順(実装セッションのWeek1タスク)

1. 各候補で App Store/Play 検索(完全一致・類似)+ J-PlatPat と USPTO の商標簡易検索
2. `<name>.app` ドメインと Xハンドルの空き確認
3. 実機で Siri フレーズ認識テスト(日英)— **Siriが正しく聞き取れない名前は即除外**
4. 残った候補から mameta が最終決定 → 全ドキュメントの「Lullalog(仮称)」を置換、Bundle ID(`app.<name>.<name>`)確定、ドメイン取得

> 注意: Bundle ID とストア登録名は後から変えられない/変えにくい。**コードより先に名前を確定させる**こと。
