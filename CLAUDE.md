# CLAUDE.md — Nenelog実装セッションへの指示書

あなた(Claude Code)はこのリポジトリで、**voice-in/voice-out で完結する世界初の授乳・育児記録アプリ**を RevenueCat Shipaton 2026(提出締切 2026-10-01 15:45 JST)に間に合わせて完成・リリースさせる。設計はすべて `docs/` に確定済み。**まず `docs/00-brief.md` を読み、次に現在の週の `docs/12-schedule.md` タスクを確認してから作業を始めること。**

## 最初のセッションでやること(順番厳守)

1. `docs/00-brief.md` → `docs/12-schedule.md` を読む(全docsを一気読みしない。必要時に参照)
2. Week 1 タスクの先頭から着手: アカウント確認(特に **Play Console の作成日確認 = R1リスク**)→ 命名確定(`docs/15-naming.md`)→ KMP雛形 → 4つの技術スパイク(`docs/12` 記載)
3. スパイク結果を `research/spike-results.md` に記録し、`docs/16-risks.md` の該当リスクを更新

## プロジェクトの鉄則

- **締切は動かない。** スコープはP0/P1/P2の3階層(`docs/01`)。遅延時はP1の賞対応から切る。P0(9/20ストア公開)は死守。
- **音声体験が心臓。** タイマー・音声まわりの domain ロジックには最も厚くテストを書く(`docs/04` のテスト方針)。実機検証を後回しにしない。
- **英語ファースト+日本語完全対応。** UI文言・TTS台本・ストア文言はすべて日英対で `strings` 管理。
- **音声の台本・コマンド文法を変えるときは必ず `docs/05-voice-ux.md` を先に更新**(docsとコードの乖離はこのプロジェクトの敵)。
- **正直さは仕様。**「世界初」は限定句付きのみ(`research/competitive-analysis.md`)。プライバシー文言は「音声を当社サーバーに送信・保存しない」で統一。
- AI(Claude API)を使うのはP1のメモ構造化のみ。実装時は `claude-api` スキルを読んで最新モデル・料金を確認する。

## ドキュメント地図

| 知りたいこと | ファイル |
|---|---|
| 全体像・締切 | docs/00-brief.md |
| 機能仕様・スコープ階層 | docs/01-product-spec.md |
| 賞ごとの戦略(何のためにその機能があるか) | docs/02-award-strategy.md |
| 技術選定(裏取り済み) | docs/03-tech-stack.md / research/tech-verification.md |
| モジュール構成・テスト方針 | docs/04-architecture.md |
| 音声コマンド・TTS台本(日英) | docs/05-voice-ux.md |
| データモデル・同期 | docs/06-data-model.md |
| 課金・ペイウォール | docs/07-monetization.md |
| 通知(OneSignal) | docs/08-notifications.md |
| デザイン・ナイトモード | docs/09-design-system.md |
| Siri/App Intents・FGS・Galaxy | docs/10-platform-integrations.md |
| 発信計画(じゃけえ) | docs/11-buildinpublic.md |
| 週次スケジュール・ゲート | docs/12-schedule.md |
| ストア申請・審査対策 | docs/13-store-release.md |
| デモ動画絵コンテ | docs/14-demo-video.md |
| 命名プロセス | docs/15-naming.md |
| リスク台帳 | docs/16-risks.md |
| 競合調査(市場空白の根拠) | research/competitive-analysis.md |
| #BuildInPublic 受賞者の発信分析 | research/bip-winner-analysis.md |

## ディレクトリ構成(予定)

`docs/03-tech-stack.md` の「リポジトリ構成」参照。アプリ本体は `app/` 以下にKMPプロジェクトとして作成する。

## Decision Log(意思決定はここに追記)

- 2026-08-15: プロジェクト発足。KMP+Compose Multiplatform採用(JetBrains賞+実利)。狙う賞はP0=Peace/BuildInPublic/KMP/Design/HAMM/OneSignal/Grand、P1=Galaxy/Stripe/Layers/Noise、捨てる=Catvertising/Game/NextGen/Influencer/Replit(理由は docs/02)。
- 2026-08-15: 仮称 Lullalog。正式名称は 8/19 までに docs/15 のプロセスで確定。
- 2026-08-15: 命名調査の結果、**Lullalog は正式名称候補から除外**(完全同名・同カテゴリの「Lullalog: Baby Tracker」が App Store に存在、`research/naming-check.md`)。最有力は第2ラウンド候補の **Nenelog**(全項目クリーン)。Siri実機テスト+最終決定は mameta(〜8/19)。
- 2026-08-15: Gmail証跡調査で Play Console / Apple Developer Program とも登録証跡なし(`research/account-status.md`)。R1 は「新規個人アカウント=14日クローズドテスト要件あり」前提で進行。mameta が即日登録要。
- 2026-08-15: **正式名称 Nenelog(ネネログ)に決定**(mameta 承認)。Bundle ID = `app.nenelog.nenelog`。docs・コードの置換完了。ドメイン/Xハンドル取得・Siri実機確認・J-PlatPat照会は残タスク(docs/15)。
- 2026-08-15: **Play Console 個人アカウント登録完了**($25支払い済、開発者名 Shinei Kikkawa)。作成日 8/15 の新規個人アカウントとなり **R1(12人×14日クローズドテスト)適用が正式確定**。9/8 テスト開始デッドライン有効。残タスク=本人確認(身分証)+電話番号SMS認証(`research/account-status.md`)。
- 2026-08-15: **Galaxy Seller Portal 登録完了**(Private Seller、Samsung Account は Google 連携で作成)。**Commercial Seller 申請も同日提出済み(審査中、数日〜数週間)**。受取用PayPal日本アカウントを新規作成(個人メールアドレスは非公開)。承認まで有料アプリ登録不可のため R5 は「承認待ち」に更新(`research/account-status.md`)。
- 2026-08-16: リポジトリのディレクトリ名を `lullalog` → `nenelog` に変更(`~/dev/project/own/nenelog`)。Play Console 本人確認は Google 審査中(8/16 時点で承認メール未着)。
- 2026-08-18: **Play Console 検証3タスク全完了**(本人確認 8/17 承認+SMS認証 8/18 完了)→ アプリ作成可能に。残タスク = アプリ「Nenelog」作成+開発者表示名「YuyaA」の変更。
- 2026-08-18: **Galaxy 方針決定 — Private Seller のまま無料構成で提出**。Commercial Seller 却下(8/17、個人不可・法人のみ)を受け公式ルールを照合した結果、Galaxy 賞は「Galaxy Store 公開+Galaxy 最適化」が要件で課金不要(RevenueCat SDK+IAP の全体要件は Play/App Store 版で充足)。Galaxy 版はペイウォール・課金導線を外す(docs/16 R5 解決済み)。
- 2026-08-19: **Apple Developer Program 年会費決済完了**(¥12,980 税込、注文番号は非公開)。Mac Developer アプリの App Store 決済が支払い方法検証バグで詰まったため、Web 登録のカード直接決済で回避。アクティベーションメール待ち。
- 2026-08-31: **Android音声スパイク全項目合格・R2解決**。Pixel 8(Android 17)で、機内モードのオンデバイス日本語認識+TTS、読み上げ後の連続再開、FGS×画面オフ5分08秒・31回再開・3コマンド応答を実機確認。初回の`ERROR_SERVER_DISCONNECTED(11)`再起動暴走は、再開予約の単一化・再開時`cancel()`除去・切断時recognizer再生成で解消。認識ミスと端末差はR7で継続検証。
- 2026-08-31: **Apple Developer Program・iOS実機署名環境の開通を確認**。Developer Team、Apple Development証明書、自動プロビジョニングでNenelogをiPhone XSにインストールし起動成功。
- 2026-08-31: **iOS音声スパイク全項目合格**。`ja-JP available=true/onDevice=true`、画面オンの複数認識+TTS再開、デバッガなし画面オフ5分・3コマンド応答を実機確認。ライブ音声が`isFinal`にならない問題は800ms無音終端で解決。背景で録音I/O再起動が`!rec`になる問題は、I/Oを維持してTTS中だけrecognition requestを外す方式で解決。P0はSFSpeechRecognizer経路に確定。
- 2026-09-01: **CMP日本語TextFieldスパイク合格・R4解決**。iPhone XSで標準日本語IMEの候補表示・変換確定を確認。初期欄にDone/フォーカス解除がないUI問題は`singleLine`+`ImeAction.Done`+余白タップ解除で修正し、実機確認済み。UIKitViewラップは不要。
- 2026-09-01: **App Intents × KMPスパイク合格・ゲートG1全4件完了**。iPhone XSでSiriの「ネネログで左/右スタート」からKMP共有domainをアプリ非表示で呼び出し、IntentDialogの文字・音声応答を実機確認。初回のSiriショートカット許可と「応答の読み上げを優先」が必要で、オンボーディング対応をR12へ記録。
- 2026-09-01: **PixelのGoogle Assistant/GeminiはSiri同等L3にしない方針を公式仕様で再確認**。App Actions Custom Intentは`en-US`限定で授乳向け日本語BIIもない。Android 16+のAppFunctionsは将来の本命だが実験版でGemini連携はprivate preview。Android P0はPixel実機合格済みのL2(FGS画面オフ連続認識+アプリTTS)を採用し、App Actions/AppFunctionsはP2とする。R13は方針確定で解決。
- 2026-09-01: **P0授乳縦切りを本番導線へ統合**。タップ、Siri App Intents、iOS/Android L2が同一のDB-backed `NursingSessionService`を使い、プロセス再作成復元、左右切替、重複開始の冪等性、停止のtransaction保存、90分自動保存、undoを実装。Composeホーム+タイムライン、Pixelのホーム連動FGS+停止通知、iPhoneのホーム連動SFSpeechRecognizerを実装し、JVMテスト・Android APK・iOS arm64 buildは合格。G2実授乳の完走は実機再接続後に実施予定。
- 2026-09-01: **Pixel本番導線の技術完走と品質ゲートを分離**。日英・画面オフ・完全オフラインで認識→TTS→保存は完走したが、2〜3回の言い直しと日本語TTSの「右」誤読が実環境で発覚。G2/R7は未完のままとし、TTS読み仮名、Android固定語バイアス、最大5候補の完全一致選択を即時実装。認識成功率90%+の反復計測前に製品品質合格とは扱わない。
- 2026-09-01: **Android開始コマンドは自然な「右/左スタート」を維持**。Pixel静音30発話で右/左開始各40%、代替語彙20発話も全体50%かつ左右差が大きかったため、OS認識に合わせて不自然な語彙へ変える案を棄却。「右/左」は短縮形として残し、固定コマンド向け認識方式の改善をR7として前倒しする。
- 2026-09-01: **Vosk固定文法が静音品質ゲート合格、採用は実環境確認まで保留**。Pixelで同じ「右/左スタート」を測り、起動待機なしは16/20(80%)だったが、700ms先に認識を起動して振動後に話すと右9/10・左10/10、全体19/20(95%)。逆方向誤実行0件。発話を変えず先頭音欠落を解消できた。次は小声・生活音・背景会話・48MBモデル容量を評価する。
- 2026-09-01: **Voskの50cm小声は6/10でR7継続**。右2/5・左4/5、失敗4件はすべて方向語欠落で逆方向誤実行は0件。自然なコマンドは変えず、AudioRecord生波形の計測とクリッピング監視付き入力ゲインを比較する。
- 2026-09-01: **Vosk小声は3倍入力で6/10→9/10へ改善**。右4/5・左5/5、逆方向0件、クリッピング0‰。左右別目標の確定前なので、十分な音量余裕を使って5倍入力を比較する。
- 2026-09-03: **Vosk入力補正は3倍を採用候補に確定**。同じ50cm小声で5倍は7/10となり、3倍の9/10を下回った。日本語L2の実機評価ビルドではVosk固定文法+3倍入力を実タイマー画面へ接続し、モデル不可/英語はOS認識へフォールバックする。最終採用は生活音・誤作動・48MB容量確認後。
- 2026-09-03: **Android高精度L2を通常ホームへ接続し、両OSの実製品導線に合格**。Pixel 8で通常タイマー開始後、「左→右→左→右→ストップ」を言い直しなしで認識し、各TTS、左右各1分のtransaction保存、FGS終了まで完走。Vosk固定文法+3倍入力をdebug評価版に採用し、生活音・画面オフ長時間・48MB容量はR7で継続判断する。同日、iPhone XSも古い能力チェック版から通常ホーム版へ上書きし、実画面での操作・音声応答を確認した。
- (以降、週次ゲートの結果と方針変更をここに追記)

## 発信との連携

実装中に「発信ネタになる出来事」(初めて動いた瞬間・ハマった話・設計の面白い判断)があったら `marketing/post-ideas.md` に1行メモを残すこと。投稿の生成・実行は `~/dev/sns/x-auto-account/` 側のセッションが担当する(このリポジトリからは直接投稿しない)。
