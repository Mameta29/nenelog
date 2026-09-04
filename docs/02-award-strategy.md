# 02. Shipaton 2026 賞戦略 — 「狙える賞は全て狙う」の具体化

> 前提データ: 2025年は812提出・受賞枠約60(入賞率ベース約7%)。審査員は英語圏(Charlie Chapman / David Barnard, RevenueCat)。
> 提出はDevpost 1件で複数カテゴリにノミネート可能(提出フォームで対象賞を選択)。

## サマリ: 賞ごとの狙い度

| 賞 | 賞金(1-3位) | 狙い度 | 必要な追加実装/行動 | 優先度 |
|---|---|---|---|---|
| RevenueCat Peace Prize | $15K/$10K/$5K | ◎ 本命 | アクセシビリティ完全対応+社会的インパクトの提示 | P0 |
| #BuildInPublic | $30K/$20K/$10K | ◎ 本命 | じゃけえでの英日発信(8月中に開始) | P0 |
| Ship Kotlin Everywhere (JetBrains) | $15K/$10K/$5K | ◎ 戦略的穴場 | KMP/CMP採用(技術選定に織込済) | P0 |
| RevenueCat Design Award | $15K/$10K/$5K | ○ | 音声状態の可視化デザイン+モーション作り込み | P0 |
| HAMM Award | $15K/$10K/$5K | ○ | 創造的マネタイズ設計(`07`) | P0 |
| Keep Them Coming Back (OneSignal) | $25K/$15K/$5K | ○ | OneSignal Journeys の本気実装(`08`) | P0 |
| Grand Prize | $100K | △ 宝くじ | 早期リリース+グロース施策+数値報告 | P0(早期リリース自体が全賞に効く) |
| Best App for Galaxy (Samsung) | 非金銭+Galaxy Storeフィーチャー | ○ 穴場 | Galaxy Store公開+フォルダブル最適化 | P1 |
| Funnel Vision (Stripe) | $15K/$10K/$5K | △ | RevenueCat Funnels+StripeでWeb課金ファネル | P1 |
| The Growth Loop (Layers) | $15K/$10K/$5K | △ | Layers SDKで成長実験1本+学びの報告 | P1 |
| Most Viral App (Noise) | $15K/$10K/$5K | △ | Noiseでコンテンツ制作(じゃけえ発信と統合) | P1 |
| Catvertising / Best Game / Next Gen / Influencer各賞 / Replit | — | × 捨てる | 下記理由 | — |

## 各賞の攻め方

### Peace Prize(最本命)
- 2025年受賞: CPR訓練 / 難聴者向け字幕 / 子どもの気分・投薬記録 → 「ヘルスケア×アクセシビリティ×具体的な受益者」が勝ちパターン。
- 提出文で語るストーリー:
  1. 視覚障害のある親にとって既存の育児記録アプリは実質使用不能 → voice-in/voice-out はそのまま支援技術(**アクセシビリティ用途の音声中核機能は無償**)
  2. 産後うつの一因である睡眠分断・認知負荷を「画面を見ない記録」で軽減(エビデンス引用: 産後の睡眠と精神健康の研究を1〜2本)
  3. 帝王切開後・双子・NICU 卒業児の家庭への無償プレミアム提供プログラム
- 実装要件: VoiceOver/TalkBack完全対応は口だけでは伝わらない。**デモ動画に視覚障害ユーザー相当の操作シーン(画面オフ・VoiceOverオン)を入れる**。

### #BuildInPublic(努力で勝率を上げられる唯一の賞)
- 2025年受賞: Gurwi(教育格差の物語) / **Echo Reminder(voice-first、2位)** / Tomo Japan(日本発3位)。voice-first も日本発も受賞実績あり。
- じゃけえの bio「子育て以外なんでも自動化する0歳児パパ」→ 「ついに育児(の記録)を自動化する」は物語として完璧。**自分の子で毎晩ドッグフーディングする様子が最強のコンテンツ**。
- 必須: 英語での発信(審査員が読めること)。方針は `11-buildinpublic.md`。8月中に開始しないと積み上がらない。

### Ship Kotlin Everywhere(JetBrains)
- KMP + Compose Multiplatform 縛りで母数が少ない。判定基準は「クロスプラットフォーム品質・一貫性・Kotlinの効果的な使用」+ コミュニティ貢献はオプション加点。
- 攻め方: 「ビジネスロジック・DB・課金は共通Kotlin、**音声レイヤーだけ expect/actual でネイティブに落とす**」構成は KMP のショーケースとして理想的。
- 加点行動: 音声レイヤーの KMP 統合パターンを技術記事(Zenn英訳 or dev.to)として公開 → 「Kotlin Multiplatformコミュニティへの貢献」加点+#BuildInPublicのネタにもなる。

### Design Award
- 「音声UIの状態(待機/認識中/応答中)を画面とハプティクスでどう表現するか」を独自のデザイン言語に昇華する(`09-design-system.md`)。
- ナイトモードの作り込み(純黒+赤系、深夜3時に眩しくない)は審査員に刺さりやすい具体性がある。

### HAMM Award
- 2025年1位 Vector Guard は「収益の50倍を低所得地区に還元」という構造で勝った。単なるサブスクでは勝てない。
- 本アプリの設計(詳細 `07`): 睡眠不足の親に誠実なペイウォール(「深夜に誤タップで課金させない」設計)+ アクセシビリティ無償枠 + NICU家庭無償プログラム + 家族プラン。「誠実さを収益戦略にする」ストーリー。

### OneSignal / Galaxy / Stripe / Layers / Noise
- OneSignal: 授乳間隔リマインダー・パートナーへの「記録されたよ」通知・週次サマリJourney。**「通知が育児の連携プレーを作る」**という必然性がある(ただの再訪促進通知ではない)。
- Galaxy: Android ビルドの Galaxy Store 同時公開+Compose の adaptive layout でフォルダブル対応。競合が少ない。
- Stripe Funnel: Web ランディング(`lullalog.app`)に RevenueCat Funnels(ノーコードWeb課金ファネル)+ RevenueCat Billing(Stripe接続)を置く。祖父母がギフトとして贈る「ギフトサブスク」をWebファネルで売る、が自然な設計。
- Layers(裏取り済: グロース実験・広告アトリビューション・ASOの統合プラットフォーム。ペイウォールA/B専業ではない): SDKを入れ、「じゃけえ発信→インストール」のアトリビューション+ASO実験を1本回して学びを報告する。
- Noise(裏取り済: AI生成ではなく**実在のUGCクリエイターに動画制作を発注するマーケットプレイス**、ビュー課金制): クリエイターに英語圏向けショート動画を1〜2本発注し、「日本の開発者が英語圏にリーチする」実験として#BuildInPublicのネタにもする。

### 捨てる賞と理由(記録として残す)
- **Catvertising(広告)**: 育児アプリに広告はUX・ブランド・Peace Prizeストーリーと矛盾。二兎を追わない。
- **Best Game / Influencer各賞(生産性・栄養・ヨガ・キャリア・ゲーム)**: テーマ不一致。
- **Next Gen**: 学生限定。
- **Replit Idea to Income**: Replitでの開発が審査対象。開発環境が異なるため対象外。

## 提出物チェックリスト(Devpost)

- [ ] アプリ機能説明テキスト(英語、各賞の審査基準に対応する段落を明示的に置く)
- [ ] デモ動画 ≤2分(YouTube公開、`14-demo-video.md` の絵コンテ)
- [ ] App Store / Google Play / Galaxy Store のURL
- [ ] 1024×1024 アイコン
- [ ] スクリーンショット 1179×2556(フレームなし)≥1枚
- [ ] 審査員用プロモコード(または無料トライアル)— **プロモコードは3ストア分用意**
- [ ] 対象賞カテゴリの選択(上表の P0/P1 全て)
- [ ] Grand Prize 用の成長数値レポート(インストール・ユーザー・収益、9月末時点)
