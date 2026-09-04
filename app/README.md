# Nenelog app — KMP + Compose Multiplatform

モジュール構成は `docs/03-tech-stack.md`、設計は `docs/04-architecture.md` 参照。

## モジュール

- `composeApp` — 共通UI(CMP)。iOS向けに `ComposeApp.framework` を生成
- `shared/domain` — 純Kotlin。イベントモデル・`NursingSessionStateMachine`・`EventStore`(テスト最厚)
- `shared/voice` — `VoiceEngine` 抽象(actual は iosApp/androidApp)
- `shared/data` — SQLDelightによるローカル永続化(Supabase同期はWeek 3)
- `androidApp` — Androidエントリ+FGS音声操作。診断スパイクはdebugソースに隔離
- `iosApp` — Xcode プロジェクト(要 Xcode)

## ビルド(このMac)

JDK は Homebrew の openjdk@21(sudo不要のため cask ではなく formula)。Android SDK は
`/opt/homebrew/share/android-commandlinetools`を使用。clone後に追跡対象外の
`local.properties`へ各環境のAndroid SDKパスを設定する。

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home

./gradlew :shared:domain:jvmTest :shared:data:jvmTest  # JVMテスト
./gradlew :androidApp:assembleDebug     # Android APK
./gradlew :androidApp:installDebug      # Pixel 実機へ(USB接続時)
open iosApp/iosApp.xcodeproj            # iOS は Xcode インストール後
```

## 注意

- `applicationId` / Bundle ID は `app.nenelog.nenelog` で確定(2026-08-15 Nenelog 決定、docs/15)。
  ストア登録後は変更不可
- iosApp の TEAM_ID は `iosApp/Configuration/Config.xcconfig` に設定
