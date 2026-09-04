# 06. データモデル — エンティティ・ローカルDB・同期

## 設計原則

1. **ローカルファースト**: 深夜・機内モード・電波なしでも全機能(AI構造化以外)が動く。同期は非同期で後追い。
2. **イベントソーシング風の追記型**: 記録は不変イベントとして追加し、訂正は打ち消しイベント(「取り消し」1語 undo と相性が良い)。UIは畳み込んだ現在ビューを表示。
3. **家族間の競合は「両方残して警告」**: 同時刻帯の重複記録はマージせず、タイムラインに並べて重複マークを付ける(自動マージの誤動作は信頼を失う)。

## エンティティ

```
Family
  id, name, createdAt
  members: [Caregiver]         // 最大: Free 2人 / Premium 無制限
Caregiver
  id, familyId, displayName, role(mother/father/grandparent/other), authUid
Baby
  id, familyId, name, birthDate, sex(optional), photoRef(optional)

Event (基底・追記型)
  id (UUIDv7: 時系列ソート可能), babyId, caregiverId
  type, occurredAt, createdAt, source(voice_l1|voice_l2|siri|tap|widget|import)
  revokedBy (打ち消しイベントID, nullable)
  payload (type別、下記)

type別 payload:
  nursing:   { segments: [{side: L|R, startedAt, endedAt}], note? }
  bottle:    { amountMl, kind(formula|breast_milk|mixed) }
  pumping:   { side, amountMl? , durationSec? }
  diaper:    { pee: bool, poop: bool, amount?(S|M|L), consistency? }
  sleep:     { startedAt, endedAt? }       // endedAt null = 睡眠中
  temperature:{ celsius }
  medicine:  { name?, note? }
  bath:      {}
  growth:    { weightG?, heightMm? }
  memo:      { text, aiParsed?: [EventDraft] }  // AI構造化の結果は下書き。本人確認後にEvent化

ActiveSession (シングルトン的状態、端末間同期対象)
  babyId, kind(nursing|sleep), currentSide?, segments[], startedAt, listeningMode
```

- `source` を必ず記録する: 音声経由率のKPI(`05`)・Layers実験・#BuildInPublic のネタ(「うちの記録の73%は声」)に直結。
- タイムゾーンは occurredAt を UTC+端末TZ併記で保存(帰省・旅行で壊れない)。

## ローカルDB

- **SQLDelight**(KMP標準。共通Kotlinからタイプセーフに使える)。
- スキーマは追記型に合わせ `events` 単一テーブル+type別ビュー。`payload` はJSON列(kotlinx.serialization)。
- ローカル端末設定は `app_settings(setting_key, setting_value)` に保存する。2026-09-04時点ではテーマ選択(`auto/light/dark/night`)を保持し、イベント履歴とは分離する。
- `src/commonMain/sqldelight/databases/<version>.db` を移行の比較元として保存し、`verifySqlDelightMigration`を必須ゲートにする。
- 全文検索はメモのみ(FTS5)。

## 同期・バックエンド

- **Supabase**(supabase-kt が KMP 対応 — `03-tech-stack.md` の裏取り結果を反映のこと):
  - Auth: Sign in with Apple / Google。**初回はサインイン不要(匿名)で全機能が使え、家族共有 or 機種変更時に初めてアカウント作成**(産後の親に登録フォームを見せない)。
  - DB: Postgres + Row Level Security(family 単位で分離)。
  - Realtime: パートナーの記録が数秒でタイムラインに反映(「共有」体験の核)。
  - Edge Functions: AIメモ構造化のプロキシ(APIキーをクライアントに置かない)。
- 同期プロトコル: 追記型なので**イベントのアップサートのみ**(last-write-win不要)。オフラインキュー→接続時フラッシュ。
- プライバシー原則(ストア文言・Peace Prize提出文で明言):
  - 音声データを自社サーバーに送らない・保存しない。認識はオンデバイス優先(iOSはオンデバイス確定。Androidは端末のオンデバイス認識が使えない場合のみOSの音声認識サービス経由となるため、文言は「音声を当社サーバーに送信・保存しません」に統一し、過大表現しない — `03-tech-stack.md` 参照)。
  - 送信されるのは構造化された記録イベントのみ。メモのAI構造化のみ明示的なテキスト送信(設定でオフ可)。
  - 子どもの写真は v1 ではプロフィール1枚のみ・Supabase Storage(P2まで拡張しない)。

## エクスポート

- CSV / PDF(小児科受診用の直近1週間サマリ)は P1。データポータビリティはぴよログからの乗り換え障壁を下げる(ぴよログのエクスポート形式のインポートを P1 で検討 — 乗り換え導線として強力)。
